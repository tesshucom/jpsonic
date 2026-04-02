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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.persistence.api.repository;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.settings.SKeys;
import com.tesshu.jpsonic.service.settings.SettingsFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Even if the timestamp is ignored when scanning, it is confirmed that Created
 * does not change.
 */
class AlbumDaoCreatedPersistenceTest extends AbstractNeedsScan {

    static final String ADMIN_NAME = "admin";

    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private SettingsFacade settingsFacade;

    private final List<MusicFolder> folders = List
        .of(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 0, false));

    @Override
    public List<MusicFolder> getMusicFolders() {
        return folders;
    }

    @BeforeEach
    void setup() throws IOException {
        populateDatabase();
    }

    @Test
    void testCreatedPersistence() throws IOException, InterruptedException {

        // Checking registered data
        List<Album> albums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
        assertEquals(4, albums.size());

        // Run a scan
        TestCaseUtils.execScan(mediaScannerService);

        List<Album> scanedAlbums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
        assertEquals(4, scanedAlbums.size());
        assertEquals(albums.get(0).getCreated(), scanedAlbums.get(0).getCreated());
        assertEquals(albums.get(1).getCreated(), scanedAlbums.get(1).getCreated());
        assertEquals(albums.get(2).getCreated(), scanedAlbums.get(2).getCreated());
        assertEquals(albums.get(3).getCreated(), scanedAlbums.get(3).getCreated());

        // Scan with IgnoreFileTimestamps enabled
        settingsFacade.commit(SKeys.musicFolder.scan.ignoreFileTimestamps, true);
        TestCaseUtils.execScan(mediaScannerService);

        /*
         * Nothing changes. album#created comes from file#changed. (The Newest in ID3
         * indicates the most recent Change. This has been the case since legacy
         * servers.) Therefore, regardless of the scan logic, this value will not change
         * unless the file is changed.
         */
        List<Album> fullScanedAlbums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
        assertEquals(4, fullScanedAlbums.size());
        assertEquals(albums.get(0).getCreated(), fullScanedAlbums.get(0).getCreated());
        assertEquals(albums.get(1).getCreated(), fullScanedAlbums.get(1).getCreated());
        assertEquals(albums.get(2).getCreated(), fullScanedAlbums.get(2).getCreated());
        assertEquals(albums.get(3).getCreated(), fullScanedAlbums.get(3).getCreated());
    }
}
