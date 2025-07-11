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

package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastChannel;

/**
 * A class that the key generation logic of CoverArt. In presentations where
 * cover art is handled, id/type and key conversion may be performed to ensure
 * uniqueness. This interface is used from the controller or service. And the
 * key generated is not stored in the persistence layer.
 */
public interface CoverArtPresentation {

    String ALBUM_COVERART_PREFIX = "al-";
    String ARTIST_COVERART_PREFIX = "ar-";
    String PLAYLIST_COVERART_PREFIX = "pl-";
    String PODCAST_COVERART_PREFIX = "pod-";

    default boolean isAlbumCoverArt(String coverArtKey) {
        return coverArtKey.startsWith(ALBUM_COVERART_PREFIX);
    }

    default boolean isArtistCoverArt(String coverArtKey) {
        return coverArtKey.startsWith(ARTIST_COVERART_PREFIX);
    }

    default boolean isPlaylistCoverArt(String coverArtKey) {
        return coverArtKey.startsWith(PLAYLIST_COVERART_PREFIX);
    }

    default boolean isPodcastCoverArt(String coverArtKey) {
        return coverArtKey.startsWith(PODCAST_COVERART_PREFIX);
    }

    default int toAlbumId(String coverArtKey) {
        return Integer.parseInt(coverArtKey.replace(ALBUM_COVERART_PREFIX, ""));
    }

    default int toArtistId(String coverArtKey) {
        return Integer.parseInt(coverArtKey.replace(ARTIST_COVERART_PREFIX, ""));
    }

    default int toPlaylistId(String coverArtKey) {
        return Integer.parseInt(coverArtKey.replace(PLAYLIST_COVERART_PREFIX, ""));
    }

    default int toPodcastId(String coverArtKey) {
        return Integer.parseInt(coverArtKey.replace(PODCAST_COVERART_PREFIX, ""));
    }

    default String createCoverArtKey(Artist artist) {
        return ARTIST_COVERART_PREFIX + artist.getId();
    }

    default String createCoverArtKey(Album album) {
        return ALBUM_COVERART_PREFIX + album.getId();
    }

    default String createCoverArtKey(Playlist playlist) {
        return PLAYLIST_COVERART_PREFIX + playlist.getId();
    }

    default String createCoverArtKey(PodcastChannel channel) {
        return PODCAST_COVERART_PREFIX + channel.getId();
    }
}
