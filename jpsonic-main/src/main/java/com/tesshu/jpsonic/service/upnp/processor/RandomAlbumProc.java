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

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.springframework.stereotype.Service;

@Service
public class RandomAlbumProc extends AlbumProc implements CountLimitProc {

    private final UpnpProcessorUtil util;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomAlbumProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
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
    public int getDirectChildrenCount() {
        return settingsService.getDlnaRandomMax();
    }

    @Override
    public List<Album> getDirectChildren(long firstResults, long maxResults) {
        int offset = (int) firstResults;
        int max = getDirectChildrenCount();
        int count = toCount(firstResults, maxResults, max);
        return searchService.getRandomAlbumsId3((int) count, (int) offset, max, util.getGuestFolders());
    }
}
