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
import org.airsonic.player.service.search.UPnPCriteriaDirector;
import org.airsonic.player.service.search.lucene.UPnPSearchCriteria;
import org.airsonic.player.service.upnp.processor.AlbumByGenreUpnpProcessor;
import org.airsonic.player.service.upnp.processor.AlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.ArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.IndexUpnpProcessor;
import org.airsonic.player.service.upnp.processor.MediaFileUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PlaylistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PodcastUpnpProcessor;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
public class DispatchingContentDirectory extends CustomContentDirectory implements UpnpProcessDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchingContentDirectory.class);

    private final int COUNT_MAX = 50;

    @Autowired
    private RootUpnpProcessor rootProcessor;

    @Qualifier("mediaFileUpnpProcessor")
    @Autowired
    private MediaFileUpnpProcessor mediaFileProcessor;

    @Lazy
    @Autowired
    private PlaylistUpnpProcessor playlistProcessor;

    @Lazy
    @Qualifier("albumUpnpProcessor")
    @Autowired
    private AlbumUpnpProcessor albumProcessor;

    @Lazy
    @Qualifier("recentAlbumUpnpProcessor")
    @Autowired
    private RecentAlbumUpnpProcessor recentAlbumProcessor;

    @Lazy
    @Qualifier("recentAlbumId3UpnpProcessor")
    @Autowired
    private RecentAlbumId3UpnpProcessor recentAlbumId3Processor;

    @Lazy
    @Autowired
    private ArtistUpnpProcessor artistProcessor;

    @Lazy
    @Autowired
    private AlbumByGenreUpnpProcessor albumByGenreProcessor;

    @Lazy
    @Autowired
    private SongByGenreUpnpProcessor songByGenreProcessor;

    @Lazy
    @Qualifier("indexUpnpProcessor")
    @Autowired
    private IndexUpnpProcessor indexProcessor;

    @Lazy
    @Qualifier("podcastUpnpProcessor")
    @Autowired
    private PodcastUpnpProcessor podcastProcessor;

    @Autowired
    private UPnPCriteriaDirector criteriaDirector;

    @Autowired
    private SearchService searchService;

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
            LOG.error("UPnP error: " + x, x);
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

        UPnPSearchCriteria upnpCriteria = criteriaDirector.construct(offset, count, upnpSearchQuery);

        if (Artist.class == upnpCriteria.getAssignableClass()) {
            return getArtistProcessor().toBrowseResult(searchService.search(upnpCriteria));
        } else if (Album.class == upnpCriteria.getAssignableClass()) {
            return getAlbumProcessor().toBrowseResult(searchService.search(upnpCriteria));
        } else if (MediaFile.class == upnpCriteria.getAssignableClass()) {
            return getMediaFileProcessor().toBrowseResult(searchService.search(upnpCriteria));
        }

        return new BrowseResult(StringUtils.EMPTY, 0, 0L, 0L);
    }

    @SuppressWarnings("rawtypes")
    private UpnpContentProcessor findProcessor(String type) {
        switch (type) {
            case CONTAINER_ID_ROOT:
                return rootProcessor;
            case CONTAINER_ID_PLAYLIST_PREFIX:
                return getPlaylistProcessor();
            case CONTAINER_ID_FOLDER_PREFIX:
                return getMediaFileProcessor();
            case CONTAINER_ID_ALBUM_PREFIX:
                return getAlbumProcessor();
            case CONTAINER_ID_RECENT_PREFIX:
                return getRecentAlbumProcessor();
            case CONTAINER_ID_RECENT_ID3_PREFIX:
                return getRecentAlbumId3Processor();
            case CONTAINER_ID_ARTIST_PREFIX:
                return getArtistProcessor();
            case CONTAINER_ID_ALBUM_BY_GENRE_PREFIX:
                return getAlbumByGenreProcessor();
            case CONTAINER_ID_SONG_BY_GENRE_PREFIX:
                return getSongByGenreProcessor();
            case CONTAINER_ID_INDEX_PREFIX:
                return getIndexProcessor();
            case CONTAINER_ID_PODCAST_PREFIX:
                return getPodcastProcessor();
        }
        return null;
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

    @Override
    public PodcastUpnpProcessor getPodcastProcessor() {
        return podcastProcessor;
    }

}
