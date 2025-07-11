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

import com.tesshu.jpsonic.command.AdvancedSettingsCommand;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
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
    private final ScannerStateService scannerStateService;

    public AdvancedSettingsController(SettingsService settingsService,
            SecurityService securityService, ShareService shareService,
            OutlineHelpSelector outlineHelpSelector, ScannerStateService scannerStateService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.outlineHelpSelector = outlineHelpSelector;
        this.scannerStateService = scannerStateService;
    }

    @GetMapping
    protected String get(HttpServletRequest request, Model model) {
        AdvancedSettingsCommand command = new AdvancedSettingsCommand();

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

        // Scan log
        command.setUseScanLog(settingsService.isUseScanLog());
        command.setScanLogRetention(settingsService.getScanLogRetention());
        command.setUseScanEvents(settingsService.isUseScanEvents());
        command.setMeasureMemory(settingsService.isMeasureMemory());

        // Danger Zone
        command.setIndexScheme(IndexScheme.valueOf(settingsService.getIndexSchemeName()));
        command
            .setForceInternalValueInsteadOfTags(
                    settingsService.isForceInternalValueInsteadOfTags());
        command.setSortAlphanum(settingsService.isSortAlphanum());
        command.setSortStrict(settingsService.isSortStrict());
        command.setDefaultSortAlphanum(SettingsService.isDefaultSortAlphanum());
        command.setDefaultSortStrict(SettingsService.isDefaultSortStrict());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        command.setShareCount(shareService.getAllShares().size());
        User user = securityService.getCurrentUserStrict(request);
        command
            .setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setScanning(scannerStateService.isScanning());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
        return "advancedSettings";
    }

    @PostMapping
    protected ModelAndView post(
            @ModelAttribute(Attributes.Model.Command.VALUE) AdvancedSettingsCommand command,
            RedirectAttributes redirectAttributes) {

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

        // Scan log
        if (!scannerStateService.isScanning()) {
            setScanLog(command);
        }

        // Danger Zone
        if (!scannerStateService.isScanning()) {
            setDangerZone(command);
        }

        settingsService.save();

        // for view page control
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), false);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);

        return new ModelAndView(new RedirectView(ViewName.ADVANCED_SETTINGS.value()));
    }

    void setScanLog(AdvancedSettingsCommand command) {
        settingsService.setUseScanLog(command.isUseScanLog());
        if (command.isUseScanLog()) {
            settingsService.setScanLogRetention(command.getScanLogRetention());
            settingsService.setUseScanEvents(command.isUseScanEvents());
            settingsService.setMeasureMemory(command.isMeasureMemory());
        } else {
            settingsService.setScanLogRetention(settingsService.getDefaultScanLogRetention());
            settingsService.setUseScanEvents(false);
            settingsService.setMeasureMemory(false);
        }
    }

    private void setDangerZone(AdvancedSettingsCommand command) {

        IndexScheme scheme = command.getIndexScheme();
        settingsService.setIndexSchemeName(scheme.name());

        if (scheme == IndexScheme.NATIVE_JAPANESE) {
            settingsService.setForceInternalValueInsteadOfTags(false);
            settingsService.setDeleteDiacritic(true);
            settingsService.setIgnoreFullWidth(true);
        } else if (scheme == IndexScheme.ROMANIZED_JAPANESE) {
            settingsService
                .setForceInternalValueInsteadOfTags(command.isForceInternalValueInsteadOfTags());
            settingsService.setDeleteDiacritic(false);
            settingsService.setIgnoreFullWidth(true);
        } else {
            settingsService.setForceInternalValueInsteadOfTags(false);
            settingsService.setDeleteDiacritic(false);
            settingsService.setIgnoreFullWidth(false);
        }

        settingsService.setSortAlphanum(command.isSortAlphanum());
        settingsService.setSortStrict(command.isSortStrict());
    }
}
