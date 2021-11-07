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

package com.tesshu.jpsonic.service.search;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/*
 * The query syntax has not changed significantly since Lucene 1.3.
 * (A slight difference)
 * If you face a problem reaping from 3.x to 7.x
 * It may be faster to look at the query than to look at the API.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryFactoryTest {

    private QueryFactory queryFactory;

    private static final String SEPA = System.getProperty("file.separator");

    private static final String PATH1 = SEPA + "var" + SEPA + "music1";
    private static final String PATH2 = SEPA + "var" + SEPA + "music2";

    private static final int FID1 = 10;
    private static final int FID2 = 20;

    private static final MusicFolder MUSIC_FOLDER1 = new MusicFolder(FID1, new File(PATH1), "music1", true,
            new java.util.Date());
    private static final MusicFolder MUSIC_FOLDER2 = new MusicFolder(FID2, new File(PATH2), "music2", true,
            new java.util.Date());

    private static final List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    private static final List<MusicFolder> MULTI_FOLDERS = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    @BeforeEach
    public void setup() {
        queryFactory = new QueryFactory(new AnalyzerFactory(mock(SettingsService.class)), null);
    }

    @Test
    @Order(4)
    void testGetRandomSongs() throws IOException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), 1900, 2000,
                SINGLE_FOLDERS);
        Query query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), 1900, 2000, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), 1900, null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2147483647] +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, 2000, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[-2147483648 TO 2000] +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());

        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock", "Rock & Roll"), 1900, 2000,
                SINGLE_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock g:Rock & Roll) +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString(),
                "multi genre");

        criteria = new RandomSearchCriteria(50, null, 1900, 2000, SINGLE_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString(), "null genre");
    }

    @Order(5)
    @Test
    void testGetRandomSongsByMusicFolder() {
        Query query = queryFactory.getRandomSongs(SINGLE_FOLDERS);
        assertEquals("+m:MUSIC +(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getRandomSongs(MULTI_FOLDERS);
        assertEquals("+m:MUSIC +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Order(6)
    @Test
    void testGetRandomAlbums() {
        Query query = queryFactory.getRandomAlbums(SINGLE_FOLDERS);
        assertEquals("(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getRandomAlbums(MULTI_FOLDERS);
        assertEquals("(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Order(7)
    @Test
    void testGetRandomAlbumsId3() {
        Query query = queryFactory.getRandomAlbumsId3(SINGLE_FOLDERS);
        assertEquals("(fId:" + FID1 + ")", query.toString());
        query = queryFactory.getRandomAlbumsId3(MULTI_FOLDERS);
        assertEquals("(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Order(8)
    @Test
    void testGetAlbumId3sByGenre() throws IOException {
        Query query = queryFactory.getAlbumId3sByGenres("Instrumental pop", SINGLE_FOLDERS);
        assertEquals("+(g:Instrumental pop) +(fId:" + FID1 + ")", query.toString());
        query = queryFactory.getAlbumId3sByGenres("Rock & Roll", MULTI_FOLDERS);
        assertEquals("+(g:Rock & Roll) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        query = queryFactory.getAlbumId3sByGenres("Pop;Pop/Funk", MULTI_FOLDERS);
        assertEquals("+(g:Pop g:Pop/Funk) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        query = queryFactory.getAlbumId3sByGenres("", MULTI_FOLDERS);
        assertEquals("+(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Order(9)
    @Test
    void testGetMediasByGenre() throws IOException {
        Query query = queryFactory.getMediasByGenres("Instrumental pop", SINGLE_FOLDERS);
        assertEquals("+(g:Instrumental pop) +(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Rock & Roll", MULTI_FOLDERS);
        assertEquals("+(g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk", MULTI_FOLDERS);
        assertEquals("+(g:Pop g:Pop/Funk) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk;Rock & Roll", MULTI_FOLDERS);
        assertEquals("+(g:Pop g:Pop/Funk g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString(),
                "multi genre");
        query = queryFactory.getMediasByGenres("", MULTI_FOLDERS);
        assertEquals("+(f:" + PATH1 + " f:" + PATH2 + ")", query.toString(), "null genre");
    }

    @Order(10)
    @Test
    void testToPreAnalyzedGenres() throws IOException {
        Query query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock"));
        assertEquals("+(g:Classic Rock)", query.toString(), "genre");
        query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock", "Rock & Roll"));
        assertEquals("+(g:Classic Rock g:Rock & Roll)", query.toString(), "multi genres");
        query = queryFactory.toPreAnalyzedGenres(Arrays.asList(""));
        assertEquals("+()", query.toString(), "multi genres");
    }

    @Order(11)
    @Test
    void testGetGenre() throws IOException {
        assertEquals("g:Classic Rock", queryFactory.getGenre("Classic Rock").toString());
    }

}
