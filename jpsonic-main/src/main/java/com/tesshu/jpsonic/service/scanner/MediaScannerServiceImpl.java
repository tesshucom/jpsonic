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
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.domain.ScanLog.ScanLogType;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Class where the main flow of scanning is handled. Thread control for scanning
 * is done only in this class.
 */
@Service("mediaScannerService")
@DependsOn("scanExecutor")
public class MediaScannerServiceImpl implements MediaScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    private final SettingsService settingsService;
    private final ScannerStateServiceImpl scannerState;
    private final ScannerProcedureService procedure;
    private final ExpungeService expungeService;
    private final StaticsDao staticsDao;
    private final ThreadPoolTaskExecutor scanExecutor;

    private final ReentrantLock cancelLock = new ReentrantLock();

    public MediaScannerServiceImpl(SettingsService settingsService,
            ScannerStateServiceImpl scannerState, ScannerProcedureService procedure,
            ExpungeService expungeService, StaticsDao staticsDao,
            @Qualifier("scanExecutor") ThreadPoolTaskExecutor scanExecutor) {
        super();
        this.settingsService = settingsService;
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
        cancelLock.lock();
        try {
            if (isScanning()) {
                procedure.setCancel(true);
            }
        } finally {
            cancelLock.unlock();
        }
    }

    @Override
    public long getScanCount() {
        return scannerState.getScanCount();
    }

    @Override
    public Optional<ScanEventType> getLastScanEventType() {
        return getLastScanEventType(true);
    }

    private Optional<ScanEventType> getLastScanEventType(boolean scanningCheck) {
        if (scanningCheck && isScanning() || neverScanned()) {
            return Optional.empty();
        }
        List<ScanEvent> scanEvents = staticsDao.getLastScanAllStatuses();
        if (scanEvents.isEmpty()) {
            return Optional.of(ScanEventType.FAILED);
        }
        return Optional.of(scanEvents.get(0).getType());
    }

    boolean isOptionalProcessSkippable() {
        MutableBoolean skippable = new MutableBoolean(!settingsService.isIgnoreFileTimestamps());
        if (skippable.isTrue()) {
            getLastScanEventType(false)
                .ifPresentOrElse(
                        type -> skippable.setValue(ScanEventType.SUCCESS.compareTo(type) == 0),
                        () -> skippable.setValue(false));
        }
        if (skippable.isTrue()) {
            skippable.setValue(!staticsDao.isfolderChangedSinceLastScan());
        }
        return skippable.booleanValue();
    }

    @Override
    public void expunge() {
        expungeService.expunge();
    }

    @Override
    public void scanLibrary() {
        scanExecutor.execute(this::doScanLibrary);
    }

    void doScanLibrary() {

        if (!scannerState.tryScanningLock()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup/Scan/Podcast Download is already running.");
            }
            return;
        }

        LOG.info("Starting to scan media library.");
        Instant scanDate = scannerState.getScanDate();

        boolean skippable = isOptionalProcessSkippable();
        procedure.createScanLog(scanDate, ScanLogType.SCAN_ALL);

        procedure.beforeScan(scanDate);
        procedure.checkMudicFolders(scanDate);

        procedure.parseFileStructure(scanDate);
        procedure.parseVideo(scanDate);
        procedure.parsePodcast(scanDate);
        procedure.iterateFileStructure(scanDate);

        boolean parsedAlbum = procedure.parseAlbum(scanDate);
        boolean updatedSortOfAlbum = procedure.updateSortOfAlbum(scanDate);
        procedure.updateOrderOfAlbum(scanDate, skippable && !parsedAlbum && !updatedSortOfAlbum);
        boolean updatedSortOfArtist = procedure.updateSortOfArtist(scanDate);
        procedure.updateOrderOfArtist(scanDate, skippable && !parsedAlbum && !updatedSortOfArtist);
        procedure.updateOrderOfSongsDirectlyUnderMusicfolder(scanDate);

        if (LOG.isInfoEnabled()) {
            LOG.info("Scanned media library with " + scannerState.getScanCount() + " entries.");
        }

        boolean refleshedAlbumId3 = procedure.refleshAlbumId3(scanDate);
        procedure.updateOrderOfAlbumId3(scanDate, skippable && !refleshedAlbumId3);

        boolean refleshedArtistId3 = procedure.refleshArtistId3(scanDate);
        procedure.updateOrderOfArtistId3(scanDate, skippable && !refleshedArtistId3);

        procedure
            .updateAlbumCounts(scanDate, skippable && !refleshedAlbumId3 && !refleshedArtistId3);

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
            procedure.success(scanDate);
        }

        procedure.rotateScanLog();

        cancelLock.lock();
        try {
            scannerState.unlockScanning();
            procedure.setCancel(false);
        } finally {
            cancelLock.unlock();
        }
    }
}
