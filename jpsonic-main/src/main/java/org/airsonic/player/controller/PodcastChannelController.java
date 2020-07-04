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
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.controller;

import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller for the "Podcast channel" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastChannel")
public class PodcastChannelController {

    @Autowired
    private PodcastService podcastService;
    @Autowired
    private SecurityService securityService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ModelAndView result = new ModelAndView();

        int channelId = ServletRequestUtils.getRequiredIntParameter(request, "id");
        result.addObject("model", LegacyMap.of(
                "user", securityService.getCurrentUser(request),
                "channel", podcastService.getChannel(channelId),
                "episodes", podcastService.getEpisodes(channelId)));

        return result;
    }

}
