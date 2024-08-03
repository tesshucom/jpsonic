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

package com.tesshu.jpsonic.service.upnp.processor.composite;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Genre;

public record GenreAndAlbum(Genre genre, Album album) {

    // Definitely not part of genre name.
    private static final String SEPA = ";";

    public String createCompositeId() {
        return album.getId() + SEPA + genre.getName();
    }

    // TODO to be fixed
    public static boolean isCompositeId(String s) {
        return s.contains(SEPA);
    }

    public static int parseAlbumId(String compositeId) {
        return Integer.parseInt(compositeId.substring(0, compositeId.indexOf(SEPA)));
    }

    public static String parseGenreName(String compositeId) {
        return compositeId.substring(compositeId.indexOf(SEPA) + 1);
    }
}
