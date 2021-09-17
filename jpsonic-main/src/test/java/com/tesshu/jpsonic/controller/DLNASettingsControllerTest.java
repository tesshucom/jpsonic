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

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.DLNASettingsCommand;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.UPnPService;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

class DLNASettingsControllerTest {

    private SettingsService settingsService;
    private MusicFolderService musicFolderService;
    private UPnPService upnpService;
    private DLNASettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        musicFolderService = mock(MusicFolderService.class);
        upnpService = mock(UPnPService.class);
        controller = new DLNASettingsController(settingsService, musicFolderService, mock(SecurityService.class),
                mock(PlayerService.class), mock(TranscodingService.class), upnpService, mock(ShareService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.DLNA_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("dlnaSettings", modelAndView.getViewName());

        DLNASettingsCommand command = (DLNASettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPost() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.DLNA_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("dlnaSettings", modelAndView.getViewName());

        DLNASettingsCommand command = (DLNASettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.DLNA_SETTINGS.value()).flashAttr("model", command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.DLNA_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Test
    void testIsDlnaGenreCountVisible() {

        DLNASettingsCommand command = new DLNASettingsCommand();
        command.setDlnaGenreCountVisible(false);
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(boolean.class);
        Mockito.doNothing().when(settingsService).setDlnaGenreCountVisible(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1)).setDlnaGenreCountVisible(Mockito.any(boolean.class));
        assertFalse(captor.getValue());

        command.setDlnaGenreCountVisible(true);
        Mockito.doNothing().when(settingsService).setDlnaGenreCountVisible(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(2)).setDlnaGenreCountVisible(Mockito.any(boolean.class));
        Assertions.assertTrue(captor.getValue());

        /*
         * Always false if all folders are not allowed. Because the genre count is a statistical result for all
         * directories.
         */
        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(0, null, null, true, null));
        Mockito.when(musicFolderService.getAllMusicFolders()).thenReturn(musicFolders);
        Mockito.when(musicFolderService.getMusicFoldersForUser(User.USERNAME_GUEST)).thenReturn(musicFolders);

        command.setDlnaGenreCountVisible(false);
        captor = ArgumentCaptor.forClass(boolean.class);
        Mockito.doNothing().when(settingsService).setDlnaGenreCountVisible(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(3)).setDlnaGenreCountVisible(Mockito.any(boolean.class));
        assertFalse(captor.getValue());

        command.setDlnaGenreCountVisible(true);
        Mockito.doNothing().when(settingsService).setDlnaGenreCountVisible(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(4)).setDlnaGenreCountVisible(Mockito.any(boolean.class));
        assertFalse(captor.getValue());
    }

    @Documented
    private @interface MediaServerEnabledDecision {
        @interface Conditions {
            @interface EnabledChanged {
                @interface False {
                }

                @interface True {
                }
            }

            @interface NameOrUrlChanged {
                @interface False {
                }

                @interface True {
                }
            }

            @interface Command {
                @interface DlnaEnabled {
                    @interface False {
                    }

                    @interface True {
                    }
                }
            }

        }

        @interface Results {
            @interface Never {
            }

            @interface MediaServerEnabled {
                @interface False {
                }

                @interface True {
                }
            }

        }

    }

    @Nested
    class MediaServerEnabledTest {

        private String defaultDlnaServerName = "jpsonic";
        private String defaultDlnaBaseLANURL = "url";

        @MediaServerEnabledDecision.Conditions.EnabledChanged.False
        @MediaServerEnabledDecision.Conditions.NameOrUrlChanged.False
        @MediaServerEnabledDecision.Results.Never
        // Never (Nothing has changed)
        @Test
        void m01() {
            Mockito.when(settingsService.isDlnaEnabled()).thenReturn(false);
            Mockito.when(settingsService.getDlnaServerName()).thenReturn(defaultDlnaServerName);
            Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn(defaultDlnaBaseLANURL);

            DLNASettingsCommand command = new DLNASettingsCommand();
            command.setDlnaEnabled(false);
            command.setDlnaServerName(defaultDlnaServerName);
            command.setDlnaBaseLANURL(defaultDlnaBaseLANURL);

            controller.post(command, Mockito.mock(RedirectAttributes.class));
            Mockito.verify(upnpService, Mockito.never()).setMediaServerEnabled(Mockito.any(boolean.class));
        }

        @MediaServerEnabledDecision.Conditions.Command.DlnaEnabled.True
        @MediaServerEnabledDecision.Conditions.EnabledChanged.True
        @MediaServerEnabledDecision.Conditions.NameOrUrlChanged.False
        @MediaServerEnabledDecision.Results.MediaServerEnabled.True
        @Test
        // Boot
        void m02() {
            Mockito.when(settingsService.isDlnaEnabled()).thenReturn(false);
            Mockito.when(settingsService.getDlnaServerName()).thenReturn(defaultDlnaServerName);
            Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn(defaultDlnaBaseLANURL);

            DLNASettingsCommand command = new DLNASettingsCommand();
            command.setDlnaEnabled(true);
            command.setDlnaServerName(defaultDlnaServerName);
            command.setDlnaBaseLANURL(defaultDlnaBaseLANURL);

            ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(boolean.class);
            Mockito.doNothing().when(upnpService).setMediaServerEnabled(captor.capture());
            controller.post(command, Mockito.mock(RedirectAttributes.class));
            Mockito.verify(upnpService, Mockito.times(1)).setMediaServerEnabled(Mockito.any(boolean.class));
            Assertions.assertTrue(captor.getValue());
        }

        @MediaServerEnabledDecision.Conditions.Command.DlnaEnabled.False
        @MediaServerEnabledDecision.Conditions.EnabledChanged.True
        @MediaServerEnabledDecision.Conditions.NameOrUrlChanged.False
        @MediaServerEnabledDecision.Results.MediaServerEnabled.False
        @Test
        // Shutdown
        void m03() {
            Mockito.when(settingsService.isDlnaEnabled()).thenReturn(true);
            Mockito.when(settingsService.getDlnaServerName()).thenReturn(defaultDlnaServerName);
            Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn(defaultDlnaBaseLANURL);

            DLNASettingsCommand command = new DLNASettingsCommand();
            command.setDlnaEnabled(false);
            command.setDlnaServerName(defaultDlnaServerName);
            command.setDlnaBaseLANURL(defaultDlnaBaseLANURL);

            ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(boolean.class);
            Mockito.doNothing().when(upnpService).setMediaServerEnabled(captor.capture());
            controller.post(command, Mockito.mock(RedirectAttributes.class));
            Mockito.verify(upnpService, Mockito.times(1)).setMediaServerEnabled(Mockito.any(boolean.class));
            assertFalse(captor.getValue());
        }

        @MediaServerEnabledDecision.Conditions.Command.DlnaEnabled.False
        @MediaServerEnabledDecision.Conditions.EnabledChanged.False
        @MediaServerEnabledDecision.Conditions.NameOrUrlChanged.True
        @MediaServerEnabledDecision.Results.Never
        // Never (Do nothing if you change the name and URL while DLNA is stopped)
        @Test
        void m04() {
            Mockito.when(settingsService.isDlnaEnabled()).thenReturn(false);
            Mockito.when(settingsService.getDlnaServerName()).thenReturn(defaultDlnaServerName);
            Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn(defaultDlnaBaseLANURL);

            DLNASettingsCommand command = new DLNASettingsCommand();
            command.setDlnaEnabled(false);
            command.setDlnaServerName("changedDlnaServerName");
            command.setDlnaBaseLANURL(defaultDlnaBaseLANURL);

            controller.post(command, Mockito.mock(RedirectAttributes.class));
            Mockito.verify(upnpService, Mockito.never()).setMediaServerEnabled(Mockito.any(boolean.class));
        }

        @MediaServerEnabledDecision.Conditions.Command.DlnaEnabled.True
        @MediaServerEnabledDecision.Conditions.EnabledChanged.False
        @MediaServerEnabledDecision.Conditions.NameOrUrlChanged.True
        @MediaServerEnabledDecision.Results.MediaServerEnabled.False
        @MediaServerEnabledDecision.Results.MediaServerEnabled.True
        // Reboot
        @Test
        void m05() {
            Mockito.when(settingsService.isDlnaEnabled()).thenReturn(true);
            Mockito.when(settingsService.getDlnaServerName()).thenReturn(defaultDlnaServerName);
            Mockito.when(settingsService.getDlnaBaseLANURL()).thenReturn(defaultDlnaBaseLANURL);

            DLNASettingsCommand command = new DLNASettingsCommand();
            command.setDlnaEnabled(true);
            command.setDlnaServerName(defaultDlnaServerName);
            command.setDlnaBaseLANURL("changedDlnaBaseLANURL");

            ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(boolean.class);
            Mockito.doNothing().when(upnpService).setMediaServerEnabled(captor.capture());
            controller.post(command, Mockito.mock(RedirectAttributes.class));
            Mockito.verify(upnpService, Mockito.times(2)).setMediaServerEnabled(Mockito.any(boolean.class));
            assertFalse(captor.getAllValues().get(0));
            Assertions.assertTrue(captor.getAllValues().get(1));
        }
    }
}
