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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(EnvironmentConfiguration.class)
class EnvironmentProviderTest {

    @Autowired
    private EnvironmentProvider provider;

    @Nested
    class DefaultHomeTests {

        @TempDir
        private static Path tempDir;

        @BeforeAll
        static void init() {
            System.setProperty("jpsonic.home", tempDir.toString());
        }

        @Test
        void testDefaultHomeDirectory() {
            Path home = provider.getJpsonicHome();
            assertEquals(tempDir, home);
        }
    }

    @Nested
    class TempHomeTests {

        private static Path tempHome;

        @TempDir
        private static Path tempDir;

        @BeforeAll
        static void init() {
            tempHome = tempDir;
            System.setProperty("jpsonic.home", tempHome.toString());
        }

        @DynamicPropertySource
        static void overrideHome(DynamicPropertyRegistry registry) {
            System.setProperty("jpsonic.home", tempHome.toString());
        }

        @Test
        void testLocalDatabasePropertiesFile() {
            assertEquals(tempHome.resolve("db").resolve("jpsonic").resolve(".properties"),
                    provider.getLocalDatabasePropertiesFile());
        }

        @Test
        void testLogFile() {
            assertEquals(tempHome.resolve("jpsonic").resolve(".log"), provider.getLogFile());
        }

        @Test
        void testDefaultJdbcUsername() {
            assertEquals(EnvKeys.database.localDBUsername.defaultValue,
                    provider.getDefaultJDBCUsername());
        }

        @Test
        void testDefaultJdbcPassword() {
            assertEquals(EnvKeys.database.localDBPassword.defaultValue,
                    provider.getDefaultJDBCPassword());
        }

        @Test
        void testDefaultUpnpPort() {
            System.clearProperty(EnvKeys.network.upnpPort.envVarName);
            assertEquals(EnvKeys.network.upnpPort.defaultValue, provider.getDefaultUPnPPort());
        }

        @Test
        void testOverriddenUpnpPort() {
            System.setProperty(EnvKeys.network.upnpPort.envVarName, "12345");
            assertEquals(12_345, provider.getDefaultUPnPPort());
        }
    }
}
