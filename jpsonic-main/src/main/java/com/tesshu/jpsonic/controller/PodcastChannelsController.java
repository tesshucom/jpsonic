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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the "Podcast channels" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/podcastChannels", "/podcastChannels.view" })
public class PodcastChannelsController {

    private final SecurityService securityService;
    private final PodcastService podcastService;
    private final ViewAsListSelector viewSelector;

    public PodcastChannelsController(SecurityService securityService, PodcastService podcastService,
            ViewAsListSelector viewSelector) {
        super();
        this.securityService = securityService;
        this.podcastService = podcastService;
        this.viewSelector = viewSelector;
    }

    @GetMapping
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    /*
     * LinkedHashMap used in Legacy code. Should be triaged in #831.
     */
    protected ModelAndView get(HttpServletRequest request, HttpServletResponse response) {

        Map<PodcastChannel, List<PodcastEpisode>> channels = new LinkedHashMap<>();
        Map<Integer, PodcastChannel> channelMap = LegacyMap.of();
        for (PodcastChannel channel : podcastService.getAllChannels()) {
            channels.put(channel, podcastService.getEpisodes(channel.getId()));
            channelMap.put(channel.getId(), channel);
        }

        User user = securityService.getCurrentUser(request);
        Map<String, Object> map = LegacyMap.of("user", securityService.getCurrentUser(request), "channels", channels,
                "channelMap", channelMap, "newestEpisodes", podcastService.getNewestEpisodes(10), "viewAsList",
                viewSelector.isViewAsList(request, user.getUsername()), "coverArtSize",
                CoverArtScheme.MEDIUM.getSize());

        ModelAndView result = new ModelAndView();
        result.addObject("model", map);
        return result;
    }

}
