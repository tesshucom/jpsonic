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

package com.tesshu.jpsonic.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.util.Properties;

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.spring.AirsonicHsqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public final class LegacyHsqlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHsqlUtil.class);
    public static final String UPGRADE_NEEDED_VERSION1 = "1.8.0";
    public static final String UPGRADE_NEEDED_VERSION2 = "1.8.1";

    private LegacyHsqlUtil() {
    }

    /**
     * Return the current version of the HSQLDB database, as reported by the database properties file.
     */
    static String getHsqldbDatabaseVersion() {
        File configFile = new File(SettingsService.getDefaultJDBCPath() + ".properties");
        if (!configFile.exists()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("HSQLDB database doesn't exist, cannot determine version");
            }
            return null;
        }
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(configFile));
            return properties.getProperty("version");
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to determine HSQLDB database version", e);
            }
            return null;
        }
    }

    /**
     * Check if you need to upgrade your HSQLDB database file. This version of Jpsonic does not offer HSQLDB 1.x data
     * conversions. HSQLDB 2.5.0 is required for 1.x data conversion, and data converters that depend on it are
     * supported by Jpsonic v110.x.x. So what is done here is to check if HSQLDB 1.x is about to be loaded, and if so,
     * to display a message prompting for data conversion by v110.x.x.
     */
    public static void checkHsqldbDatabaseVersion() {

        // Check the current database version
        String currentVersion = getHsqldbDatabaseVersion();
        if (currentVersion == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("HSQLDB database not found, skipping upgrade checks");
            }
            return;
        }

        // Check the database driver version
        String driverVersion;
        try {
            Driver driver = (Driver) Class
                    .forName("org.hsqldb.jdbc.JDBCDriver", true, Thread.currentThread().getContextClassLoader())
                    .getDeclaredConstructor().newInstance();
            driverVersion = String.format("%d.%d", driver.getMajorVersion(), driver.getMinorVersion());
            if (driver.getMajorVersion() != AirsonicHsqlDatabase.CURRENT_SUPPORTED_MAJOR_VERSION) {
                LOG.warn(
                        "HSQLDB database driver version {} is untested ; trying to connect anyway, this may upgrade the database from version {}",
                        driverVersion, currentVersion);
                return;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOG.warn(
                    "HSQLDB database driver version cannot be determined ; trying to connect anyway, this may upgrade the database from version {}",
                    currentVersion, e);
            return;
        }

        // Log what we're about to do and determine if we should perform a controlled upgrade with backups.
        if (currentVersion.startsWith(driverVersion)) {
            // If we're already on the same version as the driver, nothing should happen.
            return;
        } else if (UPGRADE_NEEDED_VERSION1.equals(currentVersion) || UPGRADE_NEEDED_VERSION2.equals(currentVersion)) {
            // If we're on a 1.8.0 or 1.8.1 database and upgrading to 2.x, we're going to handle this manually and check
            // what we're doing.
            if (LOG.isInfoEnabled()) {
                LOG.info("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
                LOG.info("This version of Jpsonic does not support conversion from 1.x to 2.x. "
                        + "Please get https://github.com/jpsonic/jpsonic/releases/tag/v110.2.0 and upgrade your data of database.");
            }
            return;
        } else {
            // If this happens we're on a completely untested version and we don't know what will happen.
            LOG.warn("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            LOG.info("This version of Jpsonic does not support conversion from 1.x to 2.x. "
                    + "Please get https://github.com/jpsonic/jpsonic/releases/tag/v110.2.0 and upgrade your data of database.");
            return;
        }
    }
}
