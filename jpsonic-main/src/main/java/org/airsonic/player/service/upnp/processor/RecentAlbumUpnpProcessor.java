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

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
public class RecentAlbumUpnpProcessor extends MediaFileUpnpProcessor {

    private final static int RECENT_COUNT = 50;

    private final MediaFileDao mediaFileDao;

    public RecentAlbumUpnpProcessor(UpnpProcessDispatcher dispatcher, SettingsService settingsService, SearchService searchService, MediaFileDao mediaFileDao, MediaFileService mediaFileService,
            JWTSecurityService jwtSecurityService, PlayerService playerService, TranscodingService transcodingService) {
        super(dispatcher, settingsService, searchService, mediaFileDao, mediaFileService, jwtSecurityService, playerService, transcodingService);
        this.mediaFileDao = mediaFileDao;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RECENT_PREFIX);
    }

    public BrowseResult browseRoot(String filter, long offset, long max, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        if (offset < RECENT_COUNT) {
            long count = RECENT_COUNT < offset + max ? RECENT_COUNT - offset : max;
            getItems(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
    public int getItemCount() {
        int count = mediaFileDao.getAlbumCount(getAllMusicFolders());
        return Math.min(count, RECENT_COUNT);
    }

    @Override
    public List<MediaFile> getItems(long first, long max) {
        return mediaFileDao.getNewestAlbums(first, max, getAllMusicFolders());
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.recentAlbums");
    }

}
