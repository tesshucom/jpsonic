
package org.airsonic.player.spring;

import liquibase.database.Database;
import liquibase.exception.CustomPreconditionErrorException;
import liquibase.exception.CustomPreconditionFailedException;
import liquibase.exception.DatabaseException;
import liquibase.precondition.CustomPrecondition;

public class DbmsVersionPrecondition implements CustomPrecondition {
    private Integer major;
    private Integer minor;

    @Override
    public void check(Database database) throws CustomPreconditionFailedException, CustomPreconditionErrorException {
        try {
            int dbMajor = database.getDatabaseMajorVersion();
            if (major != null && !major.equals(dbMajor)) {
                throw new CustomPreconditionFailedException(
                        "DBMS Major Version Precondition failed: expected " + major + ", got " + dbMajor);
            }
            int dbMinor = database.getDatabaseMinorVersion();
            if (minor != null && !minor.equals(dbMinor)) {
                throw new CustomPreconditionFailedException(
                        "DBMS Minor Version Precondition failed: expected " + minor + ", got " + dbMinor);
            }
        } catch (DatabaseException e) {
            throw new CustomPreconditionErrorException("Database version check failed.", e);
        }
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

}
