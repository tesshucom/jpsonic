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

package org.airsonic.player.api.jukebox;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.LineUnavailableException;

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.service.jukebox.AudioPlayer;
import org.airsonic.player.service.jukebox.AudioPlayerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;

public class AirsonicRestApiJukeboxLegacyIntTest extends AirsonicRestApiJukeboxIntTest {

    @SpyBean
    private TranscodingService transcodingService;
    @MockBean
    protected AudioPlayerFactory audioPlayerFactory;

    private AudioPlayer mockAudioPlayer;

    @Before
    @Override
    public void setup() throws ExecutionException {
        super.setup();
        mockAudioPlayer = mock(AudioPlayer.class);
        try {
            when(audioPlayerFactory.createAudioPlayer(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenReturn(mockAudioPlayer);
            doReturn(null).when(transcodingService).getTranscodedInputStream(ArgumentMatchers.any());
        } catch (LineUnavailableException | IOException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    protected final void createTestPlayer() {
        Player jukeBoxPlayer = new Player();
        jukeBoxPlayer.setName(JUKEBOX_PLAYER_NAME);
        jukeBoxPlayer.setUsername("admin");
        jukeBoxPlayer.setClientId(CLIENT_NAME + "-jukebox");
        jukeBoxPlayer.setTechnology(PlayerTechnology.JUKEBOX);
        playerService.createPlayer(jukeBoxPlayer);
    }

    @Test
    @WithMockUser(username = "admin")
    @Override
    public void jukeboxStartActionTest() throws ExecutionException {
        super.jukeboxStartActionTest();
        verify(mockAudioPlayer).play();
    }

    @Test
    @WithMockUser(username = "admin")
    @Override
    public void jukeboxStopActionTest() throws ExecutionException {
        super.jukeboxStopActionTest();
        verify(mockAudioPlayer).play();
        verify(mockAudioPlayer).pause();
    }

}
