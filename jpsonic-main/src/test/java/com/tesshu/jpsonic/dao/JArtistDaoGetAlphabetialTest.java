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

package com.tesshu.jpsonic.dao;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JArtistDaoGetAlphabetialTest extends AbstractAirsonicHomeTest {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Compare"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private JArtistDao artistDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @Before
    public void setup() {
        setSortAlphanum(true);
        setSortStrict(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetAlphabetialArtists() {
        List<Artist> all = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, Arrays.asList(MUSIC_FOLDERS.get(0)));
        List<String> names = all.stream().map(Artist::getName).collect(Collectors.toList());
        assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(names));
    }
}
