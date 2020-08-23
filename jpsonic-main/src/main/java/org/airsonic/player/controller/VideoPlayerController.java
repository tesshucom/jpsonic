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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Controller for the page used to play videos.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/videoPlayer")
public class VideoPlayerController {

    public static final int DEFAULT_BIT_RATE = 2000;
    private static final int[] BIT_RATES = {200, 300, 400, 500, 700, 1000, 1200, 1500, 2000, 3000, 5000};

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        int id = ServletRequestUtils.getRequiredIntParameter(request, "id");
        MediaFile file = mediaFileService.getMediaFile(id);
        MediaFile dir = mediaFileService.getParentOf(file);

        // Redirect if root directory.
        if (mediaFileService.isRoot(dir)) {
            return new ModelAndView(new RedirectView("home.view?"));
        }

        String username = securityService.getCurrentUsername(request);
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            return new ModelAndView(new RedirectView("accessDenied.view"));
        }

        User user = securityService.getCurrentUser(request);
        mediaFileService.populateStarredDate(file, user.getUsername());
        Integer duration = file.getDurationSeconds();
        Integer playerId = playerService.getPlayer(request, response).getId();
        String url = NetworkService.getBaseUrl(request);
        String streamUrl = url + "stream?id=" + file.getId() + "&player=" + playerId + "&format=mp4";
        String coverArtUrl = url + "coverArt.view?id=" + file.getId();
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        Map<String, Object> map = LegacyMap.of();
        map.put("dir", dir);
        map.put("ancestors", getAncestors(file));
        map.put("breadcrumbIndex", userSettings.isBreadcrumbIndex());
        map.put("selectedMusicFolder", settingsService.getSelectedMusicFolder(user.getUsername()));
        map.put("video", file);
        map.put("streamUrl", streamUrl);
        map.put("remoteStreamUrl", streamUrl);
        map.put("remoteCoverArtUrl", coverArtUrl);
        map.put("duration", duration);
        map.put("bitRates", BIT_RATES);
        map.put("defaultBitRate", DEFAULT_BIT_RATE);
        map.put("user", user);
        try {
            MediaFile parent = mediaFileService.getParentOf(file);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !mediaFileService.isRoot(parent));
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return new ModelAndView("videoPlayer", "model", map);
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap") /* LinkedHashMap used in Legacy code */
    public static Map<String, Integer> createSkipOffsets(int durationSeconds) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < durationSeconds; i += 60) {
            result.put(StringUtil.formatDurationMSS(i), i);
        }
        return result;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private List<MediaFile> getAncestors(MediaFile dir) {
        LinkedList<MediaFile> result = new LinkedList<>();

        try {
            MediaFile parent = mediaFileService.getParentOf(dir);
            while (parent != null && !mediaFileService.isRoot(parent)) {
                result.addFirst(parent);
                parent = mediaFileService.getParentOf(parent);
            }
        } catch (SecurityException x) {
            // Happens if Podcast directory is outside music folder.
        }
        return result;
    }

}
