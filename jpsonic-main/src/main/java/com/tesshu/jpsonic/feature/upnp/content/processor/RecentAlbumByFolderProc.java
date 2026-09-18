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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.springframework.stereotype.Controller;

@Controller
class RecentAlbumByFolderProc extends MediaFileByFolderProc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final MusicFolderProvider musicFolderProvider;
    private final MediaFileProvider mediaFileProvider;

    RecentAlbumByFolderProc(MusicFolderProvider musicFolderProvider,
            MediaFileProvider mediaFileProvider, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super(musicFolderProvider, mediaFileProvider, settingsFacade, factory);
        this.musicFolderProvider = musicFolderProvider;
        this.mediaFileProvider = mediaFileProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_BY_FOLDER;
    }

    @Override
    public List<MediaFile> getChildren(MediaFile mediaFile, long offset, long count) {
        if (mediaFile.isAlbum()) {
            return super.getChildren(mediaFile, (int) offset, (int) count);
        }

        MusicFolder folder = musicFolderProvider
            .getGuestFolders()
            .stream()
            .filter(f -> f.pathString().equals(mediaFile.pathString()))
            .findFirst()
            .orElseGet(null);
        int albumCount = mediaFileProvider.countAlbums(List.of(folder));
        int resultCount = toCount(offset, count, Math.min(albumCount, RECENT_COUNT));
        if (resultCount == 0) {
            return Collections.emptyList();
        }
        return mediaFileProvider.findNewestAlbums(List.of(folder), offset, resultCount);
    }
}
