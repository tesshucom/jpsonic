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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.dao;

import java.sql.ResultSet;
import java.time.Instant;

import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StaticsDao extends AbstractDao {

    private final RowMapper<MediaLibraryStatistics> libStatsMapper = (ResultSet rs, int rowNum) -> {
        return new MediaLibraryStatistics(nullableInstantOf(rs.getTimestamp(1)), rs.getInt(2), rs.getInt(3),
                rs.getInt(4), rs.getInt(5), rs.getLong(6), rs.getLong(7));
    };

    public StaticsDao(DaoHelper daoHelper) {
        super(daoHelper);
    }

    public void createScanLog(@NonNull Instant scanDate, @NonNull ScanLogType type) {
        update("insert into scan_log (start_date, type) values (?, ?)", scanDate, type.name());
    }

    public void deleteBefore(Instant retention) {
        update("delete from scan_log where start_date < ?", retention);
    }

    public void deleteOtherThanLatest() {
        update("delete from scan_log where start_date not in(select start_date from scan_log "
                + "where type = 'SCAN_ALL' order by start_date desc limit 1)");
    }

    public @Nullable MediaLibraryStatistics getRecentMediaLibraryStatistics() {
        String sql = "select start_date, folder_id, sum(artist_count) as artist_count, sum(album_count) as album_count, "
                + "sum(song_count) as song_count, sum(total_size) as total_size, sum(total_duration) as total_duration "
                + "from media_library_statistics "
                + "where start_date = (select max(start_date) from scan_log where type = 'SCAN_ALL') "
                + "group by start_date, folder_id, artist_count, album_count, song_count, total_size, total_duration";
        return queryOne(sql, libStatsMapper);
    }

    public boolean isNeverScanned() {
        return queryForInt("select count(*) from scan_log where type='SCAN_ALL'", 0) == 0;
    }

    public void createMediaLibraryStatistics(MediaLibraryStatistics stats) {
        String sql = "insert into media_library_statistics (start_date, folder_id, artist_count, album_count, "
                + "song_count, total_size, total_duration) values (?, ?, ?, ?, ?, ?, ?)";
        update(sql, stats.getExecuted(), stats.getFolderId(), stats.getArtistCount(), stats.getAlbumCount(),
                stats.getSongCount(), stats.getTotalSize(), stats.getTotalDuration());
    }

    public enum ScanLogType {
        SCAN_ALL, EXPUNGE, PODCAST_REFRESH_ALL
    }
}
