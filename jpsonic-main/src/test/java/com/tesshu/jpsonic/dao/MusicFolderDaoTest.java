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

package com.tesshu.jpsonic.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Date;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test of {@link MusicFolderDao}.
 *
 * @author Sindre Mehus
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class MusicFolderDaoTest {

    @Autowired
    private GenericDaoHelper daoHelper;

    @Autowired
    private MusicFolderDao musicFolderDao;

    @BeforeEach
    public void setUp() {
        daoHelper.getJdbcTemplate().execute("delete from music_folder");
    }

    @Test
    void testCreateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    @Test
    void testUpdateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        musicFolder = musicFolderDao.getAllMusicFolders().get(0);

        musicFolder.setPath(new File("newPath"));
        musicFolder.setName("newName");
        musicFolder.setEnabled(false);
        musicFolder.setChanged(new Date(234_234L));
        musicFolderDao.updateMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    @Test
    void testDeleteMusicFolder() {
        assertEquals(0, musicFolderDao.getAllMusicFolders().size(), "Wrong number of music folders.");

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals(1, musicFolderDao.getAllMusicFolders().size(), "Wrong number of music folders.");

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals(2, musicFolderDao.getAllMusicFolders().size(), "Wrong number of music folders.");

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals(1, musicFolderDao.getAllMusicFolders().size(), "Wrong number of music folders.");

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals(0, musicFolderDao.getAllMusicFolders().size(), "Wrong number of music folders.");
    }

    private void assertMusicFolderEquals(MusicFolder expected, MusicFolder actual) {
        assertEquals(expected.getName(), actual.getName(), "Wrong name.");
        assertEquals(expected.getPath(), actual.getPath(), "Wrong path.");
        assertEquals(expected.isEnabled(), actual.isEnabled(), "Wrong enabled state.");
        assertEquals(expected.getChanged(), actual.getChanged(), "Wrong changed date.");
    }

}
