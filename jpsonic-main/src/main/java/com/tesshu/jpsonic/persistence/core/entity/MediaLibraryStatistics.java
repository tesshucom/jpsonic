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

package com.tesshu.jpsonic.persistence.core.entity;

import java.time.Instant;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Contains media libaray statistics, including the number of artists, albums
 * and songs.
 *
 * @author Sindre Mehus
 */
public class MediaLibraryStatistics {

    private Instant executed;
    private int folderId;
    private int artistCount;
    private int albumCount;
    private int songCount;
    private int videoCount;
    private long totalSize;
    private long totalDuration;

    public MediaLibraryStatistics(@NonNull Instant executed) {
        this.executed = executed;
    }

    public MediaLibraryStatistics(@NonNull Instant executed, int folderId, int artistCount,
            int albumCount, int songCount, int videoCount, long totalSize, long totalDuration) {
        super();
        this.executed = executed;
        this.folderId = folderId;
        this.artistCount = artistCount;
        this.albumCount = albumCount;
        this.songCount = songCount;
        this.videoCount = videoCount;
        this.totalSize = totalSize;
        this.totalDuration = totalDuration;
    }

    public @NonNull Instant getExecuted() {
        return executed;
    }

    public void setExecuted(@NonNull Instant executed) {
        this.executed = executed;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public int getArtistCount() {
        return artistCount;
    }

    public void setArtistCount(int artistCount) {
        this.artistCount = artistCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }
}
