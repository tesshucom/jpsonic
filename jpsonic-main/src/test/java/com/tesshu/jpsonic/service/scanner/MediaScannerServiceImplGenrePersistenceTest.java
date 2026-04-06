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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.service.settings.SKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MediaScannerServiceImplGenrePersistenceTest extends AbstractNeedsScan {

    @Autowired
    private SearchService searchService;

    private final List<MusicFolder> folders = List
        .of(new MusicFolder(1, resolveBaseMediaPath("MultiGenre"), "MultiGenre", true, now(), 0,
                false));

    @Override
    public List<MusicFolder> getMusicFolders() {
        return folders;
    }

    @BeforeEach
    void setup() throws IOException {
        populateDatabase();
    }

    /*
     * Confirm that the Genre Count does not increase or decrease during Normal-Scan
     * and Scan with IgnoreTimestamp.
     */
    @Test
    void testGenreCountPersistence() throws IOException {
        GenreMasterCriteria albumGenreCriteria = new GenreMasterCriteria(folders, Scope.ALBUM,
                Sort.NAME);
        GenreMasterCriteria songGenreCriteria = new GenreMasterCriteria(folders, Scope.SONG,
                Sort.NAME);
        assertTrue(assertAlbumGenreCount(
                searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
        assertTrue(assertSongGenreCount(
                searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));

        // Run a scan
        TestCaseUtils.execScan(mediaScannerService);
        assertTrue(assertAlbumGenreCount(
                searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
        assertTrue(assertSongGenreCount(
                searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));

        // Scan with IgnoreFileTimestamps enabled
        settingsFacade.commit(SKeys.musicFolder.scan.ignoreFileTimestamps, true);
        TestCaseUtils.execScan(mediaScannerService);
        assertTrue(assertAlbumGenreCount(
                searchService.getGenres(albumGenreCriteria, 0, Integer.MAX_VALUE)));
        assertTrue(assertSongGenreCount(
                searchService.getGenres(songGenreCriteria, 0, Integer.MAX_VALUE)));
    }

    private boolean assertAlbumGenreCount(List<Genre> genres) {
        assertEquals(14, genres.size());
        assertEquals("Audiobook - Historical", genres.get(0).getName());
        assertEquals(1, genres.get(0).getAlbumCount());
        assertEquals("Audiobook - Sports", genres.get(1).getName());
        assertEquals(1, genres.get(1).getAlbumCount());
        assertEquals("GENRE_A", genres.get(2).getName());
        assertEquals(1, genres.get(2).getAlbumCount());
        assertEquals("GENRE_B", genres.get(3).getName());
        assertEquals(1, genres.get(3).getAlbumCount());
        assertEquals("GENRE_C", genres.get(4).getName());
        assertEquals(1, genres.get(4).getAlbumCount());
        assertEquals("GENRE_D", genres.get(5).getName());
        assertEquals(2, genres.get(5).getAlbumCount());
        assertEquals("GENRE_E", genres.get(6).getName());
        assertEquals(1, genres.get(6).getAlbumCount());
        assertEquals("GENRE_F", genres.get(7).getName());
        assertEquals(1, genres.get(7).getAlbumCount());
        assertEquals("GENRE_G", genres.get(8).getName());
        assertEquals(1, genres.get(8).getAlbumCount());
        assertEquals("GENRE_H", genres.get(9).getName());
        assertEquals(1, genres.get(9).getAlbumCount());
        assertEquals("GENRE_I", genres.get(10).getName());
        assertEquals(1, genres.get(10).getAlbumCount());
        assertEquals("GENRE_J", genres.get(11).getName());
        assertEquals(1, genres.get(11).getAlbumCount());
        assertEquals("GENRE_K", genres.get(12).getName());
        assertEquals(2, genres.get(12).getAlbumCount());
        assertEquals("GENRE_L", genres.get(13).getName());
        assertEquals(2, genres.get(13).getAlbumCount());
        return true;
    }

    private boolean assertSongGenreCount(List<Genre> genres) {
        assertEquals(15, genres.size());
        assertEquals("Audiobook - Historical", genres.get(0).getName());
        assertEquals(1, genres.get(0).getSongCount());
        assertEquals("Audiobook - Sports", genres.get(1).getName());
        assertEquals(1, genres.get(1).getSongCount());
        assertEquals("GENRE_A", genres.get(2).getName());
        assertEquals(2, genres.get(2).getSongCount());
        assertEquals("GENRE_B", genres.get(3).getName());
        assertEquals(1, genres.get(3).getSongCount());
        assertEquals("GENRE_C", genres.get(4).getName());
        assertEquals(1, genres.get(4).getSongCount());
        assertEquals("GENRE_D", genres.get(5).getName());
        assertEquals(2, genres.get(5).getSongCount());
        assertEquals("GENRE_E", genres.get(6).getName());
        assertEquals(2, genres.get(6).getSongCount());
        assertEquals("GENRE_F", genres.get(7).getName());
        assertEquals(2, genres.get(7).getSongCount());
        assertEquals("GENRE_G", genres.get(8).getName());
        assertEquals(1, genres.get(8).getSongCount());
        assertEquals("GENRE_H", genres.get(9).getName());
        assertEquals(1, genres.get(9).getSongCount());
        assertEquals("GENRE_I", genres.get(10).getName());
        assertEquals(1, genres.get(10).getSongCount());
        assertEquals("GENRE_J", genres.get(11).getName());
        assertEquals(1, genres.get(11).getSongCount());
        assertEquals("GENRE_K", genres.get(12).getName());
        assertEquals(2, genres.get(12).getSongCount());
        assertEquals("GENRE_L", genres.get(13).getName());
        assertEquals(2, genres.get(13).getSongCount());
        assertEquals("NO_ALBUM", genres.get(14).getName());
        assertEquals(1, genres.get(14).getSongCount());
        return true;
    }
}
