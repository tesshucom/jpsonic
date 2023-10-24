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

package com.tesshu.jpsonic.dao;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.tesshu.jpsonic.dao.MediaFileDao.RandomSongsQueryBuilder;
import com.tesshu.jpsonic.dao.base.DaoHelper;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.dao.dialect.DialectMediaFileDao;
import com.tesshu.jpsonic.domain.ArtistSortCandidate;
import com.tesshu.jpsonic.domain.ArtistSortCandidate.TargetField;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
class MediaFileDaoTest {

    private JdbcTemplate jdbcTemplate;
    private MediaFileDao mediaFileDao;

    @BeforeEach
    public void setup() {
        jdbcTemplate = mock(JdbcTemplate.class);
        DaoHelper daoHelper = mock(DaoHelper.class);
        Mockito.when(daoHelper.getJdbcTemplate()).thenReturn(jdbcTemplate);
        TemplateWrapper templateWrapper = new TemplateWrapper(daoHelper);
        mediaFileDao = new MediaFileDao(templateWrapper, mock(DialectMediaFileDao.class));
    }

    @Test
    void updateArtistSortTest() {
        ArtistSortCandidate artist = new ArtistSortCandidate("artist", "artistSort", 1, "DIRECTORY",
                TargetField.ARTIST.getValue());
        List<ArtistSortCandidate> cands = List.of(artist);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.when(jdbcTemplate.update(queryCaptor.capture(), argCaptor.capture())).thenReturn(0);
        mediaFileDao.updateArtistSort(cands);
        assertEquals("update media_file set artist_reading=?, artist_sort=?, music_index = ? where id = ?",
                queryCaptor.getValue());
        assertEquals(4, argCaptor.getAllValues().size());

        ArtistSortCandidate artistOfSong = new ArtistSortCandidate("artist", "artistSort", 1, "MUSIC",
                TargetField.ARTIST.getValue());
        cands = List.of(artistOfSong);
        queryCaptor = ArgumentCaptor.forClass(String.class);
        argCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.when(jdbcTemplate.update(queryCaptor.capture(), argCaptor.capture())).thenReturn(0);
        mediaFileDao.updateArtistSort(cands);
        assertEquals("update media_file set artist_reading=?, artist_sort=? where id = ?", queryCaptor.getValue());
        assertEquals(3, argCaptor.getAllValues().size());

        ArtistSortCandidate albumArtist = new ArtistSortCandidate("albumArtist", "albumArtistSort", 1, "MUSIC",
                TargetField.ALBUM_ARTIST.getValue());
        cands = List.of(artistOfSong, albumArtist);
        queryCaptor = ArgumentCaptor.forClass(String.class);
        argCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.when(jdbcTemplate.update(queryCaptor.capture(), argCaptor.capture())).thenReturn(0);
        mediaFileDao.updateArtistSort(cands);
        assertEquals(
                "update media_file set artist_reading=?, artist_sort=?, album_artist_reading=?, album_artist_sort=? where id = ?",
                queryCaptor.getValue());
        assertEquals(5, argCaptor.getAllValues().size());

        ArtistSortCandidate composer = new ArtistSortCandidate("albumArtist", "albumArtistSort", 1, "MUSIC",
                TargetField.COMPOSER.getValue());
        cands = List.of(artistOfSong, albumArtist, composer);
        queryCaptor = ArgumentCaptor.forClass(String.class);
        argCaptor = ArgumentCaptor.forClass(Object[].class);
        Mockito.when(jdbcTemplate.update(queryCaptor.capture(), argCaptor.capture())).thenReturn(0);
        mediaFileDao.updateArtistSort(cands);
        assertEquals(
                "update media_file set artist_reading=?, artist_sort=?, album_artist_reading=?, album_artist_sort=?, composer_sort=? where id = ?",
                queryCaptor.getValue());
        assertEquals(6, argCaptor.getAllValues().size());
    }

    @Nested
    class RandomSongsQueryBuilderTest {

        // Below is a test of MediaFileDao#RandomSongsQueryBuilder

