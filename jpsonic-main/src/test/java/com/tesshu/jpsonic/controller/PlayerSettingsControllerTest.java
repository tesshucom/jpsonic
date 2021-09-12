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

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.PlayerSettingsCommand;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class PlayerSettingsControllerTest {

    private PlayerService playerService;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {

        playerService = Mockito.mock(PlayerService.class);
        Player player = new Player();
        player.setId(100);
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        Mockito.when(playerService.getPlayerById(player.getId())).thenReturn(player);
        Mockito.when(playerService.getPlayerById(0)).thenReturn(player);
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(player);

        mockMvc = MockMvcBuilders.standaloneSetup(
                new PlayerSettingsController(mock(SettingsService.class), mock(SecurityService.class), playerService,
                        mock(TranscodingService.class), mock(ShareService.class), mock(OutlineHelpSelector.class)))
                .build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testDisplayForm() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.PLAYER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("playerSettings", modelAndView.getViewName());

        PlayerSettingsCommand command = (PlayerSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testDoSubmitAction() throws Exception {

        Player player = playerService.getPlayer(new MockHttpServletRequest(), new MockHttpServletResponse());

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.PLAYER_SETTINGS.value())
                        .param(Attributes.Request.ID.value(), Integer.toString(player.getId())))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("playerSettings", modelAndView.getViewName());

        PlayerSettingsCommand command = (PlayerSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.PLAYER_SETTINGS.value())
                        .param(Attributes.Request.ID.value(), Integer.toString(player.getId()))
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.PLAYER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

}
