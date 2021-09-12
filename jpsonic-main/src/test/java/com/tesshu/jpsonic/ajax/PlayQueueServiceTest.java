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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import com.tesshu.jpsonic.dao.InternetRadioDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlayQueueDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.ServletRequestBindingException;

class PlayQueueServiceTest {

    private MediaFileService mediaFileService;
    private PlayQueueService playQueueService;

    @BeforeEach
    public void setup() {

        final PlayerService playerService = Mockito.mock(PlayerService.class);
        Player player = new Player();
        player.setId(100);
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        player.setPlayQueue(new PlayQueue());
        Mockito.when(playerService.getPlayerById(player.getId())).thenReturn(player);
        Mockito.when(playerService.getPlayerById(0)).thenReturn(player);
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(player);

        PodcastService podcastService = mock(PodcastService.class);
        PodcastEpisode podcastEpisode = new PodcastEpisode(0, 0, null, null, null, null, null, null, null, null, null,
                null);
        Mockito.when(podcastService.getEpisode(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(podcastEpisode);
        Mockito.when(podcastService.getEpisodes(Mockito.anyInt())).thenReturn(Collections.emptyList());

        mediaFileService = mock(MediaFileService.class);

        playQueueService = new PlayQueueService(mock(MusicFolderService.class), mock(SecurityService.class),
                playerService, mock(JpsonicComparators.class), mediaFileService, mock(LastFmService.class),
                mock(SearchService.class), mock(RatingService.class), podcastService, mock(PlaylistService.class),
                mock(MediaFileDao.class), mock(PlayQueueDao.class), mock(InternetRadioDao.class),
                mock(JWTSecurityService.class), mock(InternetRadioService.class), AjaxMockUtils.mock(AjaxHelper.class));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlay() throws ServletRequestBindingException {
        MediaFile song = new MediaFile();
        song.setId(0);
        Mockito.when(mediaFileService.getMediaFile(song.getId())).thenReturn(song);
        assertNotNull(playQueueService.play(song.getId()));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayPlaylist() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playPlaylist(0, 0));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayTopSong() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playTopSong(0, 0));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayPodcastEpisode() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playPodcastEpisode(0));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayNewestPodcastEpisode() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playNewestPodcastEpisode(0));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayStarred() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playStarred());
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlayShuffle() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playShuffle("newest", 0, 0, null, null));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testPlaySimilar() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playSimilar(0, 1));
    }
}
