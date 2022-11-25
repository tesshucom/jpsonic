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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.AvoidLiteralsInIfCondition", "PMD.NPathComplexity" })
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class SortProcedureServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(SortProcedureServiceTest.class);

    /**
     * If SORT exists for one name and null-sort data exists, unify it to SORT.
     */
    @Nested
    @Order(1)
    class CompensateSortOfArtistTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays.asList(new MusicFolder(1,
                resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Compensation"), "Duplicate", true, now()));

        @Autowired
        private JMediaFileDao mediaFileDao;

        @Autowired
        private JArtistDao artistDao;

        @Autowired
        private SortProcedureService sortProcedureService;

        @Autowired
        private ScannerStateServiceImpl scannerStateService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            scannerStateService.enableCleansing(false);
            populateDatabase();
            scannerStateService.enableCleansing(true);
        }

        @Test
        @DisabledOnOs(OS.LINUX)
        void testCompensateSortOfArtistOnWindows() throws ExecutionException {

            sortProcedureService.mergeSortOfArtist();
            sortProcedureService.copySortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
            MediaFile artist = artists.stream().filter(m -> "ARTIST".equals(m.getArtist())).findFirst().get();
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(),
                    false);
            assertEquals(3, files.size());

            files.forEach(m -> {
                switch (m.getName()) {
                case "file1":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("近衛秀麿", m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "file2":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("中山晋平", m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "file3":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("ARTIST", m.getArtist());
                    assertNull(m.getArtistSort());
                    assertEquals("世阿弥", m.getComposer());
                    assertNull(m.getComposerSort());
                    break;

                default:
                    fail();
                    break;
                }
            });

            artist = artists.stream().filter(m -> "山田耕筰".equals(m.getArtist())).findFirst().get();
            albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(), false);
            assertEquals(1, files.size());
            assertEquals("file4", files.get(0).getName());
            assertEquals("山田耕筰", files.get(0).getArtist());
            assertNull(files.get(0).getArtistSort());

            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(2, artistID3s.size());
            artistID3s.forEach(a -> {
                switch (a.getName()) {
                case "近衛秀麿":
                case "山田耕筰":
                    assertNull(a.getSort());
                    break;
                default:
                    fail();
                    break;
                }
            });

            sortProcedureService.compensateSortOfArtist();

            artists = mediaFileDao.getArtistAll(musicFolders);
            artist = artists.stream().filter(m -> "ARTIST".equals(m.getArtist())).findFirst().get();
            albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(), false);
            assertEquals(3, files.size());

            files.forEach(m -> {
                switch (m.getName()) {
                case "file1":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertEquals("コノエヒデマロ", m.getAlbumArtistSort());
                    assertEquals("近衛秀麿", m.getArtist());
                    assertEquals("コノエヒデマロ", m.getArtistSort());
                    break;
                case "file2":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertEquals("コノエヒデマロ", m.getAlbumArtistSort());
                    assertEquals("中山晋平", m.getArtist());
                    assertEquals("ナカヤマシンペイ", m.getArtistSort());
                    break;
                case "file3":
                    assertEquals("近衛秀麿", m.getAlbumArtist());
                    assertEquals("コノエヒデマロ", m.getAlbumArtistSort());
                    assertEquals("ARTIST", m.getArtist());
                    assertEquals("ARTIST", m.getArtistSort());
                    assertEquals("世阿弥", m.getComposer());
                    assertEquals("ゼアミ", m.getComposerSort());
                    break;

                default:
                    fail();
                    break;
                }
            });

            artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(2, artistID3s.size());
            artistID3s.forEach(a -> {
                switch (a.getName()) {
                case "近衛秀麿":
                    assertEquals("コノエヒデマロ", a.getSort());
                    break;
                case "山田耕筰":
                    assertEquals("ヤマダコウサク", a.getSort());
                    break;
                default:
                    fail();
                    break;
                }
            });
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testCompensateSortOfArtistOnLinux() throws ExecutionException {

            sortProcedureService.mergeSortOfArtist();
            sortProcedureService.copySortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
            MediaFile artist = artists.stream().filter(m -> "ARTIST".equals(m.getArtist())).findFirst().get();
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(),
                    false);
            assertEquals(3, files.size());

            files.forEach(m -> {
                switch (m.getName()) {
                case "file1":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("近衛秀麿", m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "file2":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("中山晋平", m.getArtist());
                    assertNull(m.getArtistSort());
                    break;
                case "file3":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertNull(m.getAlbumArtistSort());
                    assertEquals("ARTIST", m.getArtist());
                    assertNull(m.getArtistSort());
                    assertEquals("世阿弥", m.getComposer());
                    assertNull(m.getComposerSort());
                    break;

                default:
                    fail();
                    break;
                }
            });

            artist = artists.stream().filter(m -> "山田耕筰".equals(m.getArtist())).findFirst().get();
            albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(), false);
            assertEquals(1, files.size());
            assertEquals("file4", files.get(0).getName());
            assertEquals("山田耕筰", files.get(0).getArtist());
            assertNull(files.get(0).getArtistSort());

            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(2, artistID3s.size());
            artistID3s.stream().forEach(a -> LOG.info(a.getName()));
            artistID3s.forEach(a -> {
                switch (a.getName()) {
                case "近衛秀麿":
                case "山田耕筰":
                case "ARTIST":
                case "中山晋平":
                    assertNull(a.getSort());
                    break;
                default:
                    fail();
                    break;
                }
            });

            sortProcedureService.compensateSortOfArtist();

            artists = mediaFileDao.getArtistAll(musicFolders);
            artist = artists.stream().filter(m -> "ARTIST".equals(m.getArtist())).findFirst().get();
            albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artist.getPathString(), false);
            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPathString(), false);
            assertEquals(3, files.size());

            files.stream().forEach(m -> LOG.info(m.getName() + ", " + m.getAlbumArtist() + ", " + m.getArtist()));
            files.forEach(m -> {
                switch (m.getName()) {
                case "file1":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertEquals("ナカヤマシンペイ", m.getAlbumArtistSort());
                    assertEquals("近衛秀麿", m.getArtist());
                    assertEquals("コノエヒデマロ", m.getArtistSort());
                    break;
                case "file2":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertEquals("ナカヤマシンペイ", m.getAlbumArtistSort());
                    assertEquals("中山晋平", m.getArtist());
                    assertEquals("ナカヤマシンペイ", m.getArtistSort());
                    break;
                case "file3":
                    assertEquals("中山晋平", m.getAlbumArtist());
                    assertEquals("ナカヤマシンペイ", m.getAlbumArtistSort());
                    assertEquals("ARTIST", m.getArtist());
                    assertEquals("ARTIST", m.getArtistSort());
                    assertEquals("世阿弥", m.getComposer());
                    assertEquals("ゼアミ", m.getComposerSort());
                    break;

                default:
                    // fail(); TODO
                    break;
                }
            });

            artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(2, artistID3s.size());
            artistID3s.forEach(a -> {
                switch (a.getName()) {
                case "近衛秀麿":
                    assertEquals("コノエヒデマロ", a.getSort());
                    break;
                case "山田耕筰":
                    assertEquals("ヤマダコウサク", a.getSort());
                    break;
                case "中山晋平":
                    assertEquals("ナカヤマシンペイ", a.getSort());
                    break;
                case "世阿弥":
                    assertEquals("ゼアミ", a.getSort());
                    break;
                default:
                    // fail(); TODO
                    break;
                }
            });
        }
    }

    /**
     * If SORT exists for one name and null-sort data exists, unify it to SORT.
     */
    @Nested
    @Order(2)
    class CopySortOfArtistTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays.asList(
                new MusicFolder(1, resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Copy"), "Duplicate", true, now()));

        @Autowired
        private JMediaFileDao mediaFileDao;

        @Autowired
        private JArtistDao artistDao;

        @Autowired
        private SortProcedureService sortProcedureService;

        @Autowired
        private ScannerStateServiceImpl scannerStateService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            scannerStateService.enableCleansing(false);
            populateDatabase();
            scannerStateService.enableCleansing(true);
        }

        @Test
        @DisabledOnOs(OS.LINUX)
        void testCopySortOfArtistOnWindows() throws ExecutionException {

            sortProcedureService.mergeSortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
            assertEquals(1, artists.size());
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(0).getPathString(),
                    false);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPathString(), false);
            assertEquals(2, files.size());
            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(1, artistID3s.size());

            assertEquals("file1", files.get(0).getName());
            assertEquals("case1", files.get(0).getArtist());
            assertNull(files.get(0).getArtistSort());
            assertEquals("file2", files.get(1).getName());
            assertEquals("case1", files.get(1).getArtist());
            assertEquals("artistA", files.get(1).getArtistSort());

            assertEquals("case1", artistID3s.get(0).getName());
            assertNull(artistID3s.get(0).getSort());

            sortProcedureService.copySortOfArtist();

            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPathString(), false);
            artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);

            assertEquals(2, files.size());
            files.forEach(f -> {
                switch (f.getName()) {
                case "file1":
                case "file2":
                    assertEquals("case1", f.getArtist());
                    assertEquals("artistA", f.getArtistSort());
                    break;
                default:
                    fail();
                    break;
                }
            });

            assertEquals(1, artistID3s.size());
            assertEquals("case1", artistID3s.get(0).getName());
            assertEquals("artistA", artistID3s.get(0).getSort());
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testCopySortOfArtistOnLinux() throws ExecutionException {

            sortProcedureService.mergeSortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
            assertEquals(1, artists.size());
            List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(0).getPathString(),
                    false);
            assertEquals(1, albums.size());
            MediaFile album = albums.get(0);
            List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPathString(), false);
            assertEquals(2, files.size());
            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(1, artistID3s.size());

            assertEquals("file2", files.get(0).getName());
            assertEquals("case1", files.get(0).getArtist());
            assertEquals("artistA", files.get(0).getArtistSort());
            assertEquals("file1", files.get(1).getName());
            assertEquals("case1", files.get(1).getArtist());
            assertNull(files.get(1).getArtistSort());

            assertEquals("case1", artistID3s.get(0).getName());
            assertEquals("artistA", artistID3s.get(0).getSort());

            sortProcedureService.copySortOfArtist();

            files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPathString(), false);
            artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);

            assertEquals(2, files.size());
            files.forEach(f -> {
                switch (f.getName()) {
                case "file1":
                case "file2":
                    assertEquals("case1", f.getArtist());
                    assertEquals("artistA", f.getArtistSort());
                    break;
                default:
                    fail();
                    break;
                }
            });

            assertEquals(1, artistID3s.size());
            assertEquals("case1", artistID3s.get(0).getName());
            assertEquals("artistA", artistID3s.get(0).getSort());
        }
    }

    /**
     * If two or more SORTs exist in one name, unify them into one SORT.
     */
    @Nested
    @Order(3)
    // Windows Server 2022 and Ubuntu results are different
    // Windows 10 Home and Windows Server 2022 results are same
    class MergeSortOfArtistTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays.asList(
                new MusicFolder(1, resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Merge"), "Duplicate", true, now()));

        @Autowired
        private JMediaFileDao mediaFileDao;

        @Autowired
        private JArtistDao artistDao;

        @Autowired
        private JAlbumDao albumDao;

        @Autowired
        private SortProcedureService sortProcedureService;

        @Autowired
        private ScannerStateServiceImpl scannerStateService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            scannerStateService.enableCleansing(false);

            assertEquals(0, mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders).size());
            assertEquals(0, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders).size());

            Instant now = now();
            populateDatabase(null, () -> {
                List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
                albums.forEach(a -> {
                    List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPathString(), false);
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

            scannerStateService.enableCleansing(true);
        }

        @Test
        @DisabledOnOs(OS.LINUX)
        void testMergeSortOfArtistOnWindows() throws ExecutionException {

            // Windows 10 Home
            // for Windows Server 2022(ver.20220426.1)

            assertEquals(11, mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders).size());
            assertEquals(10, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders).size()); // OS
                                                                                                          // dipendent
            LOG.info(" *** Artist(id, name)");
            artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders)
                    .forEach(a -> LOG.info(a.getId() + ", " + a.getName()));

            LOG.info(" *** Artist(id, name)");
            artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders)
                    .forEach(a -> LOG.info(a.getId() + ", " + a.getName()));

            sortProcedureService.mergeSortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
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

            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(10, artistID3s.size()); // OS dipendent

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
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
                assertEquals(1, albums.size());
                MediaFile album = albums.get(0);
                assertEquals("case10", album.getArtist());
                assertEquals("case10", album.getArtistReading());
                assertEquals("artistT", album.getArtistSort());
            });

            artists.stream().filter(m -> "case11".equals(m.getName())).findFirst().ifPresent(m -> {
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
                assertEquals(1, albums.size());
                MediaFile album = albums.get(0);
                assertEquals("case11", album.getArtist());
                assertEquals("case11", album.getArtistReading());
                assertEquals("artistU", album.getArtistSort());
            });

            artists.stream().filter(m -> "ARTIST".equals(m.getName())).findFirst().ifPresent(m -> {
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
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

                List<MediaFile> songs = albums.stream().flatMap(
                        al -> mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, al.getPathString(), false).stream())
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

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);

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

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testMergeSortOfArtistOnLinux() throws ExecutionException {

            // for Ubuntu 20.04.4(ver.20220425.1)

            assertEquals(11, mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders).size());
            assertEquals(11, artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders).size()); // OS
                                                                                                          // dipendent
            LOG.info(" *** Artist(id, name)");
            artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders)
                    .forEach(a -> LOG.info(a.getId() + ", " + a.getName()));

            sortProcedureService.mergeSortOfArtist();

            List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
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

            List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
            assertEquals(11, artistID3s.size()); // OS dipendent

            artistID3s.stream().forEach(a -> LOG.info(a.getName()));
            assertEquals("case01", artistID3s.get(0).getReading());
            assertEquals("case02", artistID3s.get(1).getReading());
            assertEquals("case03", artistID3s.get(2).getReading());
            assertEquals("case04", artistID3s.get(3).getReading());
            assertEquals("case05", artistID3s.get(4).getReading());
            assertEquals("case06", artistID3s.get(5).getReading());
            assertEquals("case07", artistID3s.get(6).getReading());
            assertEquals("case08", artistID3s.get(7).getReading());
            assertEquals("case09", artistID3s.get(8).getReading());
            assertEquals("case10", artistID3s.get(9).getReading());
            assertEquals("case11", artistID3s.get(10).getReading());

            artistID3s.stream().forEach(a -> LOG.info(a.getReading()));
            assertEquals("artistA", artistID3s.get(0).getSort());
            assertEquals("artistD", artistID3s.get(1).getSort());
            assertEquals("artistE", artistID3s.get(2).getSort());
            assertEquals("artistH", artistID3s.get(3).getSort());
            assertEquals("artistJ", artistID3s.get(4).getSort());
            assertEquals("artistL", artistID3s.get(5).getSort());
            assertEquals("artistN", artistID3s.get(6).getSort());
            assertEquals("artistO", artistID3s.get(7).getSort());
            assertEquals("artistQ", artistID3s.get(8).getSort());
            assertEquals("artistT", artistID3s.get(9).getSort());
            assertEquals("artistU", artistID3s.get(10).getSort());

            artistID3s.stream().forEach(a -> LOG.info(a.getSort()));
            assertEquals("artistA", artistID3s.get(0).getSort());
            assertEquals("artistD", artistID3s.get(1).getSort());
            assertEquals("artistE", artistID3s.get(2).getSort());
            assertEquals("artistH", artistID3s.get(3).getSort());
            assertEquals("artistJ", artistID3s.get(4).getSort());
            assertEquals("artistL", artistID3s.get(5).getSort());
            assertEquals("artistN", artistID3s.get(6).getSort());
            assertEquals("artistO", artistID3s.get(7).getSort());
            assertEquals("artistQ", artistID3s.get(8).getSort());
            assertEquals("artistT", artistID3s.get(9).getSort());
            assertEquals("artistU", artistID3s.get(10).getSort());

            artists.stream().filter(m -> "case10".equals(m.getName())).findFirst().ifPresent(m -> {
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
                assertEquals(1, albums.size());
                MediaFile album = albums.get(0);
                assertEquals("case10", album.getArtist());
                assertEquals("case10", album.getArtistReading());
                assertEquals("artistT", album.getArtistSort());
            });

            artists.stream().filter(m -> "case11".equals(m.getName())).findFirst().ifPresent(m -> {
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
                assertEquals(1, albums.size());
                MediaFile album = albums.get(0);
                assertEquals("case11", album.getArtist());
                assertEquals("case11", album.getArtistReading());
                assertEquals("artistU", album.getArtistSort());
            });

            artists.stream().filter(m -> "case01".equals(m.getName())).findFirst().ifPresent(m -> {
                List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, m.getPathString(), false);
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
                        assertEquals("case04", album.getArtist());
                        assertEquals("case04", album.getArtistReading());
                        assertEquals("artistH", album.getArtistSort());
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
                        assertEquals("case07", album.getArtist());
                        assertEquals("case07", album.getArtistReading());
                        assertEquals("artistN", album.getArtistSort());
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

                List<MediaFile> songs = albums.stream().flatMap(
                        al -> mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, al.getPathString(), false).stream())
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

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);

            assertEquals(11, albumId3s.size());

            albumId3s.stream().forEach(a -> LOG.info(a.getName()));
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

            albumId3s.stream().forEach(a -> LOG.info(a.getArtist()));
            assertEquals("case01", albumId3s.get(0).getArtist());
            assertEquals("case10", albumId3s.get(1).getArtist());
            assertEquals("case11", albumId3s.get(2).getArtist());
            assertEquals("case02", albumId3s.get(3).getArtist());
            assertEquals("case03", albumId3s.get(4).getArtist());
            assertEquals("case04", albumId3s.get(5).getArtist());
            assertEquals("case05", albumId3s.get(6).getArtist());
            assertEquals("case06", albumId3s.get(7).getArtist());
            assertEquals("case07", albumId3s.get(8).getArtist());
            assertEquals("case08", albumId3s.get(9).getArtist());
            assertEquals("case09", albumId3s.get(10).getArtist());

            albumId3s.stream().forEach(a -> LOG.info(a.getArtistReading()));
            assertEquals("case01", albumId3s.get(0).getArtistReading());
            assertEquals("case10", albumId3s.get(1).getArtistReading());
            assertEquals("case11", albumId3s.get(2).getArtistReading());
            assertEquals("case02", albumId3s.get(3).getArtistReading());
            assertEquals("case03", albumId3s.get(4).getArtistReading());
            assertEquals("case04", albumId3s.get(5).getArtistReading());
            assertEquals("case05", albumId3s.get(6).getArtistReading());
            assertEquals("case06", albumId3s.get(7).getArtistReading());
            assertEquals("case07", albumId3s.get(8).getArtistReading());
            assertEquals("case08", albumId3s.get(9).getArtistReading());
            assertEquals("case09", albumId3s.get(10).getArtistReading());

            albumId3s.stream().forEach(a -> LOG.info(a.getArtistSort()));
            assertEquals("artistA", albumId3s.get(0).getArtistSort());
            assertEquals("artistT", albumId3s.get(1).getArtistSort());
            assertEquals("artistU", albumId3s.get(2).getArtistSort());
            assertEquals("artistD", albumId3s.get(3).getArtistSort());
            assertEquals("artistE", albumId3s.get(4).getArtistSort());
            assertEquals("artistH", albumId3s.get(5).getArtistSort());
            assertEquals("artistJ", albumId3s.get(6).getArtistSort());
            assertEquals("artistL", albumId3s.get(7).getArtistSort());
            assertEquals("artistN", albumId3s.get(8).getArtistSort());
            assertEquals("artistO", albumId3s.get(9).getArtistSort());
            assertEquals("artistQ", albumId3s.get(10).getArtistSort());
        }
    }

    @Nested
    @Order(4)
    // Windows Server 2022 and Ubuntu results are different
    // Windows 10 Home and Ubuntu results are same
    class UpdateSortOfAlbumTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Cleansing/AlbumSort"), "Duplicate", true, now()));

        @Autowired
        private JMediaFileDao mediaFileDao;

        @Autowired
        private JAlbumDao albumDao;

        @Autowired
        private SortProcedureService sortProcedureService;

        @Autowired
        private ScannerStateServiceImpl scannerStateService;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {

            scannerStateService.enableCleansing(false);

            // Update the date of a particular file to cause a merge
            String latestMediaFileTitle1 = "file1";
            String latestMediaFileTitle2 = "file4";

            populateDatabase(null, () -> {
                List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
                Instant now = now();
                albums.forEach(a -> {
                    List<MediaFile> songs = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPathString(), false);
                    songs.forEach(m -> {
                        if (latestMediaFileTitle1.equals(m.getTitle()) || latestMediaFileTitle2.equals(m.getTitle())) {
                            m.setChanged(now);
                            mediaFileDao.createOrUpdateMediaFile(m);
                        }
                    });
                });
                return true;
            });
            scannerStateService.enableCleansing(true);
        }

        @Test
        @DisabledOnOs({ OS.LINUX, OS.WINDOWS }) // Flaky in Windows Server 2022 (#1645)
        void testUpdateSortOfAlbumOnWindows() throws ExecutionException {

            // for Windows Server 2022(ver.20220426.1) Results may differ from Desktop Windows.

            LOG.info(" *** Before merge(id, name, sort)");
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());
            albums.forEach(m -> LOG.info("MediaFile.ALBUM:" + m.getId() + ", " + m.getAlbumName() + ", "
                    + m.getAlbumSort() + ", " + m.getPathString()));

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());
            albumId3s.forEach(a -> LOG.info("Album:" + a.getId() + ", " + a.getName() + ", " + a.getNameSort()));

            // to be merge
            assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
            assertEquals(1L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(1L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());

            assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());

            sortProcedureService.mergeSortOfAlbum();

            LOG.info(" *** After merge(id, name, sort)");
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());
            albums.forEach(m -> LOG.info("MediaFile.ALBUM:" + m.getId() + ", " + m.getAlbumName() + ", "
                    + m.getAlbumSort() + ", " + m.getPathString()));

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());
            albumId3s.forEach(a -> LOG.info("Album:" + a.getId() + ", " + a.getName() + ", " + a.getNameSort()));

            // merged
            assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
            assertEquals(0L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count()); // merged
            assertEquals(2L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count()); // merged

            assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
            assertEquals(0L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count()); // merged
            assertEquals(2L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count()); // merged

            sortProcedureService.copySortOfAlbum();

            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());

            // copied
            assertEquals(2L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count()); // copied
            assertEquals(1L, albums.stream().filter(m -> m.getAlbumSort() == null).count()); // copied
            assertEquals(0L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());

            assertEquals(2L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count()); // copied
            assertEquals(1L, albumId3s.stream().filter(a -> a.getNameSort() == null).count()); // copied
            assertEquals(0L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());

            sortProcedureService.compensateSortOfAlbum();

            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());

            // compensated
            assertEquals(2L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(0L, albums.stream().filter(m -> m.getAlbumSort() == null).count()); // compensated
            assertEquals(0L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());
            assertEquals(1L, albums.stream().filter(m -> "ニホンゴノアルバムメイ".equals(m.getAlbumSort())).count());

            assertEquals(2L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(0L, albumId3s.stream().filter(a -> a.getNameSort() == null).count()); // compensated
            assertEquals(0L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "ニホンゴノアルバムメイ".equals(a.getNameSort())).count());
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testUpdateSortOfAlbumOnLinux() throws ExecutionException {

            // Windows 10 Home
            // for Ubuntu 20.04.4(ver.20220425.1)

            LOG.info(" *** Before merge(id, name, sort)");
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());
            albums.forEach(m -> LOG.info("MediaFile.ALBUM:" + m.getId() + ", " + m.getAlbumName() + ", "
                    + m.getAlbumSort() + ", " + m.getPathString()));

            List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());
            albumId3s.forEach(a -> LOG.info("Album:" + a.getId() + ", " + a.getName() + ", " + a.getNameSort()));

            // to be merge
            assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
            assertEquals(1L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(1L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());

            assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());

            sortProcedureService.mergeSortOfAlbum();

            LOG.info(" *** After merge(id, name, sort)");
            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());
            albums.forEach(m -> LOG.info("MediaFile.ALBUM:" + m.getId() + ", " + m.getAlbumName() + ", "
                    + m.getAlbumSort() + ", " + m.getPathString()));

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());
            albumId3s.forEach(a -> LOG.info("Album:" + a.getId() + ", " + a.getName() + ", " + a.getNameSort()));

            // merged
            assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
            assertEquals(2L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count()); // merged
            assertEquals(0L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count()); // merged

            assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
            assertEquals(2L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count()); // merged
            assertEquals(0L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count()); // merged

            sortProcedureService.copySortOfAlbum();

            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());

            // copied
            assertEquals(2L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count()); // copied
            assertEquals(1L, albums.stream().filter(m -> m.getAlbumSort() == null).count()); // copied
            assertEquals(2L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(0L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());

            assertEquals(2L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count()); // copied
            assertEquals(1L, albumId3s.stream().filter(a -> a.getNameSort() == null).count()); // copied
            assertEquals(2L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(0L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());

            sortProcedureService.compensateSortOfAlbum();

            albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            assertEquals(5, albums.size());

            albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
            assertEquals(5, albumId3s.size());

            // compensated
            assertEquals(2L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
            assertEquals(0L, albums.stream().filter(m -> m.getAlbumSort() == null).count()); // compensated
            assertEquals(2L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
            assertEquals(0L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());
            assertEquals(1L, albums.stream().filter(m -> "ニホンゴノアルバムメイ".equals(m.getAlbumSort())).count());

            assertEquals(2L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
            assertEquals(0L, albumId3s.stream().filter(a -> a.getNameSort() == null).count()); // compensated
            assertEquals(2L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
            assertEquals(0L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());
            assertEquals(1L, albumId3s.stream().filter(a -> "ニホンゴノアルバムメイ".equals(a.getNameSort())).count());
        }
    }
}
