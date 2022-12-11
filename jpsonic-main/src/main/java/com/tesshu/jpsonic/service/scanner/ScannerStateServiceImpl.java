package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.springframework.stereotype.Service;

/**
 * A class that holds the scan progress. It is generally said that services should not have state. Now they are
 * aggregated in this class.
 */
@Service("scannerStateService")
public class ScannerStateServiceImpl implements ScannerStateService {

    // TODO To be fixed in v111.6.0 #1841
    private final IndexManager indexManager;

    private final LongAdder scanCount = new LongAdder();

    private final ReentrantLock scanningLock = new ReentrantLock();

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean destroy = new AtomicBoolean();

    private final AtomicBoolean cleansing = new AtomicBoolean(true);

    /**
     * Scan start time. Use only within the thread that acquired the lock.
     */
    private Instant scanDate = FAR_PAST;

    public ScannerStateServiceImpl(IndexManager indexManager) {
        super();
        this.indexManager = indexManager;
    }

    Instant getScanDate() {
        return scanDate;
    }

    void incrementScanCount() {
        scanCount.increment();
    }

    @Override
    public long getScanCount() {
        return scanCount.sum();
    }

    boolean tryScanningLock() {
        boolean acquired = scanningLock.tryLock();
        if (acquired) {
            scanDate = now();
            scanCount.reset();
        }
        return acquired;
    }

    void unlockScanning() {
        scanningLock.unlock();
        scanDate = FAR_PAST;
        scanCount.reset();
    }

    @Override
    public boolean isScanning() {
        return scanningLock.isLocked();
    }

    @PreDestroy
    void preDestroy() {
        destroy.set(true);
    }

    boolean isDestroy() {
        return destroy.get();
    }

    void enableCleansing(boolean b) {
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
