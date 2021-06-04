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

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 * Status for a single transfer (stream, download or upload).
 *
 * @author Sindre Mehus
 */
public class TransferStatus {

    private static final int HISTORY_LENGTH = 200;
    private static final long SAMPLE_INTERVAL_MILLIS = 5000;

    /*
     * History-only locking is sufficient on this class of legacy design. If have problems with synchronization
     * restrictions, the constructor design should be reviewed. Signed-off-by: tesshucom <webmaster@tesshu.com>
     */
    private static final Object HISTORY_LOCK = new Object();

    private Player player;
    private File file;
    private final AtomicLong bytesTransfered;
    private final AtomicLong bytesSkipped;
    private final AtomicLong bytesTotal;
    private final SampleHistory history;
    private boolean terminated;
    private boolean active;

    public TransferStatus() {
        bytesTransfered = new AtomicLong();
        bytesSkipped = new AtomicLong();
        bytesTotal = new AtomicLong();
        history = new SampleHistory(HISTORY_LENGTH);
        active = true;
    }

    /**
     * Return the number of bytes transferred.
     *
     * @return The number of bytes transferred.
     */
    public long getBytesTransfered() {
        return bytesTransfered.get();
    }

    /**
     * Adds the given byte count to the total number of bytes transferred.
     *
     * @param byteCount
     *            The byte count.
     */
    public void addBytesTransfered(long byteCount) {
        setBytesTransfered(bytesTransfered.addAndGet(byteCount));
    }

    /**
     * Sets the number of bytes transferred.
     *
     * @param bytesTransfered
     *            The number of bytes transferred.
     */
    public void setBytesTransfered(long bytesTransfered) {
        synchronized (HISTORY_LOCK) {
            this.bytesTransfered.set(bytesTransfered);
            createSample(bytesTransfered, false);
        }
    }

    private void createSample(long bytesTransfered, boolean force) {
        long now = System.currentTimeMillis();

        if (history.isEmpty()) {
            history.add(new Sample(bytesTransfered, now));
        } else {
            Sample lastSample = history.getLast();
            if (force || now - lastSample.getTimestamp() > TransferStatus.SAMPLE_INTERVAL_MILLIS) {
                history.add(new Sample(bytesTransfered, now));
            }
        }
    }

    /**
     * Returns the number of milliseconds since the transfer status was last updated.
     *
     * @return Number of milliseconds, or <code>0</code> if never updated.
     */
    public long getMillisSinceLastUpdate() {
        synchronized (HISTORY_LOCK) {
            if (history.isEmpty()) {
                return 0L;
            }
            return System.currentTimeMillis() - history.getLast().getTimestamp();
        }
    }

    /**
     * Returns the total number of bytes, or 0 if unknown.
     *
     * @return The total number of bytes, or 0 if unknown.
     */
    public long getBytesTotal() {
        return bytesTotal.get();
    }

    /**
     * Sets the total number of bytes, or 0 if unknown.
     *
     * @param bytesTotal
     *            The total number of bytes, or 0 if unknown.
     */
    public void setBytesTotal(long bytesTotal) {
        this.bytesTotal.set(bytesTotal);
    }

    /**
     * Returns the number of bytes that has been skipped (for instance when resuming downloads).
     *
     * @return The number of skipped bytes.
     */
    public long getBytesSkipped() {
        return bytesSkipped.get();
    }

    /**
     * Sets the number of bytes that has been skipped (for instance when resuming downloads).
     *
     * @param bytesSkipped
     *            The number of skipped bytes.
     */
    public void setBytesSkipped(long bytesSkipped) {
        this.bytesSkipped.set(bytesSkipped);
    }

    /**
     * Adds the given byte count to the total number of bytes skipped.
     *
     * @param byteCount
     *            The byte count.
     */
    public void addBytesSkipped(long byteCount) {
        bytesSkipped.addAndGet(byteCount);
    }

    /**
     * Returns the file that is currently being transferred.
     *
     * @return The file that is currently being transferred.
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file that is currently being transferred.
     *
     * @param file
     *            The file that is currently being transferred.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Returns the remote player for the stream.
     *
     * @return The remote player for the stream.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Sets the remote player for the stream.
     *
     * @param player
     *            The remote player for the stream.
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Returns a history of samples for the stream
     *
     * @return A (copy of) the history list of samples.
     */
    public SampleHistory getHistory() {
        synchronized (HISTORY_LOCK) {
            return new SampleHistory(HISTORY_LENGTH, history);
        }
    }

    /**
     * Returns the history length in milliseconds.
     *
     * @return The history length in milliseconds.
     */
    public long getHistoryLengthMillis() {
        return TransferStatus.SAMPLE_INTERVAL_MILLIS * (TransferStatus.HISTORY_LENGTH - 1);
    }

    /**
     * Indicate that the stream should be terminated.
     */
    public void terminate() {
        terminated = true;
    }

    /**
     * Returns whether this stream has been terminated. Not that the <em>terminated status</em> is cleared by this
     * method.
     *
     * @return Whether this stream has been terminated.
     */
    public boolean isTerminated() {
        boolean result = terminated;
        terminated = false;
        return result;
    }

    /**
     * Returns whether this transfer is active, i.e., if the connection is still established.
     *
     * @return Whether this transfer is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether this transfer is active, i.e., if the connection is still established.
     *
     * @param active
     *            Whether this transfer is active.
     */
    public void setActive(boolean active) {
        synchronized (HISTORY_LOCK) {
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

    /**
     * A sample containing a timestamp and the number of bytes transferred up to that point in time.
     */
    public static class Sample {
        private final long bytesTransfered;
        private final long timestamp;

        /**
         * Creates a new sample.
         *
         * @param bytesTransfered
         *            The total number of bytes transferred.
         * @param timestamp
         *            A point in time, in milliseconds.
         */
        public Sample(long bytesTransfered, long timestamp) {
            this.bytesTransfered = bytesTransfered;
            this.timestamp = timestamp;
        }

        /**
         * Returns the number of bytes transferred.
         *
         * @return The number of bytes transferred.
         */
        public long getBytesTransfered() {
            return bytesTransfered;
        }

        /**
         * Returns the timestamp of the sample.
         *
         * @return The timestamp in milliseconds.
         */
        public long getTimestamp() {
            return timestamp;
        }
    }

    @Override
    public String toString() {
        return "TransferStatus-" + hashCode() + " [player: " + player.getId() + ", file: " + file + ", terminated: "
                + terminated + ", active: " + active + "]";
    }

    /**
     * Contains recent history of samples.
     */
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
