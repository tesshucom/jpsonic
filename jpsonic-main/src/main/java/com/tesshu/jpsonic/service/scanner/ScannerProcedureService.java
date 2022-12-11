package com.tesshu.jpsonic.service.scanner;

import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import net.sf.ehcache.Ehcache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Procedure used for main processing of scan
 */
@Service
public class ScannerProcedureService {

    private static final Logger LOG = LoggerFactory.getLogger(ScannerProcedureService.class);

    private final SettingsService settingsService;
    private final IndexManager indexManager;
    private final MediaFileService mediaFileService;
    private final WritableMediaFileService writableMediaFileService;
    private final PlaylistService playlistService;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final SortProcedureService sortProcedure;
    private final ScannerStateServiceImpl scannerState;

    private final Ehcache indexCache;
    private final MediaFileCache mediaFileCache;

    public ScannerProcedureService(SettingsService settingsService, IndexManager indexManager,
            MediaFileService mediaFileService, WritableMediaFileService writableMediaFileService,
            PlaylistService playlistService, MediaFileDao mediaFileDao, ArtistDao artistDao, AlbumDao albumDao,
            SortProcedureService sortProcedure, ScannerStateServiceImpl scannerState, Ehcache indexCache,
            MediaFileCache mediaFileCache) {
        super();
        this.settingsService = settingsService;
        this.indexManager = indexManager;
        this.mediaFileService = mediaFileService;
        this.writableMediaFileService = writableMediaFileService;
        this.playlistService = playlistService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.sortProcedure = sortProcedure;
        this.scannerState = scannerState;
        this.indexCache = indexCache;
        this.mediaFileCache = mediaFileCache;
    }