        private static final String SELECT = """
                select media_file.id, media_file.path, media_file.folder, media_file.type, \
                media_file.format, media_file.title, media_file.album, media_file.artist, media_file.album_artist, \
                media_file.disc_number, media_file.track_number, media_file.year, media_file.genre, \
                media_file.bit_rate, media_file.variable_bit_rate, media_file.duration_seconds, \
                media_file.file_size, media_file.width, media_file.height, media_file.cover_art_path, \
                media_file.parent_path, media_file.play_count, media_file.last_played, media_file.comment, \
                media_file.created, media_file.changed, media_file.last_scanned, media_file.children_last_updated, \
                media_file.present, media_file.version, media_file.mb_release_id, media_file.mb_recording_id, \
                media_file.composer, media_file.artist_sort, media_file.album_sort, media_file.title_sort, \
                media_file.album_artist_sort, media_file.composer_sort, media_file.artist_reading, \
                media_file.album_reading, media_file.album_artist_reading, media_file.artist_sort_raw, \
                media_file.album_sort_raw, media_file.album_artist_sort_raw, media_file.composer_sort_raw, \
                media_file.media_file_order, media_file.music_index from media_file\
                """;
        private static final String CONDITION_JOIN1 = """
                 left outer join starred_media_file \
                on media_file.id = starred_media_file.media_file_id and starred_media_file.username = :username\
                """;
        private static final String CONDITION_JOIN2 = """
                 left outer join media_file media_album \
                on media_album.type = 'ALBUM' and media_album.album = media_file.album \
                and media_album.artist = media_file.artist \
                left outer join user_rating \
                on user_rating.path = media_album.path and user_rating.username = :username\
                """;
        private static final String WHERE = " where media_file.present and media_file.type = 'MUSIC'";
        private static final String ORDER_LIMIT = " order by rand() limit 0";
        private static final String CONDITION_SHOW_STARRED_SONGS = " and starred_media_file.id is not null";
        private static final String CONDITION_MIN_ALBUM_RATING = " and user_rating.rating >= :minAlbumRating";

        @Test
        void testGetIfJoinStarred() throws ExecutionException {

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getIfJoinStarred();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true,
                    false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinStarred();
            assertEquals(CONDITION_JOIN1, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false,
                    true, null);
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinStarred();
            assertEquals(CONDITION_JOIN1, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true,
                    true, null);
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinStarred();
            assertFalse(op.isPresent());
        }

        @Test
        void testGetIfJoinAlbumRating() throws ExecutionException {

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getIfJoinAlbumRating();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, 1, null, null, null, false,
                    false, null);
            assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinAlbumRating();
            assertEquals(CONDITION_JOIN2, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, 1, null, null, false,
                    false, null);
            assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinAlbumRating();
            assertEquals(CONDITION_JOIN2, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, 1, 1, null, null, false, false,
                    null);
            assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
            assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getIfJoinAlbumRating();
            assertEquals(CONDITION_JOIN2, op.get());
        }

