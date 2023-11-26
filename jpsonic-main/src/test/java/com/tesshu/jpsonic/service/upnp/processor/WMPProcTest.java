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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.AUTHOR;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("PMD.TooManyStaticImports")
class WMPProcTest {

    private MediaFileService mediaFileService;
    private UpnpProcessorUtil util;
    private WMPProc wmpProcessor;

    @BeforeEach
    public void setup() throws URISyntaxException {
        mediaFileService = mock(MediaFileService.class);
        util = mock(UpnpProcessorUtil.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getDlnaBaseLANURL()).thenReturn("https://192.168.1.1:4040");
        JWTSecurityService jwtSecurityService = mock(JWTSecurityService.class);
        UriComponentsBuilder builder = mock(UriComponentsBuilder.class);
        UriComponents components = mock(UriComponents.class);
        when(components.encode()).thenReturn(components);
        when(components.toUri()).thenReturn(new URI("https://192.168.1.1/dummyArt.jpg"));
        when(builder.build()).thenReturn(components);
        when(builder.toUriString()).thenReturn("https://192.168.1.1/dummyResource.mp3");
        when(jwtSecurityService.addJWTToken(any(UriComponentsBuilder.class))).thenReturn(builder);
        PlayerService playerService = mock(PlayerService.class);
        TranscodingService transcodingService = mock(TranscodingService.class);
        UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService, jwtSecurityService, mediaFileService,
                playerService, transcodingService);
        wmpProcessor = new WMPProc(util, factory, mediaFileService);
        TestCaseUtils.setLogLevel(WMPProc.class, Level.DEBUG);
    }

    @AfterEach
    public void tearDown() {
        TestCaseUtils.setLogLevel(WMPProc.class, Level.WARN);
    }

    @Test
    void testIsAvailable() {
        assertTrue(wmpProcessor.isAvailable("dc:title,microsoft:folderPath"));
        assertTrue(wmpProcessor.isAvailable("*"));
        assertFalse(wmpProcessor.isAvailable("dc:title,apple:folderPath"));
    }

    @Test
    void testFolderPath() {
        assertEmpty(wmpProcessor.getBrowseResult(
                "upnp:class derivedfrom \"object.container.playlistContainer\" and @refID exists false",
                "dc:title,microsoft:folderPath", 0, 0));

        assertEmpty(wmpProcessor.getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                "dc:title,microsoft:folderPath", 0, 0));

    }

    @Test
    void testCreateMusicTrack() {

        MediaFile m = new MediaFile();
        m.setPathString("path1");
        // when(factory.toRes(m)).thenReturn(null);
        // when(factory.toAlbumArt(m)).thenReturn(null);

        MusicTrack mt = wmpProcessor.createMusicTrack(m);
        assertNull(mt.getParentID());
        assertEquals(0, mt.getArtists().length);
        assertEquals(0, mt.getGenres().length);
        assertNull(mt.getDate());
        assertEquals(0, mt.getProperties(AUTHOR.class).length);

        int parentId = 200;
        MediaFile parent = new MediaFile();
        parent.setId(parentId);
        parent.setPathString("parentPath");
        when(mediaFileService.getParentOf(m)).thenReturn(parent);
        m.setAlbumArtist("albumArtist");
        m.setGenre("genre");
        m.setYear(2021);
        m.setComposer("composer");

        mt = wmpProcessor.createMusicTrack(m);
        assertEquals(Integer.toString(parentId), mt.getParentID());
        assertEquals(1, mt.getGenres().length);
        assertEquals("genre", mt.getGenres()[0]);
        assertEquals("2021-01-01", mt.getDate());

        // Please check using WMP as it is difficult to test with Mock.
        // There are two perspectives: "AlbumArtist is transferred instead of Artist" and "Composer
        // is transferred".

        // assertEquals("albumArtist", mt.getArtists()[0].getName());
        // assertEquals(1, mt.getArtists().length);
        // assertNull(mt.getArtists()[0].getRole());
        // assertEquals(1, mt.getProperties(AUTHOR.class).length);
        // assertEquals("author", mt.getProperties(AUTHOR.class)[0].getDescriptorName());
        // assertEquals("composer", mt.getProperties(AUTHOR.class)[0].getValue().getName());
    }

    private void assertEmpty(BrowseResult result) {
        assertEquals(StringUtils.EMPTY, result.getResult());
        assertEquals(0, result.getCount().getValue());
        assertEquals(0, result.getTotalMatches().getValue());
    }

    @Nested
    class GetSomthingTest {

        @Test
        void testAudio() {
            BrowseResult result = wmpProcessor.getBrowseResult(
                    "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false", "*", 0, 0);
            assertEquals("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:sec="http://www.sec.co.kr/" \
                    xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"/>\
                    """, result.getResult());
            assertEquals(0, result.getCount().getValue());
            assertEquals(0, result.getTotalMatches().getValue());

            when(util.getGuestFolders()).thenReturn(Collections.emptyList());

            MediaFile m = new MediaFile();
            m.setPathString("path2");
            m.setTitle("dummy title");
            List<MediaFile> songs = Arrays.asList(m);
            MusicFolder mf = new MusicFolder(0, "path3", "dummy", true, null, 0);
            List<MusicFolder> folders = Arrays.asList(mf);
            when(util.getGuestFolders()).thenReturn(folders);
            when(mediaFileService.getSongs(anyLong(), anyLong(), anyList())).thenReturn(songs);
            int parentId = 200;
            MediaFile parent = new MediaFile();
            parent.setId(parentId);
            parent.setPathString("parentPath2");
            when(mediaFileService.getParentOf(m)).thenReturn(parent);
            when(mediaFileService.countSongs(anyList())).thenReturn(20L);

            result = wmpProcessor.getBrowseResult(
                    "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false", "*", 1, 1);

            assertEquals(
                    """
                            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                            \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                            \sxmlns:sec="http://www.sec.co.kr/"\
                            \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                            <item id="0" parentID="200" restricted="1">\
                            <dc:title>dummy title</dc:title>\
                            <upnp:class>object.item.audioItem.musicTrack</upnp:class>\
                            <upnp:album/>\
                            <upnp:originalTrackNumber/>\
                            <upnp:albumArtURI>https://192.168.1.1/dummyArt.jpg</upnp:albumArtURI>\
                            <dc:description/>\
                            <res protocolInfo="http-get:*:application/octet-stream:*">https://192.168.1.1/dummyResource.mp3</res>\
                            </item>\
                            </DIDL-Lite>\
                            """,
                    result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(20, result.getTotalMatches().getValue());
        }

        @Test
        void testVideo() {
            BrowseResult result = wmpProcessor.getBrowseResult(
                    "upnp:class derivedfrom \"object.item.videoItem\" and @refID exists false", "*", 0, 0);
            assertEquals("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:sec="http://www.sec.co.kr/" \
                    xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"/>\
                    """, result.getResult());
            assertEquals(0, result.getCount().getValue());
            assertEquals(0, result.getTotalMatches().getValue());

            when(util.getGuestFolders()).thenReturn(Collections.emptyList());

            MediaFile m = new MediaFile();
            m.setPathString("path5");
            m.setTitle("dummy title");
            List<MediaFile> songs = Arrays.asList(m);
            MusicFolder mf = new MusicFolder(0, "path6", "dummy", true, null, 0);
            List<MusicFolder> folders = Arrays.asList(mf);
            when(util.getGuestFolders()).thenReturn(folders);
            when(mediaFileService.getVideos(anyLong(), anyLong(), anyList())).thenReturn(songs);
            int parentId = 200;
            MediaFile parent = new MediaFile();
            parent.setId(parentId);
            parent.setPathString("parentPath1");
            when(mediaFileService.getParentOf(m)).thenReturn(parent);
            when(mediaFileService.countVideos(anyList())).thenReturn(20L);

            result = wmpProcessor.getBrowseResult(
                    "upnp:class derivedfrom \"object.item.videoItem\" and @refID exists false", "*", 1, 1);

            assertEquals(
                    """
                            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                            \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                            \sxmlns:sec="http://www.sec.co.kr/"\
                            \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                            <item id="0" parentID="200" restricted="1">\
                            <dc:title>dummy title</dc:title>\
                            <upnp:class>object.item.videoItem</upnp:class>\
                            <upnp:albumArtURI>https://192.168.1.1/dummyArt.jpg</upnp:albumArtURI>\
                            <dc:description/>\
                            <res protocolInfo="http-get:*:application/octet-stream:*">https://192.168.1.1/dummyResource.mp3</res>\
                            </item>\
                            </DIDL-Lite>\
                            """,
                    result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(20, result.getTotalMatches().getValue());
        }

        @Test
        void testImage() {
            assertEmpty(wmpProcessor.getBrowseResult(
                    "upnp:class derivedfrom \"object.item.imageItem\" and @refID exists false", "*", 0, 0));
        }

        @Test
        void testSingleAudio() {
            int id = 99;
            MediaFile m = new MediaFile();
            m.setId(id);
            m.setPathString("path4");
            m.setTitle("dummy title");
            when(mediaFileService.getMediaFileStrict(id)).thenReturn(m);
            assertEmpty(wmpProcessor.getBrowseResult("dc:title = \"99\"", "*", 0, 0));

            m.setMediaType(MediaType.MUSIC);
            int parentId = 200;
            MediaFile parent = new MediaFile();
            parent.setId(parentId);
            parent.setPathString("parentPath4");
            when(mediaFileService.getParentOf(m)).thenReturn(parent);
            BrowseResult result = wmpProcessor.getBrowseResult("dc:title = \"99\"", "*", 0, 0);
            assertEquals(
                    """
                            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                            \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                            \sxmlns:sec="http://www.sec.co.kr/"\
                            \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                            <item id="99" parentID="200" restricted="1">\
                            <dc:title>dummy title</dc:title>\
                            <upnp:class>object.item.audioItem.musicTrack</upnp:class>\
                            <upnp:album/>\
                            <upnp:originalTrackNumber/>\
                            <upnp:albumArtURI>https://192.168.1.1/dummyArt.jpg</upnp:albumArtURI>\
                            <dc:description/>\
                            <res protocolInfo="http-get:*:application/octet-stream:*">https://192.168.1.1/dummyResource.mp3</res>\
                            </item>\
                            </DIDL-Lite>\
                            """,
                    result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(1, result.getTotalMatches().getValue());
        }

        @Test
        void testUnknownQuery() {
            assertNull(wmpProcessor.getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                    "*", 0, 0));
        }
    }

    @Test
    void testUnknownFilter() {
        assertNull(wmpProcessor.getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                "dc:title,apple:folderPath", 0, 0));
    }
}
