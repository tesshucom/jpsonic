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

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AlbumUpnpProcessorTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Albums"));
        musicFolders.add(new MusicFolder(1, musicDir, "Albums", true, new Date()));
    }

    @Autowired
    private AlbumUpnpProcessor albumUpnpProcessor;

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
        assertEquals(31, albumUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        settingsService.setSortAlbumsByYear(false);

        List<Album> items = albumUpnpProcessor.getItems(0, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i), items.get(i).getName());
        }

        items = albumUpnpProcessor.getItems(10, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 10), items.get(i).getName());
        }

        items = albumUpnpProcessor.getItems(20, 100);
        assertEquals(11, items.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 20), items.get(i).getName());
        }

    }

    @Test
    public void testGetItemsByYear() {

        // The result does not change depending on the setting
        settingsService.setSortAlbumsByYear(true);

        List<Album> items = albumUpnpProcessor.getItems(0, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i), items.get(i).getName());
        }

        items = albumUpnpProcessor.getItems(10, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 10), items.get(i).getName());
        }

        items = albumUpnpProcessor.getItems(20, 100);
        assertEquals(11, items.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 20), items.get(i).getName());
        }

    }

    @Test
    public void testGetChildSizeOf() {
        List<Album> albums = albumUpnpProcessor.getItems(0, 1);
        assertEquals(1, albums.size());
        assertEquals("10", albums.get(0).getName());
        assertEquals(31, albumUpnpProcessor.getChildSizeOf(albums.get(0)));
    }

    @Test
    public void testGetChild() {

        List<Album> albums = albumUpnpProcessor.getItems(0, 1);
        assertEquals(1, albums.size());
        assertEquals("10", albums.get(0).getName());

        List<MediaFile> children = albumUpnpProcessor.getChildren(albums.get(0), 0, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.childrenList.get(i), children.get(i).getName());
        }

        children = albumUpnpProcessor.getChildren(albums.get(0), 10, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.childrenList.get(i + 10), children.get(i).getName());
        }

        children = albumUpnpProcessor.getChildren(albums.get(0), 20, 100);
        assertEquals(11, children.size());
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.childrenList.get(i + 20), children.get(i).getName());
        }

    }

}
