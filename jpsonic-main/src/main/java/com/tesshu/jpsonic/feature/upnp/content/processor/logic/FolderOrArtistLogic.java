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

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFArtist;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Component;

@Component
public class FolderOrArtistLogic {

    private static final int SINGLE_FOLDER = 1;

    private final MusicFolderProvider musicFolderProvider;
    private final ArtistProvider artistProvider;
    private final UPnPDIDLFactory factory;

    public FolderOrArtistLogic(MusicFolderProvider musicFolderProvider,
            ArtistProvider artistProvider, UPnPDIDLFactory factory) {
        super();
        this.musicFolderProvider = musicFolderProvider;
        this.artistProvider = artistProvider;
        this.factory = factory;
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
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.size() != SINGLE_FOLDER) {
            return folders.stream().skip(offset).limit(count).map(FolderOrFArtist::new).toList();
        }
        MusicFolder folder = folders.get(0);
        return artistProvider
            .findArtists(Arrays.asList(folders.get(0)), offset, count)
            .stream()
            .map(artist -> new FolderArtist(folder, artist))
            .map(FolderOrFArtist::new)
            .toList();
    }

    public int getDirectChildrenCount() {
        List<MusicFolder> folders = musicFolderProvider.getGuestFolders();
        if (folders.size() == SINGLE_FOLDER) {
            return artistProvider.countArtists(musicFolderProvider.getGuestFolders());
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

    public FolderOrFArtist getDirectChild(String id) {
        if (FolderArtist.isCompositeId(id)) {
            Artist artist = artistProvider.requireArtist(FolderArtist.parseArtistId(id));
            MusicFolder folder = getFolder(FolderArtist.parseFolderId(id));
            return new FolderOrFArtist(new FolderArtist(folder, artist));
        }
        return new FolderOrFArtist(getFolder(Integer.parseInt(id)));
    }

    private int getChildSizeOf(MusicFolder folder) {
        return artistProvider.countArtists(Arrays.asList(folder));
    }

    private int getChildSizeOf(FolderArtist folderArtist) {
        return folderArtist.artist().albumCount();
    }

    public int getChildSizeOf(FolderOrFArtist folderOrArtist) {
        if (folderOrArtist.isFolderArtist()) {
            return getChildSizeOf(folderOrArtist.getFolderArtist());
        }
        return getChildSizeOf(folderOrArtist.getFolder());
    }
}
