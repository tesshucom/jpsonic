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

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.tesshu.jpsonic.command.MusicFolderSettingsCommand;
import com.tesshu.jpsonic.command.MusicFolderSettingsCommand.MusicFolderInfo;
import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MusicFolderSettingsControllerTest {

    private static final String VIEW_NAME = "musicFolderSettings";

    private SettingsService settingsService;
    private MusicFolderService musicFolderService;
    private MediaScannerService mediaScannerService;
    private MusicFolderSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        musicFolderService = mock(MusicFolderService.class);
        mediaScannerService = mock(MediaScannerService.class);
        controller = new MusicFolderSettingsController(settingsService, musicFolderService, mock(SecurityService.class),
                mediaScannerService, mock(ShareService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @Order(1)
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(2)
    void testDoScan() throws Exception {

        Mockito.doNothing().when(mediaScannerService).scanLibrary();

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())
                        .param(Attributes.Request.NameConstants.SCAN_NOW, "true"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        Mockito.verify(mediaScannerService, Mockito.times(1)).scanLibrary();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(3)
    void testDoExpunge() throws Exception {

        Mockito.doNothing().when(mediaScannerService).expunge();

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())
                        .param(Attributes.Request.NameConstants.EXPUNGE, "true"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        Mockito.verify(mediaScannerService, Mockito.times(1)).expunge();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(4)
    void testPost() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(5)
    void testCreateMusicFolder() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        File dir = new File(MusicFolderSettingsControllerTest.class.getResource("/MEDIAS/Music").toURI());
        MusicFolder musicFolder = new MusicFolder(dir, "Music", true, new Date());
        MusicFolderInfo musicFolderInfo = new MusicFolderInfo(musicFolder);
        command.setNewMusicFolder(musicFolderInfo);

        ArgumentCaptor<MusicFolder> captor = ArgumentCaptor.forClass(MusicFolder.class);
        Mockito.doNothing().when(musicFolderService).createMusicFolder(captor.capture());

        RedirectAttributes redirectAttributes = Mockito.mock(RedirectAttributes.class);
        controller.post(command, redirectAttributes);
        assertEquals(captor.getValue(), musicFolder);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Order(6)
    void testUpdateAndDelteMusicFolder() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        File dir = new File(MusicFolderSettingsControllerTest.class.getResource("/MEDIAS/Music").toURI());
        MusicFolder musicFolder1 = new MusicFolder(99, dir, "Music", true, new Date());
        MusicFolderInfo musicFolderInfo1 = new MusicFolderInfo(musicFolder1);
        musicFolderInfo1.setDelete(true);
        File dir2 = new File(MusicFolderSettingsControllerTest.class.getResource("/MEDIAS/Music2").toURI());
        MusicFolder musicFolder2 = new MusicFolder(dir2, null, true, new Date());
        MusicFolderInfo musicFolderInfo2 = new MusicFolderInfo(musicFolder2);
        command.setMusicFolders(Arrays.asList(musicFolderInfo1, musicFolderInfo2));

        // Case where the registered path is deleted on the web page
        File dir3 = new File(MusicFolderSettingsControllerTest.class.getResource("/MEDIAS/Music3").toURI());
        MusicFolder musicFolder3 = new MusicFolder(dir3, null, true, new Date());
        MusicFolderInfo musicFolderInfo3 = new MusicFolderInfo(musicFolder3);
        musicFolderInfo3.setPath(null);
        // Cases that do not (already) exist. Update will be executed but will be ignored in Dao.
        File dir4 = new File("/Unknown");
        MusicFolder musicFolder4 = new MusicFolder(dir4, null, true, new Date());
        MusicFolderInfo musicFolderInfo4 = new MusicFolderInfo(musicFolder4);

        command.setMusicFolders(Arrays.asList(musicFolderInfo1, musicFolderInfo2, musicFolderInfo3, musicFolderInfo4));

        ArgumentCaptor<Integer> captorDelete = ArgumentCaptor.forClass(int.class);
        Mockito.doNothing().when(musicFolderService).deleteMusicFolder(captorDelete.capture());
        ArgumentCaptor<MusicFolder> captorUpdate = ArgumentCaptor.forClass(MusicFolder.class);
        Mockito.doNothing().when(musicFolderService).updateMusicFolder(captorUpdate.capture());

        RedirectAttributes redirectAttributes = Mockito.mock(RedirectAttributes.class);
        controller.post(command, redirectAttributes);

        assertEquals(captorDelete.getValue(), musicFolder1.getId());
        assertEquals(captorUpdate.getValue(), musicFolder2);
        assertEquals(2, captorUpdate.getAllValues().size());
    }

    @Test
    @Order(10)
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testIfFullScanNext() throws Exception {

        @SuppressWarnings("PMD.AvoidCatchingGenericException")
        Supplier<MusicFolderSettingsCommand> supplier = () -> {
            try {
                return (MusicFolderSettingsCommand) mockMvc
                        .perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())).andReturn()
                        .getModelAndView().getModelMap().get(Attributes.Model.Command.VALUE);
            } catch (Exception e) {
                Assertions.fail();
            }
            return null;
        };

        // Basically should be false.
        MusicFolderSettingsCommand command = supplier.get();
        assertFalse(command.isFullScanNext());

        // Full scan if any property of IgnoreFileTimestamps* is true.
        Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(true);
        Mockito.when(settingsService.isIgnoreFileTimestampsNext()).thenReturn(false);
        Assertions.assertTrue(supplier.get().isFullScanNext());
        Mockito.when(settingsService.isIgnoreFileTimestamps()).thenReturn(false);
        Mockito.when(settingsService.isIgnoreFileTimestampsNext()).thenReturn(true);
        Assertions.assertTrue(supplier.get().isFullScanNext());

        /*
         * IgnoreFileTimestamps is intentionally set by the user from the web page. IgnoreFileTimestamps is a hidden
         * option on legacy servers and was kept private due to the difficulty of understanding it. (The cases to use
         * are very limited.) IgnoreFileTimestampsNext is set when it is needed for server processing, not the user.
         * "Next" is set to false once scan has been performed.
         */
    }

    @Test
    @Order(11)
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testIfIgnoreFileTimestamps() throws Exception {

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())).andReturn()
                .getModelAndView().getModelMap().get(Attributes.Model.Command.VALUE);
        assertFalse(command.isIgnoreFileTimestamps());

        // IgnoreFileTimestamps is enabled only when the check method is LAST_MODIFIED.
        command.setFileModifiedCheckScheme(FileModifiedCheckScheme.LAST_MODIFIED);
        command.setIgnoreFileTimestamps(true);
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestamps(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Assertions.assertTrue(captor.getValue());

        // Disabled if LAST_SCANNED is specified.
        command.setFileModifiedCheckScheme(FileModifiedCheckScheme.LAST_SCANNED);
        command.setIgnoreFileTimestamps(true);
        captor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestamps(captor.capture());
        command.setIgnoreFileTimestamps(true);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        assertFalse(captor.getValue());
    }

    @Test
    @Order(12)
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testIfIgnoreFileTimestampsForEachAlbum() throws Exception {

        MusicFolderSettingsCommand command = (MusicFolderSettingsCommand) mockMvc
                .perform(MockMvcRequestBuilders.get("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())).andReturn()
                .getModelAndView().getModelMap().get(Attributes.Model.Command.VALUE);
        assertFalse(command.isIgnoreFileTimestampsForEachAlbum());

        /*
         * isIgnoreFileTimestampsForEachAlbum is a flag to show the scan-force-button on the album page. Default is
         * false.
         */
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsForEachAlbum(captor.capture());
        mockMvc.perform(MockMvcRequestBuilders.post("/" + ViewName.MUSIC_FOLDER_SETTINGS.value())
                .flashAttr(Attributes.Model.Command.VALUE, command)).andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.MUSIC_FOLDER_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        assertFalse(captor.getValue());

        /*
         * Forced scanning each album can coexist with traditional scanning specifications. Show button if
         * IgnoreFileTimestampsForEachAlbum is true.
         */
        command.setIgnoreFileTimestampsForEachAlbum(true);
        command.setFileModifiedCheckScheme(FileModifiedCheckScheme.LAST_MODIFIED);
        captor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsForEachAlbum(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Assertions.assertTrue(captor.getValue());

        /*
         * Depending on the scan check method, it is necessary to always display a button on the album page.
         */
        command.setIgnoreFileTimestampsForEachAlbum(false);
        command.setFileModifiedCheckScheme(FileModifiedCheckScheme.LAST_SCANNED);
        captor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsForEachAlbum(captor.capture());
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Assertions.assertTrue(captor.getValue());
    }
}
