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

import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PlaylistContainer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component
public class PlaylistUpnpProcessor extends UpnpContentProcessor <Playlist, MediaFile> {

    private final UpnpProcessorUtil util;

    private final PlaylistService playlistService;

    private final CoverArtLogic coverArtLogic;

    public PlaylistUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, PlaylistService p, CoverArtLogic c) {
        super(d, u);
        this.util = u;
        this.playlistService = p;
        this.coverArtLogic = c;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_PLAYLIST_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.playlists");
    }

    @Override
    public Container createContainer(Playlist item) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + item.getId());
        container.setParentID(getRootId());
        container.setTitle(item.getName());
        container.setDescription(item.getComment());
        container.setChildCount(playlistService.getFilesInPlaylist(item.getId()).size());
        container.setProperties(Arrays.asList(new ALBUM_ART_URI(getArtURI(item))));
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
        return org.airsonic.player.util.PlayerUtils.subList(playlists, offset, maxResults);
    }

    @Override
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

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

    private URI getArtURI(Playlist playlist) {
        return util.addJWTToken(UriComponentsBuilder.fromUriString(util.getBaseUrl() + "/ext/" + ViewName.COVER_ART.value())
                .queryParam("id", coverArtLogic.createKey(playlist))
                .queryParam("size", CoverArtScheme.LARGE.getSize())).build().encode().toUri();
    }

}
