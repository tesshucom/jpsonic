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
import com.tesshu.jpsonic.service.upnp.ProcId;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.springframework.stereotype.Service;

@Service
public class RecentAlbumId3UpnpProcessor extends AlbumUpnpProcessor {

    private static final int RECENT_COUNT = 50;

    private final UpnpProcessorUtil util;
    private final AlbumDao albumDao;

    public RecentAlbumId3UpnpProcessor(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService, AlbumDao albumDao) {
        super(util, factory, mediaFileService, albumDao);
        this.util = util;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_ID3;
    }

    @Override
    public BrowseResult browseRoot(String filter, long offset, long max) throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        if (offset < RECENT_COUNT) {
            long count = RECENT_COUNT < offset + max ? RECENT_COUNT - offset : max;
            getDirectChildren(offset, count).forEach(a -> addItem(didl, a));
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public int getDirectChildrenCount() {
        int count = albumDao.getAlbumCount(util.getGuestFolders());
        return Math.min(count, RECENT_COUNT);
    }

    @Override
    public List<Album> getDirectChildren(long offset, long max) {
        return albumDao.getNewestAlbums((int) offset, (int) max, util.getGuestFolders());
    }
}
