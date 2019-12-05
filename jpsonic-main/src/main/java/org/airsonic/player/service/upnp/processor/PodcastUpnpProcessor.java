/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PodcastChannel;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.domain.logic.CoverArtLogic;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PodcastService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.upnp.UpnpProcessDispatcher;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
public class PodcastUpnpProcessor extends UpnpContentProcessor <PodcastChannel, PodcastEpisode> {

    private final PodcastService podcastService;

    private final MediaFileService mediaFileService;

    private final CoverArtLogic coverArtLogic;

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public PodcastUpnpProcessor(UpnpProcessDispatcher dispatcher, SettingsService settingsService, SearchService searchService, JWTSecurityService jwtSecurityService, PodcastService podcastService,
            CoverArtLogic coverArtLogic, MediaFileService mediaFileService) {
        super(dispatcher, settingsService, searchService, jwtSecurityService);
        this.podcastService = podcastService;
        this.mediaFileService = mediaFileService;
        this.coverArtLogic = coverArtLogic;
        setRootId(UpnpProcessDispatcher.CONTAINER_ID_PODCAST_PREFIX);
    }

    @PostConstruct
    public void initTitle() {
        setRootTitleWithResource("dlna.title.podcast");
    }

    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<PodcastChannel> channels = getItems(firstResult, maxResults);
        for (PodcastChannel channel : channels) {
            addItem(didl, channel);
        }
        return createBrowseResult(didl, (int) didl.getCount(), getItemCount());
    }

    public Container createContainer(PodcastChannel channel) {
        MusicAlbum container = new MusicAlbum();
        container.setId(getRootId() + UpnpProcessDispatcher.OBJECT_ID_SEPARATOR + channel.getId());
        container.setParentID(getRootId());
        container.setTitle(channel.getTitle());
        container.setChildCount(podcastService.getEpisodes(channel.getId()).size());
        if (!isEmpty(channel.getImageUrl())) {
            container.setProperties(Arrays.asList(new ALBUM_ART_URI(createPodcastChannelURI(channel))));
        }
        return container;
    }

    public final Item createItem(PodcastEpisode episode) {

        MusicTrack item = new MusicTrack();

        item.setId(String.valueOf(episode.getId()));
        item.setTitle(episode.getTitle());

        item.setParentID(String.valueOf(episode.getChannelId()));
        PodcastChannel channel = podcastService.getChannel(episode.getChannelId());
        item.setAlbum(channel.getTitle());
        item.addProperty(new ALBUM_ART_URI(createPodcastChannelURI(channel)));

        Date publishDate = episode.getPublishDate();
        if (isEmpty(publishDate)) {
            Calendar.getInstance();
            Calendar c = Calendar.getInstance();
            c.setTime(publishDate);
            item.setDate(DATE_FORMAT.format(c.getTime()));
        }

        if (episode.getStatus() == PodcastStatus.COMPLETED) {
            if (!isEmpty(episode.getMediaFileId())) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getMediaFileId());
                item.setResources(Arrays.asList(getDispatcher().getMediaFileProcessor().createResourceForSong(mediaFile)));
            }
        }

        return item;
    }

    @Override
    public int getItemCount() {
        return podcastService.getAllChannels().size();
    }

    @Override
    public List<PodcastChannel> getItems(long offset, long maxResults) {
        return org.airsonic.player.util.Util.subList(podcastService.getAllChannels(), offset, maxResults);
    }

    public PodcastChannel getItemById(String id) {
        return podcastService.getChannel(Integer.parseInt(id));
    }

    @Override
    public int getChildSizeOf(PodcastChannel channel) {
        return podcastService.getEpisodes(channel.getId()).size();
    }

    @Override
    public List<PodcastEpisode> getChildren(PodcastChannel channel, long offset, long maxResults) {
        return org.airsonic.player.util.Util.subList(podcastService.getEpisodes(channel.getId()), offset, maxResults);
    }

    public void addChild(DIDLContent didl, PodcastEpisode child) {
        didl.addItem(createItem(child));
    }

    private URI createPodcastChannelURI(PodcastChannel channel) {
        return createURIWithToken(UriComponentsBuilder.fromUriString(getBaseUrl() + "/ext/coverArt.view")
                .queryParam("id", coverArtLogic.createKey(channel))
                .queryParam("size", CoverArtScheme.LARGE.getSize()));
    }

}
