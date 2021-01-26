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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.util.LegacyMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Test to correct sort inconsistencies.
 */
public class PlaylistUpnpProcessorTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Pagination/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir, "Artists", true, new Date()));
    }

    @Autowired
    private PlaylistUpnpProcessor playlistUpnpProcessor;

    @Autowired
    private AlbumUpnpProcessor albumUpnpProcessor;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private PlaylistDao playlistDao;

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

        Function<String, Playlist> toPlaylist = (title) -> {
            Date now = new Date();
            Playlist playlist = new Playlist();
            playlist.setName(title);
            playlist.setUsername("admin");
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(false);
            return playlist;
        };

        if (0 == playlistDao.getAllPlaylists().size()) {
            List<String> shallow = new ArrayList<>();
            shallow.addAll(UpnpProcessorTestUtils.jPSonicNaturalList);
            Collections.shuffle(shallow);
            shallow.stream().map(toPlaylist).forEach(p -> playlistDao.createPlaylist(p));
        }

        List<Album> albums = albumUpnpProcessor.getItems(0, 100);
        assertEquals(61, albums.size());
        List<MediaFile> files = albums.stream()
                .map(a -> mediaFileDao.getSongsForAlbum(a.getArtist(), a.getName()).get(0))
                .collect(Collectors.toList());
        assertEquals(61, files.size());
        playlistDao.setFilesInPlaylist(playlistUpnpProcessor.getItems(0, 1).get(0).getId(), files);

    }

    @Test
    public void testGetItemCount() {
        assertEquals(31, playlistUpnpProcessor.getItemCount());
    }

    @Test
    public void testGetItems() {

        Map<String, Playlist> c = LegacyMap.of();

        List<Playlist> items = playlistUpnpProcessor.getItems(0, 10);
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 10);

        items = playlistUpnpProcessor.getItems(10, 10);
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 20);

        items = playlistUpnpProcessor.getItems(20, 100);
        assertEquals(11, items.size());
        items.stream().filter(g -> !c.containsKey(g.getName())).forEach(g -> c.put(g.getName(), g));
        assertEquals(c.size(), 31);

    }

    @Test
    public void testGetChildSizeOf() {
        List<Playlist> playlists = playlistUpnpProcessor.getItems(0, 1);
        assertEquals(1, playlists.size());
        assertEquals(61, playlistUpnpProcessor.getChildSizeOf(playlists.get(0)));
    }

    @Test
    public void testGetChildren() {

        List<Playlist> playlists = playlistUpnpProcessor.getItems(0, 1);
        assertEquals(1, playlists.size());
        assertEquals(61, playlistUpnpProcessor.getChildSizeOf(playlists.get(0)));

        Map<Integer, MediaFile> c = LegacyMap.of();

        List<MediaFile> children = playlistUpnpProcessor.getChildren(playlists.get(0), 0, 20);
        children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(children.size(), 20);

        children = playlistUpnpProcessor.getChildren(playlists.get(0), 20, 20);
        children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(c.size(), 40);

        children = playlistUpnpProcessor.getChildren(playlists.get(0), 40, 100);
        assertEquals(21, children.size());
        children.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
        assertEquals(c.size(), 61);

    }

}
