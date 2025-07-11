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

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.search.QueryFactory;
import com.tesshu.jpsonic.service.search.UPnPSearchCriteria;
import com.tesshu.jpsonic.service.search.UPnPSearchCriteriaDirector;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.SortCriterion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "rootUpnpProc", "mediaFileProc" })
public class DispatchingContentDirectory extends CustomContentDirectory
        implements UpnpProcessDispatcher, CountLimitProc {

    private static final int SEARCH_COUNT_MAX = 50;

    private final RootUpnpProc rootProc;
    private final MediaFileProc mediaFileProc;
    private final MediaFileByFolderProc mediaFileByFolderProc;
    private final PlaylistProc playlistProc;
    private final AlbumId3Proc albumId3Proc;
    private final AlbumId3ByFolderProc albumId3ByFolderProc;
    private final RecentAlbumProc recentAlbumProc;
    private final RecentAlbumByFolderProc recentAlbumByFolderProc;
    private final RecentAlbumId3Proc recentAlbumId3Proc;
    private final RecentAlbumId3ByFolderProc recentAlbumId3ByFolderProc;
    private final ArtistProc artistProc;
    private final ArtistByFolderProc artistByFolderProc;
    private final AlbumByGenreProc albumByGenreProc;
    private final AlbumId3ByGenreProc albumId3ByGenreProc;
    private final AlbumProc albumProc;
    private final AlbumByFolderProc albumByFolderProc;
    private final AlbumId3ByFolderGenreProc albumId3ByFolderGenreProc;
    private final SongByGenreProc songByGenreProc;
    private final SongByFolderGenreProc songByFolderGenreProc;
    private final AudiobookByGenreProc audiobookByGenreProc;
    private final IndexProc indexProc;
    private final IndexId3Proc indexId3Proc;
    private final PodcastProc podcastProc;
    private final RandomAlbumProc randomAlbumProc;
    private final RandomSongProc randomSongProcessor;
    private final RandomSongByArtistProc randomSongByArtistProc;
    private final RandomSongByFolderArtistProc randomSongByFolderArtistProc;
    private final RandomSongByGenreProc randomSongByGenreProc;
    private final RandomSongByFolderGenreProc randomSongByFolderGenreProc;
    private final QueryFactory queryFactory;
    private final UpnpProcessorUtil util;
    private final WMPProc wmpProc;
    private final SearchService searchService;

    public DispatchingContentDirectory(RootUpnpProc rp,
            @Qualifier("mediaFileProc") MediaFileProc mfp,
            @Lazy @Qualifier("mediaFileByFolderProc") MediaFileByFolderProc mfbfp,
            @Lazy PlaylistProc playp, @Lazy @Qualifier("albumId3Proc") AlbumId3Proc aid3p,
            @Lazy @Qualifier("albumId3ByFolderProc") AlbumId3ByFolderProc alid3bfp,
            @Lazy AlbumProc alp, @Lazy AlbumByFolderProc albfp,
            @Lazy @Qualifier("recentAlbumProc") RecentAlbumProc rap,
            @Lazy @Qualifier("recentAlbumByFolderProc") RecentAlbumByFolderProc rabfp,
            @Lazy @Qualifier("recentAlbumId3Proc") RecentAlbumId3Proc raip,
            @Lazy @Qualifier("recentAlbumId3ByFolderProc") RecentAlbumId3ByFolderProc raigfp,
            @Lazy ArtistProc arP, @Lazy ArtistByFolderProc abfP,
            @Lazy @Qualifier("albumByGenreProc") AlbumByGenreProc abgp,
            @Lazy @Qualifier("albumId3ByGenreProc") AlbumId3ByGenreProc aibgp,
            @Lazy @Qualifier("albumId3ByFolderGenreProc") AlbumId3ByFolderGenreProc aibfgp,
            @Lazy @Qualifier("songByGenreProc") SongByGenreProc sbgp,
            @Lazy @Qualifier("songByFolderGenreProc") SongByFolderGenreProc sbfgp,
            @Lazy AudiobookByGenreProc abbgp, @Lazy @Qualifier("indexProc") IndexProc ip,
            @Lazy IndexId3Proc iip, @Lazy @Qualifier("podcastProc") PodcastProc podp,
            @Lazy @Qualifier("randomAlbumProc") RandomAlbumProc randomap,
            @Lazy @Qualifier("randomSongProc") RandomSongProc randomsp,
            @Lazy RandomSongByArtistProc randomsbap, @Lazy RandomSongByFolderArtistProc randomsbfap,
            @Lazy RandomSongByGenreProc rsbgp, @Lazy RandomSongByFolderGenreProc rsbfgp,
            QueryFactory queryFactory, UpnpProcessorUtil util, WMPProc wmpp, SearchService ss) {
        super();
        rootProc = rp;
        mediaFileProc = mfp;
        mediaFileByFolderProc = mfbfp;
        playlistProc = playp;
        albumId3Proc = aid3p;
        albumId3ByFolderProc = alid3bfp;
        albumProc = alp;
        albumByFolderProc = albfp;
        recentAlbumProc = rap;
        recentAlbumByFolderProc = rabfp;
        recentAlbumId3Proc = raip;
        recentAlbumId3ByFolderProc = raigfp;
        artistProc = arP;
        artistByFolderProc = abfP;
        albumByGenreProc = abgp;
        albumId3ByGenreProc = aibgp;
        albumId3ByFolderGenreProc = aibfgp;
        songByGenreProc = sbgp;
        songByFolderGenreProc = sbfgp;
        audiobookByGenreProc = abbgp;
        indexProc = ip;
        indexId3Proc = iip;
        podcastProc = podp;
        randomAlbumProc = randomap;
        randomSongProcessor = randomsp;
        randomSongByArtistProc = randomsbap;
        randomSongByFolderArtistProc = randomsbfap;
        randomSongByGenreProc = rsbgp;
        randomSongByFolderGenreProc = rsbfgp;
        this.queryFactory = queryFactory;
        this.util = util;
        this.wmpProc = wmpp;
        searchService = ss;
    }

    @Override
    public UPnPContentProcessor<?, ?> findProcessor(ProcId id) {
        return switch (id) {
        case ROOT -> rootProc;
        case PLAYLIST -> playlistProc;
        case MEDIA_FILE -> mediaFileProc;
        case MEDIA_FILE_BY_FOLDER -> mediaFileByFolderProc;
        case ALBUM_ID3 -> albumId3Proc;
        case ALBUM_ID3_BY_FOLDER -> albumId3ByFolderProc;
        case ALBUM -> albumProc;
        case ALBUM_BY_FOLDER -> albumByFolderProc;
        case RECENT -> recentAlbumProc;
        case RECENT_BY_FOLDER -> recentAlbumByFolderProc;
        case RECENT_ID3 -> recentAlbumId3Proc;
        case RECENT_ID3_BY_FOLDER -> recentAlbumId3ByFolderProc;
        case ARTIST -> artistProc;
        case ARTIST_BY_FOLDER -> artistByFolderProc;
        case ALBUM_BY_GENRE -> albumByGenreProc;
        case ALBUM_ID3_BY_GENRE -> albumId3ByGenreProc;
        case ALBUM_ID3_BY_FOLDER_GENRE -> albumId3ByFolderGenreProc;
        case SONG_BY_GENRE -> songByGenreProc;
        case SONG_BY_FOLDER_GENRE -> songByFolderGenreProc;
        case AUDIOBOOK_BY_GENRE -> audiobookByGenreProc;
        case INDEX -> indexProc;
        case INDEX_ID3 -> indexId3Proc;
        case PODCAST -> podcastProc;
        case RANDOM_ALBUM -> randomAlbumProc;
        case RANDOM_SONG -> randomSongProcessor;
        case RANDOM_SONG_BY_ARTIST -> randomSongByArtistProc;
        case RANDOM_SONG_BY_FOLDER_ARTIST -> randomSongByFolderArtistProc;
        case RANDOM_SONG_BY_GENRE -> randomSongByGenreProc;
        case RANDOM_SONG_BY_FOLDER_GENRE -> randomSongByFolderGenreProc;
        };
    }

    ProcId getProcId(@NonNull String objectId) {
        int i = objectId.indexOf(ProcId.CID_SEPA);
        return ProcId.of(i == -1 ? objectId : objectId.substring(0, i));
    }

    @Nullable
    String getItemId(@NonNull String objectId) {
        int i = objectId.indexOf(ProcId.CID_SEPA);
        return i == -1 ? null : objectId.substring(i + 1);
    }

    @Override
    public BrowseResult browse(String objectId, BrowseFlag browseFlag, String filter,
            long firstResult, final long maxResults, SortCriterion[] orderBy)
            throws ContentDirectoryException {
        if (isEmpty(objectId)) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS,
                    "objectId is null");
        }

        UPnPContentProcessor<?, ?> processor = findProcessor(getProcId(objectId));
        String itemId = getItemId(objectId);
        long max = maxResults == 0 ? Long.MAX_VALUE : maxResults;
        try {
            if (isEmpty(itemId)) {
                return browseFlag == BrowseFlag.METADATA ? processor.browseMetadata()
                        : processor.browseRoot(filter, firstResult, max);
            }
            return browseFlag == BrowseFlag.METADATA ? processor.browseDirectChildren(itemId)
                    : processor.browseLeaf(itemId, filter, firstResult, max);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS.getCode(),
                    ContentDirectoryErrorCode.CANNOT_PROCESS.getDescription(), e);
        }
    }

    @Override
    public BrowseResult search(String containerId, String upnpSearchQuery, String filter,
            long firstResult, long maxResults, SortCriterion[] orderBy)
            throws ContentDirectoryException {

        // For known filters, delegation processing
        if (wmpProc.isAvailable(filter)) {
            BrowseResult wmpResult = wmpProc
                .getBrowseResult(upnpSearchQuery, filter, maxResults, firstResult);
            if (!isEmpty(wmpResult)) {
                return wmpResult;
            }
        }

        // General UPnP search
        int offset = (int) firstResult;
        int count = toCount(firstResult, maxResults, SEARCH_COUNT_MAX);
        UPnPSearchMethod searchMethod = util.getUPnPSearchMethod();
        List<MusicFolder> folders = util.getGuestFolders();
        UPnPSearchCriteriaDirector director = new UPnPSearchCriteriaDirector(searchMethod, folders,
                queryFactory);
        UPnPSearchCriteria criteria = director.construct(offset, count, upnpSearchQuery);

        return switch (criteria.targetType()) {
        case ARTIST, ALBUM, SONG -> mediaFileProc.toBrowseResult(searchService.search(criteria));
        case ALBUM_ID3 -> albumId3Proc.toBrowseResult(searchService.search(criteria));
        case ARTIST_ID3 -> artistProc.toBrowseResult(searchService.search(criteria));
        case GENRE, ALBUM_ID3_GENRE -> throw new AssertionError("Unreachable code.");
        };
    }
}
