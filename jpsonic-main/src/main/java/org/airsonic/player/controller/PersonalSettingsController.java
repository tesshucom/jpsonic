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

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.OutlineHelpSelector;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.controller.WebFontUtils;
import com.tesshu.jpsonic.domain.FontScheme;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.SupportableBCP47;
import org.airsonic.player.command.PersonalSettingsCommand;
import org.airsonic.player.domain.AlbumListType;
import org.airsonic.player.domain.AvatarScheme;
import org.airsonic.player.domain.Theme;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Controller for the page used to administrate per-user settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/personalSettings")
public class PersonalSettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private OutlineHelpSelector outlineHelpSelector;

    @ModelAttribute
    protected void formBackingObject(HttpServletRequest request, Model model, @RequestParam("toast") Optional<Boolean> toast) {
        PersonalSettingsCommand command = new PersonalSettingsCommand();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        command.setUser(user);
        command.setDefaultSettings(settingsService.getUserSettings(""));
        command.setTabletSettings(settingsService.createDefaultTabletUserSettings(""));
        command.setSmartphoneSettings(settingsService.createDefaultSmartphoneUserSettings(""));
        command.setFontFamilyDefault(WebFontUtils.DEFAULT_FONT_FAMILY);
        command.setFontFamilyJpEmbedDefault(WebFontUtils.JP_FONT_NAME.concat(", ").concat(WebFontUtils.DEFAULT_FONT_FAMILY));
        command.setFontSizeDefault(WebFontUtils.DEFAULT_FONT_SIZE);
        command.setFontSizeJpEmbedDefault(WebFontUtils.DEFAULT_JP_FONT_SIZE);
        command.setLocaleIndex("-1");
        command.setThemeIndex("-1");
        command.setAlbumLists(AlbumListType.values());
        command.setAlbumListId(userSettings.getDefaultAlbumList().getId());
        command.setAvatars(settingsService.getAllSystemAvatars());
        command.setCustomAvatar(settingsService.getCustomAvatar(user.getUsername()));
        command.setAvatarId(getAvatarId(userSettings));
        command.setPartyModeEnabled(userSettings.isPartyModeEnabled());
        command.setQueueFollowingSongs(userSettings.isQueueFollowingSongs());
        command.setCloseDrawer(userSettings.isCloseDrawer());
        command.setClosePlayQueue(userSettings.isClosePlayQueue());
        command.setAlternativeDrawer(userSettings.isAlternativeDrawer());
        command.setShowIndex(userSettings.isShowIndex());
        command.setAssignAccesskeyToNumber(userSettings.isAssignAccesskeyToNumber());
        command.setOpenDetailIndex(userSettings.isOpenDetailIndex());
        command.setOpenDetailSetting(userSettings.isOpenDetailSetting());
        command.setOpenDetailStar(userSettings.isOpenDetailStar());
        command.setShowNowPlayingEnabled(userSettings.isShowNowPlayingEnabled());
        command.setShowArtistInfoEnabled(userSettings.isShowArtistInfoEnabled());
        command.setNowPlayingAllowed(userSettings.isNowPlayingAllowed());
        command.setMainVisibility(userSettings.getMainVisibility());
        command.setPlaylistVisibility(userSettings.getPlaylistVisibility());
        command.setFinalVersionNotificationEnabled(userSettings.isFinalVersionNotificationEnabled());
        command.setBetaVersionNotificationEnabled(userSettings.isBetaVersionNotificationEnabled());
        command.setSongNotificationEnabled(userSettings.isSongNotificationEnabled());
        command.setAutoHidePlayQueue(userSettings.isAutoHidePlayQueue());
        command.setKeyboardShortcutsEnabled(userSettings.isKeyboardShortcutsEnabled());
        command.setLastFmEnabled(userSettings.isLastFmEnabled());
        command.setLastFmUsername(userSettings.getLastFmUsername());
        command.setLastFmPassword(userSettings.getLastFmPassword());
        command.setListenBrainzEnabled(userSettings.isListenBrainzEnabled());
        command.setListenBrainzToken(userSettings.getListenBrainzToken());
        command.setPaginationSize(userSettings.getPaginationSize());
        command.setSimpleDisplay(userSettings.isSimpleDisplay());
        command.setShowSibling(userSettings.isShowSibling());
        command.setShowRate(userSettings.isShowRate());
        command.setShowAlbumSearch(userSettings.isShowAlbumSearch());
        command.setShowLastPlay(userSettings.isShowLastPlay());
        command.setShowDownload(userSettings.isShowDownload());
        command.setShowTag(userSettings.isShowTag());
        command.setShowComment(userSettings.isShowComment());
        command.setShowShare(userSettings.isShowShare());
        command.setShowChangeCoverArt(userSettings.isShowChangeCoverArt());
        command.setShowTopSongs(userSettings.isShowTopSongs());
        command.setShowSimilar(userSettings.isShowSimilar());
        command.setShowAlbumActions(userSettings.isShowAlbumActions());
        command.setBreadcrumbIndex(userSettings.isBreadcrumbIndex());
        command.setPutMenuInDrawer(userSettings.isPutMenuInDrawer());
        command.setFontSchemes(FontScheme.values());
        command.setFontSchemeName(userSettings.getFontSchemeName());
        command.setFontSize(userSettings.getFontSize());
        command.setForceBio2Eng(userSettings.isForceBio2Eng());
        command.setShowOutlineHelp(outlineHelpSelector.isShowOutlineHelp(request, user.getUsername()));
        command.setVoiceInputEnabled(userSettings.isVoiceInputEnabled());
        command.setOthersPlayingEnabled(settingsService.isOthersPlayingEnabled());
        command.setShowCurrentSongInfo(userSettings.isShowCurrentSongInfo());
        command.setSpeechLangSchemes(SpeechToTextLangScheme.values());
        command.setSpeechLangSchemeName(userSettings.getSpeechLangSchemeName());
        if (isEmpty(userSettings.getLocale())) {
            command.setIetfDefault(SupportableBCP47.valueOf(settingsService.getLocale()).getValue());
            command.setIetfDisplayDefault(settingsService.getLocale().getDisplayName(settingsService.getLocale()));
        } else {
            command.setIetfDefault(SupportableBCP47.valueOf(userSettings.getLocale()).getValue());
            command.setIetfDisplayDefault(userSettings.getLocale().getDisplayName(userSettings.getLocale()));
        }
        if (SpeechToTextLangScheme.DEFAULT.name().equals(userSettings.getSpeechLangSchemeName())) {
            command.setIetf(SupportableBCP47
                    .valueOf(isEmpty(userSettings.getLocale()) ? settingsService.getLocale() : userSettings.getLocale())
                    .getValue());
        } else {
            command.setIetf(userSettings.getIetf());
        }
        WebFontUtils.setToCommand(userSettings, command);
        toast.ifPresent(b -> command.setShowToast(b));

        Locale currentLocale = userSettings.getLocale();
        Locale[] locales = settingsService.getAvailableLocales();
        String[] localeStrings = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            localeStrings[i] = locales[i].getDisplayName(locales[i]);
            if (locales[i].equals(currentLocale)) {
                command.setLocaleIndex(String.valueOf(i));
            }
        }
        command.setLocales(localeStrings);

        String currentThemeId = userSettings.getThemeId();
        Theme[] themes = settingsService.getAvailableThemes();
        command.setThemes(themes);
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].getId().equals(currentThemeId)) {
                command.setThemeIndex(String.valueOf(i));
                break;
            }
        }

        command.setUseRadio(settingsService.isUseRadio());
        command.setUseSonos(settingsService.isUseSonos());

        model.addAttribute(Attributes.Model.Command.VALUE, command);
    }

    @GetMapping
    protected String displayForm() {
        return "personalSettings";
    }

    @PostMapping
    protected ModelAndView doSubmitAction(@ModelAttribute("command") PersonalSettingsCommand command, RedirectAttributes redirectAttributes) {

        int localeIndex = Integer.parseInt(command.getLocaleIndex());
        Locale locale = null;
        if (localeIndex != -1) {
            locale = settingsService.getAvailableLocales()[localeIndex];
        }

        int themeIndex = Integer.parseInt(command.getThemeIndex());
        String themeId = null;
        if (themeIndex != -1) {
            themeId = settingsService.getAvailableThemes()[themeIndex].getId();
        }

        String username = command.getUser().getUsername();
        UserSettings settings = settingsService.getUserSettings(username);

        settings.setLocale(locale);
        settings.setThemeId(themeId);
        settings.setDefaultAlbumList(AlbumListType.fromId(command.getAlbumListId()));
        settings.setPartyModeEnabled(command.isPartyModeEnabled());
        settings.setQueueFollowingSongs(command.isQueueFollowingSongs());
        if (settingsService.isOthersPlayingEnabled()) {
            settings.setShowNowPlayingEnabled(command.isShowNowPlayingEnabled());
            settings.setNowPlayingAllowed(command.isNowPlayingAllowed());
        } else {
            settings.setShowNowPlayingEnabled(false);
            settings.setNowPlayingAllowed(false);
        }
        settings.setShowArtistInfoEnabled(command.isShowArtistInfoEnabled());
        settings.setCloseDrawer(command.isCloseDrawer());
        settings.setClosePlayQueue(command.isClosePlayQueue());
        settings.setAlternativeDrawer(command.isAlternativeDrawer());
        settings.setShowIndex(command.isShowIndex());
        settings.setAssignAccesskeyToNumber(command.isAssignAccesskeyToNumber());
        settings.setOpenDetailIndex(command.isOpenDetailIndex());
        settings.setOpenDetailSetting(command.isOpenDetailSetting());
        settings.setOpenDetailStar(command.isOpenDetailStar());
        settings.setMainVisibility(command.getMainVisibility());
        settings.setPlaylistVisibility(command.getPlaylistVisibility());
        settings.setFinalVersionNotificationEnabled(command.isFinalVersionNotificationEnabled());
        settings.setBetaVersionNotificationEnabled(command.isBetaVersionNotificationEnabled());
        settings.setSongNotificationEnabled(command.isSongNotificationEnabled());
        settings.setAutoHidePlayQueue(command.isAutoHidePlayQueue());
        settings.setKeyboardShortcutsEnabled(command.isKeyboardShortcutsEnabled());
        settings.setLastFmEnabled(command.isLastFmEnabled());
        settings.setLastFmUsername(command.getLastFmUsername());
        settings.setListenBrainzEnabled(command.isListenBrainzEnabled());
        settings.setListenBrainzToken(command.getListenBrainzToken());
        settings.setSystemAvatarId(getSystemAvatarId(command));
        settings.setAvatarScheme(getAvatarScheme(command));
        settings.setPaginationSize(command.getPaginationSize());
        settings.setSimpleDisplay(command.isSimpleDisplay());
        settings.setShowSibling(command.isShowSibling());
        settings.setShowRate(command.isShowRate());
        settings.setShowAlbumSearch(command.isShowAlbumSearch());
        settings.setShowLastPlay(command.isShowLastPlay());
        settings.setShowDownload(command.isShowDownload());
        settings.setShowTag(command.isShowTag());
        settings.setShowComment(command.isShowComment());
        settings.setShowShare(command.isShowShare());
        settings.setShowChangeCoverArt(command.isShowChangeCoverArt());
        settings.setShowTopSongs(command.isShowTopSongs());
        settings.setShowSimilar(command.isShowSimilar());
        settings.setShowAlbumActions(command.isShowAlbumActions());
        settings.setBreadcrumbIndex(command.isBreadcrumbIndex());
        settings.setPutMenuInDrawer(command.isPutMenuInDrawer());
        settings.setFontSchemeName(command.getFontSchemeName());
        settings.setFontSize(command.getFontSize());
        settings.setForceBio2Eng(command.isForceBio2Eng());
        settings.setVoiceInputEnabled(command.isVoiceInputEnabled());
        settings.setShowCurrentSongInfo(command.isShowCurrentSongInfo());
        settings.setSpeechLangSchemeName(command.getSpeechLangSchemeName());
        if (SpeechToTextLangScheme.DEFAULT.name().equals(command.getSpeechLangSchemeName())) {
            settings.setIetf(SupportableBCP47.valueOf(locale).getValue());
        } else if (StringUtils.isNotBlank(command.getIetf()) && command.getIetf().matches("[a-zA-Z\\-\\_]+")) {
            settings.setIetf(command.getIetf());
        }
        if (StringUtils.isNotBlank(command.getLastFmPassword())) {
            settings.setLastFmPassword(command.getLastFmPassword());
        }
        WebFontUtils.setToSettings(command, settings);
        settings.setChanged(new Date());
        settingsService.updateUserSettings(settings);

        redirectAttributes.addFlashAttribute("settings_reload", true);

        return new ModelAndView(new RedirectView(ViewName.PERSONAL_SETTINGS.value()));
    }

    private int getAvatarId(UserSettings userSettings) {
        AvatarScheme avatarScheme = userSettings.getAvatarScheme();
        return avatarScheme == AvatarScheme.SYSTEM ? userSettings.getSystemAvatarId() : avatarScheme.getCode();
    }

    private AvatarScheme getAvatarScheme(PersonalSettingsCommand command) {
        if (command.getAvatarId() == AvatarScheme.NONE.getCode()) {
            return AvatarScheme.NONE;
        }
        if (command.getAvatarId() == AvatarScheme.CUSTOM.getCode()) {
            return AvatarScheme.CUSTOM;
        }
        return AvatarScheme.SYSTEM;
    }

    private Integer getSystemAvatarId(PersonalSettingsCommand command) {
        int avatarId = command.getAvatarId();
        if (avatarId == AvatarScheme.NONE.getCode() ||
            avatarId == AvatarScheme.CUSTOM.getCode()) {
            return null;
        }
        return avatarId;
    }

}
