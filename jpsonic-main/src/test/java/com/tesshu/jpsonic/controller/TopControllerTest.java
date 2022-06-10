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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.SignatureDeclareThrowsException" })
class TopControllerTest {

    private SecurityService securityService;
    private MusicFolderService musicFolderService;
    private MediaScannerService mediaScannerService;
    private MusicIndexService musicIndexService;
    private TopController controller;

    private MockMvc mockMvc;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() throws ExecutionException {
        securityService = mock(SecurityService.class);
        musicFolderService = mock(MusicFolderService.class);
        mediaScannerService = mock(MediaScannerService.class);
        musicIndexService = mock(MusicIndexService.class);
        Mockito.when(
                musicIndexService.getMusicFolderContent(Mockito.nullable(List.class), Mockito.nullable(boolean.class)))
                .thenReturn(new MusicFolderContent(new TreeMap<>(), Collections.emptyList()));
        controller = new TopController(mock(SettingsService.class), musicFolderService, securityService,
                mediaScannerService, musicIndexService, mock(VersionService.class), mock(InternetRadioService.class),
                mock(AirsonicLocaleResolver.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testHandleRequestInternal() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.TOP.value())
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(), "0"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("top", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGetLastModified() throws ServletRequestBindingException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertNotEquals(0, controller.getLastModified(req));
    }

    @Documented
    private @interface RefleshDecision {
        @interface Conditions {
            @interface ParamRefresh {
                @interface None {
                }

                @interface True {
                }
            }

            @interface Scanning {
                @interface False {
                }

                @interface True {
                }
            }

        }

        @interface Result {
            @interface Refresh {
                // useFastCache
                @interface False {
                }

                // !useFastCache
                @interface True {
                }
            }
        }
    }

    @Nested
    class RefleshTest {

        @RefleshDecision.Conditions.ParamRefresh.None
        @RefleshDecision.Conditions.Scanning.False
        @RefleshDecision.Result.Refresh.False
        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c1() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.TOP.value())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), "0"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            ArgumentCaptor<Boolean> refreshCaptor = ArgumentCaptor.forClass(boolean.class);
            Mockito.verify(musicFolderService, Mockito.never()).clearMusicFolderCache();
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(Mockito.anyList(),
                    refreshCaptor.capture());
            assertFalse(refreshCaptor.getValue());
        }

        @RefleshDecision.Conditions.ParamRefresh.True
        @RefleshDecision.Conditions.Scanning.False
        @RefleshDecision.Result.Refresh.True
        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c2() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.TOP.value())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), "0")
                    .param(Attributes.Request.REFRESH.value(), "true"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            ArgumentCaptor<Boolean> refreshCaptor = ArgumentCaptor.forClass(boolean.class);
            Mockito.verify(musicFolderService, Mockito.times(1)).clearMusicFolderCache();
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(Mockito.anyList(),
                    refreshCaptor.capture());
            assertTrue(refreshCaptor.getValue());
        }

        @RefleshDecision.Conditions.ParamRefresh.None
        @RefleshDecision.Conditions.Scanning.True
        @RefleshDecision.Result.Refresh.False
        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c3() throws Exception {
            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.TOP.value())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), "0"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            ArgumentCaptor<Boolean> refreshCaptor = ArgumentCaptor.forClass(boolean.class);
            Mockito.verify(musicFolderService, Mockito.never()).clearMusicFolderCache();
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(Mockito.anyList(),
                    refreshCaptor.capture());
            assertFalse(refreshCaptor.getValue());
        }

        @RefleshDecision.Conditions.ParamRefresh.True
        @RefleshDecision.Conditions.Scanning.True
        @RefleshDecision.Result.Refresh.False
        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c4() throws Exception {
            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.TOP.value())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), "0")
                    .param(Attributes.Request.REFRESH.value(), "true"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            ArgumentCaptor<Boolean> refreshCaptor = ArgumentCaptor.forClass(boolean.class);
            Mockito.verify(musicFolderService, Mockito.never()).clearMusicFolderCache();
            Mockito.verify(musicIndexService, Mockito.times(1)).getMusicFolderContent(Mockito.anyList(),
                    refreshCaptor.capture());
            assertFalse(refreshCaptor.getValue());
        }
    }

    @Nested
    class GetLastModifiedTest {

        // ... This method is not currently used, but is probably wrong.

        @Test
        void testWithScanning() throws ServletRequestBindingException {
            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            MockHttpServletRequest request = mock(MockHttpServletRequest.class);
            long lastModified = controller.getLastModified(request);
            assertEquals(-1L, lastModified);
        }

        @Test
        void testWithoutSelectedMusicFolders() throws ServletRequestBindingException, URISyntaxException {
            List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(1,
                    Path.of(TopControllerTest.class.getResource("/MEDIAS/Sort/Pagination/Artists").toURI()).toString(),
                    "MEDIAS", true, new Date()));
            Mockito.when(musicFolderService.getMusicFoldersForUser(Mockito.anyString())).thenReturn(musicFolders);

            MockHttpServletRequest request = mock(MockHttpServletRequest.class);
            long lastModified = controller.getLastModified(request);
            assertNotEquals(-1L, lastModified);
        }

        @Test
        void testWithSelectedMusicFolders() throws ServletRequestBindingException, URISyntaxException {
            List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(1,
                    Path.of(TopControllerTest.class.getResource("/MEDIAS/Sort/Pagination/Artists").toURI()).toString(),
                    "MEDIAS", true, new Date()));
            Mockito.when(musicFolderService.getMusicFoldersForUser(Mockito.anyString())).thenReturn(musicFolders);
            Mockito.when(securityService.getSelectedMusicFolder(Mockito.anyString())).thenReturn(musicFolders.get(0));

            MockHttpServletRequest request = mock(MockHttpServletRequest.class);
            long lastModified = controller.getLastModified(request);
            assertNotEquals(-1L, lastModified);
        }

    }
}
