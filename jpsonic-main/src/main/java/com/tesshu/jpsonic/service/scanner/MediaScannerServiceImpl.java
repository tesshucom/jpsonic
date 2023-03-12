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

import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaScannerService;
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

    private final Object cancelLock = new Object();

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
    public boolean isCancel() {
        return procedure.isCancel();
    }

    @Override
    public void tryCancel() {
        synchronized (cancelLock) {
            if (isScanning()) {
                procedure.setCancel(true);
            }
        }
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

        procedure.checkMudicFolders(scanDate);

        procedure.parseFileStructure(scanDate);
        procedure.parseVideo(scanDate);
        procedure.parsePodcast(scanDate);
        procedure.iterateFileStructure(scanDate);

        boolean parsedAlbum = procedure.parseAlbum(scanDate);
        boolean updatedSortOfAlbum = procedure.updateSortOfAlbum(scanDate);
        procedure.updateOrderOfAlbum(scanDate, parsedAlbum || updatedSortOfAlbum);
        boolean updatedSortOfArtist = procedure.updateSortOfArtist(scanDate);
        procedure.updateOrderOfArtist(scanDate, parsedAlbum || updatedSortOfArtist);

        if (LOG.isInfoEnabled()) {
            LOG.info("Scanned media library with " + scannerState.getScanCount() + " entries.");
        }

        boolean refleshedAlbumId3 = procedure.refleshAlbumId3(scanDate);
        procedure.updateOrderOfAlbumId3(scanDate, refleshedAlbumId3);
        boolean refleshedArtistId3 = procedure.refleshArtistId3(scanDate);
        procedure.updateOrderOfArtistId3(scanDate, refleshedArtistId3);

        procedure.updateAlbumCounts(scanDate);
        procedure.updateGenreMaster(scanDate);

        procedure.runStats(scanDate);

        procedure.afterScan(scanDate);

        if (scannerState.isDestroy()) {
            LOG.warn("The scan was stopped due to the shutdown.");
            procedure.createScanEvent(scanDate, ScanEventType.DESTROYED, null);
            return;
        } else if (isCancel()) {
            LOG.warn("The scan was stopped due to cancellation.");
            procedure.createScanEvent(scanDate, ScanEventType.CANCELED, null);
        } else {
            procedure.importPlaylists(scanDate);
            procedure.checkpoint(scanDate);
            LOG.info("Completed media library scan.");
            procedure.createScanEvent(scanDate, ScanEventType.FINISHED, null);
        }

        procedure.rotateScanLog();

        synchronized (cancelLock) {
            scannerState.unlockScanning();
            procedure.setCancel(false);
        }
    }
}
