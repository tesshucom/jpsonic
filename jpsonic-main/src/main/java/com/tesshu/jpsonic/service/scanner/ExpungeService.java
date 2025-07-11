package com.tesshu.jpsonic.service.scanner;

import java.time.Instant;

import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.domain.ScanLog.ScanLogType;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Class for doing physical deletion.
 */
@Service
public class ExpungeService {

    private static final Logger LOG = LoggerFactory.getLogger(ExpungeService.class);

    private final ScannerStateServiceImpl scannerState;
    private final IndexManager indexManager;
    private final TemplateWrapper template;
    private final RatingDao ratingDao;
    private final ScannerProcedureService procedure;

    public ExpungeService(ScannerStateServiceImpl scannerStateService, IndexManager indexManager,
            TemplateWrapper template, RatingDao ratingDao,
            ScannerProcedureService scannerProcedure) {
        super();
        this.scannerState = scannerStateService;
        this.indexManager = indexManager;
        this.template = template;
        this.ratingDao = ratingDao;
        this.procedure = scannerProcedure;
    }

    void expunge() {

        if (!scannerState.tryScanningLock()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup/Scan/Podcast Download is already running.");
            }
            return;
        }
        Instant scanDate = scannerState.getScanDate();
        procedure.createScanLog(scanDate, ScanLogType.EXPUNGE);

        if (scannerState.neverScanned()) {
            LOG.warn("The scan has never completed yet. No cleanup is performed.");
        } else {

            indexManager.startIndexing();
            procedure.iterateAlbumId3(scanDate, procedure.isPodcastInMusicFolders());
            procedure.iterateArtistId3(scanDate, procedure.isPodcastInMusicFolders());
            procedure.expungeFileStructure();
            indexManager.stopIndexing();

            template.checkpoint();

            // to be after expungeFileStructure
            ratingDao.expunge();
        }

        procedure.createScanEvent(scanDate, ScanEventType.SUCCESS, null);
        procedure.rotateScanLog();
        scannerState.unlockScanning();
    }
}
