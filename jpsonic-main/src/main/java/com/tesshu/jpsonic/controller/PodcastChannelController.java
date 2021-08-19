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

package com.tesshu.jpsonic.controller;

import javax.servlet.http.HttpServletRequest;

import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
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
@RequestMapping({ "/podcastChannel", "/podcastChannel.view" })
public class PodcastChannelController {

    private final SecurityService securityService;
    private final PodcastService podcastService;

    public PodcastChannelController(SecurityService securityService, PodcastService podcastService) {
        super();
        this.securityService = securityService;
        this.podcastService = podcastService;
    }

    @GetMapping
    protected ModelAndView get(HttpServletRequest request) throws ServletRequestBindingException {

        ModelAndView result = new ModelAndView();

        int channelId = ServletRequestUtils.getRequiredIntParameter(request, Attributes.Request.ID.value());
        result.addObject("model",
                LegacyMap.of("user", securityService.getCurrentUser(request), "channel",
                        podcastService.getChannel(channelId), "episodes", podcastService.getEpisodes(channelId),
                        "coverArtSize", CoverArtScheme.LARGE.getSize()));

        return result;
    }

}
