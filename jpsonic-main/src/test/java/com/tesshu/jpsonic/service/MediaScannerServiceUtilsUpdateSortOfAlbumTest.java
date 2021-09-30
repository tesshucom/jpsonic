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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.AvoidDuplicateLiterals" })
/*
 * In the testing class, it may be less readable.
 */
class MediaScannerServiceUtilsUpdateSortOfAlbumTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Cleansing/AlbumSort"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Duplicate", true, new Date()));
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
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {

        mediaScannerService.setJpsonicCleansingProcess(false);

        // Update the date of a particular file to cause a merge
        String latestMediaFileTitle1 = "file1";
        String latestMediaFileTitle2 = "file4";

        populateDatabaseOnlyOnce(null, () -> {
            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
            Date now = new Date();
            albums.forEach(a -> {
                List<MediaFile> songs = mediaFileDao.getChildrenOf(0, Integer.MAX_VALUE, a.getPath(), false);
                songs.forEach(m -> {
                    if (latestMediaFileTitle1.equals(m.getTitle()) || latestMediaFileTitle2.equals(m.getTitle())) {
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
    void testUpdateSortOfAlbum() throws ExecutionException {

        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
        assertEquals(5, albums.size());

        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, MUSIC_FOLDERS);
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

        utils.mergeSortOfAlbum();

        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
        assertEquals(5, albums.size());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, MUSIC_FOLDERS);
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

        utils.copySortOfAlbum();

        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
        assertEquals(5, albums.size());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, MUSIC_FOLDERS);
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

        utils.compensateSortOfAlbum();

        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, MUSIC_FOLDERS);
        assertEquals(5, albums.size());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, MUSIC_FOLDERS);
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
