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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
class MediaFileServiceTest {

    private SecurityService securityService;
    private MediaFileService mediaFileService;

    @BeforeEach
    public void setup() throws URISyntaxException {
        SettingsService settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);
        MediaFileDao mediaFileDao = mock(MediaFileDao.class);
        mediaFileService = new MediaFileService(settingsService, mock(MusicFolderService.class), securityService,
                mock(MediaFileCache.class), mediaFileDao, mock(JpsonicComparators.class));

        Mockito.when(settingsService.getVideoFileTypesAsArray()).thenReturn(new String[0]);
        Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(new String[] { "mp3" });
        Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);
    }

    @Nested
    class FindCoverArtTest {

        private Path createPath(String path) throws URISyntaxException {
            return Path.of(MediaFileServiceTest.class.getResource(path).toURI());
        }

        @Test
        void coverArtFileTypesTest() throws ExecutionException, URISyntaxException {
            // fileNames
            Path path = createPath("/MEDIAS/Metadata/coverart/cover.jpg");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/cover.png");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/cover.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/folder.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // extensions
            path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.jpeg");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.gif");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());
            path = createPath("/MEDIAS/Metadata/coverart/album.png");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // letter case
            path = createPath("/MEDIAS/Metadata/coverart/coveratrt.GIF");
            assertEquals(path.toString(), mediaFileService.findCoverArt(path).get().toString());

            // dir
            path = createPath("/MEDIAS/Metadata/coverart/coveratrt.jpg");
            assertTrue(Files.exists(path));
            assertTrue(mediaFileService.findCoverArt(path).isEmpty());
        }

        @Test
        @DisabledOnOs(OS.LINUX)
        void testIsEmbeddedArtworkApplicableOnWin() throws ExecutionException, URISyntaxException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            // coverArt
            Path parent = createPath("/MEDIAS/Metadata/coverart");
            Path firstChild = createPath("/MEDIAS/Metadata/coverart/album.gif"); // OS dependent
            assertEquals(firstChild, mediaFileService.findCoverArt(parent).get());

            // coverArt
            Path containsEmbeddedFormats = createPath("/MEDIAS/Metadata/v2.4");
            Path embeddedFormat = createPath("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3");
            assertEquals(embeddedFormat, mediaFileService.findCoverArt(containsEmbeddedFormats).get());

            // empty
            Path containsDirOnly = createPath("/MEDIAS/Metadata/tagger3");
            assertTrue(mediaFileService.findCoverArt(containsDirOnly).isEmpty());
        }

        @SuppressWarnings("PMD.DetachedTestCase")
        // @Test
        // @Disabled
        // @DisabledOnOs(OS.WINDOWS)
        void testIsEmbeddedArtworkApplicableOnLinux() throws ExecutionException, URISyntaxException {

            Mockito.when(securityService.isReadAllowed(Mockito.any(Path.class))).thenReturn(true);

            // coverArt
            Path parent = createPath("/MEDIAS/Metadata/coverart");
            Path firstChild = createPath("/MEDIAS/Metadata/coverart/coveratrt.GIF"); // OS dependent
            assertEquals(firstChild, mediaFileService.findCoverArt(parent).get());

            // coverArt
            Path containsEmbeddedFormats = createPath("/MEDIAS/Metadata/v2.4");
            Path embeddedFormat = createPath("/MEDIAS/Metadata/v2.4/Mp3tag2.9.7.mp3");
            assertEquals(embeddedFormat, mediaFileService.findCoverArt(containsEmbeddedFormats).get());

            // empty
            Path containsDirOnly = createPath("/MEDIAS/Metadata/tagger3");
            assertTrue(mediaFileService.findCoverArt(containsDirOnly).isEmpty());
        }
    }
}
