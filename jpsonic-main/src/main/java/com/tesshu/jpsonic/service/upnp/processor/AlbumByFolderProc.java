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
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrAlbum;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class AlbumByFolderProc extends DirectChildrenContentProc<FolderOrAlbum, AlbumOrSong> {

    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;
    private final UpnpDIDLFactory factory;
    private final FolderOrAlbumLogic deligate;

    public AlbumByFolderProc(MediaFileService mediaFileService, AlbumDao albumDao, UpnpDIDLFactory factory,
            FolderOrAlbumLogic folderOrAlbumLogic) {
        super();
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
        this.factory = factory;
        this.deligate = folderOrAlbumLogic;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.ALBUM_BY_FOLDER;
    }

    @Override
    public Container createContainer(FolderOrAlbum folderOrAlbum) {
        return deligate.createContainer(getProcId(), folderOrAlbum);
    }

    @Override
    public List<FolderOrAlbum> getDirectChildren(long offset, long count) {
        return deligate.getDirectChildren(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return deligate.getDirectChildrenCount();
    }

    @Override
    public FolderOrAlbum getDirectChild(String compositeId) {
        return deligate.getDirectChild(compositeId);
    }

    @Override
    public List<AlbumOrSong> getChildren(FolderOrAlbum folderOrAlbum, long offset, long count) {

        if (folderOrAlbum.isAlbum()) {
            Album album = folderOrAlbum.getAlbum();
            return mediaFileService.getSongsForAlbum(offset, count, album.getArtist(), album.getName()).stream()
                    .map(AlbumOrSong::new).toList();
        }
        return albumDao
                .getAlphabeticalAlbums((int) offset, (int) count, false, true, Arrays.asList(folderOrAlbum.getFolder()))
                .stream().map(AlbumOrSong::new).toList();
    }

    @Override
    public int getChildSizeOf(FolderOrAlbum folderOrAlbum) {
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
