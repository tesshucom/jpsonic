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

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.springframework.stereotype.Component;

@Component
public class PlaylistUpnpProcessor extends DirectChildrenContentProcessor<Playlist, MediaFile> {

    private final UpnpDIDLFactory factory;
    private final PlaylistService playlistService;

    public PlaylistUpnpProcessor(UpnpDIDLFactory factory, PlaylistService playlistService) {
        super();
        this.factory = factory;
        this.playlistService = playlistService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PLAYLIST;
    }

    @Override
    public Container createContainer(Playlist item) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(ProcId.PLAYLIST.getValue() + ProcId.CID_SEPA + item.getId());
        container.setParentID(ProcId.PLAYLIST.getValue());
        container.setTitle(item.getName());
        container.setDescription(item.getComment());
        container.setChildCount(playlistService.getFilesInPlaylist(item.getId()).size());
        container.addProperty(factory.toPlaylistArt(item));
        return container;
    }

    @Override
    public int getDirectChildrenCount() {
        return playlistService.getCountAll();
    }

    @Override
    public List<Playlist> getDirectChildren(long offset, long maxResults) {
        return PlayerUtils.subList(playlistService.getAllPlaylists(), offset, maxResults);
    }

    @Override
    public Playlist getDirectChild(String id) {
        return playlistService.getPlaylist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Playlist item) {
        return playlistService.getCountInPlaylist(item.getId());
    }

    @Override
    public List<MediaFile> getChildren(Playlist item, long offset, long maxResults) {
        return playlistService.getFilesInPlaylist(item.getId(), offset, maxResults);
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile song) {
        didl.addItem(factory.toMusicTrack(song));
    }
}
