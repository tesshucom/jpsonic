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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXB;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.ajax.LyricsService;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlayQueueDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.Share;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.domain.logic.CoverArtLogic;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.AudioScrobblerService;
import com.tesshu.jpsonic.service.BookmarkService;
import com.tesshu.jpsonic.service.InternetRadioService;
import com.tesshu.jpsonic.service.LastFmService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.RatingService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.ShareService;
import com.tesshu.jpsonic.service.StatusService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.scanner.WritableMediaFileService;
import com.tesshu.jpsonic.service.search.SearchCriteriaDirector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.ServletRequestBindingException;
import org.subsonic.restapi.NowPlaying;
import org.subsonic.restapi.Response;
import org.subsonic.restapi.ResponseStatus;

@AutoConfigureMockMvc
@SuppressWarnings({ "PMD.AvoidCatchingGenericException", // springframework/MockMvc#perform
        "PMD.JUnitTestsShouldIncludeAssert", "PMD.DetachedTestCase", "PMD.TooManyStaticImports" })
class SubsonicRESTControllerTest {

    private static final String CLIENT_NAME = "jpsonic";
    private static final String ADMIN_PASS = "admin";
    private static final String EXPECTED_FORMAT = "json";
    private static final String JSON_PATH_ERROR_CODE = "$.subsonic-response.error.code";
    private static final String JSON_PATH_ERROR_MESSAGE = "$.subsonic-response.error.message";
    private static final String JSON_PATH_STATUS = "$.subsonic-response.status";
    private static final String JSON_PATH_VERSION = "$.subsonic-response.version";
    private static final String JSON_VALUE_OK = "ok";
    private static final String JSON_VALUE_FAILED = "failed";

    private final String apiVerion = TestCaseUtils.restApiVersion();

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    @Nested
    class UnitTest {

        private SecurityService securityService;
        private TopController topController;
        private SubsonicRESTController controller;
        private PodcastService podcastService;
        private MediaScannerService mediaScannerService;

        @BeforeEach
        public void setup() {
            final SettingsService settingsService = mock(SettingsService.class);
            final MusicFolderService musicFolderService = mock(MusicFolderService.class);
            securityService = mock(SecurityService.class);
            final PlayerService playerService = mock(PlayerService.class);
            final MediaFileService mediaFileService = mock(MediaFileService.class);
            final WritableMediaFileService writableMediaFileService = mock(WritableMediaFileService.class);
            final LastFmService lastFmService = mock(LastFmService.class);
            final MusicIndexService musicIndexService = mock(MusicIndexService.class);
            final TranscodingService transcodingService = mock(TranscodingService.class);
            final DownloadController downloadController = mock(DownloadController.class);
            final CoverArtController coverArtController = mock(CoverArtController.class);
            final AvatarController avatarController = mock(AvatarController.class);
            final UserSettingsController userSettingsController = mock(UserSettingsController.class);
            topController = mock(TopController.class);
            final StatusService statusService = mock(StatusService.class);
            final StreamController streamController = mock(StreamController.class);
            final HLSController hlsController = mock(HLSController.class);
            final ShareService shareService = mock(ShareService.class);
            final PlaylistService playlistService = mock(PlaylistService.class);
            final LyricsService lyricsService = mock(LyricsService.class);
            final AudioScrobblerService audioScrobblerService = mock(AudioScrobblerService.class);
            podcastService = mock(PodcastService.class);
            final RatingService ratingService = mock(RatingService.class);
            final SearchService searchService = mock(SearchService.class);
            final InternetRadioService internetRadioService = mock(InternetRadioService.class);
            final MediaFileDao mediaFileDao = mock(MediaFileDao.class);
            final ArtistDao artistDao = mock(ArtistDao.class);
            final AlbumDao albumDao = mock(AlbumDao.class);
            final BookmarkService bookmarkService = mock(BookmarkService.class);
            final PlayQueueDao playQueueDao = mock(PlayQueueDao.class);
            mediaScannerService = mock(MediaScannerService.class);
            final AirsonicLocaleResolver airsonicLocaleResolver = mock(AirsonicLocaleResolver.class);
            final CoverArtLogic logic = mock(CoverArtLogic.class);
            final SearchCriteriaDirector director = mock(SearchCriteriaDirector.class);
            controller = new SubsonicRESTController(settingsService, musicFolderService, securityService, playerService,
                    mediaFileService, writableMediaFileService, lastFmService, musicIndexService, transcodingService,
                    downloadController, coverArtController, avatarController, userSettingsController, topController,
                    statusService, streamController, hlsController, shareService, playlistService, lyricsService,
                    audioScrobblerService, podcastService, ratingService, searchService, internetRadioService,
                    mediaFileDao, artistDao, albumDao, bookmarkService, playQueueDao, mediaScannerService,
                    airsonicLocaleResolver, logic, director);
        }