        @Test
        void testGetFolderCondition() throws ExecutionException {

            final String condition = " and media_file.folder in (:folders)";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                    null, null, null, null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getFolderCondition();
            assertFalse(op.isPresent());

            List<com.tesshu.jpsonic.domain.MusicFolder> folders = new ArrayList<>();
            folders.add(new com.tesshu.jpsonic.domain.MusicFolder("/", "", false, null));
            criteria = new RandomSearchCriteria(0, null, null, null, folders, null, null, null, null, null, null, false,
                    false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getFolderCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetGenreCondition() throws ExecutionException {

            final String condition = " and media_file.genre in (:genres)";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                    null, null, null, null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getGenreCondition();
            assertFalse(op.isPresent());

            List<String> genres = new ArrayList<>();
            genres.add("genre");
            criteria = new RandomSearchCriteria(0, genres, null, null, null, null, null, null, null, null, null, false,
                    false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getGenreCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetFormatCondition() throws ExecutionException {

            final String condition = " and media_file.format = :format";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                    null, null, null, null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getFormatCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false,
                    false, "mp3");
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getFormatCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetFromYearCondition() throws ExecutionException {

            final String condition = " and media_file.year >= :fromYear";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                    null, null, null, null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getFromYearCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, 1900, null, null, null, null, null, null, null, null, false,
                    false, null);
            assertEquals(1900, criteria.getFromYear());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getFromYearCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetToYearCondition() throws ExecutionException {

            final String condition = " and media_file.year <= :toYear";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getToYearCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, 2020, null, null, null, null, null, null, null, false,
                    false, null);
            assertEquals(2020, criteria.getToYear());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getToYearCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetMinLastPlayedCondition() throws ExecutionException {

            final String condition = " and media_file.last_played >= :minLastPlayed";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMinLastPlayedDateCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, now(), null, null, null, null, null, false,
                    false, null);
            Assertions.assertNotNull(criteria.getMinLastPlayedDate());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMinLastPlayedDateCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetMaxLastPlayedCondition() throws ExecutionException {

            final String condition1 = " and media_file.last_played <= :maxLastPlayed";
            final String condition2 = " and (media_file.last_played is null or media_file.last_played <= :maxLastPlayed)";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            assertNull(criteria.getMinLastPlayedDate());
            assertNull(criteria.getMaxLastPlayedDate());
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMaxLastPlayedDateCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, now(), now(), null, null, null, null, false,
                    false, null);
            Assertions.assertNotNull(criteria.getMinLastPlayedDate());
            Assertions.assertNotNull(criteria.getMaxLastPlayedDate());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxLastPlayedDateCondition();
            assertEquals(condition1, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, now(), null, null, null, null, false,
                    false, null);
            assertNull(criteria.getMinLastPlayedDate());
            Assertions.assertNotNull(criteria.getMaxLastPlayedDate());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxLastPlayedDateCondition();
            assertEquals(condition2, op.get());
        }

        @Test
        void testGetMinAlbumRatingCondition() throws ExecutionException {

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMinAlbumRatingCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, 1, null, null, null, false,
                    false, null);
            assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMinAlbumRatingCondition();
            assertEquals(CONDITION_MIN_ALBUM_RATING, op.get());
        }

        @Test
        void testGetMaxAlbumRatingCondition() throws ExecutionException {

            final String condition1 = " and user_rating.rating <= :maxAlbumRating";
            final String condition2 = " and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            assertNull(criteria.getMinLastPlayedDate());
            assertNull(criteria.getMaxLastPlayedDate());
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMaxAlbumRatingCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, 1, 2, null, null, false, false,
                    null);
            assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
            assertEquals(Integer.valueOf(2), criteria.getMaxAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxAlbumRatingCondition();
            assertEquals(condition1, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, 1, null, null, false,
                    false, null);
            assertNull(criteria.getMinAlbumRating());
            assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxAlbumRatingCondition();
            assertEquals(condition2, op.get());
        }

        @Test
        void testGetMinPlayCountCondition() throws ExecutionException {

            final String condition = " and media_file.play_count >= :minPlayCount";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMinPlayCountCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, 1, null, false,
                    false, null);
            assertEquals(Integer.valueOf(1), criteria.getMinPlayCount());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMinPlayCountCondition();
            assertEquals(condition, op.get());
        }

        @Test
        void testGetMaxPlayCountCondition() throws ExecutionException {

            final String condition1 = " and media_file.play_count <= :maxPlayCount";
            final String condition2 = " and (media_file.play_count is null or media_file.play_count <= :maxPlayCount)";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            assertNull(criteria.getMinLastPlayedDate());
            assertNull(criteria.getMaxLastPlayedDate());
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getMaxPlayCountCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, 1, 2, false, false,
                    null);
            assertEquals(Integer.valueOf(1), criteria.getMinPlayCount());
            assertEquals(Integer.valueOf(2), criteria.getMaxPlayCount());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxPlayCountCondition();
            assertEquals(condition1, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, 1, false,
                    false, null);
            assertNull(criteria.getMinPlayCount());
            assertEquals(Integer.valueOf(1), criteria.getMaxPlayCount());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getMaxPlayCountCondition();
            assertEquals(condition2, op.get());
        }

