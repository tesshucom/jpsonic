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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class SongByGenreProcTest {

    @SuppressWarnings("PMD.SingularField") // pmd/pmd#4616
    @Nested
    class UnitTest {
        private SettingsService settingsService;
        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private SearchService searchService;
        private SongByGenreProc proc;

        @BeforeEach
        public void setup() {
            settingsService = mock(SettingsService.class);
            JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            PlayerService playerService = mock(PlayerService.class);
            TranscodingService transcodingService = mock(TranscodingService.class);
            factory = new UpnpDIDLFactory(settingsService, jwtSecurityService, mediaFileService, playerService,
                    transcodingService);
            searchService = mock(SearchService.class);
            util = new UpnpProcessorUtil(settingsService, mock(MusicFolderService.class), mock(SecurityService.class),
                    mock(JpsonicComparators.class));
            proc = new SongByGenreProc(util, factory, searchService);
        }

        @Test
        void testGetProcId() {
            assertEquals("sbg", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            Container container = proc.createContainer(genre);
            assertInstanceOf(GenreContainer.class, container);
            assertEquals("sbg/English/Japanese", container.getId());
            assertEquals("sbg", container.getParentID());
            assertEquals("English/Japanese", container.getTitle());
            assertEquals(50, container.getChildCount());

            when(settingsService.isDlnaGenreCountVisible()).thenReturn(true);
            container = proc.createContainer(genre);
            assertEquals("English/Japanese 50", container.getTitle());
        }

        @Test
        void testGetDirectChildren() {
            assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
            verify(searchService, times(1)).getGenres(anyBoolean(), anyLong(), anyLong());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(0, proc.getDirectChildrenCount());
            verify(searchService, times(1)).getGenresCount(anyBoolean());
        }

        @Test
        void testGetDirectChild() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            when(searchService.getGenres(false)).thenReturn(List.of(genre));
            assertEquals("English/Japanese", proc.getDirectChild("English/Japanese").getName());
            assertNull(proc.getDirectChild("None"));
        }

        @Test
        void testGetChildren() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            assertEquals(Collections.emptyList(), proc.getChildren(genre, 0, 0));
            verify(searchService, times(1)).getSongsByGenres(anyString(), anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetChildSizeOf() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            assertEquals(50, proc.getChildSizeOf(genre));
        }

        @Test
        void testAddChild() {
            DIDLContent content = new DIDLContent();
            MediaFile song = new MediaFile();
            factory = mock(UpnpDIDLFactory.class);
            proc = new SongByGenreProc(util, factory, searchService);
            proc.addChild(content, song);
            verify(factory, times(1)).toMusicTrack(any(MediaFile.class));
            assertEquals(1, content.getCount());
            assertEquals(0, content.getContainers().size());
            assertEquals(1, content.getItems().size());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays.asList(
                new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true, now(), 1, false));

        @Autowired
        private SongByGenreProc songByGenreProc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            settingsService.setSortGenresByAlphabet(false);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetItemCount() {
            assertEquals(31, songByGenreProc.getDirectChildrenCount());
        }

        @Test
        void testGetDirectChildren() {

            Map<String, Genre> c = LegacyMap.of();

            List<Genre> items = songByGenreProc.getDirectChildren(0, 10);
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 10);

            items = songByGenreProc.getDirectChildren(10, 10);
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 20);

            items = songByGenreProc.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 31);

        }

        @Test
        void testGetChildren() {

            List<Genre> artists = songByGenreProc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            // assertEquals("A;B;C", artists.get(0).getName());

            Map<String, MediaFile> c = LegacyMap.of();

            List<MediaFile> children = songByGenreProc.getChildren(artists.get(0), 0, 10);
            children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
            assertEquals(c.size(), 10);

            children = songByGenreProc.getChildren(artists.get(0), 10, 10);
            children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
            assertEquals(c.size(), 20);

            children = songByGenreProc.getChildren(artists.get(0), 20, 100);
            assertEquals(11, children.size());
            children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
            assertEquals(c.size(), 31);

        }

        @Test
        void testGetChildSizeOf() {
            List<Genre> artists = songByGenreProc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            // assertEquals("A;B;C", artists.get(0).getName());
            assertEquals(31, songByGenreProc.getChildSizeOf(artists.get(0)));
        }
    }
}
