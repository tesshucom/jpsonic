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

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * DAO helper class which creates the data source, and updates the database schema.
 *
 * @author Sindre Mehus
 */
public interface DaoHelper {

    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    JdbcTemplate getJdbcTemplate();

    /**
     * Returns a named parameter JDBC template for performing database operations.
     *
     * @return A named parameter JDBC template.
     */
    NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();

    DataSource getDataSource();

    /**
     * Tries to perform a checkpoint against the database, if supported
     *
     * Database checkpoints will make sure that the database is written on the disk and optimize on-disk storage.
     */
    void checkpoint();
}
