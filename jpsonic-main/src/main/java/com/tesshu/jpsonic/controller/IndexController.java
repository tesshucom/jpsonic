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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/index")
public class IndexController {

    private final SecurityService securityService;
    private final SettingsService settingsService;

    public IndexController(SecurityService securityService, SettingsService settingsService) {
        super();
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    private Map<String, Object> createDefaultModel(HttpServletRequest request) {
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        return LegacyMap.of("keyboardShortcutsEnabled", userSettings.isKeyboardShortcutsEnabled(), "showLeft",
                userSettings.isCloseDrawer(), "brand", settingsService.getBrand());
    }

    @GetMapping
    public ModelAndView index(HttpServletRequest request) {
        return new ModelAndView("index", "model", createDefaultModel(request));
    }

    @PostMapping
    public ModelAndView index(HttpServletRequest request, @RequestParam("mainView") Optional<String> mainView) {
        Map<String, Object> model = createDefaultModel(request);
        mainView.ifPresent(v -> model.put("mainView", v));
        return new ModelAndView("index", "model", model);
    }
}
