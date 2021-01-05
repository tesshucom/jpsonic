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

import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
public class SongByGenreUpnpProcessor extends UpnpContentProcessor <Genre, MediaFile> {

    private SearchService searchService;

    private final UpnpProcessorUtil util;

    public SongByGenreUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, SearchService s) {
        super(d, u);
        this.util = u;
        this.searchService = s;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_SONG_BY_GENRE_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.songbygenres");
    }

    /**
     * Browses the top-level content of a type.
     */
    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        // we have to override this to do an index-based id.
        DIDLContent didl = new DIDLContent();
        List<Genre> selectedItems = getItems(firstResult, maxResults);
        for (int i = 0; i < selectedItems.size(); i++) {
            Genre item = selectedItems.get(i);
            didl.addContainer(createContainer(item, (int) (i + firstResult)));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Deprecated
    @Override
    public Container createContainer(Genre item) {
        return null;
    }

    protected Container createContainer(Genre item, int index) {
        GenreContainer container = new GenreContainer();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + index);
        container.setParentID(getRootId());
        container.setTitle(util.isDlnaGenreCountVisible() ? item.getName().concat(" ").concat(Integer.toString(item.getSongCount())) : item.getName());
        container.setChildCount(item.getSongCount());
        return container;
    }

    @Override
    public int getItemCount() {
        return searchService.getGenresCount(false);
    }

    @Override
    public List<Genre> getItems(long offset, long maxResults) {
        return searchService.getGenres(false, offset, maxResults);
    }

    @Override
    public Genre getItemById(String id) {
        int index = Integer.parseInt(id);
        List<Genre> genres = searchService.getGenres(false);
        if (genres.size() > index) {
            return genres.get(index);
        }
        return null;
    }

    @Override
    public int getChildSizeOf(Genre item) {
        return searchService.getSongsByGenres(item.getName(), 0, Integer.MAX_VALUE, util.getAllMusicFolders()).size();
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long maxResults) {
        return searchService.getSongsByGenres(item.getName(), (int) offset, (int) maxResults, util.getAllMusicFolders());
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

}
