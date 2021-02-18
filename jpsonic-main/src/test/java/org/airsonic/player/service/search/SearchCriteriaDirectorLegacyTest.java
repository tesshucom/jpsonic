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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class SearchCriteriaDirectorLegacyTest {

    private static final String QUERY_PATTERN_INCLUDING_KATAKANA = "ネコ ABC";
    private static final String QUERY_PATTERN_ALPHANUMERIC_ONLY = "ABC 123";
    private static final String QUERY_PATTERN_HIRAGANA_ONLY = "ねこ いぬ";
    private static final String QUERY_PATTERN_OTHERS = "ABC ねこ";
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
    private static final int OFFSET = 10;
    private static final int COUNT = Integer.MAX_VALUE;

    private boolean includeComposer;

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private SettingsService settingsService;

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private SearchCriteriaDirector director;

    @Documented
    private @interface DirectorDecisions {
        @interface Conditions {
            @SuppressWarnings("unused")
            @interface SearchInput {
            }

            @SuppressWarnings("unused")
            @interface Offset {
            }

            @SuppressWarnings("unused")
            @interface Count {
            }

            @interface IncludeComposer {
                @interface FALSE {
                }

                @interface TRUE {
                }
            }

            @interface MusicFolders {
                @interface SingleFolders {
                }

                @interface MultiFolders {
                }
            }

            @interface Indextype {
                @interface ARTIST {
                }

                @interface ALBUM {
                }

                @interface SONG {
                }

                @interface ArtistId3 {
                }

                @interface AlbumId3 {
                }
            }
        }

        @interface Actions {
            @interface Construct {
            }
        }
    }

    @ClassRule
    public static final SpringClassRule CLASS_RULE = new SpringClassRule() {
        final HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Before
    public void setup() throws ExecutionException {
        Method setSearchMethodLegacy;
        try {
            setSearchMethodLegacy = analyzerFactory.getClass().getDeclaredMethod("setSearchMethodLegacy",
                    boolean.class);
            setSearchMethodLegacy.setAccessible(true);
            setSearchMethodLegacy.invoke(analyzerFactory, true);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new ExecutionException(e);
        }
        settingsService.setSearchMethodLegacy(true);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    public void c01() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    public void c02() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    public void c03() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    public void c04() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    public void c05() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    public void c06() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    public void c07() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    public void c08() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2 (cmpR:abc*)^1.1 cmp:abc*) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2 (cmpR:123*)^1.1 cmp:123*)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    public void c09() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c10() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c11() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c12() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c13() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c14() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + ")",
                criteria.getParsedQuery().toString());
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    public void c15() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchAlbum() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1
                        + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(fId:"
                        + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1
                        + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtist() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString());
    }

    @Test
    public void testSearchSong() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(QUERY_PATTERN_INCLUDING_KATAKANA,
                "+(((tit:ネコ*)^2.2 (artR:ねこ*)^1.4 (art:ネコ*)^1.2) ((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_ALPHANUMERIC_ONLY,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_HIRAGANA_ONLY,
                "+(((titEX:ねこいぬ*)^2.3 (tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2) ((tit:いぬ*)^2.2 (artR:いぬ*)^1.4 (art:いぬ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(QUERY_PATTERN_OTHERS,
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString());
    }

}