    private void writeInfo(String msg) {
        if (settingsService.isVerboseLogScanning() && LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    private void writeScanLog(MediaFile file) {
        if (LOG.isInfoEnabled() && scannerState.getScanCount() % 250 == 0) {
            writeInfo("Scanned media library with " + scannerState.getScanCount() + " entries.");
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Scanning file {}", file.toPath());
        }
    }

    void beforeScan() {

        // TODO To be fixed in v111.6.0
        if (settingsService.isIgnoreFileTimestampsNext()) {
            mediaFileDao.resetLastScanned();
            settingsService.setIgnoreFileTimestampsNext(false);
            settingsService.save();
        } else if (settingsService.isIgnoreFileTimestamps()) {
            mediaFileDao.resetLastScanned();
        }

        sortProcedure.clearOrder();
        indexCache.removeAll();
        mediaFileCache.setEnabled(false);
        indexManager.startIndexing();
        mediaFileCache.removeAll();
    }

    // TODO To be fixed in v111.6.0 #1841
    void afterScan(Instant scanDate) {
        mediaFileCache.setEnabled(true);

        MediaLibraryStatistics stats = new MediaLibraryStatistics(scanDate);
        stats.setArtistCount(mediaFileDao.getArtistCount());
        stats.setAlbumCount(mediaFileDao.getAlbumCount());
        stats.setSongCount(mediaFileDao.getSongCount());
        stats.setTotalDurationInSeconds(mediaFileDao.getTotalSeconds());
        stats.setTotalLengthInBytes(mediaFileDao.getTotalBytes());

        indexManager.stopIndexing(stats);
        sortProcedure.clearMemoryCache();
    }

    void doCleansingProcess() {
        if (!scannerState.isDestroy() && scannerState.isEnableCleansing()) {
            writeInfo("[1/2] Additional processing after scanning by Jpsonic. Supplementing sort/read data.");
            sortProcedure.updateSortOfArtist();
            sortProcedure.updateSortOfAlbum();
            writeInfo("[1/2] Done.");
            if (settingsService.isSortStrict()) {
                writeInfo(
                        "[2/2] Additional processing after scanning by Jpsonic. Create dictionary sort index in database.");
                sortProcedure.updateOrderOfAll();
            } else {
                writeInfo(
                        "[2/2] A dictionary sort index is not created in the database. See Settings > General > Sort settings.");
            }
            writeInfo("[2/2] Done.");
        }
    }

    void scanFile(MediaFile file, MusicFolder musicFolder, Instant scanDate, boolean isPodcast)
            throws ExecutionException {

        interruptIfCancelled();
        scannerState.incrementScanCount();
        writeScanLog(file);

        // Update the root folder if it has changed.
        String musicFolderPath = musicFolder.getPathString();
        if (!musicFolderPath.equals(file.getFolder())) {
            file.setFolder(musicFolderPath);
            writableMediaFileService.updateFolder(file);
        }

        indexManager.index(file);

        if (file.isDirectory()) {
            for (MediaFile child : writableMediaFileService.getChildrenOf(file, true, false, scanDate)) {
                scanFile(child, musicFolder, scanDate, isPodcast);
            }
            for (MediaFile child : writableMediaFileService.getChildrenOf(file, false, true, scanDate)) {
                scanFile(child, musicFolder, scanDate, isPodcast);
            }
        } else {
            if (!isPodcast) {
                updateAlbum(file, musicFolder, scanDate);
                updateArtist(file, musicFolder, scanDate);
            }
        }

        mediaFileDao.markPresent(file.getPathString(), scanDate);
        artistDao.markPresent(file.getAlbumArtist(), scanDate);
    }

    void interruptIfCancelled() throws ExecutionException {
        if (scannerState.isDestroy()) {
            throw new ExecutionException(new InterruptedException("The scan was stopped due to the shutdown."));
        }
    }

    void updateAlbum(@NonNull MediaFile file, @NonNull MusicFolder musicFolder, @NonNull Instant lastScanned) {

        if (isNotAlbumUpdatable(file)) {
            return;
        }

        String artist;
        String artistReading;
        String artistSort;
        if (isEmpty(file.getAlbumArtist())) {
            artist = file.getArtist();
            artistReading = file.getArtistReading();
            artistSort = file.getArtistSort();
        } else {
            artist = file.getAlbumArtist();
            artistReading = file.getAlbumArtistReading();
            artistSort = file.getAlbumArtistSort();
        }

        Album album = getMergedAlbum(file, artist, artistReading, artistSort);
        boolean firstEncounter = !lastScanned.equals(album.getLastScanned());
        if (firstEncounter) {
            mergeOnFirstEncount(album, file, musicFolder);
        }
        album.setDurationSeconds(album.getDurationSeconds() + (int) defaultIfNull(file.getDurationSeconds(), 0));
        album.setSongCount(album.getSongCount() + 1);
        album.setLastScanned(lastScanned);
        album.setPresent(true);
        albumDao.createOrUpdateAlbum(album);

        if (firstEncounter) {
            indexManager.index(album);
        }

        if (!Objects.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            file.setAlbumArtistReading(album.getArtistReading());
            file.setAlbumArtistSort(album.getArtistSort());
            // TODO To be fixed in v111.6.0 #1925 Do not use createOrUpdate here.
            mediaFileDao.createOrUpdateMediaFile(file);
        }
    }

    void mergeOnFirstEncount(Album album, MediaFile file, MusicFolder musicFolder) {
        album.setFolderId(musicFolder.getId());
        album.setDurationSeconds(0);
        album.setSongCount(0);
        // see #414 Change properties only on firstEncounter
        if (file.getYear() != null) {
            album.setYear(file.getYear());
        }
        if (file.getGenre() != null) {
            album.setGenre(file.getGenre());
        }
    }

    boolean isNotAlbumUpdatable(MediaFile file) {
        return file.getAlbumName() == null || file.getParentPathString() == null || !file.isAudio()
                || file.getArtist() == null && file.getAlbumArtist() == null;
    }

    Album getMergedAlbum(MediaFile file, String artist, String artistReading, String artistSort) {
        Album album = albumDao.getAlbumForFile(file);
        if (album == null) {
            album = new Album();
            album.setPath(file.getParentPathString());
            album.setName(file.getAlbumName());
            album.setNameReading(file.getAlbumReading());
            album.setNameSort(file.getAlbumSort());
            album.setArtist(artist);
            album.setArtistReading(artistReading);
            album.setArtistSort(artistSort);
            album.setCreated(file.getChanged());
        }
        if (file.getMusicBrainzReleaseId() != null) {
            album.setMusicBrainzReleaseId(file.getMusicBrainzReleaseId());
        }
        MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null && parent.getCoverArtPathString() != null) {
            album.setCoverArtPath(parent.getCoverArtPathString());
        }
        return album;
    }

    void updateArtist(MediaFile file, MusicFolder musicFolder, Instant lastScanned) {
        if (file.getAlbumArtist() == null || !file.isAudio()) {
            return;
        }

        Artist artist = artistDao.getArtist(file.getAlbumArtist());
        if (artist == null) {
            artist = new Artist();
            artist.setName(file.getAlbumArtist());
            artist.setReading(file.getAlbumArtistReading());
            artist.setSort(file.getAlbumArtistSort());
        }
        if (artist.getCoverArtPath() == null) {
            MediaFile parent = mediaFileService.getParentOf(file);
            if (parent != null) {
                artist.setCoverArtPath(parent.getCoverArtPathString());
            }
        }
        final boolean firstEncounter = !lastScanned.equals(artist.getLastScanned());
        artist.setFolderId(musicFolder.getId());
        artist.setLastScanned(lastScanned);
        artist.setPresent(true);
        artistDao.createOrUpdateArtist(artist);

        if (firstEncounter) {
            indexManager.index(artist, musicFolder);
        }
    }

    void markNonPresent(Instant scanDate) {
        writeInfo("Marking non-present files.");
        mediaFileDao.markNonPresent(scanDate);
        writeInfo("Marking non-present artists.");
        artistDao.markNonPresent(scanDate);
        writeInfo("Marking non-present albums.");
        albumDao.markNonPresent(scanDate);
    }

    void updateAlbumCounts() {
        List<Artist> artists = artistDao.getAlbumCounts();
        for (Artist artist : artists) {
            artistDao.updateAlbumCount(artist.getId(), artist.getAlbumCount());
        }
    }

    void updateGenreMaster() {
        List<Genre> genres = mediaFileDao.getGenreCounts();
        mediaFileDao.updateGenres(genres);
    }

    void importPlaylists() {
        writeInfo("Starting playlist import.");
        playlistService.importPlaylists();
        writeInfo("Completed playlist import.");
    }

    void checkpoint() {
        mediaFileDao.checkpoint();
    }
}
