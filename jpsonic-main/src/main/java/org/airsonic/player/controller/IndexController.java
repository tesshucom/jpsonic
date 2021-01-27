
package org.airsonic.player.controller;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

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
