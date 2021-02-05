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

package org.airsonic.player.service.upnp.processor;

import java.util.List;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class RecentAlbumUpnpProcessor extends MediaFileUpnpProcessor {

    private final UpnpProcessorUtil util;

    private final JMediaFileService mediaFileService;

    private static final int RECENT_COUNT = 50;

    public RecentAlbumUpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            PlayerService p) {
        super(d, u, m, p);
        this.util = u;
        this.mediaFileService = m;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RECENT_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.recentAlbums");
    }

    @Override
    public BrowseResult browseRoot(String filter, long offset, long max, SortCriterion... orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        if (offset < RECENT_COUNT) {
            long count = RECENT_COUNT < offset + max ? RECENT_COUNT - offset : max;
            getItems(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    @Override
    public int getItemCount() {
        int count = mediaFileService.getAlbumCount(util.getAllMusicFolders());
        return Math.min(count, RECENT_COUNT);
    }

    @Override
    public List<MediaFile> getItems(long first, long max) {
        return mediaFileService.getNewestAlbums((int) first, (int) max, util.getAllMusicFolders());
    }

}
