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

package org.airsonic.player.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.airsonic.player.domain.RandomSearchCriteria;
import org.assertj.core.util.Arrays;
import org.junit.Test;

public class MediaFileDaoTest {

    // Below is a test of MediaFileDao#RandomSongsQueryBuilder

    private final String select = "select media_file.id, media_file.path, media_file.folder, media_file.type, "
            + "media_file.format, media_file.title, media_file.album, media_file.artist, media_file.album_artist, "
            + "media_file.disc_number, media_file.track_number, media_file.year, media_file.genre, "
            + "media_file.bit_rate, media_file.variable_bit_rate, media_file.duration_seconds, "
            + "media_file.file_size, media_file.width, media_file.height, media_file.cover_art_path, "
            + "media_file.parent_path, media_file.play_count, media_file.last_played, media_file.comment, "
            + "media_file.created, media_file.changed, media_file.last_scanned, media_file.children_last_updated, "
            + "media_file.present, media_file.version, media_file.mb_release_id, media_file.mb_recording_id, "
            + "media_file.composer, media_file.artist_sort, media_file.album_sort, media_file.title_sort, "
            + "media_file.album_artist_sort, media_file.composer_sort, media_file.artist_reading, "
            + "media_file.album_reading, media_file.album_artist_reading, media_file.artist_sort_raw, "
            + "media_file.album_sort_raw, media_file.album_artist_sort_raw, media_file.composer_sort_raw, "
            + "media_file.media_file_order from media_file";
    private final String conditionJoin1 = " left outer join starred_media_file "
            + "on media_file.id = starred_media_file.media_file_id and starred_media_file.username = :username";
    private final String conditionJoin2 = " left outer join media_file media_album "
            + "on media_album.type = 'ALBUM' and media_album.album = media_file.album "
            + "and media_album.artist = media_file.artist " + "left outer join user_rating "
            + "on user_rating.path = media_album.path and user_rating.username = :username";
    private final String where = " where media_file.present and media_file.type = 'MUSIC'";
    private final String orderLimit = " order by rand() limit 0";
    private final String conditionShowStarredSongs = " and starred_media_file.id is not null";
    private final String conditionMinAlbumRating = " and user_rating.rating >= :minAlbumRating";

