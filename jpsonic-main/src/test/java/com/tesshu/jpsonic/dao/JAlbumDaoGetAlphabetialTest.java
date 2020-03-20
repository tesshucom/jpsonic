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
package com.tesshu.jpsonic.dao;

import com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.domain.Album;
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
import java.util.stream.Collectors;

@SpringBootConfiguration
@ComponentScan(basePackages = { "org.airsonic.player", "com.tesshu.jpsonic" })
@SpringBootTest
public class JAlbumDaoGetAlphabetialTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Compare"));
        musicFolders.add(new MusicFolder(1, musicDir, "Albums", true, new Date()));
    }

    @Autowired
    private AlbumDao albumDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        setSortStrict(true);
        setSortAlphanum(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetAlphabeticalAlbums() {
        List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, musicFolders);
        List<String> names = albums.stream().filter(a -> !"☆彡ALBUM".equals(a.getName())).map(a -> a.getName()).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(names);
    }

}
