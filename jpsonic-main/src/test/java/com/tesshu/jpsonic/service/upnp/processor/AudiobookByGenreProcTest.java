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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
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
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile.MediaType;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria;
import com.tesshu.jpsonic.util.LegacyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class AudiobookByGenreProcTest {

    @Nested
    @SuppressWarnings("PMD.SingularField")
    class UnitTest {
        private SettingsService settingsService;
        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private SearchService searchService;
        private AudiobookByGenreProc proc;

        @BeforeEach
        public void setup() {
            settingsService = mock(SettingsService.class);
            JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            PlayerService playerService = mock(PlayerService.class);
            TranscodingService transcodingService = mock(TranscodingService.class);
            factory = new UpnpDIDLFactory(settingsService, jwtSecurityService, mediaFileService,
                    playerService, transcodingService);
            searchService = mock(SearchService.class);
            util = new UpnpProcessorUtil(mock(MusicFolderService.class),
                    mock(SecurityService.class), settingsService, mock(JpsonicComparators.class));
            proc = new AudiobookByGenreProc(settingsService, util, factory, searchService);
        }

        @Test
        void testGetProcId() {
            assertEquals("abbg", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            Container container = proc.createContainer(genre);
            assertInstanceOf(GenreContainer.class, container);
            assertEquals("abbg/English/Japanese", container.getId());
            assertEquals("abbg", container.getParentID());
            assertEquals("English/Japanese", container.getTitle());
            assertEquals(50, container.getChildCount());
        }

        @Test
        void testGetDirectChildren() {
            assertEquals(Collections.emptyList(), proc.getDirectChildren(0, 0));
            verify(searchService, times(1))
                .getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(0, proc.getDirectChildrenCount());
            verify(searchService, times(1)).getGenresCount(any(GenreMasterCriteria.class));
        }

        @Test
        void testGetDirectChild() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            when(searchService.getGenres(any(GenreMasterCriteria.class), anyLong(), anyLong()))
                .thenReturn(List.of(genre));
            assertEquals("English/Japanese", proc.getDirectChild("English/Japanese").getName());
            assertNull(proc.getDirectChild("None"));
        }

        @Test
        void testGetChildren() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            assertEquals(Collections.emptyList(), proc.getChildren(genre, 0, 0));
            verify(searchService, times(1))
                .getSongsByGenres(anyString(), anyInt(), anyInt(), anyList(),
                        any(MediaType[].class));
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
            proc = new AudiobookByGenreProc(mock(SettingsService.class), util, factory,
                    searchService);
            proc.addChild(content, song);
            verify(factory, times(1)).toMusicTrack(any(MediaFile.class));
            assertEquals(1, content.getCount());
            assertEquals(0, content.getContainers().size());
            assertEquals(1, content.getItems().size());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true,
                    now(), 1, false));

        @Autowired
        private AudiobookByGenreProc audiobookByGenreProc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetItemCount() {
            assertEquals(2, audiobookByGenreProc.getDirectChildrenCount());
        }

        @Test
        void testGetDirectChildren() {
            Map<String, Genre> c = LegacyMap.of();
            List<Genre> items = audiobookByGenreProc.getDirectChildren(0, 10);
            items
                .stream()
                .filter(g -> !c.containsKey(g.getName()))
                .forEach(g -> c.put(g.getName(), g));
            assertEquals(c.size(), 2);
        }

        @Test
        void testGetChildren() {
            List<Genre> genre = audiobookByGenreProc.getDirectChildren(0, 1);
            assertEquals(1, genre.size());
            assertEquals("Audiobook - Historical", genre.get(0).getName());

            List<MediaFile> children = audiobookByGenreProc.getChildren(genre.get(0), 0, 10);
            assertEquals(1, children.size());
            assertEquals("FILE14", children.get(0).getName());
            assertEquals("Audiobook - Historical", children.get(0).getGenre());
        }

        @Test
        void testGetChildSizeOf() {
            List<Genre> genre = audiobookByGenreProc.getDirectChildren(0, 1);
            assertEquals(1, genre.size());
            assertEquals("Audiobook - Historical", genre.get(0).getName());

            assertEquals(1, audiobookByGenreProc.getChildSizeOf(genre.get(0)));
        }
    }
}
