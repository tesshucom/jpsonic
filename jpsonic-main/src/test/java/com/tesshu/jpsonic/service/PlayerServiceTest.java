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
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.PlayerDao;
import com.tesshu.jpsonic.domain.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

public class PlayerServiceTest {

    private PlayerDao playerDao;
    private TranscodingService transcodingService;
    private PlayerService playerService;

    @BeforeEach
    public void setup() throws ExecutionException {
        playerDao = mock(PlayerDao.class);
        transcodingService = mock(TranscodingService.class);
        playerService = new PlayerService(playerDao, null, mock(SecurityService.class), transcodingService);
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
}
