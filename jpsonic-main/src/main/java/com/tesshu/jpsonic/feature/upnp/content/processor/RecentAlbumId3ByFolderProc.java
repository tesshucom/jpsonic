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
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.feature.upnp.content.CountLimitProc;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrAlbumLogic;
import org.springframework.stereotype.Controller;

@Controller
class RecentAlbumId3ByFolderProc extends AlbumId3ByFolderProc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;
    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);

    private final MediaFileProvider mediaFileProvider;
    private final AlbumProvider albumProvider;

    RecentAlbumId3ByFolderProc(MediaFileProvider mediaFileProvider, AlbumProvider albumProvider,
            FolderOrAlbumLogic folderOrAlbumLogic, UPnPDIDLFactory factory) {
        super(mediaFileProvider, albumProvider, folderOrAlbumLogic, factory);
        this.mediaFileProvider = mediaFileProvider;
        this.albumProvider = albumProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_ID3_BY_FOLDER;
    }

    @Override
    public List<AlbumOrSong> getChildren(FolderOrFAlbum folderOrAlbum, long offset, long count) {
        if (folderOrAlbum.isFolderAlbum()) {
            Album album = folderOrAlbum.getFolderAlbum().album();
            MusicFolder folder = folderOrAlbum.getFolderAlbum().folder();
            return mediaFileProvider
                .findChildren(List.of(folder), album, offset, count, EXCLUDED_TYPES)
                .stream()
                .map(AlbumOrSong::new)
                .toList();
        }
        MusicFolder folder = folderOrAlbum.getFolder();
        int albumCount = albumProvider.countAlbums(List.of(folder));
        int resultCount = toCount(offset, count, Math.min(albumCount, RECENT_COUNT));
        if (count == 0) {
            return Collections.emptyList();
        }
        return albumProvider
            .findNewestAlbums(List.of(folderOrAlbum.getFolder()), offset, resultCount)
            .stream()
            .map(AlbumOrSong::new)
            .toList();
    }
}
