package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import net.sf.ehcache.Ehcache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
    private final MusicFolderService musicFolderService;
    private final IndexManager indexManager;
    private final MediaFileService mediaFileService;
    private final WritableMediaFileService wmfs;
    private final PlaylistService playlistService;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final StaticsDao staticsDao;
    private final SortProcedureService sortProcedure;
    private final ScannerStateServiceImpl scannerState;

    private final Ehcache indexCache;
    private final MediaFileCache mediaFileCache;

    public ScannerProcedureService(SettingsService settingsService, MusicFolderService musicFolderService,
            IndexManager indexManager, MediaFileService mediaFileService, WritableMediaFileService wmfs,
            PlaylistService playlistService, MediaFileDao mediaFileDao, ArtistDao artistDao, AlbumDao albumDao,
            StaticsDao staticsDao, SortProcedureService sortProcedure, ScannerStateServiceImpl scannerStateService,
            Ehcache indexCache, MediaFileCache mediaFileCache) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.indexManager = indexManager;
        this.mediaFileService = mediaFileService;
        this.wmfs = wmfs;
        this.playlistService = playlistService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.staticsDao = staticsDao;
        this.sortProcedure = sortProcedure;
        this.scannerState = scannerStateService;
        this.indexCache = indexCache;
        this.mediaFileCache = mediaFileCache;
    }

    private void writeInfo(@NonNull String msg) {
        if (LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    private void writeParsedCount(@NonNull Instant scanDate, @NonNull MediaFile file) {
        if (scannerState.getScanCount() % 250 != 0) {
            return;
        }

        String msg = "Scanned media library with " + scannerState.getScanCount() + " entries.";

        if (LOG.isInfoEnabled()) {
            writeInfo(msg);
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Scanning file {}", file.toPath());
        }

        createScanEvent(scanDate, ScanEventType.PARSED_COUNT, msg);
    }

    void createScanLog(@NonNull Instant scanDate, @NonNull ScanLogType logType) {
        if (logType == ScanLogType.SCAN_ALL || logType == ScanLogType.EXPUNGE || settingsService.isUseScanLog()) {
            staticsDao.createScanLog(scanDate, logType);
        }
    }

    void rotateScanLog() {
        int retention = settingsService.getScanLogRetention();
        if (retention == settingsService.getDefaultScanLogRetention()) {
            staticsDao.deleteOtherThanLatest();
        } else {
            staticsDao.deleteBefore(Instant.now().truncatedTo(DAYS).minus(retention, DAYS));
        }
    }

    void createScanEvent(@NonNull Instant scanDate, @NonNull ScanEventType logType, @Nullable String comment) {
        if (!(logType == ScanEventType.FINISHED || logType == ScanEventType.DESTROYED
                || logType == ScanEventType.FAILED) && !settingsService.isUseScanEvents()) {
            return;
        }
        boolean isMeasureMemory = settingsService.isMeasureMemory();
        Long maxMemory = isMeasureMemory ? Runtime.getRuntime().maxMemory() : null;
        Long totalMemory = isMeasureMemory ? Runtime.getRuntime().totalMemory() : null;
        Long freeMemory = isMeasureMemory ? Runtime.getRuntime().freeMemory() : null;
        ScanEvent scanEvent = new ScanEvent(scanDate, now(), logType, maxMemory, totalMemory, freeMemory, comment);
        staticsDao.createScanEvent(scanEvent);
    }

    void beforeScan(@NonNull Instant scanDate) {

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

        createScanEvent(scanDate, ScanEventType.BEFORE_SCAN, null);
    }

    void parseAudio(@NonNull Instant scanDate) throws ExecutionException {
        for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders()) {
            MediaFile root = wmfs.getMediaFile(scanDate, musicFolder.toPath());
            if (root != null) {
                scanFile(scanDate, musicFolder, root);
            }
        }
        createScanEvent(scanDate, ScanEventType.PARSE_AUDIO, null);
    }

    private void interruptIfCancelled() throws ExecutionException {
        if (scannerState.isDestroy()) {
            throw new ExecutionException(new InterruptedException("The scan was stopped due to the shutdown."));
        }
    }

    void scanFile(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file)
            throws ExecutionException {

        interruptIfCancelled();
        scannerState.incrementScanCount();
        writeParsedCount(scanDate, file);

        // Update the root folder if it has changed.
        String folderPath = folder.getPathString();
        if (!folderPath.equals(file.getFolder())) {
            file.setFolder(folderPath);
            wmfs.updateFolder(file);
        }

        indexManager.index(file);

        if (file.isDirectory()) {
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, true)) {
                scanFile(scanDate, folder, child);
            }
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, false)) {
                scanFile(scanDate, folder, child);
            }
        } else {
            updateAlbum(scanDate, folder, file); // Split into Audio, Album and Movie #1925
            updateArtist(scanDate, folder, file); // Split into Audio, Album and Movie #1925
        }

        mediaFileDao.markPresent(file.getPathString(), scanDate);
        artistDao.markPresent(file.getAlbumArtist(), scanDate);
    }

    void updateAlbum(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file) {

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
        boolean firstEncounter = !scanDate.equals(album.getLastScanned());
        if (firstEncounter) {
            mergeOnFirstEncount(album, file, folder);
        }
        album.setDurationSeconds(album.getDurationSeconds() + (int) defaultIfNull(file.getDurationSeconds(), 0));
        album.setSongCount(album.getSongCount() + 1);
        album.setLastScanned(scanDate);
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
            mediaFileDao.updateMediaFile(file);
        }
    }

    boolean isNotAlbumUpdatable(@NonNull MediaFile file) {
        return file.getAlbumName() == null || file.getParentPathString() == null || !file.isAudio()
                || file.getArtist() == null && file.getAlbumArtist() == null;
    }

    Album getMergedAlbum(@NonNull MediaFile file, String artist, String artistReading, String artistSort) {
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

    void mergeOnFirstEncount(@NonNull Album album, @NonNull MediaFile file, @NonNull MusicFolder folder) {
        album.setFolderId(folder.getId());
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

    void updateArtist(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file) {
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
        final boolean firstEncounter = !scanDate.equals(artist.getLastScanned());
        artist.setFolderId(folder.getId());
        artist.setLastScanned(scanDate);
        artist.setPresent(true);
        artistDao.createOrUpdateArtist(artist);

        if (firstEncounter) {
            indexManager.index(artist, folder);
        }
    }

    void parsePodcast(@NonNull Instant scanDate) throws ExecutionException {
        if (settingsService.getPodcastFolder() == null) {
            return;
        }
        Path podcastFolder = Path.of(settingsService.getPodcastFolder());
        MediaFile root = wmfs.getMediaFile(scanDate, podcastFolder);
        if (root != null) {
            MusicFolder dummy = new MusicFolder(podcastFolder.toString(), null, true, null);
            scanPodcast(scanDate, dummy, root);
            createScanEvent(scanDate, ScanEventType.PARSE_PODCAST, null);
        }
    }

    // TODO To be fixed in v111.7.0 #1927
    void scanPodcast(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file)
            throws ExecutionException {

        interruptIfCancelled();
        scannerState.incrementScanCount();
        writeParsedCount(scanDate, file);

        // Update the root folder if it has changed.
        String folderPath = folder.getPathString();
        if (!folderPath.equals(file.getFolder())) {
            file.setFolder(folderPath);
            wmfs.updateFolder(file);
        }

        indexManager.index(file);

        if (file.isDirectory()) {
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, true)) {
                scanPodcast(scanDate, folder, child);
            }
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, false)) {
                scanPodcast(scanDate, folder, child);
            }
        }

        mediaFileDao.markPresent(file.getPathString(), scanDate);
        artistDao.markPresent(file.getAlbumArtist(), scanDate);
    }

    void markNonPresent(@NonNull Instant scanDate) {
        writeInfo("Marking non-present files.");
        mediaFileDao.markNonPresent(scanDate);
        writeInfo("Marking non-present artists.");
        artistDao.markNonPresent(scanDate);
        writeInfo("Marking non-present albums.");
        albumDao.markNonPresent(scanDate);
        createScanEvent(scanDate, ScanEventType.MARK_NON_PRESENT, null);
    }

    void updateAlbumCounts(@NonNull Instant scanDate) {
        for (Artist artist : artistDao.getAlbumCounts()) {
            artistDao.updateAlbumCount(artist.getId(), artist.getAlbumCount());
        }
        createScanEvent(scanDate, ScanEventType.UPDATE_ALBUM_COUNTS, null);
    }

    void updateGenreMaster(@NonNull Instant scanDate) {
        List<Genre> genres = mediaFileDao.getGenreCounts();
        mediaFileDao.updateGenres(genres);
        createScanEvent(scanDate, ScanEventType.UPDATE_GENRE_MASTER, null);
    }

    void updateSortOfArtist(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateSortOfArtist();
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ARTIST, null);
    }

    void updateSortOfAlbum(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateSortOfAlbum();
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ALBUM, null);
    }

    void updateOrderOfArtist(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateOrderOfArtist();
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST, null);
    }

    void updateOrderOfAlbum(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateOrderOfAlbum();
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM, null);
    }

    void updateOrderOfArtistId3(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateOrderOfArtistID3();
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, null);
    }

    void updateOrderOfAlbumID3(@NonNull Instant scanDate) throws ExecutionException {
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            return;
        }
        interruptIfCancelled();
        sortProcedure.updateOrderOfAlbumID3();
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, null);
    }

    void runStats(@NonNull Instant scanDate) {
        writeInfo("Collecting media library statistics ...");
        MediaLibraryStatistics stats = new MediaLibraryStatistics(scanDate);
        for (MusicFolder folder : musicFolderService.getAllMusicFolders()) {
            stats.setFolderId(folder.getId());
            stats.setArtistCount(mediaFileDao.getArtistCount(folder));
            stats.setAlbumCount(mediaFileDao.getAlbumCount(folder));
            stats.setSongCount(mediaFileDao.getSongCount(folder));
            stats.setTotalDuration(mediaFileDao.getTotalSeconds(folder));
            stats.setTotalSize(mediaFileDao.getTotalBytes(folder));
            staticsDao.createMediaLibraryStatistics(stats);
        }
        createScanEvent(scanDate, ScanEventType.RUN_STATS, null);
    }

    void afterScan(@NonNull Instant scanDate) {
        mediaFileCache.setEnabled(true);
        indexManager.stopIndexing();
        sortProcedure.clearMemoryCache();
        createScanEvent(scanDate, ScanEventType.AFTER_SCAN, null);
    }

    void importPlaylists(@NonNull Instant scanDate) {
        writeInfo("Starting playlist import.");
        playlistService.importPlaylists();
        writeInfo("Completed playlist import.");
        createScanEvent(scanDate, ScanEventType.IMPORT_PLAYLISTS, null);
    }

    void checkpoint(@NonNull Instant scanDate) {
        mediaFileDao.checkpoint();
        createScanEvent(scanDate, ScanEventType.CHECKPOINT, null);
    }
}
