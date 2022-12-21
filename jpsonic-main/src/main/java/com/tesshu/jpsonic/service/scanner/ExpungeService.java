package com.tesshu.jpsonic.service.scanner;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.RatingDao;
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

    public ExpungeService(ScannerStateServiceImpl scannerState, IndexManager indexManager, ArtistDao artistDao,
            AlbumDao albumDao, MediaFileDao mediaFileDao, RatingDao ratingDao) {
        super();
        this.scannerState = scannerState;
        this.indexManager = indexManager;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.mediaFileDao = mediaFileDao;
        this.ratingDao = ratingDao;
    }

    void expunge() {

        if (!scannerState.tryScanningLock()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cleanup/Scan/Podcast Download is already running.");
            }
            return;
        }

        if (scannerState.neverScanned()) {
            LOG.warn(
                    "Index hasn't been created yet or during scanning. Plese execute clean up after scan is completed.");
        } else {
            // to be before dao#expunge
            indexManager.startIndexing();
            indexManager.expunge();
            indexManager.stopIndexing();

            // to be after indexManager#expunge
            artistDao.expunge();
            albumDao.expunge();
            mediaFileDao.expunge();

            // to be after mediaFileDao#expunge
            ratingDao.expunge();

            mediaFileDao.checkpoint();
        }

        scannerState.unlockScanning();
    }
}
