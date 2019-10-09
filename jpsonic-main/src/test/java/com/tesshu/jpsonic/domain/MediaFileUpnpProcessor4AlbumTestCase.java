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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.service.upnp.MediaFileUpnpProcessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.validateJPSonicNaturalList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MediaFileUpnpProcessor4AlbumTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Albums"));
        musicFolders.add(new MusicFolder(1, musicDir, "Albums", true, new Date()));
    }

    @Autowired
    private MediaFileUpnpProcessor mediaFileUpnpProcessor;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetAllItems() {
        List<MediaFile> all = mediaFileUpnpProcessor.getAllItems();
        assertEquals(1, all.size());
        MediaFile artist = all.get(0);
        assertEquals("ARTIST", artist.getName());
        List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist);
        assertTrue(validateJPSonicNaturalList(albums.stream().map(a -> a.getName()).collect(Collectors.toList())));
    }

}
