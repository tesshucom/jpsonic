/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.upnp.processor;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
public class RandomSongUpnpProcessor extends MediaFileUpnpProcessor {

    private final UpnpProcessorUtil util;

    private final SearchService searchService;

    private final SettingsService settingsService;

    public RandomSongUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, MediaFileService m, PlayerService p, SearchService s, SettingsService ss) {
        super(d, u, m, p);
        this.util = u;
        this.searchService = s;
        this.settingsService = ss;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RANDOM_SONG);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomSong");
    }

    public BrowseResult browseRoot(String filter, long offset, long maxResults, SortCriterion[] orderBy) throws Exception {
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
    public List<MediaFile> getItems(long first, long maxResults) {
        int randomMax = settingsService.getDlnaRandomMax();
        int offset = (int) first;
        int count = (offset + (int) maxResults) > randomMax ? randomMax - offset : (int) maxResults;
        return searchService.getRandomSongs(count, offset, randomMax, util.getAllMusicFolders());
    }

}
