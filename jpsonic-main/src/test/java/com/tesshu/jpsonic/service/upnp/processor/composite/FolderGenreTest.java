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

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FolderGenreTest {

    @Test
    void testCreateCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        assertEquals("fg:99;GENRE", folderGenre.createCompositeId());
    }

    @Test
    void testIsCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        assertFalse(GenreAlbum.isCompositeId(Integer.toString(folder.getId())));
        assertFalse(GenreAlbum.isCompositeId(genre.getName()));
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        assertTrue(FolderGenre.isCompositeId(folderGenre.createCompositeId()));
    }

    @Test
    void testParseFolderId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        assertEquals(99, FolderGenre.parseFolderId(folderGenre.createCompositeId()));
    }

    @Test
    void testParseGenreName() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        FolderGenre folderGenre = new FolderGenre(folder, genre);
        assertEquals("GENRE", FolderGenre.parseGenreName(folderGenre.createCompositeId()));
    }
}
