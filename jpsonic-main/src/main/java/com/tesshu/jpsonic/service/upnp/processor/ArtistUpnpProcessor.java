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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.domain.logic.CoverArtLogic;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ArtistUpnpProcessor extends UpnpContentProcessor<Artist, Album> {

    private final UpnpProcessorUtil util;
    private final JArtistDao artistDao;
    private final CoverArtLogic coverArtLogic;

    public ArtistUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JArtistDao a, CoverArtLogic c) {
        super(d, u);
        this.util = u;
        this.artistDao = a;
        this.coverArtLogic = c;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ARTIST_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.artists");
    }

    @Override
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
        return artistDao.getArtistsCount(util.getGuestMusicFolders());
    }

    @Override
    public List<Artist> getItems(long offset, long maxResults) {
        return artistDao.getAlphabetialArtists((int) offset, (int) maxResults, util.getGuestMusicFolders());
    }

    @Override
    public Artist getItemById(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        int size = getDispatcher().getAlbumProcessor().getAlbumsCountForArtist(artist.getName(),
                util.getGuestMusicFolders());
        return size > 1 ? size + 1 : size;
    }

    @Override
    public List<Album> getChildren(Artist artist, long offset, long maxResults) {
        List<Album> albums = getDispatcher().getAlbumProcessor().getAlbumsForArtist(artist.getName(),
                offset > 1 ? offset - 1 : offset, 0L == offset ? maxResults - 1 : maxResults,
                util.isSortAlbumsByYear(artist.getName()), util.getGuestMusicFolders());
        if (albums.size() > 1 && 0L == offset) {
            Album firstElement = new Album();
            firstElement.setName(util.getResource("dlna.element.allalbums"));
            firstElement.setId(-1);
            firstElement.setComment(AlbumUpnpProcessor.ALL_BY_ARTIST + "_" + artist.getId());
            albums.add(0, firstElement);
        }
        return albums;
    }

    @Override
    public void addChild(DIDLContent didl, Album album) {
        didl.addContainer(getDispatcher().getAlbumProcessor().createContainer(album));
    }

    public URI createArtistArtURI(Artist artist) {
        return util.createURIWithToken(UriComponentsBuilder
                .fromUriString(util.getBaseUrl() + "/ext/" + ViewName.COVER_ART.value())
                .queryParam("id", coverArtLogic.createKey(artist)).queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

    public final BrowseResult toBrowseResult(ParamSearchResult<Artist> result) {
        DIDLContent didl = new DIDLContent();
        try {
            for (Artist item : result.getItems()) {
                addItem(didl, item);
            }
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }

}
