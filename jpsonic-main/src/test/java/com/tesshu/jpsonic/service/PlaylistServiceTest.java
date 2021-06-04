package com.tesshu.jpsonic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.tesshu.jpsonic.dao.DaoHelper;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.dao.JPlaylistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlaylistDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.service.playlist.DefaultPlaylistExportHandler;
import com.tesshu.jpsonic.service.playlist.DefaultPlaylistImportHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PlaylistServiceTest {

    @Nested
    public class ExportTest {

        private PlaylistService playlistService;

        @InjectMocks
        private DefaultPlaylistExportHandler defaultPlaylistExportHandler;

        @Mock
        private DaoHelper daoHelper;

        @Mock
        private MediaFileDao mediaFileDao;

        @Mock
        private PlaylistDao playlistDao;

        @Mock
        private SettingsService settingsService;

        @Mock
        private SecurityService securityService;

        // @Captor
        // private ArgumentCaptor<Playlist> actual;

        // @Captor
        // private ArgumentCaptor<List<MediaFile>> medias;

        @BeforeEach
        public void setup() {
            MockitoAnnotations.openMocks(this);
            JMediaFileDao jMediaFileDao = new JMediaFileDao(daoHelper, mediaFileDao);
            JPlaylistDao jPlaylistDao = new JPlaylistDao(daoHelper, playlistDao);
            playlistService = new PlaylistService(jMediaFileDao, jPlaylistDao, securityService, settingsService,
                    Lists.newArrayList(defaultPlaylistExportHandler), Collections.emptyList(), null);
        }

        @Test
        public void testExportToM3U() throws Exception {
            Mockito.when(mediaFileDao.getFilesInPlaylist(ArgumentMatchers.eq(23))).thenReturn(getPlaylistFiles());
            Mockito.when(settingsService.getPlaylistExportFormat()).thenReturn("m3u");

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
            mf1.setPath("/some/path/to_album/to_artist/name - of - song.mp3");
            mf1.setPresent(true);
            List<MediaFile> mediaFiles = new ArrayList<>();
            mediaFiles.add(mf1);

            MediaFile mf2 = new MediaFile();
            mf2.setId(1235);
            mf2.setPath("/some/path/to_album2/to_artist/another song.mp3");
            mf2.setPresent(true);
            mediaFiles.add(mf2);

            MediaFile mf3 = new MediaFile();
            mf3.setId(198_403);
            mf3.setPath("/some/path/to_album2/to_artist/another song2.mp3");
            mf3.setPresent(false);
            mediaFiles.add(mf3);

            return mediaFiles;
        }
    }

    @Nested
    public class ImportTest {

        private PlaylistService playlistService;

        @Mock
        private DaoHelper daoHelper;

        @Mock
        private MediaFileDao mediaFileDao;

        @Mock
        private PlaylistDao playlistDao;

        @Mock
        private MediaFileService mediaFileService;

        @Mock
        private SettingsService settingsService;

        @Mock
        private SecurityService securityService;

        @Captor
        private ArgumentCaptor<Playlist> actual;

        @Captor
        private ArgumentCaptor<List<MediaFile>> medias;

        @TempDir
        public Path tempDirPath;

        public File tempDir;

        @BeforeEach
        public void setup() {
            MockitoAnnotations.openMocks(this);
            JMediaFileDao jMediaFileDao = new JMediaFileDao(daoHelper, mediaFileDao);
            JPlaylistDao jPlaylistDao = new JPlaylistDao(daoHelper, playlistDao);
            DefaultPlaylistImportHandler importHandler = new DefaultPlaylistImportHandler(mediaFileService);
            playlistService = new PlaylistService(jMediaFileDao, jPlaylistDao, securityService, settingsService,
                    Collections.emptyList(), Lists.newArrayList(importHandler), null);
            if (tempDir != null) {
                tempDir = tempDirPath.toFile();
                if (!tempDir.exists()) {
                    tempDir.mkdir();
                }
            }
        }

        @Test
        public void testImportFromM3U() throws Exception {
            final String username = "testUser";
            final String playlistName = "test-playlist";
            StringBuilder builder = new StringBuilder();
            builder.append("#EXTM3U\n");
            File tempDir = tempDirPath.toFile();
            if (!tempDir.exists()) {
                tempDir.mkdir();
            }
            File mf1 = new File(tempDir, "EXTM3U-mf1");
            FileUtils.touch(mf1);
            File mf2 = new File(tempDir, "EXTM3U-mf2");
            FileUtils.touch(mf2);
            File mf3 = new File(tempDir, "EXTM3U-mf3");
            FileUtils.touch(mf3);
            builder.append(mf1.toURI().toString()).append('\n').append(mf2.toURI().toString()).append('\n')
                    .append(mf3.toURI().toString()).append('\n');

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());
            doAnswer(new MediaFileHasEverything()).when(mediaFileService)
                    .getMediaFile(ArgumentMatchers.any(File.class));

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
            String path = new File("/path/to/" + playlistName + ".m3u").toURI().toString();

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

            assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"),
                    "\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n"
                            + ToStringBuilder.reflectionToString(expected));
            List<MediaFile> mediaFiles = medias.getValue();
            assertEquals(3, mediaFiles.size());
        }

        @Test
        public void testImportFromPLS() throws Exception {
            final String username = "testUser";
            final String playlistName = "test-playlist";
            File mf1 = new File(tempDir, "PLS-mf1");
            FileUtils.touch(mf1);
            File mf2 = new File(tempDir, "PLS-mf2");
            FileUtils.touch(mf2);
            File mf3 = new File(tempDir, "PLS-mf3");
            FileUtils.touch(mf3);
            StringBuilder builder = new StringBuilder(40);
            builder.append("[playlist]\nFile1=").append(mf1.toURI().toString()).append("\nFile2=")
                    .append(mf2.toURI().toString()).append("\nFile3=").append(mf3.toURI().toString()).append('\n');

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());
            doAnswer(new MediaFileHasEverything()).when(mediaFileService)
                    .getMediaFile(ArgumentMatchers.any(File.class));

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
            String path = new File("/path/to/" + playlistName + ".pls").toURI().toString();

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

            assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"),
                    "\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n"
                            + ToStringBuilder.reflectionToString(expected));
            List<MediaFile> mediaFiles = medias.getValue();
            assertEquals(3, mediaFiles.size());
        }

        @Test
        public void testImportFromXSPF() throws Exception {
            final String username = "testUser";
            final String playlistName = "test-playlist";
            File mf1 = new File(tempDir, "XSPF-mf1");
            FileUtils.touch(mf1);
            File mf2 = new File(tempDir, "XSPF-mf2");
            FileUtils.touch(mf2);
            File mf3 = new File(tempDir, "XSPF-mf3");
            FileUtils.touch(mf3);
            StringBuilder builder = new StringBuilder(300);
            builder.append(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n<trackList>\n<track><location>")
                    .append(mf1.toURI().toString()).append("</location></track>\n<track><location>")
                    .append(mf2.toURI().toString()).append("</location></track>\n<track><location>")
                    .append(mf3.toURI().toString()).append("</location></track>\n</trackList>\n</playlist>\n");

            doAnswer(new PersistPlayList(23)).when(playlistDao).createPlaylist(ArgumentMatchers.any());
            doAnswer(new MediaFileHasEverything()).when(mediaFileService)
                    .getMediaFile(ArgumentMatchers.any(File.class));

            InputStream inputStream = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
            String path = new File("/path/to/" + playlistName + ".xspf").toURI().toString();

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
            assertTrue(EqualsBuilder.reflectionEquals(actual.getValue(), expected, "created", "changed"),
                    "\n" + ToStringBuilder.reflectionToString(actual.getValue()) + "\n\n did not equal \n\n"
                            + ToStringBuilder.reflectionToString(expected));
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

    private static class MediaFileHasEverything implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocationOnMock) {
            File file = invocationOnMock.getArgument(0);
            MediaFile mediaFile = new MediaFile();
            mediaFile.setPath(file.getPath());
            return mediaFile;
        }
    }

}
