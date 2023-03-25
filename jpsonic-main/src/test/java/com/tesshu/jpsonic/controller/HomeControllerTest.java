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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.scanner.ScannerStateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.ModelAndView;

@SuppressWarnings("PMD.TooManyStaticImports")
class HomeControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HomeController(mock(SettingsService.class), mock(SecurityService.class),
                        mock(MusicFolderService.class), mock(ScannerStateServiceImpl.class), mock(RatingService.class),
                        mock(MediaFileService.class), mock(SearchService.class), mock(MusicIndexService.class)))
                .build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testHandleRequestInternal() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.HOME.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("home", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
    }

    @Nested
    class GetTest {

        private SettingsService settingsService;
        private SecurityService securityService;
        private MusicFolderService musicFolderService;
        private ScannerStateService scannerStateService;
        private RatingService ratingService;
        private MediaFileService mediaFileService;
        private SearchService searchService;
        private MusicIndexService musicIndexService;
        private HomeController controller;

        @BeforeEach
        public void setup() throws ExecutionException {
            settingsService = mock(SettingsService.class);
            securityService = mock(SecurityService.class);
            musicFolderService = mock(MusicFolderService.class);
            scannerStateService = mock(ScannerStateService.class);
            ratingService = mock(RatingService.class);
            mediaFileService = mock(MediaFileService.class);
            searchService = mock(SearchService.class);
            musicIndexService = mock(MusicIndexService.class);
            controller = new HomeController(settingsService, securityService, musicFolderService, scannerStateService,
                    ratingService, mediaFileService, searchService, musicIndexService);
        }

        @Test
        void testHighest() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.HIGHEST.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(ratingService, Mockito.times(1)).getHighestRatedAlbums(anyInt(), anyInt(), anyList());
        }

        @Test
        void testFrequent() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.FREQUENT.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getMostFrequentlyPlayedAlbums(anyInt(), anyInt(),
                    anyList());
        }

        @Test
        void testRecent() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.RECENT.getId());

            MediaFile album = new MediaFile();
            album.setId(1);
            Mockito.when(mediaFileService.getMostRecentlyPlayedAlbums(anyInt(), anyInt(), anyList()))
                    .thenReturn(Arrays.asList(album));

            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getMostRecentlyPlayedAlbums(anyInt(), anyInt(),
                    anyList());
            Mockito.clearInvocations(mediaFileService);

            album.setLastPlayed(now());
            Mockito.when(mediaFileService.getMostRecentlyPlayedAlbums(anyInt(), anyInt(), anyList()))
                    .thenReturn(Arrays.asList(album));
            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getMostRecentlyPlayedAlbums(anyInt(), anyInt(),
                    anyList());
        }

        @Test
        void testNewest() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.NEWEST.getId());

            MediaFile album = new MediaFile();
            album.setId(1);
            album.setCreated(now());
            Mockito.when(mediaFileService.getNewestAlbums(anyInt(), anyInt(), anyList()))
                    .thenReturn(Arrays.asList(album));

            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getNewestAlbums(anyInt(), anyInt(), anyList());
        }

        @Test
        void testStarred() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.STARRED.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getStarredAlbums(anyInt(), anyInt(), anyString(),
                    anyList());
        }

        @Test
        void testRandom() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.RANDOM.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(searchService, Mockito.times(1)).getRandomAlbums(anyInt(), anyList());
        }

        @Test
        void testAlphabetical() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.ALPHABETICAL.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(),
                    anyList());
        }

        @Test
        void testDecade() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.DECADE.getId());
            controller.handleRequestInternal(req);
            Mockito.verify(mediaFileService, Mockito.times(1)).getAlbumsByYear(anyInt(), anyInt(), anyInt(), anyInt(),
                    anyList());
        }

        @Test
        void testGenre() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.GENRE.getId());
            List<Genre> genres = Arrays.asList(new Genre("pops"));
            Mockito.when(searchService.getGenres(true)).thenReturn(genres);
            controller.handleRequestInternal(req);
            Mockito.verify(searchService, Mockito.times(1)).getAlbumsByGenres(anyString(), anyInt(), anyInt(),
                    anyList());
        }

        @Test
        void testIndex() throws ServletRequestBindingException {
            MockHttpServletRequest req = mock(MockHttpServletRequest.class);
            Mockito.when(req.getParameter(Attributes.Request.LIST_TYPE.value()))
                    .thenReturn(AlbumListType.INDEX.getId());
            List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(0, "", "name", false, now()));
            Mockito.when(musicFolderService.getMusicFoldersForUser(anyString(), Mockito.nullable(Integer.class)))
                    .thenReturn(musicFolders);
            Mockito.when(musicIndexService.getMusicFolderContent(musicFolders))
                    .thenReturn(new MusicFolderContent(new TreeMap<>(), Collections.emptyList()));
            controller.handleRequestInternal(req);
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(musicFolders);
        }
    }
}
