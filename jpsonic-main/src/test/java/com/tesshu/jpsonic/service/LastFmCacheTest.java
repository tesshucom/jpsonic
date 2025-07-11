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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LastFmCacheTest {

    private LastFmCache lastFmCache;
    private Path tempDir;

    @BeforeEach
    public void setup(@TempDir Path tempDir)
            throws ExecutionException, IOException, URISyntaxException {
        this.tempDir = tempDir;
        lastFmCache = new LastFmCache(tempDir, 1L);
        Path dummyCache = Path.of(tempDir.toString(), "jpsonic.log");
        if (!Files.exists(dummyCache)) {
            dummyCache = Files.createFile(dummyCache);
            Path dummySource = Path.of(LastFmCacheTest.class.getResource("/banner.txt").toURI());
            Files.copy(dummySource, dummyCache, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    void testClear() throws IOException {
        assertEquals(1, Files.list(tempDir).count());
        lastFmCache.clear();
        assertEquals(0, Files.list(tempDir).count());
    }
}
