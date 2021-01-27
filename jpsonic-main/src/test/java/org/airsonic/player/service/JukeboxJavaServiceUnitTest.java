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

package org.airsonic.player.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JukeboxJavaServiceUnitTest {

    private static final String USER_NAME = "admin";

    private JukeboxJavaService service;
    @Mock
    private Player airsonicPlayer;
    @Mock
    private AudioScrobblerService audioScrobblerService;
    @Mock
    private StatusService statusService;
    @Mock
    private SecurityService securityService;
    @Mock
    private MediaFileService mediaFileService;
    @Mock
    private JavaPlayerFactory javaPlayerFactory;
    @Mock
    private com.github.biconou.AudioPlayer.api.Player player;
    @Mock
    private User user;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private MediaFile mediaFile;

    @Before
    public void setup() {
        service = new JukeboxJavaService(audioScrobblerService, statusService, securityService, mediaFileService,
                javaPlayerFactory);
        when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.JAVA_JUKEBOX);
        when(airsonicPlayer.getUsername()).thenReturn(USER_NAME);
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(player);
        when(securityService.getUserByName(USER_NAME)).thenReturn(user);
        when(user.isJukeboxRole()).thenReturn(true);
        when(airsonicPlayer.getPlayQueue()).thenReturn(playQueue);
        when(playQueue.getCurrentFile()).thenReturn(mediaFile);
    }

    @Test
    public void play() {
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player).play();
    }

    @Test
    public void playForNonDefaultMixer() {
        // Given
        when(airsonicPlayer.getJavaJukeboxMixer()).thenReturn("mixer");
        when(javaPlayerFactory.createJavaPlayer("mixer")).thenReturn(player);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer("mixer");
        verify(player).play();
    }

    @Test
    public void playAndStop() {
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player).play();
        // When
        service.stop(airsonicPlayer);
        // Then
        verifyNoMoreInteractions(javaPlayerFactory);
        verify(player).pause();

    }

    @Test
    public void playWithNonJukeboxUser() {
        // Given
        when(user.isJukeboxRole()).thenReturn(false);
        // CreateJavaPlayer should not be called if you do not have permission.
        // When
        service.play(airsonicPlayer);
        verify(player, never()).play();
    }

    @Test(expected = RuntimeException.class)
    public void playWithNonJukeboxPlayer() {
        // Given
        when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.WEB);
        // When
        service.play(airsonicPlayer);
    }

    @Test
    public void playWithNoPlayQueueEmpty() {
        // Given
        when(playQueue.getCurrentFile()).thenReturn(null);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player, never()).play();
    }

    @Test(expected = RuntimeException.class)
    public void playerInitProblem() {
        // Given
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(null);
        // When
        service.play(airsonicPlayer);
    }
}
