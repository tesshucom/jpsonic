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

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.logic.CoverArtLogic;
import com.tesshu.jpsonic.service.JMediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RandomAlbumUpnpProcessor extends AlbumUpnpProcessor {

    private final UpnpProcessorUtil util;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomAlbumUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            JAlbumDao a, CoverArtLogic c, SearchService s, SettingsService ss) {
        super(d, u, m, a, c);
        this.util = u;
        this.searchService = s;
        this.settingsService = ss;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RANDOM_ALBUM);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomAlbum");
    }

    @Override
    public BrowseResult browseRoot(String filter, long offset, long maxResults, SortCriterion... orderBy)
            throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        int randomMax = settingsService.getDlnaRandomMax();
        if (offset < randomMax) {
            long count = randomMax < offset + maxResults ? randomMax - offset : maxResults;
            getItems(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
    public int getItemCount() {
        return settingsService.getDlnaRandomMax();
    }

    @Override
    public List<Album> getItems(long first, long maxResults) {
        int randomMax = settingsService.getDlnaRandomMax();
        int offset = (int) first;
        int count = (offset + (int) maxResults) > randomMax ? randomMax - offset : (int) maxResults;
        return searchService.getRandomAlbumsId3(count, offset, randomMax, util.getGuestMusicFolders());
    }

}
