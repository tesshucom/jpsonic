/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.dao;

import com.google.common.base.Supplier;
import com.tesshu.jpsonic.service.MediaScannerServiceUtilsMergeSortTest;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * @deprecated A test class was provisionally provided at the transition of processing.
 * Content overlaps with MediaScannerServiceUtilsMergeSortTest.
 * {@link MediaScannerServiceUtilsMergeSortTest}
 */
@Deprecated
public class MediaFileDaoNoSortFieldsTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    MediaFileDao dao;

    @Autowired
    AlbumDao albumDao;

    @Autowired
    ArtistDao artistDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            musicFolders.add(new MusicFolder(1, new File(resolveBaseMediaPath.apply("Metadata")), "noSortFields", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    private Supplier<Boolean> isWin = () -> System.getProperty("os.name").startsWith("Win");

    @Test
    public void testNoSortFields() {

        String path = resolveBaseMediaPath.apply("Metadata/noSortFields/v2.3+v1.1.mp3");
        if (isWin.get()) {
            path = path.replaceAll("^/", "").replaceAll("/", "\\\\");
        }

        MediaFile mediaFile = dao.getMediaFile(path);

        assertEquals("タイトル", mediaFile.getName());
        assertEquals("タイトル", mediaFile.getTitle());
        assertNull(mediaFile.getTitleSort());

        assertEquals("アーティスト", mediaFile.getArtist());
        assertEquals("アーティスト", mediaFile.getArtistReading());
        assertNull(mediaFile.getArtistSort());

        assertEquals("アルバム", mediaFile.getAlbumName());
        assertEquals("アルバム", mediaFile.getAlbumReading());
        assertNull(mediaFile.getAlbumSort());

        assertEquals("アルバムアーティスト", mediaFile.getAlbumArtist());
        assertEquals("アルバムアーティストメイ(ヨミ)", mediaFile.getAlbumArtistReading()); // #406 Copied from other data during scanning
        assertEquals("アルバムアーティスト名(読み)", mediaFile.getAlbumArtistSort()); // #406 Copied from other data during scanning

        assertEquals("作曲者", mediaFile.getComposer());
        assertEquals("作曲者(読み)",mediaFile.getComposerSort());

        assertEquals(Integer.valueOf(3), mediaFile.getDiscNumber());
        assertEquals("ジャンル", mediaFile.getGenre());
        assertEquals(Integer.valueOf(1), mediaFile.getTrackNumber());
        assertEquals(Integer.valueOf(2019), mediaFile.getYear());

        Album album = albumDao.getAlbum(mediaFile.getAlbumArtist(), mediaFile.getAlbumName());

        assertEquals("アルバムアーティスト", album.getArtist());
        if (isWin.get()) { // #307
            assertEquals("アルバムアーティストメイ(ヨミ)", album.getArtistReading());// By washing process
        } else {
            assertTrue("アルバムアーティスト".equals(album.getArtistReading())
                    || "アルバムアーティストメイ(ヨミ)".equals(album.getArtistReading()));
        }
        assertEquals("アルバムアーティスト名(読み)", album.getArtistSort());// By washing process
        assertEquals("アルバム", album.getName());
        assertEquals("アルバム(ヨミ)", album.getNameReading());// By washing process
        assertEquals("アルバム(読み)", album.getNameSort());// By washing process

        Artist artist = artistDao.getArtist(album.getArtist());
        assertEquals("アルバムアーティスト", artist.getName());
        if (isWin.get()) { // #307
            assertEquals("アルバムアーティストメイ(ヨミ)", artist.getReading());// By washing process
        } else {
            assertTrue("アルバムアーティスト".equals(artist.getReading()) || "アルバムアーティストメイ(ヨミ)".equals(artist.getReading()));
        }
        assertEquals("アルバムアーティスト名(読み)", artist.getSort());// By washing process

    }
}