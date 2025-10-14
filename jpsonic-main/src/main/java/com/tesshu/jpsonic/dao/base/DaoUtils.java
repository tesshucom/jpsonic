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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.dao.base;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MediaFile;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;

public final class DaoUtils {

    private static final String MSG_NO_DEF = "Definition does not exist: %s)";
    private static final String MEDIA_FILE_INSERT_COLUMNS = """
            path, folder, type, format, title, album, artist, album_artist, disc_number,
            track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds,
            file_size, width, height, cover_art_path, parent_path, play_count, last_played,
            comment, created, changed, last_scanned, children_last_updated, present, version,
            mb_release_id, mb_recording_id, composer, artist_sort, album_sort, title_sort,
            album_artist_sort, composer_sort, artist_reading, album_reading, album_artist_reading,
            artist_sort_raw, album_sort_raw, album_artist_sort_raw, composer_sort_raw,
            media_file_order, music_index\s
            """;

    private static final String ALBUM_INSERT_COLUMNS = """
            path, name, artist, song_count, duration_seconds, cover_art_path, year, genre,
            play_count, last_played, comment, created, last_scanned, present, folder_id,
            mb_release_id, artist_sort, name_sort, artist_reading, name_reading, album_order\s
            """;

    private DaoUtils() {
    }

    public static @Nullable Instant nullableInstantOf(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static String questionMarks(String columns) {
        int numberOfColumns = StringUtils.countMatches(columns, ",") + 1;
        return StringUtils.repeat("?", ", ", numberOfColumns);
    }

    public static String prefix(String columns, String prefix) {
        List<String> l = Arrays.asList(columns.replaceAll("\n", " ").split(", "));
        l.replaceAll(s -> prefix + "." + s);
        return String.join(", ", l).trim().concat(" ");
    }

    public static String getInsertColumns(Class<?> domainClass) {
        if (domainClass == MediaFile.class) {
            return MEDIA_FILE_INSERT_COLUMNS;
        } else if (domainClass == Album.class) {
            return ALBUM_INSERT_COLUMNS;
        }
        throw new IllegalArgumentException(MSG_NO_DEF.formatted(domainClass.getSimpleName()));
    }

    public static String getQueryColumns(Class<?> domainClass) {
        if (domainClass == MediaFile.class) {
            return "id, " + MEDIA_FILE_INSERT_COLUMNS;
        } else if (domainClass == Album.class) {
            return "id, " + ALBUM_INSERT_COLUMNS;
        }
        throw new IllegalArgumentException(MSG_NO_DEF.formatted(domainClass.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    public static <T> RowMapper<T> createRowMapper(Class<T> domainClass) {
        if (domainClass == MediaFile.class) {
            return (RowMapper<T>) createMediaFileRowMapper();
        } else if (domainClass == Album.class) {
            return (RowMapper<T>) createAlbumRowMapper();
        }
        throw new IllegalArgumentException(MSG_NO_DEF.formatted(domainClass.getSimpleName()));
    }

    @SuppressWarnings({ "PMD.CognitiveComplexity" })
    private static RowMapper<MediaFile> createMediaFileRowMapper() {
        return (rs, num) -> new MediaFile(rs.getInt(1), rs.getString(2), rs.getString(3),
                MediaFile.MediaType.valueOf(rs.getString(4)), rs.getString(5), rs.getString(6),
                rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getInt(10) == 0 ? null : rs.getInt(10),
                rs.getInt(11) == 0 ? null : rs.getInt(11),
                rs.getInt(12) == 0 ? null : rs.getInt(12), rs.getString(13),
                rs.getInt(14) == 0 ? null : rs.getInt(14), rs.getBoolean(15),
                rs.getInt(16) == 0 ? null : rs.getInt(16),
                rs.getLong(17) == 0 ? null : rs.getLong(17),
                rs.getInt(18) == 0 ? null : rs.getInt(18),
                rs.getInt(19) == 0 ? null : rs.getInt(19), rs.getString(20), rs.getString(21),
                rs.getInt(22), nullableInstantOf(rs.getTimestamp(23)), rs.getString(24),
                nullableInstantOf(rs.getTimestamp(25)), nullableInstantOf(rs.getTimestamp(26)),
                nullableInstantOf(rs.getTimestamp(27)), nullableInstantOf(rs.getTimestamp(28)),
                rs.getBoolean(29), rs.getInt(30), rs.getString(31), rs.getString(32),
                rs.getString(33), rs.getString(34), rs.getString(35), rs.getString(36),
                rs.getString(37), rs.getString(38), rs.getString(39), rs.getString(40),
                rs.getString(41), rs.getString(42), rs.getString(43), rs.getString(44),
                rs.getString(45), rs.getInt(46), rs.getString(47));
    }

    private static RowMapper<Album> createAlbumRowMapper() {
        return (rs, num) -> new Album(rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getString(7),
                rs.getInt(8) == 0 ? null : rs.getInt(8), rs.getString(9), rs.getInt(10),
                nullableInstantOf(rs.getTimestamp(11)), rs.getString(12),
                nullableInstantOf(rs.getTimestamp(13)), nullableInstantOf(rs.getTimestamp(14)),
                rs.getBoolean(15), rs.getInt(16), rs.getString(17), rs.getString(18),
                rs.getString(19), rs.getString(20), rs.getString(21), rs.getInt(22));
    }
}
