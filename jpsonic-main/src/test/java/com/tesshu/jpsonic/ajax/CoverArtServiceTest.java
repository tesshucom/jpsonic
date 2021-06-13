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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath("Music"));
            musicFolders.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
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
}
