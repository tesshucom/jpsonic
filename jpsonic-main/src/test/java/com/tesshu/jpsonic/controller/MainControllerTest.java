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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class MainControllerTest {

    private SecurityService securityService;
    private MockMvc mockMvc;

    private MediaFile root;
    private MediaFile artist;
    private MediaFile album;
    private MediaFile song;

    @BeforeEach
    public void setup() throws ExecutionException {

        securityService = mock(SecurityService.class);
        MediaFileService mediaFileService = mock(MediaFileService.class);
        MainController controller = new MainController(mock(SettingsService.class), securityService,
                mock(JpsonicComparators.class), mock(RatingService.class), mediaFileService,
                mock(ViewAsListSelector.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        root = new MediaFile();
        int rootId = 100;
        String rootPath = MainControllerTest.class.getResource("/MEDIAS").getPath();
        root.setId(rootId);
        root.setPath(rootPath);
        root.setMediaType(MediaType.DIRECTORY);
        Mockito.when(mediaFileService.getMediaFile(rootPath)).thenReturn(root);
        Mockito.when(mediaFileService.getMediaFile(rootId)).thenReturn(root);
        Mockito.when(mediaFileService.isRoot(root)).thenReturn(true);

        artist = new MediaFile();
        int artistId = 200;
        String artistPath = MainControllerTest.class.getResource("/MEDIAS/Music").getPath();
        artist.setId(artistId);
        artist.setPath(artistPath);
        artist.setMediaType(MediaType.DIRECTORY);

        Mockito.when(mediaFileService.getMediaFile(artistPath)).thenReturn(artist);
        Mockito.when(mediaFileService.getMediaFile(artistId)).thenReturn(artist);
        Mockito.when(mediaFileService.isRoot(artist)).thenReturn(false);
        Mockito.when(securityService.isFolderAccessAllowed(artist, ServiceMockUtils.ADMIN_NAME)).thenReturn(true);
        Mockito.when(mediaFileService.getChildrenOf(artist, true, true, true)).thenReturn(Arrays.asList(album));
        Mockito.when(mediaFileService.getParentOf(artist)).thenReturn(root);

        album = new MediaFile();
        int albumId = 300;
        String albumPath = MainControllerTest.class
                .getResource("/MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice").getPath();
        album.setId(albumId);
        album.setPath(albumPath);
        album.setMediaType(MediaType.ALBUM);

        Mockito.when(mediaFileService.getMediaFile(albumPath)).thenReturn(album);
        Mockito.when(mediaFileService.getMediaFile(albumId)).thenReturn(album);
        Mockito.when(mediaFileService.isRoot(album)).thenReturn(false);
        Mockito.when(securityService.isFolderAccessAllowed(album, ServiceMockUtils.ADMIN_NAME)).thenReturn(true);
        Mockito.when(mediaFileService.getChildrenOf(artist, true, true, true)).thenReturn(Arrays.asList(album));

        song = new MediaFile();
        int songId = 400;
        String songPath = MainControllerTest.class.getResource(
                "/MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice/01 - Sonata Violin & Cello I. Allegro.ogg")
                .getPath();
        song.setId(songId);
        song.setPath(songPath);
        song.setMediaType(MediaType.MUSIC);
        Mockito.when(mediaFileService.getMediaFile(songPath)).thenReturn(song);
        Mockito.when(mediaFileService.getMediaFile(songId)).thenReturn(song);
        Mockito.when(mediaFileService.isRoot(song)).thenReturn(false);
        Mockito.when(mediaFileService.getParentOf(song)).thenReturn(album);
        Mockito.when(mediaFileService.getChildrenOf(album, true, true, true)).thenReturn(Arrays.asList(song));
    }

    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {

        // with ID
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value())
                .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("albumMain", modelAndView.getViewName());
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(Attributes.Model.VALUE);
        assertNotNull(model);

        // ... or path
        result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value())
                .param(Attributes.Request.PATH.value(), album.getPath()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        modelAndView = result.getModelAndView();
        assertEquals("albumMain", modelAndView.getViewName());
        model = (Map<String, Object>) modelAndView.getModel().get(Attributes.Model.VALUE);
        assertNotNull(model);

        // Returns parent if not directory.
        mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value()).param(Attributes.Request.ID.value(),
                Integer.toString(song.getId()))).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        modelAndView = result.getModelAndView();
        assertEquals("albumMain", modelAndView.getViewName());
        model = (Map<String, Object>) modelAndView.getModel().get(Attributes.Model.VALUE);
        assertNotNull(model);
    }

    /*
     * Verify the properties related to Children of the target media file
     */
    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGetForChildren() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value())
                .param(Attributes.Request.ID.value(), Integer.toString(artist.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("artistMain", modelAndView.getViewName());
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get(Attributes.Model.VALUE);
        assertNotNull(model);

        assertEquals(0, ((List<MediaFile>) model.get("files")).size());
        List<MediaFile> subDirs = (List<MediaFile>) model.get("subDirs");
        assertEquals(1, subDirs.size());
        assertEquals(album, subDirs.get(0));
        assertEquals(root, (MediaFile) model.get("parent"));
        assertFalse((Boolean) model.get("navigateUpAllowed"));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testGetFail() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value()).param(Attributes.Request.ID.value(),
                Integer.toString(-1))).andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.NOTFOUND.value()));

        Mockito.when(securityService.isFolderAccessAllowed(album, ServiceMockUtils.ADMIN_NAME)).thenReturn(false);
        mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value()).param(Attributes.Request.ID.value(),
                Integer.toString(album.getId()))).andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.ACCESS_DENIED.value()));

        // Redirect to home if root directory.
        mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MAIN.value()).param(Attributes.Request.ID.value(),
                Integer.toString(root.getId()))).andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.HOME.value() + "?"));
    }
}
