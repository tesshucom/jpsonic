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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import java.util.List;

import com.tesshu.jpsonic.domain.model.PodcastChannel;
import com.tesshu.jpsonic.domain.model.PodcastEpisode;
import com.tesshu.jpsonic.domain.provider.resource.PodcastProvider;
import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.springframework.stereotype.Controller;

@Controller
class PodcastProc extends DirectChildrenContentProc<PodcastChannel, PodcastEpisode> {

    private final UPnPDIDLFactory factory;
    private final PodcastProvider podcastProvider;

    PodcastProc(PodcastProvider podcastProvider, UPnPDIDLFactory factory) {
        super();
        this.factory = factory;
        this.podcastProvider = podcastProvider;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PODCAST;
    }

    @Override
    public Container createContainer(PodcastChannel channel) {
        return factory.toAlbum(channel, getChildSizeOf(channel));
    }

    @Override
    public List<PodcastChannel> getDirectChildren(long offset, long count) {
        return podcastProvider.findChannels(offset, count);
    }

    @Override
    public int getDirectChildrenCount() {
        return podcastProvider.countChannels();
    }

    @Override
    public PodcastChannel getDirectChild(String id) {
        return podcastProvider.requireChannel(Integer.parseInt(id));
    }

    @Override
    public List<PodcastEpisode> getChildren(PodcastChannel channel, long offset, long count) {
        return podcastProvider
            .findEpisodes(channel, offset, count, PodcastEpisode.EpisodePhase.COMPLETED);
    }

    @Override
    public int getChildSizeOf(PodcastChannel channel) {
        return podcastProvider.countEpisodes(channel, PodcastEpisode.EpisodePhase.COMPLETED);
    }

    @Override
    public void addChild(DIDLContent parent, PodcastEpisode episode) {
        parent
            .addItem(factory
                .toMusicTrack(episode, podcastProvider.requireChannel(episode.channelId())));
    }
}
