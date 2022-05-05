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

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MusicFolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link MetaDataParser}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class MetaDataParserTest {

    private MusicFolderService musicFolderService;
    private MetaDataParser parser;

    @BeforeEach
    void setUp() {

        musicFolderService = mock(MusicFolderService.class);

        parser = new MetaDataParser() {
            @Override
            public MetaData getRawMetaData(File file) {
                return null;
            }

            @Override
            public void setMetaData(MediaFile file, MetaData metaData) {
                // to be none
            }

            @Override
            public boolean isEditingSupported(File file) {
                return false;
            }

            @Override
            protected MusicFolderService getMusicFolderService() {
                return musicFolderService;
            }

            @Override
            public boolean isApplicable(File file) {
                return false;
            }
        };
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
    }

    @Test
    void testGuessArtist() {
        assertThrows(NullPointerException.class, () -> parser.guessArtist(Path.of("/").toFile()));
        assertThrows(NullPointerException.class, () -> parser.guessArtist(Path.of("/song.mp3").toFile()));
        assertEquals("", parser.guessArtist(Path.of("/MusicFolder/artist").toFile()));
        assertEquals("MusicFolder", parser.guessArtist(Path.of("/MusicFolder/artist/song.mp3").toFile()));
        assertEquals("MusicFolder", parser.guessArtist(Path.of("/MusicFolder/artist/album").toFile()));
        assertEquals("artist", parser.guessArtist(Path.of("/MusicFolder/artist/album/song.mp3").toFile()));

        List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(new File("/MusicFolder"), "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertThrows(NullPointerException.class, () -> parser.guessArtist(Path.of("/").toFile()));
        assertThrows(NullPointerException.class, () -> parser.guessArtist(Path.of("/song.mp3").toFile()));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist").toFile()));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist/song.mp3").toFile()));
        assertNull(parser.guessArtist(Path.of("/MusicFolder/artist/album").toFile()));
        assertEquals("artist", parser.guessArtist(Path.of("/MusicFolder/artist/album/song.mp3").toFile()));
    }

    @Test
    void testGuessAlbum() {
        assertThrows(NullPointerException.class, () -> parser.guessAlbum(Path.of("/").toFile(), "artist"));
        assertEquals("", parser.guessAlbum(Path.of("/song.mp3").toFile(), "artist"));
        assertEquals("MusicFolder", parser.guessAlbum(Path.of("/MusicFolder/artist").toFile(), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/song.mp3").toFile(), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/album").toFile(), "artist"));
        assertEquals("album", parser.guessAlbum(Path.of("/MusicFolder/artist/album/song.mp3").toFile(), "artist"));

        List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(new File("/MusicFolder"), "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertThrows(NullPointerException.class, () -> parser.guessAlbum(Path.of("/").toFile(), "artist"));
        assertEquals("", parser.guessAlbum(Path.of("/song.mp3").toFile(), "artist"));
        assertNull(parser.guessAlbum(Path.of("/MusicFolder/artist").toFile(), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/song.mp3").toFile(), "artist"));
        assertEquals("artist", parser.guessAlbum(Path.of("/MusicFolder/artist/album").toFile(), "artist"));
        assertEquals("album", parser.guessAlbum(Path.of("/MusicFolder/artist/album/song.mp3").toFile(), "artist"));
    }

    @Test
    void testGuessTitle() {
        assertEquals("", parser.guessTitle(Path.of("/").toFile()));
        assertEquals("song", parser.guessTitle(Path.of("/song.mp3").toFile()));
        assertEquals("artist", parser.guessTitle(Path.of("/MusicFolder/artist").toFile()));
        assertEquals("song", parser.guessTitle(Path.of("/MusicFolder/artist/song.mp3").toFile()));
        assertEquals("album", parser.guessTitle(Path.of("/MusicFolder/artist/album").toFile()));
        assertEquals("song", parser.guessTitle(Path.of("/MusicFolder/artist/album/song.mp3").toFile()));
    }

    @Test
    void testIsRoot() {
        assertFalse(parser.isRoot(Path.of("/MusicFolder/artist").toFile()));
        assertFalse(parser.isRoot(Path.of("/MusicFolder").toFile()));
        assertFalse(parser.isRoot(Path.of("/").toFile()));
        List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(new File("/MusicFolder"), "MusicFolder", true, null));
        Mockito.when(musicFolderService.getAllMusicFolders(false, true)).thenReturn(musicFolders);
        assertFalse(parser.isRoot(Path.of("/MusicFolder/artist").toFile()));
        assertTrue(parser.isRoot(Path.of("/MusicFolder").toFile()));
        assertFalse(parser.isRoot(Path.of("/").toFile()));
    }
}
