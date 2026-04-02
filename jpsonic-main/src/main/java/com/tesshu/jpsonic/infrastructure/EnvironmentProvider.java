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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.nio.charset.Charset;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.system.IndexGeneration;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.PathValidator;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides environment-dependent values and initializes required directories
 * for Jpsonic at application startup.
 *
 * <p>
 * This provider centralizes access to runtime environment information such as:
 * <ul>
 * <li>Operating system detection</li>
 * <li>Resolution of the Jpsonic home directory (including system property
 * overrides)</li>
 * <li>Default values derived from environment variables or system
 * properties</li>
 * <li>Application-specific paths such as database and log locations</li>
 * </ul>
 *
 * <p>
 * Configuration keys used by this provider are centrally defined in
 * {@link EnvKeys}.
 *
 * <p>
 * The home directory is initialized on first access. If the directory does not
 * exist, it is created to ensure the application can operate correctly. This
 * initialization occurs only once during the application's lifecycle.
 *
 * <p>
 * EnvironmentProvider is an internal infrastructure component. It is not
 * intended as a public API surface, but is exposed to the service layer for
 * dependency injection and environment resolution.
 */
public class EnvironmentProvider {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentProvider.class);
    private static final EnvironmentProvider INSTANCE = new EnvironmentProvider();

    private static final String APP_NAME = "jpsonic";
    private static final Path JPSONIC_HOME_WINDOWS = Path.of("C:/", APP_NAME);
    private static final Path JPSONIC_HOME_LINUX = Path.of("/var", APP_NAME);
    private static final String INDEX_ROOT_DIR_NAME = "index-JP";

    private final ReentrantLock homeLock = new ReentrantLock();
    private Path cachedHome;

    public static EnvironmentProvider getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void resetCache() {
        homeLock.lock();
        try {
            cachedHome = null;
        } finally {
            homeLock.unlock();
        }
    }

    public String getBrand() {
        return "Jpsonic";
    }

    public String getDefaultJDBCUsername() {
        return EnvKeys.database.localDBUsername.defaultValue;
    }

    public String getDefaultJDBCPassword() {
        return EnvKeys.database.localDBPassword.defaultValue;
    }

    // ============================================================
    // 1. OS / Execution Environment
    // ============================================================

    public boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public String getOsName() {
        return System.getProperty("os.name");
    }

    public String getUserName() {
        return System.getProperty("user.name");
    }

    public String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public String guessGCName() {
        List<String> names = ManagementFactory
            .getGarbageCollectorMXBeans()
            .stream()
            .map(MemoryManagerMXBean::getName)
            .toList();
        if (names.contains("ZGC Cycles") && names.contains("ZGC Pauses")) {
            return "Z GC";
        } else if (names.contains("G1 Young Generation") && names.contains("G1 Old Generation")) {
            return "G1 GC";
        } else if (names.contains("PS MarkSweep") && names.contains("PS Scavenge")) {
            return "Parallel GC";
        } else if (names.contains("Copy") && names.contains("MarkSweepCompact")) {
            return "Serial GC";
        }
        return null;
    }

    // ============================================================
    // 2. Jpsonic HOME / Directory Structure
    // ============================================================

    @NonNull
    Path getJpsonicHome() {
        if (cachedHome != null) {
            return cachedHome;
        }
        homeLock.lock();
        try {
            if (cachedHome != null) {
                return cachedHome;
            }
            String overrideHome = System.getProperty(EnvKeys.filesystem.appHome.envVarName);
            if (overrideHome != null && !overrideHome.isBlank()) {
                cachedHome = Path.of(overrideHome);
            } else {
                cachedHome = isWindows() ? JPSONIC_HOME_WINDOWS : JPSONIC_HOME_LINUX;
            }
            ensureDirectoryPresent(cachedHome);
        } finally {
            homeLock.unlock();
        }
        return cachedHome;
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

    public Path getPropertyFilePath() {
        return getJpsonicHome().resolve(APP_NAME + ".properties");
    }

    public Path getLogFilePath() {
        return getJpsonicHome().resolve(APP_NAME + ".log");
    }

    public Path getRollbackFilePath() {
        return getJpsonicHome().resolve("rollback.sql");
    }

    public Path getLocalDatabaseDirectory() {
        return getJpsonicHome().resolve("db");
    }

    public Path getLocalDatabasePropertyFilePath() {
        return getLocalDatabaseDirectory().resolve(APP_NAME + ".properties");
    }

    public Path getDatabaseLogFilePath() {
        return getLocalDatabaseDirectory().resolve(APP_NAME + ".log");
    }

    public Path getImageCacheDirectory(int size) {
        return getJpsonicHome().resolve("thumbs" + size);
    }

    public Path getEhCacheDirectory() {
        return getJpsonicHome().resolve("cache");
    }

    public Path getLastfmCacheDirectory() {
        return getJpsonicHome().resolve("lastfmcache");
    }

    public Path getTranscodeDirectory() {
        return getJpsonicHome().resolve("transcode");
    }

    public Path getFfmpegPath() {
        return getTranscodeDirectory().resolve(isWindows() ? "ffmpeg.exe" : "ffmpeg");
    }

    public Path getFfprobePath() {
        return getTranscodeDirectory().resolve(isWindows() ? "ffprobe.exe" : "ffprobe");
    }

    // ============================================================
    // 3. Configuration Values (Environment Variables & System Properties)
    // ============================================================

    public int getDefaultUPnPPort() {
        return Optional
            .ofNullable(System.getProperty(EnvKeys.network.upnpPort.envVarName))
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .orElse(EnvKeys.network.upnpPort.defaultValue);
    }

    public boolean isScanOnBoot() {
        return Optional
            .ofNullable(System.getProperty(EnvKeys.application.scanOnBoot.envVarName))
            .map(Boolean::parseBoolean)
            .orElse(EnvKeys.application.scanOnBoot.defaultValue);
    }

    public boolean isEmbeddedFonts() {
        return Optional
            .ofNullable(System.getProperty(EnvKeys.application.embeddedFont.envVarName))
            .map(Boolean::parseBoolean)
            .orElse(EnvKeys.application.embeddedFont.defaultValue);
    }

    public boolean isSuppressTomcatCaching() {
        return Optional
            .ofNullable(System.getProperty(EnvKeys.application.suppressTomcatCaching.envVarName))
            .map(Boolean::parseBoolean)
            .orElse(EnvKeys.application.suppressTomcatCaching.defaultValue);
    }

    @Nullable
    public String getRememberMeKey() {
        String key = System.getProperty(EnvKeys.application.rememberMeKey.envVarName);
        return StringUtils.isBlank(key) ? null : key;
    }

    private String resolveDefaultFolder(String key, String winDefault, String linuxDefault) {
        String arg = System.getProperty(key);
        if (PathValidator.validateFolderPath(arg).isEmpty()) {
            return isWindows() ? winDefault : linuxDefault;
        }
        return arg;
    }

    public String getDefaultMusicFolder() {
        return resolveDefaultFolder(EnvKeys.application.defaultMusicFolder.envVarName, "c:\\music",
                "/var/music");
    }

    public String getDefaultPodcastFolder() {
        return resolveDefaultFolder(EnvKeys.application.defaultPodcastFolder.envVarName,
                "c:\\music\\Podcast", "/var/music/Podcast");
    }

    public String getDefaultPlaylistFolder() {
        return resolveDefaultFolder(EnvKeys.application.defaultPlaylistFolder.envVarName,
                "c:\\playlists", "/var/playlists");
    }

    @NonNull
    public String getMemeDsf() {
        return StringUtils
            .defaultIfBlank(System.getProperty(EnvKeys.misc.mimeDsf.envVarName),
                    EnvKeys.misc.mimeDsf.defaultValue);
    }

    @NonNull
    public String getMemeDff() {
        return StringUtils
            .defaultIfBlank(System.getProperty(EnvKeys.misc.mimeDff.envVarName),
                    EnvKeys.misc.mimeDff.defaultValue);
    }

    // ============================================================
    // 4. Values Derived from HOME
    // ============================================================

    public Path getCurrentIndexRootDirectory() {
        return getJpsonicHome().resolve(INDEX_ROOT_DIR_NAME + IndexGeneration.CURRENT.value());
    }

    public String buildDefaultJDBCUrl() {
        return "jdbc:hsqldb:file:" + getJpsonicHome().toString() + "/db/" + APP_NAME
                + ";sql.enforce_size=false;sql.regular_names=false";
    }

    // ============================================================
    // 5. Lucene / Index Management
    // ============================================================

    private void deleteIndexPath(String label, Path old) {
        if (Files.exists(old)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Found " + label + ". Try to delete : {}", old);
            }
            if (Files.isRegularFile(old)) {
                FileUtil.deleteIfExists(old);
            } else {
                FileUtil.deleteDirectory(old);
            }
        }
    }

    public void deleteLegacyFiles() {
        // Delete legacy files unconditionally
        Pattern legacyPattern = Pattern.compile("^lucene\\d+$");
        String legacyName = "index";
        try (Stream<Path> files = Files.list(getJpsonicHome())) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return legacyPattern.matcher(name).matches() || legacyName.equals(name);
            }).forEach(path -> deleteIndexPath("legacy index file", path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deleteOldFiles() {
        // Delete old index files except the current root index directory
        String currentRootDirName = getCurrentIndexRootDirectory().getFileName().toString();
        Pattern indexPattern = Pattern.compile("^" + INDEX_ROOT_DIR_NAME + "\\d+$");
        try (Stream<Path> files = Files.list(getJpsonicHome())) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return indexPattern.matcher(name).matches() && !currentRootDirName.equals(name);
            }).forEach(path -> deleteIndexPath("old index file", path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a directory corresponding to the current index version.
     */
    public void initializeIndexDirectory(Runnable onFirstCreation) {
        Path rootIndexDir = getCurrentIndexRootDirectory();

        // Check if the current version of the index exists
        if (Files.exists(rootIndexDir)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Index was found (index version {}).", IndexGeneration.CURRENT.value());
            }
            return;
        }

        onFirstCreation.run();

        // Attempt to create the index directory
        if (FileUtil.createDirectories(rootIndexDir) == null && LOG.isWarnEnabled()) {
            LOG
                .warn("Failed to create index directory (index version {}).",
                        IndexGeneration.CURRENT.value());
        }
    }

    // ============================================================
    // 6. Locale / Aggregated Information
    // ============================================================

    public LocaleInfo getLocaleInfo() {
        return new LocaleInfo(Locale.getDefault().toString(), System.getProperty("user.language"),
                System.getProperty("user.country"), System.getProperty("file.encoding"),
                System.getProperty("sun.jnu.encoding"),
                System.getProperty("sun.io.unicode.encoding"), System.getenv("LANG"),
                System.getenv("LC_ALL"), Charset.defaultCharset().toString(),
                ZoneOffset.systemDefault().toString());
    }

    private DirectoryInfo getDirectoryInfo(Path dir) {
        try {
            long directorySize = PathUtils.countDirectory(dir).getByteCounter().get();
            FileStore fileStore = Files.getFileStore(dir);
            long usableSpace = fileStore.getUsableSpace();
            long totalSpace = fileStore.getTotalSpace();
            return new DirectoryInfo(directorySize, usableSpace, totalSpace);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public DirectoryInfo getJpsonicHomeInfo() {
        return getDirectoryInfo(getJpsonicHome());
    }

    public DirectoryInfo getDatabaseDirectoryInfo() {
        Path path = getLocalDatabaseDirectory();
        if (!Files.exists(path)) {
            return new DirectoryInfo(0, 0, 0);
        }
        return getDirectoryInfo(path);
    }

    public record LocaleInfo(String localeDefault, String localeUserLanguage,
            String localeUserCountry, String localeFileEncoding, String localeSunJnuEncoding,
            String localeSunIoUnicodeEncoding, String localeLang, String localeLcAll,
            String localeDefaultCharset, String localeDefaultZoneOffset) {
    }

    public record DirectoryInfo(long sizeBytes, long usableSpaceBytes, long totalSpaceBytes) {
    }
}
