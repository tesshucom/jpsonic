/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.UserSettingsCommand;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

class UserSettingsControllerTest {

    private SecurityService securityService;
    private MusicFolderService musicFolderService;
    private PlayerService playerService;
    private UserSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        securityService = mock(SecurityService.class);
        musicFolderService = mock(MusicFolderService.class);
        playerService = mock(PlayerService.class);
        controller = new UserSettingsController(mock(SettingsService.class), musicFolderService, securityService,
                mock(TranscodingService.class), mock(ShareService.class), playerService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.USER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("userSettings", modelAndView.getViewName());

        UserSettingsCommand command = (UserSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPost() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.USER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("userSettings", modelAndView.getViewName());

        UserSettingsCommand command = (UserSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.USER_SETTINGS.value())
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.USER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Nested
    class UpdateUserTest {

        @Test
        void testUpdateUser() throws Exception {

            User user = new User("updateTest", "notChangedPassword", "");
            Mockito.when(securityService.getUserByName(user.getUsername())).thenReturn(user);
            UserSettings settings = new UserSettings(user.getUsername());
            Mockito.when(securityService.getUserSettings(user.getUsername())).thenReturn(settings);

            UserSettingsCommand command = new UserSettingsCommand();
            command.setUsername(user.getUsername());
            command.setLdapAuthenticated(true);
            command.setPasswordChange(false);
            command.setPassword("updatedPassword");
            command.setEmail("updatedEmail");
            command.setAdminRole(true);
            command.setSettingsRole(true);
            command.setStreamRole(true);
            command.setDownloadRole(true);
            command.setUploadRole(true);
            command.setShareRole(true);
            command.setCoverArtRole(true);
            command.setCommentRole(true);
            command.setPodcastRole(true);
            command.setTranscodeScheme(TranscodeScheme.OFF);
            command.setAllowedMusicFolderIds(1, 2, 3);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            Mockito.doNothing().when(securityService).updateUser(userCaptor.capture());
            ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
            Mockito.doNothing().when(securityService).updateUserSettings(settingsCaptor.capture());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Integer>> idsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.doNothing().when(musicFolderService).setMusicFoldersForUser(Mockito.anyString(),
                    idsCaptor.capture());

            controller.updateUser(command);

            User updatedUser = userCaptor.getValue();
            assertEquals("updateTest", updatedUser.getUsername());
            assertEquals("notChangedPassword", updatedUser.getPassword());
            assertEquals("updatedEmail", updatedUser.getEmail());
            assertTrue(updatedUser.isAdminRole());
            assertTrue(updatedUser.isSettingsRole());
            assertTrue(updatedUser.isStreamRole());
            assertTrue(updatedUser.isDownloadRole());
            assertTrue(updatedUser.isUploadRole());
            assertTrue(updatedUser.isShareRole());
            assertTrue(updatedUser.isCoverArtRole());
            assertTrue(updatedUser.isCommentRole());
            assertTrue(updatedUser.isPodcastRole());
            UserSettings updatedSettings = settingsCaptor.getValue();
            assertEquals(TranscodeScheme.OFF, updatedSettings.getTranscodeScheme());
            assertNotNull(updatedSettings.getChanged());
            assertEquals(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)),
                    idsCaptor.getValue());
        }

        @Test
        void testUpdateUserChangePass() throws Exception {

            User user = new User("changePass", "", "");
            Mockito.when(securityService.getUserByName(user.getUsername())).thenReturn(user);
            UserSettings settings = new UserSettings(user.getUsername());
            Mockito.when(securityService.getUserSettings(user.getUsername())).thenReturn(settings);

            UserSettingsCommand command = new UserSettingsCommand();
            command.setUsername(user.getUsername());

            command.setPasswordChange(true);
            command.setPassword("updatedPassword");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            Mockito.doNothing().when(securityService).updateUser(userCaptor.capture());

            controller.updateUser(command);

            User updatedUser = userCaptor.getValue();
            assertEquals("updatedPassword", updatedUser.getPassword());
        }

        /*
         * When the bit rate is changed, the value of the player whose value is set to be larger than that value is
         * changed to the new value.
         */
        @Test
        void testUpdateUserChangeTranscodeScheme() throws Exception {

            User user = new User("changeTranscode", "", "");
            Mockito.when(securityService.getUserByName(user.getUsername())).thenReturn(user);
            UserSettings settings = new UserSettings(user.getUsername());
            Mockito.when(securityService.getUserSettings(user.getUsername())).thenReturn(settings);

            final List<Player> players = new ArrayList<>();
            Player player1 = new Player();
            player1.setId(1);
            player1.setUsername(user.getUsername());
            player1.setTranscodeScheme(TranscodeScheme.OFF);
            players.add(player1);
            Player player2 = new Player();
            player2.setId(2);
            player2.setUsername(user.getUsername());
            player2.setTranscodeScheme(TranscodeScheme.MAX_128);
            players.add(player2);
            Player player3 = new Player();
            player3.setId(3);
            player3.setUsername(user.getUsername());
            player3.setTranscodeScheme(TranscodeScheme.MAX_256);
            players.add(player3);
            Player player4 = new Player();
            player4.setId(4);
            player4.setUsername(user.getUsername());
            player4.setTranscodeScheme(TranscodeScheme.MAX_320);
            players.add(player4);
            Mockito.when(playerService.getAllPlayers()).thenReturn(players);

            // When changed to 256...
            UserSettingsCommand command = new UserSettingsCommand();
            command.setUsername(user.getUsername());
            command.setTranscodeScheme(TranscodeScheme.MAX_256);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerService).updatePlayer(playerCaptor.capture());

            controller.updateUser(command);

            List<Player> updatedPlayers = playerCaptor.getAllValues();
            assertEquals(2, updatedPlayers.size());

            // OFF and 320 will be changed to 256
            assertEquals(1, updatedPlayers.get(0).getId());
            assertEquals(TranscodeScheme.MAX_256, updatedPlayers.get(0).getTranscodeScheme());
            assertEquals(4, updatedPlayers.get(1).getId());
            assertEquals(TranscodeScheme.MAX_256, updatedPlayers.get(1).getTranscodeScheme());
        }
    }
}
