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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.List;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.Before;
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

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchCriteriaDirectorLegacyTest {

    @Documented
    private @interface DirectorDecisions {
        @interface Conditions {
            @SuppressWarnings("unused")
            @interface searchInput {
            }

            @SuppressWarnings("unused")
            @interface offset {
            }

            @SuppressWarnings("unused")
            @interface count {
            }

            @interface includeComposer {
                @interface FALSE {
                }

                @interface TRUE {
                }
            }

            @interface musicFolders {
                @interface SINGLE_FOLDERS {
                }

                @interface MULTI_FOLDERS {
                }
            }

            @interface indexType {
                @interface ARTIST {
                }

                @interface ALBUM {
                }

                @interface SONG {
                }

                @interface ARTIST_ID3 {
                }

                @interface ALBUM_ID3 {
                }
            }
        }

        @interface Actions {
            @interface construct {
            }
        }
    }

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private SettingsService settingsService;

    @Before
    public void setup() {
        analyzerFactory.setSearchMethodLegacy(true);
        settingsService.setSearchMethodLegacy(true);
    }

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

    private final MusicFolder MUSIC_FOLDER1 = new MusicFolder(Integer.valueOf(FID1), new File(PATH1), "music1", true,
            new java.util.Date());
    private final MusicFolder MUSIC_FOLDER2 = new MusicFolder(Integer.valueOf(FID2), new File(PATH2), "music2", true,
            new java.util.Date());

    List<MusicFolder> SINGLE_FOLDERS = Arrays.asList(MUSIC_FOLDER1);
    List<MusicFolder> MULTI_FOLDERS = Arrays.asList(MUSIC_FOLDER1, MUSIC_FOLDER2);

    private final int offset = 10;
    private final int count = Integer.MAX_VALUE;
    private final boolean includeComposer = false;

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST
    @DirectorDecisions.Actions.construct
    @Test
    public void c01() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.TRUE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST
    @DirectorDecisions.Actions.construct
    @Test
    public void c02() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, true,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.MULTI_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST
    @DirectorDecisions.Actions.construct
    @Test
    public void c03() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM
    @DirectorDecisions.Actions.construct
    @Test
    public void c04() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.TRUE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM
    @DirectorDecisions.Actions.construct
    @Test
    public void c05() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, true,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.MULTI_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM
    @DirectorDecisions.Actions.construct
    @Test
    public void c06() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.SONG
    @DirectorDecisions.Actions.construct
    @Test
    public void c07() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.TRUE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.SONG
    @DirectorDecisions.Actions.construct
    @Test
    public void c08() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, true,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2 (cmpR:abc*)^1.1 cmp:abc*) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2 (cmpR:123*)^1.1 cmp:123*)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.MULTI_FOLDERS
    @DirectorDecisions.Conditions.indexType.SONG
    @DirectorDecisions.Actions.construct
    @Test
    public void c09() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c10() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.TRUE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c11() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, true,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.MULTI_FOLDERS
    @DirectorDecisions.Conditions.indexType.ARTIST_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c12() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c13() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.TRUE
    @DirectorDecisions.Conditions.musicFolders.SINGLE_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c14() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, true,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.includeComposer.FALSE
    @DirectorDecisions.Conditions.musicFolders.MULTI_FOLDERS
    @DirectorDecisions.Conditions.indexType.ALBUM_ID3
    @DirectorDecisions.Actions.construct
    @Test
    public void c15() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, false,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchAlbum() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(fId:"
                        + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtist() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchSong() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, offset, count, includeComposer,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((tit:ネコ*)^2.2 (artR:ねこ*)^1.4 (art:ネコ*)^1.2) ((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((titEX:ねこいぬ*)^2.3 (tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2) ((tit:いぬ*)^2.2 (artR:いぬ*)^1.4 (art:いぬ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, offset, count, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

}
