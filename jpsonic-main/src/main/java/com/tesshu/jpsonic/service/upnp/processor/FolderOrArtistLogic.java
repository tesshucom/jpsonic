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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.service.upnp.composite.FolderOrArtist;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.springframework.stereotype.Service;

@Service
public class FolderOrArtistLogic {

    private static final int SINGLE_FOLDER = 1;

    private final UpnpProcessorUtil util;
    private final UpnpDIDLFactory factory;
    private final ArtistDao artistDao;
    private final MusicFolderDao musicFolderDao;

    public FolderOrArtistLogic(UpnpProcessorUtil util, UpnpDIDLFactory factory, MusicFolderDao musicFolderDao,
            ArtistDao artistDao) {
        super();
        this.util = util;
        this.factory = factory;
        this.artistDao = artistDao;
        this.musicFolderDao = musicFolderDao;
    }

    public Container createContainer(ProcId procId, FolderOrArtist folderArtist) {
        if (folderArtist.isArtist()) {
            MusicArtist artist = factory.toArtist(folderArtist.getArtist());
            artist.setId(procId.getValue() + ProcId.CID_SEPA + folderArtist.createCompositeId());
            artist.setParentID(procId.getValue());
            return artist;
        }
        MusicFolder folder = folderArtist.getFolder();
        int childCount = getChildSizeOf(folderArtist);
        return factory.toMusicFolder(folder, procId, childCount);
    }

    public List<FolderOrArtist> getDirectChildren(long offset, long count) {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() != SINGLE_FOLDER) {
            return folders.stream().skip(offset).limit(count).map(FolderOrArtist::new).toList();
        }
        return artistDao.getAlphabetialArtists((int) offset, (int) count, Arrays.asList(folders.get(0))).stream()
                .map(FolderOrArtist::new).toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = util.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return artistDao.getArtistsCount(util.getGuestFolders());
        }
        return util.getGuestFolders().size();
    }

    public FolderOrArtist getDirectChild(String compositeId) {
        int id = FolderOrArtist.toId(compositeId);
        if (FolderOrArtist.isArtistId(compositeId)) {
            Artist artist = artistDao.getArtist(id);
            if (artist == null) {
                throw new IllegalArgumentException("The specified Artist cannot be found.");
            }
            return new FolderOrArtist(artist);
        }
        return new FolderOrArtist(
                musicFolderDao.getAllMusicFolders().stream().filter(m -> id == m.getId()).findFirst().get());
    }

    public int getChildSizeOf(FolderOrArtist folderOrArtist) {
        if (folderOrArtist.isArtist()) {
            return folderOrArtist.getArtist().getAlbumCount();
        }
        return artistDao.getArtistsCount(util.getGuestFolders());
    }
}
