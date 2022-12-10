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

package com.tesshu.jpsonic.service.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Genres;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.IndexManager;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Class where the main flow of scanning is handled. Thread control for scanning is done only in this class.
 */
@Service("mediaScannerService")
@DependsOn("scanExecutor")
public class MediaScannerServiceImpl implements MediaScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final IndexManager indexManager;
    private final PlaylistService playlistService;
    private final WritableMediaFileService writableMediaFileService;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final ThreadPoolTaskExecutor scanExecutor;

    private final ScannerStateServiceImpl scannerState;
    private final ScannerProcedureService procedure;
    private final ExpungeService expungeService;

    public MediaScannerServiceImpl(SettingsService settingsService, MusicFolderService musicFolderService,
            IndexManager indexManager, PlaylistService playlistService,
            WritableMediaFileService writableMediaFileService, MediaFileDao mediaFileDao, ArtistDao artistDao,
            AlbumDao albumDao, ThreadPoolTaskExecutor scanExecutor, ScannerStateServiceImpl scannerStateService,
            ScannerProcedureService procedure, ExpungeService expungeService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.indexManager = indexManager;
        this.playlistService = playlistService;
        this.writableMediaFileService = writableMediaFileService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.scanExecutor = scanExecutor;
        this.scannerState = scannerStateService;
        this.procedure = procedure;
        this.expungeService = expungeService;
    }

    // TODO To be fixed in v111.6.0
    @PostConstruct
    public void init() {
        indexManager.deleteOldIndexFiles();
        indexManager.initializeIndexDirectory();
    }

    @PreDestroy
    void preDestroy() {
        scannerState.setDestroy(true);
    }

    private void writeInfo(String msg) {
        if (settingsService.isVerboseLogScanning() && LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    @Override
    public boolean neverScanned() {
        return scannerState.neverScanned();
    }

    @Override
    public boolean isScanning() {
        return scannerState.isScanning();
    }

    @Override
    public long getScanCount() {
        return scannerState.getScanCount();
    }

    // TODO To be fixed in v111.6.0
    @Override
    public void expunge() {
        expungeService.expunge();
    }

    @Override
    @SuppressWarnings("PMD.AccessorMethodGeneration") // Triaged in #833 or #834
    public void scanLibrary() {
        scanExecutor.execute(this::doScanLibrary);
    }

    // TODO To be fixed in v111.6.0
    private void doScanLibrary() {

        if (!scannerState.tryScanningLock()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup/Scan/Podcast Download is already running.");
            }
            return;
        }

        LOG.info("Starting to scan media library.");

        procedure.beforeScan();

        MediaLibraryStatistics stats = writableMediaFileService.newStatistics();
        if (LOG.isDebugEnabled()) {
            LOG.debug("New last scan date is " + stats.getScanDate());
        }

        try {

            // init
            Map<String, Integer> albumCount = new ConcurrentHashMap<>();
            Genres genres = new Genres();

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders()) {
                MediaFile root = writableMediaFileService.getMediaFile(musicFolder.toPath(), stats);
                procedure.scanFile(root, musicFolder, stats, albumCount, genres, false);
            }

            // Scan podcast folder.
            if (settingsService.getPodcastFolder() != null) {
                Path podcastFolder = Path.of(settingsService.getPodcastFolder());
                if (Files.exists(podcastFolder)) {
                    procedure.scanFile(writableMediaFileService.getMediaFile(podcastFolder, stats),
                            new MusicFolder(podcastFolder.toString(), null, true, null), stats, albumCount, genres,
                            true);
                }
            }

            writeInfo("Scanned media library with " + scannerState.getScanCount() + " entries.");
            writeInfo("Marking non-present files.");
            mediaFileDao.markNonPresent(stats.getScanDate());
            writeInfo("Marking non-present artists.");
            artistDao.markNonPresent(stats.getScanDate());
            writeInfo("Marking non-present albums.");
            albumDao.markNonPresent(stats.getScanDate());

            // Update statistics
            stats.incrementArtists(albumCount.size());
            for (Integer albums : albumCount.values()) {
                stats.incrementAlbums(albums);
            }

            // Update genres
            mediaFileDao.updateGenres(genres.getGenres());

            procedure.doCleansingProcess();

            LOG.info("Completed media library scan.");

        } catch (ExecutionException e) {
            scannerState.unlockScanning();
            ConcurrentUtils.handleCauseUnchecked(e);
            if (scannerState.isDestroy()) {
                writeInfo("Interrupted to scan media library.");
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to scan media library.", e);
            }
        } finally {
            procedure.afterScan(stats);
        }

        // Launch another process after Scan.
        if (!scannerState.isDestroy()) {
            playlistService.importPlaylists();
            procedure.checkpoint();
        }

        scannerState.unlockScanning();
    }
}
