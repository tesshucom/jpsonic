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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.InternetRadio;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Provides database services for internet radio.
 *
 * @author Sindre Mehus
 */
@Repository
public class InternetRadioDao {

    private static final Logger LOG = LoggerFactory.getLogger(InternetRadioDao.class);
    private static final String INSERT_COLUMNS = """
            name, stream_url, homepage_url, enabled, changed\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final TemplateWrapper template;
    private final InternetRadioRowMapper rowMapper;

    public InternetRadioDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        rowMapper = new InternetRadioRowMapper();
    }

    public @Nullable InternetRadio getInternetRadioById(int id) {
        String sql = "select " + QUERY_COLUMNS + """
                from internet_radio
                where id=?
                """;
        return template.queryOne(sql, rowMapper, id);
    }

    public List<InternetRadio> getAllInternetRadios() {
        String sql = "select " + QUERY_COLUMNS + " from internet_radio";
        return template.query(sql, rowMapper);
    }

    public void createInternetRadio(InternetRadio radio) {
        String sql = "insert into internet_radio (" + INSERT_COLUMNS + ") values (?, ?, ?, ?, ?)";
        template.update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(),
                radio.getChanged());
        if (LOG.isInfoEnabled()) {
            LOG.info("Created internet radio station " + radio.getName());
        }
    }

    public void deleteInternetRadio(Integer id) {
        String sql = """
                delete from internet_radio
                where id=?
                """;
        template.update(sql, id);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted internet radio station with ID " + id);
        }
    }

    public void updateInternetRadio(InternetRadio radio) {
        String sql = """
                update internet_radio
                set name=?, stream_url=?, homepage_url=?, enabled=?, changed=?
                where id=?
                """;
        template.update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(),
                radio.getChanged(), radio.getId());
    }

    private static class InternetRadioRowMapper implements RowMapper<InternetRadio> {
        @Override
        public InternetRadio mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InternetRadio(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5),
                    nullableInstantOf(rs.getTimestamp(6)));
        }
    }
}
