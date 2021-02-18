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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * If SORT exists for one name and null-sort data exists, unify it to SORT.
 */
@SpringBootConfiguration
@ComponentScan(basePackages = { "org.airsonic.player", "com.tesshu.jpsonic" })
@SpringBootTest
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class MediaScannerServiceUtilsCompensateSortOfArtistTest extends AbstractAirsonicHomeTest {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Compensation"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Duplicate", true, new Date()));
    }

    @Autowired
    private JMediaFileDao mediaFileDao;

    @Autowired
    private JArtistDao artistDao;

    @Autowired
    private MediaScannerServiceUtils utils;

    @Autowired
    private MediaScannerService mediaScannerService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @Before
    public void setup() {
        mediaScannerService.setJpsonicCleansingProcess(false);
        populateDatabaseOnlyOnce();
        mediaScannerService.setJpsonicCleansingProcess(true);
    }

    @Test
    public void testCompensateSortOfArtist() throws ExecutionException {

        invokeUtils(utils, "mergeSortOfArtist");
        invokeUtils(utils, "copySortOfArtist");

        List<MediaFile> artists = mediaFileDao.getArtistAll(MUSIC_FOLDERS);
        List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(0).getPath(), false);
        List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPath(), false);
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

        albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(1).getPath(), false);
        files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPath(), false);
        assertEquals(1, files.size());
        assertEquals("file4", files.get(0).getName());
        assertEquals("山田耕筰", files.get(0).getArtist());
        assertNull(files.get(0).getArtistSort());

        List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, MUSIC_FOLDERS);
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

        invokeUtils(utils, "compensateSortOfArtist");

        artists = mediaFileDao.getArtistAll(MUSIC_FOLDERS);
        albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(0).getPath(), false);
        files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, albums.get(0).getPath(), false);
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

        artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, MUSIC_FOLDERS);
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

}
