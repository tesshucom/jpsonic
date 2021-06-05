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

import com.tesshu.jpsonic.domain.InternetRadio;
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
public class InternetRadioDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(InternetRadioDao.class);
    private static final String INSERT_COLUMNS = "name, stream_url, homepage_url, enabled, changed";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final InternetRadioRowMapper rowMapper;

    public InternetRadioDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new InternetRadioRowMapper();
    }

    /**
     * Returns the internet radio station with the given ID.
     *
     * @param id
     *            The unique internet radio station ID.
     * 
     * @return The internet radio station with the given ID, or <code>null</code> if no such internet radio exists.
     */
    public InternetRadio getInternetRadioById(int id) {
        String sql = "select " + QUERY_COLUMNS + " from internet_radio where id=?";
        return queryOne(sql, rowMapper, id);
    }

    /**
     * Returns all internet radio stations.
     *
     * @return Possibly empty list of all internet radio stations.
     */
    public List<InternetRadio> getAllInternetRadios() {
        String sql = "select " + QUERY_COLUMNS + " from internet_radio";
        return query(sql, rowMapper);
    }

    /**
     * Creates a new internet radio station.
     *
     * @param radio
     *            The internet radio station to create.
     */
    public void createInternetRadio(InternetRadio radio) {
        String sql = "insert into internet_radio (" + INSERT_COLUMNS + ") values (?, ?, ?, ?, ?)";
        update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(),
                radio.getChanged());
        if (LOG.isInfoEnabled()) {
            LOG.info("Created internet radio station " + radio.getName());
        }
    }

    /**
     * Deletes the internet radio station with the given ID.
     *
     * @param id
     *            The internet radio station ID.
     */
    public void deleteInternetRadio(Integer id) {
        String sql = "delete from internet_radio where id=?";
        update(sql, id);
        if (LOG.isInfoEnabled()) {
            LOG.info("Deleted internet radio station with ID " + id);
        }
    }

    /**
     * Updates the given internet radio station.
     *
     * @param radio
     *            The internet radio station to update.
     */
    public void updateInternetRadio(InternetRadio radio) {
        String sql = "update internet_radio set name=?, stream_url=?, homepage_url=?, enabled=?, changed=? where id=?";
        update(sql, radio.getName(), radio.getStreamUrl(), radio.getHomepageUrl(), radio.isEnabled(),
                radio.getChanged(), radio.getId());
    }

    private static class InternetRadioRowMapper implements RowMapper<InternetRadio> {
        @Override
        public InternetRadio mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new InternetRadio(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5),
                    rs.getTimestamp(6));
        }
    }

}
