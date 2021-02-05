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

package org.airsonic.player.service.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Date;

import org.airsonic.player.dao.MusicFolderTestData;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.apache.lucene.document.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DocumentFactoryTest {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private SettingsService settingsService;

    @Before
    public void setup() {
        settingsService.setSearchMethodLegacy(false);
    }

    @Test
    public void testCreateAlbum() {
        Album album = new Album();
        album.setId(1);
        album.setName("name");
        album.setNameSort("nameSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setGenre("genre");
        album.setFolderId(10);
        Document document = documentFactory.createAlbumId3Document(album);
        assertEquals("fields.size", 14, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "1", document.get(FieldNamesConstants.ID));
        assertEquals("FieldNamesConstants.ALBUM", "name", document.get(FieldNamesConstants.ALBUM));
        assertEquals("FieldNamesConstants.ALBUM_EX", "name", document.get(FieldNamesConstants.ALBUM_EX));
        assertEquals("FieldNamesConstants.ARTIST", "artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("FieldNamesConstants.ARTIST_EX", "artist", document.get(FieldNamesConstants.ARTIST_EX));
        assertEquals("FieldNamesConstants.ARTIST_READING", "artistSort",
                document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("FieldNamesConstants.GENRE", "genre", document.get(FieldNamesConstants.GENRE));
        assertEquals("FieldNamesConstants.FOLDER_ID", "10", document.get(FieldNamesConstants.FOLDER_ID));
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
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        assertEquals("fields.size", 8, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "1", document.get(FieldNamesConstants.ID));
        assertEquals("FieldNamesConstants.ARTIST", "name", document.get(FieldNamesConstants.ARTIST));
        assertEquals("FieldNamesConstants.ARTIST_EX", "name", document.get(FieldNamesConstants.ARTIST_EX));
        assertEquals("FieldNamesConstants.ARTIST_READING", "sort", document.get(FieldNamesConstants.ARTIST_READING));
        assertNotEquals("FieldNamesConstants.FOLDER_ID", "10", document.get(FieldNamesConstants.FOLDER_ID));
        assertEquals("FieldNamesConstants.FOLDER_ID", "100", document.get(FieldNamesConstants.FOLDER_ID));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArtistNullPointer() {
        documentFactory.createArtistId3Document(new Artist(), null); // Folder is a required item.
    }

    @Test
    public void testCreateMediaAlbum() {
        MediaFile album = new MediaFile();
        album.setId(1);
        album.setAlbumName("albumName");
        album.setAlbumSort("albumSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setGenre("genre");
        album.setFolder("folder");
        Document document = documentFactory.createAlbumDocument(album);
        assertEquals("fields.size", 14, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "1", document.get(FieldNamesConstants.ID));
        assertEquals("FieldNamesConstants.ALBUM", "albumName", document.get(FieldNamesConstants.ALBUM));
        assertEquals("FieldNamesConstants.ALBUM_EX", "albumName", document.get(FieldNamesConstants.ALBUM_EX));
        assertEquals("FieldNamesConstants.ARTIST", "artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("FieldNamesConstants.ARTIST_EX", "artist", document.get(FieldNamesConstants.ARTIST_EX));
        assertEquals("FieldNamesConstants.ARTIST_READING", "artistSort",
                document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("FieldNamesConstants.GENRE", "genre", document.get(FieldNamesConstants.GENRE));
        assertEquals("FieldNamesConstants.FOLDER", "folder", document.get(FieldNamesConstants.FOLDER));
    }

    @Test
    public void testCreateMediaArtist() {
        MediaFile artist = new MediaFile();
        artist.setId(1);
        artist.setArtist("artist");
        artist.setArtistSort("artistSort");
        artist.setFolder("folder");
        Document document = documentFactory.createArtistDocument(artist);
        assertEquals("fields.size", 8, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "1", document.get(FieldNamesConstants.ID));
        assertEquals("FieldNamesConstants.ARTIST", "artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("FieldNamesConstants.ARTIST_EX", "artist", document.get(FieldNamesConstants.ARTIST_EX));
        assertEquals("FieldNamesConstants.ARTIST_READING", "artistSort",
                document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("FieldNamesConstants.FOLDER", "folder", document.get(FieldNamesConstants.FOLDER));
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
        Document document = documentFactory.createSongDocument(song);
        assertEquals("fields.size", 16, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "1", document.get(FieldNamesConstants.ID));
        assertEquals("FieldNamesConstants.ARTIST", "artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("FieldNamesConstants.ARTIST_EX", "artist", document.get(FieldNamesConstants.ARTIST_EX));
        assertEquals("FieldNamesConstants.ARTIST_READING", "artistSort",
                document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("FieldNamesConstants.TITLE", "title", document.get(FieldNamesConstants.TITLE));
        assertEquals("FieldNamesConstants.TITLE_EX", "title", document.get(FieldNamesConstants.TITLE_EX));
        assertEquals("FieldNamesConstants.MEDIA_TYPE", "MUSIC", document.get(FieldNamesConstants.MEDIA_TYPE));
        assertEquals("FieldNamesConstants.GENRE", "genre", document.get(FieldNamesConstants.GENRE));
        assertEquals("FieldNamesConstants.YEAR", null, document.get(FieldNamesConstants.YEAR));
        // assertEquals("FieldNamesConstants.YEAR", "2000", document.get(FieldNamesConstants.YEAR));
        assertEquals("FieldNamesConstants.FOLDER", "folder", document.get(FieldNamesConstants.FOLDER));
    }

    @Test
    public void testCreateGenreDocument() {
        MediaFile song = new MediaFile();
        song.setId(1);
        song.setGenre("genre");
        Document document = documentFactory.createGenreDocument(song);
        assertEquals("fields.size", 3, document.getFields().size());
        assertEquals("FieldNamesConstants.GENRE_KEY", "genre", document.get(FieldNamesConstants.GENRE_KEY));
        assertEquals("FieldNamesConstants.GENRE", "genre", document.get(FieldNamesConstants.GENRE));
    }

    @Test(expected = NullPointerException.class)
    public void testCreateNullAlbum() {
        Document document = documentFactory.createAlbumId3Document(new Album());
        assertEquals("fields.size", 1, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "0", document.get(FieldNamesConstants.ID)); // Because domain getter is
                                                                                           // int type
        assertNull("FieldNamesConstants.ALBUM", document.get(FieldNamesConstants.ALBUM));
        assertNull("FieldNamesConstants.ALBUM_EX", document.get(FieldNamesConstants.ALBUM_EX));
        assertNull("FieldNamesConstants.ARTIST", document.get(FieldNamesConstants.ARTIST));
        assertNull("FieldNamesConstants.ARTIST_EX", document.get(FieldNamesConstants.ARTIST_EX));
        assertNull("FieldNamesConstants.ARTIST_READING", document.get(FieldNamesConstants.ARTIST_READING));
        assertNull("FieldNamesConstants.GENRE", document.get(FieldNamesConstants.GENRE));
        assertNull("FieldNamesConstants.FOLDER_ID", document.get(FieldNamesConstants.FOLDER_ID));
    }

    @Test
    public void testCreateNullArtist() {
        File musicDir = new File(MusicFolderTestData.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());
        Document document = documentFactory.createArtistId3Document(new Artist(), musicFolder);
        assertEquals("fields.size", 2, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "0", document.get(FieldNamesConstants.ID)); // Because domain getter is
                                                                                           // int type
        assertNull("FieldNamesConstants.ARTIST", document.get(FieldNamesConstants.ARTIST));
        assertNull("FieldNamesConstants.ARTIST_EX", document.get(FieldNamesConstants.ARTIST_EX));
        assertNull("FieldNamesConstants.ARTIST_READING", document.get(FieldNamesConstants.ARTIST_READING));
        assertNotEquals("FieldNamesConstants.FOLDER_ID", "10", document.get(FieldNamesConstants.FOLDER_ID));
        assertEquals("FieldNamesConstants.FOLDER_ID", "100", document.get(FieldNamesConstants.FOLDER_ID));
    }

    @Test
    public void testCreateNullMediaAlbum() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.ALBUM);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createAlbumDocument(mediaFile);
        assertEquals("fields.size", 2, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "0", document.get(FieldNamesConstants.ID)); // Because domain getter is
                                                                                           // int type
        assertNull("FieldNamesConstants.ALBUM", document.get(FieldNamesConstants.ALBUM));
        assertNull("FieldNamesConstants.ALBUM_EX", document.get(FieldNamesConstants.ALBUM_EX));
        assertNull("FieldNamesConstants.ARTIST", document.get(FieldNamesConstants.ARTIST));
        assertNull("FieldNamesConstants.ARTIST_EX", document.get(FieldNamesConstants.ARTIST_EX));
        assertNull("FieldNamesConstants.ARTIST_READING", document.get(FieldNamesConstants.ARTIST_READING));
        assertNull("FieldNamesConstants.GENRE", document.get(FieldNamesConstants.GENRE));
    }

    @Test
    public void testCreateNullMediaArtist() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.DIRECTORY);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createArtistDocument(mediaFile);
        assertEquals("fields.size", 2, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "0", document.get(FieldNamesConstants.ID)); // Because domain getter is
                                                                                           // int type
        assertNull("FieldNamesConstants.ARTIST", document.get(FieldNamesConstants.ARTIST));
        assertNull("FieldNamesConstants.ARTIST_EX", document.get(FieldNamesConstants.ARTIST_EX));
        assertNull("FieldNamesConstants.ARTIST_READING", document.get(FieldNamesConstants.ARTIST_READING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileAlbumNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createAlbumDocument(mediaFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileArtistNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createArtistDocument(mediaFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMediaFileSongNullFolder() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.MUSIC);
        assertNull("Folder is a required item.", mediaFile.getFolder());
        documentFactory.createSongDocument(mediaFile);
    }

    @Test(expected = NullPointerException.class)
    public void testMediaFileSongNullPointer() {
        MediaFile mediaFile = new MediaFile();
        assertNull("MediaType is a required item.", mediaFile.getMediaType());
        documentFactory.createSongDocument(mediaFile);
    }

    @Test
    public void testCreateNullMediaSong() {
        MediaFile song = new MediaFile();
        song.setMediaType(MediaType.MUSIC);
        song.setFolder("folder");
        Document document = documentFactory.createSongDocument(song);
        assertEquals("fields.size", 3, document.getFields().size());
        assertEquals("FieldNamesConstants.ID", "0", document.get(FieldNamesConstants.ID)); // Because domain getter is
                                                                                           // int type
        assertNull("FieldNamesConstants.ARTIST", document.get(FieldNamesConstants.ARTIST));
        assertNull("FieldNamesConstants.ARTIST_EX", document.get(FieldNamesConstants.ARTIST_EX));
        assertNull("FieldNamesConstants.ARTIST_READING", document.get(FieldNamesConstants.ARTIST_READING));
        assertNull("FieldNamesConstants.TITLE", document.get(FieldNamesConstants.TITLE));
        assertNull("FieldNamesConstants.TITLE_EX", document.get(FieldNamesConstants.TITLE_EX));
        assertNull("FieldNamesConstants.GENRE", document.get(FieldNamesConstants.GENRE));
        assertNull("FieldNamesConstants.YEAR", document.get(FieldNamesConstants.YEAR));
    }

}
