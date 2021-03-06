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

package com.tesshu.jpsonic.command;

import java.util.List;

import com.tesshu.jpsonic.controller.SearchController;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.User;

/**
 * Command used in {@link SearchController}.
 *
 * @author Sindre Mehus
 */
public class SearchCommand {

    private String query;
    private List<MediaFile> artists;
    private List<MediaFile> albums;
    private List<MediaFile> songs;
    private boolean indexBeingCreated;
    private User user;
    private boolean partyModeEnabled;
    private Player player;
    private boolean composerVisible;
    private boolean genreVisible;
    private boolean simpleDisplay;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isIndexBeingCreated() {
        return indexBeingCreated;
    }

    public void setIndexBeingCreated(boolean indexBeingCreated) {
        this.indexBeingCreated = indexBeingCreated;
    }

    public List<MediaFile> getArtists() {
        return artists;
    }

    public void setArtists(List<MediaFile> artists) {
        this.artists = artists;
    }

    public List<MediaFile> getAlbums() {
        return albums;
    }

    public void setAlbums(List<MediaFile> albums) {
        this.albums = albums;
    }

    public List<MediaFile> getSongs() {
        return songs;
    }

    public void setSongs(List<MediaFile> songs) {
        this.songs = songs;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isPartyModeEnabled() {
        return partyModeEnabled;
    }

    public void setPartyModeEnabled(boolean partyModeEnabled) {
        this.partyModeEnabled = partyModeEnabled;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isComposerVisible() {
        return composerVisible;
    }

    public void setComposerVisible(boolean isComposerVisible) {
        this.composerVisible = isComposerVisible;
    }

    public boolean isGenreVisible() {
        return genreVisible;
    }

    public void setGenreVisible(boolean isGenreVisible) {
        this.genreVisible = isGenreVisible;
    }

    public boolean isSimpleDisplay() {
        return simpleDisplay;
    }

    public void setSimpleDisplay(boolean isSimpleDisplay) {
        this.simpleDisplay = isSimpleDisplay;
    }

    public static class Match {
        private final MediaFile mediaFile;
        private final String title;
        private final String album;
        private final String artist;

        public Match(MediaFile mediaFile, String title, String album, String artist) {
            this.mediaFile = mediaFile;
            this.title = title;
            this.album = album;
            this.artist = artist;
        }

        public MediaFile getMediaFile() {
            return mediaFile;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public String getArtist() {
            return artist;
        }
    }
}
