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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.jupnp.support.model.item.MusicTrack;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class RandomSongByArtistProcTest {

    @SuppressWarnings("PMD.SingularField") // pmd/pmd#4616
    @Nested
    class UnitTest {

        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private ArtistDao artistDao;
        private SearchService searchService;
        private SettingsService settingsService;

        private RandomSongByArtistProc proc;

        @BeforeEach
        public void setup() {
            util = mock(UpnpProcessorUtil.class);
            factory = new UpnpDIDLFactory(settingsService, mock(JWTSecurityService.class),
                    mock(MediaFileService.class), mock(PlayerService.class),
                    mock(TranscodingService.class));
            artistDao = mock(ArtistDao.class);
            searchService = mock(SearchService.class);
            settingsService = mock(SettingsService.class);
            proc = new RandomSongByArtistProc(util, factory, artistDao, searchService,
                    settingsService);
        }

        @Test
        void testGetProcId() {
            assertEquals("rsbar", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            Artist artist = new Artist();
            artist.setName("artistName");
            artist.setAlbumCount(50);
            Container container = proc.createContainer(artist);
            assertInstanceOf(MusicArtist.class, container);
            assertEquals("rsbar/0", container.getId());
            assertEquals("rsbar", container.getParentID());
            assertEquals("artistName", container.getTitle());
            assertEquals(50, container.getChildCount());
        }

        @Test
        void testGetDirectChildren() {
            proc.getDirectChildren(0, 0);
            verify(artistDao, times(1)).getAlphabetialArtists(anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(0, proc.getDirectChildrenCount());
            verify(artistDao, times(1)).getArtistsCount(anyList());
        }

        @Test
        void testGetDirectChild() {
            assertNull(proc.getDirectChild("0"));
            verify(artistDao, times(1)).getArtist(anyInt());
        }

        @Test
        void testGetChildren() {
            assertEquals(0, proc.getChildren(new Artist(), 0, 0).size());
            verify(searchService, times(1))
                .getRandomSongsByArtist(any(Artist.class), anyInt(), anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetChildSizeOf() {
            proc.getChildSizeOf(null);
            verify(settingsService, times(1)).getDlnaRandomMax();
        }

        @Test
        void testAddChild() {
            DIDLContent content = new DIDLContent();
            assertEquals(0, content.getContainers().size());
            MediaFile song = new MediaFile();
            factory = mock(UpnpDIDLFactory.class);
            proc = new RandomSongByArtistProc(util, factory, artistDao, searchService,
                    settingsService);

            when(factory.toMusicTrack(song)).thenReturn(new MusicTrack());
            proc.addChild(content, song);
            assertEquals(1, content.getItems().size());
        }
    }

    /*
     * Test to correct sort inconsistencies.
     */
    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists",
                    true, now(), 1, false));

        @Autowired
        private RandomSongByArtistProc randomSongByArtistProc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetDirectChildren() {

            Map<String, Artist> c = LegacyMap.of();

            List<Artist> items = randomSongByArtistProc.getDirectChildren(0, 10);
            items
                .stream()
                .filter(g -> !c.containsKey(g.getName()))
                .forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 10);

            items = randomSongByArtistProc.getDirectChildren(10, 10);
            items
                .stream()
                .filter(g -> !c.containsKey(g.getName()))
                .forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 20);

            items = randomSongByArtistProc.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            items
                .stream()
                .filter(g -> !c.containsKey(g.getName()))
                .forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 31);
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(31, randomSongByArtistProc.getDirectChildrenCount());
        }

        @Test
        void testGetChildren() {
            List<Artist> artists = randomSongByArtistProc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());

            Map<String, MediaFile> c = LegacyMap.of();
            List<MediaFile> children = randomSongByArtistProc.getChildren(artists.get(0), 0, 10);
            children
                .stream()
                .filter(m -> !c.containsKey(m.getArtist()))
                .forEach(m -> c.put(m.getArtist(), m));
            assertEquals(c.size(), 1);
        }

        @Test
        void testGetChildSizeOf() {
            List<Artist> artists = randomSongByArtistProc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            assertEquals(50, randomSongByArtistProc.getChildSizeOf(artists.get(0)));
        }
    }
}
