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

package com.tesshu.jpsonic.validator;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.UserSettingsCommand;
import com.tesshu.jpsonic.controller.UserSettingsController;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validator for {@link UserSettingsController}.
 *
 * @author Sindre Mehus
 */
public class UserSettingsValidator implements Validator {

    private static final String REJECTED_FIELD_USERNAME = "username";
    private static final String REJECTED_FIELD_EMAIL = "email";
    private static final String REJECTED_FIELD_PASSWORD = "password";
    private static final String REJECTED_FIELD_DELETEUSER = "deleteUser";
    private static final String REJECTED_FIELD_ADMINROLE = "adminRole";

    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final HttpServletRequest request;

    public UserSettingsValidator(SecurityService securityService, SettingsService settingsService,
            HttpServletRequest request) {
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.request = request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(UserSettingsCommand.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.ConfusingTernary") // false positive. Do not change the order.
    public void validate(Object obj, Errors errors) {
        UserSettingsCommand command = (UserSettingsCommand) obj;
        validateNewUser(command, errors);
        validatePassword(command, errors);
        validateLdap(command, errors);
        validateCurrentUser(command, errors);
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive. Do not change the order.
    private void validateNewUser(UserSettingsCommand command, Errors errors) {
        if (command.isNewUser()) {
            String username = command.getUsername();
            String email = StringUtils.trimToNull(command.getEmail());
            String password = StringUtils.trimToNull(command.getPassword());
            if (username == null || username.isEmpty()) {
                errors.rejectValue(REJECTED_FIELD_USERNAME, "usersettings.nousername");
            } else if (securityService.getUserByName(username) != null) {
                errors.rejectValue(REJECTED_FIELD_USERNAME, "usersettings.useralreadyexists");
            } else if (email == null) {
                errors.rejectValue(REJECTED_FIELD_EMAIL, "usersettings.noemail");
            } else if (command.isLdapAuthenticated() && !settingsService.isLdapEnabled()) {
                errors.rejectValue(REJECTED_FIELD_PASSWORD, "usersettings.ldapdisabled");
            } else if (command.isLdapAuthenticated() && password != null) {
                errors.rejectValue(REJECTED_FIELD_PASSWORD, "usersettings.passwordnotsupportedforldap");
            }
        }
    }

    private void validatePassword(UserSettingsCommand command, Errors errors) {
        if ((command.isNewUser() || command.isPasswordChange()) && !command.isLdapAuthenticated()) {
            String password = StringUtils.trimToNull(command.getPassword());
            String confirmPassword = command.getConfirmPassword();
            if (password == null) {
                errors.rejectValue(REJECTED_FIELD_PASSWORD, "usersettings.nopassword");
            } else if (!password.equals(confirmPassword)) {
                errors.rejectValue(REJECTED_FIELD_PASSWORD, "usersettings.wrongpassword");
            }
        }
    }

    private void validateLdap(UserSettingsCommand command, Errors errors) {
        if (command.isPasswordChange() && command.isLdapAuthenticated()) {
            errors.rejectValue(REJECTED_FIELD_PASSWORD, "usersettings.passwordnotsupportedforldap");
        }
    }

    private void validateCurrentUser(UserSettingsCommand command, Errors errors) {
        String username = command.getUsername();
        if (securityService.getCurrentUser(request).getUsername().equals(username)) {
            if (command.isDeleteUser()) {
                errors.rejectValue(REJECTED_FIELD_DELETEUSER, "usersettings.cantdeleteuser");
            }
            if (!command.isAdminRole()) {
                errors.rejectValue(REJECTED_FIELD_ADMINROLE, "usersettings.cantremoverole");
            }
        }
    }

}
