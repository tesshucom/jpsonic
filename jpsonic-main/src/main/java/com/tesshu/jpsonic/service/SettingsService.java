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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.domain.Theme;
import com.tesshu.jpsonic.service.SettingsConstants.Pair;
import com.tesshu.jpsonic.spring.DataSourceConfigType;
import com.tesshu.jpsonic.util.PlayerUtils;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
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
@SuppressWarnings("PMD.DefaultPackage")
@Service
/*
 * [DefaultPackage] A remnant of legacy, some methods are implemented in package private. This is intended not to be
 * used by other than Service. Little bad practices. Design improvements can be made by resolving Godclass.
 */
public class SettingsService {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsService.class);
    private static final Map<LocksKeys, Object> LOCKS;

    private enum LocksKeys {
        HOME, MUSIC_FILE, VIDEO_FILE, COVER_ART, THEMES, LOCALES
    }

    static {
        Map<LocksKeys, Object> m = new ConcurrentHashMap<>();
        Arrays.stream(LocksKeys.values()).forEach(k -> m.put(k, new Object()));
        LOCKS = Collections.unmodifiableMap(m);
    }

    private static final String LOCALES_FILE = "/com/tesshu/jpsonic/i18n/locales.txt";
    private static final String THEMES_FILE = "/com/tesshu/jpsonic/theme/themes.txt";
    private static final File JPSONIC_HOME_WINDOWS = new File("c:/jpsonic");
    private static final File JPSONIC_HOME_OTHER = new File("/var/jpsonic");
    private static final Pair<Integer> ENV_UPNP_PORT = Pair.of("UPNP_PORT", -1);

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
            "UseSonos", "SearchMethodLegacy", "SearchMethodChanged", "FastCacheEnabled");

    private static final int ELEMENT_COUNT_IN_LINE_OF_THEME = 2;

    private static List<Theme> themes;
    private static Locale[] locales;
    private static String[] coverArtFileTypes;
    private static String[] musicFileTypes;
    private static String[] videoFileTypes;

    private final ApacheCommonsConfigurationService configurationService;
    private final UPnPSubnet uPnPSubnet;

    private Pattern excludePattern;
    private Locale locale;

    public SettingsService(ApacheCommonsConfigurationService configurationService, UPnPSubnet uPnPSubnet) {
        super();
        this.configurationService = configurationService;
        this.uPnPSubnet = uPnPSubnet;
        this.uPnPSubnet.setDlnaBaseLANURL(getDlnaBaseLANURL());
    }

    public static boolean isDevelopmentMode() {
        return System.getProperty("airsonic.development") != null;
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

    private static void ensureDirectoryPresent(File home) {
        // Attempt to create home directory if it doesn't exist.
        if (!home.exists() || !home.isDirectory()) {
            boolean success = home.mkdirs();
            if (!success) {
                String message = "The directory " + home + " does not exist. Please create it and make it writable. "
                        + "(You can override the directory location by specifying -Djpsonic.home=... when "
                        + "starting the servlet container.)";
                throw new IllegalStateException(message);
            }
        }
    }

    @SuppressWarnings({ "PMD.UseLocaleWithCaseConversions", "PMD.ConfusingTernary" })
    /*
     * [UseLocaleWithCaseConversions] The locale doesn't matter, as only comparing the OS names. [ConfusingTernary]
     * false positive
     */
    public static File getJpsonicHome() {
        File home;
        synchronized (LOCKS.get(LocksKeys.HOME)) {
            String overrideHome = System.getProperty("jpsonic.home");
            String oldHome = System.getProperty("libresonic.home");
            if (overrideHome != null) {
                home = new File(overrideHome);
            } else if (oldHome != null) {
                home = new File(oldHome);
            } else {
                home = PlayerUtils.isWindows() ? JPSONIC_HOME_WINDOWS : JPSONIC_HOME_OTHER;
            }
            ensureDirectoryPresent(home);
        }
        return home;
    }

    public static boolean isScanOnBoot() {
        return Optional.ofNullable(System.getProperty("jpsonic.scan.onboot")).map(Boolean::parseBoolean).orElse(false);
    }

    private static String getFileSystemAppName() {
        String home = getJpsonicHome().getPath();
        return home.contains("libresonic") ? "libresonic" : "jpsonic";
    }

    public static String getDefaultJDBCPath() {
        return getJpsonicHome().getPath() + "/db/" + getFileSystemAppName();
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

    public static File getLogFile() {
        File jpsonicHome = SettingsService.getJpsonicHome();
        return new File(jpsonicHome, getFileSystemAppName() + ".log");
    }

    static File getPropertyFile() {
        File propertyFile = getJpsonicHome();
        return new File(propertyFile, getFileSystemAppName() + ".properties");
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

    private String[] toStringArray(String s) {
        List<String> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(s, " ");
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken());
        }

        return result.toArray(new String[0]);
    }

    @PostConstruct
    public void init() {
        if (isVerboseLogStart() && LOG.isInfoEnabled()) {
            LOG.info("Java: " + System.getProperty("java.version") + ", OS: " + System.getProperty("os.name"));
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
            setProperty(SettingsConstants.SETTINGS_CHANGED, System.currentTimeMillis());
        }
        configurationService.save();
    }

    public void save() {
        save(true);
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

    public boolean isShowRefresh() {
        return getBoolean(SettingsConstants.MusicFolder.Scan.SHOW_REFRESH);
    }

    public void setShowRefresh(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Scan.SHOW_REFRESH, b);
    }

    public String getExcludePatternString() {
        return getString(SettingsConstants.MusicFolder.Exclusion.EXCLUDE_PATTERN_STRING);
    }

    @SuppressWarnings({ "PMD.NullAssignment", "PMD.ConfusingTernary" })
    /*
     * [NullAssignment](excludePattern) Intentional allocation to clear cache. [ConfusingTernary] false positive
     */
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

    public String getFileModifiedCheckSchemeName() {
        return getString(SettingsConstants.MusicFolder.Others.FILE_MODIFIED_CHECK_SCHEME_NAME);
    }

    public void setFileModifiedCheckSchemeName(String s) {
        setProperty(SettingsConstants.MusicFolder.Others.FILE_MODIFIED_CHECK_SCHEME_NAME, s);
    }

    public boolean isIgnoreFileTimestamps() {
        return getBoolean(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS);
    }

    public void setIgnoreFileTimestamps(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS, b);
    }

    public boolean isIgnoreFileTimestampsNext() {
        return getBoolean(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS_NEXT);
    }

    public void setIgnoreFileTimestampsNext(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS_NEXT, b);
    }

    public boolean isIgnoreFileTimestampsForEachAlbum() {
        return getBoolean(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS_FOR_EACH_ALBUM);
    }

    public void setIgnoreFileTimestampsForEachAlbum(boolean b) {
        setProperty(SettingsConstants.MusicFolder.Others.IGNORE_FILE_TIMESTAMPS_FOR_EACH_ALBUM, b);
    }

    public Locale[] getAvailableLocales() {
        synchronized (LOCKS.get(LocksKeys.LOCALES)) {
            if (locales == null) {
                List<Locale> l = new ArrayList<>();
                try (InputStream in = SettingsService.class.getResourceAsStream(LOCALES_FILE)) {
                    String[] lines = StringUtil.readLines(in);
                    for (String line : lines) {
                        l.add(StringUtil.parseLocale(line));
                    }
                } catch (IOException x) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to resolve list of locales.", x);
                    }
                    l.add(Locale.ENGLISH);
                }
                locales = l.toArray(new Locale[0]);
            }
        }
        return locales;
    }

    public static String getBrand() {
        return "Jpsonic";
    }

    public Locale getLocale() {
        if (isEmpty(locale)) {
            String language = getString(SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE);
            String country = getString(SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY);
            String variant = getString(SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT);
            locale = new Locale(language, country, variant);
        }
        return locale;
    }

    @SuppressWarnings("PMD.NullAssignment") // (locale) Intentional allocation to clear cache
    public void setLocale(Locale locale) {
        this.locale = null;
        setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE, locale.getLanguage());
        setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY, locale.getCountry());
        setProperty(SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT, locale.getVariant());
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
        synchronized (LOCKS.get(LocksKeys.THEMES)) {
            if (themes == null) {
                List<Theme> l = new ArrayList<>();
                try (InputStream in = SettingsService.class.getResourceAsStream(THEMES_FILE)) {
                    String[] lines = StringUtil.readLines(in);
                    for (String line : lines) {
                        String[] elements = StringUtil.split(line);
                        if (elements.length == ELEMENT_COUNT_IN_LINE_OF_THEME) {
                            l.add(new Theme(elements[0], elements[1]));
                        } else {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Failed to parse theme from line: [" + line + "].");
                            }
                        }
                    }
                } catch (IOException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to resolve list of themes.", e);
                    }
                    l.add(new Theme("default", "Jpsonic default"));
                }
                themes = Collections.unmodifiableList(l);
            }
        }
        return themes;
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
        return getString(SettingsConstants.General.Index.IGNORED_ARTICLES);
    }

    public void setIgnoredArticles(String s) {
        setProperty(SettingsConstants.General.Index.IGNORED_ARTICLES, s);
    }

    public String[] getIgnoredArticlesAsArray() {
        return getIgnoredArticles().split("\\s+");
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

    public boolean isSortAlphanum() {
        return getBoolean(SettingsConstants.General.Sort.ALPHANUM);
    }

    public void setSortAlphanum(boolean b) {
        setProperty(SettingsConstants.General.Sort.ALPHANUM, b);
    }

    public static boolean isDefaultSortAlphanum() {
        return SettingsConstants.General.Sort.ALPHANUM.defaultValue;
    }

    public boolean isSortStrict() {
        return getBoolean(SettingsConstants.General.Sort.STRICT);
    }

    public void setSortStrict(boolean b) {
        setProperty(SettingsConstants.General.Sort.STRICT, b);
    }

    public static boolean isDefaultSortStrict() {
        return SettingsConstants.General.Sort.STRICT.defaultValue;
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

    public boolean isShowServerLog() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_SERVER_LOG);
    }

    public void setShowServerLog(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_SERVER_LOG, b);
    }

    public boolean isShowStatus() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_STATUS);
    }

    public void setShowStatus(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_STATUS, b);
    }

    public boolean isOthersPlayingEnabled() {
        return getBoolean(SettingsConstants.General.Legacy.OTHERS_PLAYING_ENABLED);
    }

    public void setOthersPlayingEnabled(boolean b) {
        setProperty(SettingsConstants.General.Legacy.OTHERS_PLAYING_ENABLED, b);
    }

    public boolean isShowRememberMe() {
        return getBoolean(SettingsConstants.General.Legacy.SHOW_REMEMBER_ME);
    }

    public void setShowRememberMe(boolean b) {
        setProperty(SettingsConstants.General.Legacy.SHOW_REMEMBER_ME, b);
    }

    public boolean isPublishPodcast() {
        return getBoolean(SettingsConstants.General.Legacy.PUBLISH_PODCAST);
    }

    public void setPublishPodcast(boolean b) {
        setProperty(SettingsConstants.General.Legacy.PUBLISH_PODCAST, b);
    }

    public boolean isUseRadio() {
        return getBoolean(SettingsConstants.General.Legacy.USE_RADIO);
    }

    public void setUseRadio(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_RADIO, b);
    }

    public boolean isUseExternalPlayer() {
        return getBoolean(SettingsConstants.General.Legacy.USE_EXTERNAL_PLAYER);
    }

    public void setUseExternalPlayer(boolean b) {
        setProperty(SettingsConstants.General.Legacy.USE_EXTERNAL_PLAYER, b);
    }

    public String getMusicFileTypes() {
        synchronized (LOCKS.get(LocksKeys.MUSIC_FILE)) {
            return getString(SettingsConstants.General.Extension.MUSIC_FILE_TYPES);
        }
    }

    public String getDefaultMusicFileTypes() {
        return SettingsConstants.General.Extension.MUSIC_FILE_TYPES.defaultValue;
    }

    @SuppressWarnings("PMD.NullAssignment") // (musicFileTypes) Intentional allocation to clear cache
    public void setMusicFileTypes(String s) {
        synchronized (LOCKS.get(LocksKeys.MUSIC_FILE)) {
            setProperty(SettingsConstants.General.Extension.MUSIC_FILE_TYPES, s);
            musicFileTypes = null;
        }
    }

    String[] getMusicFileTypesAsArray() {
        synchronized (LOCKS.get(LocksKeys.MUSIC_FILE)) {
            if (musicFileTypes == null) {
                musicFileTypes = toStringArray(getMusicFileTypes());
            }
        }
        return musicFileTypes;
    }

    public String getVideoFileTypes() {
        synchronized (LOCKS.get(LocksKeys.VIDEO_FILE)) {
            return getString(SettingsConstants.General.Extension.VIDEO_FILE_TYPES);
        }
    }

    public String getDefaultVideoFileTypes() {
        return SettingsConstants.General.Extension.VIDEO_FILE_TYPES.defaultValue;
    }

    @SuppressWarnings("PMD.NullAssignment") // (videoFileTypes) Intentional allocation to clear cache
    public void setVideoFileTypes(String s) {
        synchronized (LOCKS.get(LocksKeys.VIDEO_FILE)) {
            setProperty(SettingsConstants.General.Extension.VIDEO_FILE_TYPES, s);
            videoFileTypes = null;
        }
    }

    public String[] getVideoFileTypesAsArray() {
        synchronized (LOCKS.get(LocksKeys.VIDEO_FILE)) {
            if (videoFileTypes == null) {
                videoFileTypes = toStringArray(getVideoFileTypes());
            }
        }
        return videoFileTypes;
    }

    public String getCoverArtFileTypes() {
        synchronized (LOCKS.get(LocksKeys.COVER_ART)) {
            return getString(SettingsConstants.General.Extension.COVER_ART_FILE_TYPES);
        }
    }

    public String getDefaultCoverArtFileTypes() {
        return SettingsConstants.General.Extension.COVER_ART_FILE_TYPES.defaultValue;
    }

    @SuppressWarnings("PMD.NullAssignment") // (coverArtFileTypes) Intentional allocation to clear cache
    public void setCoverArtFileTypes(String s) {
        synchronized (LOCKS.get(LocksKeys.COVER_ART)) {
            setProperty(SettingsConstants.General.Extension.COVER_ART_FILE_TYPES, s);
            coverArtFileTypes = null;
        }
    }

    String[] getCoverArtFileTypesAsArray() {
        synchronized (LOCKS.get(LocksKeys.COVER_ART)) {
            if (coverArtFileTypes == null) {
                coverArtFileTypes = toStringArray(getCoverArtFileTypes());
            }
        }
        return coverArtFileTypes;
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

    public String[] getShortcutsAsArray() {
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

    public boolean isVerboseLogStart() {
        return getBoolean(SettingsConstants.Advanced.VerboseLog.START);
    }

    public void setVerboseLogStart(boolean b) {
        setProperty(SettingsConstants.Advanced.VerboseLog.START, b);
    }

    public boolean isVerboseLogScanning() {
        return getBoolean(SettingsConstants.Advanced.VerboseLog.SCANNING);
    }

    public void setVerboseLogScanning(boolean b) {
        setProperty(SettingsConstants.Advanced.VerboseLog.SCANNING, b);
    }

    public boolean isVerboseLogPlaying() {
        return getBoolean(SettingsConstants.Advanced.VerboseLog.PLAYING);
    }

    public void setVerboseLogPlaying(boolean b) {
        setProperty(SettingsConstants.Advanced.VerboseLog.PLAYING, b);
    }

    public boolean isVerboseLogShutdown() {
        return getBoolean(SettingsConstants.Advanced.VerboseLog.SHUTDOWN);
    }

    public void setVerboseLogShutdown(boolean b) {
        setProperty(SettingsConstants.Advanced.VerboseLog.SHUTDOWN, b);
    }

    /**
     * @return The limit in Kbit/s. Zero if unlimited.
     */
    public long getDownloadBitrateLimit() {
        return getLong(SettingsConstants.Advanced.Bandwidth.DOWNLOAD_BITRATE_LIMIT);
    }

    /**
     * @param l
     *            The limit in Kbit/s. Zero if unlimited.
     */
    public void setDownloadBitrateLimit(long l) {
        setProperty(SettingsConstants.Advanced.Bandwidth.DOWNLOAD_BITRATE_LIMIT, l);
    }

    /**
     * @return The limit in Kbit/s. Zero if unlimited.
     */
    public long getUploadBitrateLimit() {
        return getLong(SettingsConstants.Advanced.Bandwidth.UPLOAD_BITRATE_LIMIT);
    }

    /**
     * @param l
     *            The limit in Kbit/s. Zero if unlimited.
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

    public final String getDlnaBaseLANURL() {
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

    public boolean isDlnaIndexVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.INDEX);
    }

    public void setDlnaIndexVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.INDEX, b);
    }

    public boolean isDlnaIndexId3Visible() {
        return getBoolean(SettingsConstants.UPnP.Processor.INDEX_ID3);
    }

    public void setDlnaIndexId3Visible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.INDEX_ID3, b);
    }

    public boolean isDlnaFolderVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.FOLDER);
    }

    public void setDlnaFolderVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.FOLDER, b);
    }

    public boolean isDlnaArtistVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.ARTIST);
    }

    public void setDlnaArtistVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.ARTIST, b);
    }

    public boolean isDlnaArtistByFolderVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.ARTIST_BY_FOLDER);
    }

    public void setDlnaArtistByFolderVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.ARTIST_BY_FOLDER, b);
    }

    public boolean isDlnaAlbumVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.ALBUM);
    }

    public void setDlnaAlbumVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.ALBUM, b);
    }

    public boolean isDlnaPlaylistVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.PLAYLIST);
    }

    public void setDlnaPlaylistVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.PLAYLIST, b);
    }

    public boolean isDlnaAlbumByGenreVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.ALBUM_BY_GENRE);
    }

    public void setDlnaAlbumByGenreVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.ALBUM_BY_GENRE, b);
    }

    public boolean isDlnaSongByGenreVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.SONG_BY_GENRE);
    }

    public void setDlnaSongByGenreVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.SONG_BY_GENRE, b);
    }

    public boolean isDlnaRecentAlbumVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RECENT_ALBUM);
    }

    public void setDlnaRecentAlbumVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RECENT_ALBUM, b);
    }

    public boolean isDlnaRecentAlbumId3Visible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RECENT_ALBUM_ID3);
    }

    public void setDlnaRecentAlbumId3Visible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RECENT_ALBUM_ID3, b);
    }

    public boolean isDlnaRandomSongVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RANDOM_SONG);
    }

    public void setDlnaRandomSongVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RANDOM_SONG, b);
    }

    public boolean isDlnaRandomAlbumVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RANDOM_ALBUM);
    }

    public void setDlnaRandomAlbumVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RANDOM_ALBUM, b);
    }

    public boolean isDlnaRandomSongByArtistVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RANDOM_SONG_BY_ARTIST);
    }

    public void setDlnaRandomSongByArtistVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RANDOM_SONG_BY_ARTIST, b);
    }

    public boolean isDlnaRandomSongByFolderArtistVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.RANDOM_SONG_BY_FOLDER_ARTIST);
    }

    public void setDlnaRandomSongByFolderArtistVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.RANDOM_SONG_BY_FOLDER_ARTIST, b);
    }

    public boolean isDlnaPodcastVisible() {
        return getBoolean(SettingsConstants.UPnP.Processor.PODCAST);
    }

    public void setDlnaPodcastVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Processor.PODCAST, b);
    }

    public boolean isDlnaGenreCountVisible() {
        return getBoolean(SettingsConstants.UPnP.Options.GENRE_COUNT);
    }

    public void setDlnaGenreCountVisible(boolean b) {
        setProperty(SettingsConstants.UPnP.Options.GENRE_COUNT, b);
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
        String raw = getString(SettingsConstants.Database.TYPE);
        return DataSourceConfigType.valueOf(StringUtils.upperCase(raw));
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
        setDatabaseConfigType(DataSourceConfigType.valueOf(SettingsConstants.Database.TYPE.defaultValue));
    }
}
