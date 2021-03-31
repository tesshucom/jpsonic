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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service.upnp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.airsonic.player.AbstractNeedsScan;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.LegacyMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RecentAlbumId3UpnpProcessorTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Pagination/Albums"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Albums", true, new Date()));
    }

    @Autowired
    private RecentAlbumId3UpnpProcessor processor;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        setSortStrict(true);
        setSortAlphanum(true);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetItemCount() {
        assertEquals(32, processor.getItemCount()); // (allAlbumCount + -ALL-) or MAX
    }

    @Test
    public void testGetItems() {

        // It is OK if the following requirements can be cleared

        assertEquals(30, processor.getItems(0, 30).size());
        assertEquals(2, processor.getItems(30, 30).size());

        // processor.getItems(0, 30).forEach(a -> System.out.println(a.getName()));
        // processor.getItems(30, 30).forEach(a -> System.out.println(a.getName()));

        Map<Integer, Album> c = LegacyMap.of();

        List<Album> items = processor.getItems(0, 10);
        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(10, c.size());

        items = processor.getItems(10, 10);
        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(20, c.size());

        items = processor.getItems(20, 100);
        assertEquals(12, items.size());
        items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(32, c.size());

        // Currently the border is not strict

        assertEquals(4, processor.getItems(0, 4).size());
        assertEquals(3, processor.getItems(0, 3).size());
        assertEquals(1, processor.getItems(0, 2).size());
        assertEquals(32, processor.getItems(0, 1).size());

        assertEquals(5, processor.getItems(1, 4).size());
        assertEquals(4, processor.getItems(1, 3).size());
        assertEquals(3, processor.getItems(1, 2).size());
        assertEquals(1, processor.getItems(1, 1).size());

    }

    @Test
    public void testGetChildSizeOf() {
        List<Album> albums = processor.getItems(1, 1);
        assertEquals(1, albums.size());
        assertEquals(1, processor.getChildSizeOf(albums.get(0)));
    }

}
