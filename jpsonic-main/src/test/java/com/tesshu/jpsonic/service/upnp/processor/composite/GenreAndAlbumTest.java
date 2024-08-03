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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Genre;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GenreAndAlbumTest {

    @Test
    void testCreateCompositeId() {
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(99);
        GenreAndAlbum genreAndAlbum = new GenreAndAlbum(genre, album);
        assertEquals("99;GENRE", genreAndAlbum.createCompositeId());
    }

    @Test
    void testIsCompositeId() {
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(99);
        assertFalse(GenreAndAlbum.isCompositeId(genre.getName()));
        assertFalse(GenreAndAlbum.isCompositeId(Integer.toString(album.getId())));
        GenreAndAlbum genreAndAlbum = new GenreAndAlbum(genre, album);
        assertTrue(GenreAndAlbum.isCompositeId(genreAndAlbum.createCompositeId()));
    }

    @Test
    void testParseAlbumId() {
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(99);
        GenreAndAlbum genreAndAlbum = new GenreAndAlbum(genre, album);
        assertEquals(99, GenreAndAlbum.parseAlbumId(genreAndAlbum.createCompositeId()));
    }

    @Test
    void testParseGenreName() {
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(99);
        GenreAndAlbum genreAndAlbum = new GenreAndAlbum(genre, album);
        assertEquals("GENRE", GenreAndAlbum.parseGenreName(genreAndAlbum.createCompositeId()));
    }
}
