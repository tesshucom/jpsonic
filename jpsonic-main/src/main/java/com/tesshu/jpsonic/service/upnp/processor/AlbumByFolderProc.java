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

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import org.springframework.stereotype.Service;

@Service
public class AlbumByFolderProc extends MediaFileByFolderProc {

    private final UpnpProcessorUtil util;
    private final MediaFileService mediaFileService;

    public AlbumByFolderProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService) {
        super(util, factory, mediaFileService);
        this.util = util;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_FOLDER;
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        } else if (folders.size() == SINGLE_MUSIC_FOLDER) {
            return mediaFileService.getAlphabeticalAlbums((int) offset, (int) count, true, folders);
        }
        return folders.stream().skip(offset).limit(count).map(folder -> mediaFileService.getMediaFile(folder.toPath()))
                .toList();
    }

    @Override
    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_MUSIC_FOLDER) {
            return (int) mediaFileService.getAlbumCount(folders);
        }
        return folders.size();
    }
}
