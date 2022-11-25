package com.tesshu.jpsonic.service.scanner;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.tesshu.jpsonic.service.search.IndexManager;
import org.springframework.stereotype.Service;

/**
 * A class that holds the scan progress. It is generally said that services should not have state. Now they are
 * aggregated in this class.
 */
@Service
public class ScannerStateService {

    // TODO To be fixed in v111.6.0
    private final IndexManager indexManager;

    // TODO To be fixed in v111.6.0
    private final AtomicInteger scanCount = new AtomicInteger();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean scanning = new AtomicBoolean();
    private final AtomicBoolean expunging = new AtomicBoolean();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean destroy = new AtomicBoolean();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean cleansing = new AtomicBoolean(true);

    public ScannerStateService(IndexManager indexManager) {
        super();
        this.indexManager = indexManager;
    }

    void incrementScanCount() {
        scanCount.incrementAndGet();
    }

    int getScanCount() {
        return scanCount.get();
    }

    void resetScanCount() {
        scanCount.set(0);
    }

    void setScanning(boolean b) {
        scanning.set(b);
    }

    boolean isScanning() {
        return scanning.get();
    }

    void setDestroy(boolean b) {
        destroy.set(b);
    }

    boolean isDestroy() {
        return destroy.get();
    }

    void setExpunging(boolean b) {
        expunging.set(b);
    }

    boolean isExpunging() {
        return expunging.get();
    }

    public void enableCleansing(boolean b) {
        cleansing.set(b);
    }

    boolean isEnableCleansing() {
        return cleansing.get();
    }

    boolean neverScanned() {
        return indexManager.getStatistics() == null;
    }
}
