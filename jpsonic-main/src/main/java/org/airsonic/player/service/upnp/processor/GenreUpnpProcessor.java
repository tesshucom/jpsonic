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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
public class GenreUpnpProcessor extends UpnpContentProcessor <Genre, MediaFile> {

    private SearchService searchService;
    
    public GenreUpnpProcessor(SearchService searchService) {
        super();
        this.searchService = searchService;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_GENRE_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dnla.title.genres");
    }

    /**
     * Browses the top-level content of a type.
     */
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
    public Container createContainer(Genre item) {
        // genre uses index because we don't have a proper id
        return null;
    }

    private final Container createContainer(Genre item, int index) {
        GenreContainer container = new GenreContainer();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + index);
        container.setParentID(getRootId());
        container.setTitle(item.getName());
        container.setChildCount(item.getAlbumCount());
        return container;
    }

    @Override
    public int getItemCount() {
        return searchService.getGenresCount();
    }

    @Override
    public List<Genre> getItems(long offset, long maxResults) {
        return searchService.getGenres(false, offset, maxResults);
    }

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
        return searchService.getSongsCountByGenres(item.getName(), getAllMusicFolders());
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long maxResults) {
        return searchService.getSongsByGenres(item.getName(), (int) offset, (int) maxResults, getAllMusicFolders());
    }

    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(getDispatcher().getMediaFileProcessor().createItem(child));
    }

}
