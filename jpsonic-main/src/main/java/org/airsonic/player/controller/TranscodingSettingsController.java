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
import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the set of transcoding configurations.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/transcodingSettings")
public class TranscodingSettingsController {

    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private OutlineHelpSelector outlineHelpSelector;

    @GetMapping
    public String doGet(HttpServletRequest request, Model model) {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        model.addAttribute("model",
                LegacyMap.of("transcodings", transcodingService.getAllTranscodings(), "transcodeDirectory",
                        transcodingService.getTranscodeDirectory(), "hlsCommand", settingsService.getHlsCommand(),
                        "brand", settingsService.getBrand(), "isOpenDetailSetting", userSettings.isOpenDetailSetting(),
                        "useRadio", settingsService.isUseRadio(), "useSonos", settingsService.isUseSonos(),
                        "showOutlineHelp", outlineHelpSelector.isShowOutlineHelp(request, user.getUsername())));
        return "transcodingSettings";
    }

    @PostMapping
    public ModelAndView doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String error = handleParameters(request, redirectAttributes);
        if (error == null) {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        } else {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.ERROR.value(), error);
        }
        return new ModelAndView(new RedirectView(ViewName.TRANSCODING_SETTINGS.value()));
    }

    private String handleParameters(HttpServletRequest request, RedirectAttributes redirectAttributes) {

        for (Transcoding transcoding : transcodingService.getAllTranscodings()) {
            Integer id = transcoding.getId();
            String name = getParameter(request, Attributes.Request.NAME.value(), id);
            String sourceFormats = getParameter(request, Attributes.Request.SOURCE_FORMATS.value(), id);
            String targetFormat = getParameter(request, Attributes.Request.TARGET_FORMAT.value(), id);
            String step1 = getParameter(request, Attributes.Request.STEP1.value(), id);
            String step2 = getParameter(request, Attributes.Request.STEP2.value(), id);
            boolean delete = getParameter(request, Attributes.Request.DELETE.value(), id) != null;

            if (delete) {
                transcodingService.deleteTranscoding(id);
            } else if (name == null) {
                return "transcodingsettings.noname";
            } else if (sourceFormats == null) {
                return "transcodingsettings.nosourceformat";
            } else if (targetFormat == null) {
                return "transcodingsettings.notargetformat";
            } else if (step1 == null) {
                return "transcodingsettings.nostep1";
            } else {
                transcoding.setName(name);
                transcoding.setSourceFormats(sourceFormats);
                transcoding.setTargetFormat(targetFormat);
                transcoding.setStep1(step1);
                transcoding.setStep2(step2);
                transcodingService.updateTranscoding(transcoding);
            }
        }

        String name = StringUtils.trimToNull(request.getParameter(Attributes.Request.NAME.value()));
        String sourceFormats = StringUtils.trimToNull(request.getParameter(Attributes.Request.SOURCE_FORMATS.value()));
        String targetFormat = StringUtils.trimToNull(request.getParameter(Attributes.Request.TARGET_FORMAT.value()));
        String step1 = StringUtils.trimToNull(request.getParameter(Attributes.Request.STEP1.value()));
        String step2 = StringUtils.trimToNull(request.getParameter(Attributes.Request.STEP2.value()));
        boolean defaultActive = request.getParameter(Attributes.Request.DEFAULT_ACTIVE.value()) != null;

        if (name != null || sourceFormats != null || targetFormat != null || step1 != null || step2 != null) {
            Transcoding transcoding = new Transcoding(null, name, sourceFormats, targetFormat, step1, step2, null,
                    defaultActive);
            String error = null;
            if (name == null) {
                error = "transcodingsettings.noname";
            } else if (sourceFormats == null) {
                error = "transcodingsettings.nosourceformat";
            } else if (targetFormat == null) {
                error = "transcodingsettings.notargetformat";
            } else if (step1 == null) {
                error = "transcodingsettings.nostep1";
            } else {
                transcodingService.createTranscoding(transcoding);
            }
            if (error != null) {
                redirectAttributes.addAttribute(Attributes.Redirect.NEW_TRANSCODING.value(), transcoding);
                return error;
            }
        }
        settingsService.setHlsCommand(StringUtils.trim(request.getParameter(Attributes.Request.HLS_COMMAND.value())));
        settingsService.save();
        return null;
    }

    private String getParameter(HttpServletRequest request, String name, Integer id) {
        return StringUtils.trimToNull(request.getParameter(name + "[" + id + "]"));
    }
}
