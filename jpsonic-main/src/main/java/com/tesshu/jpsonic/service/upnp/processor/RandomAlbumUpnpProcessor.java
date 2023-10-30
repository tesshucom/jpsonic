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
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.springframework.stereotype.Service;

@Service
public class RandomAlbumUpnpProcessor extends AlbumUpnpProcessor {

    private final UpnpProcessorUtil util;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomAlbumUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            AlbumDao albumDao, SearchService searchService, SettingsService settingsService) {
        super(util, factory, mediaFileService, albumDao);
        this.util = util;
        this.searchService = searchService;
        this.settingsService = settingsService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_ALBUM;
    }

    @Override
    public BrowseResult browseRoot(String filter, long offset, long maxResults) throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        int randomMax = settingsService.getDlnaRandomMax();
        if (offset < randomMax) {
            long count = randomMax < offset + maxResults ? randomMax - offset : maxResults;
            getDirectChildren(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public int getDirectChildrenCount() {
        return settingsService.getDlnaRandomMax();
    }

    @Override
    public List<Album> getDirectChildren(long first, long maxResults) {
        int randomMax = settingsService.getDlnaRandomMax();
        int offset = (int) first;
        int count = (offset + (int) maxResults) > randomMax ? randomMax - offset : (int) maxResults;
        return searchService.getRandomAlbumsId3(count, offset, randomMax, util.getGuestFolders());
    }
}
