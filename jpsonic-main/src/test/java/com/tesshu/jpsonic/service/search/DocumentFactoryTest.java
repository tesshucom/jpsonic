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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.annotation.Documented;
import java.util.Date;

import com.tesshu.jpsonic.dao.MusicFolderTestDataUtils;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class DocumentFactoryTest {

    private SettingsService settingsService;
    private DocumentFactory documentFactory;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        documentFactory = new DocumentFactory(settingsService, new JapaneseReadingUtils(settingsService));
    }

    @Test
    void testCreateAlbumDocument() {
        MediaFile album = new MediaFile();
        album.setId(1);
        album.setAlbumName("albumName");
        album.setAlbumSort("albumSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setGenre("genre");
        album.setFolder("folder");
        Document document = documentFactory.createAlbumDocument(album);
        assertEquals(12, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID));
        assertEquals("albumName", document.get(FieldNamesConstants.ALBUM));
        assertEquals("albumSort", document.get(FieldNamesConstants.ALBUM_READING));
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("genre", document.get(FieldNamesConstants.GENRE));
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER));

        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.ALBUM);
        mediaFile.setFolder("folder");
        document = documentFactory.createAlbumDocument(mediaFile);
        assertEquals(2, document.getFields().size(), "fields.size");
        // Because domain getter is int type
        assertEquals("0", document.get(FieldNamesConstants.ID));
        assertNull(document.get(FieldNamesConstants.ALBUM));
        assertNull(document.get(FieldNamesConstants.ALBUM_READING));
        assertNull(document.get(FieldNamesConstants.ARTIST));
        assertNull(document.get(FieldNamesConstants.ARTIST_READING));
        assertNull(document.get(FieldNamesConstants.GENRE));

        final MediaFile file = new MediaFile();
        assertNull(file.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createAlbumDocument(file));
    }

    @Test
    void testCreateArtistDocument() {
        MediaFile artist = new MediaFile();
        artist.setId(1);
        artist.setArtist("artist");
        artist.setArtistSort("artistSort");
        artist.setFolder("folder");
        Document document = documentFactory.createArtistDocument(artist);
        assertEquals(6, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID));
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER));

        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.DIRECTORY);
        mediaFile.setFolder("folder");
        document = documentFactory.createArtistDocument(mediaFile);
        assertEquals(2, document.getFields().size(), "fields.size");
        // Because domain getter is int type
        assertEquals("0", document.get(FieldNamesConstants.ID));
        assertNull(document.get(FieldNamesConstants.ARTIST));
        assertNull(document.get(FieldNamesConstants.ARTIST_READING));

        final MediaFile file = new MediaFile();
        assertNull(file.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createArtistDocument(file));
    }

    @Test
    void testCreateAlbumId3Document() {
        Album album = new Album();
        album.setId(1);
        album.setName("name");
        album.setNameSort("nameSort");
        album.setArtist("artist");
        album.setArtistSort("artistSort");
        album.setGenre("genre");
        album.setFolderId(10);
        Document document = documentFactory.createAlbumId3Document(album);
        assertEquals(12, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID));
        assertEquals("name", document.get(FieldNamesConstants.ALBUM));
        assertEquals("nameSort", document.get(FieldNamesConstants.ALBUM_READING));
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("genre", document.get(FieldNamesConstants.GENRE));
        assertEquals("10", document.get(FieldNamesConstants.FOLDER_ID));

        assertThrows(NullPointerException.class, () -> documentFactory.createAlbumId3Document(new Album()));
    }

    @Test
    void testCreateArtistId3Document() {
        Artist artist = new Artist();
        artist.setId(1);
        artist.setName("name");
        artist.setSort("sort");
        artist.setFolderId(10);
        File musicDir = new File(MusicFolderTestDataUtils.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(100, musicDir, "Music", true, new Date());
        Document document = documentFactory.createArtistId3Document(artist, musicFolder);
        assertEquals(6, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID));
        assertEquals("name", document.get(FieldNamesConstants.ARTIST));
        assertEquals("sort", document.get(FieldNamesConstants.ARTIST_READING));
        Assertions.assertNotEquals("10", document.get(FieldNamesConstants.FOLDER_ID));
        assertEquals("100", document.get(FieldNamesConstants.FOLDER_ID));

        document = documentFactory.createArtistId3Document(new Artist(), musicFolder);
        assertEquals(2, document.getFields().size(), "fields.size");
        // Because domain getter is int type
        assertEquals("0", document.get(FieldNamesConstants.ID));
        assertNull(document.get(FieldNamesConstants.ARTIST));
        assertNull(document.get(FieldNamesConstants.ARTIST_READING));
        Assertions.assertNotEquals("10", document.get(FieldNamesConstants.FOLDER_ID));
        assertEquals("100", document.get(FieldNamesConstants.FOLDER_ID));

        // Folder is a required item.
        assertThrows(NullPointerException.class, () -> documentFactory.createArtistId3Document(new Artist(), null));
    }

    @Test
    void testCreateSongDocument() {
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
        song.setComposer("composer");
        song.setComposerSortRaw("composerSort");

        Document document = documentFactory.createSongDocument(song);
        assertEquals(18, document.getFields().size(), "fields.size");
        assertEquals("1", document.get(FieldNamesConstants.ID));
        assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
        assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
        assertEquals("title", document.get(FieldNamesConstants.TITLE));
        assertEquals("title", document.get(FieldNamesConstants.TITLE_READING));
        assertEquals("MUSIC", document.get(FieldNamesConstants.MEDIA_TYPE));
        assertEquals("genre", document.get(FieldNamesConstants.GENRE));
        assertNull(document.get(FieldNamesConstants.YEAR));
        // assertEquals("FieldNamesConstants.YEAR", "2000", document.get(FieldNamesConstants.YEAR));
        assertEquals("folder", document.get(FieldNamesConstants.FOLDER));
        assertEquals("composer", document.get(FieldNamesConstants.COMPOSER));
        assertEquals("composerSort", document.get(FieldNamesConstants.COMPOSER_READING));

        song = new MediaFile();
        song.setMediaType(MediaType.MUSIC);
        song.setFolder("folder");
        document = documentFactory.createSongDocument(song);
        assertEquals(3, document.getFields().size(), "fields.size");
        // Because domain getter is int type
        assertEquals("0", document.get(FieldNamesConstants.ID));
        assertNull(document.get(FieldNamesConstants.ARTIST));
        assertNull(document.get(FieldNamesConstants.ARTIST_READING));
        assertNull(document.get(FieldNamesConstants.TITLE));
        assertNull(document.get(FieldNamesConstants.TITLE_READING));
        assertNull(document.get(FieldNamesConstants.GENRE));
        assertNull(document.get(FieldNamesConstants.YEAR));
        assertNull(document.get(FieldNamesConstants.COMPOSER));
        assertNull(document.get(FieldNamesConstants.COMPOSER_READING));

        MediaFile mediaFile = new MediaFile();
        mediaFile.setMediaType(MediaType.MUSIC);
        assertNull(mediaFile.getFolder(), "Folder is a required item.");
        assertThrows(IllegalArgumentException.class, () -> documentFactory.createSongDocument(mediaFile));

        final MediaFile file = new MediaFile();
        assertNull(file.getMediaType(), "MediaType is a required item.");
        assertThrows(NullPointerException.class, () -> documentFactory.createSongDocument(file));
    }

    @Test
    void testCreateGenreDocument() {
        MediaFile song = new MediaFile();
        song.setId(1);
        song.setGenre("genre");
        Document document = documentFactory.createGenreDocument(song);
        assertEquals(3, document.getFields().size(), "fields.size");
        assertEquals("genre", document.get(FieldNamesConstants.GENRE_KEY));
        assertEquals("genre", document.get(FieldNamesConstants.GENRE));
    }

    @Documented
    private @interface ReadingDecisions {
        @interface Conditions {
            @interface IndexScheme {
                @interface NativeJapanese {
                }

                @interface RomanizedJapanese {
                }

                @interface WithoutJpLangProcessing {
                }
            }

            @interface Value {
                @interface Null {
                }

                @interface NotNull {
                    @interface EqSort {
                    }

                    @interface Japanese {

                    }

                    @interface NotJapanese {

                    }
                }
            }
        }
    }

    @Nested
    class AcceptReadingTest {

        private MediaFile createSong() {
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
            song.setComposer("composer");
            song.setComposerSortRaw("composerSort");
            return song;
        }

        @ReadingDecisions.Conditions.Value.Null
        @Test
        void c01() {
            MediaFile song = createSong();
            song.setArtist(null);
            song.setComposer(null);
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertNull(document.get(FieldNamesConstants.ARTIST));
            assertNull(document.get(FieldNamesConstants.ARTIST_READING));
            assertNull(document.get(FieldNamesConstants.COMPOSER));
            assertNull(document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.Value.NotNull.EqSort
        @Test
        void c02() {
            MediaFile song = createSong();
            song.setArtist("Artist");
            song.setArtistSort("Artist");
            song.setComposer("Composer");
            song.setComposerSortRaw("Composer");
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("Artist", document.get(FieldNamesConstants.ARTIST));
            assertNull(document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("Composer", document.get(FieldNamesConstants.COMPOSER));
            assertNull(document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.IndexScheme.NativeJapanese
        @ReadingDecisions.Conditions.Value.NotNull.NotJapanese
        @Test
        void c03() {
            MediaFile song = createSong();
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
            assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("composer", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("composerSort", document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.IndexScheme.NativeJapanese
        @ReadingDecisions.Conditions.Value.NotNull.Japanese
        @Test
        void c04() {
            MediaFile song = createSong();
            song.setArtist("アーティスト");
            song.setArtistSort("あーてぃすと");
            song.setComposer("作曲者");
            song.setComposerSortRaw("さっきょくしゃ");
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("アーティスト", document.get(FieldNamesConstants.ARTIST));
            assertEquals("あーてぃすと", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("作曲者", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("さっきょくしゃ", document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.IndexScheme.RomanizedJapanese
        @ReadingDecisions.Conditions.Value.NotNull.NotJapanese
        @Test
        void c05() {
            Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
            MediaFile song = createSong();
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
            assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("composer", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("composerSort", document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.IndexScheme.RomanizedJapanese
        @ReadingDecisions.Conditions.Value.NotNull.Japanese
        @Test
        void c06() {
            Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
            MediaFile song = createSong();
            song.setArtist("アーティスト");
            song.setArtistSort("あーてぃすと");
            song.setComposer("作曲者");
            song.setComposerSortRaw("さっきょくしゃ");
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("アーティスト", document.get(FieldNamesConstants.ARTIST));
            assertEquals("あーてぃすと", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("あーてぃすと", document.get(FieldNamesConstants.ARTIST_READING_ROMANIZED));
            assertEquals("作曲者", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("さっきょくしゃ", document.get(FieldNamesConstants.COMPOSER_READING));
            assertEquals("さっきょくしゃ", document.get(FieldNamesConstants.COMPOSER_READING_ROMANIZED));
        }

        @ReadingDecisions.Conditions.IndexScheme.WithoutJpLangProcessing
        @ReadingDecisions.Conditions.Value.NotNull.NotJapanese
        @Test
        void c07() {
            Mockito.when(settingsService.getIndexSchemeName())
                    .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
            MediaFile song = createSong();
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("artist", document.get(FieldNamesConstants.ARTIST));
            assertEquals("artistSort", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("composer", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("composerSort", document.get(FieldNamesConstants.COMPOSER_READING));
        }

        @ReadingDecisions.Conditions.IndexScheme.WithoutJpLangProcessing
        @ReadingDecisions.Conditions.Value.NotNull.Japanese
        @Test
        void c08() {
            Mockito.when(settingsService.getIndexSchemeName())
                    .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
            MediaFile song = createSong();
            song.setArtist("アーティスト");
            song.setArtistSort("あーてぃすと");
            song.setComposer("さっきょくしゃ");
            song.setComposerSortRaw("サッキョクシャ");
            Document document = documentFactory.createSongDocument(song);
            documentFactory.acceptArtistReading(document, song.getArtist(), song.getArtistSort(),
                    song.getArtistReading());
            documentFactory.acceptComposerReading(document, song.getComposer(), song.getComposerSortRaw(),
                    song.getComposerSort());
            assertEquals("アーティスト", document.get(FieldNamesConstants.ARTIST));
            assertEquals("あーてぃすと", document.get(FieldNamesConstants.ARTIST_READING));
            assertEquals("さっきょくしゃ", document.get(FieldNamesConstants.COMPOSER));
            assertEquals("サッキョクシャ", document.get(FieldNamesConstants.COMPOSER_READING));
        }
    }
}
