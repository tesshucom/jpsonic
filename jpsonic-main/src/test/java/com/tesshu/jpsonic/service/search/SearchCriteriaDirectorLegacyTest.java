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
import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class SearchCriteriaDirectorLegacyTest {

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

    @BeforeEach
    public void setup() throws ExecutionException {
        SettingsService settingsService = mock(SettingsService.class);
        Mockito.when(settingsService.isSearchMethodLegacy()).thenReturn(true);
        AnalyzerFactory analyzerFactory = new AnalyzerFactory(settingsService);
        SearchServiceUtilities utilities = new SearchServiceUtilities(null, null, null, null, null, settingsService);
        director = new SearchCriteriaDirector(new QueryFactory(analyzerFactory, utilities), settingsService);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    void c01() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    void c02() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ARTIST
    @DirectorDecisions.Actions.Construct
    @Test
    void c03() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ARTIST);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    void c04() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:"
                + PATH1 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    void c05() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ALBUM);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:"
                + PATH1 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ALBUM
    @DirectorDecisions.Actions.Construct
    @Test
    void c06() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:"
                + PATH1 + " f:" + PATH2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    void c07() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    void c08() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.SONG);
        assertEquals(
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2 (cmpR:abc*)^1.1 cmp:abc*) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2 (cmpR:123*)^1.1 cmp:123*)) +(f:"
                        + PATH1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.SONG
    @DirectorDecisions.Actions.Construct
    @Test
    void c09() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c10() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c11() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.ArtistId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c12() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c13() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:"
                + FID1 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.TRUE
    @DirectorDecisions.Conditions.MusicFolders.SingleFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c14() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, true,
                SINGLE_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:"
                + FID1 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @DirectorDecisions.Conditions.IncludeComposer.FALSE
    @DirectorDecisions.Conditions.MusicFolders.MultiFolders
    @DirectorDecisions.Conditions.Indextype.AlbumId3
    @DirectorDecisions.Actions.Construct
    @Test
    void c15() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, false,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:"
                + FID1 + " fId:" + FID2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);
    }

    @Test
    void testSearchAlbum() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM);
        assertEquals("+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(f:" + PATH1
                + " f:" + PATH2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(f:"
                + PATH1 + " f:" + PATH2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals(
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_HIRAGANA_ONLY);

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1
                + " f:" + PATH2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_OTHERS);
    }

    @Test
    void testSearchAlbumId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ALBUM_ID3);
        assertEquals("+(((alb:ネコ*)^2.3 (artR:ねこ*)^1.1 art:ネコ*) ((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*)) +(fId:" + FID1
                + " fId:" + FID2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:123*)^2.3 (artR:123*)^1.1 art:123*)) +(fId:"
                + FID1 + " fId:" + FID2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals(
                "+(((albEX:ねこいぬ*)^2.3 (alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*) ((alb:いぬ*)^2.3 (artR:いぬ*)^1.1 art:いぬ*)) +(fId:"
                        + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_HIRAGANA_ONLY);

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ALBUM_ID3);
        assertEquals("+(((alb:abc*)^2.3 (artR:abc*)^1.1 art:abc*) ((alb:ねこ*)^2.3 (artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1
                + " fId:" + FID2 + ")", criteria.getParsedQuery().toString(), QUERY_PATTERN_OTHERS);
    }

    @Test
    void testSearchArtist() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                SINGLE_FOLDERS, IndexType.ARTIST);
        assertEquals("+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals("+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals("+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_HIRAGANA_ONLY);

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(f:" + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_OTHERS);
    }

    @Test
    void testSearchArtistId3() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.ARTIST_ID3);
        assertEquals("+(((artR:ねこ*)^1.1 art:ネコ*) ((artR:abc*)^1.1 art:abc*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:123*)^1.1 art:123*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals("+(((artR:ねこ*)^1.1 art:ねこ*) ((artR:いぬ*)^1.1 art:いぬ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_HIRAGANA_ONLY);

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.ARTIST_ID3);
        assertEquals("+(((artR:abc*)^1.1 art:abc*) ((artR:ねこ*)^1.1 art:ねこ*)) +(fId:" + FID1 + " fId:" + FID2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_OTHERS);
    }

    @Test
    void testSearchSong() throws IOException {
        SearchCriteria criteria = director.construct(QUERY_PATTERN_INCLUDING_KATAKANA, OFFSET, COUNT, includeComposer,
                MULTI_FOLDERS, IndexType.SONG);
        assertEquals(
                "+(((tit:ネコ*)^2.2 (artR:ねこ*)^1.4 (art:ネコ*)^1.2) ((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_INCLUDING_KATAKANA);

        criteria = director.construct(QUERY_PATTERN_ALPHANUMERIC_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:123*)^2.2 (artR:123*)^1.4 (art:123*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_ALPHANUMERIC_ONLY);

        criteria = director.construct(QUERY_PATTERN_HIRAGANA_ONLY, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(
                "+(((titEX:ねこいぬ*)^2.3 (tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2) ((tit:いぬ*)^2.2 (artR:いぬ*)^1.4 (art:いぬ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_HIRAGANA_ONLY);

        criteria = director.construct(QUERY_PATTERN_OTHERS, OFFSET, COUNT, includeComposer, MULTI_FOLDERS,
                IndexType.SONG);
        assertEquals(
                "+(((tit:abc*)^2.2 (artR:abc*)^1.4 (art:abc*)^1.2) ((tit:ねこ*)^2.2 (artR:ねこ*)^1.4 (art:ねこ*)^1.2)) +(f:"
                        + PATH1 + " f:" + PATH2 + ")",
                criteria.getParsedQuery().toString(), QUERY_PATTERN_OTHERS);
    }

}
