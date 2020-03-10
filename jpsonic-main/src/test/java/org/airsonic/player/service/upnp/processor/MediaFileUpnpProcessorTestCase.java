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

import static org.junit.Assert.assertEquals;

public class MediaFileUpnpProcessorTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private MediaFileUpnpProcessor mediaFileUpnpProcessor;

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
        //31 + 22(topnodes)
        assertEquals(53, mediaFileUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        List<MediaFile> items = mediaFileUpnpProcessor.getItems(0, 10);
        assertEquals(10, items.size());

        items = mediaFileUpnpProcessor.getItems(10, 10);
        assertEquals(10, items.size());

        items = mediaFileUpnpProcessor.getItems(20, 100);
        assertEquals(33, items.size());

        items = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> !a.getName().startsWith("single")).collect(Collectors.toList());
        UpnpProcessorTestUtils.validateJPSonicNaturalList(items.stream().map(a -> a.getName()).collect(Collectors.toList()));
    }

    @Test
    public void testGetChildSizeOf() {
        List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());
        assertEquals(31, mediaFileUpnpProcessor.getChildSizeOf(artists.get(0)));
    }

    @Test
    public void testgetChildren() {

        List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());

        List<MediaFile> children = mediaFileUpnpProcessor.getChildren(artists.get(0), 0, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i), children.get(i).getName());
        }

        children = mediaFileUpnpProcessor.getChildren(artists.get(0), 10, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 10), children.get(i).getName());
        }

        children = mediaFileUpnpProcessor.getChildren(artists.get(0), 20, 100);
        assertEquals(11, children.size());
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 20), children.get(i).getName());
        }

    }

    @Test
    public void testAlbum() {

        settingsService.setSortAlbumsByYear(false);

        List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());

        MediaFile artist = artists.get(0);

        List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(31, albums.size());
        UpnpProcessorTestUtils.validateJPSonicNaturalList(albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

    }

    @Test
    public void testAlbumByYear() {

        // The result change depending on the setting
        settingsService.setSortAlbumsByYear(true);
        List<String> reversedByYear = new ArrayList<>(UpnpProcessorTestUtils.jPSonicNaturalList);
        Collections.reverse(reversedByYear);

        List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> "10".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());

        MediaFile artist = artists.get(0);

        List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(31, albums.size());
        assertEquals(reversedByYear, albums.stream().map(a -> a.getName()).collect(Collectors.toList()));

    }
    
    @Test
    public void testSongs() {

        settingsService.setSortAlbumsByYear(false);

        List<MediaFile> artists = mediaFileUpnpProcessor.getItems(0, 100).stream().filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());

        MediaFile artist = artists.get(0);
        assertEquals("20", artist.getName());

        List<MediaFile> albums = mediaFileUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(1, albums.size());

        MediaFile album = albums.get(0);
        assertEquals("ALBUM", album.getName());// the case where album name is different between file and id3

        List<MediaFile> songs = mediaFileUpnpProcessor.getChildren(album, 0, Integer.MAX_VALUE);
        assertEquals(1, songs.size());

        MediaFile song = songs.get(0);
        assertEquals("empty", song.getName());

    }

}
