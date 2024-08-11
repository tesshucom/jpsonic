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
 * (C) 2009 Sindre Mehus
 * (C) 2017 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class MediaFileByFolderProc extends MediaFileProc {

    private static final MediaType[] EXCLUDED_TYPES = Stream.of(MediaType.PODCAST, MediaType.VIDEO)
            .toArray(size -> new MediaType[size]);
    public static final int SINGLE_MUSIC_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;

    public MediaFileByFolderProc(UpnpProcessorUtil util, UpnpDIDLFactory factory, MediaFileService mediaFileService) {
        super(util, factory, mediaFileService);
        this.util = util;
        this.factory = factory;
        this.mediaFileService = mediaFileService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.MEDIA_FILE_BY_FOLDER;
    }

    @Override
    public Container createContainer(MediaFile entity) {
        int childSize = getChildSizeOf(entity);
        return switch (entity.getMediaType()) {
        case ALBUM -> factory.toAlbum(entity, childSize);
        case DIRECTORY -> isEmpty(entity.getArtist()) ? factory.toMusicFolder(getProcId(), entity, childSize)
                : factory.toArtist(entity, childSize);
        default -> throw new IllegalArgumentException("Unexpected value: " + entity.getMediaType());
        };
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        } else if (folders.size() == SINGLE_MUSIC_FOLDER) {
            MediaFile folder = mediaFileService.getMediaFileStrict(folders.get(0).getPathString());
            return getChildren(folder, offset, count);
        }
        return folders.stream().skip(offset).limit(count).map(folder -> mediaFileService.getMediaFile(folder.toPath()))
                .toList();
    }

    @Override
    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_MUSIC_FOLDER) {
            return mediaFileService.getChildSizeOf(folders, EXCLUDED_TYPES);
        }
        return folders.size();
    }
}
