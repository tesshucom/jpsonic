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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.command.AdvancedSettingsCommand;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
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

@SuppressWarnings("PMD.TooManyStaticImports")
class AdvancedSettingsControllerTest {

    private static final String VIEW_NAME = "advancedSettings";

    private SettingsService settingsService;
    private AdvancedSettingsController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        controller = new AdvancedSettingsController(settingsService, mock(SecurityService.class),
                mock(ShareService.class), mock(OutlineHelpSelector.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPost() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        result = mockMvc
                .perform(MockMvcRequestBuilders.post("/" + ViewName.ADVANCED_SETTINGS.value())
                        .flashAttr(Attributes.Model.Command.VALUE, command))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    /*
     * Full scan required if schema changes (Compare before reflecting settings)
     */
    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testChangeIndexScheme() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.ADVANCED_SETTINGS.value()))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);
        ModelAndView modelAndView = result.getModelAndView();
        assertEquals(VIEW_NAME, modelAndView.getViewName());

        AdvancedSettingsCommand command = (AdvancedSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        assertFalse(command.isReadGreekInJapanese());
        assertFalse(command.isForceInternalValueInsteadOfTags());
        command.setReadGreekInJapanese(true);
        command.setForceInternalValueInsteadOfTags(true);

        assertEquals(IndexScheme.NATIVE_JAPANESE, command.getIndexScheme());
        assertFalse(settingsService.isIgnoreFileTimestamps());
        assertFalse(settingsService.isIgnoreFileTimestampsNext());

        ArgumentCaptor<Boolean> ignoreFileTimestampsNext = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> readGreekInJapanese = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Boolean> forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsNext(ignoreFileTimestampsNext.capture());
        Mockito.doNothing().when(settingsService).setReadGreekInJapanese(readGreekInJapanese.capture());
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());

        command.setIndexScheme(IndexScheme.NATIVE_JAPANESE);
        command.setReadGreekInJapanese(true);

        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1)).setIgnoreFileTimestampsNext(Mockito.anyBoolean());
        assertTrue(readGreekInJapanese.getValue());
        assertFalse(forceInternalValueInsteadOfTags.getValue());

        Mockito.clearInvocations(settingsService);
        ignoreFileTimestampsNext = ArgumentCaptor.forClass(Boolean.class);
        readGreekInJapanese = ArgumentCaptor.forClass(Boolean.class);
        forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsNext(ignoreFileTimestampsNext.capture());
        Mockito.doNothing().when(settingsService).setReadGreekInJapanese(readGreekInJapanese.capture());
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());
        command.setIndexScheme(IndexScheme.ROMANIZED_JAPANESE);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1)).setIgnoreFileTimestampsNext(Mockito.anyBoolean());
        assertTrue(ignoreFileTimestampsNext.getValue());
        assertFalse(readGreekInJapanese.getValue());
        assertTrue(forceInternalValueInsteadOfTags.getValue());

        Mockito.clearInvocations(settingsService);
        ignoreFileTimestampsNext = ArgumentCaptor.forClass(Boolean.class);
        readGreekInJapanese = ArgumentCaptor.forClass(Boolean.class);
        forceInternalValueInsteadOfTags = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsNext(ignoreFileTimestampsNext.capture());
        Mockito.doNothing().when(settingsService).setReadGreekInJapanese(readGreekInJapanese.capture());
        Mockito.doNothing().when(settingsService)
                .setForceInternalValueInsteadOfTags(forceInternalValueInsteadOfTags.capture());
        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1)).setIgnoreFileTimestampsNext(Mockito.anyBoolean());
        Mockito.verify(settingsService, Mockito.times(1)).setReadGreekInJapanese(Mockito.anyBoolean());
        Mockito.verify(settingsService, Mockito.times(1)).setForceInternalValueInsteadOfTags(Mockito.anyBoolean());
        assertTrue(ignoreFileTimestampsNext.getValue());

        ignoreFileTimestampsNext = ArgumentCaptor.forClass(Boolean.class);
        Mockito.doNothing().when(settingsService).setIgnoreFileTimestampsNext(ignoreFileTimestampsNext.capture());
        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));

        ignoreFileTimestampsNext = ArgumentCaptor.forClass(Boolean.class);
        Mockito.clearInvocations(settingsService);
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        Mockito.when(settingsService.isIgnoreFileTimestampsNext()).thenReturn(false);
        assertEquals(IndexScheme.NATIVE_JAPANESE.name(), settingsService.getIndexSchemeName());
        command.setIndexScheme(IndexScheme.WITHOUT_JP_LANG_PROCESSING);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.times(1))
                .setIgnoreFileTimestampsNext(ignoreFileTimestampsNext.capture());
        assertTrue(ignoreFileTimestampsNext.getValue());

        Mockito.clearInvocations(settingsService);
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        Mockito.when(settingsService.isIgnoreFileTimestampsNext()).thenReturn(true);
        command.setIndexScheme(IndexScheme.NATIVE_JAPANESE);
        command.setReadGreekInJapanese(false);
        controller.post(command, Mockito.mock(RedirectAttributes.class));
        Mockito.verify(settingsService, Mockito.never()).setIgnoreFileTimestampsNext(Mockito.anyBoolean());
    }
}
