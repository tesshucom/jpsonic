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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static com.tesshu.jpsonic.dao.base.DaoUtils.nullableInstantOf;
import static com.tesshu.jpsonic.dao.base.DaoUtils.questionMarks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.SavedPlayQueue;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides database services for play queues
 *
 * @author Sindre Mehus
 */
@Repository
public class PlayQueueDao {

    private static final String INSERT_COLUMNS = """
            username, \"CURRENT\", position_millis, changed, changed_by\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final TemplateWrapper template;
    private final RowMapper<SavedPlayQueue> rowMapper;

    public PlayQueueDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        rowMapper = new PlayQueueMapper();
    }

    @Transactional
    public SavedPlayQueue getPlayQueue(String username) {
        SavedPlayQueue playQueue = template.queryOne("select " + QUERY_COLUMNS + """
                from play_queue
                where username=?
                """, rowMapper, username);
        if (playQueue == null) {
            return null;
        }
        List<Integer> mediaFileIds = template.queryForInts("""
                select media_file_id
                from play_queue_file
                where play_queue_id = ?
                """, playQueue.getId());
        playQueue.setMediaFileIds(mediaFileIds);
        return playQueue;
    }

    @Transactional
    public void savePlayQueue(SavedPlayQueue playQueue) {
        template.update("""
                delete from play_queue
                where username=?
                """, playQueue.getUsername());
        template
            .update("insert into play_queue(" + INSERT_COLUMNS + ") values ("
                    + questionMarks(INSERT_COLUMNS) + ")", playQueue.getUsername(),
                    playQueue.getCurrentMediaFileId(), playQueue.getPositionMillis(),
                    playQueue.getChanged(), playQueue.getChangedBy());
        int id = template.queryForInt("select max(id) from play_queue", 0);
        playQueue.setId(id);

        for (Integer mediaFileId : playQueue.getMediaFileIds()) {
            template.update("""
                    insert into play_queue_file(play_queue_id, media_file_id)
                    values (?, ?)
                    """, id, mediaFileId);
        }
    }

    private static class PlayQueueMapper implements RowMapper<SavedPlayQueue> {
        @Override
        public SavedPlayQueue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SavedPlayQueue(rs.getInt(1), rs.getString(2), null, rs.getInt(3),
                    rs.getLong(4), nullableInstantOf(rs.getTimestamp(5)), rs.getString(6));
        }
    }
}
