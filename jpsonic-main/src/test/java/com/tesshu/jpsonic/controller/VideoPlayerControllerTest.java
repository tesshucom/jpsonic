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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(NeedsHome.class)
class VideoPlayerControllerTest {

    private static final String ADMIN_NAME = "admin";

    @Mock
    private MediaFileService mediaFileService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SecurityService securityService;
    @Mock
    private SecurityService securityMock;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        MediaFile file = new MediaFile();
        file.setId(0);
        MediaFile dir = new MediaFile();
        dir.setId(1);
        dir.setPath("");
        User user = new User(ADMIN_NAME, ADMIN_NAME, ADMIN_NAME);
        Mockito.when(mediaFileService.getMediaFile(file.getId())).thenReturn(file);
        Mockito.when(mediaFileService.getParentOf(file)).thenReturn(dir);
        Mockito.when(securityMock.getCurrentUsername(Mockito.any())).thenReturn(ADMIN_NAME);
        Mockito.when(securityMock.isFolderAccessAllowed(dir, ADMIN_NAME)).thenReturn(true);
        Mockito.when(securityMock.getUserSettings(ADMIN_NAME)).thenReturn(securityService.getUserSettings(ADMIN_NAME));
        Mockito.when(securityMock.getCurrentUser(Mockito.any())).thenReturn(user);
        Mockito.doNothing().when(mediaFileService).populateStarredDate(Mockito.any(MediaFile.class), Mockito.any());
        Mockito.when(securityMock.isInPodcastFolder(Mockito.any())).thenReturn(true);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VideoPlayerController(securityMock, mediaFileService, playerService)).build();
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testFormBackingObject() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/videoPlayer.view").param(Attributes.Request.ID.value(), "0"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("videoPlayer", modelAndView.getViewName());

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) modelAndView.getModel().get("model");
        assertNotNull(model);
    }
}
