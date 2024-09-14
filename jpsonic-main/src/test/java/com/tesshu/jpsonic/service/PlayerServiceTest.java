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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.PlayerDao;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.dao.UserDao;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

@SuppressWarnings("PMD.TooManyStaticImports")
class PlayerServiceTest {

    private PlayerDao playerDao;
    private UserDao userDao;
    private TranscodingDao transcodingDao;
    private TranscodingService transcodingService;
    private PlayerService playerService;

    @BeforeEach
    public void setup() throws ExecutionException {
        playerDao = mock(PlayerDao.class);
        userDao = mock(UserDao.class);
        transcodingDao = mock(TranscodingDao.class);
        transcodingService = mock(TranscodingService.class);
        List<Transcoding> transcodings = new ArrayList<>(transcodingDao.getAllTranscodings());
        Transcoding inactiveTranscoding = new Transcoding(10, "aac",
                "mp3 ogg oga m4a flac wav wma aif aiff ape mpc shn", "aac",
                "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -", null, null, false);
        transcodings.add(inactiveTranscoding);
        Mockito.when(transcodingService.getAllTranscodings()).thenReturn(transcodings);
        MusicFolderService musicFolderService = mock(MusicFolderService.class);
        playerService = new PlayerService(playerDao, null, new SecurityService(userDao, null, musicFolderService),
                transcodingService);
    }

    @Test
    void testInit() {
        playerService.init();
        verify(playerDao, times(1)).deleteOldPlayers(anyInt());
    }

    @Documented
    private @interface GetGuestPlayerLastSeenDecision {
        @interface Conditions {
            @interface IsExists {
                @interface False {
                }

                @interface True {
                    @interface LastSeen {
                        @interface Today {
                        }

                        @interface Old {
                        }
                    }
                }
            }

            @interface Request {
                @interface Null {
                }

                @interface WithIp {
                }
            }
        }

        @interface Results {
            @interface LastSeen {
                @interface Today {
                }
            }
        }
    }

