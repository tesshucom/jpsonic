/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.OutlineHelpSelector;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.command.DatabaseSettingsCommand;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.DataSourceConfigType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/databaseSettings")
public class DatabaseSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private OutlineHelpSelector outlineHelpSelector;

    @GetMapping
    protected String displayForm() {
        return "databaseSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        DatabaseSettingsCommand command = new DatabaseSettingsCommand();
        command.setConfigType(settingsService.getDatabaseConfigType());
        command.setEmbedDriver(settingsService.getDatabaseConfigEmbedDriver());
        command.setEmbedPassword(settingsService.getDatabaseConfigEmbedPassword());
        command.setEmbedUrl(settingsService.getDatabaseConfigEmbedUrl());
        command.setEmbedUsername(settingsService.getDatabaseConfigEmbedUsername());
        command.setJNDIName(settingsService.getDatabaseConfigJNDIName());
        command.setMysqlVarcharMaxlength(settingsService.getDatabaseMysqlVarcharMaxlength());
        command.setUsertableQuote(settingsService.getDatabaseUsertableQuote());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        User user = securityService.getCurrentUser(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @PostMapping
    protected ModelAndView onSubmit(@ModelAttribute(Attributes.Model.Command.VALUE) @Validated DatabaseSettingsCommand command,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            settingsService.resetDatabaseToDefault();
            settingsService.setDatabaseConfigType(command.getConfigType());
            switch (command.getConfigType()) {
                case EMBED:
                    settingsService.setDatabaseConfigEmbedDriver(command.getEmbedDriver());
                    settingsService.setDatabaseConfigEmbedPassword(command.getEmbedPassword());
                    settingsService.setDatabaseConfigEmbedUrl(command.getEmbedUrl());
                    settingsService.setDatabaseConfigEmbedUsername(command.getEmbedUsername());
                    break;
                case JNDI:
                    settingsService.setDatabaseConfigJNDIName(command.getJNDIName());
                    break;
                case LEGACY:
                default:
                    break;
            }
            if (command.getConfigType() != DataSourceConfigType.LEGACY) {
                settingsService.setDatabaseMysqlVarcharMaxlength(command.getMysqlVarcharMaxlength());
                settingsService.setDatabaseUsertableQuote(command.getUsertableQuote());
            }
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
            settingsService.save();
            return new ModelAndView(new RedirectView(ViewName.DATABASE_SETTINGS.value()));
        } else {
            return new ModelAndView(ViewName.DATABASE_SETTINGS.value());
        }
    }

}
