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

import com.tesshu.jpsonic.domain.Playlist;

/**
 * The playlist of a player.
 *
 * @author Sindre Mehus
 */
public class PlaylistInfo {

    private final Playlist playlist;
    private final List<Entry> entries;

    public PlaylistInfo(Playlist playlist, List<Entry> entries) {
        this.playlist = playlist;
        this.entries = entries;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private final int id;
        private final String title;
        private final String artist;
        private final String composer;
        private final String album;
        private final String genre;
        private final String durationAsString;
        private final boolean starred;
        private final boolean present;

        public Entry(int id, String title, String artist, String composer, String album, String genre,
                String durationAsString, boolean starred, boolean present) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.composer = composer;
            this.album = album;
            this.genre = genre;
            this.durationAsString = durationAsString;
            this.starred = starred;
            this.present = present;
        }

        public int getId() {
            return id;
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

        public String getDurationAsString() {
            return durationAsString;
        }

        public boolean isStarred() {
            return starred;
        }

        public boolean isPresent() {
            return present;
        }
    }
}