        @Test
        void testGetIndexes() throws ServletRequestBindingException {
            controller.getIndexes(new MockHttpServletRequest(), new MockHttpServletResponse());
            Mockito.verify(topController, Mockito.times(1)).getLastModified(Mockito.any(HttpServletRequest.class));
        }

        @Test
        void testRefreshPodcasts() {
            User user = new User("name", "pass", "mail");
            user.setPodcastRole(true);
            Mockito.when(securityService.getCurrentUserStrict(Mockito.any(HttpServletRequest.class))).thenReturn(user);

            controller.refreshPodcasts(new MockHttpServletRequest(), new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.times(1)).refreshAllChannels(Mockito.anyBoolean());
            Mockito.clearInvocations(podcastService);

            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            controller.refreshPodcasts(new MockHttpServletRequest(), new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.never()).refreshAllChannels(Mockito.anyBoolean());
        }

        @Test
        void testCreatePodcastChannel() throws ServletRequestBindingException {
            User user = new User("name", "pass", "mail");
            user.setPodcastRole(true);
            Mockito.when(securityService.getCurrentUserStrict(Mockito.any(HttpServletRequest.class))).thenReturn(user);
            HttpServletRequest req = mock(HttpServletRequest.class);
            String podcastUrl = "http://boo.foo";
            Mockito.when(req.getParameter(Attributes.Request.URL.value())).thenReturn(podcastUrl);

            controller.createPodcastChannel(req, new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.times(1)).createChannel(podcastUrl);
            Mockito.clearInvocations(podcastService);

            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            controller.createPodcastChannel(req, new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.never()).createChannel(podcastUrl);
        }

        @Test
        void testDownloadPodcastEpisode() throws ServletRequestBindingException {
            User user = new User("name", "pass", "mail");
            user.setPodcastRole(true);
            Mockito.when(securityService.getCurrentUserStrict(Mockito.any(HttpServletRequest.class))).thenReturn(user);
            HttpServletRequest req = mock(HttpServletRequest.class);
            int episodeId = 99;
            Mockito.when(req.getParameter(Attributes.Request.ID.value())).thenReturn(Integer.toString(episodeId));
            PodcastEpisode episode = new PodcastEpisode(null, null, null, null, null, null, null, null, null, null,
                    null, null);
            Mockito.when(podcastService.getEpisode(episodeId, true)).thenReturn(episode);

            controller.downloadPodcastEpisode(req, new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.times(1)).downloadEpisode(episode);
            Mockito.clearInvocations(podcastService);

            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            controller.downloadPodcastEpisode(req, new MockHttpServletResponse());
            Mockito.verify(podcastService, Mockito.never()).downloadEpisode(episode);
        }
    }

    @Nested
    class IntegreationTest extends AbstractNeedsScan {

