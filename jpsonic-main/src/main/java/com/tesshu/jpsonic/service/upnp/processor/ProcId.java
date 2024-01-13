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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.stream.Stream;

/**
 * ID to identify UPnPContentProcessor.
 */
public enum ProcId {

    ROOT("0"), PLAYLIST("playlist"), FOLDER("folder"), ALBUM("album"), ARTIST("artist"),
    ARTIST_BY_FOLDER("artistByFolder"), ALBUM_BY_GENRE("abg"), SONG_BY_GENRE("sbg"), RECENT("recent"),
    RECENT_ID3("recentId3"), INDEX("index"), INDEX_ID3("indexId3"), PODCAST("podcast"), RANDOM_ALBUM("randomAlbum"),
    RANDOM_SONG("randomSong"), RANDOM_SONG_BY_ARTIST("randomSongByArtist"),
    RANDOM_SONG_BY_FOLDER_ARTIST("randomSongByFolderArtist");

    /**
     * Separator used for Compound IDs (CID).
     */
    public static final String CID_SEPA = "/";

    private final String id;

    ProcId(String id) {
        this.id = id;
    }

    public String getValue() {
        return id;
    }

    public static ProcId of(String s) {
        return Stream.of(values()).filter(id -> id.getValue().equals(s)).findFirst().get();
    }
}
