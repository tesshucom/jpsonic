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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Test to correct sort inconsistencies.
 */
public class ArtistUpnpProcessorTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private ArtistUpnpProcessor artistUpnpProcessor;

    @Autowired
    private SettingsService settingsService;

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
        assertEquals(31, artistUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        List<Artist> items = artistUpnpProcessor.getItems(0, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i), items.get(i).getName());
        }

        items = artistUpnpProcessor.getItems(10, 10);
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 10), items.get(i).getName());
        }

        items = artistUpnpProcessor.getItems(20, 100);
        assertEquals(11, items.size());
        for (int i = 0; i < items.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 20), items.get(i).getName());
        }

    }

    @Test
    public void testGetChildSizeOf() {
        List<Artist> artists = artistUpnpProcessor.getItems(0, 1);
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());
        assertEquals(32, artistUpnpProcessor.getChildSizeOf(artists.get(0))); // 31 + "-All-"
    }

    @Test
    public void testGetChildren() {

        settingsService.setSortAlbumsByYear(false);

        List<Artist> artists = artistUpnpProcessor.getItems(0, 1);
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());

        List<Album> children = artistUpnpProcessor.getChildren(artists.get(0), 0, 10);
        for (int i = 0; i < children.size(); i++) {
            if (0 != i) {
                assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i - 1), children.get(i).getName());
            }
        }

        children = artistUpnpProcessor.getChildren(artists.get(0), 10, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 10 - 1), children.get(i).getName());
        }

        children = artistUpnpProcessor.getChildren(artists.get(0), 20, 100);
        assertEquals(12, children.size()); //
        for (int i = 0; i < children.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.jPSonicNaturalList.get(i + 20 - 1), children.get(i).getName());
        }

    }

    @Test
    public void testGetChildrenByYear() {

        // The result change depending on the setting
        settingsService.setSortAlbumsByYear(true);
        List<String> reversedByYear = new ArrayList<>(UpnpProcessorTestUtils.jPSonicNaturalList);
        Collections.reverse(reversedByYear);

        List<Artist> artists = artistUpnpProcessor.getItems(0, 1);
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());

        List<Album> children = artistUpnpProcessor.getChildren(artists.get(0), 0, 10);

        for (int i = 0; i < children.size(); i++) {
            if (0 != i) {
                assertEquals(reversedByYear.get(i - 1), children.get(i).getName());
            }
        }

        children = artistUpnpProcessor.getChildren(artists.get(0), 10, 10);
        for (int i = 0; i < children.size(); i++) {
            assertEquals(reversedByYear.get(i + 10 - 1), children.get(i).getName());
        }

        children = artistUpnpProcessor.getChildren(artists.get(0), 20, 100);
        assertEquals(12, children.size()); //
        for (int i = 0; i < children.size(); i++) {
            assertEquals(reversedByYear.get(i + 20 - 1), children.get(i).getName());
        }

    }

    @Test
    public void testSongs() {

        List<Artist> artists = artistUpnpProcessor.getItems(0, Integer.MAX_VALUE).stream()
                .filter(a -> "20".equals(a.getName())).collect(Collectors.toList());
        assertEquals(1, artists.size());

        Artist artist = artists.get(0);
        assertEquals("20", artist.getName());

        List<Album> albums = artistUpnpProcessor.getChildren(artist, 0, Integer.MAX_VALUE);
        assertEquals(1, albums.size());

        Album album = albums.get(0);
        assertEquals("AlBum!", album.getName()); // the case where album name is different between file and id3

        List<MediaFile> songs = artistUpnpProcessor.getDispatcher().getAlbumProcessor().getChildren(album, 0,
                Integer.MAX_VALUE);
        assertEquals(1, songs.size());

        MediaFile song = songs.get(0);
        assertEquals("empty", song.getName());

    }

}
