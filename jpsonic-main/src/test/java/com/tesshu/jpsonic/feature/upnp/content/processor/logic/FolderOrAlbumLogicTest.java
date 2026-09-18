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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderOrFAlbum;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;
import org.jupnp.support.model.container.StorageFolder;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class FolderOrAlbumLogicTest {

    private MusicFolderProvider musicFolderProvider;
    private AlbumProvider albumProvider;
    FolderOrAlbumLogic logic;

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
        albumProvider = mock(AlbumProvider.class);
        logic = new FolderOrAlbumLogic(musicFolderProvider, albumProvider, factory);
    }

    @Test
    void testCreateContainerWithFolder() {
        MusicFolder folder = new MusicFolder(99, "path", "FolderName", true, null, 0, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
        when(albumProvider.countAlbums(anyList())).thenReturn(100);
        Container container = logic.createContainer(ProcId.ALBUM_ID3_BY_FOLDER, folderOrAlbum);

        assertInstanceOf(StorageFolder.class, container);
        assertEquals("alid3bf/99", container.getId());
        assertEquals("alid3bf", container.getParentID());
        assertEquals("FolderName", container.getTitle());
        assertEquals(100, container.getChildCount());
    }

    @Test
    void testCreateContainerWithAlbum() {
        Album album = new Album(999, "Album", "artist", 20, null);
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        Container container = logic.createContainer(ProcId.ALBUM_ID3_BY_FOLDER, folderOrAlbum);
        assertInstanceOf(MusicAlbum.class, container);
        assertEquals("alid3bf/fal:99;999", container.getId());
        assertEquals("alid3bf", container.getParentID());
        assertEquals("Album", container.getTitle());
        assertEquals(20, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        MusicFolder folder1 = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        MusicFolder folder2 = new MusicFolder(99, "/folder2", "folder2", true, null, 0, false);
        MusicFolder folder3 = new MusicFolder(99, "/folder3", "folder3", true, null, 0, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildren(0, 4).size());
        assertEquals(2, logic.getDirectChildren(0, 2).size());
        assertEquals(2, logic.getDirectChildren(1, 4).size());
        assertEquals(2, logic.getDirectChildren(1, 2).size());
        List<FolderOrFAlbum> folderOrAlbums = logic.getDirectChildren(0, 4);
        verify(albumProvider, Mockito.never())
            .findAlbums(anyList(), any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(),
                    anyLong());
        folderOrAlbums.forEach(folder -> assertFalse(folder.isFolderAlbum()));

        folders = List.of(folder1);
        when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildren(0, 4).size());
        verify(albumProvider, Mockito.times(1))
            .findAlbums(anyList(), any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(),
                    anyLong());
    }

    @Test
    void testGetDirectChildrenCount() {
        MusicFolder folder1 = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        MusicFolder folder2 = new MusicFolder(99, "/folder2", "folder2", true, null, 0, false);
        MusicFolder folder3 = new MusicFolder(99, "/folder3 ", "folder3", true, null, 0, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(3, logic.getDirectChildrenCount());
        verify(albumProvider, Mockito.never()).countAlbums(anyList());

        folders = List.of(folder1);
        when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(0, logic.getDirectChildrenCount());
        verify(albumProvider, Mockito.times(1)).countAlbums(anyList());
    }

    @Test
    void testGetDirectChildWithAlbum() {
        int id = 99;
        Album album = new Album(id, "album", "artist", 20, null);
        when(albumProvider.requireAlbum(id)).thenReturn(album);
        MusicFolder folder = new MusicFolder(88, "/folder1", "folder1", true, null, 0, false);
        when(musicFolderProvider.getGuestFolders()).thenReturn(List.of(folder));
        FolderAlbum folderAlbum = new FolderAlbum(folder, album);
        String compositeId = folderAlbum.createCompositeId();
        assertEquals(album, logic.getDirectChild(compositeId).getFolderAlbum().album());
    }

    @Test
    void testGetDirectChildWithFolder() {
        MusicFolder folder1 = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        MusicFolder folder2 = new MusicFolder(1, "/folder2", "folder2", true, now(), 2, false);
        MusicFolder folder3 = new MusicFolder(2, "/folder3", "folder3", true, now(), 3, false);
        List<MusicFolder> folders = List.of(folder1, folder2, folder3);
        when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
        assertEquals(folder3, logic.getDirectChild("2").getFolder());
    }

    @Test
    void testGetChildSizeOfWithAlbum() {
        Album album = new Album(0, "album", "artist", 3, null);
        MusicFolder folder = new MusicFolder(99, "/folder1", "folder1", true, null, 0, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(new FolderAlbum(folder, album));
        assertEquals(3, logic.getChildSizeOf(folderOrAlbum));
    }

    @Test
    void testGetChildSizeOfWithFolder() {
        MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
        FolderOrFAlbum folderOrAlbum = new FolderOrFAlbum(folder);
        assertEquals(0, logic.getChildSizeOf(folderOrAlbum));
        verify(albumProvider, Mockito.times(1)).countAlbums(anyList());
    }
}
