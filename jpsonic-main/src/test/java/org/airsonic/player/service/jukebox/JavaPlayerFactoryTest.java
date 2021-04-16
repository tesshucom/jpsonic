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

package org.airsonic.player.service.jukebox;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import com.github.biconou.AudioPlayer.JavaPlayer;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;

public class JavaPlayerFactoryTest extends AbstractPlayerFactoryTest {

    @MockBean
    protected JavaPlayerFactory javaPlayerFactory;

    @BeforeEach
    @Override
    public void setup() throws ExecutionException {
        super.setup();
        JavaPlayer mockJavaPlayer = mock(JavaPlayer.class);
        when(mockJavaPlayer.getPlayingInfos()).thenReturn(() -> 0);
        when(mockJavaPlayer.getGain()).thenReturn(0.75f);
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(mockJavaPlayer);
    }

    @Override
    protected void createTestPlayer() {
        Player jukeboxPlayer = new Player();
        jukeboxPlayer.setName(JUKEBOX_PLAYER_NAME);
        jukeboxPlayer.setUsername("admin");
        jukeboxPlayer.setClientId(CLIENT_NAME + "-jukebox");
        jukeboxPlayer.setTechnology(PlayerTechnology.JAVA_JUKEBOX);
        playerService.createPlayer(jukeboxPlayer);
    }

}
