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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PlayerTechnology;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JukeboxJavaServiceTest {

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

    @BeforeEach
    public void setup() {
        service = new JukeboxJavaService(audioScrobblerService, statusService, securityService, mediaFileService,
                javaPlayerFactory);
        lenient().when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.JAVA_JUKEBOX);
        lenient().when(airsonicPlayer.getUsername()).thenReturn(USER_NAME);
        lenient().when(javaPlayerFactory.createJavaPlayer()).thenReturn(player);
        lenient().when(securityService.getUserByName(USER_NAME)).thenReturn(user);
        lenient().when(user.isJukeboxRole()).thenReturn(true);
        lenient().when(airsonicPlayer.getPlayQueue()).thenReturn(playQueue);
        lenient().when(playQueue.getCurrentFile()).thenReturn(mediaFile);
    }

    @Test
    void testPlay() {
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player).play();
    }

    @Test
    void testPlayForNonDefaultMixer() {
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
    void testPlayAndStop() {
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
    void testPlayWithNonJukeboxUser() {
        // Given
        when(user.isJukeboxRole()).thenReturn(false);
        // CreateJavaPlayer should not be called if you do not have permission.
        // When
        service.play(airsonicPlayer);
        verify(player, Mockito.never()).play();
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testPlayWithNonJukeboxPlayer() {
        // Given
        when(airsonicPlayer.getTechnology()).thenReturn(PlayerTechnology.WEB);
        // When
        Assertions.assertThrows(RuntimeException.class, () -> service.play(airsonicPlayer));
    }

    @Test
    void testPlayWithNoPlayQueueEmpty() {
        // Given
        when(playQueue.getCurrentFile()).thenReturn(null);
        // When
        service.play(airsonicPlayer);
        // Then
        verify(javaPlayerFactory).createJavaPlayer();
        verify(player, Mockito.never()).play();
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    void testPlayerInitProblem() {
        // Given
        when(javaPlayerFactory.createJavaPlayer()).thenReturn(null);
        // When
        Assertions.assertThrows(RuntimeException.class, () -> service.play(airsonicPlayer));
    }
}
