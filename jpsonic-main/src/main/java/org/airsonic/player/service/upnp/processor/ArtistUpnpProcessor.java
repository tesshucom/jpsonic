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

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class ArtistUpnpProcessor extends UpnpContentProcessor <Artist, Album> {

    private final ArtistDao artistDao;
    
    private final CoverArtLogic coverArtLogic;
    
    private final JpsonicComparators comparators;

    public ArtistUpnpProcessor(UpnpProcessDispatcher dispatcher, SettingsService settingsService, SearchService searchService, ArtistDao artistDao, JWTSecurityService jwtSecurityService,
            CoverArtLogic coverArtLogic, JpsonicComparators comparators) {
        super(dispatcher, settingsService, searchService, jwtSecurityService);
        this.artistDao = artistDao;
        this.coverArtLogic = coverArtLogic;
        this.comparators = comparators;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ARTIST_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.artists");
    }

    public Container createContainer(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + artist.getId());
        container.setParentID(getRootId());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            container.setProperties(Arrays.asList(new ALBUM_ART_URI(createArtistArtURI(artist))));
        }
        return container;
    }

    @Override
    public int getItemCount() {
        return artistDao.getArtistsCount(getAllMusicFolders());
    }

    @Override
    public List<Artist> getItems(long offset, long maxResults) {
        return artistDao.getAlphabetialArtists(offset, maxResults, getAllMusicFolders());
    }

    public Artist getItemById(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        int size = getDispatcher().getAlbumProcessor().getAlbumsCountForArtist(artist.getName(), getAllMusicFolders());
        return size > 1 ? size + 1 : size;
    }

    @Override
    public List<Album> getChildren(Artist artist, long offset, long maxResults) {
        List<Album> albums = getDispatcher().getAlbumProcessor()
                .getAlbumsForArtist(artist.getName(),
                        offset > 1 ? offset - 1 : offset,
                        0L == offset ? maxResults - 1 : maxResults,
                        comparators.isSortAlbumsByYear(artist.getName()),
                        getAllMusicFolders());
        if (albums.size() > 1 && 0L == offset) {
            Album firstElement = new Album();
            firstElement.setName(getResource("dlna.element.allalbums"));
            firstElement.setId(-1);
            firstElement.setComment(AlbumUpnpProcessor.ALL_BY_ARTIST + "_" + artist.getId());
            albums.add(0, firstElement);
        }
        return albums;
    }

    public void addChild(DIDLContent didl, Album album) {
        didl.addContainer(getDispatcher().getAlbumProcessor().createContainer(album));
    }

    private URI createArtistArtURI(Artist artist) {
        return createURIWithToken(UriComponentsBuilder.fromUriString(getBaseUrl() + "/ext/coverArt.view")
                .queryParam("id", coverArtLogic.createKey(artist))
                .queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

}
