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

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import org.springframework.stereotype.Service;

@Service
public class RandomSongProc extends MediaFileByFolderProc implements CountLimitProc {

    private final UpnpProcessorUtil util;
    private final SearchService searchService;
    private final SettingsService settingsService;

    public RandomSongProc(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService, SearchService searchService,
            SettingsService settingsService) {
        super(util, factory, mediaFileService);
        this.util = util;
        this.searchService = searchService;
        this.settingsService = settingsService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_SONG;
    }

    @Override
    public List<MediaFile> getDirectChildren(long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int max = getDirectChildrenCount();
        int count = toCount(firstResult, maxResults, max);
        return searchService.getRandomSongs(count, offset, max, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        return settingsService.getDlnaRandomMax();
    }
}
