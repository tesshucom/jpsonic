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

package com.tesshu.jpsonic.domain;

import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;

public enum MenuItemId {

    ROOT(0, 0),

    FOLDER(10, 1), INDEX(11, 13), MEDIA_FILE_BY_FOLDER(12, 12), MEDIA_FILE(13, 11),

    ARTIST(20, 2), INDEX_ID3(21, 23), ALBUM_ARTIST(22, 21), ALBUM_ARTIST_BY_FOLDER(23, 22),

    ALBUM(30, 3), ALBUM_ID3(31, 31), ALBUM_ID3_BY_FOLDER(32, 32),

    GENRE(40, 4), ALBUM_ID3_BY_GENRE(43, 41), ALBUM_ID3_BY_FOLDER_GENRE(44, 42), SONG_BY_GENRE(42, 43),
    SONG_BY_FOLDER_GENRE(45, 44), ALBUM_BY_GENRE(41, 46), AUDIOBOOK_BY_GENRE(46, 45),

    PODCAST(50, 5), PODCAST_DEFALT(51, 51),

    PLAYLISTS(60, 6), PLAYLISTS_DEFALT(61, 61),

    RECENTLY(70, 7), RECENTLY_ADDED_ALBUM(71, 71), RECENTLY_TAGGED_ALBUM(72, 72),

    SHUFFLE(80, 8), RANDOM_ALBUM(81, 85), RANDOM_SONG(82, 81), RANDOM_SONG_BY_ARTIST(83, 82),
    RANDOM_SONG_BY_FOLDER_ARTIST(84, 83), RANDOM_SONG_BY_GENRE(85, 84),

    YEAR(90, 9),

    AUDIOBOOK(100, 10),

    VIDEO(110, 11),

    RADIO(120, 12),

    BOOKMARK(130, 13),

    // None of these (unreachable)
    ANY(999, 999);

    private final int id;
    private final int defaultOrder;

    MenuItemId(int id, int defaultOrder) {
        this.id = id;
        this.defaultOrder = defaultOrder;
    }

    public int value() {
        return id;
    }

    public int getDefaultOrder() {
        return defaultOrder;
    }

    public static @NonNull MenuItemId of(int value) {
        return Stream.of(values()).filter(id -> id.id == value).findFirst().orElse(ANY);
    }
}
