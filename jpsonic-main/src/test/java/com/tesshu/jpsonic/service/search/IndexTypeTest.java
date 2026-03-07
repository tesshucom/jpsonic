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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexTypeTest {

    @Order(1)
    @Test
    void testLength() {
        for (IndexType type : IndexType.values()) {
            assertEquals(type.getFields().size(), type.getBoosts().size() + 1, type.name());
        }
    }

    @Order(2)
    @Test
    void testSongBoosts() {
        assertEquals(7, IndexType.SONG.getBoosts().size());
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.COMPOSER_READING_ROMANIZED),
                IndexType.SONG.getBoosts().get(FieldNamesConstants.COMPOSER_READING));
        assertTrue(IndexType.SONG
            .getBoosts()
            .get(FieldNamesConstants.COMPOSER_READING) < IndexType.SONG
                .getBoosts()
                .get(FieldNamesConstants.ARTIST));
        assertTrue(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST) < IndexType.SONG
            .getBoosts()
            .get(FieldNamesConstants.ARTIST_READING));
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING),
                IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED));
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED),
                IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING));
        assertTrue(
                IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING) < IndexType.SONG
                    .getBoosts()
                    .get(FieldNamesConstants.TITLE));
        assertTrue(IndexType.SONG.getBoosts().get(FieldNamesConstants.TITLE) < IndexType.SONG
            .getBoosts()
            .get(FieldNamesConstants.TITLE_READING));
    }

    @Order(3)
    @Test
    void testAlbumBoosts() {
        assertEquals(4, IndexType.ALBUM.getBoosts().size());
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ARTIST_READING),
                IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED));
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED),
                IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ARTIST_READING));
        assertTrue(IndexType.ALBUM
            .getBoosts()
            .get(FieldNamesConstants.ARTIST_READING) < IndexType.ALBUM
                .getBoosts()
                .get(FieldNamesConstants.ALBUM));
        assertTrue(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ALBUM) < IndexType.ALBUM
            .getBoosts()
            .get(FieldNamesConstants.ALBUM_READING));
    }

    @Order(4)
    @Test
    void testAlbumId3Boosts() {
        assertEquals(4, IndexType.ALBUM_ID3.getBoosts().size());
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING),
                IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED));
        assertEquals(
                IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED),
                IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING));
        assertTrue(IndexType.ALBUM_ID3
            .getBoosts()
            .get(FieldNamesConstants.ARTIST_READING) < IndexType.ALBUM_ID3
                .getBoosts()
                .get(FieldNamesConstants.ALBUM));
        assertTrue(
                IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ALBUM) < IndexType.ALBUM_ID3
                    .getBoosts()
                    .get(FieldNamesConstants.ALBUM_READING));
    }

    @Order(5)
    @Test
    void testArtistBoosts() {
        assertEquals(2, IndexType.ARTIST.getBoosts().size());
        assertEquals(IndexType.ARTIST.getBoosts().get(FieldNamesConstants.ARTIST_READING),
                IndexType.ARTIST.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED));
    }

    @Order(6)
    @Test
    void testArtistId3Boosts() {
        assertEquals(2, IndexType.ARTIST_ID3.getBoosts().size());
        assertEquals(
                IndexType.ARTIST_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING_ROMANIZED),
                IndexType.ARTIST_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING));
    }

    @Order(7)
    @Test
    void testGenreBoosts() {
        assertEquals(1, IndexType.GENRE.getBoosts().size());
        assertEquals(IndexType.GENRE.getBoosts().get(FieldNamesConstants.GENRE_KEY), 1.1F);
    }
}
