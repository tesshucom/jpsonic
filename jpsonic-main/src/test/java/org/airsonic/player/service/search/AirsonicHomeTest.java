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

package org.airsonic.player.service.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.MusicFolderTestData;

/**
 * Test case interface for scanning MusicFolder.
 */
public interface AirsonicHomeTest {

    /**
     * MusicFolder used by test class.
     *
     * @return MusicFolder used by test class
     */
    default List<MusicFolder> getMusicFolders() {
        return MusicFolderTestData.getTestMusicFolders();
    }

    /**
     * Whether the data input has been completed.
     *
     * @return Static AtomicBoolean indicating whether the data injection has been completed
     */
    abstract AtomicBoolean dataBasePopulated();

    /**
     * Whether the data input has been completed.
     *
     * @return Static AtomicBoolean indicating whether the data injection has been completed
     */
    abstract AtomicBoolean dataBaseReady();

    /**
     * Populate the database only once. It is called in the @Before granted method.
     */
    void populateDatabaseOnlyOnce();

}
