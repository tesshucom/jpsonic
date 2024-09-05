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

import com.tesshu.jpsonic.domain.MenuItemId;

/**
 * ID to identify UPnPContentProcessor.
 */
public enum ProcId {

    ROOT("0"), PLAYLIST("playlist"), MEDIA_FILE("mediaFile"), MEDIA_FILE_BY_FOLDER("folder"), ALBUM_ID3("alid3"),
    ALBUM_ID3_BY_FOLDER("alid3bf"), ALBUM("al"), ALBUM_BY_FOLDER("albf"), ARTIST("artist"),
    ARTIST_BY_FOLDER("artistByFolder"), ALBUM_BY_GENRE("abg"), ALBUM_ID3_BY_GENRE("aibg"),
    ALBUM_ID3_BY_FOLDER_GENRE("aibfg"), SONG_BY_GENRE("sbg"), SONG_BY_FOLDER_GENRE("sbfg"), AUDIOBOOK_BY_GENRE("abbg"),
    RECENT("recent"), RECENT_ID3("recentId3"), INDEX("index"), INDEX_ID3("indexId3"), PODCAST("podcast"),
    RANDOM_ALBUM("ral"), RANDOM_SONG("rs"), RANDOM_SONG_BY_ARTIST("rsbar"), RANDOM_SONG_BY_FOLDER_ARTIST("rsbfar"),
    RANDOM_SONG_BY_GENRE("rsbg"), RANDOM_SONG_BY_FOLDER_GENRE("rsbfg");

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

    public static ProcId from(MenuItemId menuItemId) {
        return switch (menuItemId) {
        case INDEX -> INDEX;
        case MEDIA_FILE -> MEDIA_FILE;
        case MEDIA_FILE_BY_FOLDER -> MEDIA_FILE_BY_FOLDER;
        case INDEX_ID3 -> INDEX_ID3;
        case ALBUM_ARTIST -> ARTIST;
        case ALBUM_ARTIST_BY_FOLDER -> ARTIST_BY_FOLDER;
        case ALBUM_ID3 -> ALBUM_ID3;
        case ALBUM_ID3_BY_FOLDER -> ALBUM_ID3_BY_FOLDER;
        case ALBUM_FILE_STRUCTURE -> ALBUM;
        case ALBUM_FILE_STRUCTURE_BY_FOLDER -> ALBUM_BY_FOLDER;
        case ALBUM_ID3_BY_GENRE -> ALBUM_ID3_BY_GENRE;
        case ALBUM_ID3_BY_FOLDER_GENRE -> ALBUM_ID3_BY_FOLDER_GENRE;
        case SONG_BY_GENRE -> SONG_BY_GENRE;
        case SONG_BY_FOLDER_GENRE -> SONG_BY_FOLDER_GENRE;
        case AUDIOBOOK_BY_GENRE -> AUDIOBOOK_BY_GENRE;
        case ALBUM_BY_GENRE -> ALBUM_BY_GENRE;
        case PODCAST_DEFALT -> PODCAST;
        case PLAYLISTS_DEFALT -> PLAYLIST;
        case RECENTLY_ADDED_ALBUM -> RECENT;
        case RECENTLY_TAGGED_ALBUM -> RECENT_ID3;
        case RANDOM_SONG -> RANDOM_SONG;
        case RANDOM_SONG_BY_ARTIST -> RANDOM_SONG_BY_ARTIST;
        case RANDOM_SONG_BY_FOLDER_ARTIST -> RANDOM_SONG_BY_FOLDER_ARTIST;
        case RANDOM_SONG_BY_GENRE -> RANDOM_SONG_BY_GENRE;
        case RANDOM_SONG_BY_FOLDER_GENRE -> RANDOM_SONG_BY_FOLDER_GENRE;
        case RANDOM_ALBUM -> RANDOM_ALBUM;
        default -> throw new IllegalArgumentException("Unexpected value: " + menuItemId);
        };
    }
}
