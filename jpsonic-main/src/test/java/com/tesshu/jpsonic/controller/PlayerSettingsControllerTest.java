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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.PlayerSettingsCommand;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

class PlayerSettingsControllerTest {

    private SettingsService settingsService;
    private PlayerService playerService;
    private PlayerSettingsController controller;
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
        Mockito.when(playerService.getAllPlayers()).thenReturn(Arrays.asList(player));

        settingsService = mock(SettingsService.class);
        TranscodingDao transcodingDao = mock(TranscodingDao.class);
        List<Transcoding> allTranscodings = transcodingDao.getAllTranscodings();
        TranscodingService transcodingService = mock(TranscodingService.class);
        Mockito.when(transcodingService.getTranscodingsForPlayer(Mockito.any())).thenReturn(allTranscodings);
        controller = new PlayerSettingsController(settingsService, mock(SecurityService.class), playerService,
                transcodingService, mock(ShareService.class), mock(OutlineHelpSelector.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testUseExternalPlayer() throws Exception {

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

        command.setPlayerTechnology(PlayerTechnology.EXTERNAL);
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        Mockito.doNothing().when(playerService).updatePlayer(playerCaptor.capture());
        controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(playerService, Mockito.times(1)).updatePlayer(Mockito.any(Player.class));
        // If useExternalPlayer is false, the initial value will be registered
        assertEquals(PlayerTechnology.WEB, playerCaptor.getValue().getTechnology());

        Mockito.clearInvocations(playerService);
        // If true
        Mockito.when(settingsService.isUseExternalPlayer()).thenReturn(true);
        playerCaptor = ArgumentCaptor.forClass(Player.class);
        Mockito.doNothing().when(playerService).updatePlayer(playerCaptor.capture());
        controller.doSubmitAction(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(playerService, Mockito.times(1)).updatePlayer(Mockito.any(Player.class));
        // Input from the web page will be registered
        assertEquals(PlayerTechnology.EXTERNAL, playerCaptor.getValue().getTechnology());
    }
}
