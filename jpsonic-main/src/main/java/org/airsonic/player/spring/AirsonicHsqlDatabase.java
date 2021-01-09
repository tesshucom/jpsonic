package org.airsonic.player.spring;

import liquibase.exception.DatabaseException;

public class AirsonicHsqlDatabase extends liquibase.database.core.HsqlDatabase {

    public static final int CURRENT_SUPPORTED_MAJOR_VERSION = 2;

    @Override
    public boolean supportsSchemas() {
        try {
            if (getDatabaseMajorVersion() < CURRENT_SUPPORTED_MAJOR_VERSION) {
                return false;
            } else {
                return super.supportsSchemas();
            }
        } catch (DatabaseException e) {
            return false;
        }
    }
}
