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
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
public class RandomSongUpnpProcessor extends MediaFileUpnpProcessor {

    private final UpnpProcessorUtil util;

    private final SearchService searchService;

    private final static int RANDOM_MAX = 50;

    public RandomSongUpnpProcessor(UpnpProcessDispatcher d, UpnpProcessorUtil u, MediaFileService m, PlayerService p, SearchService s) {
        super(d, u, m, p);
        this.util = u;
        this.searchService = s;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RANDOM_SONG);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.randomSong");
    }

    public BrowseResult browseRoot(String filter, long offset, long max, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        if (offset < RANDOM_MAX) {
            long count = RANDOM_MAX < offset + max ? RANDOM_MAX - offset : max;
            getItems(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
    public int getItemCount() {
        // Create a fixed 50 list considering speed.
        return RANDOM_MAX;
    }

    @Override
    public List<MediaFile> getItems(long first, long max) {
        int offset = (int) first;
        int count = (offset + (int) max) > RANDOM_MAX ? RANDOM_MAX - offset : (int) max;
        return searchService.getRandomSongs(count, offset, RANDOM_MAX, util.getAllMusicFolders());
    }

}
