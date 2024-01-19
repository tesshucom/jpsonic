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

    ROOT(0),

    FOLDER(10), INDEX(11), MEDIA_FILE(12),

    ARTIST(20), INDEX_ID3(21), ALBUM_ARTIST(22), ALBUM_ARTIST_BY_FOLDER(23),

    ALBUM(30), ALBUM_ID3(31),

    GENRE(40), ALBUM_BY_GENRE(41), SONG_BY_GENRE(42),

    PODCAST(50), PODCAST_DEFALT(51),

    PLAYLISTS(60), PLAYLISTS_DEFALT(61),

    RECENTLY(70), RECENTLY_ADDED_ALBUM(71), RECENTLY_TAGGED_ALBUM(72),

    SHUFFLE(80), RANDOM_ALBUM(81), RANDOM_SONG(82), RANDOM_SONG_BY_ARTIST(83), RANDOM_SONG_BY_FOLDER_ARTIST(84),

    YEAR(90),

    AUDIOBOOK(100),

    VIDEO(110),

    RADIO(120),

    BOOKMARK(130);

    private final int v;

    MenuItemId(int v) {
        this.v = v;
    }

    public int value() {
        return v;
    }

    public static @NonNull MenuItemId of(int value) {
        return Stream.of(values()).filter(id -> id.v == value).findFirst().orElse(ROOT);
    }
}
