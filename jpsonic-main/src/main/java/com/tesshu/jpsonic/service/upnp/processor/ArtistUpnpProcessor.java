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

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class ArtistUpnpProcessor extends DirectChildrenContentProcessor<Artist, Album> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;

    public ArtistUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, ArtistDao artistDao,
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
    public int getDirectChildrenCount() {
        return artistDao.getArtistsCount(util.getGuestFolders());
    }

    @Override
    public List<Artist> getDirectChildren(long offset, long maxResults) {
        return artistDao.getAlphabetialArtists((int) offset, (int) maxResults, util.getGuestFolders());
    }

    @Override
    public Artist getDirectChild(String id) {
        return artistDao.getArtist(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Artist artist) {
        return albumDao.getAlbumsCountForArtist(artist.getName(), util.getGuestFolders());
    }

    @Override
    public List<Album> getChildren(Artist artist, long offset, long maxResults) {
        return albumDao.getAlbumsForArtist(offset, maxResults, artist.getName(),
                util.isSortAlbumsByYear(artist.getName()), util.getGuestFolders());
    }

    @Override
    public void addChild(DIDLContent didl, Album album) {
        didl.addContainer(factory.toAlbum(album));
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
