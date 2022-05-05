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

package com.tesshu.jpsonic.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FileUtilTest {

    @Test
    void testGetShortPath() {

        assertNull(FileUtil.getShortPath(null));

        assertEquals("", FileUtil.getShortPath(Path.of("/").toFile()));

        assertEquals(File.separator + "child.mp3", FileUtil.getShortPath(Path.of("/child.mp3").toFile()));

        assertEquals("MusicFolder" + File.separator + "artist",
                FileUtil.getShortPath(Path.of("/MusicFolder/artist").toFile()));

        assertEquals("artist" + File.separator + "child.mp3",
                FileUtil.getShortPath(Path.of("/MusicFolder/artist/child.mp3").toFile()));
    }
}
