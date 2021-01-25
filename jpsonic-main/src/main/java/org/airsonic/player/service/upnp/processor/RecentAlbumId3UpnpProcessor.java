/*
  This file is part of Airsonic.

  Airsonic is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Airsonic is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp.processor;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.service.JMediaFileService;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.List;

@Service
public class RecentAlbumId3UpnpProcessor extends AlbumUpnpProcessor {

    private final UpnpProcessorUtil util;

    private final JAlbumDao albumDao;

    private final static int RECENT_COUNT = 51;

    public RecentAlbumId3UpnpProcessor(@Lazy UpnpProcessDispatcher d, UpnpProcessorUtil u, JMediaFileService m,
            JAlbumDao a, CoverArtLogic c) {
        super(d, u, m, a, c);
        this.util = u;
        this.albumDao = a;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_RECENT_ID3_PREFIX);
    }

    @PostConstruct
    @Override
    public void initTitle() {
        setRootTitleWithResource("dlna.title.recentAlbumsId3");
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
        // max to be able to return for view
        int count = albumDao.getAlbumCount(util.getAllMusicFolders());
        count = count > 1 ? count + 1 : count;
        return Math.min(count, RECENT_COUNT);
    }

    @Override
    public List<Album> getItems(long first, long max) {
        long offset = first;
        long limit = getItemCount();
        long count = max;
        if (offset == 0 && 0 != limit && 0 < max && (limit - offset) / max > 0) {
            count = max - 1;
        }
        if (offset != 0 && 0 != limit && 0 < max) {
            offset = offset - 1;
        }
        List<Album> albums = albumDao.getNewestAlbums((int) offset, (int) count, util.getAllMusicFolders());
        if (albums.size() > 1 && 0L == offset) {
            Album viewAll = new Album();
            viewAll.setName(util.getResource("dlna.element.allalbums"));
            viewAll.setId(-1);
            viewAll.setComment(AlbumUpnpProcessor.ALL_RECENT_ID3);
            albums.add(0, viewAll);
        }
        return albums;
    }

}
