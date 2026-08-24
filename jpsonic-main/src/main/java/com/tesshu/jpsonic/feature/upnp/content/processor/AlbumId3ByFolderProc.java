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

import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.logic.FolderOrAlbumLogic;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class AlbumId3ByFolderProc extends DirectChildrenContentProc<FolderOrFAlbum, AlbumOrSong> {

    private static final MediaFile.Type[] EXCLUDED_TYPES = Stream
        .of(MediaFile.Type.PODCAST, MediaFile.Type.VIDEO)
        .toArray(size -> new MediaFile.Type[size]);

    private final MediaFileProvider mediaFileProvider;
    private final AlbumProvider albumProvider;
    private final FolderOrAlbumLogic deligate;
    private final UPnPDIDLFactory factory;

    AlbumId3ByFolderProc(MediaFileProvider mediaFileProvider, AlbumProvider albumProvider,
            FolderOrAlbumLogic folderOrAlbumLogic, UPnPDIDLFactory factory) {
        super();
        this.mediaFileProvider = mediaFileProvider;
        this.albumProvider = albumProvider;
        this.deligate = folderOrAlbumLogic;
        this.factory = factory;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_ID3_BY_FOLDER;
    }

    @Override
    public Container createContainer(FolderOrFAlbum folderOrAlbum) {
        return deligate.createContainer(getProcId(), folderOrAlbum);
    }

    @Override
    public List<FolderOrFAlbum> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount();
    }

    @Override
    public FolderOrFAlbum getDirectChild(String compositeId) {
        return deligate.getDirectChild(compositeId);
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
        return albumProvider
            .findAlbums(List.of(folderOrAlbum.getFolder()),
                    RuntimeOrderPolicy.AlbumSortOrder.DEFAULT, offset, count)
            .stream()
            .map(AlbumOrSong::new)
            .toList();
    }

    @Override
    public int getChildSizeOf(FolderOrFAlbum folderOrAlbum) {
        return deligate.getChildSizeOf(folderOrAlbum);
    }

    @Override
    public void addChild(DIDLContent parent, AlbumOrSong albumOrSong) {
        if (albumOrSong.isAlbum()) {
            parent.addContainer(factory.toAlbum(albumOrSong.getAlbum()));
        } else {
            parent.addItem(factory.toMusicTrack(albumOrSong.getSong()));
        }
    }
}
