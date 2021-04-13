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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Date;

import org.airsonic.player.NeedsHome;
import org.airsonic.player.dao.MusicFolderTestDataUtils;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class DocumentFactoryTest {

    @Autowired
    private DocumentFactory documentFactory;

    @Autowired
    private SettingsService settingsService;

    @BeforeEach
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
        assertEquals(14, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID");
        assertEquals("name", document.get(FieldNamesConstants.ALBUM), "FieldNamesConstants.ALBUM");
        assertEquals("name", document.get(FieldNamesConstants.ALBUM_EX), "FieldNamesConstants.ALBUM_EX");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING),
                "FieldNamesConstants.ARTIST_READING");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
        assertEquals("10", document.get(FieldNamesConstants.FOLDER_ID), "FieldNamesConstants.FOLDER_ID");
    }

    @Test
    public void testCreateArtist() {
        Artist artist = new Artist();
        artist.setId(1);
        artist.setName("name");
        artist.setSort("sort");
        artist.setFolderId(10);
        File musicDir = new File(MusicFolderTestDataUtils.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        assertEquals(8, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID");
        assertEquals("name", document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertEquals("name", document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertEquals("sort", document.get(FieldNamesConstants.ARTIST_READING), "FieldNamesConstants.ARTIST_READING");
        assertNotEquals("10", document.get(FieldNamesConstants.FOLDER_ID), "FieldNamesConstants.FOLDER_ID");
        assertEquals("100", document.get(FieldNamesConstants.FOLDER_ID), "FieldNamesConstants.FOLDER_ID");
    }

    @Test
    public void testCreateArtistNullPointer() {
        // Folder is a required item.
        assertThrows(NullPointerException.class, () -> documentFactory.createArtistId3Document(new Artist(), null));
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
        assertEquals(14, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID");
        assertEquals("albumName", document.get(FieldNamesConstants.ALBUM), "FieldNamesConstants.ALBUM");
        assertEquals("albumName", document.get(FieldNamesConstants.ALBUM_EX), "FieldNamesConstants.ALBUM_EX");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING),
                "FieldNamesConstants.ARTIST_READING");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER), "FieldNamesConstants.FOLDER");
    }

    @Test
    public void testCreateMediaArtist() {
        MediaFile artist = new MediaFile();
        artist.setId(1);
        artist.setArtist("artist");
        artist.setArtistSort("artistSort");
        artist.setFolder("folder");
        Document document = documentFactory.createArtistDocument(artist);
        assertEquals(8, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING),
                "FieldNamesConstants.ARTIST_READING");
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER), "FieldNamesConstants.FOLDER");
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
        assertEquals(16, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING),
                "FieldNamesConstants.ARTIST_READING");
        assertEquals("title", document.get(FieldNamesConstants.TITLE), "FieldNamesConstants.TITLE");
        assertEquals("title", document.get(FieldNamesConstants.TITLE_EX), "FieldNamesConstants.TITLE_EX");
        assertEquals("MUSIC", document.get(FieldNamesConstants.MEDIA_TYPE), "FieldNamesConstants.MEDIA_TYPE");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
        assertNull(document.get(FieldNamesConstants.YEAR), "FieldNamesConstants.YEAR");
        // assertEquals("FieldNamesConstants.YEAR", "2000", document.get(FieldNamesConstants.YEAR));
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER), "FieldNamesConstants.FOLDER");
    }

    @Test
    public void testCreateGenreDocument() {
        MediaFile song = new MediaFile();
        song.setId(1);
        song.setGenre("genre");
        Document document = documentFactory.createGenreDocument(song);
        assertEquals(3, document.getFields().size(), "fields.size");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE_KEY), "FieldNamesConstants.GENRE_KEY");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
    }

    @Test
    public void testCreateNullAlbum() {
        assertThrows(NullPointerException.class, () -> documentFactory.createAlbumId3Document(new Album()));
    }

    @Test
    public void testCreateNullArtist() {
        File musicDir = new File(MusicFolderTestDataUtils.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());
        Document document = documentFactory.createArtistId3Document(new Artist(), musicFolder);
        assertEquals(2, document.getFields().size(), "fields.size");
        assertEquals("0", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID"); // Because domain getter is
                                                                                           // int type
        assertNull(document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertNull(document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertNull(document.get(FieldNamesConstants.ARTIST_READING), "FieldNamesConstants.ARTIST_READING");
        assertNotEquals("10", document.get(FieldNamesConstants.FOLDER_ID), "FieldNamesConstants.FOLDER_ID");
        assertEquals("100", document.get(FieldNamesConstants.FOLDER_ID), "FieldNamesConstants.FOLDER_ID");
    }

    @Test
    public void testCreateNullMediaAlbum() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.ALBUM);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createAlbumDocument(mediaFile);
        assertEquals(2, document.getFields().size(), "fields.size");
        assertEquals("0", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID"); // Because domain getter is
                                                                                           // int type
        assertNull(document.get(FieldNamesConstants.ALBUM), "FieldNamesConstants.ALBUM");
        assertNull(document.get(FieldNamesConstants.ALBUM_EX), "FieldNamesConstants.ALBUM_EX");
        assertNull(document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertNull(document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertNull(document.get(FieldNamesConstants.ARTIST_READING), "FieldNamesConstants.ARTIST_READING");
        assertNull(document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
    }

    @Test
    public void testCreateNullMediaArtist() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.DIRECTORY);
        mediaFile.setFolder("folder");
        Document document = documentFactory.createArtistDocument(mediaFile);
        assertEquals(2, document.getFields().size(), "fields.size");
        assertEquals("0", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID"); // Because domain getter is
                                                                                           // int type
        assertNull(document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertNull(document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertNull(document.get(FieldNamesConstants.ARTIST_READING), "FieldNamesConstants.ARTIST_READING");
    }

    @Test
    public void testMediaFileAlbumNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull(mediaFile.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createAlbumDocument(mediaFile));
    }

    @Test
    public void testMediaFileArtistNullFolder() {
        MediaFile mediaFile = new MediaFile();
        assertNull(mediaFile.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createArtistDocument(mediaFile));
    }

    @Test
    public void testMediaFileSongNullFolder() {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.MUSIC);
        assertNull(mediaFile.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createSongDocument(mediaFile));
    }

    @Test
    public void testMediaFileSongNullPointer() {
        MediaFile mediaFile = new MediaFile();
        assertNull(mediaFile.getMediaType(), "MediaType is a required item.");
        assertThrows(NullPointerException.class, () -> documentFactory.createSongDocument(mediaFile));
    }

    @Test
    public void testCreateNullMediaSong() {
        MediaFile song = new MediaFile();
        song.setMediaType(MediaType.MUSIC);
        song.setFolder("folder");
        Document document = documentFactory.createSongDocument(song);
        assertEquals(3, document.getFields().size(), "fields.size");
        assertEquals("0", document.get(FieldNamesConstants.ID), "FieldNamesConstants.ID"); // Because domain getter is
                                                                                           // int type
        assertNull(document.get(FieldNamesConstants.ARTIST), "FieldNamesConstants.ARTIST");
        assertNull(document.get(FieldNamesConstants.ARTIST_EX), "FieldNamesConstants.ARTIST_EX");
        assertNull(document.get(FieldNamesConstants.ARTIST_READING), "FieldNamesConstants.ARTIST_READING");
        assertNull(document.get(FieldNamesConstants.TITLE), "FieldNamesConstants.TITLE");
        assertNull(document.get(FieldNamesConstants.TITLE_EX), "FieldNamesConstants.TITLE_EX");
        assertNull(document.get(FieldNamesConstants.GENRE), "FieldNamesConstants.GENRE");
        assertNull(document.get(FieldNamesConstants.YEAR), "FieldNamesConstants.YEAR");
    }

}
