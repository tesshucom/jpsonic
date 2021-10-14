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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.MusicFolderTestDataUtils;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.PlayQueue.Status;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.VideoTranscodingSettings;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.service.StreamService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.TranscodingService.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

@SuppressWarnings({ "PMD.JUnitTestsShouldIncludeAssert", "PMD.SignatureDeclareThrowsException",
        "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StreamControllerTest {

    private static final String TEST_URL = "/stream/test";
    private static final String TEST_PATH = "/var/dummy";

    private SettingsService settingsService;
    private SecurityService securityService;
    private PlayerService playerService;
    private StreamService streamService;
    private StatusService statusService;
    private TranscodingService transcodingService;

    /**
     * This instance does not launch writeStream method.
     */
    private StreamController streamController;
    private MockMvc mockMvc;

    private void initMocks(Player player, TranscodingService ts, StreamService ss) {
        this.transcodingService = ts;
        this.streamService = ss;

        settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);

        User user = new User(player.getUsername(), player.getUsername(), "");
        Mockito.when(securityService.getUserByName(player.getUsername())).thenReturn(user);
        playerService = mock(PlayerService.class);
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(player);

        statusService = mock(StatusService.class);
        TransferStatus transferStatus = mock(TransferStatus.class);
        Mockito.when(transferStatus.getPlayer()).thenReturn(player);
        Mockito.when(transferStatus.isTerminated()).thenReturn(true);
        Mockito.when(transferStatus.isActive()).thenReturn(true);
        Mockito.when(statusService.createStreamStatus(Mockito.nullable(Player.class))).thenReturn(transferStatus);
        Mockito.when(statusService.getStreamStatusesForPlayer(Mockito.nullable(Player.class)))
                .thenReturn(Arrays.asList(transferStatus));

        streamController = new StreamController(settingsService, securityService, playerService, ts, statusService, ss);

        JWTAuthenticationToken token = new JWTAuthenticationToken(Collections.emptyList(), ServiceMockUtils.ADMIN_NAME,
                null);
        SecurityContextHolder.getContext().setAuthentication(token);

        this.mockMvc = MockMvcBuilders.standaloneSetup(streamController).build();
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        Player player = new Player();
        player.setId(100);
        PlayQueue playQueue = new PlayQueue();
        playQueue.setStatus(Status.STOPPED);
        player.setPlayQueue(playQueue);
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        TranscodingService transcodingService = mock(TranscodingService.class);
        Parameters parameters = new TranscodingService.Parameters(null, null);
        Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class), Mockito.nullable(Player.class),
                Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);
        StreamService streamService = mock(StreamService.class);
        initMocks(player, transcodingService, streamService);
    }

    @Test
    @Order(0)
    void testSendForbidden() throws Exception {

        // no stream role
        User user = new User(ServiceMockUtils.ADMIN_NAME, ServiceMockUtils.ADMIN_NAME, "");
        user.setStreamRole(false);
        Mockito.when(securityService.getUserByName(ServiceMockUtils.ADMIN_NAME)).thenReturn(user);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());

        // no-jwt
        SecurityContextHolder.getContext().setAuthentication(null);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.status().reason("Streaming is forbidden for user admin"));
        Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        // no-jwt with stream role
        user.setStreamRole(true);
        Mockito.when(securityService.getUserByName(ServiceMockUtils.ADMIN_NAME)).thenReturn(user);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        // Unusual : Logging only
        user.setStreamRole(false);
        HttpServletResponse response = mock(MockHttpServletResponse.class);
        Mockito.doThrow(IOException.class).when(response).sendError(Mockito.anyInt(), Mockito.anyString());
        streamController.handleRequest(new MockHttpServletRequest(), response);
        assertEquals(0, response.getStatus());
        Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));
    }

    @Test
    @Order(1)
    void testGetMaxBitRate() throws Exception {
        MediaFile song = new MediaFile();
        song.setPath(TEST_PATH);
        Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

        ArgumentCaptor<Integer> maxBitRateCaptor = ArgumentCaptor.forClass(Integer.class);

        Parameters parameters = new TranscodingService.Parameters(null, null);
        Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class), Mockito.nullable(Player.class),
                maxBitRateCaptor.capture(), Mockito.nullable(String.class),
                Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

        // Null
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(transcodingService, Mockito.times(1)).getParameters(Mockito.nullable(MediaFile.class),
                Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                Mockito.nullable(VideoTranscodingSettings.class));
        assertNull(maxBitRateCaptor.getValue());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        // 0
        Mockito.clearInvocations(streamService);
        Mockito.clearInvocations(transcodingService);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.MAX_BIT_RATE.value(), "0"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(transcodingService, Mockito.times(1)).getParameters(Mockito.nullable(MediaFile.class),
                Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                Mockito.nullable(VideoTranscodingSettings.class));
        assertNull(maxBitRateCaptor.getValue());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        // 123
        Mockito.clearInvocations(streamService);
        Mockito.clearInvocations(transcodingService);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.MAX_BIT_RATE.value(), "123"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(transcodingService, Mockito.times(1)).getParameters(Mockito.nullable(MediaFile.class),
                Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                Mockito.nullable(VideoTranscodingSettings.class));
        assertEquals(123, maxBitRateCaptor.getValue());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));
    }

    @Nested
    @Order(2)
    class PrepareResponseTest {

        @Test
        @Order(0)
        void testAuthentication() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            // no-jwt with stream role(Pass the first certification check)
            SecurityContextHolder.getContext().setAuthentication(null);
            User user = new User(ServiceMockUtils.ADMIN_NAME, ServiceMockUtils.ADMIN_NAME, "");
            user.setStreamRole(true);
            Mockito.when(securityService.getUserByName(ServiceMockUtils.ADMIN_NAME)).thenReturn(user);

            // No folder access permission
            Mockito.when(securityService.isFolderAccessAllowed(Mockito.any(MediaFile.class), Mockito.any(String.class)))
                    .thenReturn(false);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL))
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.status().reason("Access to file 0 is forbidden for user admin"));
            Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            // With folder access permission
            Mockito.when(securityService.isFolderAccessAllowed(Mockito.any(MediaFile.class), Mockito.any(String.class)))
                    .thenReturn(true);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @Test
        @Order(1)
        void testVideoTranscoding() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            ArgumentCaptor<VideoTranscodingSettings> vtsCaptor = ArgumentCaptor
                    .forClass(VideoTranscodingSettings.class);
            Mockito.when(streamService.createInputStream(Mockito.nullable(Player.class),
                    Mockito.nullable(TransferStatus.class), Mockito.nullable(Integer.class),
                    Mockito.nullable(String.class), vtsCaptor.capture()))
                    .thenReturn(IOUtils.toInputStream("test", Charset.defaultCharset()));

            // not video
            song.setMediaType(MediaType.MUSIC);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
            Mockito.verify(streamService, Mockito.times(1)).createInputStream(Mockito.nullable(Player.class),
                    Mockito.nullable(TransferStatus.class), Mockito.nullable(Integer.class),
                    Mockito.nullable(String.class), Mockito.nullable(VideoTranscodingSettings.class));
            Mockito.verify(streamService, Mockito.never()).createVideoTranscodingSettings(
                    Mockito.nullable(MediaFile.class), Mockito.nullable(HttpServletRequest.class));
            assertNull(vtsCaptor.getValue());
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            // video
            Mockito.clearInvocations(streamService);
            song.setMediaType(MediaType.VIDEO);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
            Mockito.verify(streamService, Mockito.times(1)).createInputStream(Mockito.nullable(Player.class),
                    Mockito.nullable(TransferStatus.class), Mockito.nullable(Integer.class),
                    Mockito.nullable(String.class), Mockito.nullable(VideoTranscodingSettings.class));
            Mockito.verify(streamService, Mockito.times(1)).createVideoTranscodingSettings(
                    Mockito.nullable(MediaFile.class), Mockito.nullable(HttpServletRequest.class));
            assertNull(vtsCaptor.getValue());
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            // hls
            Mockito.clearInvocations(streamService);
            song.setMediaType(MediaType.MUSIC);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.HLS.value(), "true"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
            Mockito.verify(streamService, Mockito.times(1)).createInputStream(Mockito.nullable(Player.class),
                    Mockito.nullable(TransferStatus.class), Mockito.nullable(Integer.class),
                    Mockito.nullable(String.class), Mockito.nullable(VideoTranscodingSettings.class));
            Mockito.verify(streamService, Mockito.times(1)).createVideoTranscodingSettings(
                    Mockito.nullable(MediaFile.class), Mockito.nullable(HttpServletRequest.class));
            assertNull(vtsCaptor.getValue());
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }
    }

    @Documented
    private @interface ApplyRangeDecision {
        @interface Conditions {

            // @see TranscodingService#getParameters
            // @see TranscodingService#isRangeAllowed
            @interface IsRangeNotAllowed {
                @interface False {
                }

                @interface True {
                }
            }

            @interface MediaType {
                @interface Video {
                }

                @interface NotVideo {
                }
            }

            @interface CreateRange {
                @interface RequestHeader {
                    @interface Range {
                        @interface Null {
                        }

                        @interface NotNull {
                        }
                    }
                }

                @interface RequestParam {
                    @interface OffsetSeconds {
                        @interface Null {
                        }

                        @interface NotNull {
                        }

                        @interface Invalid {
                        }

                    }
                }

                @interface MediaFile {
                    @interface WithoutDurationOrSize {
                    }
                }

                @interface Result {
                    @interface Null {
                    }

                    @interface NotNull {
                    }
                }
            }
        }

        @interface Result {
            @interface Status {
                @interface Ok200 {
                }

                @interface Partial206 {
                }
            }

            @interface Header {
                @interface AcceptRanges {
                    @interface None {
                    }

                    @interface NotExist {
                    }

                    @interface Bytes {
                    }
                }
            }

        }
    }

    @Nested
    @Order(3)
    class ApplyRangeTest {

        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.False
        @ApplyRangeDecision.Result.Status.Ok200
        @ApplyRangeDecision.Result.Header.AcceptRanges.None
        @Test
        void na00() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.Video
        @ApplyRangeDecision.Result.Status.Ok200
        @ApplyRangeDecision.Result.Header.AcceptRanges.None
        @Test
        void na01() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.VIDEO);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.NotVideo
        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.True
        @ApplyRangeDecision.Conditions.CreateRange.RequestHeader.Range.Null
        @ApplyRangeDecision.Conditions.CreateRange.RequestParam.OffsetSeconds.Null
        @ApplyRangeDecision.Conditions.CreateRange.Result.Null
        @ApplyRangeDecision.Result.Status.Ok200
        @ApplyRangeDecision.Result.Header.AcceptRanges.NotExist
        @Test
        void cr00() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            song.setFileSize(3_200L);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            Parameters parameters = new TranscodingService.Parameters(song, null);
            parameters.setMaxBitRate(320);
            parameters.setRangeAllowed(true);
            parameters.setExpectedLength(song.getFileSize());
            Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class),
                    Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                    Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "3200"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            song.setFileSize(Long.valueOf(Integer.MAX_VALUE + 1));
            parameters.setExpectedLength(song.getFileSize());
            Mockito.clearInvocations(streamService);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "-2147483648"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.NotVideo
        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.True
        @ApplyRangeDecision.Conditions.CreateRange.RequestHeader.Range.NotNull
        @ApplyRangeDecision.Conditions.CreateRange.Result.NotNull
        @ApplyRangeDecision.Result.Status.Partial206
        @ApplyRangeDecision.Result.Header.AcceptRanges.Bytes
        @Test
        void cr01() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            song.setDurationSeconds(10);
            song.setFileSize(3_200L);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            Parameters parameters = new TranscodingService.Parameters(song, null);
            parameters.setMaxBitRate(320);
            parameters.setRangeAllowed(true);
            parameters.setExpectedLength(song.getFileSize());
            Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class),
                    Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                    Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).header("Range", "bytes=320-639"))
                    .andExpect(MockMvcResultMatchers.status().isPartialContent())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 320-639/3200"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "320"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            Mockito.clearInvocations(streamService);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).header("Range", "bytes=320-"))
                    .andExpect(MockMvcResultMatchers.status().isPartialContent())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 320-3199/3200"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "2880"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.NotVideo
        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.True
        @ApplyRangeDecision.Conditions.CreateRange.RequestHeader.Range.Null
        @ApplyRangeDecision.Conditions.CreateRange.RequestParam.OffsetSeconds.NotNull
        @ApplyRangeDecision.Conditions.CreateRange.Result.NotNull
        @ApplyRangeDecision.Result.Status.Partial206
        @ApplyRangeDecision.Result.Header.AcceptRanges.Bytes
        @Test
        void cr02() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            song.setDurationSeconds(10);
            song.setFileSize(3_300L);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            Parameters parameters = new TranscodingService.Parameters(song, null);
            parameters.setMaxBitRate(320);
            parameters.setRangeAllowed(true);
            parameters.setExpectedLength(song.getFileSize());
            Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class),
                    Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                    Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.OFFSET_SECONDS.value(), "1"))
                    .andExpect(MockMvcResultMatchers.status().isPartialContent())
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_RANGE, "bytes 330-3299/3300"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "2970"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.NotVideo
        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.True
        @ApplyRangeDecision.Conditions.CreateRange.RequestHeader.Range.Null
        @ApplyRangeDecision.Conditions.CreateRange.RequestParam.OffsetSeconds.Invalid
        @ApplyRangeDecision.Conditions.CreateRange.Result.NotNull
        @ApplyRangeDecision.Result.Status.Ok200
        @ApplyRangeDecision.Result.Header.AcceptRanges.NotExist
        @Test
        void cr03() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            song.setDurationSeconds(10);
            song.setFileSize(3_300L);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            Parameters parameters = new TranscodingService.Parameters(song, null);
            parameters.setMaxBitRate(320);
            parameters.setRangeAllowed(true);
            parameters.setExpectedLength(song.getFileSize());
            Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class),
                    Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                    Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.OFFSET_SECONDS.value(),
                    "Invalid offset test!")).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "3300"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }

        @ApplyRangeDecision.Conditions.MediaType.NotVideo
        @ApplyRangeDecision.Conditions.IsRangeNotAllowed.True
        @ApplyRangeDecision.Conditions.CreateRange.RequestHeader.Range.Null
        @ApplyRangeDecision.Conditions.CreateRange.RequestParam.OffsetSeconds.NotNull
        @ApplyRangeDecision.Conditions.CreateRange.MediaFile.WithoutDurationOrSize
        @ApplyRangeDecision.Result.Status.Ok200
        @ApplyRangeDecision.Result.Header.AcceptRanges.NotExist
        @Test
        void cr04() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            song.setMediaType(MediaType.MUSIC);
            song.setDurationSeconds(null);
            song.setFileSize(3_300L);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            Parameters parameters = new TranscodingService.Parameters(song, null);
            parameters.setMaxBitRate(320);
            parameters.setRangeAllowed(true);
            parameters.setExpectedLength(song.getFileSize());
            Mockito.when(transcodingService.getParameters(Mockito.nullable(MediaFile.class),
                    Mockito.nullable(Player.class), Mockito.nullable(Integer.class), Mockito.nullable(String.class),
                    Mockito.nullable(VideoTranscodingSettings.class))).thenReturn(parameters);

            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.OFFSET_SECONDS.value(), "1"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "3300"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            song.setDurationSeconds(10);
            song.setFileSize(null); // Assumed unreachble code
            parameters.setExpectedLength(song.getFileSize());
            Mockito.clearInvocations(streamService);
            assertThrows(NestedServletException.class, () -> mockMvc.perform(
                    MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.OFFSET_SECONDS.value(), "1")));
            Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }
    }

    @Documented
    private @interface HeaderDecision {
        @interface Conditions {
            @interface User {
                @interface NotAnonymous {
                }

                @interface Anonymous {
                }
            }

            @interface MediaFile {
                @interface File {
                    @interface Flac {
                    }
                }

                @interface BitRate955 {
                }
            }

            @interface Player {
                @interface ValidTranscoding {
                    @interface Exist {
                    }

                    @interface NotExist {
                    }
                }

                @interface TranscodeScheme {
                    @interface OFF {
                    }

                    @interface MaxBitRate320 {
                    }
                }
            }

            @interface Param {
                @interface MaxBitRate320 {
                }

                @interface FormatMp3 {
                }
            }
        }

        @interface Result {
            @interface ContentType {
                @interface AudioFlac {
                }

                @interface AudioMpeg {
                }
            }
        }
    }

    @Nested
    @Order(4)
    class ContentTypeAndDurationTest {

        private MediaFile song;
        private Player player;

        private void initMocksWithTranscoding(boolean isSetTranscodingsAll, boolean isAnonymous) {
            song = new MediaFile();
            song.setId(0);
            song.setPath(MusicFolderTestDataUtils.resolveMusicFolderPath()
                    + "/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac");
            song.setMediaType(MediaType.MUSIC);
            song.setFormat("flac");
            song.setFileSize(358_406L);
            song.setBitRate(955);
            song.setDurationSeconds(3);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            Mockito.when(mediaFileService.getMediaFile(song.getId())).thenReturn(song);

            player = new Player();
            player.setId(101);
            player.setTranscodeScheme(TranscodeScheme.OFF);
            PlayQueue playQueue = new PlayQueue();
            playQueue.setStatus(Status.STOPPED);
            playQueue.addFiles(false, song);
            player.setPlayQueue(playQueue);
            player.setUsername(isAnonymous ? JWTAuthenticationToken.USERNAME_ANONYMOUS : ServiceMockUtils.ADMIN_NAME);

            if (isAnonymous) {
                User user = new User(JWTAuthenticationToken.USERNAME_ANONYMOUS,
                        JWTAuthenticationToken.USERNAME_ANONYMOUS, "");
                Mockito.when(securityService.getUserByName(JWTAuthenticationToken.USERNAME_ANONYMOUS)).thenReturn(user);
                Mockito.when(settingsService.isInUPnPRange(Mockito.nullable(String.class))).thenReturn(true);
                Mockito.when(playerService.getGuestPlayer(Mockito.nullable(HttpServletRequest.class)))
                        .thenReturn(player);
            }

            TranscodingDao transcodingDao = mock(TranscodingDao.class);
            List<Transcoding> allTranscodings = isSetTranscodingsAll ? transcodingDao.getAllTranscodings()
                    : Collections.emptyList();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(Mockito.anyInt())).thenReturn(allTranscodings);

            TranscodingService ts = new TranscodingService(settingsService, securityService, transcodingDao,
                    playerService, null);
            StreamService ss = new StreamService(statusService, null, securityService, settingsService, ts, null,
                    mediaFileService, null, null);
            initMocks(player, ts, ss);
        }

        @HeaderDecision.Conditions.User.NotAnonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.NotExist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Result.ContentType.AudioFlac
        @Test
        void c1() throws Exception {
            initMocksWithTranscoding(false, false);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId()))).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "358406"))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/flac"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.NotAnonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Conditions.Param.MaxBitRate320
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c2() throws Exception {
            initMocksWithTranscoding(true, false);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream")
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                    .param(Attributes.Request.MAX_BIT_RATE.value(),
                            Integer.toString(TranscodeScheme.MAX_320.getMaxBitRate())))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.NotAnonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.MaxBitRate320
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c3() throws Exception {
            initMocksWithTranscoding(true, false);
            player.setTranscodeScheme(TranscodeScheme.MAX_320);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId()))).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.NotAnonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Conditions.Param.FormatMp3
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c4() throws Exception {
            initMocksWithTranscoding(true, false);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream")
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId())).param("format", "mp3"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.NotAnonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c5() throws Exception {
            initMocksWithTranscoding(true, false);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId())))
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "358406"))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.Anonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.NotExist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Result.ContentType.AudioFlac
        @Test
        void c1a() throws Exception {
            initMocksWithTranscoding(false, true);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId()))).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "358406"))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/flac"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.Anonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Conditions.Param.MaxBitRate320
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c2a() throws Exception {
            initMocksWithTranscoding(true, true);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream")
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                    .param(Attributes.Request.MAX_BIT_RATE.value(),
                            Integer.toString(TranscodeScheme.MAX_320.getMaxBitRate())))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.Anonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.MaxBitRate320
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c3a() throws Exception {
            initMocksWithTranscoding(true, true);
            player.setTranscodeScheme(TranscodeScheme.MAX_320);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId()))).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.Anonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Conditions.Param.FormatMp3
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c4a() throws Exception {
            initMocksWithTranscoding(true, true);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream")
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId())).param("format", "mp3"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_LENGTH))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @HeaderDecision.Conditions.User.Anonymous
        @HeaderDecision.Conditions.MediaFile.File.Flac
        @HeaderDecision.Conditions.MediaFile.BitRate955
        @HeaderDecision.Conditions.Player.ValidTranscoding.Exist
        @HeaderDecision.Conditions.Player.TranscodeScheme.OFF
        @HeaderDecision.Result.ContentType.AudioMpeg
        @Test
        void c5a() throws Exception {
            initMocksWithTranscoding(true, true);
            mockMvc.perform(MockMvcRequestBuilders.get("/stream").param(Attributes.Request.ID.value(),
                    Integer.toString(song.getId())))
                    .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "*"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCEPT_RANGES))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                    .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.CONTENT_LENGTH, "358406"))
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "audio/mpeg"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "3.0"));
        }

        @Test
        void testHls() throws Exception {
            MediaFile song = new MediaFile();
            song.setPath(TEST_PATH);
            Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

            // hls
            Mockito.clearInvocations(streamService);
            song.setMediaType(MediaType.MUSIC);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.HLS.value(), "true"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "video/MP2T"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            // not hls and null duration (Assumed unreachble code)
            song.setMediaType(MediaType.MUSIC);
            Mockito.clearInvocations(streamService);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "application/octet-stream"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist("X-Content-Duration"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));

            // not hls and duration
            song.setMediaType(MediaType.MUSIC);
            song.setDurationSeconds(10);
            Mockito.clearInvocations(streamService);
            mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.header().string("Content-Type", "application/octet-stream"))
                    .andExpect(MockMvcResultMatchers.header().string("X-Content-Duration", "10.0"));
            Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                    Mockito.nullable(TransferStatus.class));
        }
    }

    @Test
    @Order(5)
    void testWriteVerboseLog() throws Exception {
        Mockito.when(settingsService.isVerboseLogPlaying()).thenReturn(true);
        MediaFile song = new MediaFile();
        song.setPath(TEST_PATH);
        song.setDurationSeconds(10);
        song.setFileSize(3_300L);
        Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

        mockMvc.perform(MockMvcRequestBuilders.head(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.never()).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));
    }

    @Test
    @Order(6)
    void testWriteStream() throws Exception {
        // Not yet implemented!
    }

    @Test
    @Order(7)
    void testIsAliveStream() throws Exception {
        // Not yet implemented!
    }

    @Test
    @Order(8)
    void testWriteErrorLog() throws Exception {
        Mockito.when(settingsService.isVerboseLogPlaying()).thenReturn(true);
        MediaFile song = new MediaFile();
        song.setPath(TEST_PATH);
        song.setDurationSeconds(10);
        song.setFileSize(3_300L);
        Mockito.when(streamService.getSingleFile(Mockito.any(HttpServletRequest.class))).thenReturn(song);

        Mockito.doAnswer(invocation -> {
            throw new IOException("testWriteErrorLog1");
        }).when(settingsService).getBufferSize();
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        Mockito.doAnswer(invocation -> {
            throw new org.eclipse.jetty.io.EofException("testWriteErrorLog2");
        }).when(settingsService).getBufferSize();
        Mockito.clearInvocations(streamService);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));

        Mockito.doAnswer(invocation -> {
            throw new org.apache.catalina.connector.ClientAbortException("testWriteErrorLog3");
        }).when(settingsService).getBufferSize();
        Mockito.clearInvocations(streamService);
        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL)).andExpect(MockMvcResultMatchers.status().isOk());
        Mockito.verify(streamService, Mockito.times(1)).removeStreamStatus(Mockito.nullable(User.class),
                Mockito.nullable(TransferStatus.class));
    }

    @Test
    @Order(9)
    void testGet() throws Exception {
        // Playlist case only (Because path coverage are covered by other than this case)
        int playlistId = 99;
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.doNothing().when(streamService).setUpPlayQueue(Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class), Mockito.any(Player.class), idCaptor.capture());

        mockMvc.perform(MockMvcRequestBuilders.get(TEST_URL).param(Attributes.Request.PLAYLIST.value(),
                Integer.toString(playlistId))).andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(streamService).setUpPlayQueue(Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class), Mockito.any(Player.class), Mockito.any(Integer.class));
        assertEquals(playlistId, idCaptor.getValue());
    }
}
