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
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.springframework.stereotype.Controller;

@Controller
class AlbumByFolderProc extends MediaFileByFolderProc {

    private final MusicFolderProvider musicFolderProvider;
    private final MediaFileProvider mediaFileProvider;

    AlbumByFolderProc(MusicFolderProvider musicFolderProvider, MediaFileProvider mediaFileProvider,
            SettingsFacade settingsFacade, UPnPDIDLFactory factory) {
        super(musicFolderProvider, mediaFileProvider, settingsFacade, factory);
        this.musicFolderProvider = musicFolderProvider;
        this.mediaFileProvider = mediaFileProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_FOLDER;
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        } else if (folders.size() == SINGLE_MUSIC_FOLDER) {
            return mediaFileProvider
                .findAlbums(folders, RuntimeOrderPolicy.AlbumSortOrder.BY_ARTIST_AND_ALBUM, offset,
                        count);
        }
        return folders
            .stream()
            .skip(offset)
            .limit(count)
            .map(mediaFileProvider::requireMediaFile)
            .toList();
    }

    @Override
    public int getDirectChildrenCount() {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.size() == SINGLE_MUSIC_FOLDER) {
            return mediaFileProvider.countAlbums(folders);
        }
        return folders.size();
    }
}
