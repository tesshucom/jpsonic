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
package com.tesshu.jpsonic.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.validateAlphaNumList;
import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.validateJPSonicNaturalList;
import static org.junit.Assert.assertTrue;

public class ArtistDaoTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir1 = new File(resolveBaseMediaPath.apply("Sort/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir1, "Artists", true, new Date()));
        File musicDir2 = new File(resolveBaseMediaPath.apply("Sort/ArtistsAlphaNum"));
        musicFolders.add(new MusicFolder(2, musicDir2, "ArtistsAlphaNum", true, new Date()));
    }

    @Autowired
    private ArtistDao artistDao;

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
    public void testGetAlphabetialArtists() {
        List<Artist> all = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(musicFolders.get(0)));
        assertTrue(validateJPSonicNaturalList(all.stream().map(a -> a.getName()).collect(Collectors.toList())));
    }
    
    @Test
    public void testGetAlphabetialNumArtists() {
        List<Artist> all = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(musicFolders.get(1)));
        assertTrue(validateAlphaNumList(all.stream().map(a -> a.getName()).collect(Collectors.toList())));
    }

}
