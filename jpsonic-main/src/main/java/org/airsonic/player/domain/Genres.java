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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a list of genres.
 *
 * @author Sindre Mehus
 */
public class Genres {

    private final Map<String, Genre> genreMap;

    public Genres() {
        super();
        genreMap = new ConcurrentHashMap<>();
    }

    public void incrementAlbumCount(String genreName) {
        Genre genre = getOrCreateGenre(genreName);
        genre.incrementAlbumCount();
    }

    public void incrementSongCount(String genreName) {
        Genre genre = getOrCreateGenre(genreName);
        genre.incrementSongCount();
    }

    private Genre getOrCreateGenre(String genreName) {
        Genre genre = genreMap.get(genreName);
        if (genre == null) {
            genre = new Genre(genreName);
            genreMap.put(genreName, genre);
        }
        return genre;
    }

    public List<Genre> getGenres() {
        return new ArrayList<>(genreMap.values());
    }
}
