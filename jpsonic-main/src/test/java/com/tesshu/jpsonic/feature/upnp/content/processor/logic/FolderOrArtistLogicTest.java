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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor.logic;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFArtist;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.jupnp.support.model.container.StorageFolder;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class FolderOrArtistLogicTest {

    private MusicFolderProvider musicFolderProvider;
    private ArtistProvider artistProvider;
    private FolderOrArtistLogic logic;

    @BeforeEach
    void setup() {
        SettingsFacade settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040")
            .build();
        MediaFileProvider mediaFileProvider = mock(MediaFileProvider.class);
        PlayerProvider playerProvider = mock(PlayerProvider.class);
        UpnpPayloadCodec upnpPayloadCodec = mock(UpnpPayloadCodec.class);
        TranscodingParametersPlanner transcodingParametersPlanner = mock(
                TranscodingParametersPlanner.class);
        UPnPDIDLFactory factory = new UPnPDIDLFactory(settingsFacade, upnpPayloadCodec,
                mediaFileProvider, playerProvider, transcodingParametersPlanner);
        musicFolderProvider = mock(MusicFolderProvider.class);
        artistProvider = mock(ArtistProvider.class);
        logic = new FolderOrArtistLogic(musicFolderProvider, artistProvider, factory);
    }

    @Test
    void testCreateContainerWithFolder() {
        MusicFolder folder = new MusicFolder(99, "path1", "Music", false, null, 0, false);
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(folder);
        Mockito.when(artistProvider.countArtists(anyList())).thenReturn(100);
        Container container = logic
            .createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(StorageFolder.class, container);
        assertEquals("rsbfar/99", container.getId());
        assertEquals("rsbfar", container.getParentID());
        assertEquals("Music", container.getTitle());
        assertEquals(100, container.getChildCount());
    }

    @Test
    void testCreateContainerWithArtist() {
        MusicFolder folder = new MusicFolder(99, "path1", "name1", false, null, 0, false);
        Artist artist = new Artist(999, "Artist", "path", 20, 0, "reading", 0, "#");

        FolderOrFArtist folderOrArtist = new FolderOrFArtist(new FolderArtist(folder, artist));
        Container container = logic
            .createContainer(ProcId.RANDOM_SONG_BY_FOLDER_ARTIST, folderOrArtist);
        assertInstanceOf(MusicArtist.class, container);
        assertEquals("rsbfar/far:99;999", container.getId());
        assertEquals("rsbfar", container.getParentID());
        assertEquals("Artist", container.getTitle());
        assertEquals(20, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        MusicFolder folder1 = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        MusicFolder folder2 = new MusicFolder(99, "/folder2", "folder2", true, null, 0, false);
        MusicFolder folder3 = new MusicFolder(99, "/folder3 ", "folder3", true, null, 0, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildren(0, 4).size());
        assertEquals(2, logic.getDirectChildren(0, 2).size());
        assertEquals(2, logic.getDirectChildren(1, 4).size());
        assertEquals(2, logic.getDirectChildren(1, 2).size());
        List<FolderOrFArtist> folderOrArtists = logic.getDirectChildren(0, 4);
        Mockito
            .verify(artistProvider, Mockito.never())
            .findArtists(anyList(), anyLong(), anyLong());
        folderOrArtists.forEach(folder -> assertFalse(folder.isFolderArtist()));

        folders = List.of(folder1);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildren(0, 4).size());
        Mockito
            .verify(artistProvider, Mockito.times(1))
            .findArtists(anyList(), anyLong(), anyLong());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder1 = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        MusicFolder folder2 = new MusicFolder(99, "/folder2", "folder2", true, null, 0, false);
        MusicFolder folder3 = new MusicFolder(99, "/folder3 ", "folder3", true, null, 0, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildrenCount());
        Mockito.verify(artistProvider, Mockito.never()).countArtists(anyList());

        folders = List.of(folder1);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildrenCount());
        Mockito.verify(artistProvider, Mockito.times(1)).countArtists(anyList());
    }

    @Test
    void testGetDirectChildWithArtist() {
        MusicFolder folder = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder));
        int id = 88;
        Artist artist = new Artist(id, "artist", "path", 20, 0, "reading", 0, "#");
        Mockito.when(artistProvider.requireArtist(id)).thenReturn(artist);
        FolderArtist folderArtist = new FolderArtist(folder, artist);
        String compositeId = folderArtist.createCompositeId();
        logic.getDirectChild(compositeId);
        assertEquals(artist, logic.getDirectChild(compositeId).getFolderArtist().artist());
    }

    @Test
    void testGetDirectChildWithFolder() {
        MusicFolder folder = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        Mockito.when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder));
        assertEquals(folder, logic.getDirectChild("99").getFolder());
    }

    @Test
    void testGetChildSizeOfWithArtist() {
        MusicFolder folder = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        Artist artist = new Artist(0, "artist", "path", 3, 0, "reading", 0, "#");
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(new FolderArtist(folder, artist));
        assertEquals(3, logic.getChildSizeOf(folderOrArtist));
    }

    @Test
    void testGetChildSizeOfWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFArtist folderOrArtist = new FolderOrFArtist(folder);
        assertEquals(0, logic.getChildSizeOf(folderOrArtist));
        Mockito.verify(artistProvider, Mockito.times(1)).countArtists(anyList());
    }
}
