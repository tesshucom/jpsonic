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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Share;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

class ExternalPlayerControllerTest {

    private MusicFolderService musicFolderService;
    private ShareService shareService;
    private JWTSecurityService jwtSecurityService;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        musicFolderService = mock(MusicFolderService.class);
        shareService = mock(ShareService.class);
        jwtSecurityService = mock(JWTSecurityService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new ExternalPlayerController(musicFolderService, mock(PlayerService.class),
                            shareService, mock(MediaFileService.class), jwtSecurityService))
            .build();
    }

    @Test
    void testHandleRequest() throws Exception {

        final int shareId = 1;
        Share share = new Share();
        share.setId(shareId);
        LocalDateTime localDateTime = LocalDateTime.now();
        Instant current = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        share.setCreated(current);
        Instant expires = localDateTime.plusDays(10).atZone(ZoneId.systemDefault()).toInstant();
        share.setExpires(expires);
        Mockito.when(shareService.getShareByName(Mockito.anyString())).thenReturn(share);

        MediaFile mediaFile = new MediaFile();
        Path path = Path
            .of(ExternalPlayerControllerTest.class
                .getResource(
                        "/MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac")
                .toURI());
        mediaFile.setPathString(path.toString());
        List<MediaFile> mediaFiles = Arrays.asList(mediaFile);

        MusicFolder folder = new MusicFolder("", "", true, expires, false);
        List<MusicFolder> folders = Arrays.asList(folder);
        Mockito
            .when(musicFolderService.getMusicFoldersForUser(Mockito.anyString()))
            .thenReturn(folders);
        Mockito.when(shareService.getSharedFiles(shareId, folders)).thenReturn(mediaFiles);

        UriComponentsBuilder builder = mock(UriComponentsBuilder.class);
        UriComponents components = mock(UriComponents.class);
        Mockito.when(builder.build()).thenReturn(components);
        Mockito
            .when(jwtSecurityService
                .addJWTToken(Mockito.any(UriComponentsBuilder.class),
                        Mockito.nullable(Instant.class)))
            .thenReturn(builder);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders
                .get("/ext/share/AAaaA")
                .param(Attributes.Request.USER_NAME.value(), "admin")
                .param(Attributes.Request.FORCE_CUSTOM.value(), Boolean.toString(false)))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        assertNotNull(result);
    }
}
