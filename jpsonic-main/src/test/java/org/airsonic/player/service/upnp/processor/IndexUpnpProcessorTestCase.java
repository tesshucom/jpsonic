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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.indexList;
import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.jPSonicNaturalList;
import static com.tesshu.jpsonic.domain.SortingIntegrationTestCase.validateJPSonicNaturalList;
import static org.junit.Assert.assertEquals;

public class IndexUpnpProcessorTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private IndexUpnpProcessor indexUpnpProcessor;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        setSortStrict(true);
        setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testGetItemCount() {
        assertEquals(9, indexUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        List<MediaFile> items = indexUpnpProcessor.getItems(0, 5);
        assertEquals(5, items.size());
        assertEquals("A", items.get(0).getName());
        assertEquals("B", items.get(1).getName());
        assertEquals("C", items.get(2).getName());
        assertEquals("D", items.get(3).getName());
        assertEquals("E", items.get(4).getName());

        items = indexUpnpProcessor.getItems(5, 100);
        assertEquals(4, items.size());
        assertEquals("あ", items.get(0).getName());
        assertEquals("さ", items.get(1).getName());
        assertEquals("は", items.get(2).getName());
        assertEquals("#", items.get(3).getName());

    }

    @Test
    public void testGetChildSizeOf() {

        List<MediaFile> items = indexUpnpProcessor.getItems(0, 100);
        assertEquals(9, items.size());

        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(0)));
        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(1)));
        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(2)));
        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(3)));
        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(4)));
        assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(5)));
        assertEquals(1, indexUpnpProcessor.getChildSizeOf(items.get(6)));
        assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(7)));
        assertEquals(15, indexUpnpProcessor.getChildSizeOf(items.get(8)));

    }

    @Test
    public void testgetChildren() {

        List<String> artistNames = indexUpnpProcessor.getItems(0, 100).stream()
                .flatMap(m -> indexUpnpProcessor.getChildren(m, 0, 100).stream())
                .map(m -> m.getName())
                .collect(Collectors.toList());
        assertEquals(indexList, artistNames);

        List<MediaFile> items = indexUpnpProcessor.getItems(0, 100);
        assertEquals(9, items.size());
        assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(5)));
        assertEquals(5, indexUpnpProcessor.getChildSizeOf(items.get(7)));
        assertEquals(15, indexUpnpProcessor.getChildSizeOf(items.get(8)));

        List<MediaFile> artist = indexUpnpProcessor.getChildren(items.get(5), 0, 3);
        assertEquals(indexList.get(5), artist.get(0).getName());
        assertEquals(indexList.get(6), artist.get(1).getName());
        assertEquals(indexList.get(7), artist.get(2).getName());
        artist = indexUpnpProcessor.getChildren(items.get(5), 3, 100);
        assertEquals(indexList.get(8), artist.get(0).getName());
        assertEquals(indexList.get(9), artist.get(1).getName());

        artist = indexUpnpProcessor.getChildren(items.get(7), 0, 3);
        assertEquals(indexList.get(11), artist.get(0).getName());
        assertEquals(indexList.get(12), artist.get(1).getName());
        assertEquals(indexList.get(13), artist.get(2).getName());
        artist = indexUpnpProcessor.getChildren(items.get(7), 3, 100);
        assertEquals(indexList.get(14), artist.get(0).getName());
        assertEquals(indexList.get(15), artist.get(1).getName());

        artist = indexUpnpProcessor.getChildren(items.get(8), 0, 3);

        assertEquals(3, artist.size());
        assertEquals(indexList.get(16), artist.get(0).getName());
        assertEquals(indexList.get(17), artist.get(1).getName());
        assertEquals(indexList.get(18), artist.get(2).getName());
        artist = indexUpnpProcessor.getChildren(items.get(8), 3, 100);
        assertEquals(12, artist.size());
        assertEquals(indexList.get(19), artist.get(0).getName());
        assertEquals(indexList.get(20), artist.get(1).getName());
        assertEquals(indexList.get(21), artist.get(2).getName());
        assertEquals(indexList.get(22), artist.get(3).getName());
        assertEquals(indexList.get(23), artist.get(4).getName());
        assertEquals(indexList.get(24), artist.get(5).getName());
        assertEquals(indexList.get(25), artist.get(6).getName());
        assertEquals(indexList.get(26), artist.get(7).getName());
        assertEquals(indexList.get(27), artist.get(8).getName());
        assertEquals(indexList.get(28), artist.get(9).getName());
        assertEquals(indexList.get(29), artist.get(10).getName());
        assertEquals(indexList.get(30), artist.get(11).getName());

    }

    @Test
    public void testAlbum() {

        settingsService.setSortAlbumsByYear(false);

        List<MediaFile> indexes = indexUpnpProcessor.getItems(0, 100);
        assertEquals(9, indexes.size());

        MediaFile index = indexes.get(8);
        assertEquals("#", index.getName());

        List<MediaFile> artists = indexUpnpProcessor.getChildren(index, 0, Integer.MAX_VALUE);
        assertEquals(15, artists.size());
        MediaFile artist = artists.get(0);
        assertEquals("10", artist.getName());

        List<MediaFile> albums = indexUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(31, albums.size());

        validateJPSonicNaturalList(albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

    }

    @Test
    public void testAlbumByYear() {

        // The result change depending on the setting
        settingsService.setSortAlbumsByYear(true);
        List<String> reversedByYear = new ArrayList<>(jPSonicNaturalList);
        Collections.reverse(reversedByYear);

        List<MediaFile> indexes = indexUpnpProcessor.getItems(0, 100);
        assertEquals(9, indexes.size());

        MediaFile index = indexes.get(8);
        assertEquals("#", index.getName());

        List<MediaFile> artists = indexUpnpProcessor.getChildren(index, 0, Integer.MAX_VALUE);
        assertEquals(15, artists.size());
        MediaFile artist = artists.get(0);
        assertEquals("10", artist.getName());

        List<MediaFile> albums = indexUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(31, albums.size());

        assertEquals(reversedByYear, albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

    }

}
