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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.command.GeneralSettingsCommand;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.Theme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.ScannerStateService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.util.PathValidator;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to administrate general settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/generalSettings", "/generalSettings.view" })
public class GeneralSettingsController {

    /*
     * It's EN and JP(consonant)
     */
    private static final String SIMPLE_INDEX_STRING = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) " // En
            + "\u3042(\u30A2\u30A4\u30A6\u30A8\u30AA) " // Jp(a)
            + "\u304B(\u30AB\u30AD\u30AF\u30B1\u30B3) " // Jp(ka)
            + "\u3055(\u30B5\u30B7\u30B9\u30BB\u30BD) " // Jp(sa)
            + "\u305F(\u30BF\u30C1\u30C4\u30C6\u30C8) " // Jp(ta)
            + "\u306A(\u30CA\u30CB\u30CC\u30CD\u30CE) " // Jp(na)
            + "\u306F(\u30CF\u30D2\u30D5\u30D8\u30DB) " // Jp(ha)
            + "\u307E(\u30DE\u30DF\u30E0\u30E1\u30E2) " // Jp(ma)
            + "\u3084(\u30E4\u30E6\u30E8) " // Jp(ya)
            + "\u3089(\u30E9\u30EA\u30EB\u30EC\u30ED) " // Jp(ra)
            + "\u308F(\u30EF\u30F2\u30F3)"; // Jp(wa)

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final ShareService shareService;
    private final PlayerService playerService;
    private final OutlineHelpSelector outlineHelpSelector;
    private final ScannerStateService scannerStateService;

    public GeneralSettingsController(SettingsService settingsService, SecurityService securityService,
            ShareService shareService, PlayerService playerService, OutlineHelpSelector outlineHelpSelector,
            ScannerStateService scannerStateService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.shareService = shareService;
        this.playerService = playerService;
        this.outlineHelpSelector = outlineHelpSelector;
        this.scannerStateService = scannerStateService;
    }

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model,
            @RequestParam(Attributes.Request.NameConstants.TOAST) Optional<Boolean> toast) {
        GeneralSettingsCommand command = new GeneralSettingsCommand();

        // Language and theme
        List<Theme> themes = SettingsService.getAvailableThemes();
        themes.stream().filter(theme -> theme.getId().equals(settingsService.getThemeId())).findFirst()
                .ifPresent(theme -> command.setThemeIndex(String.valueOf(themes.indexOf(theme))));
        command.setThemes(themes);

        List<Locale> locales = settingsService.getAvailableLocales();
        locales.stream().filter(locale -> locale.equals(settingsService.getLocale())).findFirst()
                .ifPresent(locale -> command.setLocaleIndex(String.valueOf(locales.indexOf(locale))));
        command.setLocales(locales.stream().map(locale -> locale.getDisplayName()).collect(Collectors.toList()));

        command.setIndexScheme(IndexScheme.of(settingsService.getIndexSchemeName()));

        // Index settings
        command.setDefaultIndexString(SettingsService.getDefaultIndexString());
        command.setSimpleIndexString(SIMPLE_INDEX_STRING);
        command.setIndex(settingsService.getIndexString());
        command.setIgnoredArticles(settingsService.getIgnoredArticles());
        command.setDeleteDiacritic(settingsService.isDeleteDiacritic());
        command.setIgnoreFullWidth(settingsService.isIgnoreFullWidth());

        // Sort settings
        command.setSortAlbumsByYear(settingsService.isSortAlbumsByYear());
        command.setSortGenresByAlphabet(settingsService.isSortGenresByAlphabet());
        command.setProhibitSortVarious(settingsService.isProhibitSortVarious());
        command.setDefaultSortAlbumsByYear(SettingsService.isDefaultSortAlbumsByYear());
        command.setDefaultSortGenresByAlphabet(SettingsService.isDefaultSortGenresByAlphabet());
        command.setDefaultProhibitSortVarious(SettingsService.isDefaultProhibitSortVarious());

        // Search settings
        command.setSearchComposer(settingsService.isSearchComposer());
        command.setOutputSearchQuery(settingsService.isOutputSearchQuery());

        // Suppressed legacy features
        command.setShowServerLog(settingsService.isShowServerLog());
        command.setShowStatus(settingsService.isShowStatus());
        command.setOthersPlayingEnabled(settingsService.isOthersPlayingEnabled());
        command.setShowRememberMe(settingsService.isShowRememberMe());
        command.setPublishPodcast(settingsService.isPublishPodcast());
        command.setUseExternalPlayer(settingsService.isUseExternalPlayer());
        command.setUseCopyOfAsciiUnprintable(settingsService.isUseCopyOfAsciiUnprintable());
        command.setUseJsonp(settingsService.isUseJsonp());
        command.setUseRemovingTrackFromId3Title(settingsService.isUseRemovingTrackFromId3Title());
        command.setUseCleanUp(settingsService.isUseCleanUp());
        command.setRedundantFolderCheck(settingsService.isRedundantFolderCheck());
        command.setShowIndexDetails(settingsService.isShowIndexDetails());
        command.setShowDBDetails(settingsService.isShowDBDetails());
        command.setUseCast(settingsService.isUseCast());

        // Extensions and shortcuts
        command.setMusicFileTypes(settingsService.getMusicFileTypes());
        command.setVideoFileTypes(settingsService.getVideoFileTypes());
        command.setCoverArtFileTypes(settingsService.getCoverArtFileTypes());
        command.setExcludedCoverArts(settingsService.getExcludedCoverArts());
        command.setPlaylistFolder(settingsService.getPlaylistFolder());
        command.setShortcuts(settingsService.getShortcuts());
        command.setDefaultMusicFileTypes(settingsService.getDefaultMusicFileTypes());
        command.setDefaultVideoFileTypes(settingsService.getDefaultVideoFileTypes());
        command.setDefaultCoverArtFileTypes(settingsService.getDefaultCoverArtFileTypes());
        command.setDefaultExcludedCoverArts(settingsService.getDefaultExcludedCoverArts());
        command.setDefaultPlaylistFolder(settingsService.getDefaultPlaylistFolder().replaceAll("\\\\", "\\\\\\\\"));
        command.setDefaultShortcuts(settingsService.getDefaultShortcuts());

        // Welcom message
        command.setGettingStartedEnabled(settingsService.isGettingStartedEnabled());
        command.setWelcomeTitle(settingsService.getWelcomeTitle());
        command.setWelcomeSubtitle(settingsService.getWelcomeSubtitle());
        command.setWelcomeMessage(settingsService.getWelcomeMessage());
        command.setLoginMessage(settingsService.getLoginMessage());

        // for view page control
        command.setUseRadio(settingsService.isUseRadio());
        User user = securityService.getCurrentUserStrict(request);
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        toast.ifPresent(command::setShowToast);
        command.setShareCount(shareService.getAllShares().size());
        UserSettings userSettings = securityService.getUserSettings(user.getUsername());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setScanning(scannerStateService.isScanning());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @GetMapping
    protected String get() {
        return "generalSettings";
    }

    @PostMapping
    protected ModelAndView post(@ModelAttribute(Attributes.Model.Command.VALUE) GeneralSettingsCommand command,
            RedirectAttributes redirectAttributes) {

        // Language and theme
        int themeIndex = Integer.parseInt(command.getThemeIndex());
        Theme theme = SettingsService.getAvailableThemes().get(themeIndex);
        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = settingsService.getAvailableLocales().get(localeIndex);

        /*
         * To transition the mainframe after reloading the entire web page, not a simple transition. (Compare before
         * reflecting settings)
         */
        boolean isReload = !settingsService.getIndexString().equals(command.getIndex())
                || !settingsService.getIgnoredArticles().equals(command.getIgnoredArticles())
                || !settingsService.getShortcuts().equals(command.getShortcuts())
                || !settingsService.getThemeId().equals(theme.getId()) || !settingsService.getLocale().equals(locale)
                || settingsService.isOthersPlayingEnabled() != command.isOthersPlayingEnabled();
        redirectAttributes.addFlashAttribute(Attributes.Redirect.RELOAD_FLAG.value(), isReload);

        settingsService.setThemeId(theme.getId());
        settingsService.setLocale(locale);

        // Index settings
        settingsService.setIndexString(command.getIndex());
        settingsService.setIgnoredArticles(command.getIgnoredArticles());

        if (command.getIndexScheme() == IndexScheme.NATIVE_JAPANESE) {
            settingsService.setDeleteDiacritic(true);
            settingsService.setIgnoreFullWidth(true);
        } else if (command.getIndexScheme() == IndexScheme.ROMANIZED_JAPANESE) {
            settingsService.setDeleteDiacritic(command.isDeleteDiacritic());
            settingsService.setIgnoreFullWidth(true);
        } else if (command.getIndexScheme() == IndexScheme.WITHOUT_JP_LANG_PROCESSING) {
            settingsService.setDeleteDiacritic(command.isDeleteDiacritic());
            settingsService.setIgnoreFullWidth(command.isIgnoreFullWidth());
        }

        // Sort settings
        settingsService.setSortAlbumsByYear(command.isSortAlbumsByYear());
        settingsService.setSortGenresByAlphabet(command.isSortGenresByAlphabet());
        settingsService.setProhibitSortVarious(command.isProhibitSortVarious());

        // Search settings
        settingsService.setSearchComposer(command.isSearchComposer());
        settingsService.setOutputSearchQuery(command.isOutputSearchQuery());

        // Suppressed legacy features
        settingsService.setShowServerLog(command.isShowServerLog());
        settingsService.setShowStatus(command.isShowStatus());
        settingsService.setOthersPlayingEnabled(command.isOthersPlayingEnabled());
        settingsService.setShowRememberMe(command.isShowRememberMe());
        settingsService.setPublishPodcast(command.isPublishPodcast());
        settingsService.setUseExternalPlayer(command.isUseExternalPlayer());
        if (!command.isUseExternalPlayer()) {
            playerService.resetExternalPlayer();
        }
        settingsService.setUseCopyOfAsciiUnprintable(PlayerUtils.isWindows() && command.isUseCopyOfAsciiUnprintable());
        settingsService.setUseJsonp(command.isUseJsonp());
        if (!scannerStateService.isScanning()) {
            settingsService.setUseRemovingTrackFromId3Title(command.isUseRemovingTrackFromId3Title());
            settingsService.setUseCleanUp(command.isUseCleanUp());
            settingsService.setRedundantFolderCheck(command.isRedundantFolderCheck());
        }
        settingsService.setShowIndexDetails(command.isShowIndexDetails());
        settingsService.setShowDBDetails(command.isShowDBDetails());
        settingsService.setUseCast(command.isUseCast());

        // Extensions and shortcuts
        if (!scannerStateService.isScanning()) {
            settingsService.setMusicFileTypes(command.getMusicFileTypes());
            settingsService.setVideoFileTypes(command.getVideoFileTypes());
            settingsService.setCoverArtFileTypes(command.getCoverArtFileTypes());
            settingsService.setExcludedCoverArts(command.getExcludedCoverArts());
            PathValidator.validateFolderPath(command.getPlaylistFolder())
                    .ifPresent(folderPath -> settingsService.setPlaylistFolder(folderPath));
            settingsService.setShortcuts(command.getShortcuts());
        }

        // Welcom message
        settingsService.setGettingStartedEnabled(command.isGettingStartedEnabled());
        settingsService.setWelcomeTitle(command.getWelcomeTitle());
        settingsService.setWelcomeSubtitle(command.getWelcomeSubtitle());
        settingsService.setWelcomeMessage(command.getWelcomeMessage());
        settingsService.setLoginMessage(command.getLoginMessage());

        settingsService.save();

        // for view page control
        settingsService.setUseRadio(command.isUseRadio());
        if (!isReload) {
            redirectAttributes.addFlashAttribute(Attributes.Redirect.TOAST_FLAG.value(), true);
        }

        return new ModelAndView(new RedirectView(ViewName.GENERAL_SETTINGS.value()));
    }
}
