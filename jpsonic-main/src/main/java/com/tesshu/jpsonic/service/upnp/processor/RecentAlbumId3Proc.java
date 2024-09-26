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

import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.service.MediaFileService;
import org.springframework.stereotype.Service;

@Service
public class RecentAlbumId3Proc extends AlbumId3Proc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final UpnpProcessorUtil util;
    private final AlbumDao albumDao;

    public RecentAlbumId3Proc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService,
            AlbumDao albumDao) {
        super(util, factory, mediaFileService, albumDao);
        this.util = util;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_ID3;
    }

    @Override
    public List<Album> getDirectChildren(long firstResult, long maxResults) {
        int offset = (int) firstResult;
        int directChildrenCount = getDirectChildrenCount();
        int count = toCount(firstResult, maxResults, directChildrenCount);
        if (count == 0) {
            return Collections.emptyList();
        }
        return albumDao.getNewestAlbums(offset, count, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        return Math.min(albumDao.getAlbumCount(util.getGuestFolders()), RECENT_COUNT);
    }
}
