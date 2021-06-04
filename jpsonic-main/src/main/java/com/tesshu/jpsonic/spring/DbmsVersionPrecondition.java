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
