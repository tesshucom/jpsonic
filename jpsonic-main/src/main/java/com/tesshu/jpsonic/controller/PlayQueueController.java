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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the playlist frame.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/playQueue")
public class PlayQueueController {

    private final PlayerService playerService;
    private final SecurityService securityService;
    private final SettingsService settingsService;

    public PlayQueueController(PlayerService playerService, SecurityService securityService,
            SettingsService settingsService) {
        super();
        this.playerService = playerService;
        this.securityService = securityService;
        this.settingsService = settingsService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException {

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());
        Player player = playerService.getPlayer(request, response);

        return new ModelAndView("playQueue", "model", LegacyMap.of("user", user, "player", player, "players",
                playerService.getPlayersForUserAndClientId(user.getUsername(), null), "visibility",
                userSettings.getPlaylistVisibility(), "closePlayQueue", userSettings.isClosePlayQueue(), "partyMode",
                userSettings.isPartyModeEnabled(), "notify", userSettings.isSongNotificationEnabled(), "autoHide",
                userSettings.isAutoHidePlayQueue(), "coverArtSize", CoverArtScheme.SMALL.getSize(), "showDownload",
                userSettings.isShowDownload(), "showShare", userSettings.isShowShare(), "alternativeDrawer",
                userSettings.isAlternativeDrawer(), "showAlbumActions", userSettings.isShowAlbumActions(),
                "simpleDisplay", userSettings.isSimpleDisplay(), "playqueueQuickOpen",
                userSettings.isAutoHidePlayQueue()));
    }
}
