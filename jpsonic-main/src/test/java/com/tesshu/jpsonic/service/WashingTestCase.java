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
package com.tesshu.jpsonic.service;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SpringBootConfiguration
@ComponentScan(basePackages = { "org.airsonic.player", "com.tesshu.jpsonic" })
@SpringBootTest
public class WashingTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Metadata/washing"));
        musicFolders.add(new MusicFolder(1, musicDir, "washing", true, new Date()));
    }

    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        setSortAlphanum(true);
        setSortStrict(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testArtist() {
        // 倖田來未 -> Artist names that cannot be analyzed by kuromoji.
        Artist artist = artistDao.getArtist("倖田來未", musicFolders);
        assertEquals("倖田來未", artist.getName());
        assertEquals("コウダクミ", artist.getReading());// Expected to be corrected with tags.
        assertEquals("コウダクミ", artist.getSort());// Expected to be corrected with tags.
    }

    @Test
    public void testAlbum() {

        // Album without tag
        Album album = albumDao.getAlbum("倖田來未", "Best ～first things～ [Disc 1]");
        assertEquals("Best ～first things～ [Disc 1]", album.getName());
        assertEquals("Best ～first things～ [Disc 1]", album.getNameReading());
        assertNull(album.getNameSort());
        assertEquals("倖田來未", album.getArtist());
        assertEquals("コウダクミ", album.getArtistReading());// Expected to be corrected with tags.
        assertEquals("コウダクミ", album.getArtistSort());// Expected to be corrected with tags.

        // Album with tag
        Album album2 = albumDao.getAlbum("倖田來未", "Best ～first things～ [Disc 2]");
        assertEquals("Best ～first things～ [Disc 2]", album2.getName());
        assertEquals("Best ～first things～ Disc 2", album2.getNameReading());
        assertEquals("Best ～first things～ Disc 2", album2.getNameSort());
        assertEquals("倖田來未", album2.getArtist());
        assertEquals("コウダクミ", album2.getArtistReading());
        assertEquals("コウダクミ", album2.getArtistSort());

    }

    @Test
    public void testArtistDirectory() {
        // 倖田來未 -> Artist names that cannot be analyzed by kuromoji.
        MediaFile artist = mediaFileDao.getArtistByName("倖田來未", musicFolders);
        assertEquals("倖田來未", artist.getName());
        assertEquals("コウダクミ", artist.getArtistReading());
        assertEquals("コウダクミ", artist.getArtistSort());
        assertNull(artist.getAlbumArtist());
        assertNull(artist.getAlbumArtistReading());
    }

}
