package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.ThreadSafe;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.service.ScannerStateService;
import org.springframework.stereotype.Service;

/**
 * A class that holds the scan progress. It is generally said that services should not have state. Now they are
 * aggregated in this class.
 */
@Service("scannerStateService")
public class ScannerStateServiceImpl implements ScannerStateService {

    private final StaticsDao staticsDao;

    private final LongAdder scanCount = new LongAdder();

    private final ReentrantLock scanningLock = new ReentrantLock();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    // TODO To be fixed in v111.6.0
    private final AtomicBoolean destroy = new AtomicBoolean();

    private final AtomicBoolean cleansing = new AtomicBoolean(true);

    private Instant scanDate = FAR_PAST;

    public ScannerStateServiceImpl(StaticsDao staticsDao) {
        super();
        this.staticsDao = staticsDao;
    }

    /**
     * Called only once before shutdown.
     */
    @PreDestroy
    void preDestroy() {
        destroy.set(true);
    }

    boolean isDestroy() {
        return destroy.get();
    }

    @Override
    public boolean neverScanned() {
        return staticsDao.isNeverScanned();
    }

    /**
     * Called only once after startup.
     */
    public void setReady() {
        ready.set(true);
    }

    boolean tryScanningLock() {
        if (!ready.get() || destroy.get()) {
            return false;
        }
        boolean acquired = scanningLock.tryLock();
        if (acquired) {
            scanDate = now();
            scanCount.reset();
        }
        return acquired;
    }

    /**
     * Use only within the thread that acquired the lock.
     */
    @ThreadSafe(enableChecks = false)
    Instant getScanDate() {
        return scanDate;
    }

    /**
     * Use only within the thread that acquired the lock.
     */
    @ThreadSafe(enableChecks = false)
    void unlockScanning() {
        scanDate = FAR_PAST;
        scanCount.reset();
        scanningLock.unlock();
    }

    @Override
    public boolean isScanning() {
        return ready.get() && scanningLock.isLocked();
    }

    void incrementScanCount() {
        scanCount.increment();
    }

    @Override
    public long getScanCount() {
        return scanCount.sum();
    }

    void enableCleansing(boolean b) {
        cleansing.set(b);
    }

    boolean isEnableCleansing() {
        return cleansing.get();
    }
}
