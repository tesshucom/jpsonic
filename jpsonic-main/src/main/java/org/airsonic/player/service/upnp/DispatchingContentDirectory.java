/*
  This file is part of Airsonic.

  Airsonic is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Airsonic is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

  Copyright 2016 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.UPnPSearchCriteria;
import org.airsonic.player.service.search.UPnPSearchCriteriaDirector;
import org.airsonic.player.service.upnp.processor.AlbumByGenreUpnpProcessor;
import org.airsonic.player.service.upnp.processor.AlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.ArtistByFolderUpnpProcessor;
import org.airsonic.player.service.upnp.processor.ArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.IndexId3UpnpProcessor;
import org.airsonic.player.service.upnp.processor.IndexUpnpProcessor;
import org.airsonic.player.service.upnp.processor.MediaFileUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PlaylistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PodcastUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RandomAlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RandomSongByArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RandomSongByFolderArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RandomSongUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumId3UpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RootUpnpProcessor;
import org.airsonic.player.service.upnp.processor.SongByGenreUpnpProcessor;
import org.airsonic.player.service.upnp.processor.UpnpContentProcessor;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@DependsOn({ "rootUpnpProcessor", "mediaFileUpnpProcessor" })
public class DispatchingContentDirectory extends CustomContentDirectory implements UpnpProcessDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchingContentDirectory.class);

    private final int COUNT_MAX = 50;

    private final RootUpnpProcessor rootProcessor;
    private final MediaFileUpnpProcessor mediaFileProcessor;
    private final PlaylistUpnpProcessor playlistProcessor;
    private final AlbumUpnpProcessor albumProcessor;
    private final RecentAlbumUpnpProcessor recentAlbumProcessor;
    private final RecentAlbumId3UpnpProcessor recentAlbumId3Processor;
    private final ArtistUpnpProcessor artistProcessor;
    private final ArtistByFolderUpnpProcessor artistByFolderProcessor;
    private final AlbumByGenreUpnpProcessor albumByGenreProcessor;
    private final SongByGenreUpnpProcessor songByGenreProcessor;
    private final IndexUpnpProcessor indexProcessor;
    private final IndexId3UpnpProcessor indexId3Processor;
    private final PodcastUpnpProcessor podcastProcessor;
    private final RandomAlbumUpnpProcessor randomAlbumProcessor;
    private final RandomSongUpnpProcessor randomSongProcessor;
    private final RandomSongByArtistUpnpProcessor randomSongByArtistProcessor;
    private final RandomSongByFolderArtistUpnpProcessor randomSongByFolderArtistProcessor;

    private final UPnPSearchCriteriaDirector director;
    private final SearchService searchService;

    public DispatchingContentDirectory(// @formatter:off
            RootUpnpProcessor rp,
            @Qualifier("mediaFileUpnpProcessor") MediaFileUpnpProcessor mfp,
            @Lazy PlaylistUpnpProcessor playp,
            @Lazy @Qualifier("albumUpnpProcessor") AlbumUpnpProcessor ap,
            @Lazy @Qualifier("recentAlbumUpnpProcessor") RecentAlbumUpnpProcessor rap,
            @Lazy @Qualifier("recentAlbumId3UpnpProcessor") RecentAlbumId3UpnpProcessor raip,
            @Lazy ArtistUpnpProcessor arP,
            @Lazy ArtistByFolderUpnpProcessor abfP,
            @Lazy AlbumByGenreUpnpProcessor abgp,
            @Lazy SongByGenreUpnpProcessor sbgp,
            @Lazy @Qualifier("indexUpnpProcessor") IndexUpnpProcessor ip,
            @Lazy IndexId3UpnpProcessor iip,
            @Lazy @Qualifier("podcastUpnpProcessor") PodcastUpnpProcessor podp,
            @Lazy @Qualifier("randomAlbumUpnpProcessor") RandomAlbumUpnpProcessor randomap,
            @Lazy @Qualifier("randomSongUpnpProcessor") RandomSongUpnpProcessor randomsp,
            @Lazy RandomSongByArtistUpnpProcessor randomsbap,
            @Lazy RandomSongByFolderArtistUpnpProcessor randomsbfap,
            UPnPSearchCriteriaDirector cd,
            SearchService ss) { // @formatter:on
        super();
        rootProcessor = rp;
        mediaFileProcessor = mfp;
        playlistProcessor = playp;
        albumProcessor = ap;
        recentAlbumProcessor = rap;
        recentAlbumId3Processor = raip;
        artistProcessor = arP;
        artistByFolderProcessor = abfP;
        albumByGenreProcessor = abgp;
        songByGenreProcessor = sbgp;
        indexProcessor = ip;
        indexId3Processor = iip;
        podcastProcessor = podp;
        randomAlbumProcessor = randomap;
        randomSongProcessor = randomsp;
        randomSongByArtistProcessor = randomsbap;
        randomSongByFolderArtistProcessor = randomsbfap;
        director = cd;
        searchService = ss;
    }

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag,
                               String filter, long firstResult,
                               long maxResults, SortCriterion[] orderBy)
        throws ContentDirectoryException {

        if (isEmpty(objectId)) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "objectId is null");
        }

        // maxResult == 0 means all.
        if (maxResults == 0) {
            maxResults = Long.MAX_VALUE;
        }

        BrowseResult returnValue = null;
        try {
            String[] splitId = objectId.split(OBJECT_ID_SEPARATOR);
            String browseRoot = splitId[0];
            String itemId = splitId.length == 1 ? null : splitId[1];

            @SuppressWarnings("rawtypes")
            UpnpContentProcessor processor = findProcessor(browseRoot);
            if (isEmpty(processor)) {
                // if it's null then assume it's a file, and that the id
                // is all that's there.
                itemId = browseRoot;
                processor = getMediaFileProcessor();
            }

            if (isEmpty(itemId)) {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseRootMetadata() : processor.browseRoot(filter, firstResult, maxResults, orderBy);
            } else {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseObjectMetadata(itemId) : processor.browseObject(itemId, filter, firstResult, maxResults, orderBy);
            }
            return returnValue;
        } catch (Throwable x) {
            if (LOG.isErrorEnabled()) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("UPnP error: " + x, x);
                }
            }
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, x.toString());
        }
    }

    @Override
    public BrowseResult search(String containerId,
                               String upnpSearchQuery, String filter,
                               long firstResult, long maxResults,
                               SortCriterion[] orderBy) throws ContentDirectoryException {

        int offset = (int) firstResult;
        int count = (int) maxResults;
        if ((offset + count) > COUNT_MAX) {
            count = COUNT_MAX - offset;
        }

        UPnPSearchCriteria criteria = director.construct(offset, count, upnpSearchQuery);

        if (Artist.class == criteria.getAssignableClass()) {
            return getArtistProcessor().toBrowseResult(searchService.search(criteria));
        } else if (Album.class == criteria.getAssignableClass()) {
            return getAlbumProcessor().toBrowseResult(searchService.search(criteria));
        } else if (MediaFile.class == criteria.getAssignableClass()) {
            return getMediaFileProcessor().toBrowseResult(searchService.search(criteria));
        }

        return new BrowseResult(StringUtils.EMPTY, 0, 0L, 0L);
    }

    @Override
    public RootUpnpProcessor getRootProcessor() {
        return rootProcessor;
    }

    @Override
    public PlaylistUpnpProcessor getPlaylistProcessor() {
        return playlistProcessor;
    }

    @Override
    public MediaFileUpnpProcessor getMediaFileProcessor() {
        return mediaFileProcessor;
    }

    @Override
    public AlbumUpnpProcessor getAlbumProcessor() {
        return albumProcessor;
    }

    public RecentAlbumUpnpProcessor getRecentAlbumProcessor() {
        return recentAlbumProcessor;
    }

    @Override
    public RecentAlbumId3UpnpProcessor getRecentAlbumId3Processor() {
        return recentAlbumId3Processor;
    }

    @Override
    public ArtistUpnpProcessor getArtistProcessor() {
        return artistProcessor;
    }

    @Override
    public ArtistByFolderUpnpProcessor getArtistByFolderProcessor() {
        return artistByFolderProcessor;
    }

    @Override
    public AlbumByGenreUpnpProcessor getAlbumByGenreProcessor() {
        return albumByGenreProcessor;
    }

    @Override
    public SongByGenreUpnpProcessor getSongByGenreProcessor() {
        return songByGenreProcessor;
    }

    @Override
    public IndexUpnpProcessor getIndexProcessor() {
        return indexProcessor;
    }

    public IndexId3UpnpProcessor getIndexId3Processor() {
        return indexId3Processor;
    }

    @Override
    public PodcastUpnpProcessor getPodcastProcessor() {
        return podcastProcessor;
    }

    @Override
    public RandomAlbumUpnpProcessor getRandomAlbumProcessor() {
        return randomAlbumProcessor;
    }

    @Override
    public RandomSongUpnpProcessor getRandomSongProcessor() {
        return randomSongProcessor;
    }

    @Override
    public RandomSongByArtistUpnpProcessor getRandomSongByArtistProcessor() {
        return randomSongByArtistProcessor;
    }

    @Override
    public RandomSongByFolderArtistUpnpProcessor getRandomSongByFolderArtistProcessor() {
        return randomSongByFolderArtistProcessor;
    }

}
