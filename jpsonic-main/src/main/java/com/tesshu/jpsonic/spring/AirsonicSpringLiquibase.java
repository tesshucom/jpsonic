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

package com.tesshu.jpsonic.spring;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirsonicSpringLiquibase extends SpringLiquibase {

    private static final Logger LOG = LoggerFactory.getLogger(AirsonicSpringLiquibase.class);

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // liquibase/Scope#enter
    @Override
    public void afterPropertiesSet() throws LiquibaseException {

        try {
            // Suppress console log. May be fixed in the future. #1476
            Scope.enter(Map.of(Scope.Attr.ui.name(), new LoggerUIService()));
        } catch (Exception e) {
            throw new LiquibaseException(e);
        }

        LOG.trace("Starting Liquibase Update");
        try {
            super.afterPropertiesSet();
        } catch (LiquibaseException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("""
                        ===============================================
                        An exception occurred during database migration
                        A rollback file has been generated at %s
                        Execute it within your database to rollback any changes
                        The exception is as follows
                        ===============================================
                        """.formatted(rollbackFile.getAbsolutePath()), e);
            }
            throw e;
        }
    }

    @SuppressWarnings("PMD.CloseResource") // liquibaseConnection should not be closed here
    @Override
    protected Database createDatabase(Connection c, ResourceAccessor resourceAccessor)
            throws DatabaseException {
        DatabaseConnection liquibaseConnection;
        if (c == null) {
            log
                .warning(
                        "Null connection returned by liquibase datasource. Using offline unknown database");
            liquibaseConnection = new OfflineConnection("offline:unknown", resourceAccessor);

        } else {
            liquibaseConnection = new JdbcConnection(c);
        }
        DatabaseFactory factory = DatabaseFactory.getInstance();
        overrideHsqlDbImplementation(factory);
        Database database = factory.findCorrectDatabaseImplementation(liquibaseConnection);
        if (trimToNull(this.defaultSchema) != null) {
            database.setDefaultSchemaName(this.defaultSchema);
        }
        return database;
    }

    private void overrideHsqlDbImplementation(DatabaseFactory factory) {
        List<Database> implementedDatabases = factory.getImplementedDatabases();
        factory.clearRegistry();
        removeCurrentHsqlDb(implementedDatabases);
        implementedDatabases.forEach(factory::register);
        factory.register(new AirsonicHsqlDatabase());
    }

    private void removeCurrentHsqlDb(List<Database> implementedDatabases) {
        implementedDatabases.removeIf(db -> db instanceof HsqlDatabase);
    }

}
