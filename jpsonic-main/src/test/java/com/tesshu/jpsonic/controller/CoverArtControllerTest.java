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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.controller.CoverArtController.AlbumCoverArtRequest;
import com.tesshu.jpsonic.controller.CoverArtController.ArtistCoverArtRequest;
import com.tesshu.jpsonic.controller.CoverArtController.MediaFileCoverArtRequest;
import com.tesshu.jpsonic.controller.CoverArtController.PlaylistCoverArtRequest;
import com.tesshu.jpsonic.controller.CoverArtController.PodcastCoverArtRequest;
import com.tesshu.jpsonic.controller.CoverArtController.VideoCoverArtRequest;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlaylistService;
import com.tesshu.jpsonic.service.PodcastService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.metadata.FFmpeg;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.UncheckedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    private PlaylistService playlistService;
    private FontLoader fontLoader;
    private CoverArtController controller;
    private MockMvc mockMvc;

    private static Path createPath(String resourcePath) throws URISyntaxException {
        return Path.of(CoverArtControllerTest.class.getResource(resourcePath).toURI());
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        mediaFileService = mock(MediaFileService.class);
        playlistService = mock(PlaylistService.class);
        TranscodingService transcodingService = new TranscodingService(null, null, null, null, null);
        FFmpeg ffmpeg = new FFmpeg(transcodingService);
        fontLoader = mock(FontLoader.class);
        controller = new CoverArtController(mediaFileService, ffmpeg, playlistService, mock(PodcastService.class),
                mock(ArtistDao.class), mock(AlbumDao.class), fontLoader);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    class GetTest {

        private final BiConsumer<Path, String> mediaFileStub = (path, id) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            Mockito.when(mediaFileService.getMediaFile(path.toString())).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(path);
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(path.getParent().toString());
            parent.setArtist("CoverArtControllerTest#GetTest");
            Mockito.when(mediaFileService.getParentOf(mediaFile)).thenReturn(parent);
        };

        private final BiConsumer<Path, String> videoStub = (path, id) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            mediaFile.setMediaType(MediaType.VIDEO);
            Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(path);
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(path.getParent().toString());
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
            mediaFileStub.accept(createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createPath("/MEDIAS/Metadata/tagger3/testdata/01.mp3"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createPath("/MEDIAS/Metadata/coverart/album.jpeg"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithEmptyImage() throws Exception {
            final String mediaFileId = "99";
            mediaFileStub.accept(createPath("/MEDIAS/Metadata/coverart/album.jpg"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }

        @Test
        void testWithImageCannotRead() throws Exception {

            final String id = "99";
            Path path = Path.of("/MEDIAS/Metadata/coverart/unknown.gif");

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(path);
            Mockito.when(mediaFileService.getMediaFile(Integer.parseInt(id))).thenReturn(mediaFile);
            MediaFile parent = new MediaFile();
            parent.setPathString(path.getParent().toString());
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
            videoStub.accept(createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4"), mediaFileId);
            MvcResult result = mockMvc
                    .perform(MockMvcRequestBuilders.get("/" + ViewName.COVER_ART.value())
                            .param(Attributes.Request.ID.value(), "99").param(Attributes.Request.SIZE.value(), "150"))
                    .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
            assertNotNull(result);
        }
    }

    @Nested
    class SendUnscaledTest {

        private final Function<Path, Path> mediaFileStub = (path) -> {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            Mockito.when(mediaFileService.getMediaFile(path)).thenReturn(mediaFile);
            Mockito.when(mediaFileService.getCoverArt(mediaFile)).thenReturn(path);
            return path;
        };

        @Test
        void testWithEmbededImage() throws Exception {
            Path path = mediaFileStub.apply(createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac"));
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            MediaFileCoverArtRequest req = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService,
                    mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            controller.sendUnscaled(req, res);
            assertEquals("image/png", res.getContentType());
            try (OutputStream s = res.getOutputStream()) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            Path path = mediaFileStub.apply(createPath("/MEDIAS/Metadata/tagger3/testdata/01.mp3"));
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            MediaFileCoverArtRequest req = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService,
                    mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            assertThrows(ExecutionException.class, () -> controller.sendUnscaled(req, res));
        }

        @Test
        void testWithImage() throws Exception {
            Path path = mediaFileStub.apply(createPath("/MEDIAS/Metadata/coverart/album.gif"));
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            MediaFileCoverArtRequest req = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService,
                    mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            controller.sendUnscaled(req, res);
            assertEquals("image/gif", res.getContentType());
            try (OutputStream s = res.getOutputStream()) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            Path path = mediaFileStub.apply(Path.of("/MEDIAS/Metadata/coverart/unknown.gif"));
            MediaFile mediaFile = mediaFileService.getMediaFile(path);
            MediaFileCoverArtRequest req = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService,
                    mediaFile);
            HttpServletResponse res = new MockHttpServletResponse();
            assertThrows(ExecutionException.class, () -> controller.sendUnscaled(req, res));
        }
    }

    @Nested
    class GetImageInputStreamTest {

        @Test
        void testWithEmbededImage() throws Exception {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac");
            try (InputStream s = controller.getImageInputStream(path)) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            Path path = createPath("/MEDIAS/Metadata/tagger3/testdata/01.mp3");
            assertThrows(ExecutionException.class, () -> controller.getImageInputStream(path));
        }

        @Test
        void testWithImage() throws Exception {
            /*
             * There is no check that the current resource is an image. (Only the image resource URI is registered in
             * the database)
             */
            Path path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertTrue(Files.exists(path));
            assertFalse(Files.isDirectory(path));
            try (InputStream s = controller.getImageInputStream(path)) {
                assertNotNull(s);
            }
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            Path path = Path.of("/MEDIAS/Metadata/coverart/unknown.gif");
            assertFalse(Files.exists(path));
            assertFalse(Files.isDirectory(path));
            assertThrows(ExecutionException.class, () -> controller.getImageInputStream(path));
        }
    }

    @Nested
    class GetImageInputStreamWithTypeTest {

        @Test
        void testWithEmbededImage() throws Exception {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.flac");
            Pair<InputStream, String> pair = controller.getImageInputStreamWithType(path);
            assertNotNull(pair.getLeft());
            assertEquals("image/png", pair.getRight());
            pair.getLeft().close();
        }

        @Test
        void testWithoutEmbededImage() throws Exception {
            Path path = createPath("/MEDIAS/Metadata/tagger3/testdata/01.mp3");
            assertThrows(ExecutionException.class, () -> controller.getImageInputStreamWithType(path));
        }

        @Test
        void testWithImage() throws Exception {
            Path path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertTrue(Files.exists(path));
            assertFalse(Files.isDirectory(path));
            Pair<InputStream, String> pair = controller.getImageInputStreamWithType(path);
            assertNotNull(pair.getLeft());
            assertEquals("image/gif", pair.getRight());
            pair.getLeft().close();
        }

        @Test
        void testWithImageCannotRead() throws Exception {
            Path path = Path.of("/MEDIAS/Metadata/coverart/unknown.gif");
            assertFalse(Files.exists(path));
            assertFalse(Files.isDirectory(path));
            assertThrows(ExecutionException.class, () -> controller.getImageInputStreamWithType(path));
        }
    }

    @Nested
    class GetImageInputStreamForVideoTest {

        @Test
        void testValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            BufferedImage bi = controller.getImageInputStreamForVideo(mediaFile, 200, 160, 0);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(200, bi.getWidth());
            assertEquals(160, bi.getHeight());
        }

        @Test
        void testInValidFile() throws URISyntaxException, IOException {
            Path path = Path.of("/MEDIAS/Metadata/tagger3/tagged/test.unknown.mp4");
            assertFalse(Files.exists(path));
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path.toString());
            assertNull(controller.getImageInputStreamForVideo(mediaFile, 200, 160, 0));
        }
    }

    @Nested
    class ArtistCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {

            Artist artist = new Artist();
            Instant lastScanned = now();
            artist.setLastScanned(lastScanned);

            ArtistCoverArtRequest request = new ArtistCoverArtRequest(controller, fontLoader, artist);
            assertEquals(lastScanned.toEpochMilli(), request.lastModified());

            Path path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            artist.setCoverArtPath(path.toString());
            request = new ArtistCoverArtRequest(controller, fontLoader, artist);
            assertEquals(Files.getLastModifiedTime(path).toMillis(), request.lastModified());
        }
    }

    @Nested
    class AlbumCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {

            Album album = new Album();
            Instant lastScanned = now();
            album.setLastScanned(lastScanned);

            AlbumCoverArtRequest request = new AlbumCoverArtRequest(controller, fontLoader, album);
            assertEquals(lastScanned.toEpochMilli(), request.lastModified());

            Path path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            album.setCoverArtPath(path.toString());
            request = new AlbumCoverArtRequest(controller, fontLoader, album);
            assertEquals(Files.getLastModifiedTime(path).toMillis(), request.lastModified());
        }
    }

    @Nested
    class PlaylistCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {

            Playlist playlist = new Playlist();
            Instant changed = now();
            playlist.setChanged(changed);

            PlaylistCoverArtRequest request = new PlaylistCoverArtRequest(controller, fontLoader, mediaFileService,
                    playlistService, playlist);
            assertEquals(changed.toEpochMilli(), request.lastModified());
        }
    }

    @Nested
    class PodcastCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {
            PodcastChannel channel = new PodcastChannel("");
            PodcastCoverArtRequest request = new PodcastCoverArtRequest(controller, fontLoader, channel);
            assertEquals(-1, request.lastModified());
        }
    }

    @Nested
    class MediaFileCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {
            MediaFile album = new MediaFile();
            album.setMediaType(MediaType.ALBUM);
            assertTrue(album.isDirectory());
            Instant changed = now();
            album.setChanged(changed);
            MediaFileCoverArtRequest request = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService,
                    album);
            assertEquals(changed.toEpochMilli(), request.lastModified());

            MediaFile song = new MediaFile();
            song.setMediaType(MediaType.MUSIC);
            Path songCoverArtPath = createPath("/MEDIAS/Metadata/coverart/cover.gif");
            Mockito.when(mediaFileService.getCoverArt(song)).thenReturn(songCoverArtPath);
            request = new MediaFileCoverArtRequest(controller, fontLoader, mediaFileService, song);
            assertEquals(Files.getLastModifiedTime(songCoverArtPath).toMillis(), request.lastModified());
        }
    }

    @Nested
    class VideoCoverArtRequestTest {

        @Test
        void testLastModified() throws URISyntaxException, IOException {
            MediaFile video = new MediaFile();
            video.setMediaType(MediaType.ALBUM);
            assertTrue(video.isDirectory());
            Instant changed = now();
            video.setChanged(changed);
            VideoCoverArtRequest request = new VideoCoverArtRequest(controller, fontLoader, video, 0);
            assertEquals(changed.toEpochMilli(), request.lastModified());
        }
    }

    @Nested
    class GetImageCacheDirectoryTest {

        private final CoverArtController controller = new CoverArtController(mediaFileService, null, null, null, null,
                null, null);

        @Test
        void testGetImageCacheDirectory(@TempDir Path tmp) {
            final String home = System.getProperty("jpsonic.home");
            System.setProperty("jpsonic.home", tmp.toString());

            Path path = controller.getImageCacheDirectory(150);
            assertNotNull(path);

            System.setProperty("jpsonic.home", home);
        }

        abstract class IntRunnable implements Callable<Path> {
            int i;

            public IntRunnable(int i) {
                this.i = i;
            }
        }

        @Test
        @SuppressWarnings("PMD.UnusedLocalVariable")
        void testLock() throws Exception {

            int threadsCount = 1_000;

            final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setWaitForTasksToCompleteOnShutdown(true); // To handle Stream
            executor.setAwaitTerminationMillis(1_000);
            executor.setQueueCapacity(threadsCount);
            executor.setCorePoolSize(threadsCount);
            executor.setMaxPoolSize(threadsCount);
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.setDaemon(true);
            executor.initialize();

            MetricRegistry metrics = new MetricRegistry();
            List<Future<Path>> futures = new ArrayList<>();
            Timer globalTimer = metrics
                    .timer(MetricRegistry.name(FontLoaderTest.class, "SpeedOfGetImageCacheDirectory"));

            for (int i = 0; i < threadsCount; i++) {
                futures.add(executor.submit(new IntRunnable(i) {
                    @Override
                    public Path call() {
                        Path path;
                        try (Timer.Context globalTimerContext = globalTimer.time()) {
                            path = controller.getImageCacheDirectory(i % 10);
                        } catch (IllegalArgumentException e) {
                            throw new UncheckedException(e);
                        }
                        return path;
                    }
                }));
            }

            assertEquals(threadsCount, futures.stream().mapToInt(future -> {
                try {
                    assertNotNull(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new UncheckedException(e);
                }
                return 1;
            }).sum());
            executor.shutdown();

            ConsoleReporter.Builder builder = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS);
            try (ConsoleReporter reporter = builder.build()) {
                // to be none
            }
        }
    }

}
