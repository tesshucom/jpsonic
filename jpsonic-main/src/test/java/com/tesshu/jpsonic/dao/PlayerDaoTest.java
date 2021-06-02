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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link PlayerDao}.
 *
 * @author Sindre Mehus 
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class PlayerDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private PlayerDao playerDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from player");
    }

    @Test
    void testCreatePlayer() {
        Player player = new Player();
        player.setName("name");
        player.setType("type");
        player.setUsername("username");
        player.setIpAddress("ipaddress");
        player.setDynamicIp(false);
        player.setAutoControlEnabled(false);
        player.setTechnology(PlayerTechnology.EXTERNAL_WITH_PLAYLIST);
        player.setClientId("android");
        player.setLastSeen(new Date());
        player.setTranscodeScheme(TranscodeScheme.MAX_160);

        playerDao.createPlayer(player);
        Player newPlayer = playerDao.getAllPlayers().get(0);
        assertPlayerEquals(player, newPlayer);

        Player newPlayer2 = playerDao.getPlayerById(newPlayer.getId());
        assertPlayerEquals(player, newPlayer2);
    }

    @Test
    void testDefaultValues() {
        playerDao.createPlayer(new Player());
        Player player = playerDao.getAllPlayers().get(0);

        assertTrue(player.isDynamicIp(), "Player should have dynamic IP by default.");
        assertTrue(player.isAutoControlEnabled(), "Player should be auto-controlled by default.");
        assertNull(player.getClientId(), "Player client ID should be null by default.");
    }

    @Test
    void testIdentity() {
        Player player = new Player();

        playerDao.createPlayer(player);
        assertEquals((Integer) 1, player.getId(), "Wrong ID");
        assertEquals(1, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.createPlayer(player);
        assertEquals((Integer) 2, player.getId(), "Wrong ID");
        assertEquals(2, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.createPlayer(player);
        assertEquals((Integer) 3, player.getId(), "Wrong ID");
        assertEquals(3, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.deletePlayer(3);
        playerDao.createPlayer(player);
        assertEquals((Integer) 3, player.getId(), "Wrong ID");
        assertEquals(3, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.deletePlayer(2);
        playerDao.createPlayer(player);
        assertEquals((Integer) 4, player.getId(), "Wrong ID");
        assertEquals(3, playerDao.getAllPlayers().size(), "Wrong number of players.");
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testPlaylist() {
        Player player = new Player();
        playerDao.createPlayer(player);
        PlayQueue playQueue = player.getPlayQueue();
        Assertions.assertNotNull(playQueue, "Missing playlist.");

        playerDao.deletePlayer(player.getId());
        playerDao.createPlayer(player);
        assertFalse(EqualsBuilder.reflectionEquals(player, player.getPlayQueue(), false));
    }

    @Test
    void testGetPlayersForUserAndClientId() {
        Player player = new Player();
        player.setUsername("sindre");
        playerDao.createPlayer(player);
        player = playerDao.getAllPlayers().get(0);

        List<Player> players = playerDao.getPlayersForUserAndClientId("sindre", null);
        assertFalse(players.isEmpty(), "Error in getPlayersForUserAndClientId().");
        assertPlayerEquals(player, players.get(0));
        assertTrue(playerDao.getPlayersForUserAndClientId("sindre", "foo").isEmpty(),
                "Error in getPlayersForUserAndClientId().");

        player.setClientId("foo");
        playerDao.updatePlayer(player);

        players = playerDao.getPlayersForUserAndClientId("sindre", null);
        assertTrue(players.isEmpty(), "Error in getPlayersForUserAndClientId().");
        players = playerDao.getPlayersForUserAndClientId("sindre", "foo");
        assertFalse(players.isEmpty(), "Error in getPlayersForUserAndClientId().");
        assertPlayerEquals(player, players.get(0));
    }

    @Test
    void testUpdatePlayer() {
        Player player = new Player();
        playerDao.createPlayer(player);
        assertPlayerEquals(player, playerDao.getAllPlayers().get(0));

        player.setName("name");
        player.setType("Winamp");
        player.setTechnology(PlayerTechnology.WEB);
        player.setClientId("foo");
        player.setUsername("username");
        player.setIpAddress("ipaddress");
        player.setDynamicIp(true);
        player.setAutoControlEnabled(false);
        player.setLastSeen(new Date());
        player.setTranscodeScheme(TranscodeScheme.MAX_160);

        playerDao.updatePlayer(player);
        Player newPlayer = playerDao.getAllPlayers().get(0);
        assertPlayerEquals(player, newPlayer);
    }

    @Test
    void testDeletePlayer() {
        assertEquals(0, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.createPlayer(new Player());
        assertEquals(1, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.createPlayer(new Player());
        assertEquals(2, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.deletePlayer(1);
        assertEquals(1, playerDao.getAllPlayers().size(), "Wrong number of players.");

        playerDao.deletePlayer(2);
        assertEquals(0, playerDao.getAllPlayers().size(), "Wrong number of players.");
    }

    private void assertPlayerEquals(Player expected, Player actual) {
        assertEquals(expected.getId(), actual.getId(), "Wrong ID.");
        assertEquals(expected.getName(), actual.getName(), "Wrong name.");
        assertEquals(expected.getTechnology(), actual.getTechnology(), "Wrong technology.");
        assertEquals(expected.getClientId(), actual.getClientId(), "Wrong client ID.");
        assertEquals(expected.getType(), actual.getType(), "Wrong type.");
        assertEquals(expected.getUsername(), actual.getUsername(), "Wrong username.");
        assertEquals(expected.getIpAddress(), actual.getIpAddress(), "Wrong IP address.");
        assertEquals(expected.isDynamicIp(), actual.isDynamicIp(), "Wrong dynamic IP.");
        assertEquals(expected.isAutoControlEnabled(), actual.isAutoControlEnabled(), "Wrong auto control enabled.");
        assertEquals(expected.getLastSeen(), actual.getLastSeen(), "Wrong last seen.");
        assertEquals(expected.getTranscodeScheme(), actual.getTranscodeScheme(), "Wrong transcode scheme.");
    }
}
