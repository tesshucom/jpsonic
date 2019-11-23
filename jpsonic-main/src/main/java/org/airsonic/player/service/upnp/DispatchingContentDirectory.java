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

import org.airsonic.player.domain.CoverArtScheme;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.service.*;
import org.airsonic.player.service.search.IndexType;
import org.airsonic.player.service.upnp.processor.AlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.ArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.GenreUpnpProcessor;
import org.airsonic.player.service.upnp.processor.IndexUpnpProcessor;
import org.airsonic.player.service.upnp.processor.MediaFileUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PlaylistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumId3UpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RootUpnpProcessor;
import org.airsonic.player.service.upnp.processor.UpnpContentProcessor;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Allen Petersen
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class DispatchingContentDirectory extends CustomContentDirectory implements UpnpProcessDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchingContentDirectory.class);

    @Autowired
    private PlaylistUpnpProcessor playlistProcessor;

    @Qualifier("mediaFileUpnpProcessor")
    @Autowired
    private MediaFileUpnpProcessor mediaFileProcessor;

    @Qualifier("albumUpnpProcessor")
    @Autowired
    private AlbumUpnpProcessor albumProcessor;

    @Qualifier("recentAlbumUpnpProcessor")
    @Autowired
    private RecentAlbumUpnpProcessor recentAlbumProcessor;

    @Qualifier("recentAlbumId3UpnpProcessor")
    @Autowired
    private RecentAlbumId3UpnpProcessor recentAlbumId3Processor;

    @Autowired
    private ArtistUpnpProcessor artistProcessor;

    @Autowired
    private GenreUpnpProcessor genreProcessor;

    @Autowired
    private RootUpnpProcessor rootProcessor;

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private IndexUpnpProcessor IndexUpnpProcessor;

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag,
                               String filter, long firstResult,
                               long maxResults, SortCriterion[] orderBy)
        throws ContentDirectoryException {

        LOG.debug("UPnP request - objectId: " + objectId + ", browseFlag: " + browseFlag + ", filter: " + filter + ", firstResult: " + firstResult + ", maxResults: " + maxResults);

        if (objectId == null)
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "objectId is null");

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
            if (processor == null) {
                // if it's null then assume it's a file, and that the id
                // is all that's there.
                itemId = browseRoot;
                processor = getMediaFileProcessor();
            }

            if (itemId == null) {
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

    private final int COUNT_MAX = 50;
    
    /*
     * Legacy implementation assumes title search(Ignore other fields if specified).
     */
    private final Pattern TITLE_SEARCH = Pattern.compile("^.*dc:title.*$");
    
    @Override
    public BrowseResult search(String containerId,
                               String criteria, String filter,
                               long firstResult, long maxResults,
                               SortCriterion[] orderBy) throws ContentDirectoryException {
        long offset = firstResult;
        long count = maxResults;
        if ((offset + count) > COUNT_MAX) {
            count = COUNT_MAX - offset;
        }
        String upnpClass = criteria.replaceAll("^.*upnp:class\\s+[\\S]+\\s+\"([\\S]*)\".*$", "$1");
        String query = criteria.replaceAll("^.*dc:title\\s+[\\S]+\\s+\"([\\S]*)\".*$", "$1");
        BrowseResult returnValue = null;

        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setOffset((int) firstResult);
        searchCriteria.setCount((int) maxResults);
        searchCriteria.setIncludeComposer(settingsService.isSearchComposer());        
        searchCriteria.setQuery(query);

        boolean isDlnaFileStructure = settingsService.isDlnaFileStructureSearch();

        if (TITLE_SEARCH.matcher(criteria).matches()) {
            if ("object.container.person.musicArtist".equalsIgnoreCase(upnpClass)) {
                returnValue = isDlnaFileStructure ? getMediaFileProcessor().search(searchCriteria, IndexType.ARTIST)
                        : getArtistProcessor().searchByName(query, offset, count, orderBy);
            } else if ("object.container.album.musicAlbum".equalsIgnoreCase(upnpClass)) {
                returnValue = isDlnaFileStructure ? getMediaFileProcessor().search(searchCriteria, IndexType.ALBUM)
                        : getAlbumProcessor().searchByName(query, offset, count, orderBy);
            } else if ("object.item.audioItem".equalsIgnoreCase(upnpClass)) {
                returnValue = isDlnaFileStructure ? getMediaFileProcessor().search(searchCriteria, IndexType.SONG)
                        : getMediaFileProcessor().searchByName(query, offset, count, orderBy);
            }
        } else {
            LOG.debug("Does not support field search other than title. QUERY : {}", criteria);
        }
        return returnValue;
    }

    @SuppressWarnings("rawtypes")
    private UpnpContentProcessor findProcessor(String type) {
        switch (type) {
            case CONTAINER_ID_ROOT:
                return getRootProcessor();
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
            case CONTAINER_ID_GENRE_PREFIX:
                return getGenreProcessor();
            case CONTAINER_ID_INDEX_PREFIX:
                return getIndexProcessor();
        }
        return null;
    }

    public Item createItem(MediaFile song) {
        MediaFile parent = mediaFileService.getParentOf(song);
        MusicTrack item = new MusicTrack();
        item.setId(String.valueOf(song.getId()));
        item.setParentID(String.valueOf(parent.getId()));
        item.setTitle(song.getTitle());
        item.setAlbum(song.getAlbumName());
        if (song.getArtist() != null) {
            item.setArtists(new PersonWithRole[] { new PersonWithRole(song.getArtist()) });
        }
        Integer year = song.getYear();
        if (year != null) {
            item.setDate(year + "-01-01");
        }
        item.setOriginalTrackNumber(song.getTrackNumber());
        if (song.getGenre() != null) {
            item.setGenres(new String[] { song.getGenre() });
        }
        item.setResources(Arrays.asList(createResourceForSong(song)));
        item.setDescription(song.getComment());
        item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(getAlbumArtUrl(parent.getId())));

        return item;
    }

    private final URI getAlbumArtUrl(int id) {
        return jwtSecurityService.addJWTToken(UriComponentsBuilder.fromUriString(getBaseUrl() + "/ext/coverArt.view").queryParam("id", id).queryParam("size", CoverArtScheme.LARGE.getSize())).build().encode().toUri();
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
    public GenreUpnpProcessor getGenreProcessor() {
        return genreProcessor;
    }

    @Override
    public RootUpnpProcessor getRootProcessor() {
        return rootProcessor;
    }

    @Override
    public IndexUpnpProcessor getIndexProcessor() {
        return IndexUpnpProcessor;
    }

}
