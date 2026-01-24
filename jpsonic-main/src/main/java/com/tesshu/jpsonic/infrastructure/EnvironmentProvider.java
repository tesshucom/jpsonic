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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides environment-dependent values used by Jpsonic.
 *
 * <p>
 * This class centralizes all information derived from the runtime environment,
 * including:
 * <ul>
 * <li>OS detection</li>
 * <li>Jpsonic home directory resolution (with system property override)</li>
 * <li>Environment-based default values (e.g. UPnP port, local DB
 * credentials)</li>
 * <li>Paths derived from the home directory (e.g. DB, logs, cache, etc.)</li>
 * </ul>
 *
 * <p>
 * EnvironmentProvider is strictly responsible for <b>providing values</b>. It
 * must not perform operations such as file deletion, cleanup, or mutation of
 * the filesystem. Such behavior belongs in higher-level services.
 *
 * <p>
 * All environment-dependent paths used throughout the application should be
 * obtained through this provider to ensure consistency and testability.
 */
class EnvironmentProvider {

    // ============================================================
    // 1. Environment Facts (OS / System Properties / Home)
    // ============================================================

    private static final String APP_NAME = "jpsonic";
    private static final Path JPSONIC_HOME_WINDOWS = Path.of("C:/", APP_NAME);
    private static final Path JPSONIC_HOME_LINUX = Path.of("/var", APP_NAME);

    private final ReentrantLock homeLock = new ReentrantLock();

    private Path home;

    boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    // 1. Jpsonic Home / Directory Utilities
    @NonNull
    Path getJpsonicHome() {
        if (home != null) {
            return home;
        }
        homeLock.lock();
        try {
            if (home != null) {
                return home;
            }
            String overrideHome = System.getProperty(EnvKeys.filesystem.appHome.envVarName);
            if (overrideHome != null && !overrideHome.isBlank()) {
                home = Path.of(overrideHome);
            } else {
                home = isWindows() ? JPSONIC_HOME_WINDOWS : JPSONIC_HOME_LINUX;
            }
            ensureDirectoryPresent(home);
        } finally {
            homeLock.unlock();
        }
        return home;
    }

    private void ensureDirectoryPresent(Path home) {
        if (!Files.exists(home) && !Files.isDirectory(home)
                && FileUtil.createDirectories(home) == null) {
            throw new IllegalStateException("""
                    The directory %s does not exist. \
                    Please create it and make it writable. \
                    (You can override the directory location \
                    by specifying -Djpsonic.home=...
                    when starting the servlet container.)
                    """.formatted(home));
        }
    }

    // ============================================================
    // 2. Environment Defaults
    // ============================================================

    public String getDefaultJDBCUsername() {
        return EnvKeys.database.localDBUsername.defaultValue;
    }

    public String getDefaultJDBCPassword() {
        return EnvKeys.database.localDBPassword.defaultValue;
    }

    public int getDefaultUPnPPort() {
        return Optional
            .ofNullable(System.getProperty(EnvKeys.network.upnpPort.envVarName))
            .map(Integer::parseInt)
            .orElse(EnvKeys.network.upnpPort.defaultValue);
    }

    // ============================================================
    // 3. Environment-Derived Values
    // ============================================================

    public Path getLocalDatabaseDirectory() {
        return getJpsonicHome().resolve("db").resolve(APP_NAME);
    }

    public Path getLocalDatabasePropertiesFile() {
        return getLocalDatabaseDirectory().resolve(".properties");
    }

    @NonNull
    public Path getLogFile() {
        return getJpsonicHome().resolve(APP_NAME).resolve(".log");
    }
}