        @Test
        void testGetShowStarredSongsCondition() throws ExecutionException {

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            assertFalse(criteria.isShowStarredSongs());
            assertFalse(criteria.isShowUnstarredSongs());
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getShowStarredSongsCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true,
                    false, null);
            assertTrue(criteria.isShowStarredSongs());
            assertFalse(criteria.isShowUnstarredSongs());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getShowStarredSongsCondition();
            assertEquals(CONDITION_SHOW_STARRED_SONGS, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true,
                    true, null);
            assertTrue(criteria.isShowStarredSongs());
            assertTrue(criteria.isShowUnstarredSongs());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getShowStarredSongsCondition();
            assertFalse(op.isPresent());
        }

        @Test
        void testGetShowUnstarredSongsCondition() throws ExecutionException {

            final String condition = " and starred_media_file.id is null";

            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                    null, null, false, false, null);
            assertFalse(criteria.isShowStarredSongs());
            assertFalse(criteria.isShowUnstarredSongs());
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            Optional<String> op = builder.getShowUnstarredSongsCondition();
            assertFalse(op.isPresent());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false,
                    true, null);
            assertFalse(criteria.isShowStarredSongs());
            assertTrue(criteria.isShowUnstarredSongs());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getShowUnstarredSongsCondition();
            assertEquals(condition, op.get());

            criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true,
                    true, null);
            assertTrue(criteria.isShowStarredSongs());
            assertTrue(criteria.isShowUnstarredSongs());
            builder = new RandomSongsQueryBuilder(criteria);
            op = builder.getShowUnstarredSongsCondition();
            assertFalse(op.isPresent());
        }

        @Test
        void testBuild() throws ExecutionException {

            final String noOp = SELECT + WHERE + ORDER_LIMIT;
            RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                    null, null, null, null, null, false, false, null);
            RandomSongsQueryBuilder builder = new RandomSongsQueryBuilder(criteria);
            String query = builder.build();
            assertEquals(noOp, query);

            final String ifJoinStarred = SELECT + CONDITION_JOIN1 + WHERE + CONDITION_SHOW_STARRED_SONGS + ORDER_LIMIT;
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null,
                    null, null, true, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertEquals(ifJoinStarred, query);

            final String ifJoinAlbumRating = SELECT + CONDITION_JOIN2 + WHERE + CONDITION_MIN_ALBUM_RATING
                    + ORDER_LIMIT;
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, 1, null, null,
                    null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertEquals(ifJoinAlbumRating, query);

            final String ifJoinStarredAndRating = SELECT + CONDITION_JOIN1 + CONDITION_JOIN2 + WHERE
                    + CONDITION_MIN_ALBUM_RATING + CONDITION_SHOW_STARRED_SONGS + ORDER_LIMIT;
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, 1, null, null,
                    null, true, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertEquals(ifJoinStarredAndRating, query);

            final Function<String, Boolean> assertWithCondition = (q) -> {
                String suffix = SELECT + WHERE;
                return q.startsWith(suffix) && q.endsWith(ORDER_LIMIT) && suffix.length() < q.length();
            };

            // folder
            List<com.tesshu.jpsonic.domain.MusicFolder> folders = new ArrayList<>();
            folders.add(new com.tesshu.jpsonic.domain.MusicFolder("/", "", false, null));
            criteria = new RandomSearchCriteria(0, null, null, null, folders, null, null, null, null, null, null, false,
                    false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // genre
            List<String> genres = new ArrayList<>();
            genres.add("genre");
            criteria = new RandomSearchCriteria(0, genres, null, null, Collections.emptyList(), null, null, null, null,
                    null, null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // format
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null,
                    null, null, false, false, "mp3");
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // fromYear
            criteria = new RandomSearchCriteria(0, null, 1900, null, Collections.emptyList(), null, null, null, null,
                    null, null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // toYear
            criteria = new RandomSearchCriteria(0, null, null, 2021, Collections.emptyList(), null, null, null, null,
                    null, null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // minLastPlayedDateCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), now(), null, null, null,
                    null, null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // maxLastPlayedDateCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, now(), null, null,
                    null, null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // maxAlbumRatingCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, 1, 2, null,
                    null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            String suffix = SELECT + CONDITION_JOIN2 + WHERE;
            assertTrue(query.startsWith(suffix));
            assertTrue(query.endsWith(ORDER_LIMIT));
            assertTrue(suffix.length() < query.length());

            // minPlayCountCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null, 1,
                    null, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // maxPlayCountCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null,
                    null, 1, false, false, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            assertTrue(assertWithCondition.apply(query));

            // showUnstarredSongsCondition
            criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null,
                    null, null, false, true, null);
            builder = new RandomSongsQueryBuilder(criteria);
            query = builder.build();
            suffix = SELECT + CONDITION_JOIN1 + WHERE;
            assertTrue(query.startsWith(suffix));
            assertTrue(query.endsWith(ORDER_LIMIT));
            assertTrue(suffix.length() < query.length());
        }
    }
}
