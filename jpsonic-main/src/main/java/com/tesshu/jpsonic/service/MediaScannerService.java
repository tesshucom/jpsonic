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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Genres;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.search.IndexManager;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import net.sf.ehcache.Ehcache;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
@Service
@DependsOn("scanExecutor")
public class MediaScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);
    private static final AtomicBoolean IS_EXPUNGING = new AtomicBoolean();
    private static final AtomicBoolean IS_SCANNING = new AtomicBoolean();
    private static final Object SCAN_LOCK = new Object();

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final IndexManager indexManager;
    private final PlaylistService playlistService;
    private final MediaFileService mediaFileService;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final Ehcache indexCache;
    private final MediaScannerServiceUtils utils;
    private final ThreadPoolTaskExecutor scanExecutor;

    private AtomicBoolean cleansingProcess = new AtomicBoolean();
    private AtomicInteger scanCount = new AtomicInteger();
    private AtomicBoolean destroy = new AtomicBoolean();

    public MediaScannerService(SettingsService settingsService, MusicFolderService musicFolderService,
            IndexManager indexManager, PlaylistService playlistService, MediaFileService mediaFileService,
            MediaFileDao mediaFileDao, ArtistDao artistDao, AlbumDao albumDao, Ehcache indexCache,
            MediaScannerServiceUtils utils, ThreadPoolTaskExecutor scanExecutor) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.indexManager = indexManager;
        this.playlistService = playlistService;
        this.mediaFileService = mediaFileService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.indexCache = indexCache;
        this.utils = utils;
        this.scanExecutor = scanExecutor;
    }

    @PostConstruct
    public void init() {
        cleansingProcess.set(true);
        destroy.set(false);
        indexManager.deleteOldIndexFiles();
        indexManager.initializeIndexDirectory();
    }

    @PreDestroy
    public void onDestroy() {
        destroy.set(true);
    }

    public void initNoSchedule() {
        indexManager.deleteOldIndexFiles();
    }

    public boolean neverScanned() {
        return indexManager.getStatistics() == null;
    }

    /**
     * Returns whether the media library is currently being scanned.
     */
    public boolean isScanning() {
        return IS_SCANNING.get();
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount.get();
    }

    /**
     * Scans the media library. The scanning is done asynchronously, i.e., this method returns immediately.
     */
    @SuppressWarnings("PMD.AccessorMethodGeneration") // Triaged in #833 or #834
    public void scanLibrary() {
        if (isScanning() || IS_EXPUNGING.get()) {
            return;
        }
        synchronized (SCAN_LOCK) {
            IS_SCANNING.set(true);
            scanExecutor.execute(this::doScanLibrary);
        }
    }

    private void beforeScan() {
        if (settingsService.isIgnoreFileTimestampsNext()) {
            mediaFileDao.resetLastScanned();
            settingsService.setIgnoreFileTimestampsNext(false);
            settingsService.save();
        }
    }

    private void doScanLibrary() {

        beforeScan();

        LOG.info("Starting to scan media library.");

        MediaLibraryStatistics statistics = new MediaLibraryStatistics(DateUtils.truncate(new Date(), Calendar.SECOND));
        if (LOG.isDebugEnabled()) {
            LOG.debug("New last scan date is " + statistics.getScanDate());
        }

        try {

            // init
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();
            Genres genres = new Genres();
            scanCount.set(0);
            utils.clearOrder();
            indexCache.removeAll();
            mediaFileService.setMemoryCacheEnabled(false);
            indexManager.startIndexing();
            mediaFileService.clearMemoryCache();

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders()) {
                MediaFile root = mediaFileService.getMediaFile(musicFolder.getPath(), false);
                scanFile(root, musicFolder, statistics, albumCount, genres, false);
            }

            // Scan podcast folder.
            File podcastFolder = new File(settingsService.getPodcastFolder());
            if (podcastFolder.exists()) {
                scanFile(mediaFileService.getMediaFile(podcastFolder), new MusicFolder(podcastFolder, null, true, null),
                        statistics, albumCount, genres, true);
            }

            writeInfo("Scanned media library with " + scanCount + " entries.");
            writeInfo("Marking non-present files.");
            mediaFileDao.markNonPresent(statistics.getScanDate());
            writeInfo("Marking non-present artists.");
            artistDao.markNonPresent(statistics.getScanDate());
            writeInfo("Marking non-present albums.");
            albumDao.markNonPresent(statistics.getScanDate());

            // Update statistics
            statistics.incrementArtists(albumCount.size());
            for (Integer albums : albumCount.values()) {
                statistics.incrementAlbums(albums);
            }

            // Update genres
            mediaFileDao.updateGenres(genres.getGenres());

            docleansingProcess();

            LOG.info("Completed media library scan.");

        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (destroy.get()) {
                writeInfo("Interrupted to scan media library.");
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to scan media library.", e);
            }
        } finally {
            mediaFileService.setMemoryCacheEnabled(true);
            indexManager.stopIndexing(statistics);
            IS_SCANNING.set(false);
            utils.clearMemoryCache();
        }

        if (!destroy.get()) {
            playlistService.importPlaylists();
            mediaFileDao.checkpoint();
        }
    }

    private void docleansingProcess() {
        if (!destroy.get() && cleansingProcess.get()) {
            writeInfo("[1/2] Additional processing after scanning by Jpsonic. Supplementing sort/read data.");
            utils.updateSortOfArtist();
            utils.updateSortOfAlbum();
            writeInfo("[1/2] Done.");
            if (settingsService.isSortStrict()) {
                writeInfo(
                        "[2/2] Additional processing after scanning by Jpsonic. Create dictionary sort index in database.");
                utils.updateOrderOfAll();
            } else {
                writeInfo(
                        "[2/2] A dictionary sort index is not created in the database. See Settings > General > Sort settings.");
            }
            writeInfo("[2/2] Done.");
        }
    }

    private void writeInfo(String msg) {
        if (settingsService.isVerboseLogScanning() && LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    private void scanFile(MediaFile file, MusicFolder musicFolder, MediaLibraryStatistics statistics,
            Map<String, Integer> albumCount, Genres genres, boolean isPodcast) throws ExecutionException {

        interruptIfDestroyed();
        scanCount.incrementAndGet();
        writeScanLog(file);

        // Update the root folder if it has changed.
        if (!musicFolder.getPath().getPath().equals(file.getFolder())) {
            file.setFolder(musicFolder.getPath().getPath());
            mediaFileDao.createOrUpdateMediaFile(file);
        }

        indexManager.index(file);

        if (file.isDirectory()) {
            for (MediaFile child : mediaFileService.getChildrenOf(file, true, false, false, false)) {
                scanFile(child, musicFolder, statistics, albumCount, genres, isPodcast);
            }
            for (MediaFile child : mediaFileService.getChildrenOf(file, false, true, false, false)) {
                scanFile(child, musicFolder, statistics, albumCount, genres, isPodcast);
            }
        } else {
            if (!isPodcast) {
                updateAlbum(file, musicFolder, statistics.getScanDate(), albumCount);
                updateArtist(file, musicFolder, statistics.getScanDate(), albumCount);
            }
            statistics.incrementSongs(1);
        }

        updateGenres(file, genres);
        mediaFileDao.markPresent(file.getPath(), statistics.getScanDate());
        artistDao.markPresent(file.getAlbumArtist(), statistics.getScanDate());

        if (file.getDurationSeconds() != null) {
            statistics.incrementTotalDurationInSeconds(file.getDurationSeconds());
        }
        if (file.getFileSize() != null) {
            statistics.incrementTotalLengthInBytes(file.getFileSize());
        }
    }

    private void interruptIfDestroyed() throws ExecutionException {
        if (destroy.get()) {
            throw new ExecutionException(new InterruptedException("The scan was stopped due to the shutdown."));
        }
    }

    private void writeScanLog(MediaFile file) {
        if (LOG.isInfoEnabled() && scanCount.get() % 250 == 0) {
            writeInfo("Scanned media library with " + scanCount + " entries.");
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Scanning file {}", file.getPath());
        }
    }

    private void updateGenres(MediaFile file, Genres genres) {
        String genre = file.getGenre();
        if (genre == null) {
            return;
        }
        if (file.isAlbum()) {
            genres.incrementAlbumCount(genre);
        } else if (file.isAudio()) {
            genres.incrementSongCount(genre);
        }
    }

    private void updateAlbum(MediaFile file, MusicFolder musicFolder, Date lastScanned,
            Map<String, Integer> albumCount) {

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
            Integer n = albumCount.get(artist);
            albumCount.put(artist, n == null ? 1 : n + 1);
            mergeOnFirstEncount(album, file, musicFolder);
        }

        if (file.getDurationSeconds() != null) {
            album.setDurationSeconds(album.getDurationSeconds() + file.getDurationSeconds());
        }
        if (file.isAudio()) {
            album.setSongCount(album.getSongCount() + 1);
        }
        album.setLastScanned(lastScanned);
        album.setPresent(true);

        albumDao.createOrUpdateAlbum(album);
        if (firstEncounter) {
            indexManager.index(album);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            file.setAlbumArtistReading(album.getArtistReading());
            file.setAlbumArtistSort(album.getArtistSort());
            mediaFileDao.createOrUpdateMediaFile(file);
        }
    }

    private void mergeOnFirstEncount(Album album, MediaFile file, MusicFolder musicFolder) {
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

    private boolean isNotAlbumUpdatable(MediaFile file) {
        boolean isNotAlbumUpdatable = false;
        if (file.getAlbumName() == null || file.getParentPath() == null || !file.isAudio()
                || file.getArtist() == null && file.getAlbumArtist() == null) {
            isNotAlbumUpdatable = true;
        }
        return isNotAlbumUpdatable;
    }

    private Album getMergedAlbum(MediaFile file, String artist, String artistReading, String artistSort) {
        Album album = albumDao.getAlbumForFile(file);
        if (album == null) {
            album = new Album();
            album.setPath(file.getParentPath());
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
        if (parent != null && parent.getCoverArtPath() != null) {
            album.setCoverArtPath(parent.getCoverArtPath());
        }
        return album;
    }

    private void updateArtist(MediaFile file, MusicFolder musicFolder, Date lastScanned,
            Map<String, Integer> albumCount) {
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
                artist.setCoverArtPath(parent.getCoverArtPath());
            }
        }
        boolean firstEncounter = !lastScanned.equals(artist.getLastScanned());

        if (firstEncounter) {
            artist.setFolderId(musicFolder.getId());
        }
        Integer n = albumCount.get(artist.getName());
        artist.setAlbumCount(n == null ? 0 : n);

        artist.setLastScanned(lastScanned);
        artist.setPresent(true);
        artistDao.createOrUpdateArtist(artist);

        if (firstEncounter) {
            indexManager.index(artist, musicFolder);
        }
    }

    public boolean isJpsonicCleansingProcess() {
        return cleansingProcess.get();
    }

    public void setJpsonicCleansingProcess(boolean b) {
        this.cleansingProcess.set(b);
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    public void expunge() {

        if (IS_EXPUNGING.get()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup is already running.");
            }
            return;
        }
        IS_EXPUNGING.set(true);

        MediaLibraryStatistics statistics = indexManager.getStatistics();

        /*
         * indexManager#expunge depends on DB delete flag. For consistency, clean of DB and Lucene must run in one
         * block.
         *
         * Lucene's writing is exclusive. This process cannot be performed while during scan or scan has never been
         * performed.
         */
        if (statistics != null && !isScanning()) {

            // to be before dao#expunge
            indexManager.startIndexing();
            indexManager.expunge();
            indexManager.stopIndexing(statistics);

            // to be after indexManager#expunge
            artistDao.expunge();
            albumDao.expunge();
            mediaFileDao.expunge();

            mediaFileDao.checkpoint();

        } else {
            LOG.warn(
                    "Index hasn't been created yet or during scanning. Plese execute clean up after scan is completed.");
        }

        IS_EXPUNGING.set(false);
    }
}
