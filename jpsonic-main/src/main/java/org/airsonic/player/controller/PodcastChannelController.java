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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.controller;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.controller.Attributes;
import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the "Podcast channel" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/podcastChannel")
public class PodcastChannelController {

    private final PodcastService podcastService;
    private final SecurityService securityService;

    public PodcastChannelController(PodcastService podcastService, SecurityService securityService) {
        super();
        this.podcastService = podcastService;
        this.securityService = securityService;
    }

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws ServletRequestBindingException {

        ModelAndView result = new ModelAndView();

        int channelId = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        result.addObject("model",
                LegacyMap.of("user", securityService.getCurrentUser(request), "channel",
                        podcastService.getChannel(channelId), "episodes", podcastService.getEpisodes(channelId),
                        "coverArtSize", CoverArtScheme.LARGE.getSize()));

        return result;
    }

}
