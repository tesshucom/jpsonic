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

import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.PodcastStatus;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the "Podcast receiver" page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/podcastReceiverAdmin", "/podcastReceiverAdmin.view" })
public class PodcastReceiverAdminController {

    private final PodcastService podcastService;

    public PodcastReceiverAdminController(PodcastService podcastService) {
        super();
        this.podcastService = podcastService;
    }

    @RequestMapping(method = { RequestMethod.POST, RequestMethod.GET })
    protected ModelAndView handleRequestInternal(HttpServletRequest request) throws ServletRequestBindingException {

        if (request.getParameter(Attributes.Request.ADD.value()) != null) {
            String url = StringUtils.trim(request.getParameter(Attributes.Request.ADD.value()));
            podcastService.createChannel(url);
            return new ModelAndView(new RedirectView(ViewName.PODCAST_CHANNELS.value()));
        }

        Integer channelId = ServletRequestUtils.getIntParameter(request, Attributes.Request.CHANNEL_ID.value());
        if (request.getParameter(Attributes.Request.DOWNLOAD_EPISODE.value()) != null && channelId != null) {
            download(StringUtil.parseInts(request.getParameter(Attributes.Request.DOWNLOAD_EPISODE.value())));
            return new ModelAndView(new RedirectView(
                    ViewName.PODCAST_CHANNELS.value() + "?" + Attributes.Request.ID.value() + "=" + channelId));
        }
        if (request.getParameter(Attributes.Request.DELETE_CHANNEL.value()) != null && channelId != null) {
            podcastService.deleteChannel(channelId);
            return new ModelAndView(new RedirectView(ViewName.PODCAST_CHANNELS.value()));
        }
        if (request.getParameter(Attributes.Request.DELETE_EPISODE.value()) != null) {
            for (int episodeId : StringUtil
                    .parseInts(request.getParameter(Attributes.Request.DELETE_EPISODE.value()))) {
                podcastService.deleteEpisode(episodeId, true);
            }
            return new ModelAndView(new RedirectView(
                    ViewName.PODCAST_CHANNELS.value() + "?" + Attributes.Request.ID.value() + "=" + channelId));
        }
        if (request.getParameter(Attributes.Request.REFRESH.value()) != null) {
            if (channelId == null) {
                podcastService.refreshAllChannels(true);
                return new ModelAndView(new RedirectView(ViewName.PODCAST_CHANNELS.value()));
            } else {
                podcastService.refreshChannel(channelId, true);
                return new ModelAndView(new RedirectView(
                        ViewName.PODCAST_CHANNELS.value() + "?" + Attributes.Request.ID.value() + "=" + channelId));
            }
        }
        return new ModelAndView(new RedirectView(ViewName.PODCAST_CHANNELS.value()));
    }

    private void download(int... episodeIds) {
        for (int episodeId : episodeIds) {
            PodcastEpisode episode = podcastService.getEpisode(episodeId, false);
            if (episode != null && episode.getUrl() != null && (episode.getStatus() == PodcastStatus.NEW
                    || episode.getStatus() == PodcastStatus.ERROR || episode.getStatus() == PodcastStatus.SKIPPED)) {

                podcastService.downloadEpisode(episode);
            }
        }
    }

}
