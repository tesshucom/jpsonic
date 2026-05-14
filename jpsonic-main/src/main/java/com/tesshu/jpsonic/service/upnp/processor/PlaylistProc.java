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
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.Playlist;
import com.tesshu.jpsonic.service.PlaylistService;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Component;

@Component
public class PlaylistProc extends DirectChildrenContentProc<Playlist, MediaFile> {

    private final UpnpDIDLFactory factory;
    private final PlaylistService playlistService;

    public PlaylistProc(UpnpDIDLFactory factory, PlaylistService playlistService) {
        super();
        this.factory = factory;
        this.playlistService = playlistService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PLAYLIST;
    }

    @Override
    public Container createContainer(Playlist playlist) {
        return factory.toPlaylist(playlist);
    }

    @Override
    public List<Playlist> getDirectChildren(long offset, long count) {
        return playlistService.getAllPlaylists().stream().skip(offset).limit(count).toList();
    }

    @Override
    public int getDirectChildrenCount() {
        return playlistService.getCountAll();
    }

    @Override
    public Playlist getDirectChild(String id) {
        return playlistService.getPlaylist(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(Playlist item, long offset, long count) {
        return playlistService.getFilesInPlaylist(item.getId(), offset, count);
    }

    @Override
    public int getChildSizeOf(Playlist playlist) {
        return playlist.getFileCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile song) {
        parent.addItem(factory.toMusicTrack(song));
    }
}
