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

import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import org.springframework.stereotype.Service;

@Service
public class AlbumProc extends MediaFileProc {

    private final UpnpProcessorUtil util;
    private final MediaFileService mediaFileService;

    public AlbumProc(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            MediaFileService mediaFileService) {
        super(util, factory, mediaFileService);
        this.util = util;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM;
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        return mediaFileService
            .getAlphabeticalAlbums((int) offset, (int) count, true, util.getGuestFolders());
    }

    @Override
    public int getDirectChildrenCount() {
        return (int) mediaFileService.getAlbumCount(util.getGuestFolders());
    }
}
