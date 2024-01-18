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

package com.tesshu.jpsonic.domain;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 * Status for a single transfer (stream, download or upload).
 *
 * @author Sindre Mehus
 */
public final class TransferStatus implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HISTORY_LENGTH = 200;
    private static final long SAMPLE_INTERVAL_MILLIS = 5000;

    private transient Player player;
    private String pathString;
    private final AtomicLong bytesTransfered;
    private final AtomicLong bytesSkipped;
    private final AtomicLong bytesTotal;
    private final SampleHistory history;
    private boolean terminated;
    private boolean active;
    private final Object historyLock = new Object();

    public TransferStatus() {
        bytesTransfered = new AtomicLong();
        bytesSkipped = new AtomicLong();
        bytesTotal = new AtomicLong();
        history = new SampleHistory(HISTORY_LENGTH);
        active = true;
    }

    public long getBytesTransfered() {
        return bytesTransfered.get();
    }

    public void addBytesTransfered(long byteCount) {
        setBytesTransfered(bytesTransfered.addAndGet(byteCount));
    }

    public void setBytesTransfered(long bytesTransfered) {
        synchronized (historyLock) {
            this.bytesTransfered.set(bytesTransfered);
            createSample(bytesTransfered, false);
        }
    }

    private void createSample(long bytesTransfered, boolean force) {
        long now = Instant.now().toEpochMilli();

        if (history.isEmpty()) {
            history.add(new Sample(bytesTransfered, now));
        } else {
            Sample lastSample = history.getLast();
            if (force || now - lastSample.getTimestamp() > TransferStatus.SAMPLE_INTERVAL_MILLIS) {
                history.add(new Sample(bytesTransfered, now));
            }
        }
    }

    public long getMillisSinceLastUpdate() {
        synchronized (historyLock) {
            if (history.isEmpty()) {
                return 0L;
            }
            return Instant.now().toEpochMilli() - history.getLast().getTimestamp();
        }
    }

    public long getBytesTotal() {
        return bytesTotal.get();
    }

    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal.set(bytesTotal);
    }

    public long getBytesSkipped() {
        return bytesSkipped.get();
    }

    public void setBytesSkipped(long bytesSkipped) {
        this.bytesSkipped.set(bytesSkipped);
    }

    public void addBytesSkipped(long byteCount) {
        bytesSkipped.addAndGet(byteCount);
    }

    public String getPathString() {
        return pathString;
    }

    public void setPathString(String pathString) {
        this.pathString = pathString;
    }

    public Path toPath() {
        return Path.of(pathString);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public SampleHistory getHistory() {
        synchronized (historyLock) {
            return new SampleHistory(HISTORY_LENGTH, history);
        }
    }

    public long getHistoryLengthMillis() {
        return TransferStatus.SAMPLE_INTERVAL_MILLIS * (TransferStatus.HISTORY_LENGTH - 1);
    }

    public void terminate() {
        terminated = true;
    }

    public boolean isTerminated() {
        boolean result = terminated;
        terminated = false;
        return result;
    }

    public boolean isActive() {
        synchronized (historyLock) {
            return active;
        }
    }

    public void setActive(boolean active) {
        synchronized (historyLock) {
            this.active = active;
            if (active) {
                bytesSkipped.set(0);
                bytesTotal.set(0);
                setBytesTransfered(0L);
            } else {
                createSample(getBytesTransfered(), true);
            }
        }
    }

    public static class Sample {
        private final long bytesTransfered;
        private final long timestamp;

        public Sample(long bytesTransfered, long timestamp) {
            this.bytesTransfered = bytesTransfered;
            this.timestamp = timestamp;
        }

        public long getBytesTransfered() {
            return bytesTransfered;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    @Override
    public String toString() {
        return "TransferStatus-" + hashCode() + " [player: " + player.getId() + ", path: " + pathString
                + ", terminated: " + terminated + ", active: " + isActive() + "]";
    }

    @SuppressWarnings("serial")
    public static class SampleHistory extends CircularFifoQueue<Sample> {

        public SampleHistory(int length) {
            super(length);
        }

        public SampleHistory(int length, SampleHistory other) {
            this(length);
            addAll(other);
        }

        public Sample getLast() {
            return this.get(this.size() - 1);
        }
    }
}
