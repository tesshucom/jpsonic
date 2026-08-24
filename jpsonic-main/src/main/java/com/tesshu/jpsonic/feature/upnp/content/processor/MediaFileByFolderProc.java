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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class MediaFileByFolderProc extends MediaFileProc {

    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);
    static final int SINGLE_MUSIC_FOLDER = 1;

    private final MusicFolderProvider musicFolderProvider;
    private final MediaFileProvider mediaFileProvider;
    private final UPnPDIDLFactory factory;

    MediaFileByFolderProc(MusicFolderProvider musicFolderProvider,
            MediaFileProvider mediaFileProvider, SettingsFacade settingsFacade,
            UPnPDIDLFactory factory) {
        super(musicFolderProvider, mediaFileProvider, settingsFacade, factory);
        this.musicFolderProvider = musicFolderProvider;
        this.mediaFileProvider = mediaFileProvider;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.MEDIA_FILE_BY_FOLDER;
    }

    @Override
    public Container createContainer(MediaFile entity) {
        int childSize = getChildSizeOf(entity);
        return switch (entity.type()) {
        case ALBUM -> factory.toAlbum(entity, childSize);
        case DIRECTORY ->
            isEmpty(entity.artist()) ? factory.toMusicFolder(getProcId(), entity, childSize)
                    : factory.toArtist(entity, childSize);
        default -> throw new IllegalArgumentException("Unexpected value: " + entity.type());
        };
    }

    @Override
    public List<MediaFile> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.isEmpty()) {
            return Collections.emptyList();
        } else if (folders.size() == SINGLE_MUSIC_FOLDER) {
            MediaFile folder = mediaFileProvider.requireMediaFile(folders.get(0));
            return getChildren(folder, offset, count);
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
            return mediaFileProvider.countChildren(folders, EXCLUDED_TYPES);
        }
        return folders.size();
    }
}
