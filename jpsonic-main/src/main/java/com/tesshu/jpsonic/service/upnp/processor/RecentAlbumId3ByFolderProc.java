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

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.upnp.processor.composite.AlbumOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFAlbum;
import org.springframework.stereotype.Service;

@Service
public class RecentAlbumId3ByFolderProc extends AlbumId3ByFolderProc implements CountLimitProc {

    private static final int RECENT_COUNT = 50;

    private final MediaFileService mediaFileService;
    private final AlbumDao albumDao;

    public RecentAlbumId3ByFolderProc(MediaFileService mediaFileService, AlbumDao albumDao,
            UpnpDIDLFactory factory, FolderOrAlbumLogic folderOrAlbumLogic) {
        super(mediaFileService, albumDao, factory, folderOrAlbumLogic);
        this.mediaFileService = mediaFileService;
        this.albumDao = albumDao;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.RECENT_ID3_BY_FOLDER;
    }

    @Override
    public List<AlbumOrSong> getChildren(FolderOrFAlbum folderOrAlbum, long firstResult,
            long maxResults) {
        if (folderOrAlbum.isFolderAlbum()) {
            Album album = folderOrAlbum.getFolderAlbum().album();
            return mediaFileService
                .getSongsForAlbum(firstResult, maxResults, album.getArtist(), album.getName())
                .stream()
                .map(AlbumOrSong::new)
                .toList();
        }
        int offset = (int) firstResult;
        MusicFolder folder = folderOrAlbum.getFolder();
        int albumCount = albumDao.getAlbumCount(List.of(folder));
        int count = toCount(firstResult, maxResults, Math.min(albumCount, RECENT_COUNT));
        if (count == 0) {
            return Collections.emptyList();
        }
        return albumDao
            .getNewestAlbums(offset, count, List.of(folderOrAlbum.getFolder()))
            .stream()
            .map(AlbumOrSong::new)
            .toList();
    }
}
