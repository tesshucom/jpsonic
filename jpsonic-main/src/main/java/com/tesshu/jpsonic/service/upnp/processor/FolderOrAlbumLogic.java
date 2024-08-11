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

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFAlbum;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class FolderOrAlbumLogic {

    private static final int SINGLE_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final AlbumDao albumDao;

    public FolderOrAlbumLogic(UpnpProcessorUtil util, UpnpDIDLFactory factory, AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.albumDao = albumDao;
    }

    public Container createContainer(ProcId procId, FolderOrFAlbum folderOrAlbum) {
        if (folderOrAlbum.isFolderAlbum()) {
            FolderAlbum folderAlbum = folderOrAlbum.getFolderAlbum();
            return factory.toAlbum(procId, folderAlbum, getChildSizeOf(folderAlbum));
        }
        MusicFolder folder = folderOrAlbum.getFolder();
        return factory.toMusicFolder(folder, procId, getChildSizeOf(folder));
    }

    public List<FolderOrFAlbum> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            MusicFolder folder = folders.get(0);
            return albumDao.getAlphabeticalAlbums((int) offset, (int) count, false, true, List.of(folders.get(0)))
                    .stream().map(album -> new FolderAlbum(folder, album)).map(FolderOrFAlbum::new).toList();
        }
        return folders.stream().skip(offset).limit(count).map(FolderOrFAlbum::new).toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return albumDao.getAlbumCount(util.getGuestFolders());
        }
        return util.getGuestFolders().size();
    }

    private @Nullable MusicFolder getFolder(int folderId) {
        return util.getGuestFolders().stream().filter(musicFolder -> musicFolder.getId() == folderId).findFirst()
                .orElseGet(null);
    }

    public FolderOrFAlbum getDirectChild(String id) {
        if (FolderAlbum.isCompositeId(id)) {
            MusicFolder folder = getFolder(FolderAlbum.parseFolderId(id));
            Album album = albumDao.getAlbum(FolderAlbum.parseAlbumId(id));
            if (album == null) {
                throw new IllegalArgumentException("The specified Album cannot be found.");
            }
            return new FolderOrFAlbum(new FolderAlbum(folder, album));
        }
        return new FolderOrFAlbum(getFolder(Integer.parseInt(id)));
    }

    private int getChildSizeOf(FolderAlbum folderAlbum) {
        return folderAlbum.album().getSongCount();
    }

    private int getChildSizeOf(MusicFolder musicFolder) {
        return albumDao.getAlbumCount(List.of(musicFolder));
    }

    public int getChildSizeOf(FolderOrFAlbum folderOrAlbum) {
        if (folderOrAlbum.isFolderAlbum()) {
            return getChildSizeOf(folderOrAlbum.getFolderAlbum());
        }
        return getChildSizeOf(folderOrAlbum.getFolder());
    }
}
