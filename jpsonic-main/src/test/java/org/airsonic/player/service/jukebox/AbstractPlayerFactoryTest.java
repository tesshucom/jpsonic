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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.controller.ViewName;
import org.airsonic.player.MusicFolderTestDataUtils;
import org.airsonic.player.NeedsHome;
import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.controller.SubsonicRESTController;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.dao.PlayerDao;
import org.airsonic.player.dao.PlayerDaoPlayQueueFactory;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Player;
import org.airsonic.player.service.MediaScannerService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.StringUtil;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = AbstractPlayerFactoryTest.Config.class)
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public abstract class AbstractPlayerFactoryTest {

    private static final String EXPECTED_FORMAT = "json";
    private static String apiVersion;
    private static boolean dataBasePopulated;

    public static final String CLIENT_NAME = "jpsonic";
    public static final String JUKEBOX_PLAYER_NAME = CLIENT_NAME + "-jukebox";

    @Autowired
    protected PlayerService playerService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private MusicFolderDao musicFolderDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private DaoHelper daoHelper;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private ArtistDao artistDao;

    private Player testJukeboxPlayer;

    @BeforeAll
    public static void beforeAll() throws IOException {
        apiVersion = TestCaseUtils.restApiVersion();
        dataBasePopulated = false;
    }

    @TestConfiguration
    static class Config {
        @Bean
        public BeanPostProcessor convertToSpy() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(final Object bean, String beanName) {
                    if (bean instanceof PlayerDaoPlayQueueFactory) {
                        PlayerDaoPlayQueueFactory temp = (PlayerDaoPlayQueueFactory) Mockito.spy(bean);
                        Mockito.doReturn(Mockito.spy(temp.createPlayQueue())).when(temp).createPlayQueue();
                        return temp;
                    }
                    return bean;
                }
            };
        }
    }

    /**
     * Populate test datas in the database only once.
     *
     * <ul>
     * <li>Creates 2 music folder</li>
     * <li>Scans the music folders</li>
     * <li>Creates a test jukebox player</li>
     * </ul>
     */
    private void populateDatabase() {
        if (!dataBasePopulated) {

            MatcherAssert.assertThat(musicFolderDao.getAllMusicFolders().size(), is(equalTo(1)));
            MusicFolderTestDataUtils.getTestMusicFolders().forEach(musicFolderDao::createMusicFolder);
            settingsService.clearMusicFolderCache();

            TestCaseUtils.execScan(mediaScannerService);

            MatcherAssert.assertThat(playerDao.getAllPlayers().size(), is(equalTo(0)));
            createTestPlayer();
            MatcherAssert.assertThat(playerDao.getAllPlayers().size(), is(equalTo(1)));

            dataBasePopulated = true;
        }
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        populateDatabase();

        testJukeboxPlayer = findTestJukeboxPlayer();
        MatcherAssert.assertThat(testJukeboxPlayer, is(CoreMatchers.notNullValue()));
        Mockito.reset(testJukeboxPlayer.getPlayQueue());
        testJukeboxPlayer.getPlayQueue().clear();
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().size(), is(equalTo(0)));
        testJukeboxPlayer.getPlayQueue().addFiles(true,
                mediaFileDao.getSongsForAlbum("_DIR_ Ravel", "Complete Piano Works"));
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().size(), is(equalTo(2)));
    }

    @AfterEach
    public void cleanDataBase() {
        daoHelper.getJdbcTemplate().execute("DROP SCHEMA PUBLIC CASCADE");
        dataBasePopulated = false;
    }

    protected abstract void createTestPlayer();

    private Player findTestJukeboxPlayer() {
        return playerDao.getAllPlayers().stream().filter(player -> JUKEBOX_PLAYER_NAME.equals(player.getName()))
                .findFirst().orElseThrow(() -> new RuntimeException("No player found in database"));
    }

    private String convertDateToString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", settingsService.getLocale());
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    @SuppressWarnings("PMD.UseLocaleWithCaseConversions") // MediaType literal comparison
    private ResultMatcher playListItem1isCorrect() {
        MediaFile mediaFile = testJukeboxPlayer.getPlayQueue().getFile(0);
        MediaFile parent = mediaFileDao.getMediaFile(mediaFile.getParentPath());
        Album album = albumDao.getAlbum(mediaFile.getArtist(), mediaFile.getAlbumName());
        Artist artist = artistDao.getArtist(mediaFile.getArtist());
        MatcherAssert.assertThat(album, is(CoreMatchers.notNullValue()));
        return result -> {
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].id").value(mediaFile.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].parent").value(parent.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].isDir").value(mediaFile.isDirectory()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].title").value(mediaFile.getTitle()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].album").value(mediaFile.getAlbumName())
                    .match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].artist").value(mediaFile.getArtist()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].coverArt").value(parent.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].size").value(mediaFile.getFileSize()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].contentType")
                    .value(StringUtil.getMimeType(mediaFile.getFormat())).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].suffix").value(mediaFile.getFormat()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].duration").value(mediaFile.getDurationSeconds())
                    .match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].bitRate").value(mediaFile.getBitRate())
                    .match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].path")
                    .value(SubsonicRESTController.getRelativePath(mediaFile, settingsService)).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].isVideo").value(mediaFile.isVideo()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].playCount").isNumber().match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].created")
                    .value(convertDateToString(mediaFile.getCreated())).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].albumId").value(album.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].artistId").value(artist.getId()).match(result);
            jsonPath("$.subsonic-response.jukeboxPlaylist.entry[0].type")
                    .value(mediaFile.getMediaType().name().toLowerCase()).match(result);
        };
    }

    @Test
    @WithMockUser(username = "admin")
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link ResultActions#andExpect(ResultMatcher)}
     */
    void testJukeboxStartAction() throws ExecutionException {
        // Given

        // When and Then
        performStartAction();
        performStatusAction("true");
        try {
            performGetAction().andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.currentIndex").value("0"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.playing").value("true"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.gain").value("0.75"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.position").value("0"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.entry").isArray())
                    .andExpect(jsonPath("$.subsonic-response.jukeboxPlaylist.entry.length()").value(2))
                    .andExpect(playListItem1isCorrect()).andDo(MockMvcResultHandlers.print());
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

        Mockito.verify(testJukeboxPlayer.getPlayQueue(), Mockito.times(2)).setStatus(PlayQueue.Status.PLAYING);
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().getIndex(), is(equalTo(0)));
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().getStatus(), is(equalTo(PlayQueue.Status.PLAYING)));
    }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    @Test
    @WithMockUser(username = "admin")
    void testJukeboxStopAction() throws ExecutionException {
        // Given

        // When and Then
        performStartAction();
        performStatusAction("true");
        performStopAction();
        performStatusAction("false");

        Mockito.verify(testJukeboxPlayer.getPlayQueue(), Mockito.times(2)).setStatus(PlayQueue.Status.PLAYING);
        Mockito.verify(testJukeboxPlayer.getPlayQueue(), Mockito.times(1)).setStatus(PlayQueue.Status.STOPPED);
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().getIndex(), is(equalTo(0)));
        MatcherAssert.assertThat(testJukeboxPlayer.getPlayQueue().getStatus(), is(equalTo(PlayQueue.Status.STOPPED)));
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link
     * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
     */
    private void performStatusAction(String expectedPlayingValue) throws ExecutionException {
        try {
            mvc.perform(get("/rest/" + ViewName.JUKEBOX_CONTROL.value()).param("v", apiVersion).param("c", CLIENT_NAME)
                    .param("f", EXPECTED_FORMAT).param("action", "status").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value(expectedPlayingValue))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link
     * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
     */
    private ResultActions performGetAction() throws ExecutionException {
        try {
            return mvc
                    .perform(get("/rest/" + ViewName.JUKEBOX_CONTROL.value()).param("v", apiVersion)
                            .param("c", CLIENT_NAME).param("f", EXPECTED_FORMAT).param("action", "get")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(jsonPath("$.subsonic-response.status").value("ok"));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link
     * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
     */
    private void performStopAction() throws ExecutionException {
        try {
            mvc.perform(get("/rest/" + ViewName.JUKEBOX_CONTROL.value()).param("v", apiVersion).param("c", CLIENT_NAME)
                    .param("f", EXPECTED_FORMAT).param("action", "stop").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value("false"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link
     * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
     */
    private void performStartAction() throws ExecutionException {
        try {
            mvc.perform(get("/rest/" + ViewName.JUKEBOX_CONTROL.value()).param("v", apiVersion).param("c", CLIENT_NAME)
                    .param("f", EXPECTED_FORMAT).param("action", "start").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(jsonPath("$.subsonic-response.status").value("ok"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.currentIndex").value("0"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.playing").value("true"))
                    .andExpect(jsonPath("$.subsonic-response.jukeboxStatus.position").value("0"));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }
}
