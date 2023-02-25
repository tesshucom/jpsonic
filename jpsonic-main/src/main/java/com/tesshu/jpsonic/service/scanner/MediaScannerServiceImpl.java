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

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaScannerService;
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

    private final ScannerStateServiceImpl scannerState;
    private final ScannerProcedureService procedure;
    private final ExpungeService expungeService;
    private final ThreadPoolTaskExecutor scanExecutor;

    public MediaScannerServiceImpl(ScannerStateServiceImpl scannerState, ScannerProcedureService procedure,
            ExpungeService expungeService, ThreadPoolTaskExecutor scanExecutor) {
        super();
        this.scannerState = scannerState;
        this.procedure = procedure;
        this.expungeService = expungeService;
        this.scanExecutor = scanExecutor;
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
    public void scanLibrary() {
        scanExecutor.execute(this::doScanLibrary);
    }

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

            procedure.parseAudio(scanDate); // Split into Audio, Album and Movie #1925

            procedure.parsePodcast(scanDate);

            if (LOG.isInfoEnabled()) {
                LOG.info("Scanned media library with " + scannerState.getScanCount() + " entries.");
            }

            procedure.markNonPresent(scanDate);

            procedure.updateAlbumCounts(scanDate);

            procedure.updateGenreMaster(scanDate);

            procedure.doCleansingProcess(scanDate); // Move before parseAlbum #1925

            procedure.runStats(scanDate);

        } catch (ExecutionException e) {
            scannerState.unlockScanning();
            ConcurrentUtils.handleCauseUnchecked(e);
            if (scannerState.isDestroy()) {
                LOG.info("Interrupted to scan media library.");
                procedure.createScanEvent(scanDate, ScanEventType.DESTROYED, null);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to scan media library.", e);
                procedure.createScanEvent(scanDate, ScanEventType.FAILED, null);
            }
        } finally {
            procedure.afterScan(scanDate);
        }

        if (!scannerState.isDestroy()) {
            procedure.importPlaylists(scanDate);
            procedure.checkpoint(scanDate);
            LOG.info("Completed media library scan.");
            procedure.createScanEvent(scanDate, ScanEventType.FINISHED, null);
            procedure.rotateScanLog();
            scannerState.unlockScanning();
        }
    }
}