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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor.composite;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FolderArtistTest {

    @Test
    void testCreateCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Artist artist = new Artist();
        artist.setId(88);
        CompositeModel folderArtist = new FolderArtist(folder, artist);
        assertEquals("far:99;88", folderArtist.createCompositeId());
    }

    @Test
    void testIsCompositeId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Artist artist = new Artist();
        artist.setId(88);
        CompositeModel folderArtist = new FolderArtist(folder, artist);
        assertTrue(FolderArtist.isCompositeId(folderArtist.createCompositeId()));
    }

    @Test
    void testParseFolderId() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Artist artist = new Artist();
        artist.setId(88);
        CompositeModel folderArtist = new FolderArtist(folder, artist);
        assertEquals(99, FolderArtist.parseFolderId(folderArtist.createCompositeId()));
    }

    @Test
    void testParseGenreName() {
        MusicFolder folder = new MusicFolder(99, "path", "name", true, null, 0, false);
        Artist artist = new Artist();
        artist.setId(88);
        CompositeModel folderArtist = new FolderArtist(folder, artist);
        assertEquals(88, FolderArtist.parseArtistId(folderArtist.createCompositeId()));
    }
}
