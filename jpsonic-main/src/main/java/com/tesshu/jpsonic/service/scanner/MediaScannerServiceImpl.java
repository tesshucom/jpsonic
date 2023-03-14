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
import java.util.List;
import java.util.Optional;

import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.MediaScannerService;
import org.apache.commons.lang3.mutable.MutableBoolean;
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
    private final StaticsDao staticsDao;
    private final ThreadPoolTaskExecutor scanExecutor;

    private final Object cancelLock = new Object();

    public MediaScannerServiceImpl(ScannerStateServiceImpl scannerState, ScannerProcedureService procedure,
            ExpungeService expungeService, StaticsDao staticsDao, ThreadPoolTaskExecutor scanExecutor) {
        super();
        this.scannerState = scannerState;
        this.procedure = procedure;
        this.expungeService = expungeService;
        this.staticsDao = staticsDao;
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
    public Optional<ScanEventType> getLastScanEventType() {
        if (isScanning() || neverScanned()) {
            return Optional.empty();
        }
        List<ScanEvent> scanEvent = staticsDao.getLastScanAllStatuses();
        if (scanEvent.isEmpty()) {
            return Optional.of(ScanEventType.FAILED);
        }
        return Optional.of(scanEvent.get(0).getType());
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

        MutableBoolean lastScanFailed = new MutableBoolean(false);
        getLastScanEventType().ifPresent(type -> lastScanFailed.setValue(type != ScanEventType.FINISHED));

        boolean parsedAlbum = procedure.parseAlbum(scanDate);
        boolean updatedSortOfAlbum = procedure.updateSortOfAlbum(scanDate);
        boolean toBeSorted = lastScanFailed.getValue() || parsedAlbum || updatedSortOfAlbum;
        procedure.updateOrderOfAlbum(scanDate, toBeSorted);
        boolean updatedSortOfArtist = procedure.updateSortOfArtist(scanDate);
        toBeSorted = lastScanFailed.getValue() || parsedAlbum || updatedSortOfArtist;
        procedure.updateOrderOfArtist(scanDate, toBeSorted);

        if (LOG.isInfoEnabled()) {
            LOG.info("Scanned media library with " + scannerState.getScanCount() + " entries.");
        }

        boolean refleshedAlbumId3 = procedure.refleshAlbumId3(scanDate);
        toBeSorted = lastScanFailed.getValue() || refleshedAlbumId3;
        procedure.updateOrderOfAlbumId3(scanDate, toBeSorted);

        boolean refleshedArtistId3 = procedure.refleshArtistId3(scanDate);
        toBeSorted = lastScanFailed.getValue() || refleshedArtistId3;
        procedure.updateOrderOfArtistId3(scanDate, toBeSorted);

        boolean toBeCounted = lastScanFailed.getValue() || refleshedAlbumId3 || refleshedArtistId3;
        procedure.updateAlbumCounts(scanDate, toBeCounted);

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
