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
import com.tesshu.jpsonic.domain.MusicFolder;

public record FolderGenreAlbum(MusicFolder folder, Genre genre, Album album) implements CompositeModel {

    private static final String TYPE_PREFIX = "fgal:";
    private static final String SEPA = ";"; // Definitely not part of genre name.

    @Override
    public String createCompositeId() {
        return TYPE_PREFIX + folder.getId() + SEPA + album.getId() + SEPA + genre.getName();
    }

    public static boolean isCompositeId(String s) {
        return s.startsWith(TYPE_PREFIX);
    }

    public static int parseFolderId(String compositeId) {
        return Integer.parseInt(compositeId.substring(TYPE_PREFIX.length(), compositeId.indexOf(SEPA)));
    }

    public static int parseAlbumId(String compositeId) {
        int first = compositeId.indexOf(SEPA);
        int end = compositeId.indexOf(SEPA, first + SEPA.length());
        return Integer.parseInt(compositeId.substring(first + SEPA.length(), end));
    }

    public static String parseGenreName(String compositeId) {
        return compositeId.substring(compositeId.lastIndexOf(SEPA) + SEPA.length());
    }
}
