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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.feature.upnp.content.processor;

import com.tesshu.jpsonic.feature.upnp.content.ProcId;
import com.tesshu.jpsonic.feature.upnp.content.SearchFilterProcessor;
import com.tesshu.jpsonic.feature.upnp.content.SearchResultProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPContentProcessor;
import com.tesshu.jpsonic.feature.upnp.content.UPnPContentProcessorResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Central registry of the processors that compose the UPnP ContentDirectory.
 *
 * <p>
 * The registry maintains the mapping between {@link ProcId} values and their
 * corresponding {@link UPnPContentProcessor} implementations. It is a
 * composition point for the ContentDirectory hierarchy rather than part of the
 * processing logic itself.
 * </p>
 *
 * <p>
 * Processor selection is performed through the
 * {@link UPnPContentProcessorResolver} abstraction. The registry is responsible
 * only for resolving the processor capability required by the top-level
 * ContentDirectory entry point and does not participate in the subsequent
 * processing flow. This prevents the registry from becoming a general-purpose
 * mechanism for coupling processors to one another.
 * </p>
 */
@Service
class UPnPContentProcessorRegistry implements UPnPContentProcessorResolver {

    private final RootUPnPProc rootProc;
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
    private final RandomSongProc randomSongProc;
    private final RandomSongByArtistProc randomSongByArtistProc;
    private final RandomSongByFolderArtistProc randomSongByFolderArtistProc;
    private final RandomSongByGenreProc randomSongByGenreProc;
    private final RandomSongByFolderGenreProc randomSongByFolderGenreProc;
    private final SearchFilterProcessor searchFilterProcessor;

    // spotless:off
    UPnPContentProcessorRegistry(
            @Lazy RootUPnPProc rootProc,
            @Lazy @Qualifier("mediaFileProc") MediaFileProc mediaFileProc,
            @Lazy @Qualifier("mediaFileByFolderProc") MediaFileByFolderProc mediaFileByFolderProc,
            @Lazy PlaylistProc playlistProc,
            @Lazy @Qualifier("albumId3Proc") AlbumId3Proc albumId3Proc,
            @Lazy @Qualifier("albumId3ByFolderProc") AlbumId3ByFolderProc albumId3ByFolderProc,
            @Lazy AlbumProc albumProc, @Lazy AlbumByFolderProc albumByFolderProc,
            @Lazy @Qualifier("recentAlbumProc") RecentAlbumProc recentAlbumProc,
            @Lazy @Qualifier("recentAlbumByFolderProc") RecentAlbumByFolderProc recentAlbumByFolderProc,
            @Lazy @Qualifier("recentAlbumId3Proc") RecentAlbumId3Proc recentAlbumId3Proc,
            @Lazy @Qualifier("recentAlbumId3ByFolderProc") RecentAlbumId3ByFolderProc recentAlbumId3ByFolderProc,
            @Lazy ArtistProc artistProc,
            @Lazy ArtistByFolderProc artistByFolderProc,
            @Lazy @Qualifier("albumByGenreProc") AlbumByGenreProc albumByGenreProc,
            @Lazy @Qualifier("albumId3ByGenreProc") AlbumId3ByGenreProc albumId3ByGenreProc,
            @Lazy @Qualifier("albumId3ByFolderGenreProc") AlbumId3ByFolderGenreProc albumId3ByFolderGenreProc,
            @Lazy @Qualifier("songByGenreProc") SongByGenreProc songByGenreProc,
            @Lazy @Qualifier("songByFolderGenreProc") SongByFolderGenreProc songByFolderGenreProc,
            @Lazy AudiobookByGenreProc audiobookByGenreProc,
            @Lazy @Qualifier("indexProc") IndexProc indexProc,
            @Lazy IndexId3Proc indexId3Proc,
            @Lazy @Qualifier("podcastProc") PodcastProc podcastProc,
            @Lazy @Qualifier("randomAlbumProc") RandomAlbumProc randomAlbumProc,
            @Lazy @Qualifier("randomSongProc") RandomSongProc randomSongProc,
            @Lazy RandomSongByArtistProc randomSongByArtistProc,
            @Lazy RandomSongByFolderArtistProc randomSongByFolderArtistProc,
            @Lazy RandomSongByGenreProc randomSongByGenreProc,
            @Lazy RandomSongByFolderGenreProc randomSongByFolderGenreProc,
            SearchFilterProcessor searchFilterProcessor) {
        this.rootProc = rootProc;
        this.mediaFileProc = mediaFileProc;
        this.mediaFileByFolderProc = mediaFileByFolderProc;
        this.playlistProc = playlistProc;
        this.albumId3Proc = albumId3Proc;
        this.albumId3ByFolderProc = albumId3ByFolderProc;
        this.albumProc = albumProc;
        this.albumByFolderProc = albumByFolderProc;
        this.recentAlbumProc = recentAlbumProc;
        this.recentAlbumByFolderProc = recentAlbumByFolderProc;
        this.recentAlbumId3Proc = recentAlbumId3Proc;
        this.recentAlbumId3ByFolderProc = recentAlbumId3ByFolderProc;
        this.artistProc = artistProc;
        this.artistByFolderProc = artistByFolderProc;
        this.albumByGenreProc = albumByGenreProc;
        this.albumId3ByGenreProc = albumId3ByGenreProc;
        this.albumId3ByFolderGenreProc = albumId3ByFolderGenreProc;
        this.songByGenreProc = songByGenreProc;
        this.songByFolderGenreProc = songByFolderGenreProc;
        this.audiobookByGenreProc = audiobookByGenreProc;
        this.indexProc = indexProc;
        this.indexId3Proc = indexId3Proc;
        this.podcastProc = podcastProc;
        this.randomAlbumProc = randomAlbumProc;
        this.randomSongProc = randomSongProc;
        this.randomSongByArtistProc = randomSongByArtistProc;
        this.randomSongByFolderArtistProc = randomSongByFolderArtistProc;
        this.randomSongByGenreProc = randomSongByGenreProc;
        this.randomSongByFolderGenreProc = randomSongByFolderGenreProc;
        this.searchFilterProcessor = searchFilterProcessor;
    }
    // spotless:on

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
        case RANDOM_SONG -> randomSongProc;
        case RANDOM_SONG_BY_ARTIST -> randomSongByArtistProc;
        case RANDOM_SONG_BY_FOLDER_ARTIST -> randomSongByFolderArtistProc;
        case RANDOM_SONG_BY_GENRE -> randomSongByGenreProc;
        case RANDOM_SONG_BY_FOLDER_GENRE -> randomSongByFolderGenreProc;
        };
    }

    @Override
    public SearchResultProcessor<?> findSearchResultProcessor(ProcId id) {
        UPnPContentProcessor<?, ?> processor = findProcessor(id);
        if (processor instanceof SearchResultProcessor) {
            return (SearchResultProcessor<?>) processor;
        }
        throw new IllegalArgumentException(
                "Processor does not support search result conversion: " + id);
    }

    @Override
    public SearchFilterProcessor findSearchFilterProcessor() {
        return searchFilterProcessor;
    }
}
