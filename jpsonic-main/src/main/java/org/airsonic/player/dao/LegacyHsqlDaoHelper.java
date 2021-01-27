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

package org.airsonic.player.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Special Dao Helper with additional features for managing the legacy embedded HSQL database.
 */
public class LegacyHsqlDaoHelper extends GenericDaoHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlDaoHelper.class);

    public LegacyHsqlDaoHelper(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void checkpoint() {
        // HSQLDB (at least version 1) does not handle automatic checkpoints very well by default.
        // This makes sure the temporary log is actually written to more persistent storage.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Database checkpoint in progress...");
        }
        getJdbcTemplate().execute("CHECKPOINT DEFRAG");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Database checkpoint complete.");
        }
    }

    /**
     * Shutdown the embedded HSQLDB database. After this has run, the database cannot be accessed again from the same
     * DataSource.
     */
    private void shutdownHsqldbDatabase() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Database shutdown in progress...");
            }
            JdbcTemplate jdbcTemplate = getJdbcTemplate();
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource != null) {
                try (Connection conn = DataSourceUtils.getConnection(dataSource)) {
                    jdbcTemplate.execute("SHUTDOWN");
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Database shutdown complete.");
                }
            } else {
                LOG.debug("Cannot get dataSource.");
            }
        } catch (SQLException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Database shutdown failed", e);
            }
        }
    }

    @PreDestroy
    public void onDestroy() {
        shutdownHsqldbDatabase();
    }
}
