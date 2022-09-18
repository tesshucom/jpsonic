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

import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.tesshu.jpsonic.dao.InternetRadioDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlayQueueDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.PlayQueue;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.SavedPlayQueue;
import com.tesshu.jpsonic.domain.UserSettings;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.ServletRequestBindingException;

class PlayQueueServiceTest {

    private MediaFileService mediaFileService;
    private PlayQueueService playQueueService;
    private SecurityService securityService;
    private PlayQueueDao playQueueDao;

    @BeforeEach
    public void setup() {

        final PlayerService playerService = Mockito.mock(PlayerService.class);
        Player player = new Player();
        player.setId(100);
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        player.setPlayQueue(new PlayQueue());

        PlayQueue playQueue = new PlayQueue();

        playQueue.addFiles(true, Arrays.asList(new MediaFile(), new MediaFile()));
        player.setPlayQueue(playQueue);

        Mockito.when(playerService.getPlayerById(player.getId())).thenReturn(player);
        Mockito.when(playerService.getPlayerById(0)).thenReturn(player);
        Mockito.when(playerService.getPlayer(Mockito.any(), Mockito.any())).thenReturn(player);

        PodcastService podcastService = mock(PodcastService.class);
        PodcastEpisode podcastEpisode = new PodcastEpisode(0, 0, null, null, null, null, null, null, null, null, null,
                null);
        Mockito.when(podcastService.getEpisodeStrict(Mockito.anyInt(), Mockito.anyBoolean()))
                .thenReturn(podcastEpisode);
        Mockito.when(podcastService.getEpisodes(Mockito.anyInt())).thenReturn(Collections.emptyList());

        mediaFileService = mock(MediaFileService.class);
        securityService = mock(SecurityService.class);
        playQueueDao = mock(PlayQueueDao.class);
        playQueueService = new PlayQueueService(mock(MusicFolderService.class), securityService, playerService,
                mock(JpsonicComparators.class), mediaFileService, mock(LastFmService.class), mock(SearchService.class),
                mock(RatingService.class), podcastService, mock(PlaylistService.class), mock(MediaFileDao.class),
                playQueueDao, mock(InternetRadioDao.class), mock(JWTSecurityService.class),
                mock(InternetRadioService.class), AjaxMockUtils.mock(AjaxHelper.class));
    }

    @Documented
    private @interface PlayDecision {
        @interface MediaFile {
            @interface IsFile {
                @interface True {
                }

                @interface False {
                }

            }

            @interface Parent {
                @interface Null {
                }

                @interface NonNull {
                }
            }
        }

        @interface UserSettings {
            @interface IsQueueFollowingSongs {
                @interface False {
                }

                @interface True {
                }
            }
        }
    }

    @Nested
    class PlayTest {

        @Test
        @PlayDecision.MediaFile.IsFile.True
        @PlayDecision.UserSettings.IsQueueFollowingSongs.False
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c01() throws ServletRequestBindingException {
            MediaFile song = new MediaFile();
            song.setId(0);
            Mockito.when(mediaFileService.getMediaFileStrict(song.getId())).thenReturn(song);
            assertNotNull(playQueueService.play(song.getId()));
        }

        @Test
        @PlayDecision.MediaFile.IsFile.True
        @PlayDecision.MediaFile.Parent.Null
        @PlayDecision.UserSettings.IsQueueFollowingSongs.True
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c02() throws ServletRequestBindingException {
            UserSettings mockedSetting = securityService.getUserSettings(ServiceMockUtils.ADMIN_NAME);
            mockedSetting.setQueueFollowingSongs(true);
            MediaFile song = new MediaFile();
            song.setId(0);
            Mockito.when(mediaFileService.getMediaFileStrict(song.getId())).thenReturn(song);
            assertNotNull(playQueueService.play(song.getId()));
        }

        @Test
        @PlayDecision.MediaFile.IsFile.True
        @PlayDecision.MediaFile.Parent.NonNull
        @PlayDecision.UserSettings.IsQueueFollowingSongs.True
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c03() throws ServletRequestBindingException {
            UserSettings mockedSetting = securityService.getUserSettings(ServiceMockUtils.ADMIN_NAME);
            mockedSetting.setQueueFollowingSongs(true);
            MediaFile parent = new MediaFile();
            parent.setId(0);
            MediaFile song1 = new MediaFile();
            song1.setId(1);
            song1.setPathString("song1");
            Mockito.when(mediaFileService.getParent(song1)).thenReturn(Optional.of(parent));
            MediaFile song2 = new MediaFile();
            song2.setId(2);
            song2.setPathString("song2");
            List<MediaFile> children = List.of(song1, song2);
            Mockito.when(mediaFileService.getChildrenOf(parent, true, false, true)).thenReturn(children);
            Mockito.when(mediaFileService.getMediaFileStrict(song1.getId())).thenReturn(song1);
            assertNotNull(playQueueService.play(song1.getId()));
        }

        @Test
        @PlayDecision.MediaFile.IsFile.False
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void c04() throws ServletRequestBindingException {
            MediaFile dir = new MediaFile();
            dir.setId(0);
            dir.setMediaType(MediaType.DIRECTORY);
            Mockito.when(mediaFileService.getMediaFileStrict(dir.getId())).thenReturn(dir);
            assertNotNull(playQueueService.play(dir.getId()));
        }
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testSavePlayQueue() throws ServletRequestBindingException {
        playQueueService.savePlayQueue(1, 1);
        Mockito.verify(playQueueDao, Mockito.times(1)).savePlayQueue(Mockito.any(SavedPlayQueue.class));
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
