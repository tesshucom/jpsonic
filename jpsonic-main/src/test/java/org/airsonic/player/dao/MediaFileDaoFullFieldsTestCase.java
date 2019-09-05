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

import static org.junit.Assert.assertEquals;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 */
public class MediaFileDaoFullFieldsTestCase extends AbstractAirsonicHomeTest {

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
            musicFolders.add(new MusicFolder(1, new File(resolveBaseMediaPath.apply("Metadata")), "fullFields", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testFullFields() {

        String path = resolveBaseMediaPath.apply("Metadata/fullFields/v2.3+v1.1.mp3");
        if (System.getProperty("os.name").startsWith("Win")) {
            path = path.replaceAll("^/", "").replaceAll("/", "\\\\");
        }

        MediaFile mediaFile = dao.getMediaFile(path);

        assertEquals("タイトル", mediaFile.getName());
        assertEquals("タイトル", mediaFile.getTitle());
        assertEquals("タイトル(読み)", mediaFile.getTitleSort());

        assertEquals("アーティスト", mediaFile.getArtist());
        assertEquals("アーティスト(読み)", mediaFile.getArtistReading());
        assertEquals("アーティスト(読み)", mediaFile.getArtistSort());

        assertEquals("アルバム", mediaFile.getAlbumName());
        assertEquals("アルバム(ヨミ)", mediaFile.getAlbumReading());
        assertEquals("アルバム(読み)", mediaFile.getAlbumSort());

        assertEquals("アルバムアーティスト", mediaFile.getAlbumArtist());
        assertEquals("アルバムアーティスト名(読み)", mediaFile.getAlbumArtistSort());

        assertEquals("作曲者", mediaFile.getComposer());
        assertEquals("作曲者(読み)", mediaFile.getComposerSort());

        assertEquals(Integer.valueOf(3), mediaFile.getDiscNumber());
        assertEquals("ジャンル", mediaFile.getGenre());
        assertEquals(Integer.valueOf(1), mediaFile.getTrackNumber());
        assertEquals(Integer.valueOf(2019), mediaFile.getYear());

        Album album = albumDao.getAlbum(0);
        assertEquals("アルバムアーティスト", album.getArtist());
        assertEquals("アルバムアーティスト名(読み)", album.getArtistSort());// By washing process
        assertEquals("アルバム", album.getName());
        assertEquals("アルバム(読み)", album.getNameSort());// By washing process

        Artist artist = artistDao.getArtist(0);
        assertEquals("アルバムアーティスト", artist.getName());
        assertEquals("アルバムアーティスト", artist.getReading());// By washing process
    }

}