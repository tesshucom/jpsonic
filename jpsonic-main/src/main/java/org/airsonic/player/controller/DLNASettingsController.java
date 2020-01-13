/*
 * This file is part of Airsonic.
 *
 * Airsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Airsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2013 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.UPnPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Controller for the page used to administrate the UPnP/DLNA server settings.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/dlnaSettings")
public class DLNASettingsController {

    @Autowired
    private UPnPService upnpService;

    @Autowired
    private SettingsService settingsService;

    @GetMapping
    public String handleGet(Model model) {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("dlnaEnabled", settingsService.isDlnaEnabled());
        map.put("dlnaServerName", settingsService.getDlnaServerName());
        map.put("dlnaBaseLANURL", settingsService.getDlnaBaseLANURL());

        map.put("dlnaAlbumVisible", settingsService.isDlnaAlbumVisible());
        map.put("dlnaArtistVisible", settingsService.isDlnaArtistVisible());
        map.put("dlnaAlbumByGenreVisible", settingsService.isDlnaAlbumByGenreVisible());
        map.put("dlnaSongByGenreVisible", settingsService.isDlnaSongByGenreVisible());
        map.put("dlnaGenreCountVisible", settingsService.isDlnaGenreCountVisible());
        map.put("dlnaFolderVisible", settingsService.isDlnaFolderVisible());
        map.put("dlnaPlaylistVisible", settingsService.isDlnaPlaylistVisible());
        map.put("dlnaRecentAlbumVisible", settingsService.isDlnaRecentAlbumVisible());
        map.put("dlnaRecentAlbumId3Visible", settingsService.isDlnaRecentAlbumId3Visible());
        map.put("dlnaRandomAlbumVisible", settingsService.isDlnaRandomAlbumVisible());
        map.put("dlnaRandomSongVisible", settingsService.isDlnaRandomSongVisible());
        map.put("dlnaIndexVisible", settingsService.isDlnaIndexVisible());
        map.put("dlnaPodcastVisible", settingsService.isDlnaPodcastVisible());
        map.put("dlnaGuestPublish", settingsService.isDlnaGuestPublish());

        model.addAttribute("model", map);
        return "dlnaSettings";
    }

    @PostMapping
    public String handlePost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        handleParameters(request);
        redirectAttributes.addFlashAttribute("settings_toast", true);
        return "redirect:dlnaSettings.view";
    }

    private void handleParameters(HttpServletRequest request) {
        boolean dlnaEnabled = ServletRequestUtils.getBooleanParameter(request, "dlnaEnabled", false);
        String dlnaServerName = trimToNull(request.getParameter("dlnaServerName"));
        if (dlnaServerName == null) {
            dlnaServerName = "Jpsonic";
        }
        String dlnaBaseLANURL = trimToNull(request.getParameter("dlnaBaseLANURL"));

        boolean dlnaAlbumVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaAlbumVisible", false);
        boolean dlnaArtistVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaArtistVisible", false);
        boolean dlnaAlbumByGenreVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaAlbumByGenreVisible", false);
        boolean dlnaSongByGenreVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaSongByGenreVisible", false);
        boolean dlnaGenreCountVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaGenreCountVisible", false);
        boolean dlnaFolderVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaFolderVisible", false);
        boolean dlnaPlaylistVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaPlaylistVisible", false);
        boolean dlnaRecentAlbumVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaRecentAlbumVisible", false);
        boolean dlnaRecentAlbumId3Visible = ServletRequestUtils.getBooleanParameter(request, "dlnaRecentAlbumId3Visible", false);
        boolean dlnaRandomAlbumVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaRandomAlbumVisible", false);
        boolean dlnaRandomSongVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaRandomSongVisible", false);
        boolean dlnaIndexVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaIndexVisible", false);
        boolean dlnaPodcastVisible = ServletRequestUtils.getBooleanParameter(request, "dlnaPodcastVisible", false);
        boolean dlnaGuestPublish = ServletRequestUtils.getBooleanParameter(request, "dlnaGuestPublish", false);
        
        boolean isEnabledStateChange =
                !(settingsService.isDlnaEnabled() == dlnaEnabled
                && !isEmpty(dlnaServerName) && dlnaServerName.equals(settingsService.getDlnaServerName())
                && !isEmpty(dlnaBaseLANURL) && dlnaBaseLANURL.equals(settingsService.getDlnaBaseLANURL()));

        settingsService.setDlnaEnabled(dlnaEnabled);
        settingsService.setDlnaServerName(dlnaServerName);
        settingsService.setDlnaBaseLANURL(dlnaBaseLANURL);

        settingsService.setDlnaAlbumVisible(dlnaAlbumVisible);
        settingsService.setDlnaArtistVisible(dlnaArtistVisible);
        settingsService.setDlnaAlbumByGenreVisible(dlnaAlbumByGenreVisible);
        settingsService.setDlnaSongByGenreVisible(dlnaSongByGenreVisible);
        settingsService.setDlnaGenreCountVisible(dlnaGenreCountVisible);
        settingsService.setDlnaFolderVisible(dlnaFolderVisible);
        settingsService.setDlnaPlaylistVisible(dlnaPlaylistVisible);
        settingsService.setDlnaRecentAlbumVisible(dlnaRecentAlbumVisible);
        settingsService.setDlnaRecentAlbumId3Visible(dlnaRecentAlbumId3Visible);
        settingsService.setDlnaRandomAlbumVisible(dlnaRandomAlbumVisible);
        settingsService.setDlnaRandomSongVisible(dlnaRandomSongVisible);
        settingsService.setDlnaIndexVisible(dlnaIndexVisible);
        settingsService.setDlnaPodcastVisible(dlnaPodcastVisible);
        settingsService.setDlnaGuestPublish(dlnaGuestPublish);

        settingsService.save();

        if (isEnabledStateChange) {
            upnpService.setMediaServerEnabled(dlnaEnabled);
        }

    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setUpnpService(UPnPService upnpService) {
        this.upnpService = upnpService;
    }
}
