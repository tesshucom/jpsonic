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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.spring;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.tesshu.jpsonic.service.SettingsService;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class DatabaseConfigurationTest {

    private DatabaseConfiguration configuration = new DatabaseConfiguration(mock(Environment.class));

    @Test
    void testLegacyDataSource() throws SQLException {
        DataSource dataSource = configuration.legacyDataSource();

        // org.apache.commons.dbcp2.BasicDataSource compatible properties
        try (BasicDataSource basicDataSource = (BasicDataSource) dataSource) {

            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> basicDataSource.getLoginTimeout()).withNoCause();

            // apps properties
            assertEquals("org.hsqldb.jdbcDriver", basicDataSource.getDriverClassName());
            assertEquals(SettingsService.getDefaultJDBCUrl(), basicDataSource.getUrl());
            assertEquals("sa", basicDataSource.getUsername());
            assertEquals("", basicDataSource.getPassword());

            // default properties
            assertFalse(basicDataSource.getAbandonedUsageTracking());
            assertTrue(basicDataSource.getAutoCommitOnReturn());
            assertTrue(basicDataSource.getCacheState());
            assertTrue(basicDataSource.getConnectionInitSqls().isEmpty());
            assertNull(basicDataSource.getDefaultAutoCommit());
            assertNull(basicDataSource.getDefaultCatalog());
            assertNull(basicDataSource.getDefaultQueryTimeout());
            assertNull(basicDataSource.getDefaultReadOnly());
            assertNull(basicDataSource.getDefaultSchema());
            assertEquals(-1, basicDataSource.getDefaultTransactionIsolation());
            assertTrue(basicDataSource.getDisconnectionSqlCodes().isEmpty());
            assertEquals("org.apache.commons.pool2.impl.DefaultEvictionPolicy",
                    basicDataSource.getEvictionPolicyClassName());
            assertFalse(basicDataSource.getFastFailValidation());
            assertEquals(0, basicDataSource.getInitialSize());
            assertNull(basicDataSource.getJmxName());
            assertTrue(basicDataSource.getLifo());
            assertFalse(basicDataSource.getLogAbandoned());
            assertTrue(basicDataSource.getLogExpiredConnections());
            assertEquals(-1, basicDataSource.getMaxConnLifetimeMillis());
            assertEquals(8, basicDataSource.getMaxIdle());
            assertEquals(-1, basicDataSource.getMaxOpenPreparedStatements());
            assertEquals(8, basicDataSource.getMaxTotal());
            assertEquals(-1, basicDataSource.getMaxWaitMillis());
            assertEquals(1800000, basicDataSource.getMinEvictableIdleTimeMillis());
            assertEquals(0, basicDataSource.getMinIdle());
            assertEquals(0, basicDataSource.getNumActive());
            assertEquals(0, basicDataSource.getNumIdle());
            assertEquals(3, basicDataSource.getNumTestsPerEvictionRun());
            assertFalse(basicDataSource.getRemoveAbandonedOnBorrow());
            assertFalse(basicDataSource.getRemoveAbandonedOnMaintenance());
            assertEquals(300, basicDataSource.getRemoveAbandonedTimeout());
            assertTrue(basicDataSource.getRollbackOnReturn());
            assertFalse(basicDataSource.getTestWhileIdle());
            assertEquals(-1, basicDataSource.getTimeBetweenEvictionRunsMillis());
            assertNull(basicDataSource.getValidationQuery());
            assertEquals(-1, basicDataSource.getValidationQueryTimeout());
        }
    }
}
