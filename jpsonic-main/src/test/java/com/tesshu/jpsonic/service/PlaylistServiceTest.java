package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.dao.DaoHelper;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.dao.JPlaylistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlaylistDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.playlist.DefaultPlaylistExportHandler;
import com.tesshu.jpsonic.service.playlist.DefaultPlaylistImportHandler;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PlaylistServiceTest {

    @Nested
    class ExportPlaylistTest {

        private MediaFileDao mediaFileDao;
        private PlaylistService playlistService;

        @BeforeEach
        public void setup() {
            mediaFileDao = mock(MediaFileDao.class);
            DaoHelper daoHelper = mock(DaoHelper.class);
            JMediaFileDao jMediaFileDao = new JMediaFileDao(daoHelper, mediaFileDao);
            JPlaylistDao jPlaylistDao = new JPlaylistDao(daoHelper, mock(PlaylistDao.class));
            playlistService = new PlaylistService(jMediaFileDao, jPlaylistDao, mock(SecurityService.class),
                    mock(SettingsService.class), Arrays.asList(new DefaultPlaylistExportHandler(mediaFileDao)),
                    Collections.emptyList(), null);
        }

        @Test
        void testExportToM3U() throws Exception {
            Mockito.when(mediaFileDao.getFilesInPlaylist(ArgumentMatchers.eq(23))).thenReturn(getPlaylistFiles());

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                playlistService.exportPlaylist(23, outputStream);
                /*
                 * Since there is no problem in operation, blank lines are excluded from verification. (The trailing
                 * newline is not output on the Windows-"server")
                 */
                String expected = IOUtils
                        .toString(getClass().getResourceAsStream("/PLAYLISTS/23.m3u"), StandardCharsets.UTF_8)
                        .replaceAll("\\r", "").replaceAll("\\n+$", "");
                String actual = outputStream.toString(StandardCharsets.UTF_8).replaceAll("\\r", "").replaceAll("\\n+$",
                        "");
                assertEquals(expected, actual);
            }
        }

        private List<MediaFile> getPlaylistFiles() {

            MediaFile mf1 = new MediaFile();
            mf1.setId(142);
            mf1.setPathString("/some/path/to_album/to_artist/name - of - song.mp3");
            mf1.setPresent(true);
            List<MediaFile> mediaFiles = new ArrayList<>();
            mediaFiles.add(mf1);

            MediaFile mf2 = new MediaFile();
            mf2.setId(1235);
            mf2.setPathString("/some/path/to_album2/to_artist/another song.mp3");
            mf2.setPresent(true);
            mediaFiles.add(mf2);

            MediaFile mf3 = new MediaFile();
            mf3.setId(198_403);
            mf3.setPathString("/some/path/to_album2/to_artist/another song2.mp3");
            mf3.setPresent(false);
            mediaFiles.add(mf3);

            return mediaFiles;
        }
    }

    @Nested
    class ImportPlaylistTest {

        private PlaylistService playlistService;

        private PlaylistDao playlistDao;
        private MediaFileService mediaFileService;

        private ArgumentCaptor<Playlist> actual;
        private ArgumentCaptor<List<MediaFile>> medias;

        @TempDir
        public Path tempDir;

        @SuppressWarnings("unchecked")
        @BeforeEach
        public void setup() {
            playlistDao = mock(PlaylistDao.class);
            mediaFileService = mock(MediaFileService.class);
            DaoHelper daoHelper = mock(DaoHelper.class);
            JMediaFileDao jMediaFileDao = new JMediaFileDao(daoHelper, mock(MediaFileDao.class));
            JPlaylistDao jPlaylistDao = new JPlaylistDao(daoHelper, playlistDao);
            DefaultPlaylistImportHandler importHandler = new DefaultPlaylistImportHandler(mediaFileService);
            playlistService = new PlaylistService(jMediaFileDao, jPlaylistDao, mock(SecurityService.class),
                    mock(SettingsService.class), Collections.emptyList(), Arrays.asList(importHandler), null);
            actual = ArgumentCaptor.forClass(Playlist.class);
            medias = ArgumentCaptor.forClass(List.class);
        }

        @Test
        void testImportFromM3U() throws Exception {

            final String username = "testUser";
            final String playlistName = "test-playlist";
            StringBuilder builder = new StringBuilder();
            builder.append("#EXTM3U\n");
            Path mf1 = Path.of(tempDir.toString(), "EXTM3U-mf1");
            FileUtil.touch(mf1);
            Path mf2 = Path.of(tempDir.toString(), "EXTM3U-mf2");
            FileUtil.touch(mf2);
            Path mf3 = Path.of(tempDir.toString(), "EXTM3U-mf3");
            FileUtil.touch(mf3);
            builder.append(mf1.toUri().toString()).append('\n').append(mf2.toUri().toString()).append('\n')
                    .append(mf3.toUri().toString()).append('\n');

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());
            String path = playlistName + ".m3u";
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path);
            Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(new MediaFile());

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));

            playlistService.importPlaylist(username, playlistName, path, inputStream, null);

            verify(playlistDao).createPlaylist(actual.capture());
            verify(playlistDao).setFilesInPlaylist(ArgumentMatchers.eq(23), medias.capture());

            Playlist expected = new Playlist();
            expected.setUsername(username);
            expected.setName(playlistName);
            expected.setComment("Auto-imported from " + path);
            expected.setImportedFrom(path);
            expected.setShared(true);
            expected.setId(23);

            Assertions.assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"),
                    "\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n"
                            + ToStringBuilder.reflectionToString(expected));
            List<MediaFile> mediaFiles = medias.getValue();
            assertEquals(3, mediaFiles.size());
        }

        @Test
        void testImportFromPLS() throws Exception {
            final String username = "testUser";
            final String playlistName = "test-playlist";
            Path mf1 = Path.of(tempDir.toString(), "PLS-mf1");
            FileUtil.touch(mf1);
            Path mf2 = Path.of(tempDir.toString(), "PLS-mf2");
            FileUtil.touch(mf2);
            Path mf3 = Path.of(tempDir.toString(), "PLS-mf3");
            FileUtil.touch(mf3);
            StringBuilder builder = new StringBuilder(40);
            builder.append("[playlist]\nFile1=").append(mf1.toUri().toString()).append("\nFile2=")
                    .append(mf2.toUri().toString()).append("\nFile3=").append(mf3.toUri().toString()).append('\n');

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());

            String path = Path.of("/path/to/" + playlistName + ".pls").toString();
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path);
            Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(mediaFile);

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));

            playlistService.importPlaylist(username, playlistName, path, inputStream, null);

            verify(playlistDao).createPlaylist(actual.capture());
            verify(playlistDao).setFilesInPlaylist(ArgumentMatchers.eq(23), medias.capture());

            Playlist expected = new Playlist();
            expected.setUsername(username);
            expected.setName(playlistName);
            expected.setComment("Auto-imported from " + path);
            expected.setImportedFrom(path);
            expected.setShared(true);
            expected.setId(23);

            Assertions.assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"));
            List<MediaFile> mediaFiles = medias.getValue();
            assertEquals(3, mediaFiles.size());
        }

        @Test
        void testImportFromXSPF() throws Exception {
            final String username = "testUser";
            final String playlistName = "test-playlist";
            Path mf1 = Path.of(tempDir.toString(), "XSPF-mf1");
            FileUtil.touch(mf1);
            Path mf2 = Path.of(tempDir.toString(), "XSPF-mf2");
            FileUtil.touch(mf2);
            Path mf3 = Path.of(tempDir.toString(), "XSPF-mf3");
            FileUtil.touch(mf3);
            StringBuilder builder = new StringBuilder(300);
            builder.append(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n<trackList>\n<track><location>")
                    .append(mf1.toUri().toString()).append("</location></track>\n<track><location>")
                    .append(mf2.toUri().toString()).append("</location></track>\n<track><location>")
                    .append(mf3.toUri().toString()).append("</location></track>\n</trackList>\n</playlist>\n");

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());
            String path = Path.of("/path/to/" + playlistName + ".xspf").toString();
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPathString(path);
            Mockito.when(mediaFileService.getMediaFile(Mockito.any(Path.class))).thenReturn(mediaFile);

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));

            playlistService.importPlaylist(username, playlistName, path, inputStream, null);

            verify(playlistDao).createPlaylist(actual.capture());
            verify(playlistDao).setFilesInPlaylist(ArgumentMatchers.eq(23), medias.capture());

            Playlist expected = new Playlist();
            expected.setUsername(username);
            expected.setName(playlistName);
            expected.setComment("Auto-imported from " + path);
            expected.setImportedFrom(path);
            expected.setShared(true);
            expected.setId(23);
            Assertions.assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"));
            List<MediaFile> mediaFiles = medias.getValue();
            assertEquals(3, mediaFiles.size());
        }
    }

    private static class PersistPlayList implements Answer<Object> {
        private final int id;

        public PersistPlayList(int id) {
            this.id = id;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) {
            Playlist playlist = invocationOnMock.getArgument(0);
            playlist.setId(id);
            return null;
        }
    }

    @Nested
    class ImportPlaylistsTest {

        private SettingsService settingsService;
        private PlaylistService playlistService;
        private PlaylistDao playlistDao;
        private MediaFileService mediaFileService;

        @TempDir
        public Path tempDir;

        @BeforeEach
        public void setup() {
            playlistDao = mock(PlaylistDao.class);
            settingsService = mock(SettingsService.class);
            mediaFileService = mock(MediaFileService.class);
            DaoHelper daoHelper = mock(DaoHelper.class);
            JMediaFileDao jMediaFileDao = new JMediaFileDao(daoHelper, mock(MediaFileDao.class));
            JPlaylistDao jPlaylistDao = new JPlaylistDao(daoHelper, playlistDao);
            DefaultPlaylistImportHandler importHandler = new DefaultPlaylistImportHandler(mediaFileService);
            playlistService = new PlaylistService(jMediaFileDao, jPlaylistDao, mock(SecurityService.class),
                    settingsService, Collections.emptyList(), Arrays.asList(importHandler), null);
        }

        @Test
        void testImportPlaylists(@TempDir Path tempDir) throws IOException {

            final Date current = new Date();

            Mockito.when(settingsService.getPlaylistFolder()).thenReturn(tempDir.toString());

            Path mf1 = Path.of(tempDir.toString(), "XSPF-mf1");
            FileUtil.touch(mf1);
            Path mf2 = Path.of(tempDir.toString(), "XSPF-mf2");
            FileUtil.touch(mf2);
            Path mf3 = Path.of(tempDir.toString(), "XSPF-mf3");
            FileUtil.touch(mf3);
            StringBuilder builder = new StringBuilder(300);
            builder.append(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n<trackList>\n<track><location>")
                    .append(mf1.toUri().toString()).append("</location></track>\n<track><location>")
                    .append(mf2.toUri().toString()).append("</location></track>\n<track><location>")
                    .append(mf3.toUri().toString()).append("</location></track>\n</trackList>\n</playlist>\n");

            Path playlistFile = Path.of(tempDir.toString(), "playlistFile");
            Files.write(playlistFile, builder.toString().getBytes());

            Playlist playlist = new Playlist();
            playlist.setImportedFrom(playlistFile.getFileName().toString());
            playlist.setChanged(current);
            Mockito.when(playlistDao.getAllPlaylists()).thenReturn(Arrays.asList(playlist));

            MediaFile mediaFile = new MediaFile();
            mediaFile.setId(0);
            mediaFile.setPathString(mf3.toString());
            Mockito.when(mediaFileService.getMediaFile(mf3)).thenReturn(mediaFile);

            ArgumentCaptor<Playlist> playlistCatCaptor = ArgumentCaptor.forClass(Playlist.class);
            Mockito.doNothing().when(playlistDao).createPlaylist(playlistCatCaptor.capture());
            playlistService.importPlaylists();

            List<Playlist> captored = playlistCatCaptor.getAllValues();
            captored.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
            assertEquals(3, playlistCatCaptor.getAllValues().size());
            assertEquals("XSPF-mf1", captored.get(0).getName());
            assertEquals("XSPF-mf2", captored.get(1).getName());
            assertEquals("XSPF-mf3", captored.get(2).getName());
        }
    }
}
