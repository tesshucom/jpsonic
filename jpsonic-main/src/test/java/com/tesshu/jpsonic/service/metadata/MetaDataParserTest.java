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

package com.tesshu.jpsonic.service.metadata;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link MetaDataParser}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class MetaDataParserTest {

    private MusicFolderService musicFolderService;
    private MetaDataParser parser;

    @BeforeEach
    void setUp() {

        musicFolderService = mock(MusicFolderService.class);

        parser = new MetaDataParser() {
            @Override
            public MetaData getRawMetaData(Path path) {
                return null;
            }

            @Override
            public void setMetaData(MediaFile file, MetaData metaData) {
                // to be none
            }

            @Override
            public boolean isEditingSupported(Path path) {
                return false;
            }

            @Override
            protected MusicFolderService getMusicFolderService() {
                return musicFolderService;
            }

            @Override
            public boolean isApplicable(Path path) {
                return false;
            }
        };
    }

    @Nested
    class GetMetaDataTest {

        private MusicParser musicParser;
        private MetaData metaData;
        private Path path;

        @BeforeEach
        void setUp() throws URISyntaxException {

            musicParser = mock(MusicParser.class);
            metaData = mock(MetaData.class);
            path = Path.of(MetaDataParserTest.class.getResource("/MEDIAS/Metadata/tagger3/blank/blank.flac").toURI());
            parser = new MetaDataParser() {
                @Override
                public MetaData getRawMetaData(Path path) {
                    return musicParser.getMetaData(path);
                }

                @Override
                public void setMetaData(MediaFile file, MetaData metaData) {
                    // to be none
                }

                @Override
                public boolean isEditingSupported(Path path) {
                    return false;
                }

                @Override
                protected MusicFolderService getMusicFolderService() {
                    return musicFolderService;
                }

                @Override
                public boolean isApplicable(Path path) {
                    return false;
                }
            };
        }

        @Test
        void testWithEmptyMeta() throws URISyntaxException {
            MusicParser musicParser = new MusicParser(musicFolderService);
            Path blankData = Path
                    .of(MetaDataParserTest.class.getResource("/MEDIAS/Metadata/tagger3/blank/blank.flac").toURI());
            MetaData metaData = musicParser.getMetaData(blankData);
            assertEquals("tagger3", metaData.getArtist());
            assertEquals("tagger3", metaData.getAlbumArtist());
            assertEquals("blank", metaData.getAlbumName());
            assertEquals("blank", metaData.getTitle());
            assertNull(metaData.getArtistSort());
            assertNull(metaData.getAlbumArtistSort());
            assertNull(metaData.getAlbumSort());
            assertNull(metaData.getTitleSort());
        }

        @Test
        void testWithMeta() {
            metaData = new MetaData();
            metaData.setArtist("Artist");
            metaData.setAlbumArtist("AlbumArtist");
            metaData.setAlbumName("AlbumName");
            metaData.setTitle("Title");
            Mockito.when(musicParser.getMetaData(path)).thenReturn(metaData);

            MetaData result = parser.getMetaData(path);
            assertEquals("Artist", result.getArtist());
            assertEquals("AlbumArtist", result.getAlbumArtist());
            assertEquals("AlbumName", result.getAlbumName());
            assertEquals("Title", result.getTitle());
            assertNull(result.getArtistSort());
            assertNull(result.getAlbumArtistSort());
            assertNull(result.getAlbumSort());
            assertNull(result.getTitleSort());
        }

        /*
         * #1875 Note that this method is not guaranteed to be perfect.
         */
        @Test
        void testExcessiveFormatting() {
            metaData = new MetaData();
            metaData.setTitle("10 Years Today");
            metaData.setTrackNumber(10);
            Mockito.when(musicParser.getMetaData(path)).thenReturn(metaData);

            MetaData result = parser.getMetaData(path);
            assertEquals("Years Today", result.getTitle());
        }
    }

    @Test
    void testRemoveTrackNumberFromTitle() {
        assertEquals("", parser.removeTrackNumberFromTitle("", null));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("kokos", null));
        assertEquals("01 kokos", parser.removeTrackNumberFromTitle("01 kokos", null));
        assertEquals("01 - kokos", parser.removeTrackNumberFromTitle("01 - kokos", null));
        assertEquals("01-kokos", parser.removeTrackNumberFromTitle("01-kokos", null));
        assertEquals("01 - kokos", parser.removeTrackNumberFromTitle("01 - kokos", null));
        assertEquals("99 - kokos", parser.removeTrackNumberFromTitle("99 - kokos", null));
        assertEquals("99.- kokos", parser.removeTrackNumberFromTitle("99.- kokos", null));
        assertEquals("01 kokos", parser.removeTrackNumberFromTitle(" 01 kokos", null));
        assertEquals("400 years", parser.removeTrackNumberFromTitle("400 years", null));
        assertEquals("49ers", parser.removeTrackNumberFromTitle("49ers", null));
        assertEquals("01", parser.removeTrackNumberFromTitle("01", null));
        assertEquals("01", parser.removeTrackNumberFromTitle("01 ", null));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01 ", null));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01", null));

        assertEquals("", parser.removeTrackNumberFromTitle("", 1));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("01 kokos", 1));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("01 - kokos", 1));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("01-kokos", 1));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("99 - kokos", 99));
        assertEquals("kokos", parser.removeTrackNumberFromTitle("99.- kokos", 99));
        assertEquals("01 kokos", parser.removeTrackNumberFromTitle("01 kokos", 2));
        assertEquals("1 kokos", parser.removeTrackNumberFromTitle("1 kokos", 2));
        assertEquals("50 years", parser.removeTrackNumberFromTitle("50 years", 1));
        assertEquals("years", parser.removeTrackNumberFromTitle("50 years", 50));
        assertEquals("15 Step", parser.removeTrackNumberFromTitle("15 Step", 1));
        assertEquals("Step", parser.removeTrackNumberFromTitle("15 Step", 15));

        assertEquals("49ers", parser.removeTrackNumberFromTitle("49ers", 1));
        assertEquals("49ers", parser.removeTrackNumberFromTitle("49ers", 49));
        assertEquals("01", parser.removeTrackNumberFromTitle("01", 1));
        assertEquals("01", parser.removeTrackNumberFromTitle("01 ", 1));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01 ", 1));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01", 1));
        assertEquals("01", parser.removeTrackNumberFromTitle("01", 2));
        assertEquals("01", parser.removeTrackNumberFromTitle("01 ", 2));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01 ", 2));
        assertEquals("01", parser.removeTrackNumberFromTitle(" 01", 2));

        /*
         * #1875 Note that this method is not guaranteed to be perfect.
         */
        assertEquals("10 Years Today", parser.removeTrackNumberFromTitle("10 - 10 Years Today", 10));
        assertEquals("10 Years Today", parser.removeTrackNumberFromTitle("10 Years Today", null));
        assertEquals("10 Years Today", parser.removeTrackNumberFromTitle("10 Years Today", 1));
        assertEquals("Years Today", parser.removeTrackNumberFromTitle("10 Years Today", 10)); // Mentioned
        assertEquals("10 Years Today", parser.removeTrackNumberFromTitle("10 Years Today", 100));
    }

    @Test
    void testGuessArtist() {
        assertThrows(IllegalArgumentException.class, () -> parser.guessArtist(Path.of("/")));
        assertThrows(IllegalArgumentException.class, () -> parser.guessArtist(Path.of("/song.mp3")));
        assertEquals("", parser.guessArtist(Path.of("/MusicFolder/artist")));
        assertEquals("MusicFolder", parser.guessArtist(Path.of("/MusicFolder/artist/song.mp3")));
        assertEquals("MusicFolder", parser.guessArtist(Path.of("/MusicFolder/artist/album")));
        assertEquals("artist", parser.guessArtist(Path.of("/MusicFolder/artist/album/song.mp3")));

        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder("/MusicFolder", "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertThrows(IllegalArgumentException.class, () -> parser.guessArtist(Path.of("/")));
        assertThrows(IllegalArgumentException.class, () -> parser.guessArtist(Path.of("/song.mp3")));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist")));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist/song.mp3")));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist/album")));
        assertEquals("artist", parser.guessArtist(Path.of("/MusicFolder/artist/album/song.mp3")));
    }

    @Test
    void testGuessAlbum() {
        assertThrows(IllegalArgumentException.class, () -> parser.guessAlbum(Path.of("/"), "artist"));
        assertEquals("", parser.guessAlbum(Path.of("/song.mp3"), "artist"));
        assertEquals("MusicFolder", parser.guessAlbum(Path.of("/MusicFolder/artist"), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/song.mp3"), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/album"), "artist"));
        assertEquals("album", parser.guessAlbum(Path.of("/MusicFolder/artist/album/song.mp3"), "artist"));

        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder("/MusicFolder", "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertThrows(IllegalArgumentException.class, () -> parser.guessAlbum(Path.of("/"), "artist"));
        assertEquals("", parser.guessAlbum(Path.of("/song.mp3"), "artist"));
        assertNull(parser.guessAlbum(Path.of("/MusicFolder/artist"), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/song.mp3"), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/album"), "artist"));
        assertEquals("album", parser.guessAlbum(Path.of("/MusicFolder/artist/album/song.mp3"), "artist"));
    }

    @Test
    void testGuessTitle() {
        assertEquals("", parser.guessTitle(Path.of("/")));
        assertEquals("song", parser.guessTitle(Path.of("/song.mp3")));
        assertEquals("artist", parser.guessTitle(Path.of("/MusicFolder/artist")));
        assertEquals("song", parser.guessTitle(Path.of("/MusicFolder/artist/song.mp3")));
        assertEquals("album", parser.guessTitle(Path.of("/MusicFolder/artist/album")));
        assertEquals("song", parser.guessTitle(Path.of("/MusicFolder/artist/album/song.mp3")));
    }

    @Test
    void testIsRoot() {
        assertFalse(parser.isRoot(Path.of("/MusicFolder/artist")));
        assertFalse(parser.isRoot(Path.of("/MusicFolder")));
        assertFalse(parser.isRoot(Path.of("/")));
        List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder("/MusicFolder", "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertFalse(parser.isRoot(Path.of("/MusicFolder/artist")));
        assertTrue(parser.isRoot(Path.of("/MusicFolder")));
        assertFalse(parser.isRoot(Path.of("/")));
    }
}
