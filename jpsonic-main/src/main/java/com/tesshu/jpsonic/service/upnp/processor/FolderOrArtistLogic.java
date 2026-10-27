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

import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderArtist;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFArtist;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class FolderOrArtistLogic {

    private static final int SINGLE_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;

    public FolderOrArtistLogic(UpnpProcessorUtil util, UpnpDIDLFactory factory,
            ArtistDao artistDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.artistDao = artistDao;
    }

    public Container createContainer(ProcId procId, FolderArtist folderArtist) {
        int childCount = getChildSizeOf(folderArtist);
        return factory.toArtist(procId, folderArtist, childCount);
    }

    public Container createContainer(ProcId procId, FolderOrFArtist folderOrArtist) {
        if (folderOrArtist.isFolderArtist()) {
            return createContainer(procId, folderOrArtist.getFolderArtist());
        }
        MusicFolder folder = folderOrArtist.getFolder();
        int childCount = getChildSizeOf(folder);
        return factory.toMusicFolder(procId, folder, childCount);
    }

    public List<FolderOrFArtist> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() != SINGLE_FOLDER) {
            return folders.stream().skip(offset).limit(count).map(FolderOrFArtist::new).toList();
        }
        MusicFolder folder = folders.get(0);
        return artistDao
            .getAlphabetialArtists((int) offset, (int) count, Arrays.asList(folders.get(0)))
            .stream()
            .map(artist -> new FolderArtist(folder, artist))
            .map(FolderOrFArtist::new)
            .toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return artistDao.getArtistsCount(util.getGuestFolders());
        }
        return util.getGuestFolders().size();
    }

    private @Nullable MusicFolder getFolder(int folderId) {
        return util
            .getGuestFolders()
            .stream()
            .filter(musicFolder -> musicFolder.getId() == folderId)
            .findFirst()
            .orElseGet(null);
    }

    public FolderOrFArtist getDirectChild(String id) {
        if (FolderArtist.isCompositeId(id)) {
            Artist artist = artistDao.getArtist(FolderArtist.parseArtistId(id));
            MusicFolder folder = getFolder(FolderArtist.parseFolderId(id));
            return new FolderOrFArtist(new FolderArtist(folder, artist));
        }
        return new FolderOrFArtist(getFolder(Integer.parseInt(id)));
    }

    private int getChildSizeOf(MusicFolder folder) {
        return artistDao.getArtistsCount(Arrays.asList(folder));
    }

    private int getChildSizeOf(FolderArtist folderArtist) {
        return folderArtist.artist().getAlbumCount();
    }

    public int getChildSizeOf(FolderOrFArtist folderOrArtist) {
        if (folderOrArtist.isFolderArtist()) {
            return getChildSizeOf(folderOrArtist.getFolderArtist());
        }
        return getChildSizeOf(folderOrArtist.getFolder());
    }
}
