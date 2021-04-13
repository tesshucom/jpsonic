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

import java.io.File;
import java.io.IOException;

import org.airsonic.player.NeedsHome;
import org.airsonic.player.dao.MusicFolderDao;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
public class LegacyDatabaseStartupTest {

    @Autowired
    private MusicFolderDao musicFolderDao;

    @BeforeAll
    public static void beforeAll() throws IOException {
        String homePath = System.getProperty("jpsonic.home");
        File dbDirectory = new File(homePath, "/db");
        FileUtils.forceMkdir(dbDirectory);
        org.airsonic.player.util.FileUtils.copyResourcesRecursively(
                LegacyDatabaseStartupTest.class.getResource("/db/pre-liquibase/db"), dbDirectory);
    }

    @Test
    public void testStartup() {
        assertEquals(1, musicFolderDao.getAllMusicFolders().size());
    }

}
