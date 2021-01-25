/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */

package com.tesshu.jpsonic.dao;

import java.util.List;

import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

@Repository("jplaylistDao")
@DependsOn({ "playlistDao" })
public class JPlaylistDao extends AbstractDao {

    private final PlaylistDao deligate;

    public JPlaylistDao(PlaylistDao deligate) {
        super();
        this.deligate = deligate;
    }

    public void addPlaylistUser(int playlistId, String username) {
        deligate.addPlaylistUser(playlistId, username);
    }

    public void createPlaylist(Playlist playlist) {
        deligate.createPlaylist(playlist);
    }

    public void deletePlaylist(int id) {
        deligate.deletePlaylist(id);
    }

    public void deletePlaylistUser(int playlistId, String username) {
        deligate.deletePlaylistUser(playlistId, username);
    }

    public List<Playlist> getAllPlaylists() {
        return deligate.getAllPlaylists();
    }

    public int getCountAll() {
        return queryForInt("select count(id) from playlist", 0);
    }

    public Playlist getPlaylist(int id) {
        return deligate.getPlaylist(id);
    }

    public List<String> getPlaylistUsers(int playlistId) {
        return deligate.getPlaylistUsers(playlistId);
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {
        return deligate.getReadablePlaylistsForUser(username);
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {
        return deligate.getWritablePlaylistsForUser(username);
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        deligate.setFilesInPlaylist(id, files);
    }

    public void updatePlaylist(Playlist playlist) {
        deligate.updatePlaylist(playlist);
    }
}
