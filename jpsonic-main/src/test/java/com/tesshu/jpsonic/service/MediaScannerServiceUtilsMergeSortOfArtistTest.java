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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.MediaScannerServiceUtilsTestUtils.invokeUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import org.airsonic.player.AbstractNeedsScan;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaScannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * If two or more SORTs exist in one name, unify them into one SORT.
 */
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.AvoidLiteralsInIfCondition", "PMD.NPathComplexity" })
/*
 * In the testing class, it may be less readable.
 */
public class MediaScannerServiceUtilsMergeSortOfArtistTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Merge"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Duplicate", true, new Date()));
    }

    @Autowired
    private JMediaFileDao mediaFileDao;

    @Autowired
    private JArtistDao artistDao;

    @Autowired
    private JAlbumDao albumDao;

    @Autowired
    private MediaScannerServiceUtils utils;

    @Autowired
    private MediaScannerService mediaScannerService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        Date now = new Date();
        mediaScannerService.setJpsonicCleansingProcess(false);

        populateDatabaseOnlyOnce(null, () -> {
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
            albums.forEach(a -> {
                List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPath(), false);
                files.forEach(m -> {
                    if ("file10".equals(m.getName()) || "file12".equals(m.getName()) || "file14".equals(m.getName())
                            || "file17".equals(m.getName())) {
                        m.setChanged(now);
                        mediaFileDao.createOrUpdateMediaFile(m);
                    }
                });
            });
            return true;
        });

        mediaScannerService.setJpsonicCleansingProcess(true);

    }

    @Test
    public void testMergeSortOfArtist() throws ExecutionException {

        invokeUtils(utils, "mergeSortOfArtist");

        List<MediaFile> artists = mediaFileDao.getArtistAll(MUSIC_FOLDERS);
        assertEquals(3, artists.size());

        artists.forEach(m -> {
            switch (m.getName()) {
            case "case10":
                assertEquals("case10", m.getArtistReading());
                assertEquals("artistT", m.getArtistSort());
                break;
            case "case11":
                assertEquals("case11", m.getArtistReading());
                assertEquals("artistU", m.getArtistSort());
                break;
            case "ARTIST":
                assertEquals("ARTIST", m.getArtistReading());
                assertNull(m.getArtistSort());
                break;

            default:
                fail();
                break;
            }
        });

        List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, MUSIC_FOLDERS);
        assertEquals(10, artistID3s.size());

        assertEquals("ARTIST", artistID3s.get(0).getName());
        assertEquals("case01", artistID3s.get(1).getName());
        assertEquals("case02", artistID3s.get(2).getName());
        assertEquals("case03", artistID3s.get(3).getName());
        assertEquals("case05", artistID3s.get(4).getName());
        assertEquals("case06", artistID3s.get(5).getName());
        assertEquals("case08", artistID3s.get(6).getName());
        assertEquals("case09", artistID3s.get(7).getName());
        assertEquals("case10", artistID3s.get(8).getName());
        assertEquals("case11", artistID3s.get(9).getName());

        assertEquals("ARTIST", artistID3s.get(0).getReading());
        assertEquals("case01", artistID3s.get(1).getReading());
        assertEquals("case02", artistID3s.get(2).getReading());
        assertEquals("case03", artistID3s.get(3).getReading());
        assertEquals("case05", artistID3s.get(4).getReading());
        assertEquals("case06", artistID3s.get(5).getReading());
        assertEquals("case08", artistID3s.get(6).getReading());
        assertEquals("case09", artistID3s.get(7).getReading());
        assertEquals("case10", artistID3s.get(8).getReading());
        assertEquals("case11", artistID3s.get(9).getReading());

        assertNull(artistID3s.get(0).getSort());
        assertEquals("artistA", artistID3s.get(1).getSort());
        assertEquals("artistD", artistID3s.get(2).getSort());
        assertEquals("artistE", artistID3s.get(3).getSort());
        assertEquals("artistJ", artistID3s.get(4).getSort());
        assertEquals("artistL", artistID3s.get(5).getSort());
        assertEquals("artistO", artistID3s.get(6).getSort());
        assertEquals("artistQ", artistID3s.get(7).getSort());
        assertEquals("artistT", artistID3s.get(8).getSort());
        assertEquals("artistU", artistID3s.get(9).getSort());

        artists.stream().filter(m -> "case10".equals(m.getName())).findFirst().ifPresent(m -> {
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPath(), false);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            assertEquals("case10", album.getArtist());
            assertEquals("case10", album.getArtistReading());
            assertEquals("artistT", album.getArtistSort());
        });

        artists.stream().filter(m -> "case11".equals(m.getName())).findFirst().ifPresent(m -> {
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPath(), false);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            assertEquals("case11", album.getArtist());
            assertEquals("case11", album.getArtistReading());
            assertEquals("artistU", album.getArtistSort());
        });

        artists.stream().filter(m -> "ARTIST".equals(m.getName())).findFirst().ifPresent(m -> {
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPath(), false);
            assertEquals(9, albums.size());

            albums.forEach(album -> {
                switch (album.getName()) {
                case "ALBUM1":
                    assertEquals("case01", album.getArtist());
                    assertEquals("case01", album.getArtistReading());
                    assertEquals("artistA", album.getArtistSort());
                    break;
                case "ALBUM2":
                    assertEquals("case02", album.getArtist());
                    assertEquals("case02", album.getArtistReading());
                    assertEquals("artistD", album.getArtistSort());
                    break;
                case "ALBUM3":
                    assertEquals("case03", album.getArtist());
                    assertEquals("case03", album.getArtistReading());
                    assertEquals("artistE", album.getArtistSort());
                    break;
                case "ALBUM4":
                    assertEquals("ARTIST", album.getArtist());
                    assertEquals("ARTIST", album.getArtistReading());
                    assertNull(album.getArtistSort());
                    break;
                case "ALBUM5":
                    assertEquals("case05", album.getArtist());
                    assertEquals("case05", album.getArtistReading());
                    assertEquals("artistJ", album.getArtistSort());
                    break;
                case "ALBUM6":
                    assertEquals("case06", album.getArtist());
                    assertEquals("case06", album.getArtistReading());
                    assertEquals("artistL", album.getArtistSort());
                    break;
                case "ALBUM7":
                    assertEquals("ARTIST", album.getArtist());
                    assertEquals("ARTIST", album.getArtistReading());
                    assertNull(album.getArtistSort());
                    break;
                case "ALBUM8":
                    assertEquals("case08", album.getArtist());
                    assertEquals("case08", album.getArtistReading());
                    assertEquals("artistO", album.getArtistSort());
                    break;
                case "ALBUM9":
                    assertEquals("case09", album.getArtist());
                    assertEquals("case09", album.getArtistReading());
                    assertEquals("artistQ", album.getArtistSort());
                    break;

                default:
                    fail();
                    break;
                }

            });

            List<MediaFile> songs = albums.stream()
                    .flatMap(al -> mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, al.getPath(), false).stream())
                    .collect(Collectors.toList());
            assertEquals(15, songs.size());

            songs.forEach(s -> {

                switch (s.getName()) {

                case "file1":
                    assertEquals("case01", s.getArtist());
                    assertEquals("case01", s.getArtistReading());
                    assertEquals("artistA", s.getArtistSort());
                    assertEquals("case01", s.getAlbumArtist());
                    assertEquals("case01", s.getAlbumArtistReading());
                    assertEquals("artistA", s.getAlbumArtistSort());
                    assertEquals("case01", s.getComposer());
                    assertEquals("artistA", s.getComposerSort());
                    break;
                case "file2":
                    assertEquals("case02", s.getArtist());
                    assertEquals("case02", s.getArtistReading());
                    assertEquals("artistD", s.getArtistSort());
                    assertEquals("case02", s.getAlbumArtist());
                    assertEquals("case02", s.getAlbumArtistReading());
                    assertEquals("artistD", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file3":
                    assertEquals("case03", s.getArtist());
                    assertEquals("case03", s.getArtistReading());
                    assertEquals("artistE", s.getArtistSort());
                    assertEquals("case03", s.getAlbumArtist());
                    assertEquals("case03", s.getAlbumArtistReading());
                    assertEquals("artistE", s.getAlbumArtistSort());
                    assertEquals("case03", s.getComposer());
                    assertEquals("artistE", s.getComposerSort());
                    break;
                case "file4":
                    assertEquals("ARTIST", s.getArtist());
                    assertEquals("ARTIST", s.getArtistReading());
                    assertNull(s.getArtistSort());
                    assertEquals("ARTIST", s.getAlbumArtist());
                    assertEquals("ARTIST", s.getAlbumArtistReading());
                    assertNull(s.getAlbumArtistSort());
                    assertEquals("case04", s.getComposer());
                    assertEquals("artistH", s.getComposerSort());
                    break;
                case "file5":
                    assertEquals("case04", s.getArtist());
                    assertEquals("case04", s.getArtistReading());
                    assertEquals("artistH", s.getArtistSort());
                    assertEquals("ARTIST", s.getAlbumArtist());
                    assertEquals("ARTIST", s.getAlbumArtistReading());
                    assertNull(s.getAlbumArtistSort());
                    assertEquals("case04", s.getComposer());
                    assertEquals("artistH", s.getComposerSort());
                    break;
                case "file6":
                    assertEquals("case05", s.getArtist());
                    assertEquals("case05", s.getArtistReading());
                    assertEquals("artistJ", s.getArtistSort());
                    assertEquals("case05", s.getAlbumArtist());
                    assertEquals("case05", s.getAlbumArtistReading());
                    assertEquals("artistJ", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file7":
                    assertEquals("case05", s.getArtist());
                    assertEquals("case05", s.getArtistReading());
                    assertEquals("artistJ", s.getArtistSort());
                    assertEquals("case05", s.getAlbumArtist());
                    assertEquals("case05", s.getAlbumArtistReading());
                    assertEquals("artistJ", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file8":
                    assertEquals("case06", s.getArtist());
                    assertEquals("case06", s.getArtistReading());
                    assertEquals("artistL", s.getArtistSort());
                    assertEquals("case06", s.getAlbumArtist());
                    assertEquals("case06", s.getAlbumArtistReading());
                    assertEquals("artistL", s.getAlbumArtistSort());
                    assertEquals("case06", s.getComposer());
                    assertEquals("artistL", s.getComposerSort());
                    break;
                case "file9":
                    assertEquals("case06", s.getArtist());
                    assertEquals("case06", s.getArtistReading());
                    assertEquals("artistL", s.getArtistSort());
                    assertEquals("case06", s.getAlbumArtist());
                    assertEquals("case06", s.getAlbumArtistReading());
                    assertEquals("artistL", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file10":
                    assertEquals("ARTIST", s.getArtist());
                    assertEquals("ARTIST", s.getArtistReading());
                    assertNull(s.getArtistSort());
                    assertEquals("ARTIST", s.getAlbumArtist());
                    assertEquals("ARTIST", s.getAlbumArtistReading());
                    assertNull(s.getAlbumArtistSort());
                    assertEquals("case07", s.getComposer());
                    assertEquals("artistM", s.getComposerSort());
                    break;
                case "file11":
                    assertEquals("case07", s.getArtist());
                    assertEquals("case07", s.getArtistReading());
                    assertEquals("artistM", s.getArtistSort());
                    assertEquals("ARTIST", s.getAlbumArtist());
                    assertEquals("ARTIST", s.getAlbumArtistReading());
                    assertNull(s.getAlbumArtistSort());
                    assertEquals("case07", s.getComposer());
                    assertEquals("artistM", s.getComposerSort());
                    break;
                case "file12":
                    assertEquals("case08", s.getArtist());
                    assertEquals("case08", s.getArtistReading());
                    assertEquals("artistO", s.getArtistSort());
                    assertEquals("case08", s.getAlbumArtist());
                    assertEquals("case08", s.getAlbumArtistReading());
                    assertEquals("artistO", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file13":
                    assertEquals("case08", s.getArtist());
                    assertEquals("case08", s.getArtistReading());
                    assertEquals("artistO", s.getArtistSort());
                    assertEquals("case08", s.getAlbumArtist());
                    assertEquals("case08", s.getAlbumArtistReading());
                    assertEquals("artistO", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;
                case "file14":
                    assertEquals("case09", s.getArtist());
                    assertEquals("case09", s.getArtistReading());
                    assertEquals("artistQ", s.getArtistSort());
                    assertEquals("case09", s.getAlbumArtist());
                    assertEquals("case09", s.getAlbumArtistReading());
                    assertEquals("artistQ", s.getAlbumArtistSort());
                    assertEquals("case09", s.getComposer());
                    assertEquals("artistQ", s.getComposerSort());
                    break;
                case "file15":
                    assertEquals("case09", s.getArtist());
                    assertEquals("case09", s.getArtistReading());
                    assertEquals("artistQ", s.getArtistSort());
                    assertEquals("case09", s.getAlbumArtist());
                    assertEquals("case09", s.getAlbumArtistReading());
                    assertEquals("artistQ", s.getAlbumArtistSort());
                    assertNull(s.getComposer());
                    assertNull(s.getComposerSort());
                    break;

                default:
                    fail();
                    break;
                }
            });

        });

        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, MUSIC_FOLDERS);

        assertEquals(11, albumId3s.size());

        assertEquals("ALBUM1", albumId3s.get(0).getName());
        assertEquals("ALBUM10", albumId3s.get(1).getName());
        assertEquals("ALBUM11", albumId3s.get(2).getName());
        assertEquals("ALBUM2", albumId3s.get(3).getName());
        assertEquals("ALBUM3", albumId3s.get(4).getName());
        assertEquals("ALBUM4", albumId3s.get(5).getName());
        assertEquals("ALBUM5", albumId3s.get(6).getName());
        assertEquals("ALBUM6", albumId3s.get(7).getName());
        assertEquals("ALBUM7", albumId3s.get(8).getName());
        assertEquals("ALBUM8", albumId3s.get(9).getName());
        assertEquals("ALBUM9", albumId3s.get(10).getName());

        assertEquals("case01", albumId3s.get(0).getArtist());
        assertEquals("case10", albumId3s.get(1).getArtist());
        assertEquals("case11", albumId3s.get(2).getArtist());
        assertEquals("case02", albumId3s.get(3).getArtist());
        assertEquals("case03", albumId3s.get(4).getArtist());
        assertEquals("ARTIST", albumId3s.get(5).getArtist());
        assertEquals("case05", albumId3s.get(6).getArtist());
        assertEquals("case06", albumId3s.get(7).getArtist());
        assertEquals("ARTIST", albumId3s.get(8).getArtist());
        assertEquals("case08", albumId3s.get(9).getArtist());
        assertEquals("case09", albumId3s.get(10).getArtist());

        assertEquals("case01", albumId3s.get(0).getArtistReading());
        assertEquals("case10", albumId3s.get(1).getArtistReading());
        assertEquals("case11", albumId3s.get(2).getArtistReading());
        assertEquals("case02", albumId3s.get(3).getArtistReading());
        assertEquals("case03", albumId3s.get(4).getArtistReading());
        assertEquals("ARTIST", albumId3s.get(5).getArtistReading());
        assertEquals("case05", albumId3s.get(6).getArtistReading());
        assertEquals("case06", albumId3s.get(7).getArtistReading());
        assertEquals("ARTIST", albumId3s.get(8).getArtistReading());
        assertEquals("case08", albumId3s.get(9).getArtistReading());
        assertEquals("case09", albumId3s.get(10).getArtistReading());

        assertEquals("artistA", albumId3s.get(0).getArtistSort());
        assertEquals("artistT", albumId3s.get(1).getArtistSort());
        assertEquals("artistU", albumId3s.get(2).getArtistSort());
        assertEquals("artistD", albumId3s.get(3).getArtistSort());
        assertEquals("artistE", albumId3s.get(4).getArtistSort());
        assertNull(albumId3s.get(5).getArtistSort());
        assertEquals("artistJ", albumId3s.get(6).getArtistSort());
        assertEquals("artistL", albumId3s.get(7).getArtistSort());
        assertNull(albumId3s.get(8).getArtistSort());
        assertEquals("artistO", albumId3s.get(9).getArtistSort());
        assertEquals("artistQ", albumId3s.get(10).getArtistSort());

    }

}
