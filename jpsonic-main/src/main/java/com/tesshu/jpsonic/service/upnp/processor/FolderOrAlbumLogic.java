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

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrAlbum;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;
import org.springframework.stereotype.Service;

@Service
public class FolderOrAlbumLogic {

    private static final int SINGLE_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final AlbumDao albumDao;
    private final MusicFolderDao musicFolderDao;

    public FolderOrAlbumLogic(UpnpProcessorUtil util, UpnpDIDLFactory factory, MusicFolderDao musicFolderDao,
            AlbumDao albumDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.albumDao = albumDao;
        this.musicFolderDao = musicFolderDao;
    }

    public Container createContainer(ProcId procId, FolderOrAlbum folderAlbum) {
        if (folderAlbum.isAlbum()) {
            MusicAlbum album = factory.toAlbum(folderAlbum.getAlbum());
            album.setId(procId.getValue() + ProcId.CID_SEPA + folderAlbum.createCompositeId());
            album.setParentID(procId.getValue());
            return album;
        }
        MusicFolder folder = folderAlbum.getFolder();
        int childCount = getChildSizeOf(folderAlbum);
        return factory.toMusicFolder(folder, procId, childCount);
    }

    public List<FolderOrAlbum> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() != SINGLE_FOLDER) {
            return folders.stream().skip(offset).limit(count).map(FolderOrAlbum::new).toList();
        }
        return albumDao.getAlphabeticalAlbums((int) offset, (int) count, false, true, Arrays.asList(folders.get(0)))
                .stream().map(FolderOrAlbum::new).toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return albumDao.getAlbumCount(util.getGuestFolders());
        }
        return util.getGuestFolders().size();
    }

    public FolderOrAlbum getDirectChild(String compositeId) {
        int id = FolderOrAlbum.toId(compositeId);
        if (FolderOrAlbum.isAlbumId(compositeId)) {
            Album album = albumDao.getAlbum(id);
            if (album == null) {
                throw new IllegalArgumentException("The specified Album cannot be found.");
            }
            return new FolderOrAlbum(album);
        }
        return new FolderOrAlbum(
                musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
    }

    public int getChildSizeOf(FolderOrAlbum folderOrAlbum) {
        if (folderOrAlbum.isAlbum()) {
            return folderOrAlbum.getAlbum().getSongCount();
        }
        return albumDao.getAlbumCount(util.getGuestFolders());
    }
}
