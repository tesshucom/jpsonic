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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.infrastructure;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.tesshu.jpsonic.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class EnvironmentProviderTest {

    @BeforeEach
    void reset() {
        EnvironmentProvider.getInstance().resetCache();
    }

    // ============================================================
    // 1. Basic Info
    // ============================================================

    @Test
    void testGetBrand() {
        assertEquals("Jpsonic", EnvironmentProvider.getInstance().getBrand());
    }

    @Test
    void testGetDefaultJDBCUsername() {
        assertEquals(EnvKeys.database.localDBUsername.defaultValue,
                EnvironmentProvider.getInstance().getDefaultJDBCUsername());
    }

    @Test
    void testGetDefaultJDBCPassword() {
        assertEquals(EnvKeys.database.localDBPassword.defaultValue,
                EnvironmentProvider.getInstance().getDefaultJDBCPassword());
    }

    // ============================================================
    // 1. OS / Execution Environment
    // ============================================================

    @EnabledOnOs(OS.WINDOWS)
    @Test
    void testIsWindowsOnWin() {
        assertTrue(EnvironmentProvider.getInstance().isWindows());
    }

    @EnabledOnOs(OS.LINUX)
    @Test
    void testIsWindowsOnLinux() {
        assertFalse(EnvironmentProvider.getInstance().isWindows());
    }

    @Test
    void testGetOsName() {
        assertNotNull(EnvironmentProvider.getInstance().getOsName());
    }

    @Test
    void testGetUserName() {
        assertNotNull(EnvironmentProvider.getInstance().getUserName());
    }

    @Test
    void testGetJavaVersion() {
        assertNotNull(EnvironmentProvider.getInstance().getJavaVersion());
    }

    @Test
    void testGuessGCName() {
        assertNotNull(EnvironmentProvider.getInstance().guessGCName());
    }

    // ============================================================
    // 2. Jpsonic HOME / Directory Structure
    // ============================================================

    @Test
    void testGetJpsonicHome() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertTrue(Files.exists(home));
        assertTrue(Files.isDirectory(home));
    }

    @Test
    void testGetPropertyFilePath() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("jpsonic.properties"),
                EnvironmentProvider.getInstance().getPropertyFilePath());
    }

    @Test
    void testGetLogFilePath() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("jpsonic.log"),
                EnvironmentProvider.getInstance().getLogFilePath());
    }

    @Test
    void testGetRollbackFilePath() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("rollback.sql"),
                EnvironmentProvider.getInstance().getRollbackFilePath());
    }

    @Test
    void testGetLocalDatabaseDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("db"),
                EnvironmentProvider.getInstance().getLocalDatabaseDirectory());
    }

    @Test
    void testGetLocalDatabasePropertyFilePath() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("db/jpsonic.properties"),
                EnvironmentProvider.getInstance().getLocalDatabasePropertyFilePath());
    }

    @Test
    void testGetDatabaseLogFilePath() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("db/jpsonic.log"),
                EnvironmentProvider.getInstance().getDatabaseLogFilePath());
    }

    @Test
    void testGetImageCacheDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("thumbs200"),
                EnvironmentProvider.getInstance().getImageCacheDirectory(200));
    }

    @Test
    void testGetEhCacheDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("cache"),
                EnvironmentProvider.getInstance().getEhCacheDirectory());
    }

    @Test
    void testGetLastfmCacheDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("lastfmcache"),
                EnvironmentProvider.getInstance().getLastfmCacheDirectory());
    }

    @Test
    void testGetTranscodeDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertEquals(home.resolve("transcode"),
                EnvironmentProvider.getInstance().getTranscodeDirectory());
    }

    @Test
    void testGetFfmpegPath() {
        Path trans = EnvironmentProvider.getInstance().getTranscodeDirectory();
        Path p = EnvironmentProvider.getInstance().getFfmpegPath();
        assertTrue(p.startsWith(trans));
    }

    @Test
    void testGetFfprobePath() {
        Path trans = EnvironmentProvider.getInstance().getTranscodeDirectory();
        Path p = EnvironmentProvider.getInstance().getFfprobePath();
        assertTrue(p.startsWith(trans));
    }

    // ============================================================
    // 3. Configuration Values
    // ============================================================

    @Nested
    class GetDefaultUPnPPortTest {

        @Test
        void testDefault() {
            System.clearProperty(EnvKeys.network.upnpPort.envVarName);
            assertEquals(EnvKeys.network.upnpPort.defaultValue,
                    EnvironmentProvider.getInstance().getDefaultUPnPPort());
        }

        @Test
        void testInt() {
            System.setProperty(EnvKeys.network.upnpPort.envVarName, "9999");
            assertEquals(9999, EnvironmentProvider.getInstance().getDefaultUPnPPort());
        }

        @Test
        void testNotInt() {
            System.setProperty(EnvKeys.network.upnpPort.envVarName, "NUMBER");
            assertEquals(EnvKeys.network.upnpPort.defaultValue,
                    EnvironmentProvider.getInstance().getDefaultUPnPPort());
        }
    }

    @Test
    void testIsScanOnBoot() {
        System.clearProperty("jpsonic.scan.onboot");
        assertFalse(EnvironmentProvider.getInstance().isScanOnBoot());
    }

    @Test
    void testIsEmbeddedFonts() {
        System.clearProperty("jpsonic.embeddedfont");
        assertFalse(EnvironmentProvider.getInstance().isEmbeddedFonts());
    }

    @Test
    void testIsSuppressTomcatCaching() {
        System.clearProperty("jpsonic.suppresstomcatcaching");
        assertFalse(EnvironmentProvider.getInstance().isSuppressTomcatCaching());
    }

    @Test
    void testGetRememberMeKey() {
        System.clearProperty("jpsonic.rememberMeKey");
        assertNull(EnvironmentProvider.getInstance().getRememberMeKey());
    }

    @Test
    void testGetDefaultMusicFolder() {
        assertNotNull(EnvironmentProvider.getInstance().getDefaultMusicFolder());
    }

    @Test
    void testGetDefaultMusicFolder4Env() {
        Path fake = EnvironmentProvider.getInstance().getJpsonicHome().resolve("musicFolder");
        System
            .setProperty(EnvKeys.application.defaultMusicFolder.envVarName,
                    fake.toAbsolutePath().toString());
        assertEquals(fake.toString(), EnvironmentProvider.getInstance().getDefaultMusicFolder());
        System.clearProperty(EnvKeys.application.defaultMusicFolder.envVarName);
    }

    @Test
    void testGetDefaultPodcastFolder() {
        assertNotNull(EnvironmentProvider.getInstance().getDefaultPodcastFolder());
    }

    @Test
    void testGetDefaultPlaylistFolder() {
        assertNotNull(EnvironmentProvider.getInstance().getDefaultPlaylistFolder());
    }

    @Test
    void testGetMemeDsf() {
        assertEquals(EnvKeys.misc.mimeDsf.defaultValue,
                EnvironmentProvider.getInstance().getMemeDsf());
    }

    @Test
    void testGetMemeDff() {
        assertEquals(EnvKeys.misc.mimeDff.defaultValue,
                EnvironmentProvider.getInstance().getMemeDff());
    }

    // ============================================================
    // 4. Values Derived from HOME
    // ============================================================

    @Test
    void testGetCurrentIndexRootDirectory() {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        assertTrue(
                EnvironmentProvider.getInstance().getCurrentIndexRootDirectory().startsWith(home));
    }

    @Test
    void testBuildDefaultJDBCUrl() {
        String url = EnvironmentProvider.getInstance().buildDefaultJDBCUrl();
        assertTrue(url.startsWith("jdbc:hsqldb:file:"));
    }

    // ============================================================
    // 5. Lucene / Index Management
    // ============================================================

