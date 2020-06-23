/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.search;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class IndexTypeTestCase extends TestCase {

    @Test
    public void testAlbumBoosts() {
        assertEquals(3, IndexType.ALBUM.getBoosts().size());
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ALBUM), 2.3F);
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ALBUM_EX), 2.3F);
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNamesConstants.ARTIST_READING), 1.1F);
    }

    @Test
    public void testAlbumFields() {
        assertEquals(5, IndexType.ALBUM.getFields().length);
    }

    @Test
    public void testAlbumId3Boosts() {
        assertEquals(3, IndexType.ALBUM_ID3.getBoosts().size());
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ALBUM), 2.3F);
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ALBUM_EX), 2.3F);
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING), 1.1F);
    }

    @Test
    public void testAlbumId3Fields() {
        assertEquals(5, IndexType.ALBUM_ID3.getFields().length);
    }

    @Test
    public void testArtistBoosts() {
        assertEquals(1, IndexType.ARTIST.getBoosts().size());
    }

    @Test
    public void testArtistFields() {
        assertEquals(3, IndexType.ARTIST.getFields().length);
    }

    @Test
    public void testArtistId3Boosts() {
        assertEquals(1, IndexType.ARTIST_ID3.getBoosts().size());
        assertEquals(IndexType.ARTIST_ID3.getBoosts().get(FieldNamesConstants.ARTIST_READING), 1.1F);
    }

    @Test
    public void testArtistId3Fields() {
        assertEquals(3, IndexType.ARTIST_ID3.getFields().length);
    }

    @Test
    public void testGenreBoosts() {
        assertEquals(1, IndexType.GENRE.getBoosts().size());
        assertEquals(IndexType.GENRE.getBoosts().get(FieldNamesConstants.GENRE_KEY), 1.1F);
    }

    @Test
    public void testGenreFields() {
        assertEquals(2, IndexType.GENRE.getFields().length);
    }

    @Test
    public void testSongBoosts() {
        assertEquals(6, IndexType.SONG.getBoosts().size());
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.TITLE_EX), 2.3F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.TITLE), 2.2F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_READING), 1.4F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST_EX), 1.3F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.ARTIST), 1.2F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNamesConstants.COMPOSER_READING), 1.1F);
    }

    @Test
    public void testSongFields() {
        assertEquals(7, IndexType.SONG.getFields().length);
    }

}