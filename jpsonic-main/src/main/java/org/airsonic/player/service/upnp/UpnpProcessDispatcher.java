package org.airsonic.player.service.upnp;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.upnp.processor.AlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.ArtistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.GenreUpnpProcessor;
import org.airsonic.player.service.upnp.processor.IndexUpnpProcessor;
import org.airsonic.player.service.upnp.processor.MediaFileUpnpProcessor;
import org.airsonic.player.service.upnp.processor.PlaylistUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumId3UpnpProcessor;
import org.airsonic.player.service.upnp.processor.RecentAlbumUpnpProcessor;
import org.airsonic.player.service.upnp.processor.RootUpnpProcessor;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.item.Item;

public interface UpnpProcessDispatcher {

    String OBJECT_ID_SEPARATOR = "/";

    /* Under deliberation.. @see CustomContentDirectory.CONTAINER_ID_ROOT */
    String CONTAINER_ID_ROOT = "0";

    String CONTAINER_ID_PLAYLIST_PREFIX = "playlist";
    String CONTAINER_ID_FOLDER_PREFIX = "folder";
    String CONTAINER_ID_ALBUM_PREFIX = "album";
    String CONTAINER_ID_ARTIST_PREFIX = "artist";
    String CONTAINER_ID_ARTISTALBUM_PREFIX = "artistalbum";
    String CONTAINER_ID_GENRE_PREFIX = "genre";
    String CONTAINER_ID_RECENT_PREFIX = "recent";
    String CONTAINER_ID_RECENT_ID3_PREFIX = "recentId3";
    String CONTAINER_ID_INDEX_PREFIX = "index";

    PlaylistUpnpProcessor getPlaylistProcessor();

    MediaFileUpnpProcessor getMediaFileProcessor();

    AlbumUpnpProcessor getAlbumProcessor();

    RecentAlbumUpnpProcessor getRecentAlbumProcessor();

    RecentAlbumId3UpnpProcessor getRecentAlbumId3Processor();

    ArtistUpnpProcessor getArtistProcessor();

    GenreUpnpProcessor getGenreProcessor();

    RootUpnpProcessor getRootProcessor();

    IndexUpnpProcessor getIndexProcessor();

    /* Under deliberation.. for AlbumUpnpProcessor */
    String getBaseUrl();

    /* Under deliberation.. for MediaFileUpnpProcessor */
    Res createResourceForSong(MediaFile song);

    /* Under deliberation.. for PlaylistUpnpProcessor */
    Item createItem(MediaFile song);

}