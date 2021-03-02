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
import org.springframework.boot.test.context.SpringBootTest;

/**
 * If SORT exists for one name and null-sort data exists, unify it to SORT.
 */
@SpringBootTest
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class MediaScannerServiceUtilsCopySortOfArtistTest extends AbstractAirsonicHomeTest {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Cleansing/ArtistSort/Copy"));
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
    public void testCopySortOfArtist() throws ExecutionException {

        invokeUtils(utils, "mergeSortOfArtist");

        List<MediaFile> artists = mediaFileDao.getArtistAll(MUSIC_FOLDERS);
        assertEquals(1, artists.size());
        List<MediaFile> albums = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, artists.get(0).getPath(), false);
        assertEquals(1, albums.size());
        MediaFile album = albums.get(0);
        List<MediaFile> files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPath(), false);
        assertEquals(2, files.size());
        List<Artist> artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, MUSIC_FOLDERS);
        assertEquals(1, artistID3s.size());

        assertEquals("file1", files.get(0).getName());
        assertEquals("case1", files.get(0).getArtist());
        assertNull(files.get(0).getArtistSort());
        assertEquals("file2", files.get(1).getName());
        assertEquals("case1", files.get(1).getArtist());
        assertEquals("artistA", files.get(1).getArtistSort());

        assertEquals("case1", artistID3s.get(0).getName());
        assertNull(artistID3s.get(0).getSort());

        invokeUtils(utils, "copySortOfArtist");

        files = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, album.getPath(), false);
        artistID3s = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, MUSIC_FOLDERS);

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
