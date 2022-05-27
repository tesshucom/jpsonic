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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
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

class MoreControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException, URISyntaxException {
        MusicFolderService musicFolderService = mock(MusicFolderService.class);
        SearchService searchService = mock(SearchService.class);
        PlayerService playerService = mock(PlayerService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MoreController(musicFolderService, mock(SecurityService.class), playerService, searchService))
                .build();
        Mockito.when(searchService.getGenres(false)).thenReturn(Collections.emptyList());
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(new Player());
        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(1,
                Path.of(MoreControllerTest.class.getResource("/MEDIAS").toURI()).toFile(), "MEDIAS", true, new Date()));
        Mockito.when(musicFolderService.getMusicFoldersForUser(ServiceMockUtils.ADMIN_NAME)).thenReturn(musicFolders);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testHandleRequestInternal() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MORE.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("more", modelAndView.getViewName());
    }
}
