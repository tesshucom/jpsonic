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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.Environment;

@ExtendWith(NeedsHome.class)
class DatabaseConfigurationTest {

    private final DatabaseConfiguration configuration = new DatabaseConfiguration(
            ServiceMockUtils.mock(Environment.class));

    @Test
    void testLegacyDataSource() throws SQLException {

        DataSource dataSource = configuration.legacyDataSource();

        // HikariDataSource compatible properties
        try (HikariDataSource hikariDataSource = (HikariDataSource) dataSource) {

            // apps properties
            assertEquals("org.hsqldb.jdbc.JDBCDriver", hikariDataSource.getDriverClassName());
            assertEquals(SettingsService.getDefaultJDBCUrl(), hikariDataSource.getJdbcUrl());
            assertEquals("sa", hikariDataSource.getUsername());
            assertEquals("", hikariDataSource.getPassword());

            assertEquals(60, hikariDataSource.getLoginTimeout());
            assertNull(hikariDataSource.getCatalog());
            assertNull(hikariDataSource.getConnectionInitSql());
            assertEquals(60_000, hikariDataSource.getConnectionTimeout());
            assertEquals(300_000, hikariDataSource.getIdleTimeout());
            assertEquals(1_800_000, hikariDataSource.getMaxLifetime());
            assertEquals(0, hikariDataSource.getKeepaliveTime());
            assertEquals(0, hikariDataSource.getLeakDetectionThreshold());
            assertEquals(0, hikariDataSource.getMinimumIdle());
            assertEquals(8, hikariDataSource.getMaximumPoolSize());
            assertNull(hikariDataSource.getMetricRegistry());
            assertEquals("HikariPool-1", hikariDataSource.getPoolName());
            assertNotNull(hikariDataSource.getHikariPoolMXBean());
            assertNull(hikariDataSource.getSchema());
            assertNull(hikariDataSource.getConnectionTestQuery());
            assertEquals(5_000, hikariDataSource.getValidationTimeout());
            assertTrue(hikariDataSource.getHealthCheckProperties().isEmpty());
            assertNull(hikariDataSource.getHealthCheckRegistry());
            assertNull(hikariDataSource.getTransactionIsolation());
            assertNull(hikariDataSource.getDataSourceClassName());
            assertNull(hikariDataSource.getDataSourceJNDI());
            assertTrue(hikariDataSource.getDataSourceProperties().isEmpty());
            assertNull(hikariDataSource.getExceptionOverrideClassName());
            assertNotNull(hikariDataSource.getHikariConfigMXBean());
            assertNull(hikariDataSource.getScheduledExecutor());
            assertNull(hikariDataSource.getThreadFactory());
            assertEquals(1, hikariDataSource.getInitializationFailTimeout());
        }
    }
}
