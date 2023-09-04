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

package com.tesshu.jpsonic.service.upnp;

import com.tesshu.jpsonic.service.upnp.processor.AlbumByGenreUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.AlbumUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.ArtistByFolderUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.ArtistUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.IndexId3UpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.IndexUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.MediaFileUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.PlaylistUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.PodcastUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RandomAlbumUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RandomSongByArtistUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RandomSongByFolderArtistUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RandomSongUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RecentAlbumId3UpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RecentAlbumUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.RootUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.SongByGenreUpnpProcessor;
import com.tesshu.jpsonic.service.upnp.processor.UpnpContentProcessor;

public interface UpnpProcessDispatcher {

    String OBJECT_ID_SEPARATOR = "/";
    String CONTAINER_ID_ROOT = "0";
    String CONTAINER_ID_PLAYLIST_PREFIX = "playlist";
    String CONTAINER_ID_FOLDER_PREFIX = "folder";
    String CONTAINER_ID_ALBUM_PREFIX = "album";
    String CONTAINER_ID_ARTIST_PREFIX = "artist";
    String CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX = "artistByFolder";
    String CONTAINER_ID_ARTISTALBUM_PREFIX = "artistalbum";
    String CONTAINER_ID_ALBUM_BY_GENRE_PREFIX = "abg";
    String CONTAINER_ID_SONG_BY_GENRE_PREFIX = "sbg";
    String CONTAINER_ID_RECENT_PREFIX = "recent";
    String CONTAINER_ID_RECENT_ID3_PREFIX = "recentId3";
    String CONTAINER_ID_INDEX_PREFIX = "index";
    String CONTAINER_ID_INDEX_ID3_PREFIX = "indexId3";
    String CONTAINER_ID_PODCAST_PREFIX = "podcast";
    String CONTAINER_ID_RANDOM_ALBUM = "randomAlbum";
    String CONTAINER_ID_RANDOM_SONG = "randomSong";
    String CONTAINER_ID_RANDOM_SONG_BY_ARTIST = "randomSongByArtist";
    String CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST = "randomSongByFolderArtist";

    RootUpnpProcessor getRootProcessor();

    PlaylistUpnpProcessor getPlaylistProcessor();

    MediaFileUpnpProcessor getMediaFileProcessor();

    AlbumUpnpProcessor getAlbumProcessor();

    RecentAlbumUpnpProcessor getRecentAlbumProcessor();

    RecentAlbumId3UpnpProcessor getRecentAlbumId3Processor();

    ArtistUpnpProcessor getArtistProcessor();

    ArtistByFolderUpnpProcessor getArtistByFolderProcessor();

    AlbumByGenreUpnpProcessor getAlbumByGenreProcessor();

    SongByGenreUpnpProcessor getSongByGenreProcessor();

    IndexUpnpProcessor getIndexProcessor();

    IndexId3UpnpProcessor getIndexId3Processor();

    PodcastUpnpProcessor getPodcastProcessor();

    RandomAlbumUpnpProcessor getRandomAlbumProcessor();

    RandomSongUpnpProcessor getRandomSongProcessor();

    RandomSongByArtistUpnpProcessor getRandomSongByArtistProcessor();

    RandomSongByFolderArtistUpnpProcessor getRandomSongByFolderArtistProcessor();

    @SuppressWarnings("rawtypes")
    default UpnpContentProcessor findProcessor(String type) {
        return switch (type) {
        case CONTAINER_ID_ROOT -> getRootProcessor();
        case CONTAINER_ID_PLAYLIST_PREFIX -> getPlaylistProcessor();
        case CONTAINER_ID_FOLDER_PREFIX -> getMediaFileProcessor();
        case CONTAINER_ID_ALBUM_PREFIX -> getAlbumProcessor();
        case CONTAINER_ID_RECENT_PREFIX -> getRecentAlbumProcessor();
        case CONTAINER_ID_RECENT_ID3_PREFIX -> getRecentAlbumId3Processor();
        case CONTAINER_ID_ARTIST_PREFIX -> getArtistProcessor();
        case CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX -> getArtistByFolderProcessor();
        case CONTAINER_ID_ALBUM_BY_GENRE_PREFIX -> getAlbumByGenreProcessor();
        case CONTAINER_ID_SONG_BY_GENRE_PREFIX -> getSongByGenreProcessor();
        case CONTAINER_ID_INDEX_PREFIX -> getIndexProcessor();
        case CONTAINER_ID_INDEX_ID3_PREFIX -> getIndexId3Processor();
        case CONTAINER_ID_PODCAST_PREFIX -> getPodcastProcessor();
        case CONTAINER_ID_RANDOM_ALBUM -> getRandomAlbumProcessor();
        case CONTAINER_ID_RANDOM_SONG -> getRandomSongProcessor();
        case CONTAINER_ID_RANDOM_SONG_BY_ARTIST -> getRandomSongByArtistProcessor();
        case CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST -> getRandomSongByFolderArtistProcessor();
        default -> throw new AssertionError(String.format("Unreachable code(%s=%s).", "type", type));
        };
    }
}
