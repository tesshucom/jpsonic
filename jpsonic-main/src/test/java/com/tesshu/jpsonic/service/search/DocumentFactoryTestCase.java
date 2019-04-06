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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import java.io.File;
import java.util.Date;

import org.airsonic.player.dao.MusicFolderTestData;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.apache.lucene.document.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/applicationContext-jpsonic.xml" })
public class DocumentFactoryTestCase {

    @Autowired
    private DocumentFactory documentFactory;

    @Test(expected = UnsupportedOperationException.class)
    public void testAlbum3Unsupported() {
        documentFactory.createDocument(IndexType.ALBUM_ID3, new MediaFile());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testArtistd3Unsupported() {
        documentFactory.createDocument(IndexType.ARTIST_ID3, new MediaFile());
    }

    @Test
    public void testCreateAlbum() {
        Album album = new Album();
        album.setId(1);
        album.setName("name");
        album.setNameSort("nameSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setFolderId(10);

        Document document = documentFactory.createDocument(album);
        assertEquals("FieldNames.ID", "1", document.get(FieldNames.ID));
        assertEquals("FieldNames.ALBUM", "name", document.get(FieldNames.ALBUM));
        assertEquals("FieldNames.ALBUM_FULL", "name", document.get(FieldNames.ALBUM_FULL));
        assertEquals("FieldNames.ALBUM_READING_HIRAGANA", "nameSort", document.get(FieldNames.ALBUM_READING_HIRAGANA));
        assertEquals("FieldNames.ARTIST", "artist", document.get(FieldNames.ARTIST));
        assertEquals("FieldNames.ARTIST_FULL", "artist", document.get(FieldNames.ARTIST_FULL));
        assertEquals("FieldNames.ARTIST_READING", "artistSort", document.get(FieldNames.ARTIST_READING));
        assertEquals("FieldNames.ARTIST_READING_HIRAGANA", "artistSort", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertEquals("FieldNames.FOLDER_ID", "10", document.get(FieldNames.FOLDER_ID));
    }

    @Test
    public void testCreateArtist() {
        Artist artist = new Artist();
        artist.setId(1);
        artist.setName("name");
        artist.setSort("sort");
        artist.setFolderId(10);
        File musicDir = new File(MusicFolderTestData.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());

        Document document = documentFactory.createDocument(artist, musicFolder);
        assertEquals("FieldNames.ID", "1", document.get(FieldNames.ID));
        assertEquals("FieldNames.ARTIST", "name", document.get(FieldNames.ARTIST));
        assertEquals("FieldNames.ARTIST_FULL", "name", document.get(FieldNames.ARTIST_FULL));
        assertEquals("FieldNames.ARTIST_READING", "sort", document.get(FieldNames.ARTIST_READING));
        assertEquals("FieldNames.ARTIST_READING_HIRAGANA", "sort", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertNotEquals("FieldNames.FOLDER_ID", "10", document.get(FieldNames.FOLDER_ID));
        assertEquals("FieldNames.FOLDER_ID", "100", document.get(FieldNames.FOLDER_ID));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArtistNullPointer() {
        documentFactory.createDocument(new Artist(), null);// Folder is a required item.
    }

    @Test
    public void testCreateMediaAlbum() {
        MediaFile album = new MediaFile();
        album.setId(1);
        album.setAlbumName("albumName");
        album.setAlbumSort("albumSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.ALBUM, album);
        assertEquals("FieldNames.ID", "1", document.get(FieldNames.ID));
        assertEquals("FieldNames.ALBUM", "albumName", document.get(FieldNames.ALBUM));
        assertEquals("FieldNames.ALBUM_FULL", "albumName", document.get(FieldNames.ALBUM_FULL));
        assertEquals("FieldNames.ALBUM_READING_HIRAGANA", "albumSort", document.get(FieldNames.ALBUM_READING_HIRAGANA));
        assertEquals("FieldNames.ARTIST", "artist", document.get(FieldNames.ARTIST));
        assertEquals("FieldNames.ARTIST_FULL", "artist", document.get(FieldNames.ARTIST_FULL));
        assertEquals("FieldNames.ARTIST_READING", "artistSort", document.get(FieldNames.ARTIST_READING));
        assertEquals("FieldNames.ARTIST_READING_HIRAGANA", "artistSort", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertEquals("FieldNames.FOLDER_ID", "folder", document.get(FieldNames.FOLDER));
    }

    @Test
    public void testCreateMediaArtist() {
        MediaFile artist = new MediaFile();
        artist.setId(1);
        artist.setArtist("artist");
        artist.setArtistSort("artistSort");
        artist.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.ARTIST, artist);
        assertEquals("FieldNames.ID", "1", document.get(FieldNames.ID));
        assertEquals("FieldNames.ARTIST", "artist", document.get(FieldNames.ARTIST));
        assertEquals("FieldNames.ARTIST_FULL", "artist", document.get(FieldNames.ARTIST_FULL));
        assertEquals("FieldNames.ARTIST_READING", "artistSort", document.get(FieldNames.ARTIST_READING));
        assertEquals("FieldNames.ARTIST_READING_HIRAGANA", "artistSort", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertEquals("FieldNames.FOLDER_ID", "folder", document.get(FieldNames.FOLDER));
    }

    @Test
    public void testCreateMediaSong() {
        MediaFile song = new MediaFile();
        song.setId(1);
        song.setArtist("artist");
        song.setArtistSort("artistSort");
        song.setTitle("title");
        song.setTitleSort("titleSort");
        song.setMediaType(MediaType.MUSIC);
        song.setGenre("genre");
        song.setYear(2000);
        song.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.SONG, song);
        assertEquals("FieldNames.ID", "1", document.get(FieldNames.ID));
        assertEquals("FieldNames.ARTIST", "artist", document.get(FieldNames.ARTIST));
        assertEquals("FieldNames.ARTIST_FULL", "artist", document.get(FieldNames.ARTIST_FULL));
        assertEquals("FieldNames.ARTIST_READING", "artistSort", document.get(FieldNames.ARTIST_READING));
        assertEquals("FieldNames.ARTIST_READING_HIRAGANA", "artistSort", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertEquals("FieldNames.TITLE", "title", document.get(FieldNames.TITLE));
        assertEquals("FieldNames.TITLE_READING_HIRAGANA", "titleSort", document.get(FieldNames.TITLE_READING_HIRAGANA));
        assertEquals("FieldNames.MEDIA_TYPE", "MUSIC", document.get(FieldNames.MEDIA_TYPE));
        assertEquals("FieldNames.GENRE", "genre", document.get(FieldNames.GENRE));
        assertEquals("FieldNames.YEAR", "2000", document.get(FieldNames.YEAR));
        assertEquals("FieldNames.FOLDER_ID", "folder", document.get(FieldNames.FOLDER));
    }

    @Test
    public void testCreateNullAlbum() {
        Document document = documentFactory.createDocument(new Album());
        assertEquals("FieldNames.ID", "0", document.get(FieldNames.ID)); // Because domain getter is int type
        assertNull("FieldNames.ALBUM", document.get(FieldNames.ALBUM));
        assertNull("FieldNames.ALBUM_FULL", document.get(FieldNames.ALBUM_FULL));
        assertNull("FieldNames.ALBUM_READING_HIRAGANA", document.get(FieldNames.ALBUM_READING_HIRAGANA));
        assertNull("FieldNames.ARTIST", document.get(FieldNames.ARTIST));
        assertNull("FieldNames.ARTIST_FULL", document.get(FieldNames.ARTIST_FULL));
        assertNull("FieldNames.ARTIST_READING", document.get(FieldNames.ARTIST_READING));
        assertNull("FieldNames.ARTIST_READING_HIRAGANA", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertNull("FieldNames.FOLDER_ID", document.get(FieldNames.FOLDER_ID));
    }

    @Test
    public void testCreateNullArtist() {
        File musicDir = new File(MusicFolderTestData.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());
        Document document = documentFactory.createDocument(new Artist(), musicFolder);
        assertEquals("FieldNames.ID", "0", document.get(FieldNames.ID)); // Because domain getter is int type
        assertNull("FieldNames.ARTIST", document.get(FieldNames.ARTIST));
        assertNull("FieldNames.ARTIST_FULL", document.get(FieldNames.ARTIST_FULL));
        assertNull("FieldNames.ARTIST_READING", document.get(FieldNames.ARTIST_READING));
        assertNull("FieldNames.ARTIST_READING_HIRAGANA", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertNotEquals("FieldNames.FOLDER_ID", "10", document.get(FieldNames.FOLDER_ID));
        assertEquals("FieldNames.FOLDER_ID", "100", document.get(FieldNames.FOLDER_ID));
    }

    @Test
    public void testCreateNullMediaAlbum() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.ALBUM);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.ALBUM, mediaFile);
        assertEquals("FieldNames.ID", "0", document.get(FieldNames.ID)); // Because domain getter is int type
        assertNull("FieldNames.ALBUM", document.get(FieldNames.ALBUM));
        assertNull("FieldNames.ALBUM_FULL", document.get(FieldNames.ALBUM_FULL));
        assertNull("FieldNames.ALBUM_READING_HIRAGANA", document.get(FieldNames.ALBUM_READING_HIRAGANA));
        assertNull("FieldNames.ARTIST", document.get(FieldNames.ARTIST));
        assertNull("FieldNames.ARTIST_FULL", document.get(FieldNames.ARTIST_FULL));
        assertNull("FieldNames.ARTIST_READING", document.get(FieldNames.ARTIST_READING));
        assertNull("FieldNames.ARTIST_READING_HIRAGANA", document.get(FieldNames.ARTIST_READING_HIRAGANA));
    }

    @Test
    public void testCreateNullMediaArtist() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.DIRECTORY);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.ARTIST, mediaFile);
        assertEquals("FieldNames.ID", "0", document.get(FieldNames.ID)); // Because domain getter is int type
        assertNull("FieldNames.ARTIST", document.get(FieldNames.ARTIST));
        assertNull("FieldNames.ARTIST_FULL", document.get(FieldNames.ARTIST_FULL));
        assertNull("FieldNames.ARTIST_READING", document.get(FieldNames.ARTIST_READING));
        assertNull("FieldNames.ARTIST_READING_HIRAGANA", document.get(FieldNames.ARTIST_READING_HIRAGANA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileAlbumNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createDocument(IndexType.ALBUM, mediaFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileArtistNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createDocument(IndexType.ARTIST, mediaFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileSongNullFolder() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.MUSIC);
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createDocument(IndexType.SONG, mediaFile);
    }

    @Test(expected = NullPointerException.class)
    public void testMediaFileSongNullPointer() {
        MediaFile mediaFile = new MediaFile();
        assertNull("MediaType is a required item.", mediaFile.getMediaType());
        documentFactory.createDocument(IndexType.SONG, mediaFile);
    }

    @Test
    public void testNullMediaSong() {
        MediaFile song = new MediaFile();
        song.setMediaType(MediaType.MUSIC);
        song.setFolder("folder");
        Document document = documentFactory.createDocument(IndexType.SONG, song);
        assertEquals("FieldNames.ID", "0", document.get(FieldNames.ID)); // Because domain getter is int type
        assertNull("FieldNames.ARTIST", document.get(FieldNames.ARTIST));
        assertNull("FieldNames.ARTIST_FULL", document.get(FieldNames.ARTIST_FULL));
        assertNull("FieldNames.ARTIST_READING", document.get(FieldNames.ARTIST_READING));
        assertNull("FieldNames.ARTIST_READING_HIRAGANA", document.get(FieldNames.ARTIST_READING_HIRAGANA));
        assertNull("FieldNames.TITLE", document.get(FieldNames.TITLE));
        assertNull("FieldNames.TITLE_READING_HIRAGANA", document.get(FieldNames.TITLE_READING_HIRAGANA));
        assertNull("FieldNames.GENRE", document.get(FieldNames.GENRE));
        assertNull("FieldNames.YEAR", document.get(FieldNames.YEAR));
    }

}
