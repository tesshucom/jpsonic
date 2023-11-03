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

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.springframework.stereotype.Service;

@Service
public class AlbumByGenreUpnpProcessor extends DirectChildrenContentProcessor<MediaFile, MediaFile> {

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final SearchService searchService;
    private final MediaFileService mediaFileService;

    public AlbumByGenreUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            SearchService searchService) {
        super();
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.searchService = searchService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_GENRE;
    }

    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults) throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        List<MediaFile> selectedItems = getDirectChildren(firstResult, maxResults);
        for (int i = 0; i < selectedItems.size(); i++) {
            MediaFile item = selectedItems.get(i);
            didl.addContainer(createContainer(item, Integer.toString((int) (i + firstResult))));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public Container createContainer(MediaFile item) {
        MusicAlbum container = new MusicAlbum();
        container.addProperty(factory.toAlbumArt(item));
        if (item.getArtist() != null) {
            container.addProperty(factory.toPerson(item.getArtist()));
        }
        container.setDescription(item.getComment());
        container.setId(ProcId.FOLDER.getValue() + ProcId.CID_SEPA + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
        if (mediaFileService.isRoot(item)) {
            container.setParentID(getProcId().getValue());
        } else {
            MediaFile parent = mediaFileService.getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        }
        return container;
    }

    private Container createContainer(MediaFile item, String index) {
        GenreContainer container = new GenreContainer();
        container.setParentID(getProcId().getValue());
        container.setId(getProcId().getValue() + ProcId.CID_SEPA + index);
        String comment = item.getComment();
        container
                .setTitle(util.isGenreCountAvailable() ? item.getName().concat(SPACE).concat(comment) : item.getName());
        container.setChildCount(isEmpty(comment) ? 0 : Integer.parseInt(comment));
        return container;
    }

    @Override
    public int getDirectChildrenCount() {
        return searchService.getGenresCount(true);
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long maxResults) {
        return searchService.getGenres(true, offset, maxResults).stream().map(this::toMediaFile).toList();
    }

    private MediaFile toMediaFile(Genre g) {
        MediaFile m = new MediaFile();
        m.setId(-1);
        m.setTitle(g.getName());
        if (0 != g.getAlbumCount()) {
            m.setComment(Integer.toString(g.getAlbumCount()));
        }
        m.setGenre(g.getName());
        return m;
    }

    @Override
    public MediaFile getDirectChild(String id) {
        int index = Integer.parseInt(id);
        List<Genre> genres = searchService.getGenres(true);
        if (genres.size() > index) {
            return toMediaFile(genres.get(index));
        }
        return null;
    }

    @Override
    public int getChildSizeOf(MediaFile item) {
        return searchService.getAlbumsByGenres(item.getName(), 0, Integer.MAX_VALUE, util.getGuestFolders()).size();
    }

    @Override
    public List<MediaFile> getChildren(MediaFile item, long offset, long maxResults) {
        if (-1 == item.getId()) {
            return searchService.getAlbumsByGenres(item.getGenre(), (int) offset, (int) maxResults,
                    util.getGuestFolders());
        }
        return mediaFileService.getSongsForAlbum(offset, maxResults, item);
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile song) {
        if (isEmpty(song.getMediaType())) {
            didl.addItem(factory.toMusicTrack(song));
        } else {
            didl.addContainer(createContainer(song));
        }
    }
}
