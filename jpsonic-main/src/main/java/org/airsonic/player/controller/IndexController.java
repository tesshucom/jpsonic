package org.airsonic.player.controller;

import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/index")
public class IndexController {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @GetMapping
    public ModelAndView index(HttpServletRequest request) {
        UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));

        return new ModelAndView("index", "model", LegacyMap.of(
                "showRight", userSettings.isShowNowPlayingEnabled(),
                "autoHidePlayQueue", userSettings.isAutoHidePlayQueue(),
                "keyboardShortcutsEnabled", userSettings.isKeyboardShortcutsEnabled(),
                "showLeft", userSettings.isCloseDrawer(),
                "brand", settingsService.getBrand()));
    }
}
