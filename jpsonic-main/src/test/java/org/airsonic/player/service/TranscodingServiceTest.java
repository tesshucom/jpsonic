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
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.airsonic.player.Integration;
import org.airsonic.player.MusicFolderTestDataUtils;
import org.airsonic.player.NeedsHome;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TranscodeScheme;
import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.domain.VideoTranscodingSettings;
import org.airsonic.player.io.TranscodeInputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.TranscodingService.Parameters;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/*
 * This test class is a white-box. The goal is to refactor logic or add new logic while ensuring
 * that the logic remains as it is.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // false positive for lambda
@SpringBootTest
@ExtendWith(NeedsHome.class)
public class TranscodingServiceTest {

    private String fmtMp3 = "mp3";
    private String fmtFlac = "flac";
    private String fmtRmf = "rmf";
    private String fmtWav = "wav";
    private String fmtMpeg = "mpeg";
    private String fakePath = "*fake-path*";
    private String realPath = MusicFolderTestDataUtils.resolveMusicFolderPath()
            + "/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]"
            + "/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac";
    private String fmtFlv = "flv";
    private String transNameMp3 = "mp3 audio";
    private String transNameFlv = "flv/h264 video";

    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private PlayerService playerService;

    @Test
    public void testgetTranscodingsForPlayer() {
        int playerId = 9999;
        Player player = new Player();
        player.setId(playerId);
        playerService.createPlayer(player);

        assertTrue(transcodingService.getTranscodingsForPlayer(player).stream()
                .anyMatch(t -> transNameMp3.equals(t.getName())));

        playerService.removePlayerById(playerId);
    }

    @Integration
    @Test
    public void testCRUDTranscoding() {

        // create player
        int playerId = 9999;
        Player player = new Player();
        player.setId(playerId);
        playerService.createPlayer(player);

        // create active
        String activeName = "test-transcodingActive";
        final Transcoding transcodingActive = new Transcoding(null, activeName, fmtMp3, fmtWav, "step1", null, null,
                true);
        transcodingService.createTranscoding(transcodingActive);

        // create noActive
        String noActiveName = "test-transcodingNoActive";
        Transcoding noActive = new Transcoding(null, noActiveName, fmtMp3, fmtWav, "step1", null, null, false);
        transcodingService.createTranscoding(noActive);

        // active for player
        transcodingService.getTranscodingsForPlayer(player).stream().filter(t -> activeName.equals(t.getName()))
                .findFirst().ifPresentOrElse(t -> assertEquals(transcodingActive, t), () -> fail());

        // noActive for player
        transcodingService.getTranscodingsForPlayer(player).stream().filter(t -> noActiveName.equals(t.getName()))
                .findFirst().ifPresent((t) -> fail());

        // update and delete
        noActive.setDefaultActive(true);
        transcodingService.updateTranscoding(noActive);
        transcodingService.getAllTranscodings().stream().filter(t -> noActiveName.equals(t.getName())).findFirst()
                .ifPresentOrElse(t -> {
                    assertTrue(t.isDefaultActive());
                    transcodingService.deleteTranscoding(t.getId());
                }, () -> fail());
        transcodingService.getAllTranscodings().stream().filter(t -> noActiveName.equals(t.getName())).findFirst()
                .ifPresent(t -> fail());

        // clean
        transcodingService.getAllTranscodings().stream().filter(t -> activeName.equals(t.getName())).findFirst()
                .ifPresent(t -> transcodingService.deleteTranscoding(t.getId()));
        playerService.removePlayerById(playerId);
    }

    @Test
    public void testIsTranscodingRequired() {

        int playerId = 9999;
        Player player = new Player();
        player.setId(playerId);
        playerService.createPlayer(player);

        /*
         * If you want to disable these transcodings, you need to either rewrite the default transcoding settings or
         * turn off transcoding.
         */
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFormat(fmtMp3);
        assertTrue(transcodingService.isTranscodingRequired(mediaFile, player));
        mediaFile.setFormat(fmtFlac);
        assertTrue(transcodingService.isTranscodingRequired(mediaFile, player));

        /*
         * Non-transcoded formats are not transcoded.
         */
        mediaFile.setFormat("rmf");
        assertFalse(transcodingService.isTranscodingRequired(mediaFile, player));

        playerService.removePlayerById(playerId);

    }

    @Test
    public void testGetSuffix() {

        int playerId = 9999;
        Player player = new Player();
        player.setId(playerId);
        playerService.createPlayer(player);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFormat(fmtMp3);
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, null));

        mediaFile.setFormat(fmtFlac);
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, fmtFlac));

        mediaFile.setFormat(fmtRmf);
        assertEquals(fmtRmf, transcodingService.getSuffix(player, mediaFile, fmtMp3));

        playerService.removePlayerById(playerId);
    }

    /*
     * Only on Windows, there are the case of running file copy and temporary file creation failure. There are also
     * cases where process execution fails. These are difficult to reproduce in UT, but are omitted because they are
     * actually rare cases.
     */
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class GetTranscodedInputStream {

        private String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -";

        @Test
        @Order(1)
        public void testGTI1() {
            // However, this is a case that is not considered in terms of implementation.
            Assertions.assertThrows(NullPointerException.class,
                    () -> transcodingService.getTranscodedInputStream(null));
        }

        /*
         * The behavior of this case on Windows is very doubtful.
         */
        @Test
        @Order(2)
        public void testGTI2() throws IOException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            Parameters parameters = new Parameters(mediaFile, null);
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step1, null, null, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = transcodingService.getTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            }
        }

        @Test
        @Order(3)
        @EnabledOnOs(OS.WINDOWS)
        public void testGTI3Win() throws IOException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            Parameters parameters = new Parameters(mediaFile, null);

            // Because * is included in fakePath
            Assertions.assertThrows(InvalidPathException.class,
                    () -> transcodingService.getTranscodedInputStream(parameters));
        }

        @Test
        @Order(3)
        @EnabledOnOs(OS.LINUX)
        public void testGTI3Linux() throws IOException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            Parameters parameters = new Parameters(mediaFile, null);

            Assertions.assertThrows(NoSuchFileException.class,
                    () -> transcodingService.getTranscodedInputStream(parameters));
        }

        @Test
        @Order(4)
        public void testGTI4() throws IOException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(realPath);
            Parameters parameters = new Parameters(mediaFile, null);

            try (InputStream stream = transcodingService.getTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            }
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class CreateTranscodedInputStream {

        private String step = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -f flv -";

        private Method ctisMethod;

        @BeforeEach
        public void before() throws ExecutionException {
            if (ctisMethod != null) {
                return;
            }
            try {
                ctisMethod = transcodingService.getClass().getDeclaredMethod("createTranscodedInputStream",
                        Parameters.class);
                ctisMethod.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
        }

        private InputStream doCreateTranscodedInputStream(@NonNull Parameters parameters) throws ExecutionException {
            try {
                return (InputStream) ctisMethod.invoke(transcodingService, parameters);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        private Parameters createParam() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setTitle("title");
            mediaFile.setAlbumName("album");
            mediaFile.setArtist("artist");
            mediaFile.setPath(fakePath);
            Parameters parameters = new Parameters(mediaFile, new VideoTranscodingSettings(640, 480, 0, 120, false));
            parameters.setExpectedLength(null);
            return parameters;
        }

        @Test
        @Order(1)
        public void testCTI1() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, null, null, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = doCreateTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(2)
        public void testCTI2() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, null, step, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = doCreateTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(3)
        public void testCTI3() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, step, null, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = doCreateTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(4)
        public void testCTI4() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, step, step, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = doCreateTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class CreateTranscodeInputStream {

        private Method ctisMethod;

        private String command = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 22050 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";

        @BeforeEach
        public void before() throws ExecutionException {
            if (ctisMethod != null) {
                return;
            }
            try {
                ctisMethod = transcodingService.getClass().getDeclaredMethod("createTranscodeInputStream", String.class,
                        Integer.class, VideoTranscodingSettings.class, MediaFile.class, InputStream.class);
                ctisMethod.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
        }

        private TranscodeInputStream doCreateTranscodeInputStream(@NonNull String command, Integer maxBitRate,
                VideoTranscodingSettings videoTranscodingSettings, @NonNull MediaFile mediaFile, InputStream in)
                throws ExecutionException {
            try {
                return (TranscodeInputStream) ctisMethod.invoke(transcodingService, command, maxBitRate,
                        videoTranscodingSettings, mediaFile, in);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(1)
        public void testCTI1() throws ExecutionException {
            Integer maxBitRate = null;
            VideoTranscodingSettings videoTranscodingSettings = null;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(realPath);
            InputStream in = null;

            try (TranscodeInputStream stream = doCreateTranscodeInputStream(command, maxBitRate,
                    videoTranscodingSettings, mediaFile, in);) {
                Assertions.assertNotNull(stream);
            }
        }

        @Test
        @Order(2)
        public void testCTI2() throws ExecutionException {
            Integer maxBitRate = null;
            VideoTranscodingSettings v = new VideoTranscodingSettings(640, 480, 0, 120, false);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setTitle("Title");
            mediaFile.setAlbumName("Album");
            mediaFile.setArtist("Artist");
            mediaFile.setPath(realPath);
            InputStream in = null;

            try (TranscodeInputStream stream = doCreateTranscodeInputStream(command, maxBitRate, v, mediaFile, in);) {
                Assertions.assertNotNull(stream);
            }
        }
    }

    @Nested
    public class GetTranscoding {

        private Method getTranscodingMethod;

        @BeforeEach
        public void before() throws ExecutionException {
            try {
                getTranscodingMethod = transcodingService.getClass().getDeclaredMethod("getTranscoding",
                        MediaFile.class, Player.class, String.class, boolean.class);
                getTranscodingMethod.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
        }

        private Transcoding doGetTranscoding(MediaFile mediaFile, Player player, String preferredTargetFormat,
                boolean hls) throws ExecutionException {
            try {
                return (Transcoding) getTranscodingMethod.invoke(transcodingService, mediaFile, player,
                        preferredTargetFormat, hls);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        public void testRaw() throws ExecutionException {
            MediaFile mediaFile = null;
            Player player = null;
            String preferredTargetFormat = "raw";
            boolean hls = false;

            Assertions.assertNull(doGetTranscoding(mediaFile, player, preferredTargetFormat, hls));
        }

        @Test
        public void testHls() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat("format");
            Player player = null;
            String preferredTargetFormat = null;
            boolean hls = true;

            Transcoding t = doGetTranscoding(mediaFile, player, preferredTargetFormat, hls);
            assertEquals("hls", t.getName());
        }

        @Test
        public void testPreferred() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);
            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            String preferredTargetFormat = fmtMp3;
            boolean hls = false;

            Transcoding transcoding = doGetTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(transNameMp3, transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtFlac.equals(f)));
            assertEquals(fmtMp3, transcoding.getTargetFormat());

            playerService.removePlayerById(playerId);
        }

        @Test
        public void testNoPreferred() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);
            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            String preferredTargetFormat = null;
            boolean hls = false;

            Transcoding transcoding = doGetTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(transNameMp3, transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtFlac.equals(f)));
            assertEquals(fmtMp3, transcoding.getTargetFormat());

            playerService.removePlayerById(playerId);
        }

        @Test
        public void testNotApplicable() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat("svg");
            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            String preferredTargetFormat = null;
            boolean hls = false;

            Assertions.assertNull(doGetTranscoding(mediaFile, player, preferredTargetFormat, hls));

            playerService.removePlayerById(playerId);
        }

        @Test
        public void testVideo() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);
            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            String preferredTargetFormat = null;
            boolean hls = false;

            Transcoding transcoding = doGetTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(transNameFlv, transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtMpeg.equals(f)));
            assertEquals(fmtFlv, transcoding.getTargetFormat());

            playerService.removePlayerById(playerId);
        }

        @Test
        public void testVideoTargetMatch() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtFlv);
            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            String preferredTargetFormat = fmtFlv;
            boolean hls = false;

            Transcoding transcoding = doGetTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(transNameFlv, transcoding.getName());
            assertFalse(Arrays.stream(transcoding.getSourceFormatsAsArray()).allMatch(f -> fmtFlv.equals(f)));
            assertEquals(fmtFlv, transcoding.getTargetFormat());

            playerService.removePlayerById(playerId);
        }

    }

    @Test
    public void testIsTranscodingSupported() {

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFormat(fmtMp3);
        assertTrue(transcodingService.isTranscodingSupported(mediaFile));
        mediaFile.setFormat(fmtFlac);
        assertTrue(transcodingService.isTranscodingSupported(mediaFile));

        /*
         * There are some isTranscodingSupported (null) in the legacy code. It smells...
         */
        assertTrue(transcodingService.isTranscodingSupported(null));

        mediaFile.setFormat("rmf");
        assertFalse(transcodingService.isTranscodingSupported(mediaFile));

    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class TranscoderInstalled {

        private Method transcoderInstalledMethod;
        private String ffmpeg = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
        private String lame = "lame -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";

        @BeforeEach
        public void before() throws ExecutionException {
            if (transcoderInstalledMethod != null) {
                return;
            }
            try {
                transcoderInstalledMethod = transcodingService.getClass().getDeclaredMethod("isTranscoderInstalled",
                        Transcoding.class);
                transcoderInstalledMethod.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
        }

        private boolean doIsTranscoderInstalled(@NonNull Transcoding transcoding) throws ExecutionException {
            try {
                return (Boolean) transcoderInstalledMethod.invoke(transcodingService, transcoding);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(1)
        public void testITI1() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, lame, lame, lame, true);

            assertFalse(doIsTranscoderInstalled(transcoding));
        }

        @Test
        @Order(2)
        public void testITI2() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, lame, lame, true);

            assertFalse(doIsTranscoderInstalled(transcoding));
        }

        @Test
        @Order(3)
        public void testITI3() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, lame, true);

            assertFalse(doIsTranscoderInstalled(transcoding));
        }

        @Test
        @Order(4)
        public void testITI4() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, ffmpeg, true);

            assertTrue(doIsTranscoderInstalled(transcoding));
        }

        @Test
        @Order(5)
        public void testITI5() throws ExecutionException {
            File f = transcodingService.getTranscodeDirectory();
            transcodingService.setTranscodeDirectory(new File(fakePath));
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, ffmpeg, true);

            assertFalse(doIsTranscoderInstalled(transcoding));

            transcodingService.setTranscodeDirectory(f);
        }

        @Test
        @Order(6)
        public void testITI6() throws ExecutionException {
            Field pathField;
            String transcodePath;
            try {
                pathField = transcodingService.getClass().getDeclaredField("transcodePath");
                pathField.setAccessible(true);
                transcodePath = Optional.ofNullable((String) pathField.get(transcodingService)).orElse(null);
                pathField.set(transcodingService, null);
                transcodingService.setTranscodeDirectory(null);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new ExecutionException(e);
            }

            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, ffmpeg, true);

            assertFalse(doIsTranscoderInstalled(transcoding));

            try {
                pathField.set(transcodingService, null);
                transcodingService.setTranscodeDirectory(null);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new ExecutionException(e);
            }

            Assertions.assertNotNull(transcodingService.getTranscodeDirectory());

            try {
                pathField.set(transcodingService, transcodePath);
                transcodingService.setTranscodeDirectory(null);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Documented
    private @interface IsNeedTranscoding {
        @interface Conditions {
            @interface Transcoding {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface MaxBitRate {
                @interface Zero {
                }

                @interface GtZero {
                }
            }

            @interface BitRate {
                @interface GtZero {
                }

                @interface GtMaxBitRate {
                }

                @interface LtMaxBitRate {
                }
            }

            @interface PreferredTargetFormat {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface MediaFile {
                @interface FormatEqTargetFormat {
                }

                @interface FormatNeTargetFormat {
                }
            }
        }

        @interface Result {
            @interface FALSE {
            }

            @interface TRUE {
            }
        }
    }

    @Documented
    private @interface GetParametersDecision {
        @interface Conditions {

            @interface MediaFile {
                @interface MediaType {
                    @interface Video {
                    }
                }

                @interface Format {
                    @interface NotNull {
                    }
                }
            }

            @interface Player {
                @interface Username {
                    @interface Anonymous {
                    }
                }
            }

            @interface MaxBitRate {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface PreferredTargetFormat {
                @interface Null {
                }
            }

            @interface VideoTranscodingSettings {
                @interface Null {
                }

                @interface Hls {
                    @interface False {
                    }

                    @interface True {
                    }
                }
            }
        }
    }

    @Documented
    private @interface CreateMaxBitrate {
        @interface Conditions {
            @interface Mb {
                @interface Zero {
                }

                @interface NeZero {
                }
            }

            @interface BitRate {
                @interface Zero {
                }

                @interface NeZero {
                }

                @interface LtMb {
                }

                @interface GtMb {
                }
            }
        }

        @interface Result {
            @interface BitRate {
            }

            @interface Mb {
            }
        }

    }

    @Documented
    private @interface GetExpectedLength {

        @interface Conditions {
            @interface Parameters {
                @interface Transcode {
                    @interface True {
                    }

                    @interface False {
                    }
                }

                @interface MaxBitRate {
                    @interface Null {

                    }

                    @interface NotNull {

                    }
                }

                @interface MediaFile {
                    @interface DurationSeconds {
                        @interface Null {

                        }

                        @interface NotNull {

                        }
                    }
                }
            }
        }

        @interface Result {
            @interface Null {
            }

            @interface MediaFile {
                @interface FileSize {

                }

            }

            @interface Estimates {

            }
        }

    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class GetParameters {

        private final Transcoding fakeTranscoding = new Transcoding(null, "fake-instance", fmtFlac, fmtMp3, "s1", "s2",
                "s3", true);

        private Method needTranscoding;
        private Method createBitrate;
        private Method createMaxBitrate;
        private Method rangeAllowed;
        private Method getExpectedLength;

        @BeforeEach
        public void before() throws ExecutionException {
            if (needTranscoding != null && createBitrate != null && createMaxBitrate != null && rangeAllowed != null
                    && getExpectedLength != null) {
                return;
            }
            try {
                needTranscoding = transcodingService.getClass().getDeclaredMethod("isNeedTranscoding",
                        Transcoding.class, int.class, int.class, String.class, MediaFile.class);
                needTranscoding.setAccessible(true);
                createBitrate = transcodingService.getClass().getDeclaredMethod("createBitrate", MediaFile.class);
                createBitrate.setAccessible(true);
                createMaxBitrate = transcodingService.getClass().getDeclaredMethod("createMaxBitrate",
                        TranscodeScheme.class, MediaFile.class, int.class);
                createMaxBitrate.setAccessible(true);
                rangeAllowed = transcodingService.getClass().getDeclaredMethod("isRangeAllowed", Parameters.class);
                rangeAllowed.setAccessible(true);
                getExpectedLength = transcodingService.getClass().getDeclaredMethod("getExpectedLength",
                        Parameters.class);
                getExpectedLength.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ExecutionException(e);
            }
        }

        @GetParametersDecision.Conditions.MaxBitRate.Null
        @GetParametersDecision.Conditions.PreferredTargetFormat.Null
        @GetParametersDecision.Conditions.VideoTranscodingSettings.Null
        @Test
        @Order(1)
        public void testGP1() {

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);

            Integer maxBitRate = null;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = null;

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            Assertions.assertNull(parameters.getExpectedLength());
            Assertions.assertNull(parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            Assertions.assertNull(parameters.getTranscoding());
            Assertions.assertNull(parameters.getVideoTranscodingSettings());
            assertTrue(parameters.isRangeAllowed());

            playerService.removePlayerById(playerId);
        }

        @GetParametersDecision.Conditions.MediaFile.Format.NotNull
        @GetParametersDecision.Conditions.MaxBitRate.NotNull
        @Test
        @Order(2)
        public void testGP2() {

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setFormat(fmtFlac);

            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);
            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = null;

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            Assertions.assertNull(parameters.getExpectedLength());
            assertEquals(224, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(transNameMp3, parameters.getTranscoding().getName());
            Assertions.assertNull(parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(playerId);
        }

        @GetParametersDecision.Conditions.MediaFile.MediaType.Video
        @GetParametersDecision.Conditions.MediaFile.Format.NotNull
        @Test
        @Order(3)
        public void testGP3() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);

            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = null;

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            Assertions.assertNull(parameters.getExpectedLength());
            assertEquals(2000, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(transNameFlv, parameters.getTranscoding().getName());
            Assertions.assertNull(parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(playerId);
        }

        @GetParametersDecision.Conditions.Player.Username.Anonymous
        @GetParametersDecision.Conditions.VideoTranscodingSettings.Hls.False
        @Test
        @Order(4)
        public void testGP4() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setFormat(fmtFlac);

            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            player.setUsername(JWTAuthenticationToken.USERNAME_ANONYMOUS);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = new VideoTranscodingSettings(640, 480, 0, 120, false);

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            Assertions.assertNull(parameters.getExpectedLength());
            assertEquals(224, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(transNameMp3, parameters.getTranscoding().getName());
            assertEquals(videoTranscodingSettings, parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(playerId);
        }

        @GetParametersDecision.Conditions.VideoTranscodingSettings.Hls.True
        @Test
        @Order(5)
        public void testGP5() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);

            int playerId = 9999;
            Player player = new Player();
            player.setId(playerId);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = new VideoTranscodingSettings(640, 480, 0, 120, true);

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            Assertions.assertNull(parameters.getExpectedLength());
            assertEquals(2000, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals("hls", parameters.getTranscoding().getName());
            assertEquals(640, parameters.getVideoTranscodingSettings().getWidth());
            assertEquals(480, parameters.getVideoTranscodingSettings().getHeight());
            assertEquals(0, parameters.getVideoTranscodingSettings().getTimeOffset());
            assertEquals(120, parameters.getVideoTranscodingSettings().getDuration());
            assertTrue(parameters.getVideoTranscodingSettings().isHls());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(playerId);
        }

        private boolean doIsNeedTranscoding(@Nullable Transcoding transcoding, int mb, int bitRate,
                @Nullable String preferredTargetFormat, @NonNull MediaFile mediaFile) throws ExecutionException {
            try {
                return (Boolean) needTranscoding.invoke(transcodingService, transcoding, mb, bitRate,
                        preferredTargetFormat, mediaFile);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @IsNeedTranscoding.Conditions.Transcoding.Null
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(11)
        public void testINT1() throws ExecutionException {
            Transcoding transcoding = null;
            int mb = 0;
            int bitRate = 0;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(doIsNeedTranscoding(transcoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.Zero
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(12)
        public void testINT2() throws ExecutionException {
            int mb = 0;
            int bitRate = 0;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(doIsNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.LtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.Null
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(13)
        public void testINT3() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(doIsNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.LtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.NotNull
        @IsNeedTranscoding.Conditions.MediaFile.FormatEqTargetFormat
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(14)
        public void testINT4() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = fmtMp3;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtMp3);

            assertFalse(doIsNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.Null
        @IsNeedTranscoding.Result.TRUE
        @Test
        @Order(15)
        public void testINT5() throws ExecutionException {
            int mb = 2;
            int bitRate = 4;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertTrue(doIsNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.LtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.NotNull
        @IsNeedTranscoding.Conditions.MediaFile.FormatNeTargetFormat
        @IsNeedTranscoding.Result.TRUE
        @Test
        @Order(16)
        public void testINT6() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = fmtMp3;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);

            assertTrue(doIsNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        private int doCreateBitrate(@NonNull MediaFile mediaFile) throws ExecutionException {
            try {
                return (Integer) createBitrate.invoke(transcodingService, mediaFile);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(21)
        public void testCB1() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setBitRate(128);

            assertEquals(128, doCreateBitrate(mediaFile));
        }

        @Test
        @Order(22)
        public void testCB2() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setBitRate(1024);

            assertEquals(1024, doCreateBitrate(mediaFile));
        }

        @Test
        @Order(23)
        public void testCB3() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(940);
            mediaFile.setVariableBitRate(false);

            assertEquals(940, doCreateBitrate(mediaFile));
        }

        @Test
        @Order(24)
        public void testCB4() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(940);
            mediaFile.setVariableBitRate(true);

            assertEquals(1128, doCreateBitrate(mediaFile));
        }

        @Test
        @Order(25)
        public void testCB5() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(128);
            mediaFile.setVariableBitRate(true);

            assertEquals(160, doCreateBitrate(mediaFile));
        }

        private int doCreateMaxBitrate(@NonNull TranscodeScheme transcodeScheme, @NonNull MediaFile mediaFile,
                int bitRate) throws ExecutionException {
            try {
                return (Integer) createMaxBitrate.invoke(transcodingService, transcodeScheme, mediaFile, bitRate);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @CreateMaxBitrate.Conditions.Mb.Zero
        @CreateMaxBitrate.Result.BitRate
        @Test
        @Order(31)
        public void testCMB1() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.OFF;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 0;

            assertEquals(0, doCreateMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.NeZero
        @CreateMaxBitrate.Conditions.BitRate.LtMb
        @CreateMaxBitrate.Result.BitRate
        @Test
        @Order(32)
        public void testCMB2() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_320;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 256;

            assertEquals(256, doCreateMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.NeZero
        @CreateMaxBitrate.Conditions.BitRate.GtMb
        @CreateMaxBitrate.Result.Mb
        @Test
        @Order(33)
        public void testCMB3() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_256;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 320;

            assertEquals(256, doCreateMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.Zero
        @CreateMaxBitrate.Result.Mb
        @Test
        @Order(34)
        public void testCMB4() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_256;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 0;

            assertEquals(256, doCreateMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        private boolean doIsRangeAllowed(@NonNull Parameters parameters) throws ExecutionException {
            try {
                return (Boolean) rangeAllowed.invoke(transcodingService, parameters);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(41)
        public void testIRA1() throws ExecutionException {
            Parameters parameters = new Parameters(null, null);

            assertTrue(doIsRangeAllowed(parameters));
        }

        @Test
        @Order(42)
        public void testIRA2() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(null);
            assertFalse(doIsRangeAllowed(parameters));
        }

        @Test
        @Order(43)
        public void testIRA3() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "not contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertFalse(doIsRangeAllowed(parameters));
        }

        @Test
        @Order(44)
        public void testIRA4() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertTrue(doIsRangeAllowed(parameters));
        }

        @Test
        @Order(45)
        public void testIRA5() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, null, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertFalse(doIsRangeAllowed(parameters));
        }

        private Long doGetExpectedLength(@NonNull Parameters parameters) throws ExecutionException {
            try {
                return (Long) getExpectedLength.invoke(transcodingService, parameters);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ExecutionException(e);
            }
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.False
        @GetExpectedLength.Result.MediaFile.FileSize
        @Test
        @Order(51)
        public void testGEL1() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileSize(123L);
            Parameters parameters = new Parameters(mediaFile, null);
            assertFalse(parameters.isTranscode());

            assertEquals(123L, doGetExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.Null
        @GetExpectedLength.Result.Null
        @Test
        @Order(52)
        public void testGEL2() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());

            Assertions.assertNull(doGetExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.NotNull
        @GetExpectedLength.Conditions.Parameters.MaxBitRate.Null
        @GetExpectedLength.Result.Null
        @Test
        @Order(53)
        public void testGEL3() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setDurationSeconds(120);
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());

            Assertions.assertNull(doGetExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.NotNull
        @GetExpectedLength.Conditions.Parameters.MaxBitRate.NotNull
        @GetExpectedLength.Result.Estimates
        @Test
        @Order(54)
        public void testGEL4() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setDurationSeconds(120);
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());
            parameters.setMaxBitRate(256);

            assertEquals(3_904_000, doGetExpectedLength(parameters));
        }

    }
}
