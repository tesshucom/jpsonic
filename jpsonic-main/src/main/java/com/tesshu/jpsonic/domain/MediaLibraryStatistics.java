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

import java.time.Instant;
import java.util.Objects;

import javax.validation.constraints.NotNull;

/**
 * Contains media libaray statistics, including the number of artists, albums and songs.
 *
 * @author Sindre Mehus
 */
public class MediaLibraryStatistics {

    @NotNull
    private Integer artistCount = 0;
    @NotNull
    private Integer albumCount = 0;
    @NotNull
    private Integer songCount = 0;
    @NotNull
    private Long totalLengthInBytes = 0L;
    @NotNull
    private Long totalDurationInSeconds = 0L;
    @NotNull
    private Instant scanDate;

    public MediaLibraryStatistics() {

    }

    public MediaLibraryStatistics(Instant scanDate) {
        if (scanDate == null) {
            throw new IllegalArgumentException();
        }
        this.scanDate = scanDate;
    }

    public Integer getArtistCount() {
        return artistCount;
    }

    public void setArtistCount(Integer artistCount) {
        this.artistCount = artistCount;
    }

    public Integer getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(Integer albumCount) {
        this.albumCount = albumCount;
    }

    public Integer getSongCount() {
        return songCount;
    }

    public void setSongCount(Integer songCount) {
        this.songCount = songCount;
    }

    public Long getTotalLengthInBytes() {
        return totalLengthInBytes;
    }

    public void setTotalLengthInBytes(Long totalLengthInBytes) {
        this.totalLengthInBytes = totalLengthInBytes;
    }

    public Long getTotalDurationInSeconds() {
        return totalDurationInSeconds;
    }

    public void setTotalDurationInSeconds(Long totalDurationInSeconds) {
        this.totalDurationInSeconds = totalDurationInSeconds;
    }

    public Instant getScanDate() {
        return scanDate;
    }

    public void setScanDate(Instant scanDate) {
        this.scanDate = scanDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaLibraryStatistics)) {
            return false;
        }
        MediaLibraryStatistics that = (MediaLibraryStatistics) o;
        return Objects.equals(artistCount, that.artistCount) && Objects.equals(albumCount, that.albumCount)
                && Objects.equals(songCount, that.songCount)
                && Objects.equals(totalLengthInBytes, that.totalLengthInBytes)
                && Objects.equals(totalDurationInSeconds, that.totalDurationInSeconds)
                && Objects.equals(scanDate, that.scanDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artistCount, albumCount, songCount, totalLengthInBytes, totalDurationInSeconds, scanDate);
    }
}
