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

package com.tesshu.jpsonic.service.search;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SearchServiceGenreMasterTest extends AbstractNeedsScan {

    @Autowired
    private SearchService searchService;

    private final List<MusicFolder> musicFolders = Arrays
        .asList(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true, now(), 1,
                false));

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @BeforeEach
    void setup() {
        populateDatabaseOnlyOnce();
    }

    /*
     * @see IndexManagerTest$CreateGenreMasterTest
     */
    @Test
    void testGet() {
        GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                Sort.NAME);
        List<Genre> genres = searchService.getGenres(criteria, 0, Integer.MAX_VALUE);
        assertEquals(14, genres.size());
        assertEquals("Audiobook - Historical", genres.get(0).getName());
        assertEquals("Audiobook - Sports", genres.get(1).getName());
        assertEquals("GENRE_A", genres.get(2).getName());
        assertEquals("GENRE_B", genres.get(3).getName());
        assertEquals("GENRE_C", genres.get(4).getName());
        assertEquals("GENRE_D", genres.get(5).getName());
        assertEquals("GENRE_E", genres.get(6).getName());
        assertEquals("GENRE_F", genres.get(7).getName());
        assertEquals("GENRE_G", genres.get(8).getName());
        assertEquals("GENRE_H", genres.get(9).getName());
        assertEquals("GENRE_I", genres.get(10).getName());
        assertEquals("GENRE_J", genres.get(11).getName());
        assertEquals("GENRE_K", genres.get(12).getName());
        assertEquals("GENRE_L", genres.get(13).getName());
        assertEquals(genres.size(), searchService.getGenresCount(criteria));
    }

    @Test
    void testOffsetCount() {

        GenreMasterCriteria criteria = new GenreMasterCriteria(musicFolders, Scope.ALBUM,
                Sort.NAME);
        List<Genre> genres = searchService.getGenres(criteria, 0, 3);
        assertEquals(3, genres.size());
        assertEquals("Audiobook - Historical", genres.get(0).getName());
        assertEquals("Audiobook - Sports", genres.get(1).getName());
        assertEquals("GENRE_A", genres.get(2).getName());

        genres = searchService.getGenres(criteria, 3, 6);
        assertEquals(6, genres.size());
        assertEquals("GENRE_B", genres.get(0).getName());
        assertEquals("GENRE_C", genres.get(1).getName());
        assertEquals("GENRE_D", genres.get(2).getName());
        assertEquals("GENRE_E", genres.get(3).getName());
        assertEquals("GENRE_F", genres.get(4).getName());
        assertEquals("GENRE_G", genres.get(5).getName());

        genres = searchService.getGenres(criteria, 9, 6);
        assertEquals(5, genres.size());
        assertEquals("GENRE_H", genres.get(0).getName());
        assertEquals("GENRE_I", genres.get(1).getName());
        assertEquals("GENRE_J", genres.get(2).getName());
        assertEquals("GENRE_K", genres.get(3).getName());
        assertEquals("GENRE_L", genres.get(4).getName());
    }
}
