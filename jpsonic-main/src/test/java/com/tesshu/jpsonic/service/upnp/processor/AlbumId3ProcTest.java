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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.search.ParamSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicAlbum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlbumId3ProcTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
        .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Albums"), "Albums", true,
                now(), 1, false));

    @Autowired
    private SettingsService settingsService;

    @Autowired
    @Qualifier("albumId3Proc")
    private AlbumId3Proc proc;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        setSortStrict(true);
        setSortAlphanum(true);
        populateDatabaseOnlyOnce();
        settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
        settingsService.save();
    }

    @Test
    void testGetProcId() {
        assertEquals("alid3", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        Album album = proc.getDirectChildren(0, 1).get(0);
        Container container = proc.createContainer(album);
        assertInstanceOf(MusicAlbum.class, container);
        // assertEquals("album/0", container.getId()); (Nonconforming test case)
        assertEquals("alid3", container.getParentID());
        assertEquals("10", container.getTitle());
        assertEquals(31, container.getChildCount());
    }

    @Nested
    class GetDirectChildrenTest {

        @Test
        void testGetDirectChildren() {

            settingsService.setSortAlbumsByYear(false);

            List<Album> items = proc.getDirectChildren(0, 10);
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i),
                        items.get(i).getName());
            }

            items = proc.getDirectChildren(10, 10);
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 10),
                        items.get(i).getName());
            }

            items = proc.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 20),
                        items.get(i).getName());
            }
        }

        @Test
        void testGetDirectChildrenByYear() {

            // The result does not change depending on the setting
            settingsService.setSortAlbumsByYear(true);

            List<Album> items = proc.getDirectChildren(0, 10);
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i),
                        items.get(i).getName());
            }

            items = proc.getDirectChildren(10, 10);
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 10),
                        items.get(i).getName());
            }

            items = proc.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            for (int i = 0; i < items.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 20),
                        items.get(i).getName());
            }
        }
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(31, proc.getDirectChildrenCount());
    }

    @Test
    void testGetDirectChild() {
        List<Album> albums = proc.getDirectChildren(0, Integer.MAX_VALUE);
        assertEquals(31, albums.size());
        Album album = proc.getDirectChild(Integer.toString(albums.get(0).getId()));
        assertEquals(albums.get(0).getId(), album.getId());
        assertEquals(albums.get(0).getName(), album.getName());
    }

    @Test
    void testGetChildren() {
        List<Album> albums = proc.getDirectChildren(0, 1);
        assertEquals(1, albums.size());
        assertEquals("10", albums.get(0).getName());

        List<MediaFile> songs = proc.getChildren(albums.get(0), 0, 10);
        for (int i = 0; i < songs.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.CHILDREN_LIST.get(i), songs.get(i).getName());
        }

        songs = proc.getChildren(albums.get(0), 10, 10);
        for (int i = 0; i < songs.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.CHILDREN_LIST.get(i + 10), songs.get(i).getName());
        }

        songs = proc.getChildren(albums.get(0), 20, 100);
        assertEquals(11, songs.size());
        for (int i = 0; i < songs.size(); i++) {
            assertEquals(UpnpProcessorTestUtils.CHILDREN_LIST.get(i + 20), songs.get(i).getName());
        }
    }

    @Test
    void testGetChildSizeOf() {
        List<Album> albums = proc.getDirectChildren(0, 1);
        assertEquals(1, albums.size());
        assertEquals("10", albums.get(0).getName());
        assertEquals(31, proc.getChildSizeOf(albums.get(0)));
    }

    @Test
    void testAddChild() {
        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        Album album = proc.getDirectChildren(0, 1).get(0);
        proc
            .getChildren(album, 0, Integer.MAX_VALUE)
            .stream()
            .forEach(song -> proc.addChild(content, song));
        assertEquals(31, content.getItems().size());
    }

    @Test
    void testToBrowseResult() {
        Album album = proc.getDirectChildren(0, 1).get(0);
        ParamSearchResult<Album> searchResult = new ParamSearchResult<>();
        searchResult.getItems().add(album);
        BrowseResult browseResult = proc.toBrowseResult(searchResult);
        assertTrue(browseResult.getResult().startsWith("""
                <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                xmlns:dc="http://purl.org/dc/elements/1.1/" \
                xmlns:sec="http://www.sec.co.kr/" \
                xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                <container childCount="31" \
                """));
        // ... id="album/***"
        assertTrue(browseResult.getResult().contains("""
                parentID="alid3" restricted="1" searchable="0">\
                <dc:title>10</dc:title>\
                <upnp:class>object.container.album.musicAlbum</upnp:class>\
                <upnp:albumArtURI>\
                """));
        // ... https://192.168.1.1:4040/ext/coverArt.view?
        // id=al-0&amp;size=300&amp;jwt=****** ...
        assertTrue(browseResult.getResult().endsWith("""
                </upnp:albumArtURI>\
                <upnp:artist>ARTIST</upnp:artist>\
                <dc:description/>\
                </container>\
                </DIDL-Lite>\
                """));
    }
}
