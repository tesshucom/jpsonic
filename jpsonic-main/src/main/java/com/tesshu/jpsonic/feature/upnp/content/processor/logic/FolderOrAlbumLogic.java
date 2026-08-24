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

package com.tesshu.jpsonic.feature.upnp.content.processor.logic;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Component;

@Component
public class FolderOrAlbumLogic {

    private static final int SINGLE_FOLDER = 1;

    private final MusicFolderProvider musicFolderProvider;
    private final AlbumProvider albumProvider;
    private final UPnPDIDLFactory factory;

    public FolderOrAlbumLogic(MusicFolderProvider musicFolderProvider, AlbumProvider albumProvider,
            UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.albumProvider = albumProvider;
        this.factory = factory;
    }

    public Container createContainer(ProcId procId, FolderOrFAlbum folderOrAlbum) {
        if (folderOrAlbum.isFolderAlbum()) {
            FolderAlbum folderAlbum = folderOrAlbum.getFolderAlbum();
            return factory.toAlbum(procId, folderAlbum, getChildSizeOf(folderAlbum));
        }
        MusicFolder folder = folderOrAlbum.getFolder();
        return factory.toMusicFolder(procId, folder, getChildSizeOf(folder));
    }

    public List<FolderOrFAlbum> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return albumProvider
                .findAlbums(folders, RuntimeOrderPolicy.AlbumSortOrder.DEFAULT, offset, count)
                .stream()
                .map(album -> new FolderAlbum(folders.get(0), album))
                .map(FolderOrFAlbum::new)
                .toList();
        }
        return folders.stream().skip(offset).limit(count).map(FolderOrFAlbum::new).toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return albumProvider.countAlbums(musicFolderProvider.getGuestFolders());
        }
        return musicFolderProvider.getGuestFolders().size();
    }

    private @Nullable MusicFolder getFolder(int folderId) {
        return musicFolderProvider
            .getGuestFolders()
            .stream()
            .filter(musicFolder -> musicFolder.id() == folderId)
            .findFirst()
            .orElseGet(null);
    }

    public FolderOrFAlbum getDirectChild(String id) {
        if (FolderAlbum.isCompositeId(id)) {
            MusicFolder folder = getFolder(FolderAlbum.parseFolderId(id));
            Album album = albumProvider.requireAlbum(FolderAlbum.parseAlbumId(id));
            if (album == null) {
                throw new IllegalArgumentException("The specified Album cannot be found.");
            }
            return new FolderOrFAlbum(new FolderAlbum(folder, album));
        }
        return new FolderOrFAlbum(getFolder(Integer.parseInt(id)));
    }

    private int getChildSizeOf(FolderAlbum folderAlbum) {
        return folderAlbum.album().songCount();
    }

    private int getChildSizeOf(MusicFolder musicFolder) {
        return albumProvider.countAlbums(List.of(musicFolder));
    }

    public int getChildSizeOf(FolderOrFAlbum folderOrAlbum) {
        if (folderOrAlbum.isFolderAlbum()) {
            return getChildSizeOf(folderOrAlbum.getFolderAlbum());
        }
        return getChildSizeOf(folderOrAlbum.getFolder());
    }
}
