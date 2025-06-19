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

package com.tesshu.jpsonic.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.Theme;
import com.tesshu.jpsonic.service.SettingsConstants.Pair;
import com.tesshu.jpsonic.spring.DataSourceConfigType;
import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.PlayerUtils;
import com.tesshu.jpsonic.util.StringUtil;
import com.tesshu.jpsonic.util.concurrent.ReadWriteLockSupport;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides persistent storage of application settings and preferences.
 *
 * @author Sindre Mehus
 */
@SuppressFBWarnings(value = { "DMI_HARDCODED_ABSOLUTE_FILENAME",
        "SSD_DO_NOT_USE_INSTANCE_LOCK_ON_SHARED_STATIC_DATA" }, justification = "Literal value for which OS is assumed. / False positives for objects stored in immutable maps")
@Service
/*
 * [DefaultPackage] A remnant of legacy, some methods are implemented in package private. This is intended not to be
 * used by other than Service. Little bad practices. Design improvements can be made by resolving Godclass.
 */
public class SettingsService implements ReadWriteLockSupport {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsService.class);

    private static final ReentrantLock HOME_LOCK = new ReentrantLock();
    private static final ReentrantLock THEMES_LOCK = new ReentrantLock();
    private final ReentrantReadWriteLock musicFileTypesLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock videoFileTypesLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock coverArtFileTypesLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock excludedCoverArtsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock localeLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock ignoredArticlesLock = new ReentrantReadWriteLock();
    private final ReentrantLock availableLocalesLock = new ReentrantLock();

    private static final String LOCALES_FILE = "/com/tesshu/jpsonic/i18n/locales.txt";
    private static final String THEMES_FILE = "/com/tesshu/jpsonic/theme/themes.txt";
    private static final Path JPSONIC_HOME_WINDOWS = Path.of("c:/jpsonic");
    private static final Path JPSONIC_HOME_OTHER = Path.of("/var/jpsonic");
    private static final Pair<Integer> ENV_UPNP_PORT = Pair.of("UPNP_PORT", -1);
    private static final String CONSECUTIVE_WHITESPACE = "\\s+";

    // Array of obsolete keys. Used to clean property file.
    private static final List<String> OBSOLETE_KEYS = Arrays.asList("PortForwardingPublicPort",
            "PortForwardingLocalPort", "DownsamplingCommand", "DownsamplingCommand2", "DownsamplingCommand3",
            "DownsamplingCommand4", "AutoCoverBatch", "MusicMask", "VideoMask", "CoverArtMask, HlsCommand",
            "HlsCommand2", "JukeboxCommand", "CoverArtFileTypes", "UrlRedirectCustomHost", "CoverArtLimit",
            "StreamPort", "PortForwardingEnabled", "RewriteUrl", "UrlRedirectCustomUrl", "UrlRedirectContextPath",
            "UrlRedirectFrom", "UrlRedirectionEnabled", "UrlRedirectType", "Port", "HttpsPort",
            "database.varchar.maxlength", "database.config.type", "database.config.embed.driver",
            "database.config.embed.url", "database.config.embed.username", "database.config.embed.password",
            "database.config.jndi.name", "database.usertable.quote", "ShowJavaJukebox", "AnonymousTranscoding",
            "UseSonos", "SearchMethodLegacy", "SearchMethodChanged", "FastCacheEnabled", "UseRefresh", "ShowRefresh",
            "VerboseLogStart", "VerboseLogScanning", "VerboseLogPlaying", "VerboseLogShutdown",
            "IgnoreFileTimestampsNext", "FileModifiedCheckSchemeName", "IgnoreFileTimestampsForEachAlbum", "BufferSize",
            "DlnaIndexVisible", "DlnaIndexId3Visible", "DlnaFolderVisible", "DlnaArtistVisible",
            "DlnaArtistByFolderVisible", "DlnaAlbumVisible", "DlnaPlaylistVisible", "DlnaAlbumByGenreVisible",
            "DlnaSongByGenreVisible", "DlnaRecentAlbumVisible", "DlnaRecentAlbumId3Visible", "DlnaRandomSongVisible",
            "DlnaRandomAlbumVisible", "DlnaRandomSongByArtistVisible", "DlnaRandomSongByFolderArtistVisible",
            "DlnaPodcastVisible", "DlnaGenreCountVisible", "ShowServerLog", "ShowStatus", "PublishPodcast",
            "UseRemovingTrackFromId3Title", "UseCleanUp", "RedundantFolderCheck", "UseCopyOfAsciiUnprintable",
            "UseExternalPlayer", "OthersPlayingEnabled");

    private static final int ELEMENT_COUNT_IN_LINE_OF_THEME = 2;
    private static final AtomicBoolean DEVELOPMENT_MODE = new AtomicBoolean();

    private static List<Theme> themes = Collections.synchronizedList(new ArrayList<>());
    private static List<Locale> locales = Collections.synchronizedList(new ArrayList<>());
    private static List<String> coverArtFileTypes = new ArrayList<>();
    private static List<String> excludedCoverArts = new ArrayList<>();
    private static List<String> musicFileTypes = new ArrayList<>();
    private static List<String> videoFileTypes = new ArrayList<>();
    private static List<String> ignoredArticles = new ArrayList<>();

    private final ApacheCommonsConfigurationService configurationService;
    private final UPnPSubnet uPnPSubnet;

    private static Path home;
    private Pattern excludePattern;
    private Locale locale;

    // getDlnaBaseLANURL is effectively final.(not declared final in order to be injected in the test)
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public SettingsService(ApacheCommonsConfigurationService configurationService, UPnPSubnet uPnPSubnet) {
        super();
        this.configurationService = configurationService;
        this.uPnPSubnet = uPnPSubnet;
        this.uPnPSubnet.setDlnaBaseLANURL(getDlnaBaseLANURL());
    }

    public static boolean isDevelopmentMode() {
        return DEVELOPMENT_MODE.get();
    }

    public static void setDevelopmentMode(boolean b) {
        DEVELOPMENT_MODE.set(b);
    }

    private void removeObsoleteProperties() {
        OBSOLETE_KEYS.forEach(oKey -> {
            if (configurationService.containsKey(oKey)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Removing obsolete property [" + oKey + ']');
                }
                configurationService.clearProperty(oKey);
            }
        });
    }

    private static void ensureDirectoryPresent(Path home) {
        if (!Files.exists(home) && !Files.isDirectory(home) && FileUtil.createDirectories(home) == null) {
            throw new IllegalStateException("""
                    The directory %s does not exist. \
                    Please create it and make it writable. \
                    (You can override the directory location \
                    by specifying -Djpsonic.home=...
                    when starting the servlet container.)
                    """.formatted(home));
        }
    }

    public static @NonNull Path getJpsonicHome() {
        if (home != null && !isDevelopmentMode()) {
            return home;
        }
        HOME_LOCK.lock();
        try {
            if (home != null && !isDevelopmentMode()) {
                return home;
            }
            String overrideHome = System.getProperty("jpsonic.home");
            String oldHome = System.getProperty("libresonic.home");
            if (overrideHome != null) {
                home = Path.of(overrideHome);
            } else if (oldHome != null) {
                home = Path.of(oldHome);
            } else {
                home = PlayerUtils.isWindows() ? JPSONIC_HOME_WINDOWS : JPSONIC_HOME_OTHER;
            }
            ensureDirectoryPresent(home);
        } finally {
            HOME_LOCK.unlock();
        }
        return home;
    }

    public static boolean isScanOnBoot() {
        return Optional.ofNullable(System.getProperty("jpsonic.scan.onboot")).map(Boolean::parseBoolean).orElse(false);
    }

    private static String getFileSystemAppName() {
        String home = getJpsonicHome().toString();
        return home.contains("libresonic") ? "libresonic" : "jpsonic";
    }

    public static String getDefaultJDBCPath() {
        return getJpsonicHome().toString() + "/db/" + getFileSystemAppName();
    }

    public static String getDefaultJDBCUrl() {
        return "jdbc:hsqldb:file:" + getDefaultJDBCPath() + ";sql.enforce_size=false;sql.regular_names=false";
    }

    public static String getDBScript() {
        return getDefaultJDBCPath() + ".script";
    }

    public static String getBackupDBScript(Path backupDir) {
        return backupDir + "/" + getFileSystemAppName() + ".script";
    }

    public static String getDefaultJDBCUsername() {
        return "sa";
    }

    public static String getDefaultJDBCPassword() {
        return "";
    }

    public static int getDefaultUPnPPort() {
        return Optional.ofNullable(System.getProperty(ENV_UPNP_PORT.key)).map(Integer::parseInt)
                .orElse(ENV_UPNP_PORT.defaultValue);
    }

    public static Path getLogFile() {
        return Path.of(getJpsonicHome().toString(), getFileSystemAppName() + ".log");
    }

    static Path getPropertyFile() {
        return Path.of(getJpsonicHome().toString(), getFileSystemAppName() + ".properties");
    }

    private int getInt(Pair<Integer> p) {
        return configurationService.getInteger(p.key, p.defaultValue);
    }

    private long getLong(Pair<Long> p) {
        return configurationService.getLong(p.key, p.defaultValue);
    }

    private boolean getBoolean(Pair<Boolean> p) {
        return configurationService.getBoolean(p.key, p.defaultValue);
    }

    private String getString(Pair<String> p) {
        return configurationService.getString(p.key, p.defaultValue);
    }

    private void setProperty(Pair<?> p, Object value) {
        if (value == null) {
            configurationService.clearProperty(p.key);
        } else {
            configurationService.setProperty(p.key, value);
        }
    }

    public long getSettingsChanged() {
        return getLong(SettingsConstants.SETTINGS_CHANGED);
    }

    public String getJWTKey() {
        return getString(SettingsConstants.JWT_KEY);
    }

    public void setJWTKey(String s) {
        setProperty(SettingsConstants.JWT_KEY, s);
    }

    public String getRememberMeKey() {
        String key = null;
        if (StringUtils.isBlank(key)) {
            key = getString(SettingsConstants.REMEMBER_ME_KEY);
        }
        if (StringUtils.isBlank(key)) {
            key = System.getProperty("airsonic.rememberMeKey");
        }
        return key;
    }

    public void save(boolean updateSettingsChanged) {
        if (updateSettingsChanged) {
            removeObsoleteProperties();
            setProperty(SettingsConstants.SETTINGS_CHANGED, Instant.now().toEpochMilli());
        }
        configurationService.save();
    }

    public void save() {
        save(true);
    }

    public boolean isIgnoreFileTimestamps() {
        return getBoolean(SettingsConstants.MusicFolder.Scan.IGNORE_FILE_TIMESTAMPS);
    }

    public void setIgnoreFileTimestamps(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Scan.IGNORE_FILE_TIMESTAMPS, b);
    }

    /**
     * Returns the number of days between automatic index creation, of -1 if automatic index creation is disabled.
     */
    public int getIndexCreationInterval() {
        return getInt(SettingsConstants.MusicFolder.Scan.INDEX_CREATION_INTERVAL);
    }

    /**
     * Sets the number of days between automatic index creation, of -1 if automatic index creation is disabled.
     */
    public void setIndexCreationInterval(int days) {
        setProperty(SettingsConstants.MusicFolder.Scan.INDEX_CREATION_INTERVAL, days);
    }

    /**
     * Returns the hour of day (0 - 23) when automatic index creation should run.
     */
    public int getIndexCreationHour() {
        return getInt(SettingsConstants.MusicFolder.Scan.INDEX_CREATION_HOUR);
    }

    /**
     * Sets the hour of day (0 - 23) when automatic index creation should run.
     */
    public void setIndexCreationHour(int hour) {
        setProperty(SettingsConstants.MusicFolder.Scan.INDEX_CREATION_HOUR, hour);
    }

    public String getExcludePatternString() {
        return getString(SettingsConstants.MusicFolder.Exclusion.EXCLUDE_PATTERN_STRING);
    }

    @SuppressWarnings("PMD.NullAssignment") // (excludePattern) Intentional allocation to clear cache
    private void compileExcludePattern() {
        if (getExcludePatternString() != null && !StringUtils.isAllBlank(getExcludePatternString())) {
            excludePattern = Pattern.compile(getExcludePatternString());
        } else {
            excludePattern = null;
        }
    }

    public void setExcludePatternString(String s) {
        setProperty(SettingsConstants.MusicFolder.Exclusion.EXCLUDE_PATTERN_STRING, s);
        compileExcludePattern();
    }

    public Pattern getExcludePattern() {
        if (excludePattern == null && getExcludePatternString() != null) {
            compileExcludePattern();
        }
        return excludePattern;
    }

    public boolean isIgnoreSymLinks() {
        return getBoolean(SettingsConstants.MusicFolder.Exclusion.IGNORE_SYMLINKS);
    }

    public void setIgnoreSymLinks(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Exclusion.IGNORE_SYMLINKS, b);
    }

    public List<Locale> getAvailableLocales() {
        if (!locales.isEmpty()) {
            return locales;
        }
        availableLocalesLock.lock();
        try {
            if (locales.isEmpty()) {
                try (InputStream in = SettingsService.class.getResourceAsStream(LOCALES_FILE)) {
                    for (String line : StringUtil.readLines(in)) {
                        locales.add(StringUtil.parseLocale(line));
                    }
                } catch (IOException e) {
                    locales.add(Locale.ENGLISH);
                    throw new UncheckedIOException(e);
                }
            }
            return locales;
        } finally {
            availableLocalesLock.unlock();
        }
    }

    public static String getBrand() {
        return "Jpsonic";
    }

    public Locale getLocale() {
        readLock(localeLock);
        try {
            if (!isEmpty(locale)) {
                return locale;
            }
            writeLock(localeLock);
            try {
                String language = getString(SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE);
                String country = getString(SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY);
                String variant = getString(SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT);
                locale = new Locale(language, country, variant);
                return locale;
            } finally {
                writeUnlock(localeLock);
            }
        } finally {
            readUnlock(localeLock);
        }
    }

    @SuppressWarnings("PMD.NullAssignment") // (locale) Intentional allocation to clear cache
    public void setLocale(Locale locale) {
        writeLock(localeLock);
        try {
            this.locale = null;
            setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE, locale.getLanguage());
            setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY, locale.getCountry());
            setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT, locale.getVariant());
        } finally {
            writeUnlock(localeLock);
        }
    }

    /**
     * Returns a list of available themes.
     *
     * @return A list of available themes.
     */
    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Returns an immutable list without unnecessary copying.")
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity" })
    /*
     * [AvoidInstantiatingObjectsInLoops] (Theme) Cannot be reused but is cached [CognitiveComplexity] #1020 Remove them
     * as they now contain unnecessary processing.
     */
    public static List<Theme> getAvailableThemes() {
        if (!themes.isEmpty()) {
            return themes;
        }
        THEMES_LOCK.lock();
        try (InputStream in = SettingsService.class.getResourceAsStream(THEMES_FILE)) {
            if (!themes.isEmpty()) {
                return themes;
            }
            for (String line : StringUtil.readLines(in)) {
                List<String> elements = StringUtil.split(line);
                if (elements.size() == ELEMENT_COUNT_IN_LINE_OF_THEME) {
                    themes.add(new Theme(elements.get(0), elements.get(1)));
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Failed to parse theme from line: [" + line + "].");
                    }
                }
            }
            return themes;
        } catch (IOException e) {
            themes.add(new Theme("default", "Jpsonic default"));
            throw new UncheckedIOException(e);
        } finally {
            THEMES_LOCK.unlock();
        }
    }

    public String getThemeId() {
        return getString(SettingsConstants.General.ThemeAndLang.THEME_ID);
    }

    public void setThemeId(String s) {
        setProperty(SettingsConstants.General.ThemeAndLang.THEME_ID, s);
    }

    public String getIndexString() {
        return getString(SettingsConstants.General.Index.INDEX_STRING);
    }

    public void setIndexString(String s) {
        setProperty(SettingsConstants.General.Index.INDEX_STRING, s);
    }

    public static String getDefaultIndexString() {
        return SettingsConstants.General.Index.INDEX_STRING.defaultValue;
    }

    public String getIgnoredArticles() {
        readLock(ignoredArticlesLock);
        try {
            return getString(SettingsConstants.General.Index.IGNORED_ARTICLES);
        } finally {
            readUnlock(ignoredArticlesLock);
        }
    }

    public void setIgnoredArticles(String s) {
        writeLock(ignoredArticlesLock);
        try {
            setProperty(SettingsConstants.General.Index.IGNORED_ARTICLES, s);
            ignoredArticles.clear();
        } finally {
            writeUnlock(ignoredArticlesLock);
        }
    }

    public List<String> getIgnoredArticlesAsArray() {
        readLock(ignoredArticlesLock);
        try {
            if (ignoredArticles.isEmpty() && !isEmpty(getIgnoredArticles())) {
                ignoredArticles.addAll(Arrays.asList(getIgnoredArticles().split(CONSECUTIVE_WHITESPACE)));
            }
            return ignoredArticles;
        } finally {
            readUnlock(ignoredArticlesLock);
        }
    }

    public boolean isSortAlbumsByYear() {
        return getBoolean(SettingsConstants.General.Sort.ALBUMS_BY_YEAR);
    }

    public void setSortAlbumsByYear(boolean b) {
        setProperty(SettingsConstants.General.Sort.ALBUMS_BY_YEAR, b);
    }

    public static boolean isDefaultSortAlbumsByYear() {
        return SettingsConstants.General.Sort.ALBUMS_BY_YEAR.defaultValue;
    }

    public boolean isSortGenresByAlphabet() {
        return getBoolean(SettingsConstants.General.Sort.GENRES_BY_ALPHABET);
    }

    public void setSortGenresByAlphabet(boolean b) {
        setProperty(SettingsConstants.General.Sort.GENRES_BY_ALPHABET, b);
    }

    public static boolean isDefaultSortGenresByAlphabet() {
        return SettingsConstants.General.Sort.GENRES_BY_ALPHABET.defaultValue;
    }

    public boolean isProhibitSortVarious() {
        return getBoolean(SettingsConstants.General.Sort.PROHIBIT_SORT_VARIOUS);
    }

    public void setProhibitSortVarious(boolean b) {
        setProperty(SettingsConstants.General.Sort.PROHIBIT_SORT_VARIOUS, b);
    }

    public static boolean isDefaultProhibitSortVarious() {
        return SettingsConstants.General.Sort.PROHIBIT_SORT_VARIOUS.defaultValue;
    }

    public boolean isSearchComposer() {
        return getBoolean(SettingsConstants.General.Search.SEARCH_COMPOSER);
    }

    public void setSearchComposer(boolean b) {
        setProperty(SettingsConstants.General.Search.SEARCH_COMPOSER, b);
    }

    public boolean isOutputSearchQuery() {
        return getBoolean(SettingsConstants.General.Search.OUTPUT_SEARCH_QUERY);
    }

    public void setOutputSearchQuery(boolean b) {
        setProperty(SettingsConstants.General.Search.OUTPUT_SEARCH_QUERY, b);
    }

    public boolean isShowRememberMe() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_REMEMBER_ME);
    }

    public void setShowRememberMe(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_REMEMBER_ME, b);
    }

    public boolean isUseRadio() {
        return getBoolean(SettingsConstants.General.Legacy.USE_RADIO);
    }

    public void setUseRadio(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_RADIO, b);
    }

    public boolean isUseJsonp() {
        return getBoolean(SettingsConstants.General.Legacy.USE_JSONP);
    }

    public void setUseJsonp(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_JSONP, b);
    }

    public boolean isShowIndexDetails() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_INDEX_DETAILS);
    }

    public void setShowIndexDetails(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_INDEX_DETAILS, b);
    }

    public boolean isShowDBDetails() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_DB_DETAILS);
    }

    public void setShowDBDetails(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_DB_DETAILS, b);
    }

    public boolean isUseCast() {
        return getBoolean(SettingsConstants.General.Legacy.USE_CAST);
    }

    public void setUseCast(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_CAST, b);
    }

    public boolean isUsePartyMode() {
        return getBoolean(SettingsConstants.General.Legacy.USE_PARTY_MODE);
    }

    public void setUsePartyMode(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_PARTY_MODE, b);
    }

    public String getMusicFileTypes() {
        readLock(musicFileTypesLock);
        try {
            return getString(SettingsConstants.General.Extension.MUSIC_FILE_TYPES);
        } finally {
            readUnlock(musicFileTypesLock);
        }
    }

    public String getDefaultMusicFileTypes() {
        return SettingsConstants.General.Extension.MUSIC_FILE_TYPES.defaultValue;
    }

    public void setMusicFileTypes(String s) {
        writeLock(musicFileTypesLock);
        try {
            setProperty(SettingsConstants.General.Extension.MUSIC_FILE_TYPES, s);
            musicFileTypes.clear();
        } finally {
            writeUnlock(musicFileTypesLock);
        }
    }

    public List<String> getMusicFileTypesAsArray() {
        readLock(musicFileTypesLock);
        try {
            if (musicFileTypes.isEmpty() && !isEmpty(getDefaultMusicFileTypes())) {
                musicFileTypes.addAll(Arrays.asList(getDefaultMusicFileTypes().split(CONSECUTIVE_WHITESPACE)));
            }
            return musicFileTypes;
        } finally {
            readUnlock(musicFileTypesLock);
        }
    }

    public String getVideoFileTypes() {
        readLock(videoFileTypesLock);
        try {
            return getString(SettingsConstants.General.Extension.VIDEO_FILE_TYPES);
        } finally {
            readUnlock(videoFileTypesLock);
        }
    }

    public String getDefaultVideoFileTypes() {
        return SettingsConstants.General.Extension.VIDEO_FILE_TYPES.defaultValue;
    }

    public void setVideoFileTypes(String s) {
        writeLock(videoFileTypesLock);
        try {
            setProperty(SettingsConstants.General.Extension.VIDEO_FILE_TYPES, s);
            videoFileTypes.clear();
        } finally {
            writeUnlock(videoFileTypesLock);
        }
    }

    public List<String> getVideoFileTypesAsArray() {
        readLock(videoFileTypesLock);
        try {
            if (videoFileTypes.isEmpty()) {
                videoFileTypes.addAll(Arrays.asList(getVideoFileTypes().split(CONSECUTIVE_WHITESPACE)));
            }
            return videoFileTypes;
        } finally {
            readUnlock(videoFileTypesLock);
        }
    }

    public String getCoverArtFileTypes() {
        readLock(coverArtFileTypesLock);
        try {
            return getString(SettingsConstants.General.Extension.COVER_ART_FILE_TYPES);
        } finally {
            readUnlock(coverArtFileTypesLock);
        }
    }

    public String getDefaultCoverArtFileTypes() {
        return SettingsConstants.General.Extension.COVER_ART_FILE_TYPES.defaultValue;
    }

    public void setCoverArtFileTypes(String s) {
        writeLock(coverArtFileTypesLock);
        try {
            setProperty(SettingsConstants.General.Extension.COVER_ART_FILE_TYPES, s);
            coverArtFileTypes.clear();
        } finally {
            writeUnlock(coverArtFileTypesLock);
        }
    }

    public List<String> getCoverArtFileTypesAsArray() {
        readLock(coverArtFileTypesLock);
        try {
            if (coverArtFileTypes.isEmpty() && !isEmpty(getCoverArtFileTypes())) {
                coverArtFileTypes.addAll(Arrays.asList(getCoverArtFileTypes().split(CONSECUTIVE_WHITESPACE)));
            }
            return coverArtFileTypes;
        } finally {
            readUnlock(coverArtFileTypesLock);
        }
    }

    public String getExcludedCoverArts() {
        readLock(excludedCoverArtsLock);
        try {
            return getString(SettingsConstants.General.Extension.EXCLUDED_COVER_ART);
        } finally {
            readUnlock(excludedCoverArtsLock);
        }
    }

    public String getDefaultExcludedCoverArts() {
        return SettingsConstants.General.Extension.EXCLUDED_COVER_ART.defaultValue;
    }

    public void setExcludedCoverArts(String s) {
        writeLock(excludedCoverArtsLock);
        try {
            setProperty(SettingsConstants.General.Extension.EXCLUDED_COVER_ART, s);
            excludedCoverArts.clear();
        } finally {
            writeUnlock(excludedCoverArtsLock);
        }
    }

    public List<String> getExcludedCoverArtsAsArray() {
        readLock(excludedCoverArtsLock);
        try {
            if (excludedCoverArts.isEmpty()) {
                excludedCoverArts.addAll(Arrays.asList(getDefaultExcludedCoverArts().split(CONSECUTIVE_WHITESPACE)));
            }
            return excludedCoverArts;
        } finally {
            readUnlock(excludedCoverArtsLock);
        }
    }

    public String getPlaylistFolder() {
        return getString(SettingsConstants.General.Extension.PLAYLIST_FOLDER);
    }

    public String getDefaultPlaylistFolder() {
        return SettingsConstants.General.Extension.PLAYLIST_FOLDER.defaultValue;
    }

    public void setPlaylistFolder(String s) {
        setProperty(SettingsConstants.General.Extension.PLAYLIST_FOLDER, s);
    }

    public String getShortcuts() {
        return getString(SettingsConstants.General.Extension.SHORTCUTS);
    }

    public String getDefaultShortcuts() {
        return SettingsConstants.General.Extension.SHORTCUTS.defaultValue;
    }

    public void setShortcuts(String s) {
        setProperty(SettingsConstants.General.Extension.SHORTCUTS, s);
    }

    public List<String> getShortcutsAsArray() {
        return StringUtil.split(getShortcuts());
    }

    public boolean isGettingStartedEnabled() {
        return getBoolean(SettingsConstants.General.Welcome.GETTING_STARTED_ENABLED);
    }

    public void setGettingStartedEnabled(boolean b) {
        setProperty(SettingsConstants.General.Welcome.GETTING_STARTED_ENABLED, b);
    }

    public String getWelcomeTitle() {
        return StringUtils.trimToNull(getString(SettingsConstants.General.Welcome.TITLE));
    }

    public void setWelcomeTitle(String s) {
        setProperty(SettingsConstants.General.Welcome.TITLE, s);
    }

    public String getWelcomeSubtitle() {
        return StringUtils.trimToNull(getString(SettingsConstants.General.Welcome.SUBTITLE));
    }

    public void setWelcomeSubtitle(String s) {
        setProperty(SettingsConstants.General.Welcome.SUBTITLE, s);
    }

    public String getWelcomeMessage() {
        return StringUtils.trimToNull(getString(SettingsConstants.General.Welcome.MESSAGE));
    }

    public void setWelcomeMessage(String s) {
        setProperty(SettingsConstants.General.Welcome.MESSAGE, s);
    }

    public String getLoginMessage() {
        return StringUtils.trimToNull(getString(SettingsConstants.General.Welcome.LOGIN_MESSAGE));
    }

    public void setLoginMessage(String s) {
        setProperty(SettingsConstants.General.Welcome.LOGIN_MESSAGE, s);
    }

    /**
     * Get the limit in Kbit/s. Zero if unlimited.
     */
    public long getDownloadBitrateLimit() {
        return getLong(SettingsConstants.Advanced.Bandwidth.DOWNLOAD_BITRATE_LIMIT);
    }

    /**
     * Set the limit in Kbit/s. Zero if unlimited.
     */
    public void setDownloadBitrateLimit(long l) {
        setProperty(SettingsConstants.Advanced.Bandwidth.DOWNLOAD_BITRATE_LIMIT, l);
    }

    /**
     * Get the limit in Kbit/s. Zero if unlimited.
     */
    public long getUploadBitrateLimit() {
        return getLong(SettingsConstants.Advanced.Bandwidth.UPLOAD_BITRATE_LIMIT);
    }

    /**
     * Set the limit in Kbit/s. Zero if unlimited.
     */
    public void setUploadBitrateLimit(long l) {
        setProperty(SettingsConstants.Advanced.Bandwidth.UPLOAD_BITRATE_LIMIT, l);
    }

    public int getBufferSize() {
        return getInt(SettingsConstants.Advanced.Bandwidth.BUFFER_SIZE);
    }

    public void setBufferSize(int bufferSize) {
        setProperty(SettingsConstants.Advanced.Bandwidth.BUFFER_SIZE, bufferSize);
    }

    public String getSmtpFrom() {
        return getString(SettingsConstants.Advanced.Smtp.FROM);
    }

    public void setSmtpFrom(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.FROM, s);
    }

    public String getSmtpServer() {
        return getString(SettingsConstants.Advanced.Smtp.SERVER);
    }

    public void setSmtpServer(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.SERVER, s);
    }

    public String getSmtpPort() {
        return getString(SettingsConstants.Advanced.Smtp.PORT);
    }

    public void setSmtpPort(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.PORT, s);
    }

    public String getSmtpEncryption() {
        return getString(SettingsConstants.Advanced.Smtp.ENCRYPTION);
    }

    public void setSmtpEncryption(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.ENCRYPTION, s);
    }

    public String getSmtpUser() {
        return getString(SettingsConstants.Advanced.Smtp.USER);
    }

    public void setSmtpUser(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.USER, s);
    }

    public String getSmtpPassword() {
        String s = getString(SettingsConstants.Advanced.Smtp.PASSWORD);
        try {
            return StringUtil.utf8HexDecode(s);
        } catch (DecoderException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to decode Smtp password.", e);
            }
            return s;
        }
    }

    public void setSmtpPassword(String s) {
        setProperty(SettingsConstants.Advanced.Smtp.PASSWORD, StringUtil.utf8HexEncode(s));
    }

    public boolean isLdapEnabled() {
        return getBoolean(SettingsConstants.Advanced.Ldap.ENABLED);
    }

    public void setLdapEnabled(boolean b) {
        setProperty(SettingsConstants.Advanced.Ldap.ENABLED, b);
    }

    public String getLdapUrl() {
        return getString(SettingsConstants.Advanced.Ldap.URL);
    }

    public void setLdapUrl(String s) {
        setProperty(SettingsConstants.Advanced.Ldap.URL, s);
    }

    public String getLdapSearchFilter() {
        return getString(SettingsConstants.Advanced.Ldap.SEARCH_FILTER);
    }

    public void setLdapSearchFilter(String s) {
        setProperty(SettingsConstants.Advanced.Ldap.SEARCH_FILTER, s);
    }

    public String getLdapManagerDn() {
        return getString(SettingsConstants.Advanced.Ldap.MANAGER_DN);
    }

    public void setLdapManagerDn(String s) {
        setProperty(SettingsConstants.Advanced.Ldap.MANAGER_DN, s);
    }

    public String getLdapManagerPassword() {
        String s = getString(SettingsConstants.Advanced.Ldap.MANAGER_PASSWORD);
        try {
            return StringUtil.utf8HexDecode(s);
        } catch (DecoderException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to decode LDAP manager password.", e);
            }
            return s;
        }
    }

    public void setLdapManagerPassword(final String s) {
        setProperty(SettingsConstants.Advanced.Ldap.MANAGER_PASSWORD, StringUtil.utf8HexEncode(s));
    }

    public boolean isLdapAutoShadowing() {
        return getBoolean(SettingsConstants.Advanced.Ldap.AUTO_SHADOWING);
    }

    public void setLdapAutoShadowing(boolean b) {
        setProperty(SettingsConstants.Advanced.Ldap.AUTO_SHADOWING, b);
    }

    public boolean isCaptchaEnabled() {
        return getBoolean(SettingsConstants.Advanced.Captcha.ENABLED);
    }

    public void setCaptchaEnabled(boolean b) {
        setProperty(SettingsConstants.Advanced.Captcha.ENABLED, b);
    }

    public String getRecaptchaSiteKey() {
        return getString(SettingsConstants.Advanced.Captcha.SITE_KEY);
    }

    public void setRecaptchaSiteKey(String s) {
        setProperty(SettingsConstants.Advanced.Captcha.SITE_KEY, s);
    }

    public String getRecaptchaSecretKey() {
        return getString(SettingsConstants.Advanced.Captcha.SECRET_KEY);
    }

    public void setRecaptchaSecretKey(String s) {
        setProperty(SettingsConstants.Advanced.Captcha.SECRET_KEY, s);
    }

    public boolean isUseScanLog() {
        return getBoolean(SettingsConstants.Advanced.ScanLog.USE_SCAN_LOG);
    }

    public void setUseScanLog(boolean b) {
        setProperty(SettingsConstants.Advanced.ScanLog.USE_SCAN_LOG, b);
    }

    public int getScanLogRetention() {
        return getInt(SettingsConstants.Advanced.ScanLog.SCAN_LOG_RETENTION);
    }

    public void setScanLogRetention(int days) {
        setProperty(SettingsConstants.Advanced.ScanLog.SCAN_LOG_RETENTION, days);
    }

    public int getDefaultScanLogRetention() {
        return SettingsConstants.Advanced.ScanLog.SCAN_LOG_RETENTION.defaultValue;
    }

    public boolean isUseScanEvents() {
        return getBoolean(SettingsConstants.Advanced.ScanLog.USE_SCAN_EVENTS);
    }

    public void setUseScanEvents(boolean b) {
        setProperty(SettingsConstants.Advanced.ScanLog.USE_SCAN_EVENTS, b);
    }

    public boolean isMeasureMemory() {
        return getBoolean(SettingsConstants.Advanced.ScanLog.MEASURE_MEMORY);
    }

    public void setMeasureMemory(boolean b) {
        setProperty(SettingsConstants.Advanced.ScanLog.MEASURE_MEMORY, b);
    }

    public String getIndexSchemeName() {
        return getString(SettingsConstants.Advanced.Index.INDEX_SCHEME_NAME);
    }

    public void setIndexSchemeName(String s) {
        setProperty(SettingsConstants.Advanced.Index.INDEX_SCHEME_NAME, s);
    }

    public boolean isForceInternalValueInsteadOfTags() {
        return getBoolean(SettingsConstants.Advanced.Index.FORCE_INTERNAL_VALUE_INSTEAD_OF_TAGS);
    }

    public void setForceInternalValueInsteadOfTags(boolean b) {
        setProperty(SettingsConstants.Advanced.Index.FORCE_INTERNAL_VALUE_INSTEAD_OF_TAGS, b);
    }

    public boolean isIgnoreFullWidth() {
        return getBoolean(SettingsConstants.Advanced.Index.IGNORE_FULL_WIDTH);
    }

    public void setIgnoreFullWidth(boolean b) {
        setProperty(SettingsConstants.Advanced.Index.IGNORE_FULL_WIDTH, b);
    }

    public boolean isDeleteDiacritic() {
        return getBoolean(SettingsConstants.Advanced.Index.DELETE_DIACRITIC);
    }

    public void setDeleteDiacritic(boolean b) {
        setProperty(SettingsConstants.Advanced.Index.DELETE_DIACRITIC, b);
    }

    public boolean isSortAlphanum() {
        return getBoolean(SettingsConstants.Advanced.Sort.ALPHANUM);
    }

    public void setSortAlphanum(boolean b) {
        setProperty(SettingsConstants.Advanced.Sort.ALPHANUM, b);
    }

    public static boolean isDefaultSortAlphanum() {
        return SettingsConstants.Advanced.Sort.ALPHANUM.defaultValue;
    }

    public boolean isSortStrict() {
        return getBoolean(SettingsConstants.Advanced.Sort.STRICT);
    }

    public void setSortStrict(boolean b) {
        setProperty(SettingsConstants.Advanced.Sort.STRICT, b);
    }

    public static boolean isDefaultSortStrict() {
        return SettingsConstants.Advanced.Sort.STRICT.defaultValue;
    }

    public String getPodcastFolder() {
        return getString(SettingsConstants.Podcast.FOLDER);
    }

    public void setPodcastFolder(String s) {
        setProperty(SettingsConstants.Podcast.FOLDER, s);
    }

    /**
     * Returns the number of hours between Podcast updates, of -1 if automatic updates are disabled.
     */
    public int getPodcastUpdateInterval() {
        return getInt(SettingsConstants.Podcast.UPDATE_INTERVAL);
    }

    /**
     * Sets the number of hours between Podcast updates, of -1 if automatic updates are disabled.
     */
    public void setPodcastUpdateInterval(int i) {
        setProperty(SettingsConstants.Podcast.UPDATE_INTERVAL, i);
    }

    /**
     * Returns the number of Podcast episodes to keep (-1 to keep all).
     */
    public int getPodcastEpisodeRetentionCount() {
        return getInt(SettingsConstants.Podcast.EPISODE_RETENTION_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to keep (-1 to keep all).
     */
    public void setPodcastEpisodeRetentionCount(int i) {
        setProperty(SettingsConstants.Podcast.EPISODE_RETENTION_COUNT, i);
    }

    /**
     * Returns the number of Podcast episodes to download (-1 to download all).
     */
    public int getPodcastEpisodeDownloadCount() {
        return getInt(SettingsConstants.Podcast.EPISODE_DOWNLOAD_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to download (-1 to download all).
     */
    public void setPodcastEpisodeDownloadCount(int count) {
        setProperty(SettingsConstants.Podcast.EPISODE_DOWNLOAD_COUNT, count);
    }

    public boolean isDlnaEnabled() {
        return getBoolean(SettingsConstants.UPnP.Basic.ENABLED);
    }

    public void setDlnaEnabled(boolean b) {
        setProperty(SettingsConstants.UPnP.Basic.ENABLED, b);
    }

    public String getDlnaServerName() {
        return getString(SettingsConstants.UPnP.Basic.SERVER_NAME);
    }

    public void setDlnaServerName(String s) {
        setProperty(SettingsConstants.UPnP.Basic.SERVER_NAME, s);
    }

    public String getDlnaBaseLANURL() {
        return getString(SettingsConstants.UPnP.Basic.BASE_LAN_URL);
    }

    public void setDlnaBaseLANURL(String s) {
        uPnPSubnet.setDlnaBaseLANURL(s);
        setProperty(SettingsConstants.UPnP.Basic.BASE_LAN_URL, s);
    }

    public boolean isInUPnPRange(String address) {
        return uPnPSubnet.isInUPnPRange(address);
    }

    public boolean isUriWithFileExtensions() {
        return getBoolean(SettingsConstants.UPnP.Basic.URI_WITH_FILE_EXTENSIONS);
    }

    public void setUriWithFileExtensions(boolean b) {
        setProperty(SettingsConstants.UPnP.Basic.URI_WITH_FILE_EXTENSIONS, b);
    }

    public static String getDlnaDefaultFilteredIp() {
        return SettingsConstants.UPnP.Basic.FILTERED_IP.defaultValue;
    }

    public boolean isDlnaEnabledFilteredIp() {
        return getBoolean(SettingsConstants.UPnP.Basic.ENABLED_FILTERED_IP);
    }

    public void setDlnaEnabledFilteredIp(boolean b) {
        setProperty(SettingsConstants.UPnP.Basic.ENABLED_FILTERED_IP, b);
    }

    public String getDlnaFilteredIp() {
        return getString(SettingsConstants.UPnP.Basic.FILTERED_IP);
    }

    public void setDlnaFilteredIp(String s) {
        setProperty(SettingsConstants.UPnP.Basic.FILTERED_IP, s);
    }

    public String getUPnPAlbumGenreSort() {
        return getString(SettingsConstants.UPnP.Options.UPNP_ALBUM_GENRE_SORT);
    }

    public void setUPnPAlbumGenreSort(String s) {
        setProperty(SettingsConstants.UPnP.Options.UPNP_ALBUM_GENRE_SORT, s);
    }

    public String getUPnPSongGenreSort() {
        return getString(SettingsConstants.UPnP.Options.UPNP_SONG_GENRE_SORT);
    }

    public void setUPnPSongGenreSort(String s) {
        setProperty(SettingsConstants.UPnP.Options.UPNP_SONG_GENRE_SORT, s);
    }

    public int getDlnaRandomMax() {
        return getInt(SettingsConstants.UPnP.Options.RANDOM_MAX);
    }

    public void setDlnaRandomMax(int i) {
        if (0 < i) {
            setProperty(SettingsConstants.UPnP.Options.RANDOM_MAX, i);
        }
    }

    public boolean isDlnaGuestPublish() {
        return getBoolean(SettingsConstants.UPnP.Options.GUEST_PUBLISH);
    }

    public void setDlnaGuestPublish(boolean b) {
        setProperty(SettingsConstants.UPnP.Options.GUEST_PUBLISH, b);
    }

    public String getUPnPSearchMethod() {
        return getString(SettingsConstants.UPnP.Search.UPNP_SEARCH_METHOD);
    }

    public void setUPnPSearchMethod(String s) {
        setProperty(SettingsConstants.UPnP.Search.UPNP_SEARCH_METHOD, s);
    }

    public boolean isSonosEnabled() {
        return getBoolean(SettingsConstants.Sonos.ENABLED);
    }

    public void setSonosEnabled(boolean b) {
        setProperty(SettingsConstants.Sonos.ENABLED, b);
    }

    public String getSonosServiceName() {
        return getString(SettingsConstants.Sonos.SERVICE_NAME);
    }

    public void setSonosServiceName(String s) {
        setProperty(SettingsConstants.Sonos.SERVICE_NAME, s);
    }

    public String getPreferredFormatShemeName() {
        return getString(SettingsConstants.Transcoding.PREFERRED_FORMAT_SHEME_NAME);
    }

    public void setPreferredFormatShemeName(String s) {
        setProperty(SettingsConstants.Transcoding.PREFERRED_FORMAT_SHEME_NAME, s);
    }

    public String getPreferredFormat() {
        return getString(SettingsConstants.Transcoding.PREFERRED_FORMAT);
    }

    public void setPreferredFormat(String s) {
        setProperty(SettingsConstants.Transcoding.PREFERRED_FORMAT, s);
    }

    public String getHlsCommand() {
        return getString(SettingsConstants.Transcoding.HLS_COMMAND);
    }

    public void setHlsCommand(String s) {
        setProperty(SettingsConstants.Transcoding.HLS_COMMAND, s);
    }

    public DataSourceConfigType getDatabaseConfigType() {
        return DataSourceConfigType.of(getString(SettingsConstants.Database.TYPE));
    }

    public void setDatabaseConfigType(DataSourceConfigType t) {
        setProperty(SettingsConstants.Database.TYPE, t.name());
    }

    public String getDatabaseConfigEmbedDriver() {
        return getString(SettingsConstants.Database.EMBED_DRIVER);
    }

    public void setDatabaseConfigEmbedDriver(String s) {
        setProperty(SettingsConstants.Database.EMBED_DRIVER, s);
    }

    public String getDatabaseConfigEmbedUrl() {
        return getString(SettingsConstants.Database.EMBED_URL);
    }

    public void setDatabaseConfigEmbedUrl(String s) {
        setProperty(SettingsConstants.Database.EMBED_URL, s);
    }

    public String getDatabaseConfigEmbedUsername() {
        return getString(SettingsConstants.Database.EMBED_USERNAME);
    }

    public void setDatabaseConfigEmbedUsername(String s) {
        setProperty(SettingsConstants.Database.EMBED_USERNAME, s);
    }

    public String getDatabaseConfigEmbedPassword() {
        return getString(SettingsConstants.Database.EMBED_PASSWORD);
    }

    public void setDatabaseConfigEmbedPassword(String s) {
        setProperty(SettingsConstants.Database.EMBED_PASSWORD, s);
    }

    public String getDatabaseConfigJNDIName() {
        return getString(SettingsConstants.Database.JNDI_NAME);
    }

    public void setDatabaseConfigJNDIName(String s) {
        setProperty(SettingsConstants.Database.JNDI_NAME, s);
    }

    public Integer getDatabaseMysqlVarcharMaxlength() {
        return getInt(SettingsConstants.Database.MYSQL_VARCHAR_MAXLENGTH);
    }

    public void setDatabaseMysqlVarcharMaxlength(int maxlength) {
        setProperty(SettingsConstants.Database.MYSQL_VARCHAR_MAXLENGTH, maxlength);
    }

    public String getDatabaseUsertableQuote() {
        return getString(SettingsConstants.Database.USERTABLE_QUOTE);
    }

    public void setDatabaseUsertableQuote(String s) {
        setProperty(SettingsConstants.Database.USERTABLE_QUOTE, s);
    }

    public void resetDatabaseToDefault() {
        setDatabaseConfigEmbedDriver(SettingsConstants.Database.EMBED_DRIVER.defaultValue);
        setDatabaseConfigEmbedPassword(SettingsConstants.Database.EMBED_PASSWORD.defaultValue);
        setDatabaseConfigEmbedUrl(SettingsConstants.Database.EMBED_URL.defaultValue);
        setDatabaseConfigEmbedUsername(SettingsConstants.Database.EMBED_USERNAME.defaultValue);
        setDatabaseConfigJNDIName(SettingsConstants.Database.JNDI_NAME.defaultValue);
        setDatabaseMysqlVarcharMaxlength(SettingsConstants.Database.MYSQL_VARCHAR_MAXLENGTH.defaultValue);
        setDatabaseUsertableQuote(SettingsConstants.Database.USERTABLE_QUOTE.defaultValue);
        setDatabaseConfigType(DataSourceConfigType.of(SettingsConstants.Database.TYPE.defaultValue));
    }
}
