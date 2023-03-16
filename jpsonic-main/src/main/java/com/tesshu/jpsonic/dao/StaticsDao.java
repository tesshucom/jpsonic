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

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.MediaLibraryStatistics;
import com.tesshu.jpsonic.domain.ScanEvent;
import com.tesshu.jpsonic.domain.ScanEvent.ScanEventType;
import com.tesshu.jpsonic.util.LegacyMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StaticsDao extends AbstractDao {

    private final RowMapper<ScanEvent> scanEventMapper = (ResultSet rs, int rowNum) -> {
        return new ScanEvent(nullableInstantOf(rs.getTimestamp(1)), nullableInstantOf(rs.getTimestamp(2)),
                ScanEventType.valueOf(rs.getString(3)), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getString(7));
    };
    private final RowMapper<MediaLibraryStatistics> libStatsMapper = (ResultSet rs, int rowNum) -> {
        return new MediaLibraryStatistics(nullableInstantOf(rs.getTimestamp(1)), rs.getInt(2), rs.getInt(3),
                rs.getInt(4), rs.getInt(5), rs.getLong(6), rs.getLong(7));
    };

    public StaticsDao(DaoHelper daoHelper) {
        super(daoHelper);
    }

    @Transactional
    public void createFolderLog(@NonNull Instant executed, @NonNull ScanEventType type) {
        Instant result = queryForInstant("select start_date from scan_log where start_date = ? and type = ?", null,
                executed, ScanLogType.FOLDER_CHANGED.name());
        if (result == null) {
            createScanLog(executed, ScanLogType.FOLDER_CHANGED);
        }
        createScanEvent(new ScanEvent(executed, now(), type, null, null, null, null));
    }

    public void createScanLog(@NonNull Instant scanDate, @NonNull ScanLogType type) {
        update("insert into scan_log (start_date, type) values (?, ?)", scanDate, type.name());
    }

    public void deleteBefore(Instant retention) {
        String query = "delete from scan_log where type = ? and (start_date <> (select start_date from scan_log "
                + "where type = ? order by start_date desc limit 1)) and start_date < ?";
        update(query, ScanLogType.SCAN_ALL.name(), ScanLogType.SCAN_ALL.name(), retention);
        update(query, ScanLogType.FOLDER_CHANGED.name(), ScanLogType.FOLDER_CHANGED.name(), retention);
        update("delete from scan_log where (type <> ? and type <> ?) and start_date < ?", ScanLogType.SCAN_ALL.name(),
                ScanLogType.FOLDER_CHANGED.name(), retention);
    }

    public void createScanEvent(@NonNull ScanEvent scanEvent) {
        update("insert into scan_event (start_date, executed, type, max_memory, total_memory, free_memory, comment) values(?, ?, ?, ?, ?, ?, ?)",
                scanEvent.getStartDate(), scanEvent.getExecuted(), scanEvent.getType().name(), scanEvent.getMaxMemory(),
                scanEvent.getTotalMemory(), scanEvent.getFreeMemory(), scanEvent.getComment());
    }

    public void deleteOtherThanLatest() {
        String query = "delete from scan_log where type = ? and start_date <> (select start_date from scan_log "
                + "where type = ? order by start_date desc limit 1)";
        update(query, ScanLogType.SCAN_ALL.name(), ScanLogType.SCAN_ALL.name());
        update(query, ScanLogType.FOLDER_CHANGED.name(), ScanLogType.FOLDER_CHANGED.name());
        update("delete from scan_log where (type <> ? and type <> ?)", ScanLogType.SCAN_ALL.name(),
                ScanLogType.FOLDER_CHANGED.name());
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

    public List<ScanEvent> getLastScanAllStatuses() {
        Map<String, Object> args = LegacyMap.of("eventTypes", Arrays.asList(ScanEventType.FINISHED.name(),
                ScanEventType.DESTROYED.name(), ScanEventType.CANCELED.name()), "logType", ScanLogType.SCAN_ALL.name());
        String sql = "select event.* from scan_event event "
                + "join (select start_date from scan_log where type = :logType order by start_date desc limit 1) last_log "
                + "on last_log.start_date = event.start_date where type in (:eventTypes)";
        return namedQuery(sql, scanEventMapper, args);
    }

    public enum ScanLogType {
        SCAN_ALL, EXPUNGE, PODCAST_REFRESH_ALL, FOLDER_CHANGED
    }
}
