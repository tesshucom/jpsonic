/*
  This file is part of Airsonic.

  Airsonic is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Airsonic is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;


/**
 * @author Allen Petersen
 * @version $Id$
 */
@Component
public class PlaylistUpnpProcessor extends UpnpContentProcessor <Playlist, MediaFile> {

    private PlaylistService playlistService;
    
    public PlaylistUpnpProcessor(PlaylistService playlistService) {
        super();
        this.playlistService = playlistService;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_PLAYLIST_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dnla.title.playlists");
    }

    public Container createContainer(Playlist item) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
        container.setParentID(getRootId());
        container.setTitle(item.getName());
        container.setDescription(item.getComment());
        container.setChildCount(playlistService.getFilesInPlaylist(item.getId()).size());
        return container;
    }

    @Override
    public int getItemCount() {
        return playlistService.getCountAll();
    }

    @Override
    public List<Playlist> getItems(long offset, long maxResults) {
        // Currently sorting on the Java side(Using sublist because less affected).
        List<Playlist> playlists = playlistService.getAllPlaylists();
        return org.airsonic.player.util.Util.subList(playlists, offset, maxResults);
    }

    public Playlist getItemById(String id) {
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

    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().createItem(child));
    }

}
