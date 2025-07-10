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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.metadata;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FFmpegTest {

    private FFmpeg ffmpeg;

    @BeforeEach
    void setUp() {
        TranscodingService transcodingService = new TranscodingService(mock(SettingsService.class),
                null, null, null, null);
        ffmpeg = new FFmpeg(transcodingService);
    }

    private Path createPath(String path) throws URISyntaxException, IOException {
        return Path.of(FFmpegTest.class.getResource(path).toURI());
    }

    @Test
    @Order(1)
    void testGetVersion() throws URISyntaxException, IOException {
        String version = ffmpeg.getVersion();
        assertNotEquals(StringUtils.EMPTY, version);
    }

    @Nested
    class CreateImageTest {

        @Test
        @Order(1)
        void testBlank() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/blank/blank.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 200, 160, 0);
            assertNull(bi);
        }

        @Test
        @Order(2)
        void testInvalidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/testdata/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 200, 160, 0);
            assertNull(bi);
        }

        @Test
        @Order(3)
        void testThumbnailWithValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 200, 160, 0);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(200, bi.getWidth());
            assertEquals(160, bi.getHeight());
        }

        @Test
        @Order(4)
        void testSeekedFrameWithValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 200, 160, 1);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(200, bi.getWidth());
            assertEquals(160, bi.getHeight());
        }

        @Test
        @Order(5)
        void testOverFrameWithValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 200, 160, Integer.MAX_VALUE);
            assertNull(bi);
        }

        @Test
        @Order(6)
        void testZeroSizeWithValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, 0, 0, 0);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(1920, bi.getWidth()); // original
            assertEquals(1080, bi.getHeight()); // original
        }

        @Test
        @Order(6)
        void testNegativeWithValidFile() throws URISyntaxException, IOException {
            Path path = createPath("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
            assertTrue(Files.exists(path));
            BufferedImage bi = ffmpeg.createImage(path, -1, -1, -1);
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, bi.getType());
            assertEquals(1920, bi.getWidth()); // original
            assertEquals(1080, bi.getHeight()); // original
        }
    }
}
