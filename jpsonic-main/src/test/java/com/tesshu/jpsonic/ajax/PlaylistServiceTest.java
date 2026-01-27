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

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.regex.Pattern;

import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.persistence.api.entity.PlayQueue;
import com.tesshu.jpsonic.persistence.api.entity.Player;
import com.tesshu.jpsonic.persistence.api.entity.Playlist;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.ServletRequestBindingException;

@SuppressWarnings("PMD.TooManyStaticImports")
class PlaylistServiceTest {

    private PlaylistService playlistService;
    private com.tesshu.jpsonic.service.PlaylistService deligate;
    private PlayerService playerService;

    @BeforeEach
    public void setup() {
        deligate = mock(com.tesshu.jpsonic.service.PlaylistService.class);
        playerService = mock(PlayerService.class);
        playlistService = new PlaylistService(mock(MusicFolderService.class),
                mock(SecurityService.class), mock(MediaFileService.class), deligate,
                mock(MediaFileDao.class), playerService, mock(AirsonicLocaleResolver.class),
                AjaxMockUtils.mock(AjaxHelper.class));
    }

    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Test
    void testGetPlaylist() {
        int id = 99;
        Playlist playlist = new Playlist();
        playlist.setCreated(now());
        playlist.setChanged(now());
        Mockito.when(deligate.getPlaylist(id)).thenReturn(playlist);

        PlaylistInfo playlistInfo = playlistService.getPlaylist(id);
        assertNull(playlistInfo.getPlaylist().getCreated());
        assertNull(playlistInfo.getPlaylist().getChanged());
    }

    @Test
    void testCreateEmptyPlaylist() {
        ArgumentCaptor<Playlist> captor = ArgumentCaptor.forClass(Playlist.class);
        Mockito.doNothing().when(deligate).createPlaylist(captor.capture());

        playlistService.createEmptyPlaylist();
        // yyyy-MM-dd HH:mm
        assertTrue(Pattern
            .compile("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}$")
            .matcher(captor.getValue().getName())
            .matches());
    }

    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Test
    void testCreatePlaylistForPlayQueue() throws ServletRequestBindingException {
        ArgumentCaptor<Playlist> captor = ArgumentCaptor.forClass(Playlist.class);
        Mockito.doNothing().when(deligate).createPlaylist(captor.capture());
        Player player = new Player();
        player.setPlayQueue(new PlayQueue());
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(player);

        playlistService.createPlaylistForPlayQueue();
        // yyyy-MM-dd HH:mm
        assertTrue(Pattern
            .compile("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}$")
            .matcher(captor.getValue().getName())
            .matches());
    }

    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    @Test
    void testCreatePlaylistForStarredSongs() {
        assertNotEquals(-1, playlistService.createPlaylistForStarredSongs());
    }
}
