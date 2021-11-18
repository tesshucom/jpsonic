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
import com.tesshu.jpsonic.domain.IndexScheme;
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
    protected String get(HttpServletRequest request, Model model) {
        AdvancedSettingsCommand command = new AdvancedSettingsCommand();

        // Logging control
        command.setVerboseLogStart(settingsService.isVerboseLogStart());
        command.setVerboseLogScanning(settingsService.isVerboseLogScanning());
        command.setVerboseLogPlaying(settingsService.isVerboseLogPlaying());
        command.setVerboseLogShutdown(settingsService.isVerboseLogShutdown());

        // Bandwidth control
        command.setDownloadLimit(String.valueOf(settingsService.getDownloadBitrateLimit()));
        command.setUploadLimit(String.valueOf(settingsService.getUploadBitrateLimit()));
        command.setBufferSize(String.valueOf(settingsService.getBufferSize()));

        // Email notification
        command.setSmtpFrom(settingsService.getSmtpFrom());
        command.setSmtpServer(settingsService.getSmtpServer());
        command.setSmtpPort(settingsService.getSmtpPort());
        command.setSmtpEncryption(settingsService.getSmtpEncryption());
        command.setSmtpUser(settingsService.getSmtpUser());

        // LDAP authentication
        command.setLdapEnabled(settingsService.isLdapEnabled());
        command.setLdapUrl(settingsService.getLdapUrl());
        command.setLdapSearchFilter(settingsService.getLdapSearchFilter());
        command.setLdapManagerDn(settingsService.getLdapManagerDn());
        command.setLdapAutoShadowing(settingsService.isLdapAutoShadowing());
        command.setBrand(SettingsService.getBrand());

        // Account recovery assistant
        command.setCaptchaEnabled(settingsService.isCaptchaEnabled());
        command.setRecaptchaSiteKey(settingsService.getRecaptchaSiteKey());

        // Danger Zone
        command.setIndexScheme(IndexScheme.valueOf(settingsService.getIndexSchemeName()));
        command.setReadGreekInJapanese(settingsService.isReadGreekInJapanese());
        command.setForceInternalValueInsteadOfTags(settingsService.isForceInternalValueInsteadOfTags());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        command.setShareCount(shareService.getAllShares().size());
        User user = securityService.getCurrentUser(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "advancedSettings";
    }

    @PostMapping
    protected ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) AdvancedSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        // Logging control
        settingsService.setVerboseLogStart(command.isVerboseLogStart());
        settingsService.setVerboseLogScanning(command.isVerboseLogScanning());
        settingsService.setVerboseLogPlaying(command.isVerboseLogPlaying());
        settingsService.setVerboseLogShutdown(command.isVerboseLogShutdown());

        // Bandwidth control
        try {
            settingsService.setDownloadBitrateLimit(Long.parseLong(command.getDownloadLimit()));
            settingsService.setUploadBitrateLimit(Long.parseLong(command.getUploadLimit()));
            settingsService.setBufferSize(Integer.parseInt(command.getBufferSize()));
        } catch (NumberFormatException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error in parse of bitrateLimit or bufferSize.", e);
            }
        }

        // Email notification
        settingsService.setSmtpFrom(command.getSmtpFrom());
        settingsService.setSmtpServer(command.getSmtpServer());
        settingsService.setSmtpPort(command.getSmtpPort());
        settingsService.setSmtpEncryption(command.getSmtpEncryption());
        settingsService.setSmtpUser(command.getSmtpUser());
        if (StringUtils.isNotEmpty(command.getSmtpPassword())) {
            settingsService.setSmtpPassword(command.getSmtpPassword());
        }

        // LDAP authentication
        settingsService.setLdapEnabled(command.isLdapEnabled());
        settingsService.setLdapUrl(command.getLdapUrl());
        settingsService.setLdapSearchFilter(command.getLdapSearchFilter());
        settingsService.setLdapManagerDn(command.getLdapManagerDn());
        if (StringUtils.isNotEmpty(command.getLdapManagerPassword())) {
            settingsService.setLdapManagerPassword(command.getLdapManagerPassword());
        }
        settingsService.setLdapAutoShadowing(command.isLdapAutoShadowing());

        // Account recovery assistant
        settingsService.setCaptchaEnabled(command.isCaptchaEnabled());
        settingsService.setRecaptchaSiteKey(command.getRecaptchaSiteKey());
        if (StringUtils.isNotEmpty(command.getRecaptchaSecretKey())) {
            settingsService.setRecaptchaSecretKey(command.getRecaptchaSecretKey());
        }

        // Danger Zone
        setDangerZone(command);

        settingsService.save();

        // for view page control
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), false);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.ADVANCED_SETTINGS.value()));
    }

    private void setDangerZone(AdvancedSettingsCommand command) {
        IndexScheme scheme = command.getIndexScheme();
        if (!settingsService.isIgnoreFileTimestampsNext()) {
            IndexScheme old = IndexScheme.valueOf(settingsService.getIndexSchemeName());
            if (old != scheme) {
                settingsService.setIgnoreFileTimestampsNext(true);
            }
        }
        settingsService.setIndexSchemeName(scheme.name());
        if (scheme == IndexScheme.NATIVE_JAPANESE) {
            if (settingsService.isReadGreekInJapanese() != command.isReadGreekInJapanese()) {
                settingsService.setIgnoreFileTimestampsNext(true);
            }
            settingsService.setReadGreekInJapanese(command.isReadGreekInJapanese());
            settingsService.setForceInternalValueInsteadOfTags(false);
        } else if (scheme == IndexScheme.ROMANIZED_JAPANESE) {
            settingsService.setReadGreekInJapanese(false);
            settingsService.setForceInternalValueInsteadOfTags(command.isForceInternalValueInsteadOfTags());
        } else {
            settingsService.setReadGreekInJapanese(false);
            settingsService.setForceInternalValueInsteadOfTags(false);
        }
    }

}
