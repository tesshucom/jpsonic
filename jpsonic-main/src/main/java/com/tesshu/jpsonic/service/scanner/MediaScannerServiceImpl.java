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
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SettingsService;
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
    private final WritableMediaFileService writableMediaFileService;
    private final ThreadPoolTaskExecutor scanExecutor;

    private final ScannerStateServiceImpl scannerState;
    private final ScannerProcedureService procedure;
    private final ExpungeService expungeService;

    public MediaScannerServiceImpl(SettingsService settingsService, MusicFolderService musicFolderService,
            WritableMediaFileService writableMediaFileService, ThreadPoolTaskExecutor scanExecutor,
            ScannerStateServiceImpl scannerStateService, ScannerProcedureService procedure,
            ExpungeService expungeService) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.writableMediaFileService = writableMediaFileService;
        this.scanExecutor = scanExecutor;
        this.scannerState = scannerStateService;
        this.procedure = procedure;
        this.expungeService = expungeService;
    }

    private void writeInfo(String msg) {
        if (LOG.isInfoEnabled()) {
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
        Instant scanDate = scannerState.getScanDate();
        procedure.createScanLog(scanDate, ScanLogType.SCAN_ALL);
        procedure.beforeScan(scanDate);

        try {

            // Recurse through all files on disk.
            for (MusicFolder musicFolder : musicFolderService.getAllMusicFolders()) {
                MediaFile root = writableMediaFileService.getMediaFile(musicFolder.toPath(), scanDate);
                procedure.scanFile(root, musicFolder, scanDate, false);
            }

            // Scan podcast folder.
            if (settingsService.getPodcastFolder() != null) {
                Path podcastFolder = Path.of(settingsService.getPodcastFolder());
                if (Files.exists(podcastFolder)) {
                    procedure.scanFile(writableMediaFileService.getMediaFile(podcastFolder, scanDate),
                            new MusicFolder(podcastFolder.toString(), null, true, null), scanDate, true);
                }
            }

            writeInfo("Scanned media library with " + scannerState.getScanCount() + " entries.");

            procedure.markNonPresent(scanDate);

            procedure.updateAlbumCounts();

            procedure.updateGenreMaster();

            procedure.doCleansingProcess();

            procedure.runStats(scanDate);

        } catch (ExecutionException e) {
            scannerState.unlockScanning();
            ConcurrentUtils.handleCauseUnchecked(e);
            if (scannerState.isDestroy()) {
                writeInfo("Interrupted to scan media library.");
                procedure.createScanEvent(scanDate, ScanEventType.DESTROYED, null);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to scan media library.", e);
                procedure.createScanEvent(scanDate, ScanEventType.FAILED, null);
            }
        } finally {
            procedure.afterScan();
        }

        // Launch another process after Scan.
        if (!scannerState.isDestroy()) {
            procedure.importPlaylists();
            procedure.checkpoint();

            LOG.info("Completed media library scan.");
            procedure.createScanEvent(scanDate, ScanEventType.FINISHED, null);
            procedure.rotateScanLog();
            scannerState.unlockScanning();
        }
    }
}
