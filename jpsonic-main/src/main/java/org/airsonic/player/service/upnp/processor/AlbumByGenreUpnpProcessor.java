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

package org.airsonic.player.service.upnp.processor;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AlbumByGenreUpnpProcessor extends UpnpContentProcessor<MediaFile, MediaFile> {

    private final UpnpProcessorUtil util;

    private final SearchService searchService;

    private final JMediaFileService mediaFileService;

    public AlbumByGenreUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            SearchService s) {
        super(d, u);
        this.util = u;
        this.mediaFileService = m;
        this.searchService = s;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_ALBUM_BY_GENRE_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.albumbygenres");
    }

    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion... orderBy)
            throws Exception {
        DIDLContent didl = new DIDLContent();
        List<MediaFile> selectedItems = getItems(firstResult, maxResults);
        for (int i = 0; i < selectedItems.size(); i++) {
            MediaFile item = selectedItems.get(i);
            didl.addContainer(createContainer(item, Integer.toString((int) (i + firstResult))));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
    public Container createContainer(MediaFile item) {
        MusicAlbum container = new MusicAlbum();
        container.setAlbumArtURIs(new URI[] { getDispatcher().getMediaFileProcessor().createAlbumArtURI(item) });
        if (item.getArtist() != null) {
            container.setArtists(getDispatcher().getAlbumProcessor().getAlbumArtists(item.getArtist()));
        }
        container.setDescription(item.getComment());
        container.setId(UpnpProcessDispatcher.CONTAINER_ID_FOLDER_PREFIX + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR
                + item.getId());
        container.setTitle(item.getName());
        container.setChildCount(getChildSizeOf(item));
        if (!mediaFileService.isRoot(item)) {
            MediaFile parent = mediaFileService.getParentOf(item);
            if (parent != null) {
                container.setParentID(String.valueOf(parent.getId()));
            }
        } else {
            container.setParentID(getRootId());
        }
        return container;
    }

    private final Container createContainer(MediaFile item, String index) {
        GenreContainer container = new GenreContainer();
        container.setParentID(getRootId());
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + index);
        container.setTitle(util.isDlnaGenreCountVisible() ? item.getName().concat(SPACE).concat(item.getComment())
                : item.getName());
        container.setChildCount(isEmpty(item.getComment()) ? 0 : Integer.parseInt(item.getComment()));
        return container;
    }

    @Override
    public int getItemCount() {
        return searchService.getGenresCount(true);
    }

    private final Function<Genre, MediaFile> toMediaFile = (g) -> {
        MediaFile m = new MediaFile();
        m.setId(-1);
        m.setTitle(g.getName());
        if (0 != g.getAlbumCount()) {
            m.setComment(Integer.toString(g.getAlbumCount()));
        }
        m.setGenre(g.getName());
        return m;
    };

    @Override
    public List<MediaFile> getItems(long offset, long maxResults) {
        return searchService.getGenres(true, offset, maxResults).stream().map(toMediaFile).collect(Collectors.toList());
    }

    @Override
    public MediaFile getItemById(String id) {
        int index = Integer.parseInt(id);
        List<Genre> genres = searchService.getGenres(true);
        if (genres.size() > index) {
            return toMediaFile.apply(genres.get(index));
        }
        return null;
    }

    @Override
    public int getChildSizeOf(MediaFile item) {
        return searchService.getAlbumsByGenres(item.getName(), 0, Integer.MAX_VALUE, util.getAllMusicFolders()).size();
    }

    @Override
    public List<MediaFile> getChildren(MediaFile item, long offset, long maxResults) {
        if (-1 == item.getId()) {
            return searchService.getAlbumsByGenres(item.getGenre(), (int) offset, (int) maxResults,
                    util.getAllMusicFolders());
        }
        return mediaFileService.getSongsForAlbum(offset, maxResults, item);
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        if (isEmpty(child.getMediaType())) {
            didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
        } else {
            didl.addContainer(createContainer(child));
        }
    }

}
