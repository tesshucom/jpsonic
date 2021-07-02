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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.InternetRadioDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlayQueueDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.JukeboxService;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.ServletRequestBindingException;

class PlayQueueServiceTest extends AbstractNeedsScan {

    private static final String ADMIN_NAME = "admin";

    @Autowired
    private PlayerService playerService;
    @Autowired
    private JukeboxService jukeboxService;
    @Autowired
    private JpsonicComparators comparators;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private RatingService ratingService;
    @Mock
    private PodcastService podcastService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private PlayQueueDao playQueueDao;
    @Autowired
    private InternetRadioDao internetRadioDao;
    @Autowired
    private JWTSecurityService jwtSecurityService;
    @Autowired
    private InternetRadioService internetRadioService;
    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpServletRequest httpServletRequest;
    @Autowired
    private MockHttpServletResponse httpServletResponse;

    private PlayQueueService playQueueService;
    private List<MusicFolder> musicFolders;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath("Music"));
            musicFolders.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
        }
        return musicFolders;
    }

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getHttpServletRequest()).thenReturn(httpServletRequest);
        Mockito.when(ajaxHelper.getHttpServletResponse()).thenReturn(httpServletResponse);
        PodcastEpisode podcastEpisode = new PodcastEpisode(0, 0, null, null, null, null, null, null, null, null, null,
                null);
        Mockito.when(podcastService.getEpisode(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(podcastEpisode);
        Mockito.when(podcastService.getEpisodes(Mockito.anyInt())).thenReturn(Collections.emptyList());
        populateDatabaseOnlyOnce();
        playQueueService = new PlayQueueService(musicFolderService, securityService, playerService, jukeboxService,
                comparators, mediaFileService, lastFmService, searchService, ratingService, podcastService,
                playlistService, mediaFileDao, playQueueDao, internetRadioDao, jwtSecurityService, internetRadioService,
                ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlay() throws ServletRequestBindingException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, getMusicFolders());
        MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
        assertNotNull(playQueueService.play(song.getId()));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayPlaylist() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playPlaylist(0, 0));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayTopSong() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playTopSong(0, 0));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayPodcastEpisode() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playPodcastEpisode(0));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayNewestPodcastEpisode() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playNewestPodcastEpisode(0));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayStarred() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playStarred());
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlayShuffle() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playShuffle("newest", 0, 0, null, null));
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testPlaySimilar() throws ServletRequestBindingException {
        assertNotNull(playQueueService.playSimilar(0, 1));
    }
}
