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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.ThreadSafe;
import com.tesshu.jpsonic.dao.StaticsDao;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.service.ScannerStateService;
import jakarta.annotation.PreDestroy;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * A class that holds the scan progress. It is generally said that services should not have state. Now they are
 * aggregated in this class.
 */
@Primary
@Service("scannerStateService")
public class ScannerStateServiceImpl implements ScannerStateService {

    private final StaticsDao staticsDao;

    private final LongAdder scanCount = new LongAdder();

    private final ReentrantLock scanningLock = new ReentrantLock();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private final AtomicBoolean destroy = new AtomicBoolean();

    private final AtomicBoolean cleansing = new AtomicBoolean(true);

    private Instant scanDate = FAR_PAST;

    private ScanEventType lastEvent = ScanEventType.UNKNOWN;

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

        boolean acquired;
        try {
            acquired = scanningLock.tryLock(1500L, TimeUnit.MILLISECONDS);
            if (acquired) {
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            acquired = false;
        }
        if (acquired) {
            scanDate = now();
            scanCount.reset();
            lastEvent = ScanEventType.UNKNOWN;
        }
        return acquired;
    }

    /**
     * Use only within the thread that acquired the lock.
     */
    @ThreadSafe(enableChecks = false)
    @NonNull
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
        lastEvent = ScanEventType.UNKNOWN;
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

    public ScanEventType getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(ScanEventType lastEvent) {
        this.lastEvent = lastEvent;
    }
}
