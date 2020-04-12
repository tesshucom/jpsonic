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
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchCriteriaDirectorTest {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    private static final String SEPA = System.getProperty("file.separator");

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private SearchCriteriaDirector director;

    private final String QUERY_PATTERN_INCLUDING_KATAKANA = "ネコ ABC";
    private final String QUERY_PATTERN_ALPHANUMERIC_ONLY = "ABC 123";
    private final String QUERY_PATTERN_HIRAGANA_ONLY = "ねこ いぬ";

    private final String QUERY_PATTERN_OTHERS = "ABC ねこ";

    private final String PATH1 = SEPA + "var" + SEPA + "music1";
    private final String PATH2 = SEPA + "var" + SEPA + "music2";

    private final int FID1 = 10;
    private final int FID2 = 20;

    private final MusicFolder MUSIC_FOLDER1 = new MusicFolder(Integer.valueOf(FID1), new File(PATH1), "music1", true, new java.util.Date());
    private final MusicFolder MUSIC_FOLDER2 = new MusicFolder(Integer.valueOf(FID2), new File(PATH2), "music2", true, new java.util.Date());

    List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    List<MusicFolder> MULTI_FOLDERS = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    private final int offset = 10;
    private final int count = Integer.MAX_VALUE;
    private final boolean includeComposer = false;

    @Test
    public void testSearchAlbum() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")", criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_OTHERS, "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtist() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchSong() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((tit:ネコ*)^2.2 (artR:ねこ*)^1.4 (art:ネコ*)^1.2) ((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((titEX:ねこいぬ*)^2.3 (tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2) ((tit:いぬ*)^2.2 (artR:いぬ*)^1.4 (art:いぬ*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

}