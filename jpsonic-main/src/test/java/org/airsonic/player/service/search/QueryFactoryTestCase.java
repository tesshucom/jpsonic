/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.util.HomeRule;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.search.Query;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
 * The query syntax has not changed significantly since Lucene 1.3.
 * (A slight difference)
 * If you face a problem reaping from 3.x to 7.x
 * It may be faster to look at the query than to look at the API.
 */
@ContextConfiguration(locations = {
        "/applicationContext-service.xml",
        "/applicationContext-cache.xml",
        "/applicationContext-testdb.xml",
        "/applicationContext-mockSonos.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QueryFactoryTestCase {
    
    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private QueryFactory queryFactory;

    private final String QUERY_PATTERN_INCLUDING_KATAKANA = "ネコ ABC";
    private final String QUERY_PATTERN_ALPHANUMERIC_ONLY = "ABC 123";
    private final String QUERY_PATTERN_HIRAGANA_ONLY = "ねこ いぬ";
    private final String QUERY_PATTERN_OTHERS = "ABC ねこ";

    private static final String SEPA = System.getProperty("file.separator");

    private final String PATH1 = SEPA + "var" + SEPA + "music1";
    private final String PATH2 = SEPA + "var" + SEPA + "music2";

    private final int FID1 = 10;
    private final int FID2 = 20;

    private final MusicFolder MUSIC_FOLDER1 = new MusicFolder(Integer.valueOf(FID1), new File(PATH1), "music1", true, new java.util.Date());
    private final MusicFolder MUSIC_FOLDER2 = new MusicFolder(Integer.valueOf(FID2), new File(PATH2), "music2", true, new java.util.Date());

    List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    List<MusicFolder> MULTI_FOLDERS = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    @Test
    public void testSearchArtist() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_PATTERN_INCLUDING_KATAKANA);
        Query query = queryFactory.search(criteria, SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + ")", query.toString());
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_ALPHANUMERIC_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_HIRAGANA_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_OTHERS);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testSearchAlbum() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_PATTERN_INCLUDING_KATAKANA);
        Query query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_ALPHANUMERIC_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria.setQuery(QUERY_PATTERN_HIRAGANA_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_OTHERS);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
    }

    @Test
    public void testSearchSong() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_PATTERN_INCLUDING_KATAKANA);
        Query query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "+(((tit:ネコ*)^2.2 (artR:ねこ*)^1.4 (art:ネコ*)^1.2) ((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria.setQuery(QUERY_PATTERN_ALPHANUMERIC_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_HIRAGANA_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "+(((titEX:ねこいぬ*)^2.3 (tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2) ((tit:いぬ*)^2.2 (artR:いぬ*)^1.4 (art:いぬ*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
        criteria.setQuery(QUERY_PATTERN_OTHERS);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                query.toString());
    }

    @Test
    public void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_PATTERN_INCLUDING_KATAKANA);
        Query query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_ALPHANUMERIC_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_HIRAGANA_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_OTHERS);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Test
    public void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOffset(10);
        criteria.setCount(Integer.MAX_VALUE);
        criteria.setQuery(QUERY_PATTERN_INCLUDING_KATAKANA);
        Query query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_ALPHANUMERIC_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                query.toString());
        criteria.setQuery(QUERY_PATTERN_HIRAGANA_ONLY);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        criteria.setQuery(QUERY_PATTERN_OTHERS);
        query = queryFactory.search(criteria, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                query.toString());
    }

    @Test
    public void testSearchByNameArtist() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_PATTERN_INCLUDING_KATAKANA);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "art:ネコ art:abc*", query.toString());
        query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_PATTERN_ALPHANUMERIC_ONLY);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "art:abc art:123*", query.toString());
        query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_PATTERN_HIRAGANA_ONLY);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "art:ねこ art:いぬ*", query.toString());
        query = queryFactory.searchByName(FieldNames.ARTIST, QUERY_PATTERN_OTHERS);
        assertEquals(QUERY_PATTERN_OTHERS, "art:abc art:ねこ*", query.toString());
    }

    @Test
    public void testSearchByNameAlbum() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_PATTERN_INCLUDING_KATAKANA);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "alb:ネコ alb:abc*", query.toString());
        query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_PATTERN_ALPHANUMERIC_ONLY);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "alb:abc alb:123*", query.toString());
        query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_PATTERN_HIRAGANA_ONLY);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "alb:ねこ alb:いぬ*", query.toString());
        query = queryFactory.searchByName(FieldNames.ALBUM, QUERY_PATTERN_OTHERS);
        assertEquals(QUERY_PATTERN_OTHERS, "alb:abc alb:ねこ*", query.toString());
    }

    @Test
    public void testSearchByNameTitle() throws IOException {
        Query query = queryFactory.searchByName(FieldNames.TITLE, QUERY_PATTERN_INCLUDING_KATAKANA);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA, "tit:ネコ tit:abc*", query.toString());
        query = queryFactory.searchByName(FieldNames.TITLE, QUERY_PATTERN_ALPHANUMERIC_ONLY);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "tit:abc tit:123*", query.toString());
        query = queryFactory.searchByName(FieldNames.TITLE, QUERY_PATTERN_HIRAGANA_ONLY);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY, "tit:ねこ tit:いぬ*", query.toString());
        query = queryFactory.searchByName(FieldNames.TITLE, QUERY_PATTERN_OTHERS);
        assertEquals(QUERY_PATTERN_OTHERS, "tit:abc tit:ねこ*", query.toString());
    }

    @Test
    public void testGetRandomSongs() throws IOException {
        RandomSearchCriteria criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), Integer.valueOf(1900), Integer.valueOf(2000), SINGLE_FOLDERS);
        Query query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria), "+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), Integer.valueOf(1900), Integer.valueOf(2000), MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria), "+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2000] +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria), "+m:MUSIC +(g:Classic Rock) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), Integer.valueOf(1900), null, MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria), "+m:MUSIC +(g:Classic Rock) +y:[1900 TO 2147483647] +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock"), null, Integer.valueOf(2000), MULTI_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals(ToStringBuilder.reflectionToString(criteria), "+m:MUSIC +(g:Classic Rock) +y:[-2147483648 TO 2000] +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());

        criteria = new RandomSearchCriteria(50, Arrays.asList("Classic Rock", "Rock & Roll"), Integer.valueOf(1900), Integer.valueOf(2000), SINGLE_FOLDERS);
        query = queryFactory.getRandomSongs(criteria);
        assertEquals("multi genre", "+m:MUSIC +(g:Classic Rock g:Rock & Roll) +y:[1900 TO 2000] +(f:" + PATH1 + ")", query.toString());
    }

    @Test
    public void testGetRandomAlbums() {
        Query query = queryFactory.getRandomAlbums(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS), "(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getRandomAlbums(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testGetRandomAlbumsId3() {
        Query query = queryFactory.getRandomAlbumsId3(SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS), "(fId:" + FID1 + ")", query.toString());
        query = queryFactory.getRandomAlbumsId3(MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Test
    public void testGetMediasByGenre() throws IOException {
        Query query = queryFactory.getMediasByGenres("Instrumental pop", SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS), "+(g:Instrumental pop) +(f:" + PATH1 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Rock & Roll", MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "+(g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk", MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "+(g:Pop g:Pop/Funk) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
        query = queryFactory.getMediasByGenres("Pop;Pop/Funk;Rock & Roll", MULTI_FOLDERS);
        assertEquals("multi genre", "+(g:Pop g:Pop/Funk g:Rock & Roll) +(f:" + PATH1 + " f:" + PATH2 + ")", query.toString());
    }

    @Test
    public void testGetAlbumId3sByGenre() throws IOException {
        Query query = queryFactory.getAlbumId3sByGenres("Instrumental pop", SINGLE_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(SINGLE_FOLDERS), "+(g:Instrumental pop) +(fId:" + FID1 + ")", query.toString());
        query = queryFactory.getAlbumId3sByGenres("Rock & Roll", MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "+(g:Rock & Roll) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
        query = queryFactory.getAlbumId3sByGenres("Pop;Pop/Funk", MULTI_FOLDERS);
        assertEquals(ToStringBuilder.reflectionToString(MULTI_FOLDERS), "+(g:Pop g:Pop/Funk) +(fId:" + FID1 + " fId:" + FID2 + ")", query.toString());
    }

    @Test
    public void testToPreAnalyzedGenres() throws IOException {
        Query query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock"));
        assertEquals("genre", "+(g:Classic Rock)", query.toString());
        query = queryFactory.toPreAnalyzedGenres(Arrays.asList("Classic Rock", "Rock & Roll"));
        assertEquals("multi genres", "+(g:Classic Rock g:Rock & Roll)", query.toString());
    }

}