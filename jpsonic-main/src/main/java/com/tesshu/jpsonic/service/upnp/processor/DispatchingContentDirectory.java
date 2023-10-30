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

package com.tesshu.jpsonic.service.upnp.processor;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.search.QueryFactory;
import com.tesshu.jpsonic.service.search.UPnPSearchCriteria;
import com.tesshu.jpsonic.service.search.UPnPSearchCriteriaDirector;
import com.tesshu.jpsonic.service.upnp.CustomContentDirectory;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.service.upnp.UPnPContentProcessor;
import com.tesshu.jpsonic.service.upnp.UpnpProcessDispatcher;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "rootUpnpProcessor", "mediaFileUpnpProcessor" })
public class DispatchingContentDirectory extends CustomContentDirectory implements UpnpProcessDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchingContentDirectory.class);

    private static final int COUNT_MAX = 50;

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
    private final QueryFactory queryFactory;
    private final UpnpProcessorUtil util;
    private final WMPProcessor wmpProcessor;
    private final SearchService searchService;

    public DispatchingContentDirectory(RootUpnpProcessor rp,
            @Qualifier("mediaFileUpnpProcessor") MediaFileUpnpProcessor mfp, @Lazy PlaylistUpnpProcessor playp,
            @Lazy @Qualifier("albumUpnpProcessor") AlbumUpnpProcessor ap,
            @Lazy @Qualifier("recentAlbumUpnpProcessor") RecentAlbumUpnpProcessor rap,
            @Lazy @Qualifier("recentAlbumId3UpnpProcessor") RecentAlbumId3UpnpProcessor raip,
            @Lazy ArtistUpnpProcessor arP, @Lazy ArtistByFolderUpnpProcessor abfP, @Lazy AlbumByGenreUpnpProcessor abgp,
            @Lazy SongByGenreUpnpProcessor sbgp, @Lazy @Qualifier("indexUpnpProcessor") IndexUpnpProcessor ip,
            @Lazy IndexId3UpnpProcessor iip, @Lazy @Qualifier("podcastUpnpProcessor") PodcastUpnpProcessor podp,
            @Lazy @Qualifier("randomAlbumUpnpProcessor") RandomAlbumUpnpProcessor randomap,
            @Lazy @Qualifier("randomSongUpnpProcessor") RandomSongUpnpProcessor randomsp,
            @Lazy RandomSongByArtistUpnpProcessor randomsbap, @Lazy RandomSongByFolderArtistUpnpProcessor randomsbfap,
            QueryFactory queryFactory, UpnpProcessorUtil util, WMPProcessor wmpp, SearchService ss) {
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
        this.queryFactory = queryFactory;
        this.util = util;
        this.wmpProcessor = wmpp;
        searchService = ss;
    }

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag, String filter, long firstResult,
            final long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {

        if (isEmpty(objectId)) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "objectId is null");
        }

        // maxResult == 0 means all.
        long max = maxResults == 0 ? Long.MAX_VALUE : maxResults;

        String[] splitId = objectId.split(ProcId.CID_SEPA, -1);
        ProcId procId = ProcId.of(splitId[0]);
        String itemId = splitId.length == 1 ? null : splitId[1];

        @SuppressWarnings("rawtypes")
        UPnPContentProcessor processor = findProcessor(procId);
        if (isEmpty(processor)) {
            // if it's null then assume it's a file, and that the id
            // is all that's there.
            itemId = procId.getValue();
            processor = mediaFileProcessor;
        }

        try {
            BrowseResult returnValue;
            if (isEmpty(itemId)) {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseMetadata()
                        : processor.browseRoot(filter, firstResult, max);
            } else {
                returnValue = browseFlag == BrowseFlag.METADATA ? processor.browseDirectChildren(itemId)
                        : processor.browseLeaf(itemId, filter, firstResult, max);
            }
            return returnValue;
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS.getCode(),
                    ContentDirectoryErrorCode.CANNOT_PROCESS.getDescription(), e);
        }
    }

    @Override
    public BrowseResult search(String containerId, String upnpSearchQuery, String filter, long firstResult,
            long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {

        // For known filters, delegation processing
        if (wmpProcessor.isAvailable(filter)) {
            BrowseResult wmpResult = wmpProcessor.getBrowseResult(upnpSearchQuery, filter, maxResults, firstResult);
            if (!isEmpty(wmpResult)) {
                return wmpResult;
            }
        } else if (!isEmpty(filter) && LOG.isInfoEnabled()) {
            LOG.info("An unknown filter was specified. Jpsonic does nothing :{}", filter);
        }

        // General UPnP search
        int offset = (int) firstResult;
        int count = (int) maxResults;
        if ((offset + count) > COUNT_MAX) {
            count = COUNT_MAX - offset;
        }
        UPnPSearchCriteriaDirector director = new UPnPSearchCriteriaDirector(queryFactory, util);
        UPnPSearchCriteria criteria = director.construct(offset, count, upnpSearchQuery);

        if (Artist.class == criteria.getAssignableClass()) {
            return artistProcessor.toBrowseResult(searchService.search(criteria));
        } else if (Album.class == criteria.getAssignableClass()) {
            return albumProcessor.toBrowseResult(searchService.search(criteria));
        } else if (MediaFile.class == criteria.getAssignableClass()) {
            return mediaFileProcessor.toBrowseResult(searchService.search(criteria));
        }

        return new BrowseResult(StringUtils.EMPTY, 0, 0L, 0L);
    }

    @Override
    public UPnPContentProcessor<?, ?> findProcessor(ProcId id) {
        return switch (id) {
        case ROOT -> rootProcessor;
        case PLAYLIST -> playlistProcessor;
        case FOLDER -> mediaFileProcessor;
        case ALBUM -> albumProcessor;
        case RECENT -> recentAlbumProcessor;
        case RECENT_ID3 -> recentAlbumId3Processor;
        case ARTIST -> artistProcessor;
        case ARTIST_BY_FOLDER -> artistByFolderProcessor;
        case ALBUM_BY_GENRE -> albumByGenreProcessor;
        case SONG_BY_GENRE -> songByGenreProcessor;
        case INDEX -> indexProcessor;
        case INDEX_ID3 -> indexId3Processor;
        case PODCAST -> podcastProcessor;
        case RANDOM_ALBUM -> randomAlbumProcessor;
        case RANDOM_SONG -> randomSongProcessor;
        case RANDOM_SONG_BY_ARTIST -> randomSongByArtistProcessor;
        case RANDOM_SONG_BY_FOLDER_ARTIST -> randomSongByFolderArtistProcessor;
        default -> throw new AssertionError(String.format("Unreachable code(%s=%s).", "type", id));
        };
    }
}
