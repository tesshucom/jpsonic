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
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("deprecation")
public class IndexTypeTestCase extends TestCase {

    @Test
    public void testAlbumBoosts() {
        assertEquals(4, IndexType.ALBUM.getBoosts().size());
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNames.ALBUM_READING_HIRAGANA), 2.4F);
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNames.ALBUM_FULL), 2.3F);
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNames.ALBUM), 2.2F);
        assertEquals(IndexType.ALBUM.getBoosts().get(FieldNames.ARTIST_READING), 1.1F);
    }

    @Test
    public void testAlbumFields() {
        assertEquals(6, IndexType.ALBUM.getFields().length);
        assertEquals(0, Arrays.stream(IndexType.ALBUM.getFields())
                .filter(f -> FieldNames.ALBUM.equals(f))
                .filter(f -> FieldNames.ALBUM_FULL.equals(f))
                .filter(f -> FieldNames.ALBUM_READING_HIRAGANA.equals(f))
                .filter(f -> FieldNames.ARTIST.equals(f))
                .filter(f -> FieldNames.ARTIST_READING.equals(f))
                .filter(f -> FieldNames.FOLDER.equals(f)).count());
    }
    
    @Test
    public void testAlbumId3Boosts() {
        assertEquals(4, IndexType.ALBUM_ID3.getBoosts().size());
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNames.ALBUM_READING_HIRAGANA), 2.4F);
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNames.ALBUM_FULL), 2.3F);
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNames.ALBUM), 2.2F);
        assertEquals(IndexType.ALBUM_ID3.getBoosts().get(FieldNames.ARTIST_READING), 1.1F);
    }

    @Test
    public void testAlbumId3Fields() {
        assertEquals(6, IndexType.ALBUM_ID3.getFields().length);
        assertEquals(0, Arrays.stream(IndexType.ALBUM_ID3.getFields())
            .filter(f -> FieldNames.ARTIST.equals(f))
            .filter(f -> FieldNames.ARTIST_READING.equals(f)).count());
    }

    @Test
    public void testArtistBoosts() {
        assertEquals(1, IndexType.ARTIST.getBoosts().size());
        assertEquals(IndexType.ARTIST.getBoosts().get(FieldNames.ARTIST_READING), 1.1F);
    }

    @Test
    public void testArtistFields() {
        assertEquals(3, IndexType.ARTIST.getFields().length);
        assertEquals(0, Arrays.stream(IndexType.ARTIST.getFields())
            .filter(f -> FieldNames.ARTIST.equals(f))
            .filter(f -> FieldNames.ARTIST_READING.equals(f))
            .filter(f -> FieldNames.FOLDER.equals(f)).count());
    }

    @Test
    public void testArtistId3Boosts() {
        assertEquals(1, IndexType.ARTIST_ID3.getBoosts().size());
        assertEquals(IndexType.ARTIST_ID3.getBoosts().get(FieldNames.ARTIST_READING), 1.1F);
    }

    @Test
    public void testArtistId3Fields() {
        assertEquals(2, IndexType.ARTIST_ID3.getFields().length);
        assertEquals(0, Arrays.stream(IndexType.ARTIST_ID3.getFields())
                .filter(f -> FieldNames.ARTIST.equals(f))
                .filter(f -> FieldNames.ARTIST_READING.equals(f)).count());
    }
    
    @Test
    public void testSongBoosts() {
        assertEquals(3, IndexType.SONG.getBoosts().size());
        assertEquals(IndexType.SONG.getBoosts().get(FieldNames.TITLE_READING_HIRAGANA), 2.4F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNames.TITLE), 2.3F);
        assertEquals(IndexType.SONG.getBoosts().get(FieldNames.ARTIST_READING), 1.1F);
    }

    @Test
    public void testSongFields() {
        assertEquals(4, IndexType.SONG.getFields().length);
        assertEquals(0, Arrays.stream(IndexType.SONG.getFields())
                .filter(f -> FieldNames.TITLE.equals(f))
                .filter(f -> FieldNames.TITLE_READING_HIRAGANA.equals(f))
                .filter(f -> FieldNames.ARTIST.equals(f))
                .filter(f -> FieldNames.ARTIST_READING.equals(f)).count());
    }
    
}