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
 * (C) 2009 Sindre Mehus
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.GenreContainer;
import org.springframework.stereotype.Service;

@Service
public class SongByGenreUpnpProcessor extends DirectChildrenContentProcessor<Genre, MediaFile> {

    private final SearchService searchService;
    private final UpnpDIDLFactory factory;
    private final UpnpProcessorUtil util;

    public SongByGenreUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, SearchService searchService) {
        super();
        this.util = util;
        this.factory = factory;
        this.searchService = searchService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.SONG_BY_GENRE;
    }

    /**
     * Browses the top-level content of a type.
     */
    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults) throws ExecutionException {
        // we have to override this to do an index-based id.
        DIDLContent didl = new DIDLContent();
        List<Genre> selectedItems = getDirectChildren(firstResult, maxResults);
        for (int i = 0; i < selectedItems.size(); i++) {
            Genre item = selectedItems.get(i);
            didl.addContainer(createContainer(item, (int) (i + firstResult)));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public Container createContainer(Genre item) {
        return null;
    }

    protected Container createContainer(Genre item, int index) {
        GenreContainer container = new GenreContainer();
        container.setId(getProcId().getValue() + ProcId.CID_SEPA + index);
        container.setParentID(getProcId().getValue());
        container.setTitle(util.isGenreCountAvailable()
                ? item.getName().concat(" ").concat(Integer.toString(item.getSongCount())) : item.getName());
        container.setChildCount(item.getSongCount());
        return container;
    }

    @Override
    public int getDirectChildrenCount() {
        return searchService.getGenresCount(false);
    }

    @Override
    public List<Genre> getDirectChildren(long offset, long maxResults) {
        return searchService.getGenres(false, offset, maxResults);
    }

    @Override
    public Genre getDirectChild(String id) {
        int index = Integer.parseInt(id);
        List<Genre> genres = searchService.getGenres(false);
        if (genres.size() > index) {
            return genres.get(index);
        }
        return null;
    }

    @Override
    public int getChildSizeOf(Genre item) {
        return searchService.getSongsByGenres(item.getName(), 0, Integer.MAX_VALUE, util.getGuestFolders()).size();
    }

    @Override
    public List<MediaFile> getChildren(Genre item, long offset, long maxResults) {
        return searchService.getSongsByGenres(item.getName(), (int) offset, (int) maxResults, util.getGuestFolders());
    }

    @Override
    public void addChild(DIDLContent didl, MediaFile child) {
        didl.addItem(factory.toMusicTrack(child));
    }

}
