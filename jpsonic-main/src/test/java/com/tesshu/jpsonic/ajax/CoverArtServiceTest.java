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

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class CoverArtServiceTest extends AbstractNeedsScan {

    private static final String TEST_IMAGE_URL = "https://avatars.githubusercontent.com/u/44695789?s=200&v=4";

    @Autowired
    private CoverArtService coverArtService;

    @Autowired
    private MediaFileDao mediaFileDao;

    private List<MusicFolder> musicFolders;

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = Arrays
                    .asList(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 1, false));
        }
        return musicFolders;
    }

    @Test
    void testSaveCoverArtImage() {
        MediaFile album = mediaFileDao.getNewestAlbums(0, 1, musicFolders).get(0);
        String msg = coverArtService.saveCoverArtImage(album.getId(), TEST_IMAGE_URL);
        assertNull(msg);
        // Failed to create image file backup....

        msg = coverArtService.saveCoverArtImage(album.getId(), TEST_IMAGE_URL);
        assertNull(msg);
        // Failed to create image file backup....
    }

    @Test
    void testRenameWithoutReplacement(@TempDir Path tmpDir) throws IOException, URISyntaxException {

        Path res = Path.of(CoverArtServiceTest.class.getResource("/MEDIAS/Metadata/coverart/cover.jpg").toURI());
        Path coverArt = Path.of(tmpDir.toString(), "cover.jpg");
        Files.copy(res, coverArt);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setPathString(tmpDir.toString());
        mediaFile.setCoverArtPathString(coverArt.toString());

        assertTrue(Files.exists(coverArt));
        coverArtService.renameWithoutReplacement(mediaFile, Path.of(tmpDir.toString(), "dummy.jpg"));
        Path moved = Path.of(tmpDir.toString(), "cover.jpg.old");
        assertFalse(Files.exists(coverArt));
        assertTrue(Files.exists(moved));

        FileUtil.deleteIfExists(moved);
    }
}
