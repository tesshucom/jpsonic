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

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.springframework.stereotype.Service;

@Service
public class RecentAlbumUpnpProcessor extends MediaFileUpnpProcessor implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final UpnpProcessorUtil util;
    private final MediaFileService mediaFileService;

    public RecentAlbumUpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory,
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
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults) throws ExecutionException {
        DIDLContent parent = new DIDLContent();
        int offset = (int) firstResult;
        int count = toCount(firstResult, maxResults, RECENT_COUNT);
        getDirectChildren(offset, count).forEach(a -> addDirectChild(parent, a));
        return createBrowseResult(parent, (int) parent.getCount(), getDirectChildrenCount());
    }

    @Override
    public List<MediaFile> getDirectChildren(long count, long offset) {
        return mediaFileService.getNewestAlbums((int) count, (int) offset, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        int count = (int) mediaFileService.getAlbumCount(util.getGuestFolders());
        return Math.min(count, RECENT_COUNT);
    }
}
