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

import static com.tesshu.jpsonic.dao.base.DaoUtils.nullableInstantOf;
import static com.tesshu.jpsonic.dao.base.DaoUtils.questionMarks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.Bookmark;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides database services for media file bookmarks.
 *
 * @author Sindre Mehus
 */
@Repository
public class BookmarkDao {

    private static final String INSERT_COLUMNS = """
            media_file_id, position_millis, username, comment, created, changed\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final TemplateWrapper template;
    private final BookmarkRowMapper bookmarkRowMapper;

    public BookmarkDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        bookmarkRowMapper = new BookmarkRowMapper();
    }

    @Deprecated
    public List<Bookmark> getBookmarks() {
        String sql = "select " + QUERY_COLUMNS + "from bookmark";
        return template.query(sql, bookmarkRowMapper);
    }

    public List<Bookmark> getBookmarks(String username) {
        String sql = "select " + QUERY_COLUMNS + """
                from bookmark
                where username=?
                """;
        return template.query(sql, bookmarkRowMapper, username);
    }

    @Transactional
    public void createOrUpdateBookmark(Bookmark bookmark) {
        int n = template
            .update("""
                    update bookmark
                    set position_millis=?, comment=?, changed=?
                    where media_file_id=? and username=?
                    """, bookmark.getPositionMillis(), bookmark.getComment(), bookmark.getChanged(),
                    bookmark.getMediaFileId(), bookmark.getUsername());

        if (n == 0) {
            template
                .update("insert into bookmark (" + INSERT_COLUMNS + ") values ("
                        + questionMarks(INSERT_COLUMNS) + ")", bookmark.getMediaFileId(),
                        bookmark.getPositionMillis(), bookmark.getUsername(), bookmark.getComment(),
                        bookmark.getCreated(), bookmark.getChanged());
            int id = template.queryForInt("""
                    select id
                    from bookmark
                    where media_file_id=? and username=?
                    """, 0, bookmark.getMediaFileId(), bookmark.getUsername());
            bookmark.setId(id);
        }
    }

    @Transactional
    public void deleteBookmark(String username, int mediaFileId) {
        template.update("""
                delete from bookmark
                where username=? and media_file_id=?
                """, username, mediaFileId);
    }

    private static class BookmarkRowMapper implements RowMapper<Bookmark> {
        @Override
        public Bookmark mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Bookmark(rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4),
                    rs.getString(5), nullableInstantOf(rs.getTimestamp(6)),
                    nullableInstantOf(rs.getTimestamp(7)));
        }
    }
}
