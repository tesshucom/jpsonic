package com.tesshu.jpsonic.service.scanner;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
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

    private final ScannerStateService scannerState;
    private final IndexManager indexManager;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final MediaFileDao mediaFileDao;

    private final Object expungingLock = new Object();

    public ExpungeService(ScannerStateService scannerState, IndexManager indexManager, ArtistDao artistDao,
            AlbumDao albumDao, MediaFileDao mediaFileDao) {
        super();
        this.scannerState = scannerState;
        this.indexManager = indexManager;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.mediaFileDao = mediaFileDao;
    }

    void expunge() {
        synchronized (expungingLock) {
            if (scannerState.isScanning()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cleanup/Scan/Podca is already running.");
                }
                return;
            }
            scannerState.setScanning(true);
        }

        MediaLibraryStatistics statistics = indexManager.getStatistics();

        if (statistics != null && !scannerState.isScanning()) {

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
        scannerState.setExpunging(false);
    }
}
