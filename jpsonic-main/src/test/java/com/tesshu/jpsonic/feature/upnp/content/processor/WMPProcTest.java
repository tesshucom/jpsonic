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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Level;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.model.TranscodingDefinition;
import com.tesshu.jpsonic.domain.model.TranscodingDefinition.BitRateLimit;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpKeyManager;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.ResolvedAudioTranscodingParameters;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLObject.Property.UPNP.AUTHOR;
import org.jupnp.support.model.item.MusicTrack;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class WMPProcTest {

    private MusicFolderProvider musicFolderProvider;
    private MediaFileProvider mediaFileProvider;
    private TranscodingParametersPlanner parametersPlanner;
    private WMPProc wmpProcessor;

    @BeforeEach
    void setup() throws URISyntaxException {
        musicFolderProvider = mock(MusicFolderProvider.class);
        mediaFileProvider = mock(MediaFileProvider.class);
        UpnpKeyManager upnpKeyManager = mock(UpnpKeyManager.class);
        when(upnpKeyManager.getKey()).thenReturn("dummyKey");

        SettingsFacade settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(SKeys.transcoding.preferredFormat, "flac")
            .withString(UPnPSKeys.basic.baseLanUrl, "https://192.168.1.1:4040")
            .build();

        UpnpKeyManager keyManager = mock(UpnpKeyManager.class);
        when(keyManager.getKey()).thenReturn("dummyKey");
        UpnpPayloadCodec codec = new UpnpPayloadCodec(upnpKeyManager);
        parametersPlanner = mock(TranscodingParametersPlanner.class);

        UPnPDIDLFactory factory = new UPnPDIDLFactory(settingsFacade, codec, mediaFileProvider,
                mock(PlayerProvider.class), parametersPlanner);

        wmpProcessor = new WMPProc(musicFolderProvider, mediaFileProvider, factory);
        TestCaseUtils.setLogLevel(WMPProc.class, Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
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
        assertEmpty(wmpProcessor
            .getBrowseResult(
                    "upnp:class derivedfrom \"object.container.playlistContainer\" and @refID exists false",
                    "dc:title,microsoft:folderPath", 0, 0));

        assertEmpty(wmpProcessor
            .getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                    "dc:title,microsoft:folderPath", 0, 0));
    }

    @Test
    void testCreateMusicTrack() {
        int parentId = 200;
        MediaFile parent = new MediaFile(parentId, "parentPath", 0, "mp3", "ALBUM", 256, 60, 9999,
                "artist", "album", "title", "albumArtist", 0, "genre", 2021, "thumbUri", "composer",
                "reading", "#", "comment");
        when(mediaFileProvider.requireMediaFile(parentId)).thenReturn(parent);

        MediaFile mediaFile = new MediaFile(1, "path1", parentId, "mp3", "MUSIC", 256, 60, 9999,
                "artist", "album", "title", "albumArtist", 0, "genre", 2026, "thumbUri", "composer",
                "reading", "#", "comment");

        TranscodingDefinition availableDefinition = new TranscodingDefinition(3, "Mp3Only",
                List.of("mp3"), "aac", List.of("%b"), true);
        ResolvedAudioTranscodingParameters param = new ResolvedAudioTranscodingParameters(false,
                mediaFile, availableDefinition, BitRateLimit.MAX_320);
        when(parametersPlanner
            .resolveAudioTranscodingParameters(nullable(Player.class), nullable(MediaFile.class),
                    nullable(Integer.class), nullable(String.class)))
            .thenReturn(param);

        MusicTrack musicTrack = wmpProcessor.createMusicTrack(mediaFile);
        assertEquals(Integer.toString(parentId), musicTrack.getParentID());
        assertEquals(1, musicTrack.getArtists().length);
        assertEquals(1, musicTrack.getGenres().length);
        assertEquals("genre", musicTrack.getGenres()[0]);
        assertEquals("2026-01-01", musicTrack.getDate());
        assertEquals(1, musicTrack.getProperties(AUTHOR.class).length);
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
            BrowseResult result = wmpProcessor
                .getBrowseResult(
                        "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false",
                        "*", 0, 0);
            assertEquals("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:sec="http://www.sec.co.kr/" \
                    xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"/>\
                    """, result.getResult());
            assertEquals(0, result.getCount().getValue());
            assertEquals(0, result.getTotalMatches().getValue());

            int parentId = 200;
            MediaFile parent = new MediaFile(parentId, "parentPath", 0, "mp3", "ALBUM", 256, 60,
                    9999, "artist", "album", "title", "albumArtist", 0, "genre", 2021, "thumbUri",
                    "composer", "reading", "#", "comment");
            when(mediaFileProvider.requireMediaFile(parentId)).thenReturn(parent);

            MediaFile mediaFile = new MediaFile(1, "path2", parentId, "flac", "MUSIC", 25_600_000,
                    60, 9999, "artist", "album", "dummy title", "albumArtist", 0, "genre", 2026,
                    "thumbUri", "composer", "reading", "#", "comment");
            assertFalse(mediaFile.isVideo());
            List<MediaFile> songs = Arrays.asList(mediaFile);

            MusicFolder mf = new MusicFolder(0, "path3", "dummy", false, null, 0, false);
            List<MusicFolder> folders = Arrays.asList(mf);

            when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
            when(mediaFileProvider.findSongs(anyList(), anyLong(), anyLong())).thenReturn(songs);
            when(mediaFileProvider.countSongs(anyList())).thenReturn(20);

            TranscodingDefinition availableDefinition = new TranscodingDefinition(3, "Mp3Only",
                    List.of("mp3"), "aac", List.of("%b"), true);
            ResolvedAudioTranscodingParameters param = new ResolvedAudioTranscodingParameters(false,
                    mediaFile, availableDefinition, BitRateLimit.MAX_320);
            when(parametersPlanner
                .resolveAudioTranscodingParameters(nullable(Player.class),
                        nullable(MediaFile.class), nullable(Integer.class), nullable(String.class)))
                .thenReturn(param);

            result = wmpProcessor
                .getBrowseResult(
                        "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false",
                        "*", 1, 1);
            assertEquals("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                    \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                    \sxmlns:sec="http://www.sec.co.kr/"\
                    \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                    <item id="1" parentID="200" restricted="1">\
                    <dc:title>dummy title</dc:title>\
                    <dc:creator>artist</dc:creator>\
                    <upnp:class>object.item.audioItem.musicTrack</upnp:class>\
                    <upnp:album>album</upnp:album>\
                    <upnp:genre>genre</upnp:genre>\
                    <upnp:albumArtURI>\
                    https://192.168.1.1:4040/ext/upnp/art/ec2432921fa73a.jpeg\
                    </upnp:albumArtURI>\
                    <upnp:artist>albumArtist</upnp:artist>\
                    <upnp:author role="composer">composer</upnp:author>\
                    <dc:date>2026-01-01</dc:date>\
                    <dc:description>comment</dc:description>\
                    <res duration="0:01:00.0" protocolInfo="http-get:*:audio/flac:*">\
                    https://192.168.1.1:4040/ext/upnp/stream/ec2437991b.flac\
                    </res>\
                    </item>\
                    </DIDL-Lite>\
                    """, result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(20, result.getTotalMatches().getValue());
        }

        @Test
        void testVideo() {

            BrowseResult result = wmpProcessor
                .getBrowseResult(
                        "upnp:class derivedfrom \"object.item.videoItem\" and @refID exists false",
                        "*", 0, 0);
            assertEquals("""
                    <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" \
                    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:sec="http://www.sec.co.kr/" \
                    xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"/>\
                    """, result.getResult());
            assertEquals(0, result.getCount().getValue());
            assertEquals(0, result.getTotalMatches().getValue());

            int parentId = 200;
            MediaFile parent = new MediaFile(parentId, "parentPath", 0, "mp3", "ALBUM", 256, 60,
                    9999, "artist", "album", "title", "albumArtist", 0, "genre", 2021, "thumbUri",
                    "composer", "reading", "#", "comment");
            when(mediaFileProvider.requireMediaFile(parentId)).thenReturn(parent);

            MediaFile mediaFile = new MediaFile(1, "path5", parentId, "mp4", "VIDEO", 256, 60, 9999,
                    "artist", "album", "dummy title", "albumArtist", 0, "genre", 2026, "thumbUri",
                    "composer", "reading", "#", "comment");
            assertTrue(mediaFile.isVideo());

            List<MediaFile> songs = Arrays.asList(mediaFile);
            MusicFolder mf = new MusicFolder(0, "path6", "dummy", true, null, 0, false);
            List<MusicFolder> folders = Arrays.asList(mf);
            when(musicFolderProvider.getGuestFolders()).thenReturn(folders);
            when(mediaFileProvider.findVideos(anyList(), anyLong(), anyLong())).thenReturn(songs);

            TranscodingDefinition availableDefinition = new TranscodingDefinition(3, "Mp3Only",
                    List.of("mp3"), "aac", List.of("%b"), true);
            ResolvedAudioTranscodingParameters param = new ResolvedAudioTranscodingParameters(false,
                    mediaFile, availableDefinition, BitRateLimit.MAX_320);
            when(parametersPlanner
                .resolveAudioTranscodingParameters(nullable(Player.class),
                        nullable(MediaFile.class), nullable(Integer.class), nullable(String.class)))
                .thenReturn(param);

            when(mediaFileProvider.countVideos(anyList())).thenReturn(20);

            result = wmpProcessor
                .getBrowseResult(
                        "upnp:class derivedfrom \"object.item.videoItem\" and @refID exists false",
                        "*", 1, 1);

            assertEquals(
                    """
                            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                            \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                            \sxmlns:sec="http://www.sec.co.kr/"\
                            \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                            <item id="1" parentID="200" restricted="1">\
                            <dc:title>dummy title</dc:title>\
                            <dc:creator>artist</dc:creator>\
                            <upnp:class>object.item.videoItem</upnp:class>\
                            <upnp:albumArtURI>https://192.168.1.1:4040/ext/upnp/art/ec2432921fa73a.jpeg</upnp:albumArtURI>\
                            <upnp:genre>genre</upnp:genre>\
                            <upnp:author role="composer">composer</upnp:author>\
                            <dc:description>comment</dc:description>\
                            <res duration="0:01:00.0" protocolInfo="http-get:*:video/mp4:*">\
                            https://192.168.1.1:4040/ext/upnp/stream/ec2437991a.mp4\
                            </res>\
                            </item>\
                            </DIDL-Lite>\
                            """,
                    result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(20, result.getTotalMatches().getValue());
        }

        @Test
        void testImage() {
            assertEmpty(wmpProcessor
                .getBrowseResult(
                        "upnp:class derivedfrom \"object.item.imageItem\" and @refID exists false",
                        "*", 0, 0));
        }

        @Test
        void testSingleAudio() {

            int parentId = 200;
            MediaFile parent = new MediaFile(parentId, "parentPath", 0, "mp3", "ALBUM", 256, 60,
                    9999, "artist", "album", "title", "albumArtist", 0, "genre", 2021, "thumbUri",
                    "composer", "reading", "#", "comment");
            when(mediaFileProvider.requireMediaFile(parentId)).thenReturn(parent);

            int id = 99;
            MediaFile mediaFile = new MediaFile(id, "path4", parentId, "mp3", "MUSIC", 256, 60,
                    9999, "artist", "album", "dummy title", "albumArtist", 0, "genre", 2026,
                    "thumbUri", "composer", "reading", "#", "comment");
            when(mediaFileProvider.requireMediaFile(id)).thenReturn(mediaFile);

            TranscodingDefinition availableDefinition = new TranscodingDefinition(3, "Mp3Only",
                    List.of("mp3"), "aac", List.of("%b"), true);
            ResolvedAudioTranscodingParameters param = new ResolvedAudioTranscodingParameters(false,
                    mediaFile, availableDefinition, BitRateLimit.MAX_320);
            when(parametersPlanner
                .resolveAudioTranscodingParameters(nullable(Player.class),
                        nullable(MediaFile.class), nullable(Integer.class), nullable(String.class)))
                .thenReturn(param);

            BrowseResult result = wmpProcessor.getBrowseResult("dc:title = \"99\"", "*", 0, 0);
            assertEquals(
                    """
                            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"\
                            \sxmlns:dc="http://purl.org/dc/elements/1.1/"\
                            \sxmlns:sec="http://www.sec.co.kr/"\
                            \sxmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">\
                            <item id="99" parentID="200" restricted="1">\
                            <dc:title>dummy title</dc:title>\
                            <dc:creator>artist</dc:creator>\
                            <upnp:class>object.item.audioItem.musicTrack</upnp:class>\
                            <upnp:album>album</upnp:album>\
                            <upnp:genre>genre</upnp:genre>\
                            <upnp:albumArtURI>https://192.168.1.1:4040/ext/upnp/art/ec2432921fa73a.jpeg</upnp:albumArtURI>\
                            <upnp:artist>albumArtist</upnp:artist>\
                            <upnp:author role="composer">composer</upnp:author>\
                            <dc:date>2026-01-01</dc:date>\
                            <dc:description>comment</dc:description>\
                            <res duration="0:01:00.0" protocolInfo="http-get:*:audio/mpeg:*">\
                            https://192.168.1.1:4040/ext/upnp/stream/ec2434b11b.mp3\
                            </res>\
                            </item>\
                            </DIDL-Lite>\
                            """,
                    result.getResult());
            assertEquals(1, result.getCount().getValue());
            assertEquals(1, result.getTotalMatches().getValue());
        }

        @Test
        void testUnknownQuery() {
            assertNull(wmpProcessor
                .getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                        "*", 0, 0));
        }
    }

    @Test
    void testUnknownFilter() {
        assertNull(wmpProcessor
            .getBrowseResult("upnp:class derivedfrom \"object.container.playlistContainer\"",
                    "dc:title,apple:folderPath", 0, 0));
    }
}
