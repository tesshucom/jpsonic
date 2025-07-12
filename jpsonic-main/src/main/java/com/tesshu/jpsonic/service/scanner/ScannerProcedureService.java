/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_FUTURE;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Orderable;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.domain.ScanLog.ScanLogType;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.apache.commons.lang3.exception.UncheckedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procedure used for main processing of scan
 */
@Service
public class ScannerProcedureService {

    /**
     * Interval at which scan count logging and scan events are triggered.
     * <p>
     * For every N parsed media files, a log message and a scan event will be
     * emitted.
     * </p>
     */
    private static final int SCAN_LOG_INTERVAL = 250;

    private static final int EXPUNGE_WAIT_INTERVAL = 20_000;
    private static final int EXPUNGE_BATCH_SIZE = 1_000;

    private static final String MSG_SKIP = "Skipped by the settings.";
    private static final String MSG_UNNECESSARY = "Skipped as it is not needed.";

    private static final List<ScanEventType> SCAN_PHASE_ALL = Arrays
        .asList(ScanEventType.BEFORE_SCAN, ScanEventType.MUSIC_FOLDER_CHECK,
                ScanEventType.PARSE_FILE_STRUCTURE, ScanEventType.PARSE_VIDEO,
                ScanEventType.PARSE_PODCAST, ScanEventType.CLEAN_UP_FILE_STRUCTURE,
                ScanEventType.PARSE_ALBUM, ScanEventType.UPDATE_SORT_OF_ALBUM,
                ScanEventType.UPDATE_ORDER_OF_ALBUM, ScanEventType.UPDATE_SORT_OF_ARTIST,
                ScanEventType.UPDATE_ORDER_OF_ARTIST, ScanEventType.UPDATE_ORDER_OF_SONG,
                ScanEventType.REFRESH_ALBUM_ID3, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3,
                ScanEventType.REFRESH_ARTIST_ID3, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3,
                ScanEventType.UPDATE_ALBUM_COUNTS, ScanEventType.UPDATE_GENRE_MASTER,
                ScanEventType.RUN_STATS, ScanEventType.IMPORT_PLAYLISTS, ScanEventType.CHECKPOINT,
                ScanEventType.AFTER_SCAN);

    private static final Logger LOG = LoggerFactory.getLogger(ScannerProcedureService.class);

    private final SettingsService settingsService;
    private final MusicFolderServiceImpl musicFolderService;
    private final IndexManager indexManager;
    private final MediaFileService mediaFileService;
    private final WritableMediaFileService wmfs;
    private final PlaylistService playlistService;
    private final TemplateWrapper template;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final StaticsDao staticsDao;
    private final SortProcedureService sortProcedure;
    private final ScannerStateServiceImpl scannerState;
    private final MusicIndexServiceImpl musicIndexService;
    private final MediaFileCache mediaFileCache;
    private final JapaneseReadingUtils readingUtils;
    private final JpsonicComparators comparators;
    private final ThreadPoolTaskExecutor scanExecutor;

    private static final int ACQUISITION_MAX = 10_000;
    private static final int REPEAT_WAIT_MILLISECONDS = 50;

    private final AtomicBoolean cancel = new AtomicBoolean();

