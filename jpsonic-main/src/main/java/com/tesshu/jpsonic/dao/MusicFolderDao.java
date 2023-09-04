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

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Provides database services for music folders.
 *
 * @author Sindre Mehus
 */
@Repository
public class MusicFolderDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(MusicFolderDao.class);
    private static final String INSERT_COLUMNS = """
            path, name, enabled, changed, folder_order\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final MusicFolderRowMapper rowMapper;
    private final UserDao userDao;

    public MusicFolderDao(DaoHelper daoHelper, UserDao userDao) {
        super(daoHelper);
        this.userDao = userDao;
        rowMapper = new MusicFolderRowMapper();
    }

    public List<MusicFolder> getAllMusicFolders() {
        String sql = "select " + QUERY_COLUMNS + """
                from music_folder
                order by enabled desc, folder_order
                """;
        return query(sql, rowMapper);
    }

    public @Nullable MusicFolder getMusicFolderForPath(String path) {
        String sql = "select " + QUERY_COLUMNS + """
                from music_folder
                where path = ?
                """;
        return queryOne(sql, rowMapper, path);
    }

    public void createMusicFolder(MusicFolder musicFolder) {
        String sql = """
                insert into music_folder (%s)
                values (?, ?, ?, ?,
                        (select count(*) + 1 from music_folder))
                """.formatted(INSERT_COLUMNS);
        update(sql, musicFolder.getPathString(), musicFolder.getName(), musicFolder.isEnabled(),
                musicFolder.getChanged());

        Integer id = queryForInt("select max(id) from music_folder", 0);
        update("""
                insert into music_folder_user (music_folder_id, username)
                select ?, username from %s
                """.formatted(userDao.getUserTable()), id);
        if (LOG.isInfoEnabled()) {
            LOG.info("Created music folder " + musicFolder.getPathString());
        }
    }

    public void deleteMusicFolder(Integer id) {
        String sql = """
                delete from music_folder
                where id=?
                """;
        update(sql, id);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted music folder with ID " + id);
        }
    }

    public void updateMusicFolder(@NonNull MusicFolder musicFolder) {
        String sql = """
                update music_folder
                set path=?, name=?, enabled=?, changed=?, folder_order=?
                where id=?
                """;
        update(sql, musicFolder.getPathString(), musicFolder.getName(), musicFolder.isEnabled(),
                musicFolder.getChanged(),
                defaultIfNull(musicFolder.getFolderOrder(), queryForInt("select count(*) from music_folder", -1)),
                musicFolder.getId());
    }

    public List<MusicFolder> getMusicFoldersForUser(String username) {
        String sql = "select " + prefix(QUERY_COLUMNS, "music_folder") + """
                from music_folder, music_folder_user
                where music_folder.id = music_folder_user.music_folder_id
                        and music_folder_user.username = ?
                order by enabled desc, folder_order
                """;
        return query(sql, rowMapper, username);
    }

    public void setMusicFoldersForUser(String username, List<Integer> musicFolderIds) {
        update("""
                delete from music_folder_user
                where username = ?
                """, username);
        for (Integer musicFolderId : musicFolderIds) {
            update("insert into music_folder_user(music_folder_id, username) values (?, ?)", musicFolderId, username);
        }
    }

    private static class MusicFolderRowMapper implements RowMapper<MusicFolder> {
        @Override
        public MusicFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MusicFolder(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4),
                    nullableInstantOf(rs.getTimestamp(5)), rs.getInt(6));
        }
    }
}
