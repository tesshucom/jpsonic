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
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumUpnpProcessor extends DirectChildrenContentProcessor<Album, MediaFile> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;

    public AlbumUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM;
    }

    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults) throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        List<Album> selectedItems = albumDao.getAlphabeticalAlbums((int) firstResult, (int) maxResults, false, true,
                util.getGuestFolders());
        for (Album item : selectedItems) {
            addItem(didl, item);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public Container createContainer(Album album) {
        return factory.toAlbum(album);
    }

    @Override
    public int getDirectChildrenCount() {
        return albumDao.getAlbumCount(util.getGuestFolders());
    }

    @Override
    public List<Album> getDirectChildren(long offset, long maxResults) {
        return albumDao.getAlphabeticalAlbums((int) offset, (int) maxResults, false, true, util.getGuestFolders());
    }

    @Override
    public Album getDirectChild(String id) {
        return albumDao.getAlbum(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(Album album) {
        return mediaFileService.getSongsCountForAlbum(album.getArtist(), album.getName());
    }

    @Override
    public List<MediaFile> getChildren(Album album, long offset, long maxResults) {
        return mediaFileService.getSongsForAlbum(offset, maxResults, album.getArtist(), album.getName());
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        return albumDao.getAlbumsCountForArtist(artist, musicFolders);
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile song) {
        didl.addItem(factory.toMusicTrack(song));
    }

    public final BrowseResult toBrowseResult(ParamSearchResult<Album> result) {
        DIDLContent didl = new DIDLContent();
        try {
            for (Album item : result.getItems()) {
                addItem(didl, item);
            }
            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return null;
        }
    }
}
