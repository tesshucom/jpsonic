/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.SonosService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for the page used to administrate the Sonos music service settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/sonosSettings")
public class SonosSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SonosService sonosService;

    @GetMapping
    public String doGet(Model model) {
        model.addAttribute("model", LegacyMap.of(
                "sonosEnabled", settingsService.isSonosEnabled(),
                "sonosServiceName", settingsService.getSonosServiceName(),
                "useRadio", settingsService.isUseRadio(),
                "useSonos", settingsService.isUseSonos()));
        return "sonosSettings";
    }

    @PostMapping
    public ModelAndView doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        handleParameters(request);
        redirectAttributes.addFlashAttribute("settings_toast", true);
        return new ModelAndView(new RedirectView(ViewName.SONOS_SETTINGS.value()));
    }

    private void handleParameters(HttpServletRequest request) {
        boolean sonosEnabled = ServletRequestUtils.getBooleanParameter(request, "sonosEnabled", false);
        String sonosServiceName = StringUtils.trimToNull(request.getParameter("sonosServiceName"));
        if (sonosServiceName == null) {
            sonosServiceName = "Jpsonic";
        }

        settingsService.setSonosEnabled(sonosEnabled);
        settingsService.setSonosServiceName(sonosServiceName);
        settingsService.save();

        sonosService.setMusicServiceEnabled(false, NetworkService.getBaseUrl(request));
        sonosService.setMusicServiceEnabled(sonosEnabled, NetworkService.getBaseUrl(request));
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSonosService(SonosService sonosService) {
        this.sonosService = sonosService;
    }
}
