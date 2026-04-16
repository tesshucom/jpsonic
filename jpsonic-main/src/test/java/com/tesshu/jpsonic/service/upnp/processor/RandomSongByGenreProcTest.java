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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.upnp.UPnPSKeys;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.GenreContainer;
import org.mockito.ArgumentMatchers;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class RandomSongByGenreProcTest {

    @Nested
    @SuppressWarnings("PMD.SingularField") // pmd/pmd#4616
    class UnitTest {
        private SettingsFacade settingsFacade;
        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private SearchService searchService;
        private RandomSongByGenreProc proc;

        @BeforeEach
        void setup() {
            settingsFacade = SettingsFacadeBuilder.create().build();
            init();
        }

        @Ignore
        void init() {
            JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            PlayerService playerService = mock(PlayerService.class);
            TranscodingService transcodingService = mock(TranscodingService.class);
            factory = new UpnpDIDLFactory(settingsFacade, jwtSecurityService, mediaFileService,
                    playerService, transcodingService);
            searchService = mock(SearchService.class);
            util = new UpnpProcessorUtil(mock(MusicFolderService.class),
                    mock(SecurityService.class), settingsFacade, mock(JpsonicComparators.class));
            proc = new RandomSongByGenreProc(settingsFacade, util, factory, searchService);
        }

        @Test
        void testGetProcId() {
            assertEquals("rsbg", proc.getProcId().getValue());
        }

        @Test
        void testCreateContainer() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            Container container = proc.createContainer(genre);
            assertInstanceOf(GenreContainer.class, container);
            assertEquals("rsbg/English/Japanese", container.getId());
            assertEquals("rsbg", container.getParentID());
            assertEquals("English/Japanese", container.getTitle());
            assertEquals(50, container.getChildCount());
        }

        @Test
        void testGetChildren() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            assertEquals(Collections.emptyList(), proc.getChildren(genre, 0, 0));
            verify(searchService, times(1))
                .getRandomSongs(anyInt(), anyInt(), anyInt(),
                        ArgumentMatchers.<MusicFolder>anyList(), any(String[].class));
        }

        @Test
        void testGetChildSizeOf() {
            Genre genre = new Genre("English/Japanese", 50, 100);
            settingsFacade = SettingsFacadeBuilder
                .create()
                .withInt(UPnPSKeys.options.randomMax, 10)
                .build();
            init();
            assertEquals(10, proc.getChildSizeOf(genre));

            settingsFacade = SettingsFacadeBuilder
                .create()
                .withInt(UPnPSKeys.options.randomMax, 1_000)
                .build();
            init();
            assertEquals(50, proc.getChildSizeOf(genre));
        }
    }
}
