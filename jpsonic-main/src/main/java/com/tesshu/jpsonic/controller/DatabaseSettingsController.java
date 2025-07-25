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

import com.tesshu.jpsonic.command.DatabaseSettingsCommand;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.spring.DataSourceConfigType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping({ "/databaseSettings", "/databaseSettings.view" })
public class DatabaseSettingsController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ShareService shareService;
    private final OutlineHelpSelector outlineHelpSelector;

    public DatabaseSettingsController(SettingsService settingsService,
            SecurityService securityService, ShareService shareService,
            OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

    @GetMapping
    protected String get() {
        return "databaseSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model) {
        DatabaseSettingsCommand command = new DatabaseSettingsCommand();
        command.setConfigType(settingsService.getDatabaseConfigType());
        command.setEmbedDriver(settingsService.getDatabaseConfigEmbedDriver());
        command.setEmbedUrl(settingsService.getDatabaseConfigEmbedUrl());
        command.setEmbedUsername(settingsService.getDatabaseConfigEmbedUsername());
        command.setEmbedPassword(settingsService.getDatabaseConfigEmbedPassword());
        command.setJNDIName(settingsService.getDatabaseConfigJNDIName());
        command.setMysqlVarcharMaxlength(settingsService.getDatabaseMysqlVarcharMaxlength());
        command.setUsertableQuote(settingsService.getDatabaseUsertableQuote());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        command.setShareCount(shareService.getAllShares().size());
        User user = securityService.getCurrentUserStrict(request);
        command
            .setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @PostMapping
    protected RedirectView onSubmit(
            @ModelAttribute(Attributes.Model.Command.VALUE) @Validated DatabaseSettingsCommand command,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            settingsService.resetDatabaseToDefault();
            settingsService.setDatabaseConfigType(command.getConfigType());

            if (command.getConfigType() == DataSourceConfigType.URL) {
                settingsService.setDatabaseConfigEmbedDriver(command.getEmbedDriver());
                settingsService.setDatabaseConfigEmbedUrl(command.getEmbedUrl());
                settingsService.setDatabaseConfigEmbedPassword(command.getEmbedPassword());
                settingsService.setDatabaseConfigEmbedUsername(command.getEmbedUsername());
            } else if (command.getConfigType() == DataSourceConfigType.JNDI) {
                settingsService.setDatabaseConfigJNDIName(command.getJNDIName());
            }

            if (command.getConfigType() != DataSourceConfigType.HOST) {
                settingsService
                    .setDatabaseMysqlVarcharMaxlength(command.getMysqlVarcharMaxlength());
                settingsService.setDatabaseUsertableQuote(command.getUsertableQuote());
            }
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
            settingsService.save();
        }
        return new RedirectView(ViewName.DATABASE_SETTINGS.value());
    }
}
