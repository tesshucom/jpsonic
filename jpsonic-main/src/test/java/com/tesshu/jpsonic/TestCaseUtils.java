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

package com.tesshu.jpsonic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.controller.JAXBWriter;
import com.tesshu.jpsonic.dao.base.DaoHelper;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "PMD.NonThreadSafeSingleton", "PMD.TestClassWithoutTestCases" })
public final class TestCaseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestCaseUtils.class);

    private static Path jpsonicHomeDirForTest;

    private TestCaseUtils() {
    }

    /**
     * Returns the path of the JPSONIC_HOME directory to use for tests. This will
     * create a temporary directory.
     *
     * @return JPSONIC_HOME directory path.
     *
     * @throws RuntimeException if it fails to create the temp directory.
     */
    public static String jpsonicHomePathForTest() {

        if (jpsonicHomeDirForTest == null) {
            try {
                jpsonicHomeDirForTest = Files.createTempDirectory("jpsonic_test_");
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Error while creating temporary JPSONIC_HOME directory for tests", e);
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("JPSONIC_HOME directory will be {}", jpsonicHomeDirForTest);
            }
        }
        return jpsonicHomeDirForTest.toString();
    }

    /**
     * @return current REST api version.
     */
    public static String restApiVersion() {
        return new JAXBWriter(null).getRestProtocolVersion();
    }

    /**
     * Cleans the JPSONIC_HOME directory used for tests.
     */
    public static void cleanJpsonicHomeForTest() throws IOException {

        Path jpsonicHomeDir = Path.of(jpsonicHomePathForTest());
        if (Files.exists(jpsonicHomeDir) && Files.isDirectory(jpsonicHomeDir)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Delete jpsonic home (ie. {}).", jpsonicHomeDir.toString());
            }
            FileUtil.deleteDirectory(jpsonicHomeDir);
        }
    }

    /**
     * Constructs a map of records count per table.
     *
     * @param daoHelper DaoHelper object
     *
     * @return Map table name -> records count
     */
    public static Map<String, Integer> recordsInAllTables(DaoHelper daoHelper) {
        List<String> tableNames = daoHelper.getJdbcTemplate().queryForList("""
                select table_name
                from information_schema.system_tables
                where table_type <> 'SYSTEM TABLE'
                """, String.class);
        return tableNames
            .stream()
            .collect(Collectors.toMap(table -> table, table -> recordsInTable(table, daoHelper)));
    }

    /**
     * Counts records in a table.
     */
    public static Integer recordsInTable(String tableName, DaoHelper daoHelper) {
        return daoHelper
            .getJdbcTemplate()
            .queryForObject("select count(1) from " + tableName, Integer.class);
    }

    /**
     * Scans the music library * @param mediaScannerService
     */
    public static void execScan(MediaScannerService mediaScannerService) {
        mediaScannerService.scanLibrary();

        while (mediaScannerService.isScanning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Scan waiting was interrupted.", e);
                }
            }
        }
    }

    public static void setLogLevel(Class<?> clazz, ch.qos.logback.classic.Level level) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(clazz)).setLevel(level);
    }
}
