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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import org.springframework.stereotype.Controller;

@Controller
class RecentAlbumProc extends MediaFileByFolderProc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final UPnPProcessorUtil util;
    private final MediaFileService mediaFileService;

    RecentAlbumProc(UPnPProcessorUtil util, UPnPDIDLFactory factory,
            MediaFileService mediaFileService) {
        super(util, factory, mediaFileService);
        this.util = util;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT;
    }

    @Override
    public List<MediaFile> getDirectChildren(long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int max = getDirectChildrenCount();
        int count = toCount(firstResult, maxResults, max);
        if (count == 0) {
            return Collections.emptyList();
        }
        return mediaFileService.getNewestAlbums(offset, count, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        int count = (int) mediaFileService.getAlbumCount(util.getGuestFolders());
        return Math.min(count, RECENT_COUNT);
    }
}