    private Object createRandomSongsQueryBuilder(RandomSearchCriteria criteria) {
        Optional<Object> builderClazz = Arrays.asList(MediaFileDao.class.getDeclaredClasses()).stream()
                .filter(o -> "RandomSongsQueryBuilder".equals(((Class<?>) o).getSimpleName())).findFirst();
        Class<?> c = ((Class<?>) builderClazz.get());
        Constructor<?> constructor = c.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {
            return constructor.newInstance(criteria);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> invokeGetCondition(Object randomSongsQueryBuilder, String methodName) {
        Method method;
        try {
            method = randomSongsQueryBuilder.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (Optional<String>) method.invoke(randomSongsQueryBuilder);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    public void testGetIfJoinStarred() {

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getIfJoinStarred");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true, false,
                null);
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinStarred");
        assertEquals(conditionJoin1, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false, true,
                null);
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinStarred");
        assertEquals(conditionJoin1, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true, true,
                null);
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinStarred");
        assertFalse(op.isPresent());
    }

    @Test
    public void testGetIfJoinAlbumRating() {

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getIfJoinAlbumRating");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, Integer.valueOf(1), null, null, null,
                false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinAlbumRating");
        assertEquals(conditionJoin2, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, Integer.valueOf(1), null, null,
                false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinAlbumRating");
        assertEquals(conditionJoin2, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, Integer.valueOf(1),
                Integer.valueOf(1), null, null, false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
        assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getIfJoinAlbumRating");
        assertEquals(conditionJoin2, op.get());
    }

    @Test
    public void testGetFolderCondition() {

        final String condition = " and media_file.folder in (:folders)";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                null, null, null, null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getFolderCondition");
        assertFalse(op.isPresent());

        List<org.airsonic.player.domain.MusicFolder> folders = new ArrayList<>();
        folders.add(new org.airsonic.player.domain.MusicFolder(null, "", false, null));
        criteria = new RandomSearchCriteria(0, null, null, null, folders, null, null, null, null, null, null, false,
                false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getFolderCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetGenreCondition() {

        final String condition = " and media_file.genre in (:genres)";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                null, null, null, null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getGenreCondition");
        assertFalse(op.isPresent());

        List<String> genres = new ArrayList<>();
        genres.add("genre");
        criteria = new RandomSearchCriteria(0, genres, null, null, null, null, null, null, null, null, null, false,
                false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getGenreCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetFormatCondition() {

        final String condition = " and media_file.format = :format";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                null, null, null, null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getFormatCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false, false,
                "mp3");
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getFormatCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetFromYearCondition() {

        final String condition = " and media_file.year >= :fromYear";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                null, null, null, null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getFromYearCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, Integer.valueOf(1900), null, null, null, null, null, null, null,
                null, false, false, null);
        assertEquals(1900, criteria.getFromYear());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getFromYearCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetToYearCondition() {

        final String condition = " and media_file.year <= :toYear";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getToYearCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, Integer.valueOf(2020), null, null, null, null, null, null,
                null, false, false, null);
        assertEquals(2020, criteria.getToYear());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getToYearCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetMinLastPlayedCondition() {

        final String condition = " and media_file.last_played >= :minLastPlayed";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMinLastPlayedDateCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, new Date(), null, null, null, null, null, false,
                false, null);
        assertNotNull(criteria.getMinLastPlayedDate());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMinLastPlayedDateCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetMaxLastPlayedCondition() {

        final String condition1 = " and media_file.last_played <= :maxLastPlayed";
        final String condition2 = " and (media_file.last_played is null or media_file.last_played <= :maxLastPlayed)";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        assertNull(criteria.getMinLastPlayedDate());
        assertNull(criteria.getMaxLastPlayedDate());
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMaxLastPlayedDateCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, new Date(), new Date(), null, null, null, null,
                false, false, null);
        assertNotNull(criteria.getMinLastPlayedDate());
        assertNotNull(criteria.getMaxLastPlayedDate());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxLastPlayedDateCondition");
        assertEquals(condition1, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, new Date(), null, null, null, null, false,
                false, null);
        assertNull(criteria.getMinLastPlayedDate());
        assertNotNull(criteria.getMaxLastPlayedDate());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxLastPlayedDateCondition");
        assertEquals(condition2, op.get());
    }

    @Test
    public void testGetMinAlbumRatingCondition() {

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMinAlbumRatingCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, Integer.valueOf(1), null, null, null,
                false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMinAlbumRatingCondition");
        assertEquals(conditionMinAlbumRating, op.get());
    }

    @Test
    public void testGetMaxAlbumRatingCondition() {

        final String condition1 = " and user_rating.rating <= :maxAlbumRating";
        final String condition2 = " and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        assertNull(criteria.getMinLastPlayedDate());
        assertNull(criteria.getMaxLastPlayedDate());
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMaxAlbumRatingCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, Integer.valueOf(1),
                Integer.valueOf(2), null, null, false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinAlbumRating());
        assertEquals(Integer.valueOf(2), criteria.getMaxAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxAlbumRatingCondition");
        assertEquals(condition1, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, Integer.valueOf(1), null, null,
                false, false, null);
        assertNull(criteria.getMinAlbumRating());
        assertEquals(Integer.valueOf(1), criteria.getMaxAlbumRating());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxAlbumRatingCondition");
        assertEquals(condition2, op.get());
    }

    @Test
    public void testGetMinPlayCountCondition() {

        final String condition = " and media_file.play_count >= :minPlayCount";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMinPlayCountCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, Integer.valueOf(1), null,
                false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinPlayCount());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMinPlayCountCondition");
        assertEquals(condition, op.get());
    }

    @Test
    public void testGetMaxPlayCountCondition() {

        final String condition1 = " and media_file.play_count <= :maxPlayCount";
        final String condition2 = " and (media_file.play_count is null or media_file.play_count <= :maxPlayCount)";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        assertNull(criteria.getMinLastPlayedDate());
        assertNull(criteria.getMaxLastPlayedDate());
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getMaxPlayCountCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, Integer.valueOf(1),
                Integer.valueOf(2), false, false, null);
        assertEquals(Integer.valueOf(1), criteria.getMinPlayCount());
        assertEquals(Integer.valueOf(2), criteria.getMaxPlayCount());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxPlayCountCondition");
        assertEquals(condition1, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, Integer.valueOf(1),
                false, false, null);
        assertNull(criteria.getMinPlayCount());
        assertEquals(Integer.valueOf(1), criteria.getMaxPlayCount());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getMaxPlayCountCondition");
        assertEquals(condition2, op.get());
    }

    @Test
    public void testGetShowStarredSongsCondition() {

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        assertFalse(criteria.isShowStarredSongs());
        assertFalse(criteria.isShowUnstarredSongs());
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getShowStarredSongsCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true, false,
                null);
        assertTrue(criteria.isShowStarredSongs());
        assertFalse(criteria.isShowUnstarredSongs());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getShowStarredSongsCondition");
        assertEquals(conditionShowStarredSongs, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true, true,
                null);
        assertTrue(criteria.isShowStarredSongs());
        assertTrue(criteria.isShowUnstarredSongs());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getShowStarredSongsCondition");
        assertFalse(op.isPresent());
    }

    @Test
    public void testGetShowUnstarredSongsCondition() {

        final String condition = " and starred_media_file.id is null";

        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null,
                null, null, false, false, null);
        assertFalse(criteria.isShowStarredSongs());
        assertFalse(criteria.isShowUnstarredSongs());
        Object builder = createRandomSongsQueryBuilder(criteria);
        Optional<String> op = invokeGetCondition(builder, "getShowUnstarredSongsCondition");
        assertFalse(op.isPresent());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, false, true,
                null);
        assertFalse(criteria.isShowStarredSongs());
        assertTrue(criteria.isShowUnstarredSongs());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getShowUnstarredSongsCondition");
        assertEquals(condition, op.get());

        criteria = new RandomSearchCriteria(0, null, null, null, null, null, null, null, null, null, null, true, true,
                null);
        assertTrue(criteria.isShowStarredSongs());
        assertTrue(criteria.isShowUnstarredSongs());
        builder = createRandomSongsQueryBuilder(criteria);
        op = invokeGetCondition(builder, "getShowUnstarredSongsCondition");
        assertFalse(op.isPresent());
    }

    private String invokeBuild(Object builder) {
        Method method;
        Object result = null;
        try {
            method = builder.getClass().getDeclaredMethod("build");
            method.setAccessible(true);
            result = method.invoke(builder);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
        return result.toString();
    }

    @Test
    public void testBuild() {

        final String noOp = select + where + orderLimit;
        RandomSearchCriteria criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null,
                null, null, null, null, null, false, false, null);
        Object builder = createRandomSongsQueryBuilder(criteria);
        String query = invokeBuild(builder);
        assertEquals(noOp, query);

        final String ifJoinStarred = select + conditionJoin1 + where + conditionShowStarredSongs + orderLimit;
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null, null,
                null, true, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertEquals(ifJoinStarred, query);

        final String ifJoinAlbumRating = select + conditionJoin2 + where + conditionMinAlbumRating + orderLimit;
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null,
                Integer.valueOf(1), null, null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertEquals(ifJoinAlbumRating, query);

        final String ifJoinStarredAndRating = select + conditionJoin1 + conditionJoin2 + where + conditionMinAlbumRating
                + conditionShowStarredSongs + orderLimit;
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null,
                Integer.valueOf(1), null, null, null, true, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertEquals(ifJoinStarredAndRating, query);

        final Function<String, Boolean> assertWithCondition = (q) -> {
            String suffix = select + where;
            return Boolean.valueOf(q.startsWith(suffix) && q.endsWith(orderLimit) && suffix.length() < q.length());
        };

        // folder
        List<org.airsonic.player.domain.MusicFolder> folders = new ArrayList<>();
        folders.add(new org.airsonic.player.domain.MusicFolder(null, "", false, null));
        criteria = new RandomSearchCriteria(0, null, null, null, folders, null, null, null, null, null, null, false,
                false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // genre
        List<String> genres = new ArrayList<>();
        genres.add("genre");
        criteria = new RandomSearchCriteria(0, genres, null, null, Collections.emptyList(), null, null, null, null,
                null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // format
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null, null,
                null, false, false, "mp3");
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // fromYear
        criteria = new RandomSearchCriteria(0, null, Integer.valueOf(1900), null, Collections.emptyList(), null, null,
                null, null, null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // toYear
        criteria = new RandomSearchCriteria(0, null, null, Integer.valueOf(2021), Collections.emptyList(), null, null,
                null, null, null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // minLastPlayedDateCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), new Date(), null, null, null,
                null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // maxLastPlayedDateCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, new Date(), null, null,
                null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // maxAlbumRatingCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null,
                Integer.valueOf(1), Integer.valueOf(2), null, null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        String suffix = select + conditionJoin2 + where;
        assertTrue(query.startsWith(suffix));
        assertTrue(query.endsWith(orderLimit));
        assertTrue(suffix.length() < query.length());

        // minPlayCountCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null,
                Integer.valueOf(1), null, false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // maxPlayCountCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null, null,
                Integer.valueOf(1), false, false, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        assertTrue(assertWithCondition.apply(query));

        // showUnstarredSongsCondition
        criteria = new RandomSearchCriteria(0, null, null, null, Collections.emptyList(), null, null, null, null, null,
                null, false, true, null);
        builder = createRandomSongsQueryBuilder(criteria);
        query = invokeBuild(builder);
        suffix = select + conditionJoin1 + where;
        assertTrue(query.startsWith(suffix));
        assertTrue(query.endsWith(orderLimit));
        assertTrue(suffix.length() < query.length());
    }
}