    public ScannerProcedureService(SettingsService settingsService,
            MusicFolderServiceImpl musicFolderService, IndexManager indexManager,
            MediaFileService mediaFileService, WritableMediaFileService wmfs,
            PlaylistService playlistService, TemplateWrapper template, MediaFileDao mediaFileDao,
            ArtistDao artistDao, AlbumDao albumDao, StaticsDao staticsDao,
            SortProcedureService sortProcedure, ScannerStateServiceImpl scannerStateService,
            MusicIndexServiceImpl musicIndexService, MediaFileCache mediaFileCache,
            JapaneseReadingUtils readingUtils, JpsonicComparators comparators,
            @Qualifier("scanExecutor") ThreadPoolTaskExecutor scanExecutor) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.indexManager = indexManager;
        this.mediaFileService = mediaFileService;
        this.wmfs = wmfs;
        this.playlistService = playlistService;
        this.template = template;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.staticsDao = staticsDao;
        this.sortProcedure = sortProcedure;
        this.scannerState = scannerStateService;
        this.musicIndexService = musicIndexService;
        this.mediaFileCache = mediaFileCache;
        this.readingUtils = readingUtils;
        this.comparators = comparators;
        this.scanExecutor = scanExecutor;
    }

    public boolean isCancel() {
        return cancel.get();
    }

    public void setCancel(boolean b) {
        cancel.set(b);
    }

    private void writeInfo(@NonNull String msg) {
        if (LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    /**
     * Logs the current number of scanned media files at fixed intervals, and emits
     * a SCANNED_COUNT scan event.
     * <p>
     * If {@code scanCount % SCAN_LOG_INTERVAL != 0}, nothing is logged.
     * </p>
     *
     * @param scanDate the timestamp of the current scan operation
     * @param file     the media file currently being scanned (used for trace
     *                 logging)
     */
    private void writeParsedCount(@NonNull Instant scanDate, @NonNull MediaFile file) {
        long scanCount = scannerState.getScanCount();

        if (scanCount % SCAN_LOG_INTERVAL != 0) {
            return;
        }

        String msg = "Scanned media library with " + scanCount + " entries.";

        if (LOG.isInfoEnabled()) {
            writeInfo(msg);
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Scanning file {}", file.toPath());
        }

        createScanEvent(scanDate, ScanEventType.SCANNED_COUNT, msg);
    }

    /**
     * Creates a scan log entry if the type requires logging or logging is enabled.
     *
     * @param scanDate the timestamp of the scan event
     * @param logType  the type of scan log to create
     */
    void createScanLog(@NonNull Instant scanDate, @NonNull ScanLogType logType) {
        boolean shouldCreate = switch (logType) {
        case SCAN_ALL, EXPUNGE, FOLDER_CHANGED -> true;
        default -> settingsService.isUseScanLog();
        };

        if (shouldCreate) {
            staticsDao.createScanLog(scanDate, logType);
        }
    }

    /**
     * Rotates old scan logs based on the retention setting. If the retention is set
     * to the default, only the latest entry is kept. Otherwise, entries older than
     * the retention threshold are deleted.
     */
    void rotateScanLog() {
        int retention = settingsService.getScanLogRetention();
        int defaultRetention = settingsService.getDefaultScanLogRetention();

        if (retention == defaultRetention) {
            staticsDao.deleteOtherThanLatest();
            return;
        }

        Instant threshold = Instant
            .now()
            .truncatedTo(ChronoUnit.DAYS)
            .minus(retention, ChronoUnit.DAYS);

        staticsDao.deleteBefore(threshold);
    }

    /**
     * Creates and records a scan event if applicable.
     *
     * @param scanDate the timestamp of the scan
     * @param logType  the type of scan event to record
     * @param comment  optional descriptive comment for the event
     */
    void createScanEvent(@NonNull Instant scanDate, @NonNull ScanEventType logType,
            @Nullable String comment) {

        scannerState.setLastEvent(logType);

        boolean shouldCreateEvent = switch (logType) {
        case SUCCESS, DESTROYED, CANCELED -> true;
        default -> settingsService.isUseScanEvents();
        };

        if (!shouldCreateEvent) {
            return;
        }

        Long maxMemory = null;
        Long totalMemory = null;
        Long freeMemory = null;

        if (settingsService.isMeasureMemory()) {
            Runtime runtime = Runtime.getRuntime();
            maxMemory = runtime.maxMemory();
            totalMemory = runtime.totalMemory();
            freeMemory = runtime.freeMemory();
        }

        ScanEvent scanEvent = new ScanEvent(scanDate, now(), logType, maxMemory, totalMemory,
                freeMemory, null, comment);

        staticsDao.createScanEvent(scanEvent);
    }

    /**
     * Prepares the system for a media scan.
     * <p>
     * This includes:
     * <ul>
     * <li>Starting indexing</li>
     * <li>Clearing caches and optionally resetting media data based on
     * settings</li>
     * <li>Cleaning up stale file structures if needed</li>
     * <li>Creating a BEFORE_SCAN scan event</li>
     * </ul>
     * </p>
     *
     * @param scanDate the timestamp of the scan initialization
     */
    void beforeScan(@NonNull Instant scanDate) {
        // Start indexing process
        indexManager.startIndexing();

        // If file timestamps should be ignored, clear scan history and index
        if (settingsService.isIgnoreFileTimestamps()) {
            mediaFileDao.resetLastScanned(null);
            artistDao.deleteAll();
            indexManager.deleteAll();
        }

        // Clear and disable in-memory media file cache
        mediaFileCache.setEnabled(false);
        mediaFileCache.removeAll();

        // Clean up orphaned file structures if any
        if (mediaFileDao.existsNonPresent()) {
            expungeFileStructure();
        }

        // Log BEFORE_SCAN event
        createScanEvent(scanDate, ScanEventType.BEFORE_SCAN, null);
    }

    /**
     * Checks the registered music folders for existence and order integrity.
     * <p>
     * - If a folder does not exist or is not a directory, it is marked as disabled
     * and updated.<br>
     * - If any folder has an invalid order (-1), all folders are renumbered
     * sequentially.<br>
     * - A scan event is created to record the outcome.
     * </p>
     *
     * @param scanDate the timestamp when the scan is performed; used for updating
     *                 folder state and events
     */
    void checkMudicFolders(@NonNull Instant scanDate) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, true);
        LongAdder missingCount = new LongAdder();

        for (MusicFolder folder : folders) {
            Path folderPath = folder.toPath();
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                folder.setEnabled(false);
                folder.setChanged(scanDate);
                musicFolderService.updateMusicFolder(scanDate, folder);
                missingCount.increment();
            }
        }

        String comment = "All registered music folders exist.";
        if (missingCount.intValue() > 0) {
            comment = "(%d) music folders changed to enabled.".formatted(missingCount.intValue());
            if (LOG.isWarnEnabled()) {
                LOG.warn(comment);
            }
        }

        boolean needsReorder = folders.stream().anyMatch(folder -> folder.getFolderOrder() == -1);
        if (needsReorder) {
            LongAdder order = new LongAdder();
            for (MusicFolder folder : folders) {
                order.increment();
                folder.setFolderOrder(order.intValue());
                folder.setChanged(scanDate);
                musicFolderService.updateMusicFolder(scanDate, folder);
            }
        }

        createScanEvent(scanDate, ScanEventType.MUSIC_FOLDER_CHECK, comment);
    }

    /**
     * Retrieves the media file corresponding to the given path as a root directory,
     * and updates its last scanned timestamp if found.
     *
     * @param scanDate the timestamp representing when the scan is occurring
     * @param path     the path to resolve as a media file root
     * @return an {@link Optional} containing the {@link MediaFile} if found, or
     *         empty otherwise
     */
    Optional<MediaFile> getRootDirectory(@NonNull Instant scanDate, Path path) {
        MediaFile root = wmfs.getMediaFile(scanDate, path);
        if (root == null) {
            return Optional.empty();
        }

        mediaFileDao.updateLastScanned(root.getId(), scanDate);
        return Optional.of(root);
    }

    /**
     * Parses the media folder structure by scanning all registered music folders.
     * <p>
     * For each music folder, attempts to resolve it to a root {@link MediaFile} and
     * scan its contents. If the operation is interrupted, the process is stopped
     * early.
     * </p>
     *
     * @param scanDate the timestamp of the current scan
     */
    void parseFileStructure(@NonNull Instant scanDate) {
        for (MusicFolder folder : musicFolderService.getAllMusicFolders()) {
            getRootDirectory(scanDate, folder.toPath())
                .ifPresent(root -> scanFile(scanDate, folder, root));
        }

        if (isInterrupted()) {
            return;
        }

        createScanEvent(scanDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
    }

    /**
     * Waits for a fixed interval between repeated operations.
     * <p>
     * If the thread is interrupted during sleep, the interrupt flag is restored and
     * an unchecked exception is thrown.
     * </p>
     */
    private void repeatWait() {
        try {
            Thread.sleep(REPEAT_WAIT_MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 割り込み状態を復元
            throw new UncheckedException(e);
        }
    }

    /**
     * Checks whether the scan process should be interrupted.
     * <p>
     * A scan is considered interrupted if it has been canceled or marked for
     * destruction.
     * </p>
     *
     * @return {@code true} if the scan is canceled or destroyed; {@code false}
     *         otherwise
     */
    private boolean isInterrupted() {
        return isCancel() || scannerState.isDestroy();
    }

    /**
     * Recursively scans the given media file and its children.
     * <p>
     * Skips scanning if the process is interrupted. Increments scan count for
     * non-video files and logs progress at intervals. If the file is a directory,
     * scans both directory and non-directory children recursively.
     * </p>
     *
     * @param scanDate the timestamp of the current scan
     * @param folder   the music folder to which the file belongs
     * @param file     the media file to scan
     */
    void scanFile(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file) {
        if (isInterrupted()) {
            return;
        }

        if (file.getMediaType() != MediaType.VIDEO) {
            scannerState.incrementScanCount();
            writeParsedCount(scanDate, file);
        }

        if (file.isDirectory()) {
            // First scan child directories
            for (MediaFile childDir : wmfs.getChildrenOf(scanDate, file, true)) {
                scanFile(scanDate, folder, childDir);
            }

            // Then scan child files
            for (MediaFile childFile : wmfs.getChildrenOf(scanDate, file, false)) {
                scanFile(scanDate, folder, childFile);
            }
        }
    }

    /**
     * Parses unprocessed video files across all registered music folders.
     * <p>
     * The method repeatedly acquires unparsed video entries in batches and
     * processes them by parsing metadata, updating the database, indexing them, and
     * incrementing scan count. The operation can be interrupted at any time, in
     * which case it will stop early.
     * </p>
     *
     * @param scanDate the timestamp of the current scan
     */
    void parseVideo(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> videos = mediaFileDao.getUnparsedVideos(ACQUISITION_MAX, folders);
        LongAdder count = new LongAdder();

        while (!videos.isEmpty()) {
            for (MediaFile video : videos) {
                if (isInterrupted()) {
                    break;
                }

                mediaFileDao
                    .updateMediaFile(wmfs.parseVideo(scanDate, video))
                    .ifPresent(updated -> {
                        indexManager.index(updated); // index only if update succeeded
                        count.increment(); // count only parsed + indexed videos
                    });

                scannerState.incrementScanCount();
                writeParsedCount(scanDate, video);
            }

            if (isInterrupted()) {
                return;
            }

            // Re-fetch next batch of unparsed videos
            videos = mediaFileDao.getUnparsedVideos(ACQUISITION_MAX, folders);
        }

        createScanEvent(scanDate, ScanEventType.PARSE_VIDEO,
                "Parsed(%d)".formatted(count.intValue()));
    }

    /**
     * Expunges obsolete artist, album, and song entries from both the index and
     * database.
     * <p>
     * This method performs:
     * <ul>
     * <li>Index-level expunging of artist, album, and song entries marked as
     * removable.</li>
     * <li>Database-level batch expunging in ID ranges.</li>
     * </ul>
     * Periodic wait is introduced every {@value #EXPUNGE_WAIT_INTERVAL} entries to
     * avoid long blocking. The process checks {@link #isInterrupted()} to allow
     * early termination.
     * </p>
     */
    void expungeFileStructure() {
        // Step 1: Remove artists from index
        mediaFileDao.getArtistExpungeCandidates().forEach(indexManager::expungeArtist);

        // Step 2: Remove albums from index
        mediaFileDao.getAlbumExpungeCandidates().forEach(indexManager::expungeAlbum);

        // Step 3: Remove songs from index, with periodic wait
        List<Integer> songIds = mediaFileDao.getSongExpungeCandidates();
        for (int i = 0; i < songIds.size(); i++) {
            indexManager.expungeSong(songIds.get(i));
            if (i % EXPUNGE_WAIT_INTERVAL == 0) {
                repeatWait();
                if (isInterrupted()) {
                    break;
                }
            }
        }

        // Step 4: Expunge from database in batches
        int minId = mediaFileDao.getMinId();
        int maxId = mediaFileDao.getMaxId();
        LongAdder deleted = new LongAdder();
        int nextWaitThreshold = EXPUNGE_WAIT_INTERVAL;

        for (int id = minId; id <= maxId; id += EXPUNGE_BATCH_SIZE) {
            deleted.add(mediaFileDao.expunge(id, id + EXPUNGE_BATCH_SIZE));

            if (deleted.intValue() > nextWaitThreshold) {
                nextWaitThreshold += EXPUNGE_WAIT_INTERVAL;
                repeatWait();
                if (isInterrupted()) {
                    break;
                }
            }
        }
    }

    /**
     * Iterates through the file structure to clean up non-present files and remove
     * obsolete data.
     * <p>
     * This method performs the following:
     * <ul>
     * <li>Marks media files as non-present if they are missing as of
     * {@code scanDate}</li>
     * <li>Expunges orphaned artist, album, and song entries from index and
     * database</li>
     * <li>Emits a scan event recording the cleanup operation</li>
     * </ul>
     * If the scan has been interrupted, the method exits early.
     * </p>
     *
     * @param scanDate the current timestamp for scan reference
     */
    @Transactional
    public void iterateFileStructure(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }

        writeInfo("Marking non-present files.");
        mediaFileDao.markNonPresent(scanDate);

        expungeFileStructure();

        String comment = "%d files checked or parsed.".formatted(scannerState.getScanCount());
        createScanEvent(scanDate, ScanEventType.CLEAN_UP_FILE_STRUCTURE, comment);
    }

    /**
     * Populates the given album media file with album-level metadata, based on the
     * provided representative track from the album.
     *
     * <p>
     * This method copies artist, album, year, and genre information from the
     * representative track to the album directory MediaFile, updates timestamps,
     * and marks the album as present. The album is then analyzed for indexing.
     * </p>
     *
     * @param scanDate            the current scan timestamp
     * @param representativeTrack the track from which album metadata is derived
     * @param album               the album media file to populate and return
     * @return the updated album media file with applied metadata
     */
    private MediaFile albumOf(@NonNull Instant scanDate, @NonNull MediaFile representativeTrack,
            @NonNull MediaFile album) {

        // Artist and album metadata
        album.setArtist(representativeTrack.getAlbumArtist());
        album.setArtistSort(representativeTrack.getAlbumArtistSort());
        album.setArtistSortRaw(representativeTrack.getAlbumArtistSort());
        album.setAlbumName(representativeTrack.getAlbumName());
        album.setAlbumSort(representativeTrack.getAlbumSort());
        album.setAlbumSortRaw(representativeTrack.getAlbumSort());
        album.setYear(representativeTrack.getYear());
        album.setGenre(representativeTrack.getGenre());

        // Timestamps
        album.setChanged(album.getChanged()); // Preserves existing change date
        album.setCreated(album.getChanged());
        album.setLastScanned(scanDate);

        // Mark as present and analyze
        album.setPresent(true);
        readingUtils.analyze(album);

        return album;
    }

    /**
     * Iterates over album entries to update last-scanned timestamps from ID3 tags,
     * and then performs cleanup and index maintenance.
     *
     * <p>
     * This method performs the following steps:
     * <ul>
     * <li>Updates last-scanned dates of albums (optionally including podcasts)</li>
     * <li>Removes obsolete album ID3 index entries</li>
     * <li>Expunges stale album records from the database</li>
     * </ul>
     * </p>
     *
     * @param scanDate    the timestamp representing the current scan session
     * @param withPodcast whether to include podcast entries in the iteration
     */
    @Transactional
    public void iterateAlbumId3(@NonNull Instant scanDate, boolean withPodcast) {
        // Update album last-scanned timestamps from ID3 metadata
        albumDao.iterateLastScanned(scanDate, withPodcast);

        // Expunge outdated ID3 index entries for albums
        indexManager.expungeAlbumId3(albumDao.getExpungeCandidates(scanDate));

        // Remove stale album records from the database
        albumDao.expunge(scanDate);
    }

    /**
     * Sorts and reorders a list of {@link Orderable} items based on a given
     * comparator. If the new order differs from the current one, applies the
     * provided updater function to persist the new order and returns the total
     * number of updated entries.
     * <p>
     * The method inserts wait intervals every 6000 updates and checks for
     * interruption.
     * </p>
     *
     * @param <T>        the type of the elements, must implement {@link Orderable}
     * @param list       the list of items to reorder
     * @param comparator the comparator defining the desired order
     * @param updater    a function that updates the item and returns an update
     *                   count (e.g., 1 if updated)
     * @return the total number of items updated
     */
    <T extends Orderable> int invokeUpdateOrder(List<T> list, Comparator<T> comparator,
            Function<T, Integer> updater) {
        // Capture current order before sorting
        List<Integer> rawOrders = list
            .stream()
            .map(Orderable::getOrder)
            .collect(Collectors.toList());

        // Sort list in-place according to comparator
        Collections.sort(list, comparator);

        LongAdder count = new LongAdder();

        for (int i = 0; i < list.size(); i++) {
            int expectedOrder = i + 1;
            int currentOrder = rawOrders.get(i);

            if (expectedOrder != currentOrder) {
                T item = list.get(i);
                item.setOrder(expectedOrder);
                count.add(updater.apply(item));

                if (count.intValue() % 6_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break;
                    }
                }
            }
        }

        return count.intValue();
    }

    /**
     * Updates the order of songs directly under the given parent media file.
     *
     * @param scanDate the current scan timestamp
     * @param parent   the parent media file (e.g., album or folder)
     * @return the first song after sorting, or null if none exist or parent is null
     */
    @Nullable
    MediaFile updateOrderOfSongs(@NonNull Instant scanDate, MediaFile parent) {
        if (parent == null) {
            return null;
        }

        List<MediaFile> childFiles = mediaFileService
            .getChildrenOf(parent, 0, Integer.MAX_VALUE, ChildOrder.BY_ALPHA, MediaType.DIRECTORY,
                    MediaType.ALBUM);

        if (childFiles.isEmpty()) {
            return null;
        }

        invokeUpdateOrder(childFiles, comparators.songsDefault(), wmfs::updateOrder);

        return childFiles.get(0);
    }

    /**
     * Updates album records that have changed, by updating their song order,
     * setting metadata, and indexing them.
     *
     * @param scanDate the current scan timestamp
     * @param folders  the list of music folders to restrict the scope
     * @return the number of albums updated
     */
    private int updateAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) {
        List<MediaFile> candidates = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        LongAdder updatedCount = new LongAdder();

        updateAlbums: while (!candidates.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) {
                if (i % 1_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateAlbums;
                    }
                }

                MediaFile registered = candidates.get(i);
                MediaFile fetchedFirstChild = updateOrderOfSongs(scanDate, registered);
                MediaFile album = (fetchedFirstChild == null) ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);

                album.setChildrenLastUpdated(scanDate);

                mediaFileDao.updateMediaFile(album).ifPresent(updated -> {
                    indexManager.index(updated);
                    updatedCount.increment();
                });
            }

            candidates = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        }

        return updatedCount.intValue();
    }

    /**
     * Creates album entries from unparsed album folders. Updates song order, sets
     * album metadata, and indexes the updated albums.
     *
     * @param scanDate the current scan timestamp
     * @param folders  the list of music folders to process
     * @return the number of albums created
     */
    private int createAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) {
        List<MediaFile> candidates = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        LongAdder createdCount = new LongAdder();

        createAlbums: while (!candidates.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) {
                if (i % 1_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createAlbums;
                    }
                }

                MediaFile registered = candidates.get(i);
                MediaFile fetchedFirstChild = updateOrderOfSongs(scanDate, registered);
                MediaFile album = (fetchedFirstChild == null) ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);

                album.setChildrenLastUpdated(scanDate);
                album.setLastScanned(scanDate);

                mediaFileDao.updateMediaFile(album).ifPresent(updated -> {
                    indexManager.index(updated);
                    createdCount.increment();
                });
            }

            candidates = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        }

        return createdCount.intValue();
    }

    /**
     * Parses album metadata by updating existing albums and creating new ones. Also
     * triggers a scan event indicating the result.
     *
     * @param scanDate the current scan timestamp
     * @return true if any albums were parsed (updated or created), false otherwise
     */
    boolean parseAlbum(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return false;
        }

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();

        int updatedCount = updateAlbums(scanDate, folders);
        int createdCount = createAlbums(scanDate, folders);
        boolean parsed = updatedCount > 0 || createdCount > 0;

        String comment = "Update(%d)/New(%d)".formatted(updatedCount, createdCount);
        createScanEvent(scanDate, ScanEventType.PARSE_ALBUM, comment);

        return parsed;
    }

    /**
     * Checks whether the configured podcast folder path exists among the registered
     * music folders.
     *
     * @return true if the podcast folder is included in the music folders, false
     *         otherwise
     */
    boolean isPodcastInMusicFolders() {
        String podcastFolderPath = settingsService.getPodcastFolder();

        return musicFolderService
            .getAllMusicFolders()
            .stream()
            .anyMatch(folder -> folder.getPathString().equals(podcastFolderPath));
    }

    /**
     * Constructs or updates an ID3-based Album object from a given song media file.
     *
     * @param scanDate   the timestamp of the current scan
     * @param folderId   the folder ID to associate with the album
     * @param song       the song media file containing ID3 metadata
     * @param registered the existing Album object to update, or null to create new
     * @return the constructed or updated Album object
     */
    private Album albumId3Of(@NonNull Instant scanDate, int folderId, @NonNull MediaFile song,
            @Nullable Album registered) {

        Album album = (registered == null) ? new Album() : registered;

        album.setFolderId(folderId);
        album.setPath(song.getParentPathString());

        album.setName(song.getAlbumName());
        album.setNameReading(song.getAlbumReading());
        album.setNameSort(song.getAlbumSort());

        album.setArtist(song.getAlbumArtist());
        album.setArtistReading(song.getAlbumArtistReading());
        album.setArtistSort(song.getAlbumArtistSort());

        album.setYear(song.getYear());
        album.setGenre(mediaFileService.getID3AlbumGenresString(song)); // VIDEO is not included

        album.setCreated(song.getChanged());
        album.setMusicBrainzReleaseId(song.getMusicBrainzReleaseId());

        mediaFileService
            .getParent(song)
            .ifPresent(parent -> album.setCoverArtPath(parent.getCoverArtPathString()));

        album.setLastScanned(scanDate);
        album.setPresent(true);

        return album;
    }

    /**
     * Finds the corresponding MusicFolder for the given MediaFile by matching
     * folder path strings.
     *
     * @param mediaFile the media file whose associated music folder is to be found
     * @return an Optional containing the matching MusicFolder if found, otherwise
     *         empty
     */
    private Optional<MusicFolder> getMusicFolder(MediaFile mediaFile) {
        return musicFolderService
            .getAllMusicFolders()
            .stream()
            .filter(folder -> folder.getPathString().equals(mediaFile.getFolder()))
            .findFirst();
    }

    /**
     * Updates existing ID3-based albums by scanning media files for changed
     * metadata. If applicable, updates the album, indexes it, and updates children
     * timestamps.
     *
     * @param scanDate    the timestamp for the scan operation
     * @param withPodcast whether to include podcast folders in the target scope
     * @return the number of updated albums
     */
    int updateAlbumId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> candidates = mediaFileDao
            .getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        LongAdder updatedCount = new LongAdder();

        updateAlbums: while (!candidates.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) {
                if (i % 4_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateAlbums;
                    }
                }

                MediaFile song = candidates.get(i);
                Album registered = albumDao.getAlbum(song.getAlbumArtist(), song.getAlbumName());

                getMusicFolder(song).ifPresent(folder -> {
                    Album album = albumId3Of(scanDate, folder.getId(), song, registered);

                    Optional.ofNullable(albumDao.updateAlbum(album)).ifPresent(updated -> {
                        indexManager.index(updated);
                        mediaFileDao.updateChildrenLastUpdated(album, scanDate);
                        updatedCount.increment();
                    });
                });
            }

            candidates = mediaFileDao.getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }

        return updatedCount.intValue();
    }

    /**
     * Creates new ID3-based Album entries from registered songs that do not yet
     * have a corresponding Album ID3 record.
     * <p>
     * This method does not parse new files from disk. Instead, it scans
     * already-registered {@link MediaFile} entries, and for each that contains
     * album-related ID3 metadata (album name, artist, etc.) but has no
     * corresponding {@link Album} record, it creates and registers a new Album.
     * <p>
     * The resulting Album entries are indexed and have their children's timestamps
     * updated.
     *
     * @param scanDate    the current scan timestamp
     * @param withPodcast whether to include podcast folders in the candidate search
     * @return the number of newly created ID3 albums
     */
    int createAlbumId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> candidates = mediaFileDao
            .getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        LongAdder createdCount = new LongAdder();

        createAlbums: while (!candidates.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) {
                if (i % 4_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createAlbums;
                    }
                }

                MediaFile song = candidates.get(i);

                getMusicFolder(song).ifPresent(folder -> {
                    Album album = albumId3Of(scanDate, folder.getId(), song, null);

                    Optional.ofNullable(albumDao.createAlbum(album)).ifPresent(created -> {
                        indexManager.index(created);
                        mediaFileDao.updateChildrenLastUpdated(album, scanDate);
                        createdCount.increment();
                    });
                });
            }

            candidates = mediaFileDao
                .getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }

        return createdCount.intValue();
    }

    /**
     * Refreshes ID3-based Album records using already-registered songs.
     * <p>
     * This method performs the following steps:
     * <ul>
     * <li>Determines whether podcast folders are included</li>
     * <li>Performs ID3 album iteration and cleanup</li>
     * <li>Updates existing Album entries from changed MediaFiles</li>
     * <li>Creates new Album entries from MediaFiles with unregistered ID3 Album
     * data</li>
     * </ul>
     * If {@code settingsService.isIgnoreFileTimestamps()} is enabled, children's
     * last-updated timestamps are reset.
     *
     * @param scanDate the current scan timestamp
     * @return true if any albums were updated or newly created; false otherwise
     */
    boolean refleshAlbumId3(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return false;
        }

        boolean withPodcast = isPodcastInMusicFolders();

        iterateAlbumId3(scanDate, withPodcast);

        if (settingsService.isIgnoreFileTimestamps()) {
            mediaFileDao.resetAlbumChildrenLastUpdated();
        }

        int countUpdate = updateAlbumId3s(scanDate, withPodcast);
        int countNew = createAlbumId3s(scanDate, withPodcast);

        String comment = "Update(%d)/New(%d)".formatted(countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.REFRESH_ALBUM_ID3, comment);

        return countUpdate > 0 || countNew > 0;
    }

    /**
     * Constructs or updates an {@link Artist} entity from the ID3 metadata of a
     * representative song.
     * <p>
     * The {@code representativeSong} is a registered {@link MediaFile} whose ID3
     * album artist tags are used to populate the {@link Artist} entity.
     *
     * @param scanDate           the current scan timestamp
     * @param folderId           the ID of the folder containing the representative
     *                           song
     * @param representativeSong the song whose ID3 metadata is used to create or
     *                           update the artist
     * @param registered         the existing Artist entity to update, or
     *                           {@code null} to create a new one
     * @return a populated or updated {@link Artist} entity based on the ID3
     *         metadata
     */
    private Artist artistId3Of(@NonNull Instant scanDate, int folderId,
            @NonNull MediaFile representativeSong, @Nullable Artist registered) {
        Artist artist = registered == null ? new Artist() : registered;

        artist.setFolderId(folderId);
        artist.setName(representativeSong.getAlbumArtist());
        artist.setReading(representativeSong.getAlbumArtistReading());
        artist.setSort(representativeSong.getAlbumArtistSort());
        artist.setCoverArtPath(representativeSong.getCoverArtPathString());
        artist.setLastScanned(scanDate);
        artist.setPresent(true);

        String index = musicIndexService.getParser().getIndex(artist).getIndex();
        artist.setMusicIndex(index);

        return artist;
    }

    /**
     * Iterates and cleans up ID3-based Artist records.
     * <p>
     * This process includes:
     * <ul>
     * <li>Marking artists scanned at the given time</li>
     * <li>Expunging outdated or missing Artist ID3 entries from the index</li>
     * <li>Removing orphaned Artist ID3 records from the database</li>
     * </ul>
     *
     * @param scanDate    the current scan timestamp
     * @param withPodcast whether to include podcast folders in the operation
     */
    @Transactional
    public void iterateArtistId3(@NonNull Instant scanDate, boolean withPodcast) {
        artistDao.iterateLastScanned(scanDate, withPodcast);

        indexManager.expungeArtistId3(artistDao.getExpungeCandidates(scanDate));

        artistDao.expunge(scanDate);
    }

    /**
     * Updates existing ID3-based Artist records based on registered songs whose
     * metadata has changed.
     * <p>
     * For each changed song, this method attempts to retrieve its parent
     * {@link MusicFolder} and, if found, constructs a new {@link Artist} entity
     * from the song's ID3 metadata. The updated artist is saved and re-indexed if
     * applicable.
     *
     * @param scanDate    the current scan timestamp
     * @param withPodcast whether to include podcast folders in the candidate search
     * @return the number of updated Artist records
     */
    int updateArtistId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> representativeSongs = mediaFileDao
            .getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);

        LongAdder countUpdate = new LongAdder();

        updateArtists: while (!representativeSongs.isEmpty()) {
            for (int i = 0; i < representativeSongs.size(); i++) {
                if (i % 15_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateArtists;
                    }
                }

                MediaFile representativeSong = representativeSongs.get(i);

                getMusicFolder(representativeSong).ifPresent(folder -> {
                    Optional
                        .ofNullable(artistDao
                            .updateArtist(artistId3Of(scanDate, folder.getId(), representativeSong,
                                    null)))
                        .ifPresent(updated -> {
                            indexManager.index(updated, folder);
                            countUpdate.increment();
                        });
                });
            }

            representativeSongs = mediaFileDao
                .getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }

        return countUpdate.intValue();
    }

    /**
     * Creates new ID3-based {@link Artist} records based on registered songs that
     * are not yet associated with any existing Artist entry.
     * <p>
     * For each eligible song, a new {@link Artist} is created from its ID3 album
     * artist metadata and stored. The artist is also indexed.
     *
     * @param scanDate    the current scan timestamp
     * @param withPodcast whether to include podcast folders in the candidate search
     * @return the number of newly created Artist records
     */
    int createArtistId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> representativeSongs = mediaFileDao
            .getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);

        LongAdder countNew = new LongAdder();

        createArtists: while (!representativeSongs.isEmpty()) {
            for (int i = 0; i < representativeSongs.size(); i++) {
                if (i % 15_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createArtists;
                    }
                }

                MediaFile representativeSong = representativeSongs.get(i);

                getMusicFolder(representativeSong).ifPresent(folder -> {
                    Optional
                        .ofNullable(artistDao
                            .createArtist(artistId3Of(scanDate, folder.getId(), representativeSong,
                                    null)))
                        .ifPresent(created -> {
                            indexManager.index(created, folder);
                            countNew.increment();
                        });
                });
            }

            representativeSongs = mediaFileDao
                .getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }

        return countNew.intValue();
    }

    /**
     * Refreshes ID3-based {@link Artist} records based on existing songs' metadata.
     * <p>
     * This process performs the following steps:
     * <ul>
     * <li>Determines whether podcast folders should be included</li>
     * <li>Marks existing Artist records as scanned and removes outdated ones</li>
     * <li>Updates existing Artist entries based on changed song metadata</li>
     * <li>Creates new Artist entries from previously unregistered songs</li>
     * <li>Creates a {@link ScanEvent} indicating the result</li>
     * </ul>
     *
     * @param scanDate the current scan timestamp
     * @return {@code true} if any Artist was created or updated; {@code false}
     *         otherwise
     */
    // (artist) Not reusable: new instance per song
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    boolean refleshArtistId3(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return false;
        }

        boolean withPodcast = isPodcastInMusicFolders();

        iterateArtistId3(scanDate, withPodcast);

        int countUpdate = updateArtistId3s(scanDate, withPodcast);
        int countNew = createArtistId3s(scanDate, withPodcast);

        String comment = "Update(%d)/New(%d)".formatted(countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.REFRESH_ARTIST_ID3, comment);

        return countUpdate > 0 || countNew > 0;
    }

    /**
     * Parses the podcast folder and registers its contents as {@link MediaFile}
     * entries.
     * <p>
     * If a podcast folder is defined in the settings and not interrupted, this
     * method:
     * <ul>
     * <li>Creates a dummy {@link MusicFolder} wrapper</li>
     * <li>Fetches the root directory as a {@link MediaFile}</li>
     * <li>Scans the directory recursively</li>
     * <li>Creates a {@link ScanEvent} for the podcast parsing</li>
     * </ul>
     *
     * @param scanDate the timestamp for this scan operation
     */
    void parsePodcast(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }

        String podcastFolderPath = settingsService.getPodcastFolder();
        if (podcastFolderPath == null) {
            return;
        }

        Path path = Path.of(podcastFolderPath);
        MusicFolder dummy = new MusicFolder(path.toString(), null, true, null, false);

        getRootDirectory(scanDate, path).ifPresent(root -> {
            scanPodcast(scanDate, dummy, root);
            createScanEvent(scanDate, ScanEventType.PARSE_PODCAST, null);
        });
    }

    // TODO To be fixed in v111.7.0 later #1925
    void scanPodcast(@NonNull Instant scanDate, @NonNull MusicFolder folder,
            @NonNull MediaFile file) {
        if (isInterrupted()) {
            return;
        }
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

    /**
     * Updates the album count for each {@link Artist} entity.
     * <p>
     * If the process is marked as skippable or interrupted, the update is skipped
     * and a {@link ScanEvent} is still recorded with a note indicating it was
     * unnecessary. Otherwise, the album count is refreshed for each artist
     * retrieved from the DAO.
     *
     * @param scanDate  the timestamp of the scan operation
     * @param skippable whether the update should be skipped due to no preceding
     *                  changes
     */
    void updateAlbumCounts(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }

        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ALBUM_COUNTS, MSG_UNNECESSARY);
            return;
        }

        for (Artist artist : artistDao.getAlbumCounts()) {
            artistDao.updateAlbumCount(artist.getId(), artist.getAlbumCount());
        }

        createScanEvent(scanDate, ScanEventType.UPDATE_ALBUM_COUNTS, null);
    }

    /**
     * Updates the genre master table based on current media file genre usage.
     * <p>
     * If not interrupted, this method:
     * <ul>
     * <li>Retrieves current genre usage counts from media files</li>
     * <li>Updates the genre master records in the DB</li>
     * <li>Removes any obsolete genres from the index</li>
     * <li>Creates a {@link ScanEvent} to log the update</li>
     * </ul>
     *
     * @param scanDate the timestamp of the scan operation
     */
    void updateGenreMaster(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }

        List<Genre> genres = mediaFileDao.getGenreCounts();
        mediaFileDao.updateGenres(genres);
        indexManager.expungeGenreOtherThan(genres);

        createScanEvent(scanDate, ScanEventType.UPDATE_GENRE_MASTER, null);
    }

    /**
     * Re-indexes media files affected by merging, copying, or compensating
     * operations.
     * <p>
     * The method:
     * <ul>
     * <li>Combines and deduplicates the given ID lists</li>
     * <li>Fetches each {@link MediaFile} and updates its index</li>
     * <li>If the media file is an album and its children are not yet updated, it
     * updates the {@code childrenLastUpdated} timestamp</li>
     * <li>Performs periodic wait and interruption check during the loop</li>
     * </ul>
     *
     * @param scanDate    the current scan timestamp
     * @param merged      list of merged media file IDs
     * @param copied      list of copied media file IDs
     * @param compensated list of compensated media file IDs
     */
    private void invokeUpdateIndex(@NonNull Instant scanDate, List<Integer> merged,
            List<Integer> copied, List<Integer> compensated) {

        List<Integer> ids = Stream
            .concat(Stream.concat(merged.stream(), copied.stream()).distinct(),
                    compensated.stream())
            .distinct()
            .collect(Collectors.toList());

        for (int i = 0; i < ids.size(); i++) {
            MediaFile mediaFile = mediaFileService.getMediaFileStrict(ids.get(i));

            indexManager.index(mediaFile);

            if (mediaFile.getMediaType() == MediaType.ALBUM
                    && FAR_FUTURE.equals(mediaFile.getChildrenLastUpdated())) {
                mediaFileDao.updateChildrenLastUpdated(mediaFile.getPathString(), scanDate);
            }

            if (i % 10_000 == 0) {
                repeatWait();
                if (isInterrupted()) {
                    LOG.warn("""
                            Registration of the search index was interrupted.
                            Rescanning with IgnoreTimestamp enabled is recommended.
                            """);
                    break;
                }
            }
        }
    }

    /**
     * Updates the sort key information for artists.
     * <p>
     * If cleansing is enabled and strict sort is requested in settings, this
     * method:
     * <ul>
     * <li>Attempts to merge existing artist sort keys</li>
     * <li>Copies missing sort values where applicable</li>
     * <li>Compensates for missing or inconsistent sort data</li>
     * <li>Re-indexes affected artists if there are any changes</li>
     * </ul>
     * A {@link ScanEvent} is recorded to log the result.
     *
     * @param scanDate the timestamp of the scan operation
     * @return true if any artist sort key was updated
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    boolean updateSortOfArtist(@NonNull Instant scanDate) {
        boolean updated = false;

        // Skip if the scan has been interrupted
        if (isInterrupted()) {
            return updated;
        }

        // Skip if cleansing is disabled or strict sort is not required
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ARTIST, MSG_SKIP);
            return updated;
        }

        // Retrieve all music folders
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();

        // Merge existing sort values where possible
        final List<Integer> merged = sortProcedure.mergeSortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Copy sort values from available sources
        final List<Integer> copied = sortProcedure.copySortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Fill in missing sort values using fallback logic
        final List<Integer> compensated = sortProcedure.compensateSortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Re-index affected artists if any updates were made
        updated = !merged.isEmpty() || !copied.isEmpty() || !compensated.isEmpty();
        if (updated) {
            invokeUpdateIndex(scanDate, merged, copied, compensated);
        }

        // Record the result as a scan event
        String comment = "Merged(%d)/Copied(%d)/Compensated(%d)"
            .formatted(merged.size(), copied.size(), compensated.size());
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ARTIST, comment);

        return updated;
    }

    /**
     * Updates the music index of all album-type MediaFiles across all music
     * folders. Only processes MediaFiles of type ALBUM and skips others.
     */
    private void updateIndexOfAlbum() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();

        // Check if there are any album-type children; if not, skip processing
        if (mediaFileDao.getChildSizeOf(folders, MediaType.ALBUM) == 0) {
            return;
        }

        // Prepare an array of MediaTypes excluding ALBUM
        MediaType[] otherThanAlbum = Stream
            .of(MediaType.values())
            .filter(type -> type != MediaType.ALBUM)
            .toArray(MediaType[]::new);

        for (MusicFolder folder : folders) {
            int offset = 0;

            // Retrieve album-type children, excluding other types
            List<MediaFile> albums = mediaFileDao
                .getChildrenOf(folder.getPathString(), offset, ACQUISITION_MAX, ChildOrder.BY_ALPHA,
                        otherThanAlbum);

            // Iterate in batches until all albums are processed
            while (!albums.isEmpty()) {
                for (MediaFile album : albums) {
                    // Update music index using the parser
                    String musicIndex = musicIndexService.getParser().getIndex(album).getIndex();
                    album.setMusicIndex(musicIndex);
                    mediaFileDao.updateMediaFile(album);
                }

                offset += ACQUISITION_MAX;

                // Fetch the next batch of albums
                albums = mediaFileDao
                    .getChildrenOf(folder.getPathString(), offset, ACQUISITION_MAX,
                            ChildOrder.BY_ALPHA, otherThanAlbum);
            }
        }
    }

    /**
     * Updates the sort fields of album-type media files across all music folders.
     * Applies merge, copy, and compensate sort operations as necessary, and
     * reindexes updated records.
     *
     * @param scanDate The timestamp of the current scan
     * @return true if any sort field was updated, false otherwise
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    boolean updateSortOfAlbum(@NonNull Instant scanDate) {
        boolean updated = false;

        // Cancel if scan was interrupted
        if (isInterrupted()) {
            return updated;
        }

        // Skip sort cleansing if not enabled or not required, and update index instead
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            updateIndexOfAlbum();
            createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ALBUM, MSG_SKIP);
            return updated;
        }

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();

        // Step 1: Merge sort fields
        final List<Integer> merged = sortProcedure.mergeSortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Step 2: Copy sort fields
        final List<Integer> copied = sortProcedure.copySortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Step 3: Compensate missing sort fields
        final List<Integer> compensated = sortProcedure.compensateSortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        // Update index if any sorting changes were made
        updated = !merged.isEmpty() || !copied.isEmpty() || !compensated.isEmpty();
        if (updated) {
            invokeUpdateIndex(scanDate, merged, copied, compensated);
        }

        // Always update album index as final step
        updateIndexOfAlbum();

        String comment = "Merged(%d)/Copied(%d)/Compensated(%d)"
            .formatted(merged.size(), copied.size(), compensated.size());
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ALBUM, comment);

        return updated;
    }

    /**
     * Updates the order of songs located directly under each registered music
     * folder. Skips processing if the scan was interrupted.
     *
     * @param scanDate The timestamp of the current scan operation
     */
    void updateOrderOfSongsDirectlyUnderMusicfolder(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }

        // For each music folder, update the order of songs directly under it
        for (MusicFolder folder : musicFolderService.getAllMusicFolders()) {
            MediaFile root = mediaFileService.getMediaFileStrict(folder.getPathString());
            updateOrderOfSongs(scanDate, root);
        }

        // Log scan event
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_SONG, null);
    }

    /**
     * Updates the order of all artist media files based on alphabetical sorting.
     * Skips update if the process is marked as skippable or if interrupted.
     *
     * @param scanDate  The current scan timestamp
     * @param skippable Whether this step can be skipped
     */
    void updateOrderOfArtist(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }

        // Skip the update if marked as skippable
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST, MSG_UNNECESSARY);
            return;
        }

        // Retrieve all artist media files from all music folders
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> artists = mediaFileDao.getArtistAll(folders);

        // Update order based on alphabetical comparator
        int count = invokeUpdateOrder(artists, comparators.mediaFileOrderByAlpha(),
                wmfs::updateOrder);

        // Record scan event with update count
        String comment = "Updated order of (%d) artists".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST, comment);
    }

    /**
     * Updates the order of all album media files in all music folders based on
     * alphabetical sorting. Skips update if the process is marked as skippable or
     * if interrupted.
     *
     * @param scanDate  The timestamp of the current scan operation
     * @param skippable Whether this step can be skipped (e.g. if no changes were
     *                  detected)
     */
    void updateOrderOfAlbum(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }

        // Skip the update if marked as skippable
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM, MSG_UNNECESSARY);
            return;
        }

        // Retrieve all albums across music folders in alphabetical order
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> albums = mediaFileService
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folders);

        // Update order using alphabetical comparator
        int count = invokeUpdateOrder(albums, comparators.mediaFileOrderByAlpha(),
                wmfs::updateOrder);

        // Log scan event with number of updated entries
        String comment = "Updated order of (%d) albums".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM, comment);
    }

    /**
     * Updates the order of ID3-based Artist entries based on alphabetical sorting.
     * If the process is skippable or interrupted, it exits early.
     *
     * @param scanDate  The timestamp of the current scan operation
     * @param skippable Whether this step can be skipped (e.g., no relevant changes
     *                  detected)
     */
    void updateOrderOfArtistId3(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }

        // Skip update if marked as unnecessary
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, MSG_UNNECESSARY);
            return;
        }

        // Retrieve ID3 artists in alphabetical order
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);

        // Update order field of each artist based on sorted position
        int count = invokeUpdateOrder(artists, comparators.artistOrderByAlpha(),
                (artist) -> artistDao.updateOrder(artist.getId(), artist.getOrder()));

        // Log scan event with update summary
        String comment = "Updated order of (%d) ID3 artists.".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, comment);
    }

    /**
     * Updates the display order of ID3-based Album entries alphabetically. Skips
     * the process if flagged as unnecessary or if the operation is interrupted.
     *
     * @param scanDate  The timestamp when the scan process is performed
     * @param skippable Whether the update process can be skipped
     */
    void updateOrderOfAlbumId3(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }

        // Skip updating if no meaningful changes are expected
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, MSG_UNNECESSARY);
            return;
        }

        // Retrieve all ID3 albums from all music folders
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Album> albums = albumDao
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, folders);

        // Apply alphabetical ordering and update album order values
        int count = invokeUpdateOrder(albums, comparators.albumOrderByAlpha(),
                album -> albumDao.updateOrder(album.getId(), album.getOrder()));

        // Create a scan event with the update result
        String comment = "Updated order of (%d) ID3 albums.".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, comment);
    }

    /**
     * Gathers and stores media library statistics for each registered music folder.
     * Includes interruption check and staggered execution with repeat wait.
     *
     * @param scanDate The timestamp when the scan process is performed
     */
    void runStats(@NonNull Instant scanDate) {
        // Log starting message
        writeInfo("""
                Collecting media library statistics ...
                """);

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();

        // Iterate over all folders and gather statistics
        for (int i = 0; i < folders.size(); i++) {
            // Wait every 4 folders to reduce processing load
            if (i % 4 == 0) {
                repeatWait();
                if (isInterrupted()) {
                    return;
                }
            }

            // Gather and store statistics for each folder
            MediaLibraryStatistics stats = staticsDao
                .gatherMediaLibraryStatistics(scanDate, folders.get(i));
            staticsDao.createMediaLibraryStatistics(stats);
        }

        // Record scan event after statistics run completes
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
        template.checkpoint();
        createScanEvent(scanDate, ScanEventType.CHECKPOINT, null);
    }

    void success(@NonNull Instant scanDate) {
        try {
            Thread.sleep(1);
            LOG.info("Completed media library scan.");
            createScanEvent(scanDate, ScanEventType.SUCCESS, null);
        } catch (InterruptedException e) {
            createScanEvent(scanDate, ScanEventType.FAILED, null);
            throw new UncheckedException(e);
        }
    }

    /**
     * Returns the current scan phase information if a scan is in progress.
     *
     * @return An Optional containing the current ScanPhaseInfo, or empty if not
     *         scanning
     */
    public Optional<ScanPhaseInfo> getScanPhaseInfo() {
        // Not currently scanning
        if (!scannerState.isScanning()) {
            return Optional.empty();
        }

        // Get the last recorded scan event
        ScanEventType lastEvent = scannerState.getLastEvent();

        // Normalize SCANNED_COUNT to MUSIC_FOLDER_CHECK (non-phase event)
        if (lastEvent == ScanEventType.SCANNED_COUNT) {
            lastEvent = ScanEventType.MUSIC_FOLDER_CHECK;
        }

        // Find the last phase index
        int lastPhaseIndex = SCAN_PHASE_ALL.indexOf(lastEvent);

        // Unknown phase (non-standard scan sequence)
        if (lastPhaseIndex == -1) {
            return Optional.of(new ScanPhaseInfo(-1, -1, "Semi Scan Proc", -1));
        }

        // Calculate the current phase index (advance if not at the end)
        int currentPhaseIndex = (lastPhaseIndex + 1 < SCAN_PHASE_ALL.size()) ? lastPhaseIndex + 1
                : lastPhaseIndex;

        // Build and return ScanPhaseInfo
        return Optional
            .of(new ScanPhaseInfo(currentPhaseIndex, SCAN_PHASE_ALL.size(),
                    SCAN_PHASE_ALL.get(currentPhaseIndex).name(), scanExecutor.getActiveCount()));
    }

    public record ScanPhaseInfo(int phase, int phaseMax, String phaseName, int thread) {
    }
}
