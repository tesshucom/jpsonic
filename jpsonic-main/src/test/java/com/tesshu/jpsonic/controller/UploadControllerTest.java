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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.servlet.ModelAndView;

@SpringBootTest
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UploadControllerTest {

    @Autowired
    private UploadController uploadController;

    @Autowired
    private MusicFolderDao musicFolderDao;

    private static final String FILE_NAME = "piano.mp3";
    private static final String FILE_PATH = "/MEDIAS/piano.mp3";
    private static final String FILE_CONTENT_TYPE = "audio/mpeg";
    private static final String ZIP_NAME = "piano.zip";
    private static final String ZIP_PATH = "/MEDIAS/piano.zip";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String BOUNDARY = "265001916915724";
    private static final String SEPA = "\r\n";

    @SuppressWarnings({ "unchecked", "PMD.DefaultPackage" })
    // @Test Currently it is not possible to run two tests in a row
    @WithMockUser(username = "admin")
    void testHandleRequestInternalWithFile(@TempDir Path tempDirPath) throws Exception {

        MusicFolder musicFolder = new MusicFolder(Integer.valueOf(0), tempDirPath.toString(), "Incoming1", true,
                new Date());
        musicFolderDao.createMusicFolder(musicFolder);

        URL url = UploadController.class.getResource(FILE_PATH);

        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();

        byte[] data = IOUtils.toByteArray(Files.newInputStream(Path.of(url.toURI())));
        req.setContent(createFileContent(tempDirPath.toString(), FILE_NAME, FILE_CONTENT_TYPE, data));
        req.setContentType("multipart/form-data; boundary=" + BOUNDARY);

        MockMultipartFile file = new MockMultipartFile("file", FILE_NAME, FILE_CONTENT_TYPE, data);
        req.addFile(file);
        req.setMethod(HttpMethod.POST.name());

        ModelAndView result = uploadController.handleRequestInternal(req, new MockHttpServletResponse());
        assertEquals("upload", result.getViewName());
        Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
        assertNotNull(model);
        assertEquals(FILE_NAME, ((List<Path>) model.get("uploadedFiles")).get(0).getFileName().toString());
        assertEquals(0, ((List<Path>) model.get("unzippedFiles")).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = "admin")
    void testHandleRequestInternalWithZip(@TempDir Path tempDirPath) throws Exception {

        MusicFolder musicFolder = new MusicFolder(Integer.valueOf(1), tempDirPath.toString(), "Incoming2", true,
                new Date());
        musicFolderDao.createMusicFolder(musicFolder);

        URL url = UploadController.class.getResource(ZIP_PATH);

        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();

        byte[] data = IOUtils.toByteArray(Files.newInputStream(Path.of(url.toURI())));
        byte[] fileContent = createFileContent(tempDirPath.toString(), ZIP_NAME, ZIP_CONTENT_TYPE, data);

        String zipField = "--" + BOUNDARY + SEPA + " Content-Disposition: form-data; name=\""
                + UploadController.FIELD_NAME_UNZIP + "\";" + "Content-type: text/plain" + SEPA + " value=\"true\""
                + SEPA + SEPA;
        String zipValue = "true" + SEPA;
        req.setContent(ArrayUtils.addAll((zipField + zipValue).getBytes(), fileContent));
        req.setContentType("multipart/form-data; boundary=" + BOUNDARY);

        MockMultipartFile file = new MockMultipartFile("file", ZIP_NAME, ZIP_CONTENT_TYPE, data);
        req.addFile(file);
        req.setMethod(HttpMethod.POST.name());

        ModelAndView result = uploadController.handleRequestInternal(req, new MockHttpServletResponse());
        assertEquals("upload", result.getViewName());
        Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
        assertNotNull(model);
        assertEquals(FILE_NAME, ((List<Path>) model.get("unzippedFiles")).get(0).getFileName().toString());
    }

    public byte[] createFileContent(String tempDir, String fileName, String contentType, byte[] fileValue) {
        String dirField = "--" + BOUNDARY + SEPA + " Content-Disposition: form-data; name=\""
                + UploadController.FIELD_NAME_DIR + "\";" + "Content-type: text/plain" + SEPA + " value=\"" + tempDir
                + "\"" + SEPA + SEPA;
        String dirValue = tempDir + SEPA;
        String fileField = "--" + BOUNDARY + SEPA + " Content-Disposition: form-data; name=\"file\"; filename=\""
                + fileName + "\"" + SEPA + "Content-type: " + contentType + SEPA + SEPA;
        String end = SEPA + "--" + BOUNDARY + "--";
        return ArrayUtils.addAll((dirField + dirValue + fileField).getBytes(),
                ArrayUtils.addAll(fileValue, end.getBytes()));
    }

    @Nested
    class ExceptionTest {

        private MediaScannerService mediaScannerService;

        @BeforeEach
        public void setup() throws ExecutionException {
            mediaScannerService = mock(MediaScannerService.class);
            uploadController = new UploadController(mock(SecurityService.class), mock(PlayerService.class),
                    mock(StatusService.class), mock(SettingsService.class), mediaScannerService);
        }

        @Test
        void testIsScanning() {

            Mockito.when(mediaScannerService.isScanning()).thenReturn(true);
            ModelAndView result = uploadController.handleRequestInternal(mock(HttpServletRequest.class),
                    mock(HttpServletResponse.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) result.getModel().get("model");
            assertEquals("Currently scanning. Please try again after a while.",
                    ((Exception) model.get("exception")).getMessage());
        }
    }
}
