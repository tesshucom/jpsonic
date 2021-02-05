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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.validator;

import org.airsonic.player.command.PasswordSettingsCommand;
import org.airsonic.player.controller.PasswordSettingsController;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator for {@link PasswordSettingsController}.
 *
 * @author Sindre Mehus
 */
@Component
public class PasswordSettingsValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(PasswordSettingsCommand.class);
    }

    @Override
    public void validate(Object obj, Errors errors) {
        PasswordSettingsCommand command = (PasswordSettingsCommand) obj;

        if (command.getPassword() == null || command.getPassword().isEmpty()) {
            errors.rejectValue("password", "usersettings.nopassword");
        } else if (!command.getPassword().equals(command.getConfirmPassword())) {
            errors.rejectValue("password", "usersettings.wrongpassword");
        }
    }
}
