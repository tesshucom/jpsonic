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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.model.SearchResult;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.SearchResultProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.concurrent.ConcurrentUtils;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class ArtistProc extends DirectChildrenContentProc<Artist, Album>
        implements SearchResultProcessor<Artist> {

    private final UPnPProcessorUtil util;
    private final UPnPDIDLFactory factory;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;

    ArtistProc(UPnPProcessorUtil util, UPnPDIDLFactory factory, ArtistDao artistDao,
            AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ARTIST;
    }

    @Override
    public Container createContainer(Artist artist) {
        return factory.toArtist(artist);
    }

    @Override
    public List<Artist> getDirectChildren(long offset, long count) {
        return artistDao.getAlphabetialArtists((int) offset, (int) count, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        return artistDao.getArtistsCount(util.getGuestFolders());
    }

    @Override
    public Artist getDirectChild(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public List<Album> getChildren(Artist artist, long offset, long count) {
        return albumDao
            .getAlbumsForArtist(offset, count, artist.getName(),
                    util.isSortAlbumsByYear(artist.getName()), util.getGuestFolders());
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        return albumDao.getAlbumsCountForArtist(artist.getName(), util.getGuestFolders());
    }

    @Override
    public void addChild(DIDLContent parent, Album album) {
        parent.addContainer(factory.toAlbum(album));
    }

    @Override
    public final BrowseResult toBrowseResult(SearchResult<Artist> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.items().forEach(artist -> addDirectChild(parent, artist));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.totalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
