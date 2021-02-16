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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.ibm.icu.util.GregorianCalendar;
import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MediaScannerServiceUtilsUpdateSortOfAlbumTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Cleansing/AlbumSort"));
        musicFolders.add(new MusicFolder(1, musicDir, "Duplicate", true, new Date()));
    }

    @Autowired
    private JMediaFileDao mediaFileDao;

    @Autowired
    private JAlbumDao albumDao;

    @Autowired
    private MediaScannerServiceUtils utils;

    @Autowired
    private MediaScannerService mediaScannerService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        Date now = GregorianCalendar.getInstance().getTime();

        mediaScannerService.setJpsonicCleansingProcess(false);

        populateDatabaseOnlyOnce(null, () -> {
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
            albums.forEach(a -> {
                List<MediaFile> songs = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPath(), false);
                songs.stream().forEach(m -> {
                    if ("file1".equals(m.getTitle()) || "file4".equals(m.getTitle())) {
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
    public void testUpdateSortOfAlbum() throws ExecutionException {

        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
        assertEquals(5, albums.size());

        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
        assertEquals(5, albumId3s.size());

        // to be merge
        assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
        assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
        assertEquals(1L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count());
        assertEquals(1L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count());

        assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
        assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
        assertEquals(1L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count());
        assertEquals(1L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count());

        invokeUtils(utils, "mergeSortOfAlbum");

        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
        assertEquals(5, albums.size());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, musicFolders);
        assertEquals(5, albumId3s.size());

        // merged
        assertEquals(1L, albums.stream().filter(m -> "albumA".equals(m.getAlbumSort())).count());
        assertEquals(2L, albums.stream().filter(m -> m.getAlbumSort() == null).count());
        assertEquals(0L, albums.stream().filter(m -> "albumC".equals(m.getAlbumSort())).count()); // merged
        assertEquals(2L, albums.stream().filter(m -> "albumD".equals(m.getAlbumSort())).count()); // merged

        assertEquals(1L, albumId3s.stream().filter(a -> "albumA".equals(a.getNameSort())).count());
        assertEquals(2L, albumId3s.stream().filter(a -> a.getNameSort() == null).count());
        assertEquals(0L, albumId3s.stream().filter(a -> "albumC".equals(a.getNameSort())).count()); // merged
        assertEquals(2L, albumId3s.stream().filter(a -> "albumD".equals(a.getNameSort())).count()); // merged

        invokeUtils(utils, "copySortOfAlbum");

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

        invokeUtils(utils, "compensateSortOfAlbum");

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

}
