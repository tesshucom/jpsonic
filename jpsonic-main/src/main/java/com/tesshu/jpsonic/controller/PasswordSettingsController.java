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

package com.tesshu.jpsonic.controller;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.PasswordSettingsCommand;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.validator.PasswordSettingsValidator;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to change password.
 *
 * @author Sindre Mehus
 */
@org.springframework.stereotype.Controller
@RequestMapping({ "/passwordSettings", "/passwordSettings.view" })
public class PasswordSettingsController {

    private final SecurityService securityService;
    private final PasswordSettingsValidator passwordSettingsValidator;

    public PasswordSettingsController(SecurityService securityService,
            PasswordSettingsValidator passwordSettingsValidator) {
        super();
        this.securityService = securityService;
        this.passwordSettingsValidator = passwordSettingsValidator;
    }

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(passwordSettingsValidator);
    }

    @GetMapping
    protected ModelAndView get(HttpServletRequest request) {
        PasswordSettingsCommand command = new PasswordSettingsCommand();
        User user = securityService.getCurrentUser(request);
        command.setUsername(user.getUsername());
        command.setLdapAuthenticated(user.isLdapAuthenticated());
        return new ModelAndView("passwordSettings", Attributes.Model.Command.VALUE, command);
    }

    @PostMapping
    protected ModelAndView doSubmitAction(
            @ModelAttribute(Attributes.Model.Command.VALUE) @Validated PasswordSettingsCommand command,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("passwordSettings");
        } else {
            User user = securityService.getUserByName(command.getUsername());
            user.setPassword(command.getPassword());
            securityService.updateUser(user);

            command.setPassword(null);
            command.setConfirmPassword(null);
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
            return new ModelAndView(new RedirectView(ViewName.PASSWORD_SETTINGS.value()));
        }
    }

}
