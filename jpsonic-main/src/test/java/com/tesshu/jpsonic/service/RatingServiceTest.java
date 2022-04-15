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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RatingServiceTest {

    private RatingDao ratingDao;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private RatingService ratingService;

    @BeforeEach
    public void setup() {
        ratingDao = mock(RatingDao.class);
        securityService = mock(SecurityService.class);
        mediaFileService = mock(MediaFileService.class);
        ratingService = new RatingService(ratingDao, securityService, mediaFileService);
    }

    @Test
    void testGetHighestRatedAlbums() throws IOException, URISyntaxException {
        Path albumPath = Path.of(RatingServiceTest.class.getResource("/MEDIAS/Music/_DIR_ Sixteen Horsepower").toURI());
        Mockito.when(ratingDao.getHighestRatedAlbums(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyList()))
                .thenReturn(Arrays.asList(albumPath.toString()));
        Mockito.when(securityService.isReadAllowed(Mockito.any(File.class))).thenReturn(true);
        MediaFile album = new MediaFile();
        album.setPathString(albumPath.toString());
        Mockito.when(mediaFileService.getMediaFile(albumPath.toString())).thenReturn(album);

        MusicFolder musicFolder = new MusicFolder(0, Path.of("path").toFile(), "Music", true, new Date());
        List<MusicFolder> musicFolders = Arrays.asList(musicFolder);
        List<MediaFile> highestRatedAlbums = ratingService.getHighestRatedAlbums(0, 0, musicFolders);
        assertEquals(1, highestRatedAlbums.size());
        assertEquals(albumPath.toString(), highestRatedAlbums.get(0).getPathString());
    }
}
