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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.PodcastStatus;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.PlayerUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.springframework.stereotype.Service;

@Service
public class PodcastUpnpProcessor extends DirectChildrenContentProcessor<PodcastChannel, PodcastEpisode> {

    private static final ThreadLocal<DateTimeFormatter> DATE_FORMAT = ThreadLocal
            .withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()));
    private final UpnpDIDLFactory factory;
    private final MediaFileService mediaFileService;
    private final PodcastService podcastService;

    public PodcastUpnpProcessor(UpnpDIDLFactory factory, MediaFileService mediaFileService,
            PodcastService podcastService) {
        super();
        this.factory = factory;
        this.mediaFileService = mediaFileService;
        this.podcastService = podcastService;
    }

    @Override
    public ProcId getProcId() {
        return ProcId.PODCAST;
    }

    @Override
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults) throws ExecutionException {
        DIDLContent didl = new DIDLContent();
        List<PodcastChannel> channels = getDirectChildren(firstResult, maxResults);
        for (PodcastChannel channel : channels) {
            addItem(didl, channel);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getDirectChildrenCount());
    }

    @Override
    public Container createContainer(PodcastChannel channel) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.PODCAST.getValue() + ProcId.CID_SEPA + channel.getId());
        container.setParentID(ProcId.PODCAST.getValue());
        container.setTitle(channel.getTitle());
        container.setChildCount(podcastService.getEpisodes(channel.getId()).size());
        if (!isEmpty(channel.getImageUrl())) {
            container.addProperty(factory.toPodcastArt(channel));
        }
        return container;
    }

    public final Item createItem(PodcastEpisode episode) {

        MusicTrack item = new MusicTrack();

        item.setId(String.valueOf(episode.getId()));
        item.setTitle(episode.getTitle());

        item.setParentID(String.valueOf(episode.getChannelId()));
        PodcastChannel channel = podcastService.getChannel(episode.getChannelId());
        if (channel != null) {
            item.setAlbum(channel.getTitle());
            if (!isEmpty(channel.getImageUrl())) {
                item.addProperty(factory.toPodcastArt(channel));
            }
        }

        if (!isEmpty(episode.getPublishDate())) {
            synchronized (DATE_FORMAT) {
                item.setDate(DATE_FORMAT.get().format(episode.getPublishDate()));
            }
        }

        if (episode.getStatus() == PodcastStatus.COMPLETED && !isEmpty(episode.getMediaFileId())) {
            MediaFile song = mediaFileService.getMediaFileStrict(episode.getMediaFileId());
            item.setResources(Arrays.asList(factory.toRes(song)));
        }

        return item;
    }

    @Override
    public int getDirectChildrenCount() {
        return podcastService.getAllChannels().size();
    }

    @Override
    public List<PodcastChannel> getDirectChildren(long offset, long maxResults) {
        return PlayerUtils.subList(podcastService.getAllChannels(), offset, maxResults);
    }

    @Override
    public PodcastChannel getDirectChild(String id) {
        return podcastService.getChannel(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(PodcastChannel channel) {
        return podcastService.getEpisodes(channel.getId()).size();
    }

    @Override
    public List<PodcastEpisode> getChildren(PodcastChannel channel, long offset, long maxResults) {
        return PlayerUtils.subList(podcastService.getEpisodes(channel.getId()), offset, maxResults);
    }

    @Override
    public void addChild(DIDLContent didl, PodcastEpisode child) {
        didl.addItem(createItem(child));
    }
}
