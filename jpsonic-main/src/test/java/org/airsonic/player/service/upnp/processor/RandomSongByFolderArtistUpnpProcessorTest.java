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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RandomSongByFolderArtistUpnpProcessorTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private RandomSongByFolderArtistUpnpProcessor processor;

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
        assertEquals(1, processor.getItemCount());
    }

    @Test
    public void testGetItems() {
        List<FolderArtistWrapper> items = processor.getItems(0, 10);
        assertEquals(1, items.size());
        assertEquals("Artists", items.get(0).getFolder().getName());
    }

    @Test
    public void testGetChildSizeOf() {
        List<FolderArtistWrapper> artists = processor.getItems(0, 1);
        assertEquals(1, artists.size());
        assertEquals(1, processor.getChildSizeOf(artists.get(0)));
    }

    @Test
    public void testGetChildren() {

        List<FolderArtistWrapper> folders = processor.getItems(0, 10);
        assertEquals(1, folders.size());
        assertEquals("Artists", folders.get(0).getName());

        List<FolderArtistWrapper> artists = processor.getChildren(folders.get(0), 0, 10);
        assertEquals(10, artists.size());
        assertEquals("10", artists.get(0).getName());
        assertEquals("20", artists.get(1).getName());
        assertEquals("30", artists.get(2).getName());

        List<FolderArtistWrapper> songs = processor.getChildren(artists.get(0), 0, Integer.MAX_VALUE);
        assertEquals(31, songs.size());

    }

}
