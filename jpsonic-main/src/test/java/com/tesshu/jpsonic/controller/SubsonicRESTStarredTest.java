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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao;
import com.tesshu.jpsonic.persistence.api.repository.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.connector.api.JsonResult;
import com.tesshu.jpsonic.util.connector.api.Response;
import com.tesshu.jpsonic.util.connector.api.Starred;
import com.tesshu.jpsonic.util.connector.api.Starred2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SubsonicRESTStarredTest extends AbstractNeedsScan {

    private static final String CLIENT_NAME = "jpsonic";
    private static final String ADMIN_PASS = "admin";
    private static final String EXPECTED_FORMAT = "json";
    private static final String JSON_PATH_STATUS = "$.subsonic-response.status";
    private static final String JSON_PATH_VERSION = "$.subsonic-response.version";
    private static final String JSON_VALUE_OK = "ok";

    private final String apiVerion = TestCaseUtils.restApiVersion();
    private final List<MusicFolder> musicFolders = List
        .of(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 1, false));
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .findAndRegisterModules();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SettingsService settingsService;
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
        populateDatabaseOnlyOnce();
    }

    private Response readResponse(String jsonContent)
            throws JsonMappingException, JsonProcessingException {
        return objectMapper.readValue(jsonContent, JsonResult.class).getResponse();
    }

    @Documented
    private @interface Decisions {
        @interface DataType {
            @interface FileStructure {
            }

            @interface Id3 {
            }
        }

        @interface IgnoreTimestamp {
            @interface False {
            }

            @interface True {
            }
        }
    }

    @Decisions.DataType.FileStructure
    @Decisions.IgnoreTimestamp.False
    @Order(1)
    @Test
    void testStarAndUnstar() throws Exception {

        List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
        assertEquals(2, artists.size());
        assertEquals("_DIR_ Ravel", artists.get(0).getArtist());
        assertEquals("_DIR_ Sixteen Horsepower", artists.get(1).getArtist());

        List<MediaFile> albums = mediaFileDao
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
        assertEquals(4, albums.size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                albums.get(0).getAlbumName());
        assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", albums.get(1).getAlbumName());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", albums.get(2).getAlbumName());
        assertEquals("Complete Piano Works", albums.get(3).getAlbumName());

        List<MediaFile> songs = mediaFileDao
            .getChildrenOf(albums.get(0).getPathString(), 0, Integer.MAX_VALUE,
                    ChildOrder.BY_ALPHA);
        assertEquals(2, songs.size());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria", songs.get(0).getTitle());

        // star
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/star")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("id", Integer.toString(artists.get(0).getId()))
                .param("id", Integer.toString(albums.get(0).getId()))
                .param("id", Integer.toString(songs.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        // getStarred
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        Response response = readResponse(result.getResponse().getContentAsString());
        Starred starred = response.getStarred();
        assertNotNull(starred);
        assertEquals(1, starred.getArtist().size());
        assertEquals("_DIR_ Ravel", starred.getArtist().get(0).getName());
        assertEquals(1, starred.getAlbum().size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                starred.getAlbum().get(0).getAlbum());
        assertEquals(1, starred.getSong().size());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria",
                starred.getSong().get(0).getTitle());

        // unstar
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/unstar")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("id", Integer.toString(artists.get(0).getId()))
                .param("id", Integer.toString(albums.get(0).getId()))
                .param("id", Integer.toString(songs.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        response = readResponse(result.getResponse().getContentAsString());
        starred = response.getStarred();
        assertNotNull(starred);
        assertEquals(0, starred.getArtist().size());
        assertEquals(0, starred.getAlbum().size());
        assertEquals(0, starred.getSong().size());
    }

    @Decisions.DataType.FileStructure
    @Decisions.IgnoreTimestamp.True
    @Order(2)
    @Test
    void testStarAndUnstarAfterScanWithIgnoreStamp() throws Exception {

        List<MediaFile> artists = mediaFileDao.getArtistAll(musicFolders);
        assertEquals(2, artists.size());
        assertEquals("_DIR_ Ravel", artists.get(0).getArtist());
        assertEquals("_DIR_ Sixteen Horsepower", artists.get(1).getArtist());

        List<MediaFile> albums = mediaFileDao
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, musicFolders);
        assertEquals(4, albums.size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                albums.get(0).getAlbumName());
        assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", albums.get(1).getAlbumName());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", albums.get(2).getAlbumName());
        assertEquals("Complete Piano Works", albums.get(3).getAlbumName());

        List<MediaFile> songs = mediaFileDao
            .getChildrenOf(albums.get(0).getPathString(), 0, Integer.MAX_VALUE,
                    ChildOrder.BY_ALPHA);
        assertEquals(2, songs.size());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria", songs.get(0).getTitle());

        // star
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/star")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("id", Integer.toString(artists.get(0).getId()))
                .param("id", Integer.toString(albums.get(0).getId()))
                .param("id", Integer.toString(songs.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        // Scan with IgnoreFileTimestamps enabled
        settingsService.setIgnoreFileTimestamps(true);
        settingsService.save();
        TestCaseUtils.execScan(mediaScannerService);

        // getStarred
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        Response response = readResponse(result.getResponse().getContentAsString());
        Starred starred = response.getStarred();
        assertNotNull(starred);
        assertEquals(1, starred.getArtist().size());
        assertEquals("_DIR_ Ravel", starred.getArtist().get(0).getName());
        assertEquals(1, starred.getAlbum().size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                starred.getAlbum().get(0).getAlbum());
        assertEquals(1, starred.getSong().size());
        assertEquals("Bach: Goldberg Variations, BWV 988 - Aria",
                starred.getSong().get(0).getTitle());

        // unstar
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/unstar")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("id", Integer.toString(artists.get(0).getId()))
                .param("id", Integer.toString(albums.get(0).getId()))
                .param("id", Integer.toString(songs.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        response = readResponse(result.getResponse().getContentAsString());
        starred = response.getStarred();
        assertNotNull(starred);
        assertEquals(0, starred.getArtist().size());
        assertEquals(0, starred.getAlbum().size());
        assertEquals(0, starred.getSong().size());
    }

    @Decisions.DataType.Id3
    @Decisions.IgnoreTimestamp.False
    @Order(3)
    @Test
    void testStarAndUnstar2() throws Exception {

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
        assertEquals(4, artists.size());
        assertEquals("_DIR_ Ravel", artists.get(0).getName());
        assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", artists.get(1).getName());
        assertEquals("_ID3_ARTIST_ Céline Frisch: Café Zimmermann", artists.get(2).getName());
        assertEquals("_ID3_ARTIST_ Sixteen Horsepower", artists.get(3).getName());

        List<Album> albums = albumDao
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, musicFolders);
        assertEquals(4, albums.size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                albums.get(0).getName());
        assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", albums.get(1).getName());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", albums.get(2).getName());
        assertEquals("Complete Piano Works", albums.get(3).getName());

        // star
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/star")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("artistId", Integer.toString(artists.get(0).getId()))
                .param("albumId", Integer.toString(albums.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        // getStarred
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred2")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        Response response = readResponse(result.getResponse().getContentAsString());
        Starred2 starred2 = response.getStarred2();
        assertNotNull(starred2);
        assertEquals(1, starred2.getArtist().size());
        assertEquals("_DIR_ Ravel", starred2.getArtist().get(0).getName());
        assertEquals(1, starred2.getAlbum().size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                starred2.getAlbum().get(0).getName());
        assertEquals(0, starred2.getSong().size());

        // unstar
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/unstar")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("artistId", Integer.toString(artists.get(0).getId()))
                .param("albumId", Integer.toString(albums.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred2")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        response = readResponse(result.getResponse().getContentAsString());
        starred2 = response.getStarred2();
        assertNotNull(starred2);
        assertEquals(0, starred2.getArtist().size());
        assertEquals(0, starred2.getAlbum().size());
        assertEquals(0, starred2.getSong().size());
    }

    @Decisions.DataType.Id3
    @Decisions.IgnoreTimestamp.True
    @Order(4)
    @Test
    void testStarAndUnstar2AfterScanWithIgnoreStamp() throws Exception {

        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, musicFolders);
        assertEquals(4, artists.size());
        assertEquals("_DIR_ Ravel", artists.get(0).getName());
        assertEquals("_ID3_ALBUMARTIST_ Sarah Walker/Nash Ensemble", artists.get(1).getName());
        assertEquals("_ID3_ARTIST_ Céline Frisch: Café Zimmermann", artists.get(2).getName());
        assertEquals("_ID3_ARTIST_ Sixteen Horsepower", artists.get(3).getName());

        List<Album> albums = albumDao
            .getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, musicFolders);
        assertEquals(4, albums.size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                albums.get(0).getName());
        assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", albums.get(1).getName());
        assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", albums.get(2).getName());
        assertEquals("Complete Piano Works", albums.get(3).getName());

        // star
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/star")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .param("artistId", Integer.toString(artists.get(0).getId()))
                .param("albumId", Integer.toString(albums.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        // Scan with IgnoreFileTimestamps enabled
        settingsService.setIgnoreFileTimestamps(true);
        settingsService.save();
        TestCaseUtils.execScan(mediaScannerService);

        // getStarred
        MvcResult result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred2")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        Response response = readResponse(result.getResponse().getContentAsString());
        Starred2 starred2 = response.getStarred2();
        assertNotNull(starred2);
        assertEquals(0, starred2.getArtist().size()); // TODO This is a case that has not yet been
                                                      // fixed.
        assertEquals(1, starred2.getAlbum().size());
        assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]",
                starred2.getAlbum().get(0).getName());
        assertEquals(0, starred2.getSong().size());

        // unstar
        mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/unstar")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                // .param("artistId", Integer.toString(artists.get(0).getId()))
                .param("albumId", Integer.toString(albums.get(0).getId()))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion));

        result = mvc
            .perform(MockMvcRequestBuilders
                .get("/rest/getStarred2")
                .param(Attributes.Request.V.value(), apiVerion)
                .param(Attributes.Request.C.value(), CLIENT_NAME)
                .param(Attributes.Request.U.value(), ServiceMockUtils.ADMIN_NAME)
                .param(Attributes.Request.P.value(), ADMIN_PASS)
                .param(Attributes.Request.F.value(), EXPECTED_FORMAT)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_STATUS).value(JSON_VALUE_OK))
            .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_VERSION).value(apiVerion))
            .andReturn();

        response = readResponse(result.getResponse().getContentAsString());
        starred2 = response.getStarred2();
        assertNotNull(starred2);
        assertEquals(0, starred2.getArtist().size());
        assertEquals(0, starred2.getAlbum().size());
        assertEquals(0, starred2.getSong().size());
    }
}
