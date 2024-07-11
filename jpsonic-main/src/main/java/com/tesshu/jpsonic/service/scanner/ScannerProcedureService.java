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
import com.tesshu.jpsonic.service.search.SearchServiceUtilities;
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

    private static final String MSG_SKIP = "Skipped by the settings.";
    private static final String MSG_UNNECESSARY = "Skipped as it is not needed.";

    private static final List<ScanEventType> SCAN_PHASE_ALL = Arrays.asList(ScanEventType.BEFORE_SCAN,
            ScanEventType.MUSIC_FOLDER_CHECK, ScanEventType.PARSE_FILE_STRUCTURE, ScanEventType.PARSE_VIDEO,
            ScanEventType.PARSE_PODCAST, ScanEventType.CLEAN_UP_FILE_STRUCTURE, ScanEventType.PARSE_ALBUM,
            ScanEventType.UPDATE_SORT_OF_ALBUM, ScanEventType.UPDATE_ORDER_OF_ALBUM,
            ScanEventType.UPDATE_SORT_OF_ARTIST, ScanEventType.UPDATE_ORDER_OF_ARTIST,
            ScanEventType.UPDATE_ORDER_OF_SONG, ScanEventType.REFRESH_ALBUM_ID3,
            ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, ScanEventType.REFRESH_ARTIST_ID3,
            ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, ScanEventType.UPDATE_ALBUM_COUNTS,
            ScanEventType.UPDATE_GENRE_MASTER, ScanEventType.RUN_STATS, ScanEventType.IMPORT_PLAYLISTS,
            ScanEventType.CHECKPOINT, ScanEventType.AFTER_SCAN);

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
    private final SearchServiceUtilities searchServiceUtilities;
    private final JapaneseReadingUtils readingUtils;
    private final JpsonicComparators comparators;
    private final ThreadPoolTaskExecutor scanExecutor;

    private static final int ACQUISITION_MAX = 10_000;
    private static final int REPEAT_WAIT_MILLISECONDS = 50;

    private final AtomicBoolean cancel = new AtomicBoolean();

    public ScannerProcedureService(SettingsService settingsService, MusicFolderServiceImpl musicFolderService,
            IndexManager indexManager, MediaFileService mediaFileService, WritableMediaFileService wmfs,
            PlaylistService playlistService, TemplateWrapper template, MediaFileDao mediaFileDao, ArtistDao artistDao,
            AlbumDao albumDao, StaticsDao staticsDao, SortProcedureService sortProcedure,
            ScannerStateServiceImpl scannerStateService, MusicIndexServiceImpl musicIndexService,
            MediaFileCache mediaFileCache, SearchServiceUtilities searchServiceUtilities,
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
        this.searchServiceUtilities = searchServiceUtilities;
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

        createScanEvent(scanDate, ScanEventType.SCANNED_COUNT, msg);
    }

    void createScanLog(@NonNull Instant scanDate, @NonNull ScanLogType logType) {
        if (logType == ScanLogType.SCAN_ALL || logType == ScanLogType.EXPUNGE || logType == ScanLogType.FOLDER_CHANGED
                || settingsService.isUseScanLog()) {
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
        scannerState.setLastEvent(logType);
        if (!(logType == ScanEventType.SUCCESS || logType == ScanEventType.DESTROYED
                || logType == ScanEventType.CANCELED) && !settingsService.isUseScanEvents()) {
            return;
        }
        boolean isMeasureMemory = settingsService.isMeasureMemory();
        Long maxMemory = isMeasureMemory ? Runtime.getRuntime().maxMemory() : null;
        Long totalMemory = isMeasureMemory ? Runtime.getRuntime().totalMemory() : null;
        Long freeMemory = isMeasureMemory ? Runtime.getRuntime().freeMemory() : null;
        ScanEvent scanEvent = new ScanEvent(scanDate, now(), logType, maxMemory, totalMemory, freeMemory, null,
                comment);
        staticsDao.createScanEvent(scanEvent);
    }

    void beforeScan(@NonNull Instant scanDate) {

        indexManager.startIndexing();

        if (settingsService.isIgnoreFileTimestamps()) {
            mediaFileDao.resetLastScanned(null);
            artistDao.deleteAll();
            albumDao.deleteAll();
            indexManager.deleteAll();
        }

        mediaFileCache.setEnabled(false);
        mediaFileCache.removeAll();

        if (!settingsService.isUseCleanUp() && mediaFileDao.existsNonPresent()) {
            // If useCleanUp=false(default), Clean-up can be managed by Scan processes.
            // Removing present=false in advance can reduce the number of subsequent queries issued.
            expungeFileStructure();
        }

        createScanEvent(scanDate, ScanEventType.BEFORE_SCAN, null);
    }

    void checkMudicFolders(@NonNull Instant scanDate) {
        LongAdder notExist = new LongAdder();
        LongAdder enabled = new LongAdder();
        musicFolderService.getAllMusicFolders(false, true).forEach(folder -> {
            Path folderPath = folder.toPath();
            if (!(Files.exists(folderPath) && Files.isDirectory(folderPath))) {
                notExist.increment();
                folder.setEnabled(false);
                folder.setChanged(scanDate);
                musicFolderService.updateMusicFolder(scanDate, folder);
                enabled.increment();
            }
        });
        String comment = "All registered music folders exist.";
        if (notExist.intValue() > 0) {
            comment = "(%d) music folders changed to enabled.".formatted(notExist.intValue());
            if (LOG.isWarnEnabled()) {
                LOG.warn(comment);
            }
        }

        if (musicFolderService.getAllMusicFolders(false, true).stream()
                .anyMatch(folder -> folder.getFolderOrder() == -1)) {
            LongAdder order = new LongAdder();
            musicFolderService.getAllMusicFolders(false, true).forEach(folder -> {
                order.increment();
                folder.setFolderOrder(order.intValue());
                folder.setChanged(scanDate);
                musicFolderService.updateMusicFolder(scanDate, folder);
            });
        }
        createScanEvent(scanDate, ScanEventType.MUSIC_FOLDER_CHECK, comment);
    }

    Optional<MediaFile> getRootDirectory(@NonNull Instant scanDate, Path path) {
        MediaFile root = wmfs.getMediaFile(scanDate, path);
        if (root != null) {
            mediaFileDao.updateLastScanned(root.getId(), scanDate);
            return Optional.of(root);
        }
        return Optional.empty();
    }

    void parseFileStructure(@NonNull Instant scanDate) {
        musicFolderService.getAllMusicFolders().forEach(folder -> getRootDirectory(scanDate, folder.toPath())
                .ifPresent(root -> scanFile(scanDate, folder, root)));
        if (isInterrupted()) {
            return;
        }
        createScanEvent(scanDate, ScanEventType.PARSE_FILE_STRUCTURE, null);
    }

    private void repeatWait() {
        try {
            Thread.sleep(REPEAT_WAIT_MILLISECONDS);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    private boolean isInterrupted() {
        return isCancel() || scannerState.isDestroy();
    }

    void scanFile(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file) {

        if (isInterrupted()) {
            return;
        }
        if (file.getMediaType() != MediaType.VIDEO) {
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
                mediaFileDao.updateMediaFile(wmfs.parseVideo(scanDate, video)).ifPresent(updated -> {
                    indexManager.index(updated);
                    count.increment();
                });
                scannerState.incrementScanCount();
                writeParsedCount(scanDate, video);
            }
            if (isInterrupted()) {
                return;
            }
            videos = mediaFileDao.getUnparsedVideos(ACQUISITION_MAX, folders);
        }
        createScanEvent(scanDate, ScanEventType.PARSE_VIDEO, "Parsed(%d)".formatted(count.intValue()));
    }

    void expungeFileStructure() {
        mediaFileDao.getArtistExpungeCandidates().forEach(indexManager::expungeArtist);
        mediaFileDao.getAlbumExpungeCandidates().forEach(indexManager::expungeAlbum);
        List<Integer> cands = mediaFileDao.getSongExpungeCandidates();
        for (int i = 0; i < cands.size(); i++) {
            indexManager.expungeSong(cands.get(i));
            if (i % 20_000 == 0) {
                repeatWait();
                if (isInterrupted()) {
                    break;
                }
            }
        }
        int minId = mediaFileDao.getMinId();
        int maxId = mediaFileDao.getMaxId();
        final int batchSize = 1000;
        LongAdder deleted = new LongAdder();
        int threshold = 20_000;
        for (int id = minId; id <= maxId; id += batchSize) {
            deleted.add(mediaFileDao.expunge(id, id + batchSize));
            if (deleted.intValue() > threshold) {
                threshold += 20_000;
                repeatWait();
                if (isInterrupted()) {
                    break;
                }
            }
        }
    }

    @Transactional
    public void iterateFileStructure(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }
        if (settingsService.isUseCleanUp()) {
            createScanEvent(scanDate, ScanEventType.CLEAN_UP_FILE_STRUCTURE, MSG_SKIP);
            return;
        }
        writeInfo("Marking non-present files.");
        mediaFileDao.markNonPresent(scanDate);
        expungeFileStructure();
        String comment = "%d files checked or parsed.".formatted(scannerState.getScanCount());
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
        registered.setChanged(registered.getChanged());
        registered.setCreated(registered.getChanged());
        registered.setLastScanned(scanDate);
        registered.setPresent(true);
        readingUtils.analyze(registered);
        return registered;
    }

    @Transactional
    public void iterateAlbumId3(@NonNull Instant scanDate, boolean withPodcast) {
        albumDao.iterateLastScanned(scanDate, withPodcast);
        indexManager.expungeAlbumId3(albumDao.getExpungeCandidates(scanDate));
        albumDao.expunge(scanDate);
    }

    <T extends Orderable> int invokeUpdateOrder(List<T> list, Comparator<T> comparator, Function<T, Integer> updater) {
        List<Integer> rawOrders = list.stream().map(Orderable::getOrder).collect(Collectors.toList());
        Collections.sort(list, comparator);
        LongAdder count = new LongAdder();
        for (int i = 0; i < list.size(); i++) {
            int order = i + 1;
            if (order != rawOrders.get(i)) {
                T orderable = list.get(i);
                orderable.setOrder(order);
                count.add(updater.apply(orderable));
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

    @Nullable
    MediaFile updateOrderOfSongs(@NonNull Instant scanDate, MediaFile parent) {
        if (parent == null) {
            return null;
        }
        List<MediaFile> songs = mediaFileService
                .getChildrenOf(parent, 0, Integer.MAX_VALUE, ChildOrder.BY_ALPHA, MediaType.DIRECTORY, MediaType.ALBUM)
                .stream().collect(Collectors.toList());
        if (songs.isEmpty()) {
            return null;
        }
        invokeUpdateOrder(songs, comparators.songsDefault(), wmfs::updateOrder);
        return songs.get(0);
    }

    private int updateAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) {
        List<MediaFile> registereds = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        LongAdder count = new LongAdder();
        updateAlbums: while (!registereds.isEmpty()) {
            for (int i = 0; i < registereds.size(); i++) {
                if (i % 1_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateAlbums;
                    }
                }
                MediaFile registered = registereds.get(i);
                MediaFile fetchedFirstChild = updateOrderOfSongs(scanDate, registered);
                MediaFile album = fetchedFirstChild == null ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);
                album.setChildrenLastUpdated(scanDate);
                mediaFileDao.updateMediaFile(album).ifPresent(updated -> {
                    indexManager.index(updated);
                    count.increment();
                });
            }
            registereds = mediaFileDao.getChangedAlbums(ACQUISITION_MAX, folders);
        }
        return count.intValue();
    }

    private int createAlbums(@NonNull Instant scanDate, List<MusicFolder> folders) {
        List<MediaFile> registereds = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        LongAdder count = new LongAdder();
        createAlbums: while (!registereds.isEmpty()) {
            for (int i = 0; i < registereds.size(); i++) {
                if (i % 1_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createAlbums;
                    }
                }
                MediaFile registered = registereds.get(i);
                MediaFile fetchedFirstChild = updateOrderOfSongs(scanDate, registered);
                MediaFile album = fetchedFirstChild == null ? registered
                        : albumOf(scanDate, fetchedFirstChild, registered);
                album.setChildrenLastUpdated(scanDate);
                album.setLastScanned(scanDate);
                mediaFileDao.updateMediaFile(album).ifPresent(updated -> {
                    indexManager.index(updated);
                    count.increment();
                });
            }
            registereds = mediaFileDao.getUnparsedAlbums(ACQUISITION_MAX, folders);
        }
        return count.intValue();
    }

    boolean parseAlbum(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return false;
        }
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        int countUpdate = updateAlbums(scanDate, folders);
        int countNew = createAlbums(scanDate, folders);
        boolean parsed = countUpdate > 0 || countNew > 0;
        String comment = "Update(%d)/New(%d)".formatted(countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.PARSE_ALBUM, comment);
        return parsed;
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
        // Please note that VIDEO is not currently included.
        album.setGenre(mediaFileService.getID3AlbumGenresString(song));
        album.setCreated(song.getChanged());
        album.setMusicBrainzReleaseId(song.getMusicBrainzReleaseId());
        mediaFileService.getParent(song).ifPresent(parent -> album.setCoverArtPath(parent.getCoverArtPathString()));
        album.setLastScanned(scanDate);
        album.setPresent(true);
        return album;
    }

    private Optional<MusicFolder> getMusicFolder(MediaFile mediaFile) {
        return musicFolderService.getAllMusicFolders().stream()
                .filter(f -> f.getPathString().equals(mediaFile.getFolder())).findFirst();
    }

    int updateAlbumId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> songs = mediaFileDao.getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        LongAdder count = new LongAdder();
        updateAlbums: while (!songs.isEmpty()) {
            for (int i = 0; i < songs.size(); i++) {
                if (i % 4_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateAlbums;
                    }
                }
                MediaFile song = songs.get(i);
                Album registered = albumDao.getAlbum(song.getAlbumArtist(), song.getAlbumName());
                getMusicFolder(song).ifPresent(folder -> {
                    Album album = albumId3Of(scanDate, folder.getId(), song, registered);
                    Optional.ofNullable(albumDao.updateAlbum(album)).ifPresent(updated -> {
                        indexManager.index(updated);
                        mediaFileDao.updateChildrenLastUpdated(album, scanDate);
                        count.increment();
                    });
                });
            }
            songs = mediaFileDao.getChangedId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }
        return count.intValue();
    }

    int createAlbumId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> songs = mediaFileDao.getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        LongAdder count = new LongAdder();
        createAlbums: while (!songs.isEmpty()) {
            for (int i = 0; i < songs.size(); i++) {
                if (i % 4_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createAlbums;
                    }
                }
                MediaFile song = songs.get(i);
                getMusicFolder(song).ifPresent(folder -> {
                    Album album = albumId3Of(scanDate, folder.getId(), song, null);
                    Optional.ofNullable(albumDao.createAlbum(album)).ifPresent(created -> {
                        indexManager.index(created);
                        mediaFileDao.updateChildrenLastUpdated(album, scanDate);
                        count.increment();
                    });
                });
            }
            songs = mediaFileDao.getUnregisteredId3Albums(ACQUISITION_MAX, folders, withPodcast);
        }
        return count.intValue();
    }

    boolean refleshAlbumId3(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return false;
        }
        boolean withPodcast = isPodcastInMusicFolders();
        iterateAlbumId3(scanDate, withPodcast);
        int countUpdate = updateAlbumId3s(scanDate, withPodcast);
        int countNew = createAlbumId3s(scanDate, withPodcast);
        String comment = "Update(%d)/New(%d)".formatted(countUpdate, countNew);
        createScanEvent(scanDate, ScanEventType.REFRESH_ALBUM_ID3, comment);
        return countUpdate > 0 || countNew > 0;
    }

    private Artist artistId3Of(@NonNull Instant scanDate, int folderId, @NonNull MediaFile artistId3,
            @Nullable Artist registered) {
        Artist artist = registered == null ? new Artist() : registered;
        artist.setFolderId(folderId);
        artist.setName(artistId3.getAlbumArtist());
        artist.setReading(artistId3.getAlbumArtistReading());
        artist.setSort(artistId3.getAlbumArtistSort());
        artist.setCoverArtPath(artistId3.getCoverArtPathString());
        artist.setLastScanned(scanDate);
        artist.setPresent(true);
        String index = musicIndexService.getParser().getIndex(artist).getIndex();
        artist.setMusicIndex(index);
        return artist;
    }

    @Transactional
    public void iterateArtistId3(@NonNull Instant scanDate, boolean withPodcast) {
        artistDao.iterateLastScanned(scanDate, withPodcast);
        indexManager.expungeArtistId3(artistDao.getExpungeCandidates(scanDate));
        artistDao.expunge(scanDate);
    }

    int updateArtistId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> artistId3s = mediaFileDao.getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);
        LongAdder countUpdate = new LongAdder();
        updateArtists: while (!artistId3s.isEmpty()) {
            for (int i = 0; i < artistId3s.size(); i++) {
                if (i % 15_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break updateArtists;
                    }
                }
                MediaFile artistId3 = artistId3s.get(i);
                getMusicFolder(artistId3).ifPresent(folder -> {
                    Optional.ofNullable(artistDao.updateArtist(artistId3Of(scanDate, folder.getId(), artistId3, null)))
                            .ifPresent(updated -> {
                                indexManager.index(updated, folder);
                                countUpdate.increment();
                            });
                });
            }
            artistId3s = mediaFileDao.getChangedId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }
        return countUpdate.intValue();
    }

    int createArtistId3s(@NonNull Instant scanDate, boolean withPodcast) {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> artistId3s = mediaFileDao.getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);
        LongAdder countNew = new LongAdder();
        createArtists: while (!artistId3s.isEmpty()) {
            for (int i = 0; i < artistId3s.size(); i++) {
                if (i % 15_000 == 0) {
                    repeatWait();
                    if (isInterrupted()) {
                        break createArtists;
                    }
                }
                MediaFile artistId3 = artistId3s.get(i);
                getMusicFolder(artistId3).ifPresent(folder -> {
                    Optional.ofNullable(artistDao.createArtist(artistId3Of(scanDate, folder.getId(), artistId3, null)))
                            .ifPresent(created -> {
                                indexManager.index(created, folder);
                                countNew.increment();
                            });
                });
            }
            artistId3s = mediaFileDao.getUnregisteredId3Artists(ACQUISITION_MAX, folders, withPodcast);
        }
        return countNew.intValue();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (artist) Not reusable
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

    void parsePodcast(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }
        if (settingsService.getPodcastFolder() == null) {
            return;
        }
        Path path = Path.of(settingsService.getPodcastFolder());
        MusicFolder dummy = new MusicFolder(path.toString(), null, true, null, false);
        getRootDirectory(scanDate, path).ifPresent(root -> {
            scanPodcast(scanDate, dummy, root);
            createScanEvent(scanDate, ScanEventType.PARSE_PODCAST, null);
        });
    }

    // TODO To be fixed in v111.7.0 later #1925
    void scanPodcast(@NonNull Instant scanDate, @NonNull MusicFolder folder, @NonNull MediaFile file) {
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

    void updateGenreMaster(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }
        List<Genre> genres = mediaFileDao.getGenreCounts();
        mediaFileDao.updateGenres(genres);
        indexManager.expungeGenreOtherThan(genres);
        createScanEvent(scanDate, ScanEventType.UPDATE_GENRE_MASTER, null);
    }

    private void invokeUpdateIndex(@NonNull Instant scanDate, List<Integer> merged, List<Integer> copied,
            List<Integer> compensated) {
        List<Integer> ids = Stream
                .concat(Stream.concat(merged.stream(), copied.stream()).distinct(), compensated.stream()).distinct()
                .collect(Collectors.toList());
        for (int i = 0; i < ids.size(); i++) {
            MediaFile mediaFile = mediaFileService.getMediaFileStrict(ids.get(i));
            indexManager.index(mediaFile);
            if (mediaFile.getMediaType() == MediaType.ALBUM && FAR_FUTURE.equals(mediaFile.getChildrenLastUpdated())) {
                mediaFileDao.updateChildrenLastUpdated(mediaFile.getPathString(), scanDate);
            }
            if (i % 10_000 == 0) {
                repeatWait();
                if (isInterrupted()) {
                    LOG.warn(
                            "Registration of the search index was interrupted. Rescanning with IgnoreTimestamp enabled is recommended.");
                    break;
                }
            }
        }
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    boolean updateSortOfArtist(@NonNull Instant scanDate) {
        boolean updated = false;
        if (isInterrupted()) {
            return updated;
        }
        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ARTIST, MSG_SKIP);
            return updated;
        }

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        final List<Integer> merged = sortProcedure.mergeSortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        final List<Integer> copied = sortProcedure.copySortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        final List<Integer> compensated = sortProcedure.compensateSortOfArtist(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        updated = !merged.isEmpty() || !copied.isEmpty() || !compensated.isEmpty();
        if (updated) {
            invokeUpdateIndex(scanDate, merged, copied, compensated);
        }

        String comment = "Merged(%d)/Copied(%d)/Compensated(%d)".formatted(merged.size(), copied.size(),
                compensated.size());
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ARTIST, comment);
        return updated;
    }

    /*
     * Add index to Album directly under the directory.
     * https://github.com/tesshucom/jpsonic/pull/2446#discussion_r1403505056
     */
    private void updateIndexOfAlbum() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        if (mediaFileDao.getChildSizeOf(folders, MediaType.ALBUM) == 0) {
            return;
        }
        MediaType[] otherThanAlbum = Stream.of(MediaType.values()).filter(type -> type != MediaType.ALBUM)
                .toArray(i -> new MediaType[i]);
        folders.stream().forEach(folder -> {
            int offset = 0;
            List<MediaFile> albums = mediaFileDao.getChildrenOf(folder.getPathString(), offset, ACQUISITION_MAX,
                    ChildOrder.BY_ALPHA, otherThanAlbum);
            while (!albums.isEmpty()) {
                albums.stream().forEach(album -> {
                    String musicIndex = musicIndexService.getParser().getIndex(album).getIndex();
                    album.setMusicIndex(musicIndex);
                    mediaFileDao.updateMediaFile(album);
                });
                offset += ACQUISITION_MAX;
                albums = mediaFileDao.getChildrenOf(folder.getPathString(), offset, ACQUISITION_MAX,
                        ChildOrder.BY_ALPHA, otherThanAlbum);
            }
        });
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    boolean updateSortOfAlbum(@NonNull Instant scanDate) {
        boolean updated = false;
        if (isInterrupted()) {
            return updated;
        }

        if (!scannerState.isEnableCleansing() || !settingsService.isSortStrict()) {
            updateIndexOfAlbum();
            createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ALBUM, MSG_SKIP);
            return updated;
        }

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        final List<Integer> merged = sortProcedure.mergeSortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        final List<Integer> copied = sortProcedure.copySortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        final List<Integer> compensated = sortProcedure.compensateSortOfAlbum(folders);
        repeatWait();
        if (isInterrupted()) {
            return updated;
        }

        updated = !merged.isEmpty() || !copied.isEmpty() || !compensated.isEmpty();
        if (updated) {
            invokeUpdateIndex(scanDate, merged, copied, compensated);
        }

        updateIndexOfAlbum();

        String comment = "Merged(%d)/Copied(%d)/Compensated(%d)".formatted(merged.size(), copied.size(),
                compensated.size());
        createScanEvent(scanDate, ScanEventType.UPDATE_SORT_OF_ALBUM, comment);
        return updated;
    }

    void updateOrderOfSongsDirectlyUnderMusicfolder(@NonNull Instant scanDate) {
        if (isInterrupted()) {
            return;
        }
        musicFolderService.getAllMusicFolders().forEach(
                folder -> updateOrderOfSongs(scanDate, mediaFileService.getMediaFileStrict(folder.getPathString())));
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_SONG, null);
    }

    void updateOrderOfArtist(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST, MSG_UNNECESSARY);
            return;
        }
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> artists = mediaFileDao.getArtistAll(folders);
        int count = invokeUpdateOrder(artists, comparators.mediaFileOrderByAlpha(), wmfs::updateOrder);
        String comment = "Updated order of (%d) artists".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST, comment);
    }

    void updateOrderOfAlbum(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM, MSG_UNNECESSARY);
            return;
        }
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> albums = mediaFileService.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folders);
        int count = invokeUpdateOrder(albums, comparators.mediaFileOrderByAlpha(), wmfs::updateOrder);
        String comment = "Updated order of (%d) albums".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM, comment);
    }

    void updateOrderOfArtistId3(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, MSG_UNNECESSARY);
            return;
        }
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        int count = invokeUpdateOrder(artists, comparators.artistOrderByAlpha(),
                (artist) -> artistDao.updateOrder(artist.getId(), artist.getOrder()));
        String comment = "Updated order of (%d) ID3 artists.".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ARTIST_ID3, comment);
    }

    void updateOrderOfAlbumId3(@NonNull Instant scanDate, boolean skippable) {
        if (isInterrupted()) {
            return;
        }
        if (skippable) {
            createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, MSG_UNNECESSARY);
            return;
        }
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, folders);
        int count = invokeUpdateOrder(albums, comparators.albumOrderByAlpha(),
                (album) -> albumDao.updateOrder(album.getId(), album.getOrder()));
        String comment = "Updated order of (%d) ID3 albums.".formatted(count);
        createScanEvent(scanDate, ScanEventType.UPDATE_ORDER_OF_ALBUM_ID3, comment);
    }

    void runStats(@NonNull Instant scanDate) {
        writeInfo("Collecting media library statistics ...");
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        for (int i = 0; i < folders.size(); i++) {
            if (i % 4 == 0) {
                repeatWait();
                if (isInterrupted()) {
                    return;
                }
            }
            MediaLibraryStatistics stats = staticsDao.gatherMediaLibraryStatistics(scanDate, folders.get(i));
            staticsDao.createMediaLibraryStatistics(stats);
        }
        createScanEvent(scanDate, ScanEventType.RUN_STATS, null);
    }

    void afterScan(@NonNull Instant scanDate) {
        mediaFileCache.setEnabled(true);
        indexManager.stopIndexing();
        searchServiceUtilities.removeCacheAll();
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

    public Optional<ScanPhaseInfo> getScanPhaseInfo() {
        if (!scannerState.isScanning()) {
            return Optional.empty();
        }

        ScanEventType lastEvent = scannerState.getLastEvent();
        if (lastEvent == ScanEventType.SCANNED_COUNT) {
            lastEvent = ScanEventType.MUSIC_FOLDER_CHECK;
        }

        int lastPhase = SCAN_PHASE_ALL.indexOf(lastEvent);
        if (lastPhase == -1) {
            return Optional.of(new ScanPhaseInfo(-1, -1, "Semi Scan Proc", -1));
        }

        int currentPhase = lastPhase + 1 >= SCAN_PHASE_ALL.size() ? lastPhase : lastPhase + 1;

        return Optional.of(new ScanPhaseInfo(currentPhase, SCAN_PHASE_ALL.size(),
                SCAN_PHASE_ALL.get(currentPhase).name(), scanExecutor.getActiveCount()));
    }

    public record ScanPhaseInfo(int phase, int phaseMax, String phaseName, int thread) {
    }
}
