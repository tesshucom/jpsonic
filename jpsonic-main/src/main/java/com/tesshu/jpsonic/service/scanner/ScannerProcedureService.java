package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_FUTURE;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
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
import org.springframework.transaction.annotation.Transactional;

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
    private final JapaneseReadingUtils readingUtils;

    private static final int ACQUISITION_MAX = 10;

    public ScannerProcedureService(SettingsService settingsService, MusicFolderService musicFolderService,
            IndexManager indexManager, MediaFileService mediaFileService, WritableMediaFileService wmfs,
            PlaylistService playlistService, MediaFileDao mediaFileDao, ArtistDao artistDao, AlbumDao albumDao,
            StaticsDao staticsDao, SortProcedureService sortProcedure, ScannerStateServiceImpl scannerStateService,
            Ehcache indexCache, MediaFileCache mediaFileCache, JapaneseReadingUtils readingUtils) {
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
        this.readingUtils = readingUtils;
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
            staticsDao.deleteBefore(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(retention, ChronoUnit.DAYS));
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

        indexManager.startIndexing();

        // TODO To be fixed in v111.6.0
        if (settingsService.isIgnoreFileTimestampsNext()) {
            indexManager.expungeGenreOtherThan(Collections.emptyList());
            mediaFileDao.resetLastScanned();
            settingsService.setIgnoreFileTimestampsNext(false);
            settingsService.save();
        } else if (settingsService.isIgnoreFileTimestamps()) {
            indexManager.expungeGenreOtherThan(Collections.emptyList());
            mediaFileDao.resetLastScanned();
        }

        indexCache.removeAll();
        mediaFileCache.setEnabled(false);
        mediaFileCache.removeAll();

        if (!settingsService.isUseCleanUp() && mediaFileDao.existsNonPresent()) {
            // If useCleanUp=false(default), Clean-up can be managed by Scan processes.
            // Removing present=false in advance can reduce the number of subsequent queries issued.
            expungeFileStructure();
        }

        createScanEvent(scanDate, ScanEventType.BEFORE_SCAN, null);
    }

    void parseFileStructure(@NonNull Instant scanDate) throws ExecutionException {
        for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders()) {
            MediaFile root = wmfs.getMediaFile(scanDate, musicFolder.toPath());
            if (root != null) {
                mediaFileDao.updateLastScanned(root.getId(), scanDate);
                scanFile(scanDate, musicFolder, root);
            }
        }
        createScanEvent(scanDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
    }

    private void interruptIfCancelled() throws ExecutionException {
        if (scannerState.isDestroy()) {
            throw new ExecutionException(new InterruptedException("The scan was stopped due to the shutdown."));
        }
    }

    void scanFile(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file)
            throws ExecutionException {

        interruptIfCancelled();
        if (!FAR_FUTURE.equals(file.getLastScanned()) && !FAR_FUTURE.equals(file.getChildrenLastUpdated())) {
            scannerState.incrementScanCount();
            writeParsedCount(scanDate, file);
        }

        if (file.isDirectory()) {
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, true)) {
                scanFile(scanDate, folder, child);
            }
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, false)) {
                scanFile(scanDate, folder, child);
            }
        }
    }

    void parseVideo(@NonNull Instant scanDate) throws ExecutionException {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> videos = mediaFileDao.getUnparsedVideos(ACQUISITION_MAX, folders);
        int countUpdate = 0;
        while (!videos.isEmpty()) {
            for (MediaFile video : videos) {
                interruptIfCancelled();
                MediaFile updated = mediaFileDao.updateMediaFile(wmfs.parseVideo(scanDate, video));
                if (updated != null) {
                    indexManager.index(updated);
                    countUpdate++;
                }
                scannerState.incrementScanCount();
                writeParsedCount(scanDate, video);
            }
            interruptIfCancelled();
            videos = mediaFileDao.getUnparsedVideos(ACQUISITION_MAX, folders);
        }
        createScanEvent(scanDate, ScanEventType.PARSE_VIDEO, String.format("Parsed(%d)", countUpdate));
    }

    void expungeFileStructure() {
        indexManager.expunge(mediaFileDao.getArtistExpungeCandidates(), mediaFileDao.getAlbumExpungeCandidates(),
                mediaFileDao.getSongExpungeCandidates());
        mediaFileDao.expunge();
    }

    @Transactional
    public void iterateFileStructure(@NonNull Instant scanDate) {
        if (settingsService.isUseCleanUp()) {
            return;
        }
        writeInfo("Marking non-present files.");
        mediaFileDao.markNonPresent(scanDate);
        expungeFileStructure();
        String comment = String.format("%d files checked or parsed.", scannerState.getScanCount());
        createScanEvent(scanDate, ScanEventType.CLEAN_UP_FILE_STRUCTURE, comment);
    }

    private MediaFile albumOf(@NonNull Instant scanDate, @NonNull MediaFile fetchedFirstChild,
            @NonNull MediaFile registered) {
        registered.setArtist(fetchedFirstChild.getAlbumArtist());
        registered.setArtistSort(fetchedFirstChild.getAlbumArtistSort());
        registered.setArtistSortRaw(fetchedFirstChild.getAlbumArtistSort());
        registered.setAlbumName(fetchedFirstChild.getAlbumName());
        registered.setAlbumSort(fetchedFirstChild.getAlbumSort());
        registered.setAlbumSortRaw(fetchedFirstChild.getAlbumSort());
        registered.setYear(fetchedFirstChild.getYear());
        registered.setGenre(fetchedFirstChild.getGenre());
        Instant lastModified = wmfs.getLastModified(scanDate, registered.toPath());
        registered.setChanged(lastModified);
        registered.setCreated(lastModified);
        registered.setLastScanned(scanDate);
        registered.setPresent(true);
        mediaFileService.findCoverArt(registered.toPath())
                .ifPresent(coverArtPath -> registered.setCoverArtPathString(coverArtPath.toString()));
        readingUtils.analyze(registered);
        return registered;
    }

    @Transactional
    public void iterateAlbumId3(@NonNull Instant scanDate, boolean withPodcast) {
        albumDao.iterateLastScanned(scanDate, withPodcast);
        indexManager.expungeAlbum(albumDao.getExpungeCandidates(scanDate));
        albumDao.expunge(scanDate);
    }

    private int updateAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) throws ExecutionException {
        List<MediaFile> registereds = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        int countUpdate = 0;
        while (!registereds.isEmpty()) {
            for (MediaFile registered : registereds) {
                interruptIfCancelled();
                sortProcedure.updateOrderOfSongs(registered);
                MediaFile fetchedFirstChild = mediaFileDao.getFetchedFirstChildOf(registered);
                MediaFile album = fetchedFirstChild == null ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);
                album.setChildrenLastUpdated(scanDate);
                MediaFile updated = mediaFileDao.updateMediaFile(album);
                if (updated != null) {
                    indexManager.index(updated);
                    countUpdate++;
                }
                scannerState.incrementScanCount();
                writeParsedCount(scanDate, registered);
            }
            interruptIfCancelled();
            registereds = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        }
        return countUpdate;
    }

    private int createAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) throws ExecutionException {
        List<MediaFile> registereds = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        int countNew = 0;
        while (!registereds.isEmpty()) {
            for (MediaFile registered : registereds) {
                interruptIfCancelled();
                sortProcedure.updateOrderOfSongs(registered);
                MediaFile fetchedFirstChild = mediaFileDao.getFetchedFirstChildOf(registered);
                MediaFile album = fetchedFirstChild == null ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);
                album.setChildrenLastUpdated(scanDate);
                album.setLastScanned(scanDate);
                MediaFile updated = mediaFileDao.updateMediaFile(album);
                if (updated != null) {
                    indexManager.index(updated);
                    countNew++;
                }
                scannerState.incrementScanCount();
                writeParsedCount(scanDate, registered);
            }
            interruptIfCancelled();
            registereds = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        }
        return countNew;
    }

    void parseAlbum(@NonNull Instant scanDate) throws ExecutionException {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        int countUpdate = updateAlbums(scanDate, folders);
        int countNew = createAlbums(scanDate, folders);
        String comment = String.format("Update(%d)/New(%d)", countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.PARSE_ALBUM, comment);
    }

    boolean isPodcastInMusicFolders() {
        return musicFolderService.getAllMusicFolders().stream()
                .anyMatch(folder -> folder.getPathString().equals(settingsService.getPodcastFolder()));
    }

    private Album albumId3Of(@NonNull Instant scanDate, int folderId, @NonNull MediaFile song,
            @Nullable Album registered) {
        Album album = registered == null ? new Album() : registered;
        album.setFolderId(folderId);
        album.setPath(song.getParentPathString());
        album.setName(song.getAlbumName());
        album.setNameReading(song.getAlbumReading());
        album.setNameSort(song.getAlbumSort());
        album.setArtist(song.getAlbumArtist());
        album.setArtistReading(song.getAlbumArtistReading());
        album.setArtistSort(song.getAlbumArtistSort());
        album.setYear(song.getYear());
        album.setGenre(song.getGenre());
        album.setCreated(song.getChanged());
        album.setMusicBrainzReleaseId(song.getMusicBrainzReleaseId());
        if (registered != null) {
            album.setCoverArtPath(registered.getCoverArtPath());
        } else {
            mediaFileService.findCoverArt(song.getParent())
                    .ifPresent(coverArtPath -> album.setCoverArtPath(coverArtPath.toString()));
        }
        album.setLastScanned(scanDate);
        album.setPresent(true);
        return album;
    }

    void refleshAlbumID3(@NonNull Instant scanDate) throws ExecutionException {

        boolean withPodcast = isPodcastInMusicFolders();
        iterateAlbumId3(scanDate, withPodcast);

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        Function<MediaFile, MusicFolder> toMusicFolder = (song) -> folders.stream()
                .filter(f -> f.getPathString().equals(song.getFolder())).findFirst().get();

        List<MediaFile> songs = mediaFileDao.getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        int countUpdate = 0;
        while (!songs.isEmpty()) {
            for (MediaFile song : songs) {
                interruptIfCancelled();
                Album registered = albumDao.getAlbum(song.getAlbumArtist(), song.getAlbumName());
                Album album = albumId3Of(scanDate, toMusicFolder.apply(song).getId(), song, registered);
                Album updated = albumDao.updateAlbum(album);
                if (updated != null) {
                    indexManager.index(updated);
                    countUpdate++;
                }
            }
            interruptIfCancelled();
            songs = mediaFileDao.getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }

        songs = mediaFileDao.getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        int countNew = 0;
        while (!songs.isEmpty()) {
            for (MediaFile song : songs) {
                interruptIfCancelled();
                Album album = albumId3Of(scanDate, toMusicFolder.apply(song).getId(), song, null);
                Album created = albumDao.createAlbum(album);
                if (created != null) {
                    indexManager.index(created);
                    countNew++;
                }
            }
            interruptIfCancelled();
            songs = mediaFileDao.getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }

        String comment = String.format("Update(%d)/New(%d)", countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.REFRESH_ALBUM_ID3, comment);
    }

    private Artist artistId3Of(@NonNull Instant scanDate, int folderId, @NonNull MediaFile song,
            @Nullable Artist registered) {
        Artist artist = registered == null ? new Artist() : registered;
        artist.setFolderId(folderId);
        artist.setName(song.getAlbumArtist());
        artist.setReading(song.getAlbumArtistReading());
        artist.setSort(song.getAlbumArtistSort());
        mediaFileService.getParent(song).ifPresent(parent -> artist.setCoverArtPath(parent.getCoverArtPathString()));
        artist.setLastScanned(scanDate);
        artist.setPresent(true);
        return artist;
    }

    @Transactional
    public void iterateArtistId3(@NonNull Instant scanDate, boolean withPodcast) {
        artistDao.iterateLastScanned(scanDate, withPodcast);
        indexManager.expungeArtistId3(artistDao.getExpungeCandidates(scanDate));
        artistDao.expunge(scanDate);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (artist) Not reusable
    void refleshArtistId3(@NonNull Instant scanDate) throws ExecutionException {

        boolean withPodcast = isPodcastInMusicFolders();
        iterateArtistId3(scanDate, withPodcast);

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        Function<MediaFile, MusicFolder> toMusicFolder = (song) -> folders.stream()
                .filter(f -> f.getPathString().equals(song.getFolder())).findFirst().get();

        List<MediaFile> songs = mediaFileDao.getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);
        int countUpdate = 0;
        while (!songs.isEmpty()) {
            for (MediaFile song : songs) {
                interruptIfCancelled();
                Artist created = artistDao
                        .updateArtist(artistId3Of(scanDate, toMusicFolder.apply(song).getId(), song, null));
                if (created != null) {
                    indexManager.index(created, toMusicFolder.apply(song));
                    countUpdate++;
                }
            }
            interruptIfCancelled();
            songs = mediaFileDao.getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }

        songs = mediaFileDao.getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);
        int countNew = 0;
        while (!songs.isEmpty()) {
            for (MediaFile song : songs) {
                interruptIfCancelled();
                Artist created = artistDao
                        .createArtist(artistId3Of(scanDate, toMusicFolder.apply(song).getId(), song, null));
                if (created != null) {
                    indexManager.index(created, toMusicFolder.apply(song));
                    countNew++;
                }
            }
            interruptIfCancelled();
            songs = mediaFileDao.getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }

        String comment = String.format("Update(%d)/New(%d)", countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.REFRESH_ARTIST_ID3, comment);
    }

    void parsePodcast(@NonNull Instant scanDate) throws ExecutionException {
        if (settingsService.getPodcastFolder() == null) {
            return;
        }
        Path podcastFolder = Path.of(settingsService.getPodcastFolder());
        MediaFile root = wmfs.getMediaFile(scanDate, podcastFolder);
        if (root != null) {
            mediaFileDao.updateLastScanned(root.getId(), scanDate);
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

        if (file.isDirectory()) {
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, true)) {
                scanPodcast(scanDate, folder, child);
            }
            for (MediaFile child : wmfs.getChildrenOf(scanDate, file, false)) {
                scanPodcast(scanDate, folder, child);
            }
        }
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
        indexManager.expungeGenreOtherThan(genres);
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
        sortProcedure.updateSortOfAlbum(musicFolderService.getAllMusicFolders());
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
