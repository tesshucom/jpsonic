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

import java.io.File;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the "more" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/more")
public class MoreController {

    private final SettingsService settingsService;
    private final SecurityService securityService;
    private final PlayerService playerService;
    private final SearchService searchService;

    public MoreController(SettingsService settingsService, SecurityService securityService, PlayerService playerService,
            SearchService searchService) {
        super();
        this.settingsService = settingsService;
        this.securityService = securityService;
        this.playerService = playerService;
        this.searchService = searchService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        User user = securityService.getCurrentUser(request);

        String uploadDirectory = null;
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(user.getUsername());
        if (!musicFolders.isEmpty()) {
            uploadDirectory = new File(musicFolders.get(0).getPath(), "Incoming").getPath();
        }

        Player player = playerService.getPlayer(request, response);
        ModelAndView result = new ModelAndView();
        result.addObject("model",
                LegacyMap.of("user", user, "uploadDirectory", uploadDirectory, "genres", searchService.getGenres(false),
                        "currentYear", Calendar.getInstance().get(Calendar.YEAR), "musicFolders", musicFolders,
                        "clientSidePlaylist", player.isExternalWithPlaylist() || player.isWeb(), "brand",
                        settingsService.getBrand()));
        return result;
    }
}
