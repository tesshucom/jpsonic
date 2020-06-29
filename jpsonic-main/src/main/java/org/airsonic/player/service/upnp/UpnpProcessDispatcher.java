package org.airsonic.player.service.upnp;

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

public interface UpnpProcessDispatcher {

    final String OBJECT_ID_SEPARATOR = "/";

    final String CONTAINER_ID_ROOT = "0";
    final String CONTAINER_ID_PLAYLIST_PREFIX = "playlist";
    final String CONTAINER_ID_FOLDER_PREFIX = "folder";
    final String CONTAINER_ID_ALBUM_PREFIX = "album";
    final String CONTAINER_ID_ARTIST_PREFIX = "artist";
    final String CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX = "artistByFolder";
    final String CONTAINER_ID_ARTISTALBUM_PREFIX = "artistalbum";
    final String CONTAINER_ID_ALBUM_BY_GENRE_PREFIX = "abg";
    final String CONTAINER_ID_SONG_BY_GENRE_PREFIX = "sbg";
    final String CONTAINER_ID_RECENT_PREFIX = "recent";
    final String CONTAINER_ID_RECENT_ID3_PREFIX = "recentId3";
    final String CONTAINER_ID_INDEX_PREFIX = "index";
    final String CONTAINER_ID_INDEX_ID3_PREFIX = "indexId3";
    final String CONTAINER_ID_PODCAST_PREFIX = "podcast";
    final String CONTAINER_ID_RANDOM_ALBUM = "randomAlbum";
    final String CONTAINER_ID_RANDOM_SONG = "randomSong";
    final String CONTAINER_ID_RANDOM_SONG_BY_ARTIST = "randomSongByArtist";
    final String CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST = "randomSongByFolderArtist";

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
            case CONTAINER_ID_ARTIST_BY_FOLDER_PREFIX:
                return getArtistByFolderProcessor();
            case CONTAINER_ID_ALBUM_BY_GENRE_PREFIX:
                return getAlbumByGenreProcessor();
            case CONTAINER_ID_SONG_BY_GENRE_PREFIX:
                return getSongByGenreProcessor();
            case CONTAINER_ID_INDEX_PREFIX:
                return getIndexProcessor();
            case CONTAINER_ID_INDEX_ID3_PREFIX:
                return getIndexId3Processor();
            case CONTAINER_ID_PODCAST_PREFIX:
                return getPodcastProcessor();
            case CONTAINER_ID_RANDOM_ALBUM:
                return getRandomAlbumProcessor();
            case CONTAINER_ID_RANDOM_SONG:
                return getRandomSongProcessor();
            case CONTAINER_ID_RANDOM_SONG_BY_ARTIST:
                return getRandomSongByArtistProcessor();
            case CONTAINER_ID_RANDOM_SONG_BY_FOLDER_ARTIST:
                return getRandomSongByFolderArtistProcessor();
            default:
                throw new AssertionError(String.format("Unreachable code(%s=%s).", "type", type));
        }
    }

}