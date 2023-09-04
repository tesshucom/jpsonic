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

package com.tesshu.jpsonic.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tesshu.jpsonic.domain.Transcoding;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides database services for transcoding configurations.
 *
 * @author Sindre Mehus
 */
@Repository
public class TranscodingDao extends AbstractDao {

    private static final String INSERT_COLUMNS = """
            name, source_formats, target_format, step1, step2, step3, default_active\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final TranscodingRowMapper rowMapper;

    public TranscodingDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new TranscodingRowMapper();
    }

    public List<Transcoding> getAllTranscodings() {
        String sql = "select " + QUERY_COLUMNS + "from transcoding2";
        return query(sql, rowMapper);
    }

    public List<Transcoding> getTranscodingsForPlayer(Integer playerId) {
        String sql = "select " + QUERY_COLUMNS + """
                from transcoding2, player_transcoding2
                where player_transcoding2.player_id = ?
                        and player_transcoding2.transcoding_id = transcoding2.id
                """;
        return query(sql, rowMapper, playerId);
    }

    @Transactional
    public void setTranscodingsForPlayer(Integer playerId, int... transcodingIds) {
        update("""
                delete from player_transcoding2
                where player_id = ?
                """, playerId);
        String sql = """
                insert into player_transcoding2(player_id, transcoding_id)
                values (?, ?)
                """;
        for (int transcodingId : transcodingIds) {
            update(sql, playerId, transcodingId);
        }
    }

    @Transactional
    public int createTranscoding(Transcoding transcoding) {
        Integer existingMax = queryForInt("""
                select max(id)
                from transcoding2
                """, 0);
        int registered = existingMax + 1;
        transcoding.setId(registered);
        String sql = "insert into transcoding2 (" + QUERY_COLUMNS + ") values (" + questionMarks(QUERY_COLUMNS) + ")";
        update(sql, transcoding.getId(), transcoding.getName(), transcoding.getSourceFormats(),
                transcoding.getTargetFormat(), transcoding.getStep1(), transcoding.getStep2(), transcoding.getStep3(),
                transcoding.isDefaultActive());
        return registered;
    }

    public void deleteTranscoding(Integer id) {
        String sql = """
                delete from transcoding2
                where id=?
                """;
        update(sql, id);
    }

    public void updateTranscoding(Transcoding transcoding) {
        String sql = """
                update transcoding2
                set name=?, source_formats=?, target_format=?,
                        step1=?, step2=?, step3=?, default_active=?
                where id=?
                """;
        update(sql, transcoding.getName(), transcoding.getSourceFormats(), transcoding.getTargetFormat(),
                transcoding.getStep1(), transcoding.getStep2(), transcoding.getStep3(), transcoding.isDefaultActive(),
                transcoding.getId());
    }

    private static class TranscodingRowMapper implements RowMapper<Transcoding> {
        @Override
        public Transcoding mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Transcoding(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getString(7), rs.getBoolean(8));
        }
    }
}
