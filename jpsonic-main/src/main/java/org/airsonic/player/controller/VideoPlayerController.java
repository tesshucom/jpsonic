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

package org.airsonic.player.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.domain.UserSettings;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.NetworkUtils;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the page used to play videos.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/videoPlayer")
public class VideoPlayerController {

    public static final int DEFAULT_BIT_RATE = 2000;
    private static final int[] BIT_RATES = { 200, 300, 400, 500, 700, 1000, 1200, 1500, 2000, 3000, 5000 };

    private final MediaFileService mediaFileService;
    private final PlayerService playerService;
    private final SecurityService securityService;
    private final SettingsService settingsService;

    public VideoPlayerController(MediaFileService mediaFileService, PlayerService playerService,
            SecurityService securityService, SettingsService settingsService) {
        super();
        this.mediaFileService = mediaFileService;
        this.playerService = playerService;
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        int id = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        MediaFile file = mediaFileService.getMediaFile(id);
        MediaFile dir = mediaFileService.getParentOf(file);

        String username = securityService.getCurrentUsername(request);
        if (!securityService.isFolderAccessAllowed(dir, username)) {
            return new ModelAndView(new RedirectView(ViewName.ACCESS_DENIED.value()));
        }

        User user = securityService.getCurrentUser(request);
        mediaFileService.populateStarredDate(file, user.getUsername());
        Integer duration = file.getDurationSeconds();
        Integer playerId = playerService.getPlayer(request, response).getId();
        String url = NetworkUtils.getBaseUrl(request);
        String streamUrl = url + "stream?id=" + file.getId() + "&player=" + playerId + "&format=mp4";
        String coverArtUrl = url + ViewName.COVER_ART.value() + "?id=" + file.getId();
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        Map<String, Object> map = LegacyMap.of();
        map.put("dir", dir);
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
        map.put("isShowDownload", userSettings.isShowDownload());
        map.put("isShowShare", userSettings.isShowShare());

        if (!securityService.isInPodcastFolder(dir.getFile())) {
            MediaFile parent = mediaFileService.getParentOf(file);
            map.put("parent", parent);
            map.put("navigateUpAllowed", !mediaFileService.isRoot(parent));
        }

        return new ModelAndView("videoPlayer", "model", map);
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    /*
     * LinkedHashMap used in Legacy code. Should be triaged in #831.
     */
    public static Map<String, Integer> createSkipOffsets(int durationSeconds) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < durationSeconds; i += 60) {
            result.put(StringUtil.formatDurationMSS(i), i);
        }
        return result;
    }

}
