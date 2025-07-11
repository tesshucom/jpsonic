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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.domain;

import java.time.Instant;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ScanEvent {

    private Instant startDate;
    private Instant executed;
    private ScanEventType type;
    private long maxMemory;
    private long totalMemory;
    private long freeMemory;
    private int maxThread;
    private String comment;

    public ScanEvent(@NonNull Instant startDate, @NonNull Instant executed,
            @NonNull ScanEventType type, @Nullable Long maxMemory, @Nullable Long totalMemory,
            @Nullable Long freeMemory, @Nullable Integer maxThread, @Nullable String comment) {
        super();
        this.startDate = startDate;
        this.executed = executed;
        this.type = type;
        setMaxMemory(maxMemory);
        setTotalMemory(totalMemory);
        setFreeMemory(freeMemory);
        setMaxThread(maxThread);
        this.comment = comment;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getExecuted() {
        return executed;
    }

    public void setExecuted(Instant executed) {
        this.executed = executed;
    }

    public ScanEventType getType() {
        return type;
    }

    public void setType(ScanEventType type) {
        this.type = type;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public final void setMaxMemory(Long maxMemory) {
        this.maxMemory = maxMemory == null ? -1 : maxMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public final void setTotalMemory(Long totalMemory) {
        this.totalMemory = totalMemory == null ? -1 : totalMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public final void setFreeMemory(Long freeMemory) {
        this.freeMemory = freeMemory == null ? -1 : freeMemory;
    }

    public int getMaxThread() {
        return maxThread;
    }

    public final void setMaxThread(Integer maxThread) {
        this.maxThread = maxThread == null ? -1 : maxThread;
    }

    public @Nullable String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public enum ScanEventType {
        SUCCESS, FAILED, DESTROYED, CANCELED,

        UNKNOWN,

        FOLDER_CREATE, FOLDER_DELETE, FOLDER_UPDATE,

        BEFORE_SCAN, MUSIC_FOLDER_CHECK, PARSE_FILE_STRUCTURE, SCANNED_COUNT, PARSE_VIDEO,
        PARSE_PODCAST, CLEAN_UP_FILE_STRUCTURE, PARSE_ALBUM, UPDATE_SORT_OF_ALBUM,
        UPDATE_ORDER_OF_ALBUM, UPDATE_SORT_OF_ARTIST, UPDATE_ORDER_OF_ARTIST, UPDATE_ORDER_OF_SONG,
        REFRESH_ALBUM_ID3, UPDATE_ORDER_OF_ALBUM_ID3, REFRESH_ARTIST_ID3,
        UPDATE_ORDER_OF_ARTIST_ID3, UPDATE_ALBUM_COUNTS, UPDATE_GENRE_MASTER, RUN_STATS,
        IMPORT_PLAYLISTS, CHECKPOINT, AFTER_SCAN,

        // Obsolete reserved word. don't use
        @Deprecated
        FINISHED, @Deprecated
        PARSED_COUNT;

        public static ScanEventType of(String name) {
            return Stream
                .of(values())
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElse(UNKNOWN);
        }
    }
}
