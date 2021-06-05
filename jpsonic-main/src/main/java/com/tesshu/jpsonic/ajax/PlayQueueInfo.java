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

package com.tesshu.jpsonic.ajax;

import java.util.List;

import com.tesshu.jpsonic.util.StringUtil;

/**
 * The playlist of a player.
 *
 * @author Sindre Mehus
 */
public class PlayQueueInfo {

    private final List<Entry> entries;
    private final boolean stopEnabled;
    private final boolean repeatEnabled;
    private final boolean shuffleRadioEnabled;
    private final boolean internetRadioEnabled;
    private final boolean sendM3U;
    private final float gain;

    private int startPlayerAt;
    private long startPlayerAtPosition; // millis

    public PlayQueueInfo(List<Entry> entries, boolean stopEnabled, boolean repeatEnabled, boolean shuffleRadioEnabled,
            boolean internetRadioEnabled, boolean sendM3U, float gain) {
        this.entries = entries;
        this.stopEnabled = stopEnabled;
        this.repeatEnabled = repeatEnabled;
        this.shuffleRadioEnabled = shuffleRadioEnabled;
        this.internetRadioEnabled = internetRadioEnabled;
        this.sendM3U = sendM3U;
        this.gain = gain;
        startPlayerAt = -1;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public String getDurationAsString() {
        int durationSeconds = 0;
        for (Entry entry : entries) {
            if (entry.getDuration() != null) {
                durationSeconds += entry.getDuration();
            }
        }
        return StringUtil.formatDurationMSS(durationSeconds);
    }

    public boolean isStopEnabled() {
        return stopEnabled;
    }

    public boolean isSendM3U() {
        return sendM3U;
    }

    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }

    public boolean isShuffleRadioEnabled() {
        return shuffleRadioEnabled;
    }

    public boolean isInternetRadioEnabled() {
        return internetRadioEnabled;
    }

    public float getGain() {
        return gain;
    }

    public int getStartPlayerAt() {
        return startPlayerAt;
    }

    private void setStartPlayerAt(int startPlayerAt) {
        this.startPlayerAt = startPlayerAt;
    }

    public PlayQueueInfo startPlayerAtAndGetInfo(int startPlayerAt) {
        setStartPlayerAt(startPlayerAt);
        return this;
    }

    public long getStartPlayerAtPosition() {
        return startPlayerAtPosition;
    }

    private void setStartPlayerAtPosition(long startPlayerAtPosition) {
        this.startPlayerAtPosition = startPlayerAtPosition;
    }

    public PlayQueueInfo startPlayerAtPositionAndGetInfo(long startPlayerAtPosition) {
        setStartPlayerAtPosition(startPlayerAtPosition);
        return this;
    }

    public static class Entry {
        private final int id;
        private final Integer trackNumber;
        private final String title;
        private final String artist;
        private final String composer;
        private final String album;
        private final String genre;
        private final Integer year;
        private final String bitRate;
        private final Integer duration;
        private final String durationAsString;
        private final String format;
        private final String contentType;
        private final String fileSize;
        private final boolean starred;
        private final String albumUrl;
        private final String streamUrl;
        private final String remoteStreamUrl;
        private final String coverArtUrl;
        private final String remoteCoverArtUrl;

        public Entry(int id, Integer trackNumber, String title, String artist, String composer, String album,
                String genre, Integer year, String bitRate, Integer duration, String durationAsString, String format,
                String contentType, String fileSize, boolean starred, String albumUrl, String streamUrl,
                String remoteStreamUrl, String coverArtUrl, String remoteCoverArtUrl) {

            this.id = id;
            this.trackNumber = trackNumber;
            this.title = title;
            this.artist = artist;
            this.composer = composer;
            this.album = album;
            this.genre = genre;
            this.year = year;
            this.bitRate = bitRate;
            this.duration = duration;
            this.durationAsString = durationAsString;
            this.format = format;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.starred = starred;
            this.albumUrl = albumUrl;
            this.streamUrl = streamUrl;
            this.remoteStreamUrl = remoteStreamUrl;
            this.coverArtUrl = coverArtUrl;
            this.remoteCoverArtUrl = remoteCoverArtUrl;
        }

        public int getId() {
            return id;
        }

        public Integer getTrackNumber() {
            return trackNumber;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        public String getComposer() {
            return composer;
        }

        public String getAlbum() {
            return album;
        }

        public String getGenre() {
            return genre;
        }

        public Integer getYear() {
            return year;
        }

        public String getBitRate() {
            return bitRate;
        }

        public String getDurationAsString() {
            return durationAsString;
        }

        public Integer getDuration() {
            return duration;
        }

        public String getFormat() {
            return format;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileSize() {
            return fileSize;
        }

        public boolean isStarred() {
            return starred;
        }

        public String getAlbumUrl() {
            return albumUrl;
        }

        public String getStreamUrl() {
            return streamUrl;
        }

        public String getRemoteStreamUrl() {
            return remoteStreamUrl;
        }

        public String getCoverArtUrl() {
            return coverArtUrl;
        }

        public String getRemoteCoverArtUrl() {
            return remoteCoverArtUrl;
        }

    }
}
