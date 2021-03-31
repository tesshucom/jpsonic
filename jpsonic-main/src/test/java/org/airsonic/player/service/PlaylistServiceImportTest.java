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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.dao.JPlaylistDao;
import org.airsonic.player.dao.DaoHelper;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.playlist.DefaultPlaylistImportHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PlaylistServiceImportTest {

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
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(ArgumentMatchers.any(File.class));

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
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(ArgumentMatchers.any(File.class));

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
        doAnswer(new MediaFileHasEverything()).when(mediaFileService).getMediaFile(ArgumentMatchers.any(File.class));

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
