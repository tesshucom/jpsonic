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

import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;

import static org.airsonic.player.service.upnp.UpnpProcessDispatcher.CONTAINER_ID_RANDOM_SONG_BY_ARTIST;

@Service
public class RandomSongByArtistUpnpProcessor extends UpnpContentProcessor <Artist, MediaFile> {

    private final UpnpProcessorUtil util;
    private final ArtistDao artistDao;
    private final SearchService searchService;

    private final static int RANDOM_MAX = 50;

    public RandomSongByArtistUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, ArtistDao a, SearchService s) {
        super(d, u);
        util = u;
        artistDao = a;
        searchService = s;
        setRootId(CONTAINER_ID_RANDOM_SONG_BY_ARTIST);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomSongByArtist");
    }

    public Container createContainer(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + artist.getId());
        container.setParentID(getRootId());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            container.setProperties(Arrays.asList(new ALBUM_ART_URI(getDispatcher().getArtistProcessor().createArtistArtURI(artist))));
        }
        return container;
    }

    @Override
    public int getItemCount() {
        return artistDao.getArtistsCount(util.getAllMusicFolders());
    }

    @Override
    public List<Artist> getItems(long offset, long maxResults) {
        return artistDao.getAlphabetialArtists(offset, maxResults, util.getAllMusicFolders());
    }

    public Artist getItemById(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        // Because there is no count cache, it is difficult to return accurate values
        return RANDOM_MAX;
    }

    @Override
    public List<MediaFile> getChildren(Artist artist, long offset, long maxResults) {
        return searchService.getRandomSongsByArtist(artist, (int) maxResults, (int) offset, RANDOM_MAX, util.getAllMusicFolders());
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

}
