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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXB;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.Integration;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.ServletRequestBindingException;
import org.subsonic.restapi.NowPlaying;
import org.subsonic.restapi.Response;
import org.subsonic.restapi.ResponseStatus;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class SubsonicRESTControllerTest extends AbstractNeedsScan {

    private static final String CLIENT_NAME = "jpsonic";
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASS = "admin";
    private static final String EXPECTED_FORMAT = "json";

    private static String apiVerion;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubsonicRESTController subsonicRESTController;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private StreamController streamController;

    @Autowired
    private StatusService statusService;

    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @BeforeAll
    public static void setupClass() throws IOException {
        apiVerion = TestCaseUtils.restApiVersion();
    }

    @Integration
    @Test
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    /*
     * Wrap&Throw Exception due to constraints of 'springframework' {@link
     * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
     */
    void testPing() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/ping").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.subsonic-response.status").value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.subsonic-response.version").value(apiVerion))
                    .andDo(MockMvcResultHandlers.print());
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
        @Integration
        @Test
        @WithMockUser(username = ADMIN_NAME)
        @Order(1)
        void testGetNowPlayingWithoutNowPlayingAllowed() throws ServletRequestBindingException, IOException {

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter(Attributes.Request.V.value(), apiVerion);
            req.setParameter(Attributes.Request.C.value(), CLIENT_NAME);
            req.setParameter(Attributes.Request.U.value(), ADMIN_NAME);
            req.setParameter(Attributes.Request.P.value(), ADMIN_PASS);
            MockHttpServletResponse res = new MockHttpServletResponse();

            final Player player = playerService.getPlayer(req, res, false, true);
            assertNotNull(player);
            assertEquals(ADMIN_NAME, player.getUsername());
            assertEquals(PlayerTechnology.WEB, player.getTechnology());
            assertEquals(TranscodeScheme.OFF, player.getTranscodeScheme());
            assertEquals(0, player.getPlayQueue().getFiles().size());

            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null,
                    musicFolderDao.getAllMusicFolders());
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
            assertNotNull(song);
            req.setParameter(Attributes.Request.PATH.value(), song.getPath());
            res = new MockHttpServletResponse();
            streamController.handleRequest(req, res);
            assertNotEquals(0, res.getContentLength());

            statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                    .findFirst().ifPresentOrElse((status) -> {
                        assertNotNull(status.getFile());
                        assertEquals(song.getFile(), status.getFile());
                    }, () -> Assertions.fail());

            res = new MockHttpServletResponse();

            UserSettings userSettings = settingsService.getUserSettings(ADMIN_NAME);
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
                        assertNotNull(status.getFile());
                        assertEquals(song.getFile(), status.getFile());
                    }, () -> Assertions.fail());

            res.getOutputStream().close();
        }

        @Integration
        @Test
        @WithMockUser(username = ADMIN_NAME)
        @Order(2)
        void testGetNowPlayingWithNowPlayingAllowed() throws ServletRequestBindingException, IOException {

            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setParameter(Attributes.Request.V.value(), apiVerion);
            req.setParameter(Attributes.Request.C.value(), CLIENT_NAME);
            req.setParameter(Attributes.Request.U.value(), ADMIN_NAME);
            req.setParameter(Attributes.Request.P.value(), ADMIN_PASS);
            MockHttpServletResponse res = new MockHttpServletResponse();

            final Player player = playerService.getPlayer(req, res, false, true);
            assertNotNull(player);
            assertEquals(ADMIN_NAME, player.getUsername());
            assertEquals(PlayerTechnology.WEB, player.getTechnology());
            assertEquals(TranscodeScheme.OFF, player.getTranscodeScheme());
            assertEquals(0, player.getPlayQueue().getFiles().size());

            RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null,
                    musicFolderDao.getAllMusicFolders());
            MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
            assertNotNull(song);
            req.setParameter(Attributes.Request.PATH.value(), song.getPath());
            res = new MockHttpServletResponse();
            streamController.handleRequest(req, res);
            assertNotEquals(0, res.getContentLength());

            statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                    .findFirst().ifPresentOrElse((status) -> {
                        assertNotNull(status.getFile());
                        assertEquals(song.getFile(), status.getFile());
                    }, () -> Assertions.fail());

            res = new MockHttpServletResponse();

            UserSettings userSettings = settingsService.getUserSettings(ADMIN_NAME);
            assertFalse(userSettings.isNowPlayingAllowed()); // default false
            userSettings.setNowPlayingAllowed(true); // Change to true
            settingsService.updateUserSettings(userSettings);
            subsonicRESTController.getNowPlaying(req, res);

            Response response = JAXB.unmarshal(new StringReader(res.getContentAsString()), Response.class);
            assertNotNull(response);
            assertEquals(ResponseStatus.OK, response.getStatus());
            assertEquals("1.15.0", response.getVersion());
            NowPlaying nowPlaying = response.getNowPlaying();
            assertNotNull(nowPlaying);
            assertEquals(1, nowPlaying.getEntry().size()); // Entry can be obtained

            statusService.getAllStreamStatuses().stream().filter(t -> player.getId() == t.getPlayer().getId())
                    .findFirst().ifPresentOrElse((status) -> {
                        assertNotNull(status.getFile());
                        assertEquals(song.getFile(), status.getFile());
                    }, () -> Assertions.fail());

            res.getOutputStream().close();
        }

    }

}
