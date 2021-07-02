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

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.InternetRadio;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.SpeechToTextLangScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.VersionService;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the top frame.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/top")
public class TopController {

    // Update this time if you want to force a refresh in clients.
    private static final Calendar LAST_COMPATIBILITY_TIME = Calendar.getInstance();
    private static final List<String> RELOADABLE_MAIN_VIEW_NAME = Arrays.asList(ViewName.MUSIC_FOLDER_SETTINGS.value(),
            ViewName.GENERAL_SETTINGS.value(), ViewName.PERSONAL_SETTINGS.value(), ViewName.USER_SETTINGS.value(),
            ViewName.PLAYER_SETTINGS.value(), ViewName.INTERNET_RADIO_SETTINGS.value(), ViewName.MORE.value());

    private final SettingsService settingsService;
    private final MusicFolderService musicFolderService;
    private final SecurityService securityService;
    private final MediaScannerService mediaScannerService;
    private final MusicIndexService musicIndexService;
    private final VersionService versionService;
    private final InternetRadioService internetRadioService;
    private final AirsonicLocaleResolver localeResolver;

    public TopController(SettingsService settingsService, MusicFolderService musicFolderService,
            SecurityService securityService, MediaScannerService mediaScannerService,
            MusicIndexService musicIndexService, VersionService versionService,
            InternetRadioService internetRadioService, AirsonicLocaleResolver localeResolver) {
        super();
        this.settingsService = settingsService;
        this.musicFolderService = musicFolderService;
        this.securityService = securityService;
        this.mediaScannerService = mediaScannerService;
        this.musicIndexService = musicIndexService;
        this.versionService = versionService;
        this.internetRadioService = internetRadioService;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            @RequestParam("mainView") Optional<String> mainView) throws ServletRequestBindingException {

        Map<String, Object> map = LegacyMap.of();

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        map.put("user", user);
        map.put("othersPlayingEnabled", settingsService.isOthersPlayingEnabled());
        map.put("showNowPlayingEnabled", userSettings.isShowNowPlayingEnabled());
        map.put("showCurrentSongInfo", userSettings.isShowCurrentSongInfo());
        map.put("closeDrawer", userSettings.isCloseDrawer());
        map.put("showAvatar", userSettings.getAvatarScheme() != AvatarScheme.NONE);
        map.put("showIndex", userSettings.isShowIndex());
        map.put("putMenuInDrawer", userSettings.isPutMenuInDrawer());
        map.put("assignAccesskeyToNumber", userSettings.isAssignAccesskeyToNumber());
        map.put("voiceInputEnabled", userSettings.isVoiceInputEnabled());
        map.put("useRadio", settingsService.isUseRadio());

        if (SpeechToTextLangScheme.DEFAULT.name().equals(userSettings.getSpeechLangSchemeName())) {
            map.put("voiceInputLocale", localeResolver.resolveLocale(request).getLanguage());
        } else if (SpeechToTextLangScheme.BCP47.name().equals(userSettings.getSpeechLangSchemeName())
                && userSettings.getIetf() != null) {
            map.put("voiceInputLocale", userSettings.getIetf());
        } else {
            map.put("voiceInputLocale", localeResolver.resolveLocale(request).getLanguage());
        }

        boolean refresh = ServletRequestUtils.getBooleanParameter(request, Attributes.Request.REFRESH.value(), false);
        if (refresh) {
            musicFolderService.clearMusicFolderCache();
        }

        String username = securityService.getCurrentUsername(request);
        List<MusicFolder> allMusicFolders = musicFolderService.getMusicFoldersForUser(username);
        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        List<MusicFolder> musicFoldersToUse = selectedMusicFolder == null ? allMusicFolders
                : Collections.singletonList(selectedMusicFolder);

        map.put("scanning", mediaScannerService.isScanning());
        map.put("musicFolders", allMusicFolders);
        map.put("selectedMusicFolder", selectedMusicFolder);
        map.put("radios", internetRadioService.getAllInternetRadios());
        map.put("shortcuts", musicIndexService.getShortcuts(musicFoldersToUse));
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("alternativeDrawer", userSettings.isAlternativeDrawer());
        map.put("organizeByFolderStructure", settingsService.isOrganizeByFolderStructure());
        boolean musicFolderChanged = saveSelectedMusicFolder(request);
        map.put("musicFolderChanged", musicFolderChanged);

        if (userSettings.isFinalVersionNotificationEnabled() && versionService.isNewFinalVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestFinalVersion());

        } else if (userSettings.isBetaVersionNotificationEnabled() && versionService.isNewBetaVersionAvailable()) {
            map.put("newVersionAvailable", true);
            map.put("latestVersion", versionService.getLatestBetaVersion());
        }
        map.put("brand", settingsService.getBrand());

