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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import net.sf.ehcache.util.FindBugsSuppressWarnings;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Service;

@Service
public class PodcastProc extends DirectChildrenContentProc<PodcastChannel, PodcastEpisode> {

    private final UpnpDIDLFactory factory;
    private final PodcastService podcastService;

    public PodcastProc(UpnpDIDLFactory factory, PodcastService podcastService) {
        super();
        this.factory = factory;
        this.podcastService = podcastService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PODCAST;
    }

    @Override
    public Container createContainer(PodcastChannel channel) {
        return factory.toAlbum(channel, podcastService.getEpisodes(channel.getId()).size());
    }

    @Override
    public List<PodcastChannel> getDirectChildren(long offset, long count) {
        return podcastService.getAllChannels().stream().skip(offset).limit(count).toList();
    }

    @Override
    public int getDirectChildrenCount() {
        return podcastService.getAllChannels().size();
    }

    @Override
    public PodcastChannel getDirectChild(String id) {
        return podcastService.getChannel(Integer.parseInt(id));
    }

    @Override
    public List<PodcastEpisode> getChildren(PodcastChannel channel, long offset, long count) {
        return podcastService.getEpisodes(channel.getId()).stream().skip(offset).limit(count).toList();
    }

    @Override
    public int getChildSizeOf(PodcastChannel channel) {
        return podcastService.getEpisodes(channel.getId()).size();
    }

    @FindBugsSuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE") // false positive
    @Override
    public void addChild(DIDLContent parent, PodcastEpisode episode) {
        if (episode.getId() == null || episode.getChannelId() == null) {
            return;
        }
        PodcastChannel channel = podcastService.getChannel(episode.getChannelId());
        if (channel == null) {
            return;
        }
        parent.addItem(factory.toMusicTrack(episode, channel));
    }
}
