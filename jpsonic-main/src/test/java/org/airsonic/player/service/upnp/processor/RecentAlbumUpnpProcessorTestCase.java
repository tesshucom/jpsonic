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
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.util.LegacyMap;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RecentAlbumUpnpProcessorTestCase extends AbstractAirsonicHomeTest {

    private static final Logger LOG = LoggerFactory.getLogger(RecentAlbumUpnpProcessorTestCase.class);

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Albums"));
        musicFolders.add(new MusicFolder(1, musicDir, "Albums", true, new Date()));
    }

    @Autowired
    private RecentAlbumUpnpProcessor processor;

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
    public void testGetItemCount() {
        // Does not include ''-ALL-''
        assertEquals(31, processor.getItemCount());
    }

    @Test
    public void testGetItems() {

        assertEquals(30, processor.getItems(0, 30).size());
        assertEquals(1, processor.getItems(30, 30).size());

        Map<Integer, MediaFile> c = LegacyMap.of();

        List<MediaFile> items = processor.getItems(0, 10);

        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(10, c.size());

        items = processor.getItems(10, 10);
        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(20, c.size());

        items = processor.getItems(20, 100);
        assertEquals(11, items.size());
        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(31, c.size());

        assertEquals(4, processor.getItems(0, 4).size());
        assertEquals(3, processor.getItems(0, 3).size());
        assertEquals(2, processor.getItems(0, 2).size());
        assertEquals(1, processor.getItems(0, 1).size());

        assertEquals(4, processor.getItems(1, 4).size());
        assertEquals(3, processor.getItems(1, 3).size());
        assertEquals(2, processor.getItems(1, 2).size());
        assertEquals(1, processor.getItems(1, 1).size());

    }

    @Test
    public void testGetChildSizeOf() {
        List<MediaFile> albums = processor.getItems(1, 1);
        assertEquals(1, albums.size());
        int childSizeOf = processor.getChildSizeOf(albums.get(0));
        if (childSizeOf == 31) {
            if (LOG.isInfoEnabled()) {
                LOG.info(
                        "In this environment, the order of recent-albums  may be partially different from other environments.");
            }
        } else {
            assertEquals(1, childSizeOf);
        }
    }

}
