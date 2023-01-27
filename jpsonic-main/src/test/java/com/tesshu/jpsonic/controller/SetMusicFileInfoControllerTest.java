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

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileCache;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.metadata.MetaDataParserFactory;
import com.tesshu.jpsonic.service.scanner.ScannerStateServiceImpl;
import com.tesshu.jpsonic.service.scanner.WritableMediaFileService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.ServletRequestBindingException;

class SetMusicFileInfoControllerTest {

    private static final int ID = 0;
    private static final String PATH = "pathString";
    private MediaFileDao mediaFileDao;
    private ScannerStateServiceImpl scannerStateService;
    private SetMusicFileInfoController controller;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws ExecutionException {
        MediaFileService mediaFileService = mock(MediaFileService.class);
        MediaFile album = new MediaFile();
        album.setId(ID);
        album.setPathString(PATH);
        Mockito.when(mediaFileService.getMediaFileStrict(ID)).thenReturn(album);

        mediaFileDao = mock(MediaFileDao.class);
        scannerStateService = mock(ScannerStateServiceImpl.class);
        WritableMediaFileService writableMediaFileService = new WritableMediaFileService(mediaFileDao,
                scannerStateService, mock(MediaFileService.class), mock(AlbumDao.class), mock(MediaFileCache.class),
                mock(MetaDataParserFactory.class), mock(SettingsService.class), mock(SecurityService.class),
                mock(JapaneseReadingUtils.class), mock(IndexManager.class));
        controller = new SetMusicFileInfoController(mediaFileService, writableMediaFileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @WithMockUser(username = "admin")
    void testPost() throws Exception {
        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders.post("/setMusicFileInfo.view").param(Attributes.Request.ID.value(),
                        Integer.toString(ID)))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl(ViewName.MAIN.value() + "?id=" + ID))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andReturn();
        assertNotNull(result);
    }

    @Nested
    class CommentTest {

        @Test
        void testNoScanning() throws ServletRequestBindingException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter(Attributes.Request.ID.value(), Integer.toString(ID));
            request.addParameter(Attributes.Request.ACTION.value(), "comment");
            String comment = "Comment Test.";
            request.addParameter(Attributes.Request.COMMENT.value(), comment);

            controller.post(request);
            Mockito.verify(mediaFileDao, Mockito.times(1)).updateComment(PATH, comment);
        }

        @Test
        void testScanning() throws ServletRequestBindingException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter(Attributes.Request.ID.value(), Integer.toString(ID));
            request.addParameter(Attributes.Request.ACTION.value(), "comment");
            String comment = "Comment Test.";
            request.addParameter(Attributes.Request.COMMENT.value(), comment);

            Mockito.when(scannerStateService.isScanning()).thenReturn(true);
            controller.post(request);
            Mockito.verify(mediaFileDao, Mockito.times(1)).updateComment(PATH, comment);
        }
    }

    @Nested
    class ResetLastScannedTest {

        @Test
        void testNoScanning() throws ServletRequestBindingException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter(Attributes.Request.ID.value(), Integer.toString(ID));
            request.addParameter(Attributes.Request.ACTION.value(), "resetLastScanned");
            controller.post(request);
            Mockito.verify(mediaFileDao, Mockito.times(1)).resetLastScanned(ID);
        }

        @Test
        void testScanning() throws ServletRequestBindingException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter(Attributes.Request.ID.value(), Integer.toString(ID));
            request.addParameter(Attributes.Request.ACTION.value(), "resetLastScanned");

            Mockito.when(scannerStateService.isScanning()).thenReturn(true);
            controller.post(request);
            Mockito.verify(mediaFileDao, Mockito.never()).resetLastScanned(ID);
        }
    }
}
