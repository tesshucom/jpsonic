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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
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

class VideoPlayerControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {

        MediaFile file = new MediaFile();
        file.setId(0);
        MediaFile dir = new MediaFile();
        dir.setId(1);
        dir.setPath("");

        MediaFileService mediaFileService = mock(MediaFileService.class);
        Mockito.when(mediaFileService.getMediaFile(file.getId())).thenReturn(file);
        Mockito.when(mediaFileService.getParentOf(file)).thenReturn(dir);
        Mockito.doNothing().when(mediaFileService).populateStarredDate(Mockito.any(MediaFile.class), Mockito.any());

        SecurityService securityService = mock(SecurityService.class);
        Mockito.when(securityService.isInPodcastFolder(Mockito.any())).thenReturn(true);
        Mockito.when(securityService.isFolderAccessAllowed(dir, ServiceMockUtils.ADMIN_NAME)).thenReturn(true);

        PlayerService playerService = Mockito.mock(PlayerService.class);
        Player player = new Player();
        player.setId(100);
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        Mockito.when(playerService.getPlayerById(player.getId())).thenReturn(player);
        Mockito.when(playerService.getPlayerById(0)).thenReturn(player);
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(player);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new VideoPlayerController(securityService, mediaFileService, playerService)).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
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
