package com.tesshu.jpsonic.service.scanner;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.springframework.stereotype.Service;

/**
 * A class that holds the scan progress. It is generally said that services should not have state. Now they are
 * aggregated in this class.
 */
@Service("scannerStateService")
public class ScannerStateServiceImpl implements ScannerStateService {

    // TODO To be fixed in v111.6.0
    private final IndexManager indexManager;

    // TODO To be fixed in v111.6.0
    private final AtomicInteger scanCount = new AtomicInteger();

    private final ReentrantLock scanningLock = new ReentrantLock();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean destroy = new AtomicBoolean();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean cleansing = new AtomicBoolean(true);

    public ScannerStateServiceImpl(IndexManager indexManager) {
        super();
        this.indexManager = indexManager;
    }

    void incrementScanCount() {
        scanCount.incrementAndGet();
    }

    @Override
    public int getScanCount() {
        return scanCount.get();
    }

    void resetScanCount() {
        scanCount.set(0);
    }

    boolean tryScanningLock() {
        return scanningLock.tryLock();
    }

    void unlockScanning() {
        scanningLock.unlock();
    }

    @Override
    public boolean isScanning() {
        return scanningLock.isLocked();
    }

    void setDestroy(boolean b) {
        destroy.set(b);
    }

    boolean isDestroy() {
        return destroy.get();
    }

    public void enableCleansing(boolean b) {
        cleansing.set(b);
    }

    boolean isEnableCleansing() {
        return cleansing.get();
    }

    @Override
    public boolean neverScanned() {
        return indexManager.getStatistics() == null;
    }
}
