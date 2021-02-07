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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.controller;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.service.NetworkUtils;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.SonosService;
import org.airsonic.player.util.LegacyMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate the Sonos music service settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/sonosSettings")
public class SonosSettingsController {

    private final SettingsService settingsService;
    private final SonosService sonosService;

    public SonosSettingsController(SettingsService settingsService, SonosService sonosService) {
        super();
        this.settingsService = settingsService;
        this.sonosService = sonosService;
    }

    @GetMapping
    public String doGet(Model model) {
        model.addAttribute("model",
                LegacyMap.of("sonosEnabled", settingsService.isSonosEnabled(), "sonosServiceName",
                        settingsService.getSonosServiceName(), "useRadio", settingsService.isUseRadio(), "useSonos",
                        settingsService.isUseSonos()));
        return "sonosSettings";
    }

    @PostMapping
    public ModelAndView doPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        handleParameters(request);
        redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        return new ModelAndView(new RedirectView(ViewName.SONOS_SETTINGS.value()));
    }

    private void handleParameters(HttpServletRequest request) {
        boolean sonosEnabled = ServletRequestUtils.getBooleanParameter(request,
                Attributes.Request.SONOS_ENABLED.value(), false);
        String sonosServiceName = StringUtils
                .trimToNull(request.getParameter(Attributes.Request.SONOS_SERVICE_NAME.value()));
        if (sonosServiceName == null) {
            sonosServiceName = "Jpsonic";
        }

        settingsService.setSonosEnabled(sonosEnabled);
        settingsService.setSonosServiceName(sonosServiceName);
        settingsService.save();

        sonosService.setMusicServiceEnabled(false, NetworkUtils.getBaseUrl(request));
        sonosService.setMusicServiceEnabled(sonosEnabled, NetworkUtils.getBaseUrl(request));
    }
}
