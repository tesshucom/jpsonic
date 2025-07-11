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
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

/*
 * The query syntax has not changed significantly since Lucene 1.3. (A slight difference) If you
 * face a problem reaping from 3.x to 7.x It may be faster to look at the query than to look at the
 * API.
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

    private static final MusicFolder MUSIC_FOLDER1 = new MusicFolder(FID1, PATH1, "music1", true,
            now(), 0, false);
    private static final MusicFolder MUSIC_FOLDER2 = new MusicFolder(FID2, PATH2, "music2", true,
            now(), 1, false);

    private static final List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    private static final List<MusicFolder> MULTI_FOLDERS = Arrays
        .asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    private SettingsService settingsService;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        queryFactory = new QueryFactory(settingsService, new AnalyzerFactory(settingsService));
    }

    @Nested
    @Order(1)
    class FilterFieldsTest {

        @Test
        void testComposer() {
            String[] notContainsComposer = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING };
            assertArrayEquals(notContainsComposer,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), false)
                        .toArray(new String[0]));

            Mockito.when(settingsService.isSearchComposer()).thenReturn(true);
            String[] containsComposer = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING, //
                    FieldNamesConstants.COMPOSER, //
                    FieldNamesConstants.COMPOSER_READING };
            assertArrayEquals(containsComposer,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), true)
                        .toArray(new String[0]));
        }

        @Test
        void testScheme() {

            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.NATIVE_JAPANESE.name());
            String[] notRomanize = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING };
            assertArrayEquals(notRomanize,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), false)
                        .toArray(new String[0]));

            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
            assertArrayEquals(notRomanize,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), false)
                        .toArray(new String[0]));

            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
            String[] romanize = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING,
                    FieldNamesConstants.ARTIST_READING_ROMANIZED };
            assertArrayEquals(romanize,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), false)
                        .toArray(new String[0]));

            // and Composer
            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.NATIVE_JAPANESE.name());
            String[] notRomanizeAndCmp = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING, //
                    FieldNamesConstants.COMPOSER, //
                    FieldNamesConstants.COMPOSER_READING };
            assertArrayEquals(notRomanizeAndCmp,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), true)
                        .toArray(new String[0]));

            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
            assertArrayEquals(notRomanizeAndCmp,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), true)
                        .toArray(new String[0]));

            Mockito
                .when(settingsService.getIndexSchemeName())
                .thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
            String[] romanizeAndCmp = { FieldNamesConstants.TITLE, //
                    FieldNamesConstants.TITLE_READING, //
                    FieldNamesConstants.ARTIST, //
                    FieldNamesConstants.ARTIST_READING, //
                    FieldNamesConstants.ARTIST_READING_ROMANIZED, //
                    FieldNamesConstants.COMPOSER, //
                    FieldNamesConstants.COMPOSER_READING, //
                    FieldNamesConstants.COMPOSER_READING_ROMANIZED };
            assertArrayEquals(romanizeAndCmp,
                    queryFactory
                        .filterFields(IndexType.SONG.getFields(), true)
                        .toArray(new String[0]));
        }
    }

    /**
     * Related {@link UPnPSearchCriteriaDirectorTest}
     */
    @Test
    @Order(2)
    void testCreatePhraseQuery() throws IOException {
        assertEquals(
                "(tit:\"cats and dogs\"~1)^6.0 (art:\"cats and dogs\"~1)^4.0 (artR:\"cats and dogs\"~1)^4.2",
                queryFactory
                    .createPhraseQuery(IndexType.SONG.getFields(), "Cats and Dogs", IndexType.SONG)
                    .get()
                    .toString());
        assertEquals(
                "(tit:\"いぬ と ねこ\"~1)^6.0 (titR:\"いぬ ぬと とね ねこ\"~1)^6.2 (art:\"いぬ と ねこ\"~1)^4.0 (artR:\"いぬ ぬと とね ねこ\"~1)^4.2",
                queryFactory
                    .createPhraseQuery(IndexType.SONG.getFields(), "いぬとねこ", IndexType.SONG)
                    .get()
                    .toString());
    }

    @Test
    @Order(3)
    void testSearchByPhrase() throws IOException {

        assertEquals(
                "+((tit:\"cats and dogs\"~1)^6.0 (art:\"cats and dogs\"~1)^4.0 (artR:\"cats and dogs\"~1)^4.2) +(f:"
                        + PATH1 + ")",
                queryFactory
                    .searchByPhrase("Cats and Dogs", false, SINGLE_FOLDERS, IndexType.SONG)
                    .toString());
        assertEquals(
                "+((alb:\"cats and dogs\"~1)^4.0 art:\"cats and dogs\"~1 (artR:\"cats and dogs\"~1)^2.2) +(fId:"
                        + FID1 + ")",
                queryFactory
                    .searchByPhrase("Cats and Dogs", false, SINGLE_FOLDERS, IndexType.ALBUM_ID3)
                    .toString());
        assertEquals(
                "+(art:\"cats and dogs\"~1 (artR:\"cats and dogs\"~1)^2.2) +(fId:" + FID1 + ")",
                queryFactory
                    .searchByPhrase("Cats and Dogs", false, SINGLE_FOLDERS, IndexType.ARTIST_ID3)
                    .toString());
        assertEquals(
                "+((tit:\"いぬ と ねこ\"~1)^6.0 (titR:\"いぬ ぬと とね ねこ\"~1)^6.2 (art:\"いぬ と ねこ\"~1)^4.0 (artR:\"いぬ ぬと とね ねこ\"~1)^4.2)"
                        + " +(f:" + PATH1 + ")",
                queryFactory
                    .searchByPhrase("いぬとねこ", false, SINGLE_FOLDERS, IndexType.SONG)
                    .toString());

        Mockito
            .when(settingsService.getIndexSchemeName())
            .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
        queryFactory = new QueryFactory(settingsService, new AnalyzerFactory(settingsService));
        assertEquals(
                "+((tit:\"い ぬ と ね こ\"~1)^6.0 (titR:\"いぬ ぬと とね ねこ\"~1)^6.2 (art:\"い ぬ と ね こ\"~1)^4.0 (artR:\"いぬ ぬと とね ねこ\"~1)^4.2) +(f:"
                        + PATH1 + ")",
                queryFactory
                    .searchByPhrase("いぬとねこ", false, SINGLE_FOLDERS, IndexType.SONG)
                    .toString());

        Mockito
            .when(settingsService.getIndexSchemeName())
            .thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
        queryFactory = new QueryFactory(settingsService, new AnalyzerFactory(settingsService));
        String query = "Inu to Neko";

        assertEquals("+((tit:\"inu to neko\"~1)^6.0 " //
                + "(art:\"inu to neko\"~1)^4.0 " //
                + "(artR:\"inu to neko\"~1)^4.2 " //
                + "(artRR:inu artRR:to artRR:neko)^4.2) " //
                + "+(f:" + PATH1 + ")",
                queryFactory
                    .searchByPhrase(query, false, SINGLE_FOLDERS, IndexType.SONG)
                    .toString());

        Mockito.when(settingsService.isSearchComposer()).thenReturn(true);
        assertEquals("+((tit:\"inu to neko\"~1)^6.0 " //
                + "(art:\"inu to neko\"~1)^4.0 " //
                + "(artR:\"inu to neko\"~1)^4.2 " //
                + "(artRR:inu artRR:to artRR:neko)^4.2 " //
                + "cmp:\"inu to neko\"~1 " //
                + "(cmpR:\"inu to neko\"~1)^2.2 " //
                + "(cmpRR:inu cmpRR:to cmpRR:neko)^2.2) " //
                + "+(f:" + PATH1 + ")",
                queryFactory
                    .searchByPhrase(query, false, SINGLE_FOLDERS, IndexType.SONG)
                    .toString());
    }

    @Test
    @Order(4)
    void testGetRandomSongs() throws IOException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"),
                1900, 2000, SINGLE_FOLDERS);
        Query query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + ")",
                query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), 1900, 2000,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(
                "+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, null,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), 1900, null,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2147483647] +(f:" + PATH1 + " f:"
                + PATH2 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, 2000,
                MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +(g:Classic Rock) +y:[-2147483648 TO 2000] +(f:" + PATH1 + " f:"
                + PATH2 + ")", query.toString());

        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock", "Rock & Roll"), 1900,
                2000, SINGLE_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(
                "+m:MUSIC +(g:Classic Rock g:Rock & Roll) +y:[1900 TO 2000] +(f:" + PATH1 + ")",
                query.toString(), "multi genre");

        criteria = new RandomSearchCriteria(50, null, 1900, 2000, SINGLE_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("+m:MUSIC +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString(),
                "null genre");
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
    void testGetRandomSongsByMusicFolderAndGenre() {
        Query query = queryFactory.getRandomSongs(SINGLE_FOLDERS, "Rock & Roll", "Pop/Funk");
        assertEquals("+m:MUSIC +(f:" + PATH1 + ") +(g:Rock & Roll g:Pop/Funk)", query.toString());
        query = queryFactory.getRandomSongs(MULTI_FOLDERS, "Rock & Roll", "Pop/Funk");
        assertEquals("+m:MUSIC +(f:" + PATH1 + " f:" + PATH2 + ") +(g:Rock & Roll g:Pop/Funk)",
                query.toString());
    }

    @Order(7)
    @Test
    void testGetRandomAlbums() {
        Query query = queryFactory.getRandomAlbums(SINGLE_FOLDERS);
        assertEquals("(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getRandomAlbums(MULTI_FOLDERS);
        assertEquals("(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Order(8)
    @Test
    void testGetRandomAlbumsId3() {
        Query query = queryFactory.getRandomAlbumsId3(SINGLE_FOLDERS);
        assertEquals("(fId:" + FID1 + ")", query.toString());
        query = queryFactory.getRandomAlbumsId3(MULTI_FOLDERS);
        assertEquals("(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Order(9)
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

    @Order(10)
    @Test
    void testGetMediasByGenre() throws IOException {
        Query query = queryFactory.getMediasByGenres("Instrumental pop", SINGLE_FOLDERS);
        assertEquals("+(g:Instrumental pop) +(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Rock & Roll", MULTI_FOLDERS);
        assertEquals("+(g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk", MULTI_FOLDERS);
        assertEquals("+(g:Pop g:Pop/Funk) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk;Rock & Roll", MULTI_FOLDERS);
        assertEquals("+(g:Pop g:Pop/Funk g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString(), "multi genre");
        query = queryFactory.getMediasByGenres("", MULTI_FOLDERS);
        assertEquals("+(f:" + PATH1 + " f:" + PATH2 + ")", query.toString(), "null genre");
    }

    @Order(11)
    @Test
    void testToPreAnalyzedGenres() throws IOException {
        Query query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock"));
        assertEquals("+(g:Classic Rock)", query.toString(), "genre");
        query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock", "Rock & Roll"));
        assertEquals("+(g:Classic Rock g:Rock & Roll)", query.toString(), "multi genres");
        query = queryFactory.toPreAnalyzedGenres(Arrays.asList(""));
        assertEquals("+()", query.toString(), "multi genres");
    }

    @Order(12)
    @Test
    void testGetGenre() throws IOException {
        assertEquals("g:Classic Rock", queryFactory.getGenre("Classic Rock").toString());
    }

}
