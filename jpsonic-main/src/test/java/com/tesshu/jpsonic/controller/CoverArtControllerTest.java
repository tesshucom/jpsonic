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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.controller.CoverArtController.MediaFileCoverArtRequest;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.logic.CoverArtLogic;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.metadata.FFmpeg;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
@ExtendWith(NeedsHome.class)
class CoverArtControllerTest {

    private MediaFileService mediaFileService;
    private CoverArtController controller;
    private MockMvc mockMvc;

    private static File createFile(String resourcePath) throws URISyntaxException {
        return new File(CoverArtControllerTest.class.getResource(resourcePath).toURI());
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        mediaFileService = mock(MediaFileService.class);
        TranscodingService transcodingService = new TranscodingService(null, null, null, null, null);
        FFmpeg ffmpeg = new FFmpeg(transcodingService);
        controller = new CoverArtController(mediaFileService, ffmpeg, mock(PlaylistService.class),
                mock(PodcastService.class), mock(ArtistDao.class), mock(AlbumDao.class), mock(CoverArtLogic.class),
                mock(FontLoader.class));
        controller.init();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class GetTest {

        private final BiConsumer<File, String> mediaFileStub = (file, id) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            Mockito.when(mediaFileService.getMediaFile(file)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(mediaFile.getFile());
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(file.getParent());
            parent.setArtist("CoverArtControllerTest#GetTest");
            Mockito.when(mediaFileService.getParentOf(mediaFile)).thenReturn(parent);
        };

        private final BiConsumer<File, String> videoStub = (file, id) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            mediaFile.setMediaType(MediaType.VIDEO);
            Mockito.when(mediaFileService.getMediaFile(file)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(mediaFile.getFile());
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(file.getParent());
            parent.setArtist("CoverArtControllerTest#GetTest");
            Mockito.when(mediaFileService.getParentOf(mediaFile)).thenReturn(parent);
        };

        @Test
        @WithMockUser(username = "admin")
        void testNoParam() throws Exception {
            // fallback case (default_cover.jpg)
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);

            result = mockMvc.perform(MockMvcRequestBuilders.get("/ext/" + ViewName.COVER_ART.value()))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        @WithMockUser(username = "admin")
        void testWithEmbededImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createFile("/MEDIAS/Metadata/coverart/album.jpeg"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithEmptyImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createFile("/MEDIAS/Metadata/coverart/album.jpg"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithImageCannotRead() throws Exception {

            final String id = "99";
            File file = new File("/MEDIAS/Metadata/coverart/unknown.gif");

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            Mockito.when(mediaFileService.getMediaFile(file)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(mediaFile.getFile());
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(file.getParent());
            parent.setArtist("CoverArtControllerTest#GetTest");
            Mockito.when(mediaFileService.getParentOf(mediaFile)).thenReturn(parent);

            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testVideoThumbnail() throws Exception {
            final String mediaFileId = "99";
            videoStub.accept(createFile("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }
    }

    @Nested
    class SendUnscaledTest {

        private final Function<File, File> mediaFileStub = (file) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            Mockito.when(mediaFileService.getMediaFile(file)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(mediaFile.getFile());
            return file;
        };

        @Test
        void testWithEmbededImage() throws Exception {
            File file = mediaFileStub.apply(createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac"));
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            MediaFileCoverArtRequest req = controller.new MediaFileCoverArtRequest(mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            controller.sendUnscaled(req, res);
            assertEquals("image/png", res.getContentType());
            try (OutputStream s = res.getOutputStream()) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            File file = mediaFileStub.apply(createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3"));
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            MediaFileCoverArtRequest req = controller.new MediaFileCoverArtRequest(mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            assertThrows(ExecutionException.class, () -> controller.sendUnscaled(req, res));
        }

        @Test
        void testWithImage() throws Exception {
            File file = mediaFileStub.apply(createFile("/MEDIAS/Metadata/coverart/album.gif"));
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            MediaFileCoverArtRequest req = controller.new MediaFileCoverArtRequest(mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            controller.sendUnscaled(req, res);
            assertEquals("image/gif", res.getContentType());
            try (OutputStream s = res.getOutputStream()) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            File file = mediaFileStub.apply(new File("/MEDIAS/Metadata/coverart/unknown.gif"));
            MediaFile mediaFile = mediaFileService.getMediaFile(file);
            MediaFileCoverArtRequest req = controller.new MediaFileCoverArtRequest(mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            assertThrows(ExecutionException.class, () -> controller.sendUnscaled(req, res));
        }
    }

    @Nested
    class GetImageInputStreamTest {

        @Test
        void testWithEmbededImage() throws Exception {
            File file = createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac");
            try (InputStream s = controller.getImageInputStream(file)) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            File file = createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3");
            assertThrows(ExecutionException.class, () -> controller.getImageInputStream(file));
        }

        @Test
        void testWithImage() throws Exception {
            /*
             * There is no check that the current resource is an image. (Only the image resource URI is registered in
             * the database)
             */
            File file = createFile("/MEDIAS/Metadata/coverart/album.gif");
            assertTrue(file.exists());
            assertTrue(file.isFile());
            try (InputStream s = controller.getImageInputStream(file)) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            File file = new File("/MEDIAS/Metadata/coverart/unknown.gif");
            assertFalse(file.exists());
            assertFalse(file.isFile());
            assertThrows(ExecutionException.class, () -> controller.getImageInputStream(file));
        }
    }

    @Nested
    class GetImageInputStreamWithTypeTest {

        @Test
        void testWithEmbededImage() throws Exception {
            File file = createFile("/MEDIAS/Metadata/tagger3/tagged/test.flac");
            Pair<InputStream, String> pair = controller.getImageInputStreamWithType(file);
            assertNotNull(pair.getLeft());
            assertEquals("image/png", pair.getRight());
            pair.getLeft().close();
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            File file = createFile("/MEDIAS/Metadata/tagger3/testdata/01.mp3");
            assertThrows(ExecutionException.class, () -> controller.getImageInputStreamWithType(file));
        }

        @Test
        void testWithImage() throws Exception {
            File file = createFile("/MEDIAS/Metadata/coverart/album.gif");
            assertTrue(file.exists());
            assertTrue(file.isFile());
            Pair<InputStream, String> pair = controller.getImageInputStreamWithType(file);
            assertNotNull(pair.getLeft());
            assertEquals("image/gif", pair.getRight());
            pair.getLeft().close();
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            File file = new File("/MEDIAS/Metadata/coverart/unknown.gif");
            assertFalse(file.exists());
            assertFalse(file.isFile());
            assertThrows(ExecutionException.class, () -> controller.getImageInputStreamWithType(file));
        }
    }

    @Nested
    class GetImageInputStreamForVideoTest {

        @Test
        void testValidFile() throws URISyntaxException, IOException {
            File file = createFile("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(file.exists());
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            BufferedImage bi = controller.getImageInputStreamForVideo(mediaFile, 200, 160, 0);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(200, bi.getWidth());
            assertEquals(160, bi.getHeight());
        }

        @Test
        void testInValidFile() throws URISyntaxException, IOException {
            File file = new File("/MEDIAS/Metadata/tagger3/tagged/test.unknown.mp4");
            assertFalse(file.exists());
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(file.getPath());
            assertNull(controller.getImageInputStreamForVideo(mediaFile, 200, 160, 0));
        }
    }
}
