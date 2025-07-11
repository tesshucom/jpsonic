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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.ParamSearchResult;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.MusicArtist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

class ArtistProcTest extends AbstractNeedsScan {

    private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
        .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true,
                now(), 1, false));

    @Autowired
    private ArtistProc proc;

    @Autowired()
    @Qualifier("albumId3Proc")
    private AlbumId3Proc albumId3Proc;

    @Autowired
    private SettingsService settingsService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @BeforeEach
    public void setup() {
        setSortStrict(true);
        setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        populateDatabaseOnlyOnce();
        settingsService.setDlnaBaseLANURL("https://192.168.1.1:4040");
        settingsService.save();
    }

    @Test
    void testGetProcId() {
        assertEquals("artist", proc.getProcId().getValue());
    }

    @Test
    void testCreateContainer() {
        Artist artist = proc.getDirectChildren(0, 1).get(0);
        Container container = proc.createContainer(artist);
        assertInstanceOf(MusicArtist.class, container);
        assertEquals("artist/0", container.getId());
        assertEquals("artist", container.getParentID());
        assertEquals("10", container.getTitle());
        assertEquals(31, container.getChildCount());
    }

    @Test
    void testGetDirectChildren() {
        List<Artist> items = proc.getDirectChildren(0, 10);
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
    void testGetDirectChildrenCount() {
        assertEquals(31, proc.getDirectChildrenCount());
    }

    @Test
    void testGetDirectChild() {
        List<Artist> artists = proc.getDirectChildren(0, Integer.MAX_VALUE);
        assertEquals(31, artists.size());
        Artist artist = proc.getDirectChild(Integer.toString(artists.get(0).getId()));
        assertEquals(artists.get(0).getId(), artist.getId());
        assertEquals(artists.get(0).getName(), artist.getName());
    }

    @Nested
    class GetChildrenTest {

        @Test
        void testGetChildren() {

            settingsService.setSortAlbumsByYear(false);

            List<Artist> artists = proc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            List<Album> children = proc.getChildren(artists.get(0), 0, 10);
            for (int i = 0; i < children.size(); i++) {
                if (0 != i) {
                    assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i),
                            children.get(i).getName());
                }
            }

            children = proc.getChildren(artists.get(0), 10, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 10),
                        children.get(i).getName());
            }

            children = proc.getChildren(artists.get(0), 20, 100);
            assertEquals(11, children.size());
            for (int i = 0; i < children.size(); i++) {
                assertEquals(UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST.get(i + 20),
                        children.get(i).getName());
            }
        }

        /*
         * The result change depending on the setting
         */
        @Test
        void testGetChildrenByYear() {
            settingsService.setSortAlbumsByYear(true);
            List<String> reversedByYear = new ArrayList<>(
                    UpnpProcessorTestUtils.JPSONIC_NATURAL_LIST);
            Collections.reverse(reversedByYear);

            List<Artist> artists = proc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            assertEquals("10", artists.get(0).getName());

            List<Album> children = proc.getChildren(artists.get(0), 0, 10);

            for (int i = 0; i < children.size(); i++) {
                if (0 != i) {
                    assertEquals(reversedByYear.get(i), children.get(i).getName());
                }
            }

            children = proc.getChildren(artists.get(0), 10, 10);
            for (int i = 0; i < children.size(); i++) {
                assertEquals(reversedByYear.get(i + 10), children.get(i).getName());
            }

            children = proc.getChildren(artists.get(0), 20, 100);
            assertEquals(11, children.size());
            for (int i = 0; i < children.size(); i++) {
                assertEquals(reversedByYear.get(i + 20), children.get(i).getName());
            }
        }

        /*
         * Processor collaboration
         */
        @Test
        void testSongs() {
            List<Artist> artists = proc
                .getDirectChildren(0, Integer.MAX_VALUE)
                .stream()
                .filter(a -> "20".equals(a.getName()))
                .collect(Collectors.toList());
            assertEquals(1, artists.size());

            Artist artist = artists.get(0);
            assertEquals("20", artist.getName());

            List<Album> albums = proc.getChildren(artist, 0, Integer.MAX_VALUE);
            assertEquals(1, albums.size());

            Album album = albums.get(0);
            assertEquals("AlBum!", album.getName()); // the case where album name is different
                                                     // between file and id3

            List<MediaFile> songs = albumId3Proc.getChildren(album, 0, Integer.MAX_VALUE);
            assertEquals(1, songs.size());

            MediaFile song = songs.get(0);
            assertEquals("empty", song.getName());
        }
    }

    @Test
    void testGetChildSizeOf() {
        List<Artist> artists = proc.getDirectChildren(0, 1);
        assertEquals(1, artists.size());
        assertEquals("10", artists.get(0).getName());
        assertEquals(31, proc.getChildSizeOf(artists.get(0)));
    }

    @Test
    void testAddChild() {
        DIDLContent content = new DIDLContent();
        assertEquals(0, content.getContainers().size());
        Artist artist = proc.getDirectChildren(0, 1).get(0);
        proc
            .getChildren(artist, 0, Integer.MAX_VALUE)
            .stream()
            .forEach(album -> proc.addChild(content, album));
        assertEquals(31, content.getContainers().size());
    }

    @Test
    void testToBrowseResult() {
        Artist artist = proc.getDirectChildren(0, 1).get(0);
        ParamSearchResult<Artist> searchResult = new ParamSearchResult<>();
        searchResult.getItems().add(artist);
        BrowseResult browseResult = proc.toBrowseResult(searchResult);
        assertEquals(
                """
                        <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                        xmlns:dc="http://purl.org/dc/elements/1.1/" \
                        xmlns:sec="http://www.sec.co.kr/" \
                        xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                        <container childCount="31" id="artist/0" parentID="artist" restricted="1" searchable="0">\
                        <dc:title>10</dc:title>\
                        <upnp:class>object.container.person.musicArtist</upnp:class>\
                        </container>\
                        </DIDL-Lite>\
                        """,
                browseResult.getResult());
    }
}