    /**
     *
     * @see PlayerDao#deleteOldPlayers(int)
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP") // It's test code!
    @Nested
    class GetGuestPlayerTest {

        private static final String PLAYER_IP = "192.168.1.2";

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.False
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c01() {

            Calendar today = Calendar.getInstance();
            today.setTime(Date.from(now()));

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(Date.from(player.getLastSeen()));
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            verify(playerDao, times(1)).createPlayer(Mockito.any(Player.class));
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True.LastSeen.Today
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c02() {

            Calendar today = Calendar.getInstance();
            today.setTime(Date.from(now()));
            Player dummy = playerService.getGuestPlayer(null);
            Player playerWithIp = new Player();
            playerWithIp.setIpAddress(PLAYER_IP);
            playerWithIp.setLastSeen(now());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(playerWithIp, dummy));

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(Date.from(player.getLastSeen()));
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True.LastSeen.Old
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c03() {

            Player dummy = playerService.getGuestPlayer(null);
            Calendar old = Calendar.getInstance();
            old.setTime(Date.from(dummy.getLastSeen()));
            old.add(Calendar.DATE, -2);
            dummy.setLastSeen(old.getTime().toInstant());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(dummy));

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(Date.from(player.getLastSeen()));
            Calendar today = Calendar.getInstance();
            today.setTime(Date.from(now()));
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            verify(playerDao, times(1)).updatePlayer(Mockito.any(Player.class));
            verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.False
        @GetGuestPlayerLastSeenDecision.Conditions.Request.WithIp
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c04() {

            Calendar today = Calendar.getInstance();
            today.setTime(Date.from(now()));

            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Collections.emptyList());

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(PLAYER_IP);

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(req);
            assertEquals(PLAYER_IP, player.getIpAddress());
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(Date.from(player.getLastSeen()));
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            verify(playerDao, times(1)).createPlayer(Mockito.any(Player.class));
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True
        @GetGuestPlayerLastSeenDecision.Conditions.Request.WithIp
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c05() {

            Calendar today = Calendar.getInstance();
            today.setTime(Date.from(now()));

            Player dummy = playerService.getGuestPlayer(null);
            Player playerWithIp = new Player();
            playerWithIp.setIpAddress(PLAYER_IP);
            playerWithIp.setLastSeen(now());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(dummy, playerWithIp));

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(PLAYER_IP);

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(req);
            assertEquals(PLAYER_IP, player.getIpAddress());
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(Date.from(player.getLastSeen()));
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

    }

    @Nested
    class CreatePlayerTest {

        @Test
        void testCreatePlayer() {
            Player player = new Player();

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).createPlayer(playerCaptor.capture());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Transcoding>> transcodingsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.doNothing().when(transcodingService).setTranscodingsForPlayer(Mockito.any(Player.class),
                    transcodingsCaptor.capture());

            playerService.createPlayer(player);

            Player createdPlayer = playerCaptor.getValue();
            assertNull(createdPlayer.getUsername());
            assertEquals(TranscodeScheme.OFF, createdPlayer.getTranscodeScheme());
            assertEquals(transcodingDao.getAllTranscodings(), transcodingsCaptor.getValue());
        }

        @SuppressWarnings("unchecked")
        @Test
        void testGuestPlayer() {
            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).createPlayer(playerCaptor.capture());
            ArgumentCaptor<List<Transcoding>> transcodingsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.doNothing().when(transcodingService).setTranscodingsForPlayer(Mockito.any(Player.class),
                    transcodingsCaptor.capture());

            playerService.getGuestPlayer(null);

            Player createdPlayer = playerCaptor.getValue();
            assertEquals(User.USERNAME_GUEST, createdPlayer.getUsername());
            assertEquals(TranscodeScheme.OFF, createdPlayer.getTranscodeScheme());
            verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.any(List.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        void testAnonymousPlayer() {
            Player player = new Player();
            player.setUsername(JWTAuthenticationToken.USERNAME_ANONYMOUS);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).createPlayer(playerCaptor.capture());
            ArgumentCaptor<List<Transcoding>> transcodingsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.doNothing().when(transcodingService).setTranscodingsForPlayer(Mockito.any(Player.class),
                    transcodingsCaptor.capture());

            playerService.createPlayer(player);

            Player createdPlayer = playerCaptor.getValue();
            assertEquals(JWTAuthenticationToken.USERNAME_ANONYMOUS, createdPlayer.getUsername());
            assertEquals(TranscodeScheme.OFF, createdPlayer.getTranscodeScheme());
            assertEquals(transcodingDao.getAllTranscodings(), transcodingsCaptor.getValue());
        }

        @Test
        void testCreatePlayerForExistingUser() {
            Player player = new Player();
            player.setId(0);
            player.setUsername("existingUser");

            UserSettings settings = new UserSettings();
            settings.setTranscodeScheme(TranscodeScheme.MAX_128);
            Mockito.when(userDao.getUserSettings(player.getUsername())).thenReturn(settings);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).createPlayer(playerCaptor.capture());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Transcoding>> transcodingsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.doNothing().when(transcodingService).setTranscodingsForPlayer(Mockito.any(Player.class),
                    transcodingsCaptor.capture());

            playerService.createPlayer(player);

            Player createdPlayer = playerCaptor.getValue();
            assertEquals(player.getUsername(), createdPlayer.getUsername());
            assertEquals(TranscodeScheme.MAX_128, createdPlayer.getTranscodeScheme());
            assertEquals(transcodingDao.getAllTranscodings(), transcodingsCaptor.getValue());
        }
    }

    @Test
    void testIsToBeUpdate() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Player player = new Player();
        assertNull(player.getLastSeen());
        assertTrue(playerService.isToBeUpdate(req, true, player));
        assertNotNull(player.getLastSeen());
    }
}
