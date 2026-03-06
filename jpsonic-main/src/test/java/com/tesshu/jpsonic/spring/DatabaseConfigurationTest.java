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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.tesshu.jpsonic.persistence.NeedsDB;
import com.tesshu.jpsonic.persistence.base.LegacyHsqlDaoHelper;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.spring.DatabaseConfiguration.SmartLifecycleLegacyDaoHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

@NeedsDB
@SuppressWarnings("PMD.TooManyStaticImports")
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

    @Nested
    class SmartLifecycleLegacyDaoHelperTest {

        private LegacyHsqlDaoHelper mockDelegate;
        private SmartLifecycleLegacyDaoHelper helper;

        @BeforeEach
        void setUp() {
            mockDelegate = mock(LegacyHsqlDaoHelper.class);
            helper = new SmartLifecycleLegacyDaoHelper(mockDelegate);
        }

        /**
         * Decision Table annotations for SmartLifecycleLegacyDaoHelper paths.
         */
        @java.lang.annotation.Documented
        @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
        @java.lang.annotation.Target({ java.lang.annotation.ElementType.METHOD })
        @interface LegacyDaoHelperDecision {

            @interface Conditions {
                @interface Running {
                    @interface True {
                    }

                    @interface False {
                    }
                }
            }

            @interface Result {
                @interface DelegateShutdown {
                }

                @interface CallbackRun {
                }
            }
        }

        @Test
        @LegacyDaoHelperDecision.Conditions.Running.False
        @LegacyDaoHelperDecision.Result.DelegateShutdown
        void testStopWhenNotRunning() {
            assertFalse(helper.isRunning());

            helper.stop();

            verify(mockDelegate).shutdownHsqldbDatabase();
            assertFalse(helper.isRunning());
        }

        @Test
        @LegacyDaoHelperDecision.Conditions.Running.True
        @LegacyDaoHelperDecision.Result.DelegateShutdown
        void testStopAfterStart() {
            helper.start();
            assertTrue(helper.isRunning());

            helper.stop();

            verify(mockDelegate).shutdownHsqldbDatabase();
            assertFalse(helper.isRunning());
        }

        @Test
        @LegacyDaoHelperDecision.Conditions.Running.False
        @LegacyDaoHelperDecision.Result.DelegateShutdown
        @LegacyDaoHelperDecision.Result.CallbackRun
        void testStopWithCallback() {
            Runnable callback = mock(Runnable.class);

            helper.stop(callback);

            verify(mockDelegate).shutdownHsqldbDatabase();
            verify(callback).run();
            assertFalse(helper.isRunning());
        }

        @Test
        void testStartAndIsRunning() {
            assertFalse(helper.isRunning());
            helper.start();
            assertTrue(helper.isRunning());
            helper.stop();
            assertFalse(helper.isRunning());
        }

        @Test
        void testGetPhase() {
            assertEquals(LifecyclePhase.DATABASE.value, helper.getPhase());
        }

        @Test
        void testJdbcTemplateDelegation() {
            helper.getJdbcTemplate();
            verify(mockDelegate, atLeast(0)).getJdbcTemplate();
        }

        @Test
        void testNamedParameterJdbcTemplateDelegation() {
            helper.getNamedParameterJdbcTemplate();
            verify(mockDelegate, atLeast(0)).getNamedParameterJdbcTemplate();
        }

        @Test
        void testDataSourceDelegation() {
            helper.getDataSource();
            verify(mockDelegate, atLeast(0)).getDataSource();
        }

        @Test
        void testCheckpointDelegation() {
            helper.checkpoint();
            verify(mockDelegate).checkpoint();
        }
    }
}
