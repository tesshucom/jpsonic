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

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    private final SecurityService securityService;

    public IndexController(SecurityService securityService) {
        super();
        this.securityService = securityService;
    }

    @RequestMapping(value = "/index", method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView index(HttpServletRequest request, @RequestParam("mainView") Optional<String> mainView) {
        UserSettings userSettings = securityService.getUserSettings(securityService.getCurrentUsername(request));
        Map<String, Object> model = LegacyMap.of("keyboardShortcutsEnabled", userSettings.isKeyboardShortcutsEnabled(),
                "showLeft", userSettings.isCloseDrawer(), "brand", SettingsService.getBrand());
        mainView.ifPresent(v -> model.put("mainView", v));
        return new ModelAndView("index", "model", model);
    }
}
