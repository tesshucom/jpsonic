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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXB;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.Integration;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.ServletRequestBindingException;
import org.subsonic.restapi.NowPlaying;
import org.subsonic.restapi.Response;
import org.subsonic.restapi.ResponseStatus;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@AutoConfigureMockMvc
@SuppressWarnings({ "PMD.JUnitTestsShouldIncludeAssert", "PMD.AvoidCatchingGenericException", "PMD.DetachedTestCase" })
/*
 * Wrap&Throw Exception due to constraints of 'springframework' {@link
 * MockMvc#perform(org.springframework.test.web.servlet.RequestBuilder)}
 */
class SubsonicRESTControllerTest extends AbstractNeedsScan {

    private static final String CLIENT_NAME = "jpsonic";
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PASS = "admin";
    private static final String EXPECTED_FORMAT = "json";
    private static final String JSON_PATH_STATUS = "$.subsonic-response.status";
    private static final String JSON_PATH_VERSION = "$.subsonic-response.version";
    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Music"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
    }

    private static String apiVerion;

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
    private SettingsService settingsService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private ArtistDao artistDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

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
    void testPing() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/ping").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
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
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetIndexes() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getIndexes").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.GENRE.value(), "genre")
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), MUSIC_FOLDERS.get(0).getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetArtists() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getArtists").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetSimilarSongs() throws ExecutionException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, MUSIC_FOLDERS);
        MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs")
                    .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetSimilarSongs2() throws ExecutionException {
        Artist artist = artistDao.getAlphabetialArtists(0, 1, MUSIC_FOLDERS).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getSimilarSongs2")
                    .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetTopSongs() throws ExecutionException {
        Artist artist = artistDao.getAlphabetialArtists(0, 1, MUSIC_FOLDERS).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getTopSongs").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ARTIST.value(), artist.getName()).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetArtistInfo() throws ExecutionException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, MUSIC_FOLDERS);
        MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetArtistInfo2() throws ExecutionException {
        Artist artist = artistDao.getAlphabetialArtists(0, 1, MUSIC_FOLDERS).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getArtistInfo2")
                    .param(Attributes.Request.V.value(), apiVerion).param(Attributes.Request.C.value(), CLIENT_NAME)
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetArtist() throws ExecutionException {
        Artist artist = artistDao.getAlphabetialArtists(0, 1, MUSIC_FOLDERS).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getArtist").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(artist.getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetAlbumList() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), Integer.toString(MUSIC_FOLDERS.get(0).getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetAlbumList2() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getAlbumList2").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.TYPE.value(), AlbumListType.NEWEST.getId())
                    .param(Attributes.Request.MUSIC_FOLDER_ID.value(), Integer.toString(MUSIC_FOLDERS.get(0).getId()))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testScrobble() throws ExecutionException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(1, null, null, null, MUSIC_FOLDERS);
        MediaFile song = mediaFileDao.getRandomSongs(criteria, ADMIN_NAME).get(0);
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/scrobble").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.ID.value(), Integer.toString(song.getId())).param("time", "0")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetStarred() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testGetStarred2() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/getStarred2").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.U.value(), ADMIN_NAME).param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testCreateShare() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/createShare").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testCreateUser() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/createUser").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.USER_NAME.value(), "test")
                    .param(Attributes.Request.PASSWORD.value(), "test")
                    .param(Attributes.Request.EMAIL.value(), "test@tesshu.com").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));
        } catch (Exception e) {
            Assertions.fail();
            throw new ExecutionException(e);
        }
    }

    @Test
    void testUpdateUser() throws ExecutionException {
        try {
            mvc.perform(MockMvcRequestBuilders.get("/rest/createUser").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.USER_NAME.value(), "updateTest")
                    .param(Attributes.Request.PASSWORD.value(), "updateTest")
                    .param(Attributes.Request.EMAIL.value(), "test@tesshu.com").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

            mvc.perform(MockMvcRequestBuilders.get("/rest/updateUser").param(Attributes.Request.V.value(), apiVerion)
                    .param(Attributes.Request.C.value(), CLIENT_NAME).param(Attributes.Request.U.value(), ADMIN_NAME)
                    .param(Attributes.Request.P.value(), ADMIN_PASS)
                    .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                    .param(Attributes.Request.USER_NAME.value(), "updateTest").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value("ok"))
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
