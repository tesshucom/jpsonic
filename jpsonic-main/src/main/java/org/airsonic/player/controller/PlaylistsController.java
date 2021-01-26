/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.ViewAsListSelector;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for the playlists page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playlists")
public class PlaylistsController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private ViewAsListSelector viewSelector;

    @GetMapping
    public String doGet(HttpServletRequest request, Model model) {
        User user = securityService.getCurrentUser(request);
        List<Playlist> playlists = playlistService.getReadablePlaylistsForUser(user.getUsername());
        model.addAttribute("model",
                LegacyMap.of("playlists", playlists, "viewAsList",
                        viewSelector.isViewAsList(request, user.getUsername()), "coverArtSize",
                        CoverArtScheme.MEDIUM.getSize(), "publishPodcast", settingsService.isPublishPodcast()));
        return "playlists";
    }
}
