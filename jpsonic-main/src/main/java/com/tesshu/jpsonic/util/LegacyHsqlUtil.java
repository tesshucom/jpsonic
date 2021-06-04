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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletionException;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.spring.AirsonicHsqlDatabase;
import org.apache.commons.io.FileUtils;
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
    public static String getHsqldbDatabaseVersion() {
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
     * Create a new connection to the HSQLDB database.
     */
    public static Connection getHsqldbDatabaseConnection() throws SQLException {
        String url = SettingsService.getDefaultJDBCUrl();
        Properties properties = new Properties();
        properties.put("user", SettingsService.getDefaultJDBCUsername());
        properties.put("password", SettingsService.getDefaultJDBCPassword());
        return DriverManager.getConnection(url, properties);
    }

    /**
     * Check if a HSQLDB database upgrade will occur and backups are needed.
     *
     * DB Driver Likely reason Decision null - new db or non-legacy false - null or !2 something went wrong, we better
     * make copies true 1.x 2.x this is the big upgrade true 2.x 2.x already up to date false
     *
     * @return true if a database backup/migration should be performed
     */
    public static boolean isHsqldbDatabaseUpgradeNeeded() {
        // Check the current database version
        String currentVersion = getHsqldbDatabaseVersion();
        if (currentVersion == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("HSQLDB database not found, skipping upgrade checks");
            }
            return false;
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
                return true;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            LOG.warn(
                    "HSQLDB database driver version cannot be determined ; trying to connect anyway, this may upgrade the database from version {}",
                    currentVersion, e);
            return true;
        }

        // Log what we're about to do and determine if we should perform a controlled upgrade with backups.
        if (currentVersion.startsWith(driverVersion)) {
            // If we're already on the same version as the driver, nothing should happen.
            if (LOG.isDebugEnabled()) {
                LOG.debug("HSQLDB database upgrade unneeded, already on version {}", driverVersion);
            }
            return false;
        } else if (currentVersion.startsWith("2.")) {
            // If the database version is 2.x but older than the driver, the upgrade should be relatively painless.
            if (LOG.isDebugEnabled()) {
                LOG.debug("HSQLDB database will be silently upgraded from version {} to {}", currentVersion,
                        driverVersion);
            }
            return false;
        } else if (UPGRADE_NEEDED_VERSION1.equals(currentVersion) || UPGRADE_NEEDED_VERSION2.equals(currentVersion)) {
            // If we're on a 1.8.0 or 1.8.1 database and upgrading to 2.x, we're going to handle this manually and check
            // what we're doing.
            if (LOG.isInfoEnabled()) {
                LOG.info("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            }
            return true;
        } else {
            // If this happens we're on a completely untested version and we don't know what will happen.
            LOG.warn("HSQLDB database upgrade needed, from version {} to {}", currentVersion, driverVersion);
            return true;
        }
    }

    /**
     * Perform a backup of the HSQLDB database, to a timestamped directory.
     * 
     * @return the path to the backup directory
     */
    public static Path performHsqldbDatabaseBackup() throws IOException {

        Path source = Paths.get(SettingsService.getDefaultJDBCPath()).getParent();
        if (source == null) {
            throw new IOException("Unable to create database backup file.");
        }

        Path fileName = source.getFileName();
        if (fileName == null) {
            throw new IOException("Unable to create database backup file.");
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        Path destination = source.resolveSibling(String.format("%s.backup.%s", fileName, timestamp));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing HSQLDB database backup...");
        }
        FileUtils.copyDirectory(source.toFile(), destination.toFile());
        if (LOG.isInfoEnabled()) {
            LOG.info("HSQLDB database backed up to {}", destination.toString());
        }

        return destination;
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public static void performAdditionOfScript(Path backupDir) throws IOException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing adding the script to the HSQLDB database script....");
        }

        File script = new File(SettingsService.getDBScript());
        File scriptBak = new File(SettingsService.getBackupDBScript(backupDir));
        if (!scriptBak.exists()) {
            LOG.warn("Script does not exist in HSQLDB database.");
            return;
        } else if (!script.canWrite()) {
            LOG.warn("You do not have write permission for the script of HSQLDB database.");
            return;
        }

        final String setRegularNamesFalse = "SET DATABASE SQL REGULAR NAMES FALSE";
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(scriptBak.toURI()))) {
            String line = reader.readLine();
            if (null != line) {
                line = line.trim();
            }
            boolean isRestrict = !setRegularNamesFalse.equals(line);
            if (isRestrict) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Set the Restrict property to false.");
                }
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(script.toURI()))) {
                    writer.write(setRegularNamesFalse + System.getProperty("line.separator"));
                    writer.write(line + System.getProperty("line.separator"));
                    int i = 1;
                    for (line = reader.readLine(); line != null; line = reader.readLine()) {
                        i++;
                        writer.write(line + System.getProperty("line.separator"));
                        if (i % 100 == 0) {
                            writer.flush();
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform an in-place database upgrade from HSQLDB 1.x to 2.x.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    public static void performHsqldbDatabaseUpgrade() throws SQLException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing HSQLDB database upgrade...");
        }

        // This will upgrade HSQLDB on the first connection. This does not
        // use Spring's DataSource, as running SHUTDOWN against it will
        // prevent further connections to the database.
        try (Connection conn = getHsqldbDatabaseConnection()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Database connection established. Current version is: {}",
                        conn.getMetaData().getDatabaseProductVersion());
            }
            // On upgrade, the official documentation recommends that we
            // run 'SHUTDOWN SCRIPT' to compact all the database into a
            // single SQL file.
            //
            // In practice, if we don't do that, we did not observe issues
            // immediately but after the upgrade.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Shutting down database (SHUTDOWN SCRIPT)...");
            }
            try (Statement st = conn.createStatement()) {
                st.execute("SHUTDOWN SCRIPT");
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("HSQLDB database has been upgraded to version {}", getHsqldbDatabaseVersion());
        }
    }

    /**
     * If needed, perform an in-place database upgrade from HSQLDB 1.x to 2.x after having created backups.
     */
    public static void upgradeHsqldbDatabaseSafely() {
        if (LegacyHsqlUtil.isHsqldbDatabaseUpgradeNeeded()) {

            Path backupDir;
            try {
                backupDir = performHsqldbDatabaseBackup();
            } catch (IOException e) {
                throw new CompletionException("Failed to backup HSQLDB database before upgrade", e);
            }
            try {
                performAdditionOfScript(backupDir);
            } catch (IOException e) {
                throw new CompletionException("Script verification/addition of HSQLDB database failed before upgrade",
                        e);
            }
            try {
                performHsqldbDatabaseUpgrade();
            } catch (SQLException e) {
                throw new CompletionException("Failed to upgrade HSQLDB database", e);
            }
        }
    }
}
