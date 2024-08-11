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
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FolderGenreAlbumTest {

    @Test
    void testCreateCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(88);
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        assertEquals("fgal:99;88;GENRE", folderGenreAlbum.createCompositeId());
    }

    @Test
    void testIsCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(88);
        assertFalse(FolderGenreAlbum.isCompositeId(Integer.toString(folder.getId())));
        assertFalse(FolderGenreAlbum.isCompositeId(genre.getName()));
        assertFalse(FolderGenreAlbum.isCompositeId(Integer.toString(album.getId())));
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        assertTrue(FolderGenreAlbum.isCompositeId(folderGenreAlbum.createCompositeId()));
    }

    @Test
    void testParseFolderId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(88);
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        assertEquals(99, FolderGenreAlbum.parseFolderId(folderGenreAlbum.createCompositeId()));
    }

    @Test
    void testParseAlbumId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(88);
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        assertEquals(88, FolderGenreAlbum.parseAlbumId(folderGenreAlbum.createCompositeId()));
    }

    @Test
    void testParseGenreName() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Genre genre = new Genre("GENRE", 0, 0);
        Album album = new Album();
        album.setId(88);
        FolderGenreAlbum folderGenreAlbum = new FolderGenreAlbum(folder, genre, album);
        assertEquals("GENRE", FolderGenreAlbum.parseGenreName(folderGenreAlbum.createCompositeId()));
    }
}
