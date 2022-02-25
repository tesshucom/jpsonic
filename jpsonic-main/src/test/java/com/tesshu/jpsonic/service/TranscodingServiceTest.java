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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.MusicFolderTestDataUtils;
import com.tesshu.jpsonic.dao.PlayerDao;
import com.tesshu.jpsonic.dao.TranscodingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import com.tesshu.jpsonic.domain.Transcodings;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.domain.VideoTranscodingSettings;
import com.tesshu.jpsonic.io.TranscodeInputStream;
import com.tesshu.jpsonic.security.JWTAuthenticationToken;
import com.tesshu.jpsonic.service.TranscodingService.Parameters;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/*
 * This test class is a white-box. The goal is to refactor logic or add new logic while ensuring
 * that the logic remains as it is.
 */
@SuppressWarnings({ "PMD.JUnitTestsShouldIncludeAssert", "PMD.DoNotUseThreads" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TranscodingServiceTest {

    private String fmtRaw = "raw";
    private String fmtMp3 = "mp3";
    private String fmtFlac = "flac";
    private String fmtRmf = "rmf";
    private String fmtWav = "wav";
    private String fmtMpeg = "mpeg";
    private String fmtFlv = "flv";
    private String fmtHls = "hls";
    private String fmtMp4 = "mp4";

    private int mockPlayerId = 10;
    private Transcoding inactiveTranscoding = new Transcoding(10, "aac",
            "mp3 ogg oga m4a flac wav wma aif aiff ape mpc shn", "aac", "ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -",
            null, null, false);
    private String fakePath = "*fake-path*";
    private String realPath = MusicFolderTestDataUtils.resolveMusicFolderPath()
            + "/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]"
            + "/01 - Bach- Goldberg Variations, BWV 988 - Aria.flac";

    private TranscodingService transcodingService;
    private PlayerDao playerDao;
    private PlayerService playerService;
    private TranscodingDao transcodingDao;
    private static ExecutorService executor;
    private SecurityService securityService;

    @BeforeAll
    public static void beforeAll() {
        executor = Executors.newSingleThreadExecutor();
    }

    @BeforeEach
    public void setup() throws ExecutionException {
        transcodingDao = mock(TranscodingDao.class);
        securityService = mock(SecurityService.class);
        Mockito.when(securityService.getUserSettings(Mockito.nullable(String.class))).thenReturn(new UserSettings());
        SettingsService settingsService = mock(SettingsService.class);
        transcodingService = new TranscodingService(settingsService, securityService, transcodingDao, playerService,
                executor);
        playerDao = mock(PlayerDao.class);
        playerService = new PlayerService(playerDao, null, securityService, transcodingService);
        // for lazy
        transcodingService = new TranscodingService(settingsService, securityService, transcodingDao, playerService,
                executor);
    }

    @AfterAll
    public static void afterAll() {
        executor.shutdown();
    }

    /*
     * Creating a regular player associates active transcoding.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Test
    @Order(0)
    void testGetTranscodingsForPlayer() {
        List<Transcoding> defaulTranscodings = transcodingDao.getAllTranscodings();
        List<Transcoding> transcodings = new ArrayList<>(defaulTranscodings);
        transcodings.add(inactiveTranscoding);
        Mockito.when(transcodingDao.getAllTranscodings()).thenReturn(transcodings);

        Player player = new Player();
        player.setId(mockPlayerId);

        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<int[]> transcodingCaptor = ArgumentCaptor.forClass(int[].class);
        Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(idCaptor.capture(),
                transcodingCaptor.capture());

        playerService.createPlayer(player);
        assertEquals(mockPlayerId, idCaptor.getValue());

        List<int[]> registered = transcodingCaptor.getAllValues();
        assertEquals(defaulTranscodings.stream().map(t -> t.getId()).collect(Collectors.toList()), registered);

        assertTrue(defaulTranscodings.stream().allMatch(t -> registered.contains(t.getId())));
        assertEquals(registered.size(), defaulTranscodings.size());

        Mockito.when(transcodingDao.getTranscodingsForPlayer(player.getId())).thenReturn(defaulTranscodings);
        assertEquals(defaulTranscodings, transcodingService.getTranscodingsForPlayer(player));
    }

    /*
     * No transcoding is associated when the guest player is created.
     */
    @Test
    @Order(1)
    void testGetTranscodingsForGuestPlayer() {
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<int[]> transcodingIdsCaptor = ArgumentCaptor.forClass(int[].class);
        Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(idCaptor.capture(),
                transcodingIdsCaptor.capture());
        playerService.getGuestPlayer(null);
        Mockito.verify(transcodingDao, Mockito.never()).setTranscodingsForPlayer(Mockito.anyInt(),
                Mockito.any(int[].class));
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    @Order(2)
    class SetTranscodingsForPlayerTest {

        @Test
        @Order(1)
        void testsetTranscodingsForPlayer() {
            Player player = new Player();
            player.setId(1);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).updatePlayer(playerCaptor.capture());
            ArgumentCaptor<int[]> idsCaptor = ArgumentCaptor.forClass(int[].class);
            Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(Mockito.anyInt(), idsCaptor.capture());

            transcodingService.setTranscodingsForPlayer(player, new int[] { 1, 2, 3 });

            Mockito.verify(playerDao, Mockito.never()).updatePlayer(Mockito.any(Player.class));
            assertEquals(Arrays.asList(1, 2, 3), idsCaptor.getAllValues());
        }

        @Test
        @Order(2)
        void testsetTranscodingsForPlayerZeroParam() {
            Player player = new Player();
            player.setUsername("setTranscodingsTest");
            UserSettings settings = new UserSettings();
            settings.setTranscodeScheme(TranscodeScheme.MAX_256);
            Mockito.when(securityService.getUserSettings(player.getUsername())).thenReturn(settings);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).updatePlayer(playerCaptor.capture());
            ArgumentCaptor<int[]> idsCaptor = ArgumentCaptor.forClass(int[].class);
            Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(Mockito.anyInt(), idsCaptor.capture());

            transcodingService.setTranscodingsForPlayer(player, new int[] {});

            assertEquals(player, playerCaptor.getValue());
            assertEquals(TranscodeScheme.MAX_256, playerCaptor.getValue().getTranscodeScheme());
            assertEquals(0, idsCaptor.getAllValues().size());
        }

        @Test
        @Order(3)
        void testsetTranscodingsForPlayerZeroParamForAnonymous() {
            Player player = new Player();
            player.setUsername(JWTAuthenticationToken.USERNAME_ANONYMOUS);
            UserSettings settings = new UserSettings();
            settings.setTranscodeScheme(TranscodeScheme.MAX_128);
            Mockito.when(securityService.getUserSettings(User.USERNAME_GUEST)).thenReturn(settings);

            ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
            Mockito.doNothing().when(playerDao).updatePlayer(playerCaptor.capture());
            ArgumentCaptor<int[]> idsCaptor = ArgumentCaptor.forClass(int[].class);
            Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(Mockito.anyInt(), idsCaptor.capture());

            transcodingService.setTranscodingsForPlayer(player, new int[] {});

            assertEquals(player, playerCaptor.getValue());
            assertEquals(TranscodeScheme.MAX_128, playerCaptor.getValue().getTranscodeScheme());
            assertEquals(0, idsCaptor.getAllValues().size());
        }
    }

    @Test
    @Order(3)
    void testCreateTranscoding() {

        // Creating active transcoding
        Player player = new Player();
        player.setId(mockPlayerId);
        Mockito.when(playerService.getAllPlayers()).thenReturn(Arrays.asList(player));
        Transcoding mockActiveTranscoding = transcodingDao.getAllTranscodings().get(0);
        ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
        Mockito.when(transcodingDao.createTranscoding(transcodingCaptor.capture())).thenReturn(0);
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(int.class);
        ArgumentCaptor<int[]> transcodingIdsCaptor = ArgumentCaptor.forClass(int[].class);
        Mockito.doNothing().when(transcodingDao).setTranscodingsForPlayer(idCaptor.capture(),
                transcodingIdsCaptor.capture());
        transcodingService.createTranscoding(mockActiveTranscoding);

        // It will be set to the existing player
        assertEquals(mockActiveTranscoding, transcodingCaptor.getValue());
        assertEquals(mockPlayerId, idCaptor.getValue());
        List<int[]> registered = transcodingIdsCaptor.getAllValues();
        assertEquals(1, registered.size());
        assertEquals(Arrays.asList(mockActiveTranscoding.getId()), registered);

        // Creating inactive transcoding
        transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
        Mockito.clearInvocations(transcodingDao);
        Mockito.when(transcodingDao.createTranscoding(transcodingCaptor.capture()))
                .thenReturn(inactiveTranscoding.getId());
        transcodingService.createTranscoding(inactiveTranscoding);

        // Not set for existing players
        assertEquals(inactiveTranscoding, transcodingCaptor.getValue());
        Mockito.verify(transcodingDao, Mockito.never()).setTranscodingsForPlayer(Mockito.anyInt(),
                Mockito.any(int[].class));
    }

    @Test
    @Order(4)
    void testUpdateTranscoding() {
        ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
        Mockito.doNothing().when(transcodingDao).updateTranscoding(transcodingCaptor.capture());
        transcodingService.updateTranscoding(inactiveTranscoding);
        assertEquals(inactiveTranscoding, transcodingCaptor.getValue());
    }

    @Test
    @Order(5)
    void testDeleteTranscoding() {
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(int.class);
        Mockito.doNothing().when(transcodingDao).deleteTranscoding(idCaptor.capture());
        transcodingService.deleteTranscoding(inactiveTranscoding.getId());
        assertEquals(inactiveTranscoding.getId(), idCaptor.getValue());
    }

    @Test
    @Order(6)
    void testIsTranscodingRequired() {

        Player player = new Player();
        player.setId(mockPlayerId);
        MediaFile mediaFile = new MediaFile();

        // not required (No valid transcode)
        Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(Collections.emptyList());
        mediaFile.setFormat(fmtMp3);
        assertFalse(transcodingService.isTranscodingRequired(mediaFile, player));

        // required (mp3 -> mp3)
        List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
        Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);
        assertTrue(transcodingService.isTranscodingRequired(mediaFile, player));

        // required (flac -> mp3)
        mediaFile.setFormat(fmtFlac);
        assertTrue(transcodingService.isTranscodingRequired(mediaFile, player));

        // not required (Transcoding exists but is incompatible)
        mediaFile.setFormat(fmtRmf);
        assertFalse(transcodingService.isTranscodingRequired(mediaFile, player));
    }

    @Test
    @Order(7)
    void testGetSuffix() {
        Player player = new Player();
        player.setId(mockPlayerId);
        MediaFile mediaFile = new MediaFile();
        List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
        Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

        mediaFile.setFormat(fmtMp3);
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, null));
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, fmtRaw));
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, fmtMp3));

        mediaFile.setFormat(fmtFlac);
        assertEquals(fmtMp3, transcodingService.getSuffix(player, mediaFile, fmtFlac));

        mediaFile.setFormat(fmtRmf);
        assertEquals(fmtRmf, transcodingService.getSuffix(player, mediaFile, fmtMp3));

        mediaFile.setMediaType(MediaType.VIDEO);
        mediaFile.setFormat(fmtMpeg);
        assertEquals(fmtFlv, transcodingService.getSuffix(player, mediaFile, fmtMpeg));
        mediaFile.setFormat(fmtMp4);
        assertEquals(fmtMp4, transcodingService.getSuffix(player, mediaFile, fmtMp4));
    }

    /*
     * Only on Windows, there are the case of running file copy and temporary file creation failure. There are also
     * cases where process execution fails. These are difficult to reproduce in UT, but are omitted because they are
     * actually rare cases.
     */
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    @Order(8)
    class GetTranscodedInputStreamTest {

        private String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -";

        @Test
        @Order(1)
        void testGTI1() {
            // However, this is a case that is not considered in terms of implementation.
            Assertions.assertThrows(NullPointerException.class,
                    () -> transcodingService.getTranscodedInputStream(null));
        }

        /*
         * The behavior of this case on Windows is very doubtful.
         */
        @Test
        @Order(2)
        void testGTI2() throws IOException {
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
        void testGTI3Win() throws IOException {
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
        void testGTI3Linux() throws IOException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            Parameters parameters = new Parameters(mediaFile, null);

            Assertions.assertThrows(NoSuchFileException.class,
                    () -> transcodingService.getTranscodedInputStream(parameters));
        }

        @Test
        @Order(4)
        void testGTI4() throws IOException {
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
    @Order(9)
    class CreateTranscodedInputStreamTest {

        private String step = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -f flv -";

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
        void testCTI1() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, null, null, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = transcodingService.createTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(2)
        void testCTI2() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, null, step, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = transcodingService.createTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(3)
        void testCTI3() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, step, null, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = transcodingService.createTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Test
        @Order(4)
        void testCTI4() throws ExecutionException {
            Parameters parameters = createParam();
            Transcoding transcoding = new Transcoding(null, null, fmtMp3, fmtWav, step, step, step, false);
            parameters.setTranscoding(transcoding);

            try (InputStream stream = transcodingService.createTranscodedInputStream(parameters)) {
                Assertions.assertNotNull(stream);
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }
    }

    @Test
    @Order(10)
    void testSplitCommand() throws IOException {
        MatcherAssert.assertThat(transcodingService.splitCommand("key1 value1 key2 value2"),
                Matchers.arrayContaining("key1", "value1", "key2", "value2"));
        MatcherAssert.assertThat(transcodingService.splitCommand("key1 value1 key2 \"value2-1 value2-2\""),
                Matchers.arrayContaining("key1", "value1", "key2", "\"value2-1 value2-2\""));
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    @Order(11)
    class CreateTranscodeInputStreamTest {

        private String command = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 22050 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";

        @Test
        @Order(1)
        void testCTI1() throws IOException {
            Integer maxBitRate = null;
            VideoTranscodingSettings videoTranscodingSettings = null;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(realPath);
            InputStream in = null;
            try (TranscodeInputStream stream = transcodingService.createTranscodeInputStream(command, maxBitRate,
                    videoTranscodingSettings, mediaFile, in)) {
                Assertions.assertNotNull(stream);
            }
        }

        @Test
        @Order(2)
        void testCTI2() throws IOException {
            Integer maxBitRate = null;
            VideoTranscodingSettings videoTranscodingSettings = new VideoTranscodingSettings(640, 480, 0, 120, false);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setTitle("Title");
            mediaFile.setAlbumName("Album");
            mediaFile.setArtist("Artist");
            mediaFile.setPath(realPath);
            InputStream in = null;

            try (TranscodeInputStream stream = transcodingService.createTranscodeInputStream(command, maxBitRate,
                    videoTranscodingSettings, mediaFile, in)) {
                Assertions.assertNotNull(stream);
            }
        }
    }

    @Nested
    @Order(12)
    class GetTranscodingTest {

        @Test
        @Order(1)
        void testRaw() throws ExecutionException {
            MediaFile mediaFile = null;
            Player player = null;
            String preferredTargetFormat = fmtRaw;
            boolean hls = false;

            Assertions.assertNull(transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls));
        }

        @Test
        @Order(2)
        void testHls() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat("format");
            Player player = null;
            String preferredTargetFormat = null;
            boolean hls = true;

            Transcoding t = transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls);
            assertEquals(fmtHls, t.getName());
        }

        @Test
        @Order(3)
        void testPreferred() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);

            Player player = new Player();
            player.setId(mockPlayerId);
            List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

            String preferredTargetFormat = fmtMp3;
            boolean hls = false;

            Transcoding transcoding = transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(Transcodings.MP3.getName(), transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtFlac.equals(f)));
            assertEquals(fmtMp3, transcoding.getTargetFormat());

        }

        @Test
        @Order(4)
        void testNotPreferred() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);

            Player player = new Player();
            player.setId(mockPlayerId);
            List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

            String preferredTargetFormat = null;
            boolean hls = false;

            Transcoding transcoding = transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(Transcodings.MP3.getName(), transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtFlac.equals(f)));
            assertEquals(fmtMp3, transcoding.getTargetFormat());
        }

        @Test
        @Order(5)
        void testNotApplicable() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat("svg");

            Player player = new Player();
            player.setId(mockPlayerId);
            List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

            String preferredTargetFormat = null;
            boolean hls = false;

            Assertions.assertNull(transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls));
        }

        @Test
        @Order(6)
        void testVideo() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);

            Player player = new Player();
            player.setId(mockPlayerId);
            List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

            String preferredTargetFormat = null;
            boolean hls = false;

            Transcoding transcoding = transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(Transcodings.FLV.getName(), transcoding.getName());
            assertTrue(Arrays.stream(transcoding.getSourceFormatsAsArray()).anyMatch(f -> fmtMpeg.equals(f)));
            assertEquals(fmtFlv, transcoding.getTargetFormat());
        }

        @Test
        @Order(7)
        void testVideoTargetMatch() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtFlv);

            Player player = new Player();
            player.setId(mockPlayerId);
            List<Transcoding> defaultActives = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(mockPlayerId)).thenReturn(defaultActives);

            String preferredTargetFormat = fmtFlv;
            boolean hls = false;

            Transcoding transcoding = transcodingService.getTranscoding(mediaFile, player, preferredTargetFormat, hls);
            Assertions.assertNotNull(transcoding.getId());
            assertEquals(Transcodings.FLV.getName(), transcoding.getName());
            assertFalse(Arrays.stream(transcoding.getSourceFormatsAsArray()).allMatch(f -> fmtFlv.equals(f)));
            assertEquals(fmtFlv, transcoding.getTargetFormat());
        }
    }

    @Test
    @Order(13)
    void testIsTranscodingSupported() {

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
    @Order(14)
    class TranscoderInstalledTest {

        private String ffmpeg = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
        private String lame = "lame -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";

        @Test
        @Order(1)
        void testITI1() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, lame, lame, lame, true);
            assertFalse(transcodingService.isTranscoderInstalled(transcoding));
        }

        @Test
        @Order(2)
        void testITI2() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, lame, lame, true);
            assertFalse(transcodingService.isTranscoderInstalled(transcoding));
        }

        @Test
        @Order(3)
        void testITI3() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, lame, true);
            assertFalse(transcodingService.isTranscoderInstalled(transcoding));
        }

        @Test
        @Order(4)
        void testITI4() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, ffmpeg, true);
            assertTrue(transcodingService.isTranscoderInstalled(transcoding));
        }

        @Test
        @Order(5)
        void testITI5() throws ExecutionException {
            File f = transcodingService.getTranscodeDirectory();
            transcodingService.setTranscodeDirectory(new File(fakePath));
            Transcoding transcoding = new Transcoding(null, null, fmtFlac, fmtMp3, ffmpeg, ffmpeg, ffmpeg, true);

            assertFalse(transcodingService.isTranscoderInstalled(transcoding));

            transcodingService.setTranscodeDirectory(f);
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

            @interface Errorlog {
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
    @Order(15)
    class GetParametersTest {

        private final Transcoding fakeTranscoding = new Transcoding(null, "fake-instance", fmtFlac, fmtMp3, "s1", "s2",
                "s3", true);

        @BeforeEach
        public void before() throws ExecutionException {
            Player player = new Player();
            player.setId(mockPlayerId);
            String username = "mockUsername";
            player.setUsername(username);
            Mockito.when(playerService.getAllPlayers()).thenReturn(Arrays.asList(player));
            UserSettings userSettings = new UserSettings();
            userSettings.setTranscodeScheme(TranscodeScheme.OFF);
            Mockito.when(securityService.getUserSettings(username)).thenReturn(userSettings);
            List<Transcoding> defaulTranscodings = transcodingDao.getAllTranscodings();
            Mockito.when(transcodingDao.getTranscodingsForPlayer(player.getId())).thenReturn(defaulTranscodings);
        }

        @GetParametersDecision.Conditions.MaxBitRate.Null
        @GetParametersDecision.Conditions.PreferredTargetFormat.Null
        @GetParametersDecision.Conditions.VideoTranscodingSettings.Null
        @Test
        @Order(1)
        void testGP1() {

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);

            Player player = new Player();
            player.setId(mockPlayerId);
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

            playerService.removePlayerById(mockPlayerId);
        }

        @GetParametersDecision.Conditions.MediaFile.Format.NotNull
        @GetParametersDecision.Conditions.MaxBitRate.NotNull
        @Test
        @Order(2)
        void testGP2() {

            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setFormat(fmtFlac);
            mediaFile.setDurationSeconds(0);

            Player player = new Player();
            player.setId(mockPlayerId);
            playerService.createPlayer(player);
            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = null;

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            assertEquals(64_000, parameters.getExpectedLength());
            assertEquals(256, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(Transcodings.MP3.getName(), parameters.getTranscoding().getName());
            Assertions.assertNull(parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(mockPlayerId);
        }

        @GetParametersDecision.Conditions.MediaFile.MediaType.Video
        @GetParametersDecision.Conditions.MediaFile.Format.NotNull
        @Test
        @Order(3)
        void testGP3() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);
            mediaFile.setDurationSeconds(0);

            Player player = new Player();
            player.setId(mockPlayerId);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = null;

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            assertEquals(500_000, parameters.getExpectedLength());
            assertEquals(2000, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(Transcodings.FLV.getName(), parameters.getTranscoding().getName());
            Assertions.assertNull(parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

            playerService.removePlayerById(mockPlayerId);
        }

        @GetParametersDecision.Conditions.Player.Username.Anonymous
        @GetParametersDecision.Conditions.VideoTranscodingSettings.Hls.False
        @Test
        @Order(4)
        void testGP4() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setFormat(fmtFlac);
            mediaFile.setDurationSeconds(0);

            Player player = new Player();
            player.setId(mockPlayerId);
            player.setUsername(JWTAuthenticationToken.USERNAME_ANONYMOUS);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = new VideoTranscodingSettings(640, 480, 0, 120, false);

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            assertEquals(64_000, parameters.getExpectedLength());
            assertEquals(256, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(Transcodings.MP3.getName(), parameters.getTranscoding().getName());
            assertEquals(videoTranscodingSettings, parameters.getVideoTranscodingSettings());
            assertFalse(parameters.isRangeAllowed());

        }

        @GetParametersDecision.Conditions.VideoTranscodingSettings.Hls.True
        @Test
        @Order(5)
        void testGP5() {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(fakePath);
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setFormat(fmtMpeg);
            mediaFile.setDurationSeconds(0);

            Player player = new Player();
            player.setId(mockPlayerId);
            playerService.createPlayer(player);

            Integer maxBitRate = 224;
            String preferredTargetFormat = null;
            VideoTranscodingSettings videoTranscodingSettings = new VideoTranscodingSettings(640, 480, 0, 120, true);

            Parameters parameters = transcodingService.getParameters(mediaFile, player, maxBitRate,
                    preferredTargetFormat, videoTranscodingSettings);

            assertEquals(500_000, parameters.getExpectedLength());
            assertEquals(2000, parameters.getMaxBitRate());
            assertEquals(mediaFile, parameters.getMediaFile());
            assertEquals(fmtHls, parameters.getTranscoding().getName());
            assertEquals(640, parameters.getVideoTranscodingSettings().getWidth());
            assertEquals(480, parameters.getVideoTranscodingSettings().getHeight());
            assertEquals(0, parameters.getVideoTranscodingSettings().getTimeOffset());
            assertEquals(120, parameters.getVideoTranscodingSettings().getDuration());
            assertTrue(parameters.getVideoTranscodingSettings().isHls());
            assertFalse(parameters.isRangeAllowed());

        }

        @IsNeedTranscoding.Conditions.Transcoding.Null
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(11)
        void testINT1() throws ExecutionException {
            Transcoding transcoding = null;
            int mb = 0;
            int bitRate = 0;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(
                    transcodingService.isNeedTranscoding(transcoding, mb, bitRate, preferredTargetFormat, mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.Zero
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(12)
        void testINT2() throws ExecutionException {
            int mb = 0;
            int bitRate = 0;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(transcodingService.isNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat,
                    mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.LtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.Null
        @IsNeedTranscoding.Result.FALSE
        @Test
        @Order(13)
        void testINT3() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertFalse(transcodingService.isNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat,
                    mediaFile));
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
        void testINT4() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = fmtMp3;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtMp3);

            assertFalse(transcodingService.isNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat,
                    mediaFile));
        }

        @IsNeedTranscoding.Conditions.Transcoding.NotNull
        @IsNeedTranscoding.Conditions.MaxBitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtZero
        @IsNeedTranscoding.Conditions.BitRate.GtMaxBitRate
        @IsNeedTranscoding.Conditions.PreferredTargetFormat.Null
        @IsNeedTranscoding.Result.TRUE
        @Test
        @Order(15)
        void testINT5() throws ExecutionException {
            int mb = 2;
            int bitRate = 4;
            String preferredTargetFormat = null;
            MediaFile mediaFile = null;

            assertTrue(transcodingService.isNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat,
                    mediaFile));
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
        void testINT6() throws ExecutionException {
            int mb = 2;
            int bitRate = 1;
            String preferredTargetFormat = fmtMp3;
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFormat(fmtFlac);

            assertTrue(transcodingService.isNeedTranscoding(fakeTranscoding, mb, bitRate, preferredTargetFormat,
                    mediaFile));
        }

        @Test
        @Order(21)
        void testCB1() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setBitRate(128);

            assertEquals(128, transcodingService.createBitrate(mediaFile, null));
        }

        @Test
        @Order(22)
        void testCB2() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setMediaType(MediaType.VIDEO);
            mediaFile.setBitRate(1024);

            assertEquals(1024, transcodingService.createBitrate(mediaFile, null));
        }

        @Test
        @Order(23)
        void testCB3() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(5000);
            mediaFile.setVariableBitRate(false);

            assertEquals(5000, transcodingService.createBitrate(mediaFile, null));
        }

        @Test
        @Order(24)
        void testCB4() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(950);
            mediaFile.setVariableBitRate(true);

            assertEquals(1411, transcodingService.createBitrate(mediaFile, null));
        }

        @Test
        @Order(25)
        void testCB5() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(128);
            mediaFile.setVariableBitRate(true);

            assertEquals(256, transcodingService.createBitrate(mediaFile, null));
        }

        @Test
        @Order(26)
        void testCB6() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setBitRate(128);
            mediaFile.setVariableBitRate(true);
            Transcoding t = new Transcoding(0, fmtFlac, fmtMp3, fmtFlac, fmtFlac, "", null, false);
            assertEquals(128, transcodingService.createBitrate(mediaFile, t));
        }

        @CreateMaxBitrate.Conditions.Mb.Zero
        @CreateMaxBitrate.Result.BitRate
        @Test
        @Order(31)
        void testCMB1() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.OFF;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 0;

            assertEquals(0, transcodingService.createMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.NeZero
        @CreateMaxBitrate.Conditions.BitRate.LtMb
        @CreateMaxBitrate.Result.BitRate
        @Test
        @Order(32)
        void testCMB2() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_320;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 256;

            assertEquals(256, transcodingService.createMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.NeZero
        @CreateMaxBitrate.Conditions.BitRate.GtMb
        @CreateMaxBitrate.Result.Mb
        @Test
        @Order(33)
        void testCMB3() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_256;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 320;

            assertEquals(256, transcodingService.createMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @CreateMaxBitrate.Conditions.Mb.NeZero
        @CreateMaxBitrate.Conditions.BitRate.Zero
        @CreateMaxBitrate.Result.Mb
        @Test
        @Order(34)
        void testCMB4() throws ExecutionException {
            TranscodeScheme transcodeScheme = TranscodeScheme.MAX_256;
            MediaFile mediaFile = new MediaFile();
            int bitRate = 0;

            assertEquals(256, transcodingService.createMaxBitrate(transcodeScheme, mediaFile, bitRate));
        }

        @Test
        @Order(41)
        void testIRA1() throws ExecutionException {
            Parameters parameters = new Parameters(null, null);

            assertTrue(transcodingService.isRangeAllowed(parameters));
        }

        @Test
        @Order(42)
        void testIRA2() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(null);
            assertFalse(transcodingService.isRangeAllowed(parameters));
        }

        @Test
        @Order(43)
        void testIRA3() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "not contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertFalse(transcodingService.isRangeAllowed(parameters));
        }

        @Test
        @Order(44)
        void testIRA4() throws ExecutionException {
            String step1 = "ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset superfast -threads 0 -";
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, step1, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertTrue(transcodingService.isRangeAllowed(parameters));
        }

        @Test
        @Order(45)
        void testIRA5() throws ExecutionException {
            Transcoding transcoding = new Transcoding(null, "contains %b", fmtMp3, fmtWav, null, null, null, true);
            Parameters parameters = new Parameters(null, null);
            parameters.setTranscoding(transcoding);
            parameters.setExpectedLength(1L);
            assertFalse(transcodingService.isRangeAllowed(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.False
        @GetExpectedLength.Result.MediaFile.FileSize
        @Test
        @Order(51)
        void testGEL1() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileSize(123L);
            Parameters parameters = new Parameters(mediaFile, null);
            assertFalse(parameters.isTranscode());

            assertEquals(123L, transcodingService.getExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.Null
        @GetExpectedLength.Result.Null
        @GetExpectedLength.Result.Errorlog // Unknown duration for null
        @Test
        @Order(52)
        void testGEL2() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());

            Assertions.assertNull(transcodingService.getExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.NotNull
        @GetExpectedLength.Conditions.Parameters.MaxBitRate.Null
        @GetExpectedLength.Result.Null
        @GetExpectedLength.Result.Errorlog // Unknown bit rate for null.
        @Test
        @Order(53)
        void testGEL3() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setDurationSeconds(120);
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());

            Assertions.assertNull(transcodingService.getExpectedLength(parameters));
        }

        @GetExpectedLength.Conditions.Parameters.Transcode.True
        @GetExpectedLength.Conditions.Parameters.MediaFile.DurationSeconds.NotNull
        @GetExpectedLength.Conditions.Parameters.MaxBitRate.NotNull
        @GetExpectedLength.Result.Estimates
        @Test
        @Order(54)
        void testGEL4() throws ExecutionException {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setDurationSeconds(120);
            Parameters parameters = new Parameters(mediaFile, null);
            parameters.setTranscoding(fakeTranscoding);
            assertTrue(parameters.isTranscode());
            parameters.setMaxBitRate(256);

            assertEquals(3_904_000, transcodingService.getExpectedLength(parameters));
        }

    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RestoreTranscodingTest {

        @Test
        @Order(1)
        void testNullParam() {
            transcodingService.restoreTranscoding(null, false);
            Mockito.verify(transcodingDao, Mockito.never()).createTranscoding(Mockito.nullable(Transcoding.class));
            Mockito.verify(transcodingDao, Mockito.never()).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.never()).getAllPlayers();
            Mockito.verify(transcodingDao, Mockito.never()).deleteTranscoding(Mockito.anyInt());
        }

        @Test
        @Order(2)
        void testRestoreMp3() {

            int createdMp3Id = 999;
            Mockito.when(transcodingDao.createTranscoding(Mockito.any(Transcoding.class))).thenReturn(createdMp3Id);

            Transcoding created = new Transcoding(createdMp3Id, Transcodings.MP3.getName(), fmtFlv, fmtFlac, fakePath,
                    null, null, false);
            List<Transcoding> defaulTranscodings = transcodingDao.getAllTranscodings();
            Mockito.clearInvocations(transcodingDao);
            List<Transcoding> transcodings = new ArrayList<>(defaulTranscodings);
            transcodings.add(created);
            Mockito.when(transcodingDao.getAllTranscodings()).thenReturn(transcodings);

            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            transcodingService.restoreTranscoding(Transcodings.MP3, false);

            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.MP3.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();
            Mockito.verify(transcodingDao, Mockito.times(1)).deleteTranscoding(Mockito.anyInt());
        }

        @Test
        @Order(3)
        void testAddTag() {
            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            Mockito.when(transcodingDao.createTranscoding(transcodingCaptor.capture())).thenReturn(0);
            transcodingService.restoreTranscoding(Transcodings.MP3, false);
            assertEquals(Transcodings.MP3.getName(), transcodingCaptor.getValue().getName());
            assertEquals("ffmpeg -i %s -map 0:0 -b:a %bk -v 0 -f mp3 -", transcodingCaptor.getValue().getStep1());

            transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            Mockito.when(transcodingDao.createTranscoding(transcodingCaptor.capture())).thenReturn(0);
            transcodingService.restoreTranscoding(Transcodings.MP3, true);
            assertEquals(Transcodings.MP3.getName(), transcodingCaptor.getValue().getName());
            assertEquals("ffmpeg -i %s -map 0:0 -b:a %bk -id3v2_version 3 -v 0 -f mp3 -",
                    transcodingCaptor.getValue().getStep1());
        }

        @Test
        @Order(4)
        void testRestoreFlv() {
            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            transcodingService.restoreTranscoding(Transcodings.FLV, false);

            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.FLV.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();
            Mockito.verify(transcodingDao, Mockito.times(1)).deleteTranscoding(Mockito.anyInt());
        }

        @Test
        @Order(5)
        void testRestoreMkv() {
            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            transcodingService.restoreTranscoding(Transcodings.MKV, false);

            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.MKV.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();
            Mockito.verify(transcodingDao, Mockito.times(1)).deleteTranscoding(Mockito.anyInt());
        }

        @Test
        @Order(6)
        void testRestoreMp4() {
            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            transcodingService.restoreTranscoding(Transcodings.MP4, false);

            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.MP4.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();
            Mockito.verify(transcodingDao, Mockito.times(1)).deleteTranscoding(Mockito.anyInt());
        }

        @Test
        @Order(7)
        void testSwap() {
            int player1Id = mockPlayerId;
            Player player1 = new Player();
            player1.setId(player1Id);

            int player2Id = mockPlayerId + 1;
            Player player2 = new Player();
            player2.setId(player2Id);

            int player3Id = mockPlayerId + 2;
            Player player3 = new Player();
            player3.setId(player3Id);

            int mp3Id = 1;
            Transcoding mp3 = new Transcoding(mp3Id, Transcodings.MP3.getName(), null, null, fakePath, null, null,
                    true);
            int flvId = 2;
            Transcoding flv = new Transcoding(flvId, Transcodings.FLV.getName(), null, null, fakePath, null, null,
                    true);

            Mockito.when(playerService.getAllPlayers()).thenReturn(Arrays.asList(player1, player2, player3));
            Mockito.when(transcodingDao.getTranscodingsForPlayer(player1Id)).thenReturn(Arrays.asList(mp3)); // mp3
            Mockito.when(transcodingDao.getTranscodingsForPlayer(player2Id)).thenReturn(Arrays.asList(flv));
            Mockito.when(transcodingDao.getTranscodingsForPlayer(player3Id)).thenReturn(Arrays.asList(mp3, flv)); // mp3

            int createdMp3Id = 999;
            Mockito.when(transcodingDao.createTranscoding(Mockito.any(Transcoding.class))).thenReturn(createdMp3Id);
            Transcoding created = new Transcoding(createdMp3Id, Transcodings.MP3.getName(), null, null, fakePath, null,
                    null, true);
            List<Transcoding> defaulTranscodings = transcodingDao.getAllTranscodings();
            Mockito.clearInvocations(transcodingDao);
            List<Transcoding> transcodings = new ArrayList<>(defaulTranscodings);
            transcodings.add(created);
            Mockito.when(transcodingDao.getAllTranscodings()).thenReturn(transcodings);

            transcodingService.restoreTranscoding(Transcodings.MP3, false);

            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.MP3.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();

            ArgumentCaptor<Integer> playerIdCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<int[]> transcodingIdsCaptor = ArgumentCaptor.forClass(int[].class);
            Mockito.verify(transcodingDao, Mockito.times(3)).setTranscodingsForPlayer(playerIdCaptor.capture(),
                    transcodingIdsCaptor.capture());

            assertEquals(createdMp3Id, transcodingIdsCaptor.getAllValues().get(2));
            assertEquals(5, transcodingIdsCaptor.getAllValues().size());

            // player1
            assertEquals(player1Id, playerIdCaptor.getAllValues().get(0));
            assertEquals(createdMp3Id, transcodingIdsCaptor.getAllValues().get(0));

            // player2
            assertEquals(player2Id, playerIdCaptor.getAllValues().get(1));
            assertEquals(flvId, transcodingIdsCaptor.getAllValues().get(1));
            assertEquals(createdMp3Id, transcodingIdsCaptor.getAllValues().get(2));

            // player3
            assertEquals(player3Id, playerIdCaptor.getAllValues().get(2));
            assertEquals(flvId, transcodingIdsCaptor.getAllValues().get(3));
            assertEquals(createdMp3Id, transcodingIdsCaptor.getAllValues().get(4));

            ArgumentCaptor<Integer> deletedIdCaptor = ArgumentCaptor.forClass(Integer.class);
            Mockito.verify(transcodingDao, Mockito.times(1)).deleteTranscoding(deletedIdCaptor.capture());
            assertEquals(1, deletedIdCaptor.getAllValues().size());

            int toBeDeletedId = defaulTranscodings.stream().filter(t -> Transcodings.MP3.getName().equals(t.getName()))
                    .collect(Collectors.toList()).get(0).getId();
            // Delete old transcode with the same name
            assertEquals(toBeDeletedId, deletedIdCaptor.getValue());
        }

        @Test
        @Order(6)
        void testRestoreFlac() {

            int player1Id = mockPlayerId;
            Player player1 = new Player();
            player1.setId(player1Id);
            Mockito.when(playerService.getAllPlayers()).thenReturn(Arrays.asList(player1));

            ArgumentCaptor<Transcoding> transcodingCaptor = ArgumentCaptor.forClass(Transcoding.class);
            transcodingService.restoreTranscoding(Transcodings.FLAC, false);

            Mockito.verify(transcodingDao, Mockito.times(1)).createTranscoding(transcodingCaptor.capture());
            assertEquals(Transcodings.FLAC.getName(), transcodingCaptor.getValue().getName());
            Mockito.verify(transcodingDao, Mockito.times(2)).getAllTranscodings();
            Mockito.verify(playerDao, Mockito.times(1)).getAllPlayers();

            // This transcoding is rewstore only. Not registered in DB at the time of installation.
            Mockito.verify(transcodingDao, Mockito.never()).deleteTranscoding(Mockito.anyInt());

            // This transcoding is registered in the DB at restore, but is not enabled for existing players.
            // (Not active by default)
            ArgumentCaptor<int[]> transcodingIdsCaptor = ArgumentCaptor.forClass(int[].class);
            Mockito.verify(transcodingDao, Mockito.times(1)).setTranscodingsForPlayer(Mockito.any(Integer.class),
                    transcodingIdsCaptor.capture());
            assertEquals(1, transcodingIdsCaptor.getAllValues().size());
            Assertions.assertNotEquals(transcodingCaptor.getValue().getId(), transcodingIdsCaptor.getValue());
        }

    }
}
