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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.controller.validator;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.controller.form.UserSettingsCommand;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.MapBindingResult;

class UserSettingsValidatorTest {

    private static final String ADMIN_NAME = "admin";
    UserSettingsValidator validator;

    @BeforeEach
    void setup() throws ExecutionException {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().build();
        validator = new UserSettingsValidator(mock(SecurityService.class), settingsFacade,
                mock(MockHttpServletRequest.class));
    }

    @Test
    void validateCurrentUser() {
        UserSettingsCommand command = new UserSettingsCommand();
        command.setUsername(ADMIN_NAME);
        MapBindingResult errors = new MapBindingResult(new ConcurrentHashMap<>(), ADMIN_NAME);
        command.setDeleteUser(false);
        command.setAdminRole(false);
        validator.validateCurrentUser(command, errors);
        assertEquals(1, errors.getAllErrors().size());
        assertEquals(1, errors.getErrorCount());
        assertEquals("usersettings.cantremoverole", errors.getFieldError().getCode());
    }
}
