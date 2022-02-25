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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.tesshu.jpsonic.domain.PlayerTechnology;
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
    private SettingsService settingsService;
    private TranscodingService transcodingService;
    private PlayerService playerService;

    @BeforeEach
    public void setup() throws ExecutionException {
        playerDao = mock(PlayerDao.class);
        userDao = mock(UserDao.class);
        transcodingDao = mock(TranscodingDao.class);
        settingsService = mock(SettingsService.class);
        transcodingService = mock(TranscodingService.class);
        List<Transcoding> transcodings = new ArrayList<>(transcodingDao.getAllTranscodings());
        Transcoding inactiveTranscoding = new Transcoding(10, "aac",
                "mp3 ogg oga m4a flac wav wma aif aiff ape mpc shn", "aac",
                "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -", null, null, false);
        transcodings.add(inactiveTranscoding);
        Mockito.when(transcodingService.getAllTranscodings()).thenReturn(transcodings);
        MusicFolderService musicFolderService = mock(MusicFolderService.class);
        playerService = new PlayerService(playerDao, null, settingsService,
                new SecurityService(userDao, null, musicFolderService, null), transcodingService);
    }

    @Test
    void testInit() {
        Player player1 = new Player();
        player1.setName("player1");
        player1.setTechnology(PlayerTechnology.WEB);
        Player player2 = new Player();
        player2.setName("player2");
        player2.setTechnology(PlayerTechnology.EXTERNAL);
        Player player3 = new Player();
        player3.setName("player3");
        player3.setTechnology(PlayerTechnology.EXTERNAL_WITH_PLAYLIST);

        Mockito.when(playerDao.getAllPlayers()).thenReturn(Arrays.asList(player1, player2, player3));
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        Mockito.doNothing().when(playerDao).updatePlayer(playerCaptor.capture());

        // Do nothing if UseExternalPlayer is enabled.
        Mockito.when(settingsService.isUseExternalPlayer()).thenReturn(true);
        playerService.init();
        Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));

        // Do reset if UseExternalPlayer is disbled.
        Mockito.when(settingsService.isUseExternalPlayer()).thenReturn(false);
        playerService.init();
        Mockito.verify(playerDao, Mockito.times(2)).updatePlayer(Mockito.any(Player.class));

        List<Player> results = playerCaptor.getAllValues();
        assertEquals("player2", results.get(0).getName());
        assertEquals(PlayerTechnology.WEB, results.get(0).getTechnology());
        assertTrue(results.get(0).isAutoControlEnabled());
        assertTrue(results.get(0).isM3uBomEnabled());
        assertEquals("player3", results.get(1).getName());
        assertEquals(PlayerTechnology.WEB, results.get(1).getTechnology());
        assertTrue(results.get(1).isAutoControlEnabled());
        assertTrue(results.get(1).isM3uBomEnabled());
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

        private String playerIp = "192.168.1.2";

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.False
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c01() {

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            Mockito.verify(playerDao, Mockito.times(1)).createPlayer(Mockito.any(Player.class));
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True.LastSeen.Today
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c02() {

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            Player dummy = playerService.getGuestPlayer(null);
            Player playerWithIp = new Player();
            playerWithIp.setIpAddress(playerIp);
            playerWithIp.setLastSeen(new Date());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(playerWithIp, dummy));

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            Mockito.verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True.LastSeen.Old
        @GetGuestPlayerLastSeenDecision.Conditions.Request.Null
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c03() {

            Player dummy = playerService.getGuestPlayer(null);
            Calendar old = Calendar.getInstance();
            old.setTime(dummy.getLastSeen());
            old.add(Calendar.DATE, -2);
            dummy.setLastSeen(old.getTime());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(dummy));

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(null);
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            Mockito.verify(playerDao, Mockito.times(1)).updatePlayer(Mockito.any(Player.class));
            Mockito.verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.False
        @GetGuestPlayerLastSeenDecision.Conditions.Request.WithIp
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c04() {

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());

            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Collections.emptyList());

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(playerIp);

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(req);
            assertEquals(playerIp, player.getIpAddress());
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            Mockito.verify(playerDao, Mockito.times(1)).createPlayer(Mockito.any(Player.class));
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
                    Mockito.anyList());
        }

        @GetGuestPlayerLastSeenDecision.Conditions.IsExists.True
        @GetGuestPlayerLastSeenDecision.Conditions.Request.WithIp
        @GetGuestPlayerLastSeenDecision.Results.LastSeen.Today
        @Test
        void c05() {

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());

            Player dummy = playerService.getGuestPlayer(null);
            Player playerWithIp = new Player();
            playerWithIp.setIpAddress(playerIp);
            playerWithIp.setLastSeen(new Date());
            Mockito.when(playerDao.getPlayersForUserAndClientId(Mockito.nullable(String.class),
                    Mockito.nullable(String.class))).thenReturn(Arrays.asList(dummy, playerWithIp));

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr(playerIp);

            Mockito.clearInvocations(playerDao);
            Player player = playerService.getGuestPlayer(req);
            assertEquals(playerIp, player.getIpAddress());
            assertNotNull(player.getLastSeen());
            Calendar lastSeen = Calendar.getInstance();
            lastSeen.setTime(player.getLastSeen());
            assertEquals(today.get(Calendar.YEAR), lastSeen.get(Calendar.YEAR));
            assertEquals(today.get(Calendar.MONTH), lastSeen.get(Calendar.MONTH));
            assertEquals(today.get(Calendar.DATE), lastSeen.get(Calendar.DATE));

            Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            Mockito.verify(playerDao, Mockito.never()).createPlayer(Mockito.any(Player.class));
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
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
            Mockito.verify(transcodingService, Mockito.never()).setTranscodingsForPlayer(Mockito.any(Player.class),
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
}
