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

import com.tesshu.jpsonic.command.PasswordSettingsCommand;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.validator.PasswordSettingsValidator;
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

class PasswordSettingsControllerTest {

    private MockMvc mockMvc;

    private SecurityService securityService;

    @BeforeEach
    public void setup() throws ExecutionException {
        securityService = mock(SecurityService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PasswordSettingsController(securityService, new PasswordSettingsValidator()))
                .build();
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGet() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/passwordSettings.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        assertEquals("passwordSettings", modelAndView.getViewName());
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPost() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/passwordSettings.view"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        assertNotNull(result);

        ModelAndView modelAndView = result.getModelAndView();
        PasswordSettingsCommand command = (PasswordSettingsCommand) modelAndView.getModelMap()
                .get(Attributes.Model.Command.VALUE);
        assertNotNull(command);

        String newPass = "newPass";
        command.setPassword(newPass);
        command.setConfirmPassword(newPass);

        User user = new User("user", "pass", "email");
        Mockito.when(securityService.getUserByNameStrict(command.getUsername())).thenReturn(user);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> passCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(securityService).updatePassword(userCaptor.capture(), passCaptor.capture(),
                Mockito.anyBoolean());

        mockMvc.perform(MockMvcRequestBuilders.post("/passwordSettings.view").flashAttr(Attributes.Model.Command.VALUE,
                command)).andExpect(MockMvcResultMatchers.status().is3xxRedirection());

        assertEquals(user, userCaptor.getValue());
        assertEquals(newPass, passCaptor.getValue());
    }
}
