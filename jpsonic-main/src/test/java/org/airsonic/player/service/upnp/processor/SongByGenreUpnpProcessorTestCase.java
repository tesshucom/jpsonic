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

import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.util.LegacyMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/*
 * Test to correct sort inconsistencies.
 */
public class SongByGenreUpnpProcessorTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private SongByGenreUpnpProcessor SongByGenreUpnpProcessor;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        setSortStrict(true);
        setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setSortGenresByAlphabet(false);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetItemCount() {
        assertEquals(31, SongByGenreUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        Map<String, Genre> c = LegacyMap.of();

        List<Genre> items = SongByGenreUpnpProcessor.getItems(0, 10);
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 10);

        items = SongByGenreUpnpProcessor.getItems(10, 10);
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 20);

        items = SongByGenreUpnpProcessor.getItems(20, 100);
        assertEquals(11, items.size());
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 31);

    }

    @Test
    public void testGetChildSizeOf() {
        List<Genre> artists = SongByGenreUpnpProcessor.getItems(0, 1);
        assertEquals(1, artists.size());
        // assertEquals("A;B;C", artists.get(0).getName());
        assertEquals(31, SongByGenreUpnpProcessor.getChildSizeOf(artists.get(0)));
    }

    @Test
    public void testGetChildren() {

        List<Genre> artists = SongByGenreUpnpProcessor.getItems(0, 1);
        assertEquals(1, artists.size());
        // assertEquals("A;B;C", artists.get(0).getName());

        Map<String, MediaFile> c = LegacyMap.of();

        List<MediaFile> children = SongByGenreUpnpProcessor.getChildren(artists.get(0), 0, 10);
        children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
        assertEquals(c.size(), 10);

        children = SongByGenreUpnpProcessor.getChildren(artists.get(0), 10, 10);
        children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
        assertEquals(c.size(), 20);

        children = SongByGenreUpnpProcessor.getChildren(artists.get(0), 20, 100);
        assertEquals(11, children.size());
        children.stream().filter(m -> !c.containsKey(m.getGenre())).forEach(m -> c.put(m.getGenre(), m));
        assertEquals(c.size(), 31);

    }

}
