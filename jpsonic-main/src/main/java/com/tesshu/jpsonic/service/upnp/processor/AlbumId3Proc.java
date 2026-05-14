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

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.search.ParamSearchResult;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumId3Proc extends DirectChildrenContentProc<Album, MediaFile> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;

    public AlbumId3Proc(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService, AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3;
    }

    @Override
    public Container createContainer(Album album) {
        return factory.toAlbum(album);
    }

    @Override
    public List<Album> getDirectChildren(long offset, long count) {
        return albumDao
            .getAlphabeticalAlbums((int) offset, (int) count, false, true, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        return albumDao.getAlbumCount(util.getGuestFolders());
    }

    @Override
    public Album getDirectChild(String id) {
        return albumDao.getAlbum(Integer.parseInt(id));
    }

    @Override
    public List<MediaFile> getChildren(Album album, long count, long maxResults) {
        return mediaFileService
            .getSongsForAlbum(count, maxResults, album.getArtist(), album.getName());
    }

    @Override
    public int getChildSizeOf(Album album) {
        return album.getSongCount();
    }

    @Override
    public void addChild(DIDLContent parent, MediaFile song) {
        parent.addItem(factory.toMusicTrack(song));
    }

    public final BrowseResult toBrowseResult(ParamSearchResult<Album> searchResult) {
        DIDLContent parent = new DIDLContent();
        try {
            searchResult.getItems().forEach(album -> addDirectChild(parent, album));
            return createBrowseResult(parent, (int) parent.getCount(), searchResult.getTotalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
