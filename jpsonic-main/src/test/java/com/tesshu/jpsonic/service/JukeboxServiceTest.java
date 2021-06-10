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

package com.tesshu.jpsonic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JukeboxServiceTest {

    private JukeboxService jukeboxService;

    @Mock
    private JukeboxLegacySubsonicService jukeboxLegacySubsonicService;

    @Mock
    private JukeboxJavaService jukeboxJavaService;
    private Player jukeboxPlayer;
    private Player legacyJukeboxPlayer;
    private Player nonJukeboxPlayer;

    @BeforeEach
    public void setUp() {
        jukeboxService = new JukeboxService(jukeboxLegacySubsonicService, jukeboxJavaService);
        jukeboxPlayer = generateJukeboxPlayer();
        legacyJukeboxPlayer = generateLegacyJukeboxPlayer();
        nonJukeboxPlayer = generateNonJukeboxPlayer();
    }

    private Player generateNonJukeboxPlayer() {
        Player player = new Player();
        player.setId(0);
        player.setTechnology(PlayerTechnology.WEB);
        return player;
    }

    private Player generateLegacyJukeboxPlayer() {
        Player player = new Player();
        player.setId(1);
        player.setTechnology(PlayerTechnology.JUKEBOX);
        return player;
    }

    private Player generateJukeboxPlayer() {
        Player player = new Player();
        player.setId(2);
        player.setTechnology(PlayerTechnology.JAVA_JUKEBOX);
        return player;
    }

    @Test
    public void setPositionWithJukeboxPlayer() {
        // When
        jukeboxService.setPosition(jukeboxPlayer, 0);
        // Then
        verify(jukeboxJavaService).setPosition(jukeboxPlayer, 0);
    }

    @Test
    public void setPositionWithLegacyJukeboxPlayer() {
        // When
        assertThrows(UnsupportedOperationException.class, () -> jukeboxService.setPosition(legacyJukeboxPlayer, 0));
    }

    @Test
    public void testGetGainWithJukeboxPlayer() {
        // When
        jukeboxService.getGain(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).getGain(jukeboxPlayer);
    }

    @Test
    public void testGetGainWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getGain(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getGain();
    }

    @Test
    public void testGetGainWithNonJukeboxPlayer() {
        // When
        float gain = jukeboxService.getGain(nonJukeboxPlayer);
        // Then
        assertThat(gain).isEqualTo(0);
    }

    @Test
    public void testUpdateJukebox() {
        // When
        jukeboxService.updateJukebox(legacyJukeboxPlayer, 0);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void testGetPositionWithJukeboxPlayer() {
        // When
        jukeboxService.getPosition(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).getPosition(jukeboxPlayer);
    }

    @Test
    public void testGetPositionWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.getPosition(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).getPosition();
    }

    @Test
    public void testGetPasitionWithNonJukeboxPlayer() {
        // When
        int position = jukeboxService.getPosition(nonJukeboxPlayer);
        // Then
        assertThat(position).isEqualTo(0);
    }

    @Test
    public void testSetGainWithJukeboxPlayer() {
        // When
        jukeboxService.setGain(jukeboxPlayer, 0.5f);
        // Then
        verify(jukeboxJavaService).setGain(jukeboxPlayer, 0.5f);
    }

    @Test
    public void testSetGaintWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.setGain(legacyJukeboxPlayer, 0.5f);
        // Then
        verify(jukeboxLegacySubsonicService).setGain(0.5f);
    }

    @Test
    public void testStartWithJukeboxPlayer() {
        // When
        jukeboxService.start(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).start(jukeboxPlayer);
    }

    @Test
    public void testStartWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.start(legacyJukeboxPlayer);

        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void testPlayWithJukeboxPlayer() {
        // When
        jukeboxService.play(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).play(jukeboxPlayer);
    }

    @Test
    public void testPlayWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.play(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void testStopWithJukeboxPlayer() {
        // When
        jukeboxService.stop(jukeboxPlayer);
        // Then
        verify(jukeboxJavaService).stop(jukeboxPlayer);
    }

    @Test
    public void testStopWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.stop(legacyJukeboxPlayer);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 0);
    }

    @Test
    public void testSkipWithJukeboxPlayer() {
        // When
        jukeboxService.skip(jukeboxPlayer, 0, 1);
        // Then
        verify(jukeboxJavaService).skip(jukeboxPlayer, 0, 1);
    }

    @Test
    public void testSkipWithLegacyJukeboxPlayer() {
        // When
        jukeboxService.skip(legacyJukeboxPlayer, 0, 1);
        // Then
        verify(jukeboxLegacySubsonicService).updateJukebox(legacyJukeboxPlayer, 1);
    }

    @Test
    public void testCanControlWithJukeboxPlayer() {
        // When
        boolean canControl = jukeboxService.canControl(jukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(true);
    }

    @Test
    public void testCanControlWithLegacyJukeboxPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(legacyJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(true);
    }

    @Test
    public void testCanControlWithLegacyJukeboxPlayerWrongPlayer() {
        // When
        when(jukeboxLegacySubsonicService.getPlayer()).thenReturn(nonJukeboxPlayer);
        boolean canControl = jukeboxService.canControl(legacyJukeboxPlayer);
        // Then
        assertThat(canControl).isEqualTo(false);
    }
}
