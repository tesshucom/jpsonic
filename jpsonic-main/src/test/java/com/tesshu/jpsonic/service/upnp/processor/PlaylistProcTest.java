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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlaylistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.PlaylistContainer;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class PlaylistProcTest {

    @Nested
    class UnitTest {

        UpnpDIDLFactory factory;
        PlaylistService playlistService;
        private PlaylistProc proc;

        @BeforeEach
        public void setup() {
            SettingsService settingsService = mock(SettingsService.class);
            factory = new UpnpDIDLFactory(settingsService, mock(JWTSecurityService.class), mock(MediaFileService.class),
                    mock(PlayerService.class), mock(TranscodingService.class));
            playlistService = mock(PlaylistService.class);
            proc = new PlaylistProc(factory, playlistService);
        }

        @Test
        void testGetProcId() {
            assertEquals("playlist", proc.getProcId().getValue());
        }

        @Test
        void testGetDirectChildren() {
            Playlist playlist1 = new Playlist(0, null, false, null, null, 0, 0, null, null, null);
            Playlist playlist2 = new Playlist(1, null, false, null, null, 0, 0, null, null, null);
            Playlist playlist3 = new Playlist(2, null, false, null, null, 0, 0, null, null, null);
            Playlist playlist4 = new Playlist(3, null, false, null, null, 0, 0, null, null, null);
            when(playlistService.getAllPlaylists()).thenReturn(List.of(playlist1, playlist2, playlist3, playlist4));
            assertEquals(1, proc.getDirectChildren(0, 1).size());
            assertEquals(0, proc.getDirectChildren(0, 1).get(0).getId());
            assertEquals(3, proc.getDirectChildren(1, 3).size());
            assertEquals(1, proc.getDirectChildren(1, 3).get(0).getId());
            assertEquals(2, proc.getDirectChildren(1, 3).get(1).getId());
            assertEquals(3, proc.getDirectChildren(1, 3).get(2).getId());
            assertEquals(4, proc.getDirectChildren(0, 100).size());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(0, proc.getDirectChildrenCount());
            verify(playlistService, times(1)).getCountAll();
        }

        @Test
        void testGetDirectChild() {
            assertNull(proc.getDirectChild("0"));
            verify(playlistService, times(1)).getPlaylist(anyInt());
        }

        @Test
        void testGetChildren() {
            assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
            verify(playlistService, times(1)).getAllPlaylists();
        }

        @Test
        void testGetChildSizeOf() {
            Playlist playlist = new Playlist(0, null, false, null, null, 99, 0, null, null, null);
            assertEquals(99, proc.getChildSizeOf(playlist));
        }

        @Test
        void testAddChild() {
            DIDLContent content = new DIDLContent();
            assertEquals(0, content.getContainers().size());
            MediaFile song = new MediaFile();
            factory = mock(UpnpDIDLFactory.class);
            proc = new PlaylistProc(factory, playlistService);
            proc.addChild(content, song);
            verify(factory, times(1)).toMusicTrack(any(MediaFile.class));
            assertEquals(1, content.getCount());
            assertEquals(1, content.getItems().size());
            assertEquals(0, content.getContainers().size());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays.asList(
                new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true, now(), 1, false));

        @Autowired
        private PlaylistProc playlistProc;

        @Autowired
        private AlbumId3Proc albumId3Proc;

        @Autowired
        private MediaFileDao mediaFileDao;

        @Autowired
        private PlaylistDao playlistDao;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setDlnaGuestPublish(false);
            populateDatabaseOnlyOnce();
            settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
            settingsService.save();

            Function<String, Playlist> toPlaylist = (title) -> {
                Instant now = now();
                Playlist playlist = new Playlist();
                playlist.setName(title);
                playlist.setUsername("admin");
                playlist.setCreated(now);
                playlist.setChanged(now);
                playlist.setShared(false);
                return playlist;
            };

            if (playlistDao.getAllPlaylists().isEmpty()) {
                List<String> shallow = new ArrayList<>();
                shallow.addAll(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST);
                Collections.shuffle(shallow);
                shallow.stream().map(toPlaylist).forEach(playlistDao::createPlaylist);
            }

            List<Album> albums = albumId3Proc.getDirectChildren(0, 100);
            assertEquals(61, albums.size());
            List<MediaFile> files = albums.stream()
                    .map(a -> mediaFileDao.getSongsForAlbum(0L, Integer.MAX_VALUE, a.getArtist(), a.getName()).get(0))
                    .collect(Collectors.toList());
            assertEquals(61, files.size());
            playlistDao.setFilesInPlaylist(playlistProc.getDirectChildren(0, 1).get(0).getId(), files);
        }

        @Test
        void testCreateContainer() {
            Playlist playlist = new Playlist(0, "admin", false, "testPlaylist", null, 99, 0, null, null, null);
            Container container = playlistProc.createContainer(playlist);
            assertInstanceOf(PlaylistContainer.class, container);
            assertEquals("playlist/0", container.getId());
            assertEquals("playlist", container.getParentID());
            assertEquals("testPlaylist", container.getTitle());
            assertEquals(99, container.getChildCount());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(31, playlistProc.getDirectChildrenCount());
        }

        @Test
        void testGetDirectChildren() {

            Map<String, Playlist> c = LegacyMap.of();

            List<Playlist> items = playlistProc.getDirectChildren(0, 10);
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 10);

            items = playlistProc.getDirectChildren(10, 10);
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 20);

            items = playlistProc.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 31);
        }

        @Test
        void testGetChildren() {

            List<Playlist> playlists = playlistProc.getDirectChildren(0, 1);
            assertEquals(1, playlists.size());
            assertEquals(61, playlistProc.getChildSizeOf(playlists.get(0)));

            Map<Integer, MediaFile> c = LegacyMap.of();

            List<MediaFile> children = playlistProc.getChildren(playlists.get(0), 0, 20);
            children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(children.size(), 20);

            children = playlistProc.getChildren(playlists.get(0), 20, 20);
            children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(c.size(), 40);

            children = playlistProc.getChildren(playlists.get(0), 40, 100);
            assertEquals(21, children.size());
            children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(c.size(), 61);
        }

        @Test
        void testGetChildSizeOf() {
            List<Playlist> playlists = playlistProc.getDirectChildren(0, 1);
            assertEquals(1, playlists.size());
            assertEquals(61, playlistProc.getChildSizeOf(playlists.get(0)));
        }
    }
}
