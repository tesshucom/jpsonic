package com.tesshu.jpsonic.service.scanner;

import java.time.Instant;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.dao.StaticsDao.ScanLogType;
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
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final MediaFileDao mediaFileDao;
    private final RatingDao ratingDao;
    private final ScannerProcedureService procedure;

    public ExpungeService(ScannerStateServiceImpl scannerState, IndexManager indexManager, ArtistDao artistDao,
            AlbumDao albumDao, MediaFileDao mediaFileDao, RatingDao ratingDao,
            ScannerProcedureService scannerProcedure) {
        super();
        this.scannerState = scannerState;
        this.indexManager = indexManager;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.mediaFileDao = mediaFileDao;
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
            // to be before dao#expunge
            indexManager.startIndexing();
            indexManager.expunge();
            indexManager.stopIndexing();

            // to be after indexManager#expunge
            artistDao.expunge();
            albumDao.expunge();
            mediaFileDao.expunge();
            mediaFileDao.checkpoint();

            // to be after mediaFileDao#expunge
            ratingDao.expunge();
        }

        procedure.rotateScanLog();
        scannerState.unlockScanning();
    }
}
