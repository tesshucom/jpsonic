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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.StatusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class NowPlayingControllerTest {

    private PlayerService playerService;
    private StatusService statusService;
    private MediaFileService mediaFileService;
    private SecurityService securityService;
    private NowPlayingController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        playerService = mock(PlayerService.class);
        statusService = mock(StatusService.class);
        mediaFileService = mock(MediaFileService.class);
        securityService = mock(SecurityService.class);
        controller = new NowPlayingController(playerService, statusService, mediaFileService,
                securityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = "admin")
    void testGetToHome() throws Exception {
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/nowPlaying.view"))
            .andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.HOME.value()))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
            .andReturn();
        assertNotNull(result);
    }

    @Test
    @WithMockUser(username = "admin")
    void testGetToMain() throws Exception {
        Player player = new Player();
        Mockito
            .when(playerService
                .getPlayer(Mockito.nullable(HttpServletRequest.class),
                        Mockito.nullable(HttpServletResponse.class)))
            .thenReturn(player);
        TransferStatus status = new TransferStatus();
        status.setPathString("/dummy");
        Mockito
            .when(statusService.getStreamStatusesForPlayer(player))
            .thenReturn(Arrays.asList(status));
        Mockito.when(securityService.isReadAllowed(status.toPath())).thenReturn(true);
        MediaFile nowPlaing = new MediaFile();
        Mockito.when(mediaFileService.getMediaFile(status.toPath())).thenReturn(nowPlaing);
        MediaFile parent = new MediaFile();
        parent.setId(99);
        Mockito.when(mediaFileService.getParentOf(nowPlaing)).thenReturn(parent);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/nowPlaying.view"))
            .andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("main.view?id=99"))
            .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
            .andReturn();
        assertNotNull(result);
    }

    @Documented
    private @interface GetNowPlayingFileDecisions {
        @interface Conditions {
            @interface Statuses {
                @interface Empty {
                }

                @interface NotEmpty {
                }

                @interface Path {
                    @interface ReadAllowed {

                    }

                    @interface NotReadAllowed {

                    }
                }
            }

            @interface MediaFile {
                @interface NonNull {
                }

                @interface Null {
                }
            }
        }

        @interface Result {
            @interface Null {
            }

            @interface NonNull {
            }
        }
    }

    @Nested
    class GetNowPlayingFileTest {

        @Test
        @GetNowPlayingFileDecisions.Conditions.Statuses.Empty
        @GetNowPlayingFileDecisions.Result.Null
        void c1() {
            Player player = new Player();
            assertNull(controller.getNowPlayingFile(player));
        }

        @Test
        @GetNowPlayingFileDecisions.Conditions.Statuses.NotEmpty
        @GetNowPlayingFileDecisions.Conditions.Statuses.Path.NotReadAllowed
        @GetNowPlayingFileDecisions.Result.Null
        void c2() {
            Player player = new Player();
            TransferStatus status = new TransferStatus();
            status.setPathString("/dummy");
            Mockito
                .when(statusService.getStreamStatusesForPlayer(player))
                .thenReturn(Arrays.asList(status));
            Mockito.when(securityService.isReadAllowed(status.toPath())).thenReturn(false);
            assertNull(controller.getNowPlayingFile(player));
        }

        @Test
        @GetNowPlayingFileDecisions.Conditions.Statuses.NotEmpty
        @GetNowPlayingFileDecisions.Conditions.Statuses.Path.ReadAllowed
        @GetNowPlayingFileDecisions.Conditions.MediaFile.NonNull
        @GetNowPlayingFileDecisions.Result.NonNull
        void c3() {
            Player player = new Player();
            TransferStatus status = new TransferStatus();
            status.setPathString("/dummy");
            Mockito
                .when(statusService.getStreamStatusesForPlayer(player))
                .thenReturn(Arrays.asList(status));
            Mockito.when(securityService.isReadAllowed(status.toPath())).thenReturn(true);
            Mockito
                .when(mediaFileService.getMediaFile(status.toPath()))
                .thenReturn(new MediaFile());
            assertNotNull(controller.getNowPlayingFile(player));
        }

        @Test
        @GetNowPlayingFileDecisions.Conditions.Statuses.NotEmpty
        @GetNowPlayingFileDecisions.Conditions.Statuses.Path.ReadAllowed
        @GetNowPlayingFileDecisions.Conditions.MediaFile.Null
        @GetNowPlayingFileDecisions.Result.Null
        void c4() {
            Player player = new Player();
            TransferStatus status = new TransferStatus();
            status.setPathString("/dummy");
            Mockito
                .when(statusService.getStreamStatusesForPlayer(player))
                .thenReturn(Arrays.asList(status));
            Mockito.when(securityService.isReadAllowed(status.toPath())).thenReturn(true);
            assertNull(controller.getNowPlayingFile(player));
        }
    }
}