        private final List<MusicFolder> musicFolders = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now()));

        @Autowired
        private MockMvc mvc;

        @Autowired
        private SubsonicRESTController subsonicRESTController;
        @Autowired
        private PlayerService playerService;
        @Autowired
        private StreamController streamController;
        @Autowired
        private StatusService statusService;
        @Autowired
        private SecurityService securityService;
        @Autowired
        private PlaylistService playlistService;
        @Autowired
        private ShareService shareService;
        @Autowired
        private MediaFileDao mediaFileDao;
        @Autowired
        private ArtistDao artistDao;
        @Autowired
        private AlbumDao albumDao;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return musicFolders;
        }

        @BeforeEach
        public void setup() {
            settingsService.setDlnaGuestPublish(false);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetLicense() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getLicense")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getLicense.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testPing() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/ping").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion)).andExpect(
                                MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

                mvc.perform(MockMvcRequestBuilders.get("/rest/ping.view").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion)).andExpect(
                                MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

                mvc.perform(MockMvcRequestBuilders.get("/rest/ping").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), "Bad password")
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion)).andExpect(
                                MockMvcResultMatchers.header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetMusicFolders() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getMusicFolders")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getMusicFolders.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetIndexes() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getIndexes")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getIndexes.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetGenres() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getGenres").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getGenres.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetSongsByGenre() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSongsByGenre")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.GENRE.value(), "genre")
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(), musicFolders.get(0).getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSongsByGenre.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.GENRE.value(), "genre")
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(), musicFolders.get(0).getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetArtists() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtists")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtists.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetSimilarSongs() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetSimilarSongs2() throws ExecutionException {
            Artist artist = artistDao.getAlphabetialArtists(0, 1, musicFolders).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs2")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetTopSongs() throws ExecutionException {
            Artist artist = artistDao.getAlphabetialArtists(0, 1, musicFolders).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getTopSongs")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ARTIST.value(), artist.getName())
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getTopSongs.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ARTIST.value(), artist.getName())
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetArtistInfo() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetArtistInfo2() throws ExecutionException {
            Artist artist = artistDao.getAlphabetialArtists(0, 1, musicFolders).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo2")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetArtist() throws ExecutionException {
            Artist artist = artistDao.getAlphabetialArtists(0, 1, musicFolders).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtist").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getArtist.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAlbum() throws ExecutionException {
            Album album = albumDao.getAlphabeticalAlbums(0, 1, false, false, musicFolders).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbum").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbum.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetSong() throws ExecutionException {
            try {

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                mvc.perform(MockMvcRequestBuilders.get("/rest/getSong").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getSong.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void testGetMusicDirectory() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getMusicDirectory")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(musicFolders.get(0).getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getMusicDirectory.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(musicFolders.get(0).getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testSearch() throws ExecutionException {
            try {
                mvc.perform(MockMvcRequestBuilders.get("/rest/search").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/search.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testSearch2() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/search2").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/search2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testSearch3() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/search3").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/search3.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetPlaylists() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlaylists")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlaylists.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testJukeboxControl() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/jukeboxControl")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

                mvc.perform(MockMvcRequestBuilders.get("/rest/jukeboxControl.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetPlaylist() throws ExecutionException {
            try {

                Playlist playlist = new Playlist(0, ServiceMockUtils.ADMIN_NAME, false, "name", "comment", 0, 0, now(),
                        now(), null);
                playlistService.createPlaylist(playlist);

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlaylist")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(playlist.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlaylist.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(playlist.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                playlistService.deletePlaylist(playlist.getId());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreatePlaylist() throws ExecutionException {
            try {

                String playlistName = "CreatePlaylistTest";

                mvc.perform(MockMvcRequestBuilders.get("/rest/createPlaylist")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.NAME.value(), playlistName))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                playlistService.getAllPlaylists().stream().filter(p -> playlistName.equals(p.getName()))
                        .forEach(p -> playlistService.deletePlaylist(p.getId()));

                mvc.perform(MockMvcRequestBuilders.get("/rest/createPlaylist.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.NAME.value(), playlistName))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                playlistService.getAllPlaylists().stream().filter(p -> playlistName.equals(p.getName()))
                        .forEach(p -> playlistService.deletePlaylist(p.getId()));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testUpdatePlaylist() throws ExecutionException {
            try {

                String playlistName = "UpdatePlaylist";
                Playlist playlist = new Playlist(0, ServiceMockUtils.ADMIN_NAME, false, playlistName, "comment", 0, 0,
                        now(), now(), null);
                playlistService.createPlaylist(playlist);
                playlist = playlistService.getAllPlaylists().stream().filter(p -> playlistName.equals(p.getName()))
                        .findFirst().get();

                mvc.perform(MockMvcRequestBuilders.get("/rest/updatePlaylist")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.PLAYLIST_ID.value(), Integer.toString(playlist.getId()))
                        .param(Attributes.Request.NAME.value(), playlistName))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/updatePlaylist.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.PLAYLIST_ID.value(), Integer.toString(playlist.getId()))
                        .param(Attributes.Request.NAME.value(), playlistName))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                playlistService.getAllPlaylists().stream().filter(p -> playlistName.equals(p.getName()))
                        .forEach(p -> playlistService.deletePlaylist(p.getId()));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testDeletePlaylist() throws ExecutionException {
            try {

                String playlistName = "DeletePlaylist";
                Playlist playlist = new Playlist(0, ServiceMockUtils.ADMIN_NAME, false, playlistName, "comment", 0, 0,
                        now(), now(), null);
                playlistService.createPlaylist(playlist);
                playlist = playlistService.getAllPlaylists().stream().filter(p -> playlistName.equals(p.getName()))
                        .findFirst().get();

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePlaylist")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(playlist.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePlaylist.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(playlist.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Playlist not found: " + playlist.getId()))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAlbumList() throws ExecutionException {
            try {
                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(),
                                Integer.toString(musicFolders.get(0).getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(),
                                Integer.toString(musicFolders.get(0).getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAlbumList2() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList2")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(),
                                Integer.toString(musicFolders.get(0).getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                        .param(Attributes.Request.MUSIC_FOLDER_ID.value(),
                                Integer.toString(musicFolders.get(0).getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetRandomSongs() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getRandomSongs")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getRandomSongs.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetVideos() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getVideos").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getVideos.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetNowPlaying() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getNowPlaying")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getNowPlaying.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        @Nested
        class GetNowPlaying {

            /*
             * @see #1048
             */
            @Test
            @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
            @Order(1)
            void testGetNowPlayingWithoutNowPlayingAllowed() throws ServletRequestBindingException, IOException {

                MockHttpServletRequest req = new MockHttpServletRequest();
                req.setParameter(Attributes.Request.V.value(), apiVerion);
                req.setParameter(Attributes.Request.C.value(), CLIENT_NAME);
                req.setParameter(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME);
                req.setParameter(Attributes.Request.P.value(), ADMIN_PASS);
                MockHttpServletResponse res = new MockHttpServletResponse();

                final Player player = playerService.getPlayer(req, res, false, true);
                assertNotNull(player);
                assertEquals(ServiceMockUtils.ADMIN_NAME, player.getUsername());
                assertEquals(PlayerTechnology.WEB, player.getTechnology());
                assertEquals(TranscodeScheme.OFF, player.getTranscodeScheme());
                assertEquals(0, player.getPlayQueue().getFiles().size());

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null,
                        musicFolderDao.getAllMusicFolders());
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                assertNotNull(song);
                req.setParameter(Attributes.Request.PATH.value(), song.getPathString());
                res = new MockHttpServletResponse();
                streamController.handleRequest(req, res, true);
                assertNotEquals(0, res.getContentLength());

                statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                        .findFirst().ifPresentOrElse((status) -> {
                            assertNotNull(status.getPathString());
                            assertNotNull(status.toPath());
                            assertEquals(song.toPath(), status.toPath());
                        }, () -> Assertions.fail());

                res = new MockHttpServletResponse();

                UserSettings userSettings = securityService.getUserSettings(ServiceMockUtils.ADMIN_NAME);
                assertFalse(userSettings.isNowPlayingAllowed()); // default false
                subsonicRESTController.getNowPlaying(req, res);

                Response response = JAXB.unmarshal(new StringReader(res.getContentAsString()), Response.class);
                assertNotNull(response);
                assertEquals(ResponseStatus.OK, response.getStatus());
                assertEquals("1.15.0", response.getVersion());
                NowPlaying nowPlaying = response.getNowPlaying();
                assertNotNull(nowPlaying);
                assertEquals(0, nowPlaying.getEntry().size()); // Entry can't be obtained

                statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                        .findFirst().ifPresentOrElse((status) -> {
                            assertNotNull(status.getPathString());
                            assertNotNull(status.toPath());
                            assertEquals(song.toPath(), status.toPath());
                        }, () -> Assertions.fail());

                res.getOutputStream().close();
            }

            @Test
            @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
            @Order(2)
            void testGetNowPlayingWithNowPlayingAllowed() throws ServletRequestBindingException, IOException {

                MockHttpServletRequest req = new MockHttpServletRequest();
                req.setParameter(Attributes.Request.V.value(), apiVerion);
                req.setParameter(Attributes.Request.C.value(), CLIENT_NAME);
                req.setParameter(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME);
                req.setParameter(Attributes.Request.P.value(), ADMIN_PASS);
                MockHttpServletResponse res = new MockHttpServletResponse();

                final Player player = playerService.getPlayer(req, res, false, true);
                assertNotNull(player);
                assertEquals(ServiceMockUtils.ADMIN_NAME, player.getUsername());
                assertEquals(PlayerTechnology.WEB, player.getTechnology());
                assertEquals(TranscodeScheme.OFF, player.getTranscodeScheme());
                assertEquals(0, player.getPlayQueue().getFiles().size());

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null,
                        musicFolderDao.getAllMusicFolders());
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                assertNotNull(song);
                req.setParameter(Attributes.Request.PATH.value(), song.getPathString());
                res = new MockHttpServletResponse();
                streamController.handleRequest(req, res, true);
                assertNotEquals(0, res.getContentLength());

                statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                        .findFirst().ifPresentOrElse((status) -> {
                            assertNotNull(status.getPathString());
                            assertNotNull(status.toPath());
                            assertEquals(song.toPath(), status.toPath());
                        }, () -> Assertions.fail());

                res = new MockHttpServletResponse();

                UserSettings userSettings = securityService.getUserSettings(ServiceMockUtils.ADMIN_NAME);
                assertFalse(userSettings.isNowPlayingAllowed()); // default false
                userSettings.setNowPlayingAllowed(true); // Change to true
                securityService.updateUserSettings(userSettings);
                subsonicRESTController.getNowPlaying(req, res);

                Response response = JAXB.unmarshal(new StringReader(res.getContentAsString()), Response.class);
                assertNotNull(response);
                assertEquals(ResponseStatus.OK, response.getStatus());
                assertEquals("1.15.0", response.getVersion());
                NowPlaying nowPlaying = response.getNowPlaying();
                assertNotNull(nowPlaying);
                assertNotEquals(0, nowPlaying.getEntry().size()); // Entry can be obtained

                statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                        .findFirst().ifPresentOrElse((status) -> {
                            assertNotNull(status.getPathString());
                            assertNotNull(status.toPath());
                            assertEquals(song.toPath(), status.toPath());
                        }, () -> Assertions.fail());

                res.getOutputStream().close();
            }

        }

        @Test
        void testDownload() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);

            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/download").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

                mvc.perform(MockMvcRequestBuilders.get("/rest/download.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testStream() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);

            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/stream").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

                mvc.perform(MockMvcRequestBuilders.get("/rest/stream.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testHls() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);

            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/hls").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

                mvc.perform(MockMvcRequestBuilders.get("/rest/hls.view").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void testScrobble() throws ExecutionException {
            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/scrobble").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId())).param("time", "0")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/scrobble.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testStar() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/star").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/star.view").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testUnStar() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/unstar").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/unstar.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetStarred() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetStarred2() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred2")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetPodcasts() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPodcasts")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPodcasts.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetNewestPodcasts() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getNewestPodcasts")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getNewestPodcasts.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void testRefreshPodcasts() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/refreshPodcasts")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/refreshPodcasts.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreatePodcastChannel() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/createPodcastChannel")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), "don't create")
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.URL.value(), "https://dont.create")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("40"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Wrong username or password."))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/createPodcastChannel.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), "don't create")
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.URL.value(), "https://dont.create")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("40"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Wrong username or password."))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testDeletePodcastChannel() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePodcastChannel")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).param(Attributes.Request.ID.value(), "0")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePodcastChannel.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS).param(Attributes.Request.ID.value(), "0")
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testDeletePodcastEpisode() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePodcastEpisode")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).param(Attributes.Request.ID.value(), "0")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deletePodcastEpisode.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS).param(Attributes.Request.ID.value(), "0")
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void testDownloadPodcastEpisode() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/downloadPodcastEpisode")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).param(Attributes.Request.ID.value(), "0")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Podcast episode 0 not found."))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/downloadPodcastEpisode.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS).param(Attributes.Request.ID.value(), "0")
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Podcast episode 0 not found."))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetInternetRadioStations() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getInternetRadioStations")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getInternetRadioStations.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetBookmarks() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getBookmarks")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getBookmarks.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreateBookmark() throws ExecutionException {
            try {

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                mvc.perform(MockMvcRequestBuilders.get("/rest/createBookmark")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .param(Attributes.Request.POSITION.value(), Integer.toString(0)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/createBookmark.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .param(Attributes.Request.POSITION.value(), Integer.toString(1)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testDeleteBookmark() throws ExecutionException {
            try {

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteBookmark")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteBookmark.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetPlayQueue() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlayQueue")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getPlayQueue.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testSavePlayQueue() throws ExecutionException {
            try {
                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                mvc.perform(MockMvcRequestBuilders.get("/rest/savePlayQueue")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .param(Attributes.Request.CURRENT.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/savePlayQueue.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .param(Attributes.Request.CURRENT.value(), Integer.toString(song.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetShares() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getShares").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getShares.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreateShare() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/createShare")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/createShare.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void testDeleteShare() throws ExecutionException {
            try {

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                Share share = shareService.createShare(Mockito.mock(HttpServletRequest.class), Arrays.asList(song));
                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteShare")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(share.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteShare.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(share.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE)
                                .value("Shared media not found."))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
        void testUpdateShare() throws ExecutionException {
            try {

                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);
                Share share = shareService.createShare(Mockito.mock(HttpServletRequest.class), Arrays.asList(song));
                mvc.perform(MockMvcRequestBuilders.get("/rest/updateShare")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(share.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/updateShare.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(share.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                shareService.deleteShare(share.getId());

            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetCoverArt() throws ExecutionException {
            try {
                RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, musicFolders);
                MediaFile song = mediaFileDao.getRandomSongs(criteria, ServiceMockUtils.ADMIN_NAME).get(0);

                mvc.perform(MockMvcRequestBuilders.get("/rest/getCoverArt")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

                mvc.perform(MockMvcRequestBuilders.get("/rest/getCoverArt.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAvatar() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAvatar").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), "103").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk());

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAvatar.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ID.value(), "103").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testChangePassword() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/changePassword")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.PASSWORD.value(), ADMIN_PASS).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/changePassword.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.PASSWORD.value(), ADMIN_PASS).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetUser() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getUser").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), ServiceMockUtils.ADMIN_NAME)
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getUser.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), ServiceMockUtils.ADMIN_NAME)
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetUsers() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getUsers").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getUsers.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testCreateUser() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/createUser")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "test1")
                        .param(Attributes.Request.PASSWORD.value(), "test1")
                        .param(Attributes.Request.EMAIL.value(), "test@tesshu.com")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/createUser.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "test2")
                        .param(Attributes.Request.PASSWORD.value(), "test2")
                        .param(Attributes.Request.EMAIL.value(), "test@tesshu.com")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testUpdateUser() throws ExecutionException {
            try {
                mvc.perform(MockMvcRequestBuilders.get("/rest/createUser")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "updateUserName")
                        .param(Attributes.Request.PASSWORD.value(), "updateUserPass")
                        .param(Attributes.Request.EMAIL.value(), "test@tesshu.com")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/updateUser")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "updateUserName")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/updateUser.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "updateUserName")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testDeleteUser() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteUser")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "testUser")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE).value("No such user: testUser"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/deleteUser.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.USER_NAME.value(), "testUser")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_CODE).value("70"))
                        .andExpect(
                                MockMvcResultMatchers.jsonPath(JSON_PATH_ERROR_MESSAGE).value("No such user: testUser"))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_FAILED))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetChatMessages() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getChatMessages")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

                mvc.perform(MockMvcRequestBuilders.get("/rest/getChatMessages.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testAddChatMessage() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/addChatMessage")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

                mvc.perform(MockMvcRequestBuilders.get("/rest/addChatMessage.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isGone());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetLyrics() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getLyrics").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ARTIST.value(), "artist")
                        .param(Attributes.Request.TITLE.value(), "title").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getLyrics.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                        .param(Attributes.Request.ARTIST.value(), "artist")
                        .param(Attributes.Request.TITLE.value(), "title").contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testSetRating() throws ExecutionException {
            try {
                MediaFile album = mediaFileDao.getAlphabeticalAlbums(0, 1, false, musicFolders).get(0);

                mvc.perform(MockMvcRequestBuilders.get("/rest/setRating").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.RATING.value(), Integer.toString(1))
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/setRating.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.RATING.value(), Integer.toString(1))
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAlbumInfo() throws ExecutionException {
            try {
                MediaFile album = mediaFileDao.getAlphabeticalAlbums(0, 1, false, musicFolders).get(0);

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumInfo")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumInfo.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetAlbumInfo2() throws ExecutionException {
            try {
                Album album = albumDao.getAlphabeticalAlbums(0, 1, false, false, musicFolders).get(0);

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumInfo2")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumInfo2.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON)
                        .param(Attributes.Request.ID.value(), Integer.toString(album.getId())))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetVideoInfo() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getVideoInfo")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

                mvc.perform(MockMvcRequestBuilders.get("/rest/getVideoInfo.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetCaptions() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getCaptions")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

                mvc.perform(MockMvcRequestBuilders.get("/rest/getCaptions.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().is5xxServerError());

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testStartScan() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/startScan").param(Attributes.Request.V.value(), apiVerion)
                        .param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/startScan.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }

        @Test
        void testGetScanStatus() throws ExecutionException {
            try {

                mvc.perform(MockMvcRequestBuilders.get("/rest/getScanStatus")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

                mvc.perform(MockMvcRequestBuilders.get("/rest/getScanStatus.view")
                        .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                        .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                        .param(Attributes.Request.P.value(), ADMIN_PASS)
                        .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
                        .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            } catch (Exception e) {
                Assertions.fail();
                throw new ExecutionException(e);
            }
        }
    }
}
