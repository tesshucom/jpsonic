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

package org.airsonic.player.dao;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.airsonic.player.domain.MusicFolder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test of {@link MusicFolderDao}.
 *
 * @author Sindre Mehus
 */
public class MusicFolderDaoTest extends DaoTestBase {

    @Autowired
    MusicFolderDao musicFolderDao;

    @Before
    public void setUp() {
        getJdbcTemplate().execute("delete from music_folder");
    }

    @Test
    public void testCreateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    @Test
    public void testUpdateMusicFolder() {
        MusicFolder musicFolder = new MusicFolder(new File("path"), "name", true, new Date());
        musicFolderDao.createMusicFolder(musicFolder);
        musicFolder = musicFolderDao.getAllMusicFolders().get(0);

        musicFolder.setPath(new File("newPath"));
        musicFolder.setName("newName");
        musicFolder.setEnabled(false);
        musicFolder.setChanged(new Date(234234L));
        musicFolderDao.updateMusicFolder(musicFolder);

        MusicFolder newMusicFolder = musicFolderDao.getAllMusicFolders().get(0);
        assertMusicFolderEquals(musicFolder, newMusicFolder);
    }

    @Test
    public void testDeleteMusicFolder() {
        assertEquals("Wrong number of music folders.", 0, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals("Wrong number of music folders.", 1, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.createMusicFolder(new MusicFolder(new File("path"), "name", true, new Date()));
        assertEquals("Wrong number of music folders.", 2, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals("Wrong number of music folders.", 1, musicFolderDao.getAllMusicFolders().size());

        musicFolderDao.deleteMusicFolder(musicFolderDao.getAllMusicFolders().get(0).getId());
        assertEquals("Wrong number of music folders.", 0, musicFolderDao.getAllMusicFolders().size());
    }

    private void assertMusicFolderEquals(MusicFolder expected, MusicFolder actual) {
        assertEquals("Wrong name.", expected.getName(), actual.getName());
        assertEquals("Wrong path.", expected.getPath(), actual.getPath());
        assertEquals("Wrong enabled state.", expected.isEnabled(), actual.isEnabled());
        assertEquals("Wrong changed date.", expected.getChanged(), actual.getChanged());
    }

}
