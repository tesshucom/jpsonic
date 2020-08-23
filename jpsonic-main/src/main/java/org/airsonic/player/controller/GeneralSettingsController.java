/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.command.GeneralSettingsCommand;
import org.airsonic.player.domain.Theme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Optional;

/**
 * Controller for the page used to administrate general settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/generalSettings")
public class GeneralSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;

    @GetMapping
    protected String displayForm() {
        return "generalSettings";
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model, @RequestParam("toast") Optional<Boolean> toast) {
        GeneralSettingsCommand command = new GeneralSettingsCommand();
        command.setCoverArtFileTypes(settingsService.getCoverArtFileTypes());
        command.setIgnoredArticles(settingsService.getIgnoredArticles());
        command.setShortcuts(settingsService.getShortcuts());
        command.setIndex(settingsService.getIndexString());
        command.setPlaylistFolder(settingsService.getPlaylistFolder());
        command.setMusicFileTypes(settingsService.getMusicFileTypes());
        command.setVideoFileTypes(settingsService.getVideoFileTypes());
        command.setSortAlbumsByYear(settingsService.isSortAlbumsByYear());
        command.setSortGenresByAlphabet(settingsService.isSortGenresByAlphabet());
        command.setProhibitSortVarious(settingsService.isProhibitSortVarious());
        command.setSortAlphanum(settingsService.isSortAlphanum());
        command.setSortStrict(settingsService.isSortStrict());
        command.setSearchComposer(settingsService.isSearchComposer());
        command.setOutputSearchQuery(settingsService.isOutputSearchQuery());
        command.setSearchMethodLegacy(settingsService.isSearchMethodLegacy());
        command.setGettingStartedEnabled(settingsService.isGettingStartedEnabled());
        command.setWelcomeTitle(settingsService.getWelcomeTitle());
        command.setWelcomeSubtitle(settingsService.getWelcomeSubtitle());
        command.setWelcomeMessage(settingsService.getWelcomeMessage());
        command.setLoginMessage(settingsService.getLoginMessage());
        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());
        command.setPublishPodcast(settingsService.isPublishPodcast());
        command.setShowJavaJukebox(settingsService.isShowJavaJukebox());
        command.setShowServerLog(settingsService.isShowServerLog());
        command.setShowRememberMe(settingsService.isShowRememberMe());
        toast.ifPresent(b -> command.setShowToast(b));

        Theme[] themes = settingsService.getAvailableThemes();
        command.setThemes(themes);
        String currentThemeId = settingsService.getThemeId();
        for (int i = 0; i < themes.length; i++) {
            if (currentThemeId.equals(themes[i].getId())) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        Locale currentLocale = settingsService.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);

            if (currentLocale.equals(locales[i])) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());

        model.addAttribute("command",command);
    }

    @PostMapping
    protected String doSubmitAction(@ModelAttribute("command") GeneralSettingsCommand command, RedirectAttributes redirectAttributes) {

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        Theme theme = settingsService.getAvailableThemes()[themeIndex];

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = settingsService.getAvailableLocales()[localeIndex];

        boolean isReload = !settingsService.getIndexString().equals(command.getIndex())
                || !settingsService.getIgnoredArticles().equals(command.getIgnoredArticles())
                || !settingsService.getShortcuts().equals(command.getShortcuts())
                || !settingsService.getThemeId().equals(theme.getId())
                || !settingsService.getLocale().equals(locale);
        redirectAttributes.addFlashAttribute("settings_reload", isReload);
        if (!isReload) {
            redirectAttributes.addFlashAttribute("settings_toast", true);
        }
        settingsService.setIndexString(command.getIndex());
        settingsService.setIgnoredArticles(command.getIgnoredArticles());
        settingsService.setShortcuts(command.getShortcuts());
        settingsService.setPlaylistFolder(command.getPlaylistFolder());
        settingsService.setMusicFileTypes(command.getMusicFileTypes());
        settingsService.setVideoFileTypes(command.getVideoFileTypes());
        settingsService.setCoverArtFileTypes(command.getCoverArtFileTypes());
        settingsService.setSortAlbumsByYear(command.isSortAlbumsByYear());
        settingsService.setSortGenresByAlphabet(command.isSortGenresByAlphabet());
        settingsService.setProhibitSortVarious(command.isProhibitSortVarious());
        settingsService.setSortAlphanum(command.isSortAlphanum());
        settingsService.setSortStrict(command.isSortStrict());
        settingsService.setSearchComposer(command.isSearchComposer());
        settingsService.setOutputSearchQuery(command.isOutputSearchQuery());
        settingsService.setSearchMethodChanged(settingsService.isSearchMethodLegacy() != command.isSearchMethodLegacy());
        settingsService.setSearchMethodLegacy(command.isSearchMethodLegacy());
        settingsService.setGettingStartedEnabled(command.isGettingStartedEnabled());
        settingsService.setWelcomeTitle(command.getWelcomeTitle());
        settingsService.setWelcomeSubtitle(command.getWelcomeSubtitle());
        settingsService.setWelcomeMessage(command.getWelcomeMessage());
        settingsService.setLoginMessage(command.getLoginMessage());
        settingsService.setUseRadio(command.isUseRadio());
        settingsService.setUseSonos(command.isUseSonos());
        settingsService.setPublishPodcast(command.isPublishPodcast());
        settingsService.setThemeId(theme.getId());
        settingsService.setLocale(locale);
        settingsService.setShowJavaJukebox(command.isShowJavaJukebox());
        settingsService.setShowServerLog(command.isShowServerLog());
        settingsService.setShowRememberMe(command.isShowRememberMe());
        settingsService.save();

        return "redirect:generalSettings.view";
    }

}
