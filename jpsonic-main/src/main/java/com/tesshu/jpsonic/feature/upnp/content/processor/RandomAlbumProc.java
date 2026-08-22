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

import java.util.List;

import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.service.MediaFileService;
import org.springframework.stereotype.Controller;

@Controller
class RandomAlbumProc extends AlbumId3Proc implements CountLimitProc {

    private final UPnPProcessorUtil util;
    private final MediaSearchProvider mediaSearchProvider;
    private final SettingsFacade settingsFacade;

    RandomAlbumProc(UPnPProcessorUtil util, UPnPDIDLFactory factory,
            MediaFileService mediaFileService, AlbumDao albumDao,
            MediaSearchProvider mediaSearchProvider, SettingsFacade settingsFacade) {
        super(util, factory, mediaFileService, albumDao);
        this.util = util;
        this.mediaSearchProvider = mediaSearchProvider;
        this.settingsFacade = settingsFacade;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RANDOM_ALBUM;
    }

    @Override
    public int getDirectChildrenCount() {
        return settingsFacade.get(UPnPSKeys.options.randomMax);
    }

    @Override
    public List<Album> getDirectChildren(long firstResults, long maxResults) {
        int offset = (int) firstResults;
        int max = getDirectChildrenCount();
        int count = toCount(firstResults, maxResults, max);
        return mediaSearchProvider.getRandomAlbumsId3(count, offset, max, util.getGuestFolders());
    }
}
