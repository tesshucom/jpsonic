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

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import org.springframework.stereotype.Controller;

@Controller
class RecentAlbumId3Proc extends AlbumId3Proc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final MusicFolderProvider musicFolderProvider;
    private final AlbumProvider albumProvider;

    RecentAlbumId3Proc(MusicFolderProvider musicFolderProvider, AlbumProvider albumProvider,
            MediaFileProvider mediaFileProvider, UPnPDIDLFactory factory) {
        super(musicFolderProvider, albumProvider, mediaFileProvider, factory);
        this.musicFolderProvider = musicFolderProvider;
        this.albumProvider = albumProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_ID3;
    }

    @Override
    public List<Album> getDirectChildren(long offset, long count) {
        return albumProvider.findNewestAlbums(musicFolderProvider.getGuestFolders(), offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return Math
            .min(albumProvider.countAlbums(musicFolderProvider.getGuestFolders()), RECENT_COUNT);
    }
}
