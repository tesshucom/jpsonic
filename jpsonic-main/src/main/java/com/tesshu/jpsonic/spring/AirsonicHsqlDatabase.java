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
