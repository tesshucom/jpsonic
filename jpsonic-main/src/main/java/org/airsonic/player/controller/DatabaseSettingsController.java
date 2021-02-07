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

package org.airsonic.player.controller;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.OutlineHelpSelector;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.command.DatabaseSettingsCommand;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.DataSourceConfigType;
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

@Controller
@RequestMapping("/databaseSettings")
public class DatabaseSettingsController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final OutlineHelpSelector outlineHelpSelector;

    public DatabaseSettingsController(SettingsService settingsService, SecurityService securityService,
            OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

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
    protected ModelAndView onSubmit(
            @ModelAttribute(Attributes.Model.Command.VALUE) @Validated DatabaseSettingsCommand command,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView(ViewName.DATABASE_SETTINGS.value());
        } else {
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
        }
    }

}
