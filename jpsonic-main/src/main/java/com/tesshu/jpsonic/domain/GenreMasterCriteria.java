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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.domain;

import java.util.List;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.MediaFile.MediaType;

/**
 * Criteria used when generating Genre Master.
 */
public record GenreMasterCriteria(List<MusicFolder> folders, Scope scope, Sort sort, MediaType... types) {

    public enum Scope {
        ALBUM, SONG;

        public static Scope of(String s) {
            return Stream.of(values()).filter(scope -> scope.name().equals(s)).findFirst().orElse(ALBUM);
        }
    }

    public enum Sort {
        FREQUENCY, NAME, ALBUM_COUNT, SONG_COUNT;

        public static Sort of(String s) {
            return Stream.of(values()).filter(sort -> sort.name().equals(s)).findFirst().orElse(FREQUENCY);
        }
    }
}