//    @Test
//    void testInitializeIndexDirectory() {
//        ArtistDao dao = mock(ArtistDao.class);
//        EnvironmentProvider.getInstance().initializeIndexDirectory(dao::deleteAll);
//        verify(dao, times(1)).deleteAll();
//    }

    @Test
    void testDeleteLegacyFiles() throws IOException {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        Path f = home.resolve("lucene2");
        Path d = home.resolve("lucene3");
        Files.createFile(f);
        FileUtil.createDirectories(d);

        EnvironmentProvider.getInstance().deleteLegacyFiles();

        assertFalse(Files.exists(f));
        assertFalse(Files.exists(d));
    }

    @Test
    void testDeleteOldFiles() throws IOException {
        Path home = EnvironmentProvider.getInstance().getJpsonicHome();
        Path d = home.resolve("index-JP22");
        FileUtil.createDirectories(d);

        EnvironmentProvider.getInstance().deleteOldFiles();

        assertFalse(Files.exists(d));
    }

    // ============================================================
    // 6. Locale / Aggregated Information
    // ============================================================

    @Test
    void testGetLocaleInfo() {
        assertNotNull(EnvironmentProvider.getInstance().getLocaleInfo());
    }

    @Test
    void testGetJpsonicHomeInfo() {
        assertNotNull(EnvironmentProvider.getInstance().getJpsonicHomeInfo());
    }

    @Test
    void testGetDatabaseDirectoryInfo() {
        assertNotNull(EnvironmentProvider.getInstance().getDatabaseDirectoryInfo());
    }
}
