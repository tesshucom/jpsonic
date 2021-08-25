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

import com.tesshu.jpsonic.command.AdvancedSettingsCommand;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate advanced settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/advancedSettings", "/advancedSettings.view" })
public class AdvancedSettingsController {

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedSettingsController.class);

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ShareService shareService;
    private final OutlineHelpSelector outlineHelpSelector;

    public AdvancedSettingsController(SettingsService settingsService, SecurityService securityService,
            ShareService shareService, OutlineHelpSelector outlineHelpSelector) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
    }

    @GetMapping
    protected String formBackingObject(HttpServletRequest request, Model model) {
        AdvancedSettingsCommand command = new AdvancedSettingsCommand();

        command.setVerboseLogStart(settingsService.isVerboseLogStart());
        command.setVerboseLogScanning(settingsService.isVerboseLogScanning());
        command.setVerboseLogPlaying(settingsService.isVerboseLogPlaying());
        command.setVerboseLogShutdown(settingsService.isVerboseLogShutdown());

        command.setDownloadLimit(String.valueOf(settingsService.getDownloadBitrateLimit()));
        command.setUploadLimit(String.valueOf(settingsService.getUploadBitrateLimit()));
        command.setBufferSize(String.valueOf(settingsService.getBufferSize()));

        command.setLdapEnabled(settingsService.isLdapEnabled());
        command.setLdapUrl(settingsService.getLdapUrl());
        command.setLdapSearchFilter(settingsService.getLdapSearchFilter());
        command.setLdapManagerDn(settingsService.getLdapManagerDn());
        command.setLdapAutoShadowing(settingsService.isLdapAutoShadowing());
        command.setBrand(SettingsService.getBrand());

        command.setSmtpServer(settingsService.getSmtpServer());
        command.setSmtpEncryption(settingsService.getSmtpEncryption());
        command.setSmtpPort(settingsService.getSmtpPort());
        command.setSmtpUser(settingsService.getSmtpUser());
        command.setSmtpFrom(settingsService.getSmtpFrom());

        command.setCaptchaEnabled(settingsService.isCaptchaEnabled());
        command.setRecaptchaSiteKey(settingsService.getRecaptchaSiteKey());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        command.setShareCount(shareService.getAllShares().size());

        User user = securityService.getCurrentUser(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "advancedSettings";
    }

    @PostMapping
    protected ModelAndView doSubmitAction(
            @ModelAttribute(Attributes.Model.Command.VALUE) AdvancedSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), false);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

        settingsService.setVerboseLogStart(command.isVerboseLogStart());
        settingsService.setVerboseLogScanning(command.isVerboseLogScanning());
        settingsService.setVerboseLogPlaying(command.isVerboseLogPlaying());
        settingsService.setVerboseLogShutdown(command.isVerboseLogShutdown());

        try {
            settingsService.setDownloadBitrateLimit(Long.parseLong(command.getDownloadLimit()));
            settingsService.setUploadBitrateLimit(Long.parseLong(command.getUploadLimit()));
            settingsService.setBufferSize(Integer.parseInt(command.getBufferSize()));
        } catch (NumberFormatException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error in parse of bitrateLimit or bufferSize.", e);
            }
        }
        settingsService.setLdapEnabled(command.isLdapEnabled());
        settingsService.setLdapUrl(command.getLdapUrl());
        settingsService.setLdapSearchFilter(command.getLdapSearchFilter());
        settingsService.setLdapManagerDn(command.getLdapManagerDn());
        settingsService.setLdapAutoShadowing(command.isLdapAutoShadowing());

        if (StringUtils.isNotEmpty(command.getLdapManagerPassword())) {
            settingsService.setLdapManagerPassword(command.getLdapManagerPassword());
        }

        settingsService.setSmtpServer(command.getSmtpServer());
        settingsService.setSmtpEncryption(command.getSmtpEncryption());
        settingsService.setSmtpPort(command.getSmtpPort());
        settingsService.setSmtpUser(command.getSmtpUser());
        settingsService.setSmtpFrom(command.getSmtpFrom());

        if (StringUtils.isNotEmpty(command.getSmtpPassword())) {
            settingsService.setSmtpPassword(command.getSmtpPassword());
        }

        settingsService.setCaptchaEnabled(command.isCaptchaEnabled());
        settingsService.setRecaptchaSiteKey(command.getRecaptchaSiteKey());
        if (StringUtils.isNotEmpty(command.getRecaptchaSecretKey())) {
            settingsService.setRecaptchaSecretKey(command.getRecaptchaSecretKey());
        }

        settingsService.save();

        return new ModelAndView(new RedirectView(ViewName.ADVANCED_SETTINGS.value()));
    }

}