        MusicFolderContent musicFolderContent = musicIndexService.getMusicFolderContent(musicFoldersToUse, refresh);
        map.put("indexedArtists", musicFolderContent.getIndexedArtists());
        map.put("singleSongs", musicFolderContent.getSingleSongs());
        map.put("indexes", musicFolderContent.getIndexedArtists().keySet());
        map.put("user", securityService.getCurrentUser(request));
        mainView.ifPresent(v -> {
            if (validateMainViewName(v)) {
                map.put("mainView", v);
            }
        });
        return new ModelAndView("top", "model", map);
    }

    static {
        LAST_COMPATIBILITY_TIME.set(2012, Calendar.MARCH, 6, 0, 0, 0);
        LAST_COMPATIBILITY_TIME.set(Calendar.MILLISECOND, 0);
    }

    private boolean validateMainViewName(String mainView) {
        return RELOADABLE_MAIN_VIEW_NAME.contains(mainView);
    }

    /**
     * This method is only used by RESTController.
     */
    public long getLastModified(HttpServletRequest request) throws ServletRequestBindingException {
        saveSelectedMusicFolder(request);

        if (mediaScannerService.isScanning()) {
            return -1L;
        }

        long lastModified = System.currentTimeMillis();
        String username = securityService.getCurrentUsername(request);

        // When was settings last changed?
        lastModified = Math.max(lastModified, settingsService.getSettingsChanged());

        // When was music folder(s) on disk last changed?
        List<MusicFolder> allMusicFolders = musicFolderService.getMusicFoldersForUser(username);
        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(username);
        if (selectedMusicFolder == null) {
            for (MusicFolder musicFolder : allMusicFolders) {
                File file = musicFolder.getPath();
                lastModified = Math.max(lastModified, FileUtil.lastModified(file));
            }
        } else {
            File file = selectedMusicFolder.getPath();
            lastModified = Math.max(lastModified, FileUtil.lastModified(file));
        }

        // When was music folder table last changed?
        for (MusicFolder musicFolder : allMusicFolders) {
            lastModified = Math.max(lastModified, musicFolder.getChanged().getTime());
        }

        // When was internet radio table last changed?
        for (InternetRadio internetRadio : internetRadioService.getAllInternetRadios()) {
            lastModified = Math.max(lastModified, internetRadio.getChanged().getTime());
        }

        // When was user settings last changed?
        UserSettings userSettings = settingsService.getUserSettings(username);
        lastModified = Math.max(lastModified, userSettings.getChanged().getTime());

        return lastModified;
    }

    private boolean saveSelectedMusicFolder(HttpServletRequest request) throws ServletRequestBindingException {
        Integer musicFolderId = ServletRequestUtils.getIntParameter(request,
                Attributes.Request.MUSIC_FOLDER_ID.value());
        if (musicFolderId == null) {
            return false;
        }
        // Note: UserSettings.setChanged() is intentionally not called. This would break browser caching
        // of the left frame.
        UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
        settings.setSelectedMusicFolderId(musicFolderId);
        settingsService.updateUserSettings(settings);

        return true;
    }
}
