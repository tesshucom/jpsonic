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
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME", justification = "Literal value for which OS is assumed.")
@SuppressWarnings("PMD.DefaultPackage")
@Service
/*
 * [DefaultPackage] A remnant of legacy, some methods are implemented in package private. This is intended not to be
 * used by other than Service. Little bad practices. Design improvements can be made by resolving Godclass.
 */
public class SettingsService {

    private enum LocksKeys {
        HOME, MUSIC_FILE, VIDEO_FILE, COVER_ART, THEMES, LOCALES
    }

    private static final Map<LocksKeys, Object> LOCKS;

    static {
        Map<LocksKeys, Object> m = new ConcurrentHashMap<>();
        Arrays.stream(LocksKeys.values()).forEach(k -> m.put(k, new Object()));
        LOCKS = Collections.unmodifiableMap(m);
    }

    private static final Logger LOG = LoggerFactory.getLogger(SettingsService.class);
    private static final int ELEMENT_COUNT_IN_LINE_OF_DEFAULT_THEME = 2;
    private static final int ELEMENT_COUNT_IN_LINE_OF_EXTENDS_THEME = 3;

    // Jpsonic home directory.
    private static final File JPSONIC_HOME_WINDOWS = new File("c:/jpsonic");
    private static final File JPSONIC_HOME_OTHER = new File("/var/jpsonic");

    private static final boolean DEFAULT_SCAN_ON_BOOT = false;

    private static final String KEY_VERBOSE_LOG_START = "VerboseLogStart";
    private static final String KEY_VERBOSE_LOG_SCANNING = "VerboseLogScanning";
    private static final String KEY_VERBOSE_LOG_PLAYING = "VerboseLogPlaying";
    private static final String KEY_VERBOSE_LOG_SHUTDOWN = "VerboseLogShutdown";

    // Global settings.
    private static final String KEY_INDEX_STRING = "IndexString";
    private static final String KEY_IGNORED_ARTICLES = "IgnoredArticles";
    private static final String KEY_SHORTCUTS = "Shortcuts";
    private static final String KEY_PLAYLIST_FOLDER = "PlaylistFolder";
    private static final String KEY_MUSIC_FILE_TYPES = "MusicFileTypes";
    private static final String KEY_VIDEO_FILE_TYPES = "VideoFileTypes";
    private static final String KEY_COVER_ART_FILE_TYPES = "CoverArtFileTypes2";
    private static final String KEY_WELCOME_TITLE = "WelcomeTitle";
    private static final String KEY_WELCOME_SUBTITLE = "WelcomeSubtitle";
    private static final String KEY_WELCOME_MESSAGE = "WelcomeMessage2";
    private static final String KEY_LOGIN_MESSAGE = "LoginMessage";
    private static final String KEY_LOCALE_LANGUAGE = "LocaleLanguage";
    private static final String KEY_LOCALE_COUNTRY = "LocaleCountry";
    private static final String KEY_LOCALE_VARIANT = "LocaleVariant";
    private static final String KEY_THEME_ID = "Theme";
    private static final String KEY_INDEX_CREATION_INTERVAL = "IndexCreationInterval";
    private static final String KEY_INDEX_CREATION_HOUR = "IndexCreationHour";
    private static final String KEY_FAST_CACHE_ENABLED = "FastCacheEnabled";
    private static final String KEY_IGNORE_FILE_TIMESTAMPS = "IgnoreFileTimestamps";
    private static final String KEY_PODCAST_UPDATE_INTERVAL = "PodcastUpdateInterval";
    private static final String KEY_PODCAST_FOLDER = "PodcastFolder";
    private static final String KEY_PODCAST_EPISODE_RETENTION_COUNT = "PodcastEpisodeRetentionCount";
    private static final String KEY_PODCAST_EPISODE_DOWNLOAD_COUNT = "PodcastEpisodeDownloadCount";
    private static final String KEY_DOWNLOAD_BITRATE_LIMIT = "DownloadBitrateLimit";
    private static final String KEY_UPLOAD_BITRATE_LIMIT = "UploadBitrateLimit";
    private static final String KEY_BUFFER_SIZE = "BufferSize";
    private static final String KEY_HLS_COMMAND = "HlsCommand3";
    private static final String KEY_JUKEBOX_COMMAND = "JukeboxCommand2";
    private static final String KEY_LDAP_ENABLED = "LdapEnabled";
    private static final String KEY_LDAP_URL = "LdapUrl";
    private static final String KEY_LDAP_MANAGER_DN = "LdapManagerDn";
    private static final String KEY_LDAP_MANAGER_PASSWORD = "LdapManagerPassword";
    private static final String KEY_LDAP_SEARCH_FILTER = "LdapSearchFilter";
    private static final String KEY_LDAP_AUTO_SHADOWING = "LdapAutoShadowing";
    private static final String KEY_GETTING_STARTED_ENABLED = "GettingStartedEnabled";
    private static final String KEY_SETTINGS_CHANGED = "SettingsChanged";
    private static final String KEY_ORGANIZE_BY_FOLDER_STRUCTURE = "OrganizeByFolderStructure";
    private static final String KEY_INDEX_ENGLISH_PRIOR = "IndexEnglishPrior";
    private static final String KEY_SORT_ALBUMS_BY_YEAR = "SortAlbumsByYear";
    private static final String KEY_SORT_GENRES_BY_ALPHABET = "SortGenresByAlphabet";
    private static final String KEY_PROHIBIT_SORT_VARIOUS = "ProhibitSortVarious";
    private static final String KEY_SORT_ALPHANUM = "SortAlphanum";
    private static final String KEY_SORT_STRICT = "SortStrict";
    private static final String KEY_SEARCH_COMPOSER = "SearchComposer";
    private static final String KEY_OUTPUT_SEARCH_QUERY = "OutputSearchQuery";
    private static final String KEY_SEARCH_METHOD_LEGACY = "SearchMethodLegacy";
    private static final String KEY_SEARCH_METHOD_CHANGED = "SearchMethodChanged";
    private static final String KEY_ANONYMOUS_TRANSCODING = "AnonymousTranscoding";

    private static final String KEY_DLNA_ENABLED = "DlnaEnabled";
    private static final String KEY_DLNA_SERVER_NAME = "DlnaServerName";
    private static final String KEY_DLNA_BASE_LAN_URL = "DlnaBaseLANURL";
    private static final String KEY_DLNA_ALBUM_VISIBLE = "DlnaAlbumVisible";
    private static final String KEY_DLNA_ARTIST_VISIBLE = "DlnaArtistVisible";
    private static final String KEY_DLNA_ARTIST_BY_FOLDER_VISIBLE = "DlnaArtistByFolderVisible";
    private static final String KEY_DLNA_ALBUM_BY_GENRE_VISIBLE = "DlnaAlbumByGenreVisible";
    private static final String KEY_DLNA_SONG_BY_GENRE_VISIBLE = "DlnaSongByGenreVisible";
    private static final String KEY_DLNA_GENRE_COUNT_VISIBLE = "DlnaGenreCountVisible";
    private static final String KEY_DLNA_FOLDER_VISIBLE = "DlnaFolderVisible";
    private static final String KEY_DLNA_PLAYLIST_VISIBLE = "DlnaPlaylistVisible";
    private static final String KEY_DLNA_RECENT_ALBUM_VISIBLE = "DlnaRecentAlbumVisible";
    private static final String KEY_DLNA_RECENT_ALBUM_ID3_VISIBLE = "DlnaRecentAlbumId3Visible";
    private static final String KEY_DLNA_INDEX_VISIBLE = "DlnaIndexVisible";
    private static final String KEY_DLNA_INDEX_ID3_VISIBLE = "DlnaIndexId3Visible";
    private static final String KEY_DLNA_PODCAST_VISIBLE = "DlnaPodcastVisible";
    private static final String KEY_DLNA_RANDOM_ALBUM_VISIBLE = "DlnaRandomAlbumVisible";
    private static final String KEY_DLNA_RANDOM_SONG_VISIBLE = "DlnaRandomSongVisible";
    private static final String KEY_DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE = "DlnaRandomSongByArtistVisible";
    private static final String KEY_DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE = "DlnaRandomSongByFolderArtistVisible";
    private static final String KEY_DLNA_RANDOM_MAX = "DlnaRandomMax";
    private static final String KEY_DLNA_GUEST_PUBLISH = "DlnaGuestPublish";
    private static final String KEY_PUBLISH_PODCAST = "PublishPodcast";
    private static final String KEY_SHOW_JAVAJUKE_BOX = "ShowJavaJukebox";
    private static final String KEY_SHOW_SERVER_LOG = "ShowServerLog";
    private static final String KEY_SHOW_STATUS = "ShowStatus";
    private static final String KEY_OTHERS_PLAYING_ENABLED = "OthersPlayingEnabled";
    private static final String KEY_SHOW_REMEMBER_ME = "ShowRememberMe";
    private static final String KEY_SONOS_ENABLED = "SonosEnabled";
    private static final String KEY_SONOS_SERVICE_NAME = "SonosServiceName";
    private static final String KEY_SONOS_SERVICE_ID = "SonosServiceId";
    private static final String KEY_JWT_KEY = "JWTKey";
    private static final String KEY_REMEMBER_ME_KEY = "RememberMeKey";

    private static final String KEY_SMTP_SERVER = "SmtpServer";
    private static final String KEY_SMTP_ENCRYPTION = "SmtpEncryption";
    private static final String KEY_SMTP_PORT = "SmtpPort";
    private static final String KEY_SMTP_USER = "SmtpUser";
    private static final String KEY_SMTP_PASSWORD = "SmtpPassword";
    private static final String KEY_SMTP_FROM = "SmtpFrom";
    private static final String KEY_EXPORT_PLAYLIST_FORMAT = "PlaylistExportFormat";
    private static final String KEY_IGNORE_SYMLINKS = "IgnoreSymLinks";
    private static final String KEY_EXCLUDE_PATTERN_STRING = "ExcludePattern";

    private static final String KEY_CAPTCHA_ENABLED = "CaptchaEnabled";
    private static final String KEY_RECAPTCHA_SITE_KEY = "ReCaptchaSiteKey";
    private static final String KEY_RECAPTCHA_SECRET_KEY = "ReCaptchaSecretKey";

    // Database Settings
    private static final String KEY_DATABASE_CONFIG_TYPE = "DatabaseConfigType";
    private static final String KEY_DATABASE_CONFIG_EMBED_DRIVER = "DatabaseConfigEmbedDriver";
    private static final String KEY_DATABASE_CONFIG_EMBED_URL = "DatabaseConfigEmbedUrl";
    private static final String KEY_DATABASE_CONFIG_EMBED_USERNAME = "DatabaseConfigEmbedUsername";
    private static final String KEY_DATABASE_CONFIG_EMBED_PASSWORD = "DatabaseConfigEmbedPassword";
    private static final String KEY_DATABASE_CONFIG_JNDI_NAME = "DatabaseConfigJNDIName";
    private static final String KEY_DATABASE_MYSQL_VARCHAR_MAXLENGTH = "DatabaseMysqlMaxlength";
    private static final String KEY_DATABASE_USERTABLE_QUOTE = "DatabaseUsertableQuote";

    private static final String KEY_USE_RADIO = "UseRadio";
    private static final String KEY_USE_SONOS = "UseSonos";

    private static final String KEY_UPNP_PORT = "UPNP_PORT";

    // Default values.
    private static final String DEFAULT_JWT_KEY = null;

    private static final boolean DEFAULT_VERBOSE_LOG_START = true;
    private static final boolean DEFAULT_VERBOSE_LOG_SCANNING = true;
    private static final boolean DEFAULT_VERBOSE_LOG_PLAYING = true;
    private static final boolean DEFAULT_VERBOSE_LOG_SHUTDOWN = true;

    /*
     * It's EN and JP(syllabary)
     */
    private static final String DEFAULT_INDEX_STRING = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) " // En
            + "\u3042(\u30A2) \u3044(\u30A4) \u3046(\u30A6) \u3048(\u30A8) \u304A(\u30AA) " // Jp(a)
            + "\u304B(\u30AB) \u304D(\u30AD) \u304F(\u30AF) \u3051(\u30B1) \u3053(\u30B3) " // Jp(ka)
            + "\u3055(\u30B5) \u3057(\u30B7) \u3059(\u30B9) \u305B(\u30BB) \u305D(\u30BD) " // Jp(sa)
            + "\u305F(\u30BF) \u3061(\u30C1) \u3064(\u30C4) \u3066(\u30C6) \u3068(\u30C8) " // Jp(ta)
            + "\u306A(\u30CA) \u306B(\u30CB) \u306C(\u30CC) \u306D(\u30CD) \u306E(\u30CE) " // Jp(na)
            + "\u306F(\u30CF) \u3072(\u30D2) \u3075(\u30D5) \u3078(\u30D8) \u307B(\u30DB) " // Jp(ha)
            + "\u307E(\u30DE) \u307F(\u30DF) \u3080(\u30E0) \u3081(\u30E1) \u3082(\u30E2) " // Jp(ma)
            + "\u3084(\u30E4) \u3086(\u30E6) \u3088(\u30E8) " // Jp(ya)
            + "\u3089(\u30E9) \u308A(\u30EA) \u308B(\u30EB) \u308C(\u30EC) \u308D(\u30ED) " // Jp(ra)
            + "\u308F(\u30EF) \u3092(\u30F2) \u3093(\u30F3)"; // Jp(wa)
    /*
     * It's EN and JP(consonant)
     */
    private static final String SIMPLE_INDEX_STRING = "A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) " // En
            + "\u3042(\u30A2\u30A4\u30A6\u30A8\u30AA) " // Jp(a)
            + "\u304B(\u30AB\u30AD\u30AF\u30B1\u30B3) " // Jp(ka)
            + "\u3055(\u30B5\u30B7\u30B9\u30BB\u30BD) " // Jp(sa)
            + "\u305F(\u30BF\u30C1\u30C4\u30C6\u30C8) " // Jp(ta)
            + "\u306A(\u30CA\u30CB\u30CC\u30CD\u30CE) " // Jp(na)
            + "\u306F(\u30CF\u30D2\u30D5\u30D8\u30DB) " // Jp(ha)
            + "\u307E(\u30DE\u30DF\u30E0\u30E1\u30E2) " // Jp(ma)
            + "\u3084(\u30E4\u30E6\u30E8) " // Jp(ya)
            + "\u3089(\u30E9\u30EA\u30EB\u30EC\u30ED) " // Jp(ra)
            + "\u308F(\u30EF\u30F2\u30F3)"; // Jp(wa)

    private static final String DEFAULT_IGNORED_ARTICLES = "The El La Las Le Les";
    private static final String DEFAULT_SHORTCUTS = "New Incoming Podcast";
    private static final String DEFAULT_PLAYLIST_FOLDER = PlayerUtils.getDefaultPlaylistFolder();
    private static final String DEFAULT_MUSIC_FILE_TYPES = "mp3 ogg oga aac m4a m4b flac wav wma aif aiff ape mpc shn mka opus";
    private static final String DEFAULT_VIDEO_FILE_TYPES = "flv avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts webm";
    private static final String DEFAULT_COVER_ART_FILE_TYPES = "cover.jpg cover.png cover.gif folder.jpg jpg jpeg gif png";
    private static final String DEFAULT_WELCOME_TITLE = "\u30db\u30fc\u30e0"; // "Home" in Japanese
    private static final String DEFAULT_WELCOME_SUBTITLE = null;
    private static final String DEFAULT_WELCOME_MESSAGE = null;
    private static final String DEFAULT_LOGIN_MESSAGE = null;
    private static final String DEFAULT_LOCALE_LANGUAGE = "ja";
    private static final String DEFAULT_LOCALE_COUNTRY = "jp";
    private static final String DEFAULT_LOCALE_VARIANT = "";
    private static final String DEFAULT_THEME_ID = "jpsonic";
    private static final int DEFAULT_INDEX_CREATION_INTERVAL = 1;
    private static final int DEFAULT_INDEX_CREATION_HOUR = 3;
    private static final boolean DEFAULT_FAST_CACHE_ENABLED = true;
    private static final boolean DEFAULT_IGNORE_FILE_TIMESTAMPS = false;
    private static final int DEFAULT_PODCAST_UPDATE_INTERVAL = 24;
    private static final String DEFAULT_PODCAST_FOLDER = PlayerUtils.getDefaultPodcastFolder();
    private static final int DEFAULT_PODCAST_EPISODE_RETENTION_COUNT = 10;
    private static final int DEFAULT_PODCAST_EPISODE_DOWNLOAD_COUNT = 1;
    private static final long DEFAULT_DOWNLOAD_BITRATE_LIMIT = 0;
    private static final long DEFAULT_UPLOAD_BITRATE_LIMIT = 0;
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final String DEFAULT_HLS_COMMAND = "ffmpeg -ss %o -t %d -i %s -async 1 -b:v %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mpegts -c:v libx264 -preset superfast -c:a libmp3lame -threads 0 -";
    private static final String DEFAULT_JUKEBOX_COMMAND = "ffmpeg -ss %o -i %s -map 0:0 -v 0 -ar 44100 -ac 2 -f s16be -";
    private static final boolean DEFAULT_LDAP_ENABLED = false;
    private static final String DEFAULT_LDAP_URL = "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com";
    private static final String DEFAULT_LDAP_MANAGER_DN = null;
    private static final String DEFAULT_LDAP_MANAGER_PASSWORD = null;
    private static final String DEFAULT_LDAP_SEARCH_FILTER = "(sAMAccountName={0})";
    private static final boolean DEFAULT_LDAP_AUTO_SHADOWING = false;
    private static final boolean DEFAULT_GETTING_STARTED_ENABLED = true;
    private static final long DEFAULT_SETTINGS_CHANGED = 0L;
    private static final boolean DEFAULT_ORGANIZE_BY_FOLDER_STRUCTURE = true;
    private static final boolean DEFAULT_INDEX_ENGLISH_PRIOR = true;
    private static final boolean DEFAULT_SORT_ALBUMS_BY_YEAR = true;
    private static final boolean DEFAULT_SORT_GENRES_BY_ALPHABET = true;
    private static final boolean DEFAULT_PROHIBIT_SORT_VARIOUS = true;
    private static final boolean DEFAULT_SORT_ALPHANUM = true;
    private static final boolean DEFAULT_SORT_STRICT = true;
    private static final boolean DEFAULT_SEARCH_COMPOSER = false;
    private static final boolean DEFAULT_OUTPUT_SEARCH_QUERY = false;
    private static final boolean DEFAULT_SEARCH_METHOD_LEGACY = false;
    private static final boolean DEFAULT_SEARCH_METHOD_CHANGED = false;
    private static final boolean DEFAULT_ANONYMOUS_TRANSCODING = false;

    private static final boolean DEFAULT_DLNA_ENABLED = false;
    private static final String DEFAULT_DLNA_SERVER_NAME = "Jpsonic";
    private static final String DEFAULT_DLNA_BASE_LAN_URL = null;
    private static final boolean DEFAULT_DLNA_ALBUM_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_ARTIST_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_ARTIST_BY_FOLDER_VISIBLE = false;
    private static final boolean DEFAULT_DLNA_ALBUM_BY_GENRE_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_SONG_BY_GENRE_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_GENRE_COUNT_VISIBLE = false;
    private static final boolean DEFAULT_DLNA_FOLDER_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_PLAYLIST_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RECENT_ALBUM_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RECENT_ALBUM_ID3_VISIBLE = false;
    private static final boolean DEFAULT_DLNA_INDEX_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_INDEX_ID3_VISIBLE = false;
    private static final boolean DEFAULT_DLNA_PODCAST_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RANDOM_ALBUM_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RANDOM_SONG_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE = true;
    private static final boolean DEFAULT_DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE = false;
    private static final boolean DEFAULT_DLNA_GUEST_PUBLISH = false;
    private static final int DEFAULT_DLNA_RANDOM_MAX = 50;

    private static final boolean DEFAULT_PUBLISH_PODCAST = false;
    private static final boolean DEFAULT_SHOW_JAVAJUKE_BOX = false;
    private static final boolean DEFAULT_SHOW_SERVER_LOG = false;
    private static final boolean DEFAULT_SHOW_STATUS = false;
    private static final boolean DEFAULT_OTHERS_PLAYING_ENABLED = false;
    private static final boolean DEFAULT_SHOW_REMEMBER_ME = false;
    private static final boolean DEFAULT_SONOS_ENABLED = false;
    private static final String DEFAULT_SONOS_SERVICE_NAME = "Jpsonic";
    private static final int DEFAULT_SONOS_SERVICE_ID = 242;
    private static final String DEFAULT_EXPORT_PLAYLIST_FORMAT = "m3u";
    private static final boolean DEFAULT_IGNORE_SYMLINKS = false;
    private static final String DEFAULT_EXCLUDE_PATTERN_STRING = null;

    private static final String DEFAULT_SMTP_SERVER = null;
    private static final String DEFAULT_SMTP_ENCRYPTION = "None";
    private static final String DEFAULT_SMTP_PORT = "25";
    private static final String DEFAULT_SMTP_USER = null;
    private static final String DEFAULT_SMTP_PASSWORD = null;
    private static final String DEFAULT_SMTP_FROM = "jpsonic@tesshu.com";

    private static final boolean DEFAULT_CAPTCHA_ENABLED = false;
    private static final String DEFAULT_RECAPTCHA_SITE_KEY = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
    private static final String DEFAULT_RECAPTCHA_SECRET_KEY = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";

    private static final DataSourceConfigType DEFAULT_DATABASE_CONFIG_TYPE = DataSourceConfigType.LEGACY;
    private static final String DEFAULT_DATABASE_CONFIG_EMBED_DRIVER = null;
    private static final String DEFAULT_DATABASE_CONFIG_EMBED_URL = null;
    private static final String DEFAULT_DATABASE_CONFIG_EMBED_USERNAME = null;
    private static final String DEFAULT_DATABASE_CONFIG_EMBED_PASSWORD = null;
    private static final String DEFAULT_DATABASE_CONFIG_JNDI_NAME = null;
    private static final Integer DEFAULT_DATABASE_MYSQL_VARCHAR_MAXLENGTH = 512;
    private static final String DEFAULT_DATABASE_USERTABLE_QUOTE = null;

    private static final boolean DEFAULT_USE_RADIO = false;
    private static final boolean DEFAULT_USE_SONOS = false;

    private static final int DEFAULT_UPNP_PORT = -1;

    // Array of obsolete keys. Used to clean property file.
    private static final List<String> OBSOLETE_KEYS = Arrays.asList("PortForwardingPublicPort",
            "PortForwardingLocalPort", "DownsamplingCommand", "DownsamplingCommand2", "DownsamplingCommand3",
            "DownsamplingCommand4", "AutoCoverBatch", "MusicMask", "VideoMask", "CoverArtMask, HlsCommand",
            "HlsCommand2", "JukeboxCommand", "CoverArtFileTypes", "UrlRedirectCustomHost", "CoverArtLimit",
            "StreamPort", "PortForwardingEnabled", "RewriteUrl", "UrlRedirectCustomUrl", "UrlRedirectContextPath",
            "UrlRedirectFrom", "UrlRedirectionEnabled", "UrlRedirectType", "Port", "HttpsPort",
            // Database settings renamed
            "database.varchar.maxlength", "database.config.type", "database.config.embed.driver",
            "database.config.embed.url", "database.config.embed.username", "database.config.embed.password",
            "database.config.jndi.name", "database.usertable.quote");

    private static final String LOCALES_FILE = "/com/tesshu/jpsonic/i18n/locales.txt";
    private static final String THEMES_FILE = "/com/tesshu/jpsonic/theme/themes.txt";

    private static List<Theme> themes;
    private static Locale[] locales;
    private static String[] coverArtFileTypes;
    private static String[] musicFileTypes;
    private static String[] videoFileTypes;

    private final ApacheCommonsConfigurationService configurationService;

    private Pattern excludePattern;
    private Locale locale;

    public SettingsService(ApacheCommonsConfigurationService configurationService) {
        super();
        this.configurationService = configurationService;
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
                boolean isWindows = System.getProperty("os.name", "Windows").toLowerCase().startsWith("windows");
                home = isWindows ? JPSONIC_HOME_WINDOWS : JPSONIC_HOME_OTHER;
            }
            ensureDirectoryPresent(home);
        }
        return home;
    }

    public static boolean isScanOnBoot() {
        return Optional.ofNullable(System.getProperty("jpsonic.scan.onboot")).map(Boolean::parseBoolean)
                .orElse(DEFAULT_SCAN_ON_BOOT);
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
        return Optional.ofNullable(System.getProperty(KEY_UPNP_PORT)).map(Integer::parseInt).orElse(DEFAULT_UPNP_PORT);
    }

    public static File getLogFile() {
        File jpsonicHome = SettingsService.getJpsonicHome();
        return new File(jpsonicHome, getFileSystemAppName() + ".log");
    }

    /**
     * Register in service locator so that non-Spring objects can access me. This method is invoked automatically by
     * Spring.
     */
    @PostConstruct
    public void init() {
        logServerInfo();
    }

    private void logServerInfo() {
        if (isVerboseLogStart() && LOG.isInfoEnabled()) {
            LOG.info("Java: " + System.getProperty("java.version") + ", OS: " + System.getProperty("os.name"));
        }
    }

    public void save() {
        save(true);
    }

    public void save(boolean updateSettingsChanged) {
        if (updateSettingsChanged) {
            removeObsoleteProperties();
            this.setLong(KEY_SETTINGS_CHANGED, System.currentTimeMillis());
        }
        configurationService.save();
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

    static File getPropertyFile() {
        File propertyFile = getJpsonicHome();
        return new File(propertyFile, getFileSystemAppName() + ".properties");
    }

    private int getInt(String key, int defaultValue) {
        return configurationService.getInteger(key, defaultValue);
    }

    private void setInt(String key, Integer value) {
        setProperty(key, value);
    }

    private long getLong(String key, long defaultValue) {
        return configurationService.getLong(key, defaultValue);
    }

    private void setLong(String key, Long value) {
        setProperty(key, value);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return configurationService.getBoolean(key, defaultValue);
    }

    private void setBoolean(String key, Boolean value) {
        setProperty(key, value);
    }

    private String getString(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }

    private void setString(String key, String value) {
        setProperty(key, value);
    }

    public boolean isVerboseLogStart() {
        return getBoolean(KEY_VERBOSE_LOG_START, DEFAULT_VERBOSE_LOG_START);
    }

    public void setVerboseLogStart(boolean b) {
        setBoolean(KEY_VERBOSE_LOG_START, b);
    }

    public boolean isVerboseLogScanning() {
        return getBoolean(KEY_VERBOSE_LOG_SCANNING, DEFAULT_VERBOSE_LOG_SCANNING);
    }

    public void setVerboseLogScanning(boolean b) {
        setBoolean(KEY_VERBOSE_LOG_SCANNING, b);
    }

    public boolean isVerboseLogPlaying() {
        return getBoolean(KEY_VERBOSE_LOG_PLAYING, DEFAULT_VERBOSE_LOG_PLAYING);
    }

    public void setVerboseLogPlaying(boolean b) {
        setBoolean(KEY_VERBOSE_LOG_PLAYING, b);
    }

    public boolean isVerboseLogShutdown() {
        return getBoolean(KEY_VERBOSE_LOG_SHUTDOWN, DEFAULT_VERBOSE_LOG_SHUTDOWN);
    }

    public void setVerboseLogShutdown(boolean b) {
        setBoolean(KEY_VERBOSE_LOG_SHUTDOWN, b);
    }

    public String getDefaultIndexString() {
        return DEFAULT_INDEX_STRING;
    }

    public String getSimpleIndexString() {
        return SIMPLE_INDEX_STRING;
    }

    public String getIndexString() {
        return getProperty(KEY_INDEX_STRING, DEFAULT_INDEX_STRING);
    }

    private String getProperty(String key, String defaultValue) {
        return configurationService.getString(key, defaultValue);
    }

    public void setIndexString(String indexString) {
        setProperty(KEY_INDEX_STRING, indexString);
    }

    public String getIgnoredArticles() {
        return getProperty(KEY_IGNORED_ARTICLES, DEFAULT_IGNORED_ARTICLES);
    }

    public String[] getIgnoredArticlesAsArray() {
        return getIgnoredArticles().split("\\s+");
    }

    public void setIgnoredArticles(String ignoredArticles) {
        setProperty(KEY_IGNORED_ARTICLES, ignoredArticles);
    }

    public String getShortcuts() {
        return getProperty(KEY_SHORTCUTS, DEFAULT_SHORTCUTS);
    }

    public String[] getShortcutsAsArray() {
        return StringUtil.split(getShortcuts());
    }

    public void setShortcuts(String shortcuts) {
        setProperty(KEY_SHORTCUTS, shortcuts);
    }

    public String getPlaylistFolder() {
        return getProperty(KEY_PLAYLIST_FOLDER, DEFAULT_PLAYLIST_FOLDER);
    }

    public void setPlaylistFolder(String playlistFolder) {
        setProperty(KEY_PLAYLIST_FOLDER, playlistFolder);
    }

    public String getMusicFileTypes() {
        synchronized (LOCKS.get(LocksKeys.MUSIC_FILE)) {
            return getProperty(KEY_MUSIC_FILE_TYPES, DEFAULT_MUSIC_FILE_TYPES);
        }
    }

    @SuppressWarnings("PMD.NullAssignment") // (musicFileTypes) Intentional allocation to clear cache
    public void setMusicFileTypes(String fileTypes) {
        synchronized (LOCKS.get(LocksKeys.MUSIC_FILE)) {
            setProperty(KEY_MUSIC_FILE_TYPES, fileTypes);
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
            return getProperty(KEY_VIDEO_FILE_TYPES, DEFAULT_VIDEO_FILE_TYPES);
        }
    }

    @SuppressWarnings("PMD.NullAssignment") // (videoFileTypes) Intentional allocation to clear cache
    public void setVideoFileTypes(String fileTypes) {
        synchronized (LOCKS.get(LocksKeys.VIDEO_FILE)) {
            setProperty(KEY_VIDEO_FILE_TYPES, fileTypes);
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
            return getProperty(KEY_COVER_ART_FILE_TYPES, DEFAULT_COVER_ART_FILE_TYPES);
        }
    }

    @SuppressWarnings("PMD.NullAssignment") // (coverArtFileTypes) Intentional allocation to clear cache
    public void setCoverArtFileTypes(String fileTypes) {
        synchronized (LOCKS.get(LocksKeys.COVER_ART)) {
            setProperty(KEY_COVER_ART_FILE_TYPES, fileTypes);
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

    public String getWelcomeTitle() {
        return StringUtils.trimToNull(getProperty(KEY_WELCOME_TITLE, DEFAULT_WELCOME_TITLE));
    }

    public void setWelcomeTitle(String title) {
        setProperty(KEY_WELCOME_TITLE, title);
    }

    public String getWelcomeSubtitle() {
        return StringUtils.trimToNull(getProperty(KEY_WELCOME_SUBTITLE, DEFAULT_WELCOME_SUBTITLE));
    }

    public void setWelcomeSubtitle(String subtitle) {
        setProperty(KEY_WELCOME_SUBTITLE, subtitle);
    }

    public String getWelcomeMessage() {
        return StringUtils.trimToNull(getProperty(KEY_WELCOME_MESSAGE, DEFAULT_WELCOME_MESSAGE));
    }

    public void setWelcomeMessage(String message) {
        setProperty(KEY_WELCOME_MESSAGE, message);
    }

    public String getLoginMessage() {
        return StringUtils.trimToNull(getProperty(KEY_LOGIN_MESSAGE, DEFAULT_LOGIN_MESSAGE));
    }

    public void setLoginMessage(String message) {
        setProperty(KEY_LOGIN_MESSAGE, message);
    }

    /**
     * Returns the number of days between automatic index creation, of -1 if automatic index creation is disabled.
     */
    public int getIndexCreationInterval() {
        return getInt(KEY_INDEX_CREATION_INTERVAL, DEFAULT_INDEX_CREATION_INTERVAL);
    }

    /**
     * Sets the number of days between automatic index creation, of -1 if automatic index creation is disabled.
     */
    public void setIndexCreationInterval(int days) {
        setInt(KEY_INDEX_CREATION_INTERVAL, days);
    }

    /**
     * Returns the hour of day (0 - 23) when automatic index creation should run.
     */
    public int getIndexCreationHour() {
        return getInt(KEY_INDEX_CREATION_HOUR, DEFAULT_INDEX_CREATION_HOUR);
    }

    /**
     * Sets the hour of day (0 - 23) when automatic index creation should run.
     */
    public void setIndexCreationHour(int hour) {
        setInt(KEY_INDEX_CREATION_HOUR, hour);
    }

    public boolean isFastCacheEnabled() {
        return getBoolean(KEY_FAST_CACHE_ENABLED, DEFAULT_FAST_CACHE_ENABLED);
    }

    public void setFastCacheEnabled(boolean enabled) {
        setBoolean(KEY_FAST_CACHE_ENABLED, enabled);
    }

    public boolean isIgnoreFileTimestamps() {
        return getBoolean(KEY_IGNORE_FILE_TIMESTAMPS, DEFAULT_IGNORE_FILE_TIMESTAMPS);
    }

    public void setIgnoreFileTimestamps(boolean ignore) {
        setBoolean(KEY_IGNORE_FILE_TIMESTAMPS, ignore);
    }

    /**
     * Returns the number of hours between Podcast updates, of -1 if automatic updates are disabled.
     */
    public int getPodcastUpdateInterval() {
        return getInt(KEY_PODCAST_UPDATE_INTERVAL, DEFAULT_PODCAST_UPDATE_INTERVAL);
    }

    /**
     * Sets the number of hours between Podcast updates, of -1 if automatic updates are disabled.
     */
    public void setPodcastUpdateInterval(int hours) {
        setInt(KEY_PODCAST_UPDATE_INTERVAL, hours);
    }

    /**
     * Returns the number of Podcast episodes to keep (-1 to keep all).
     */
    public int getPodcastEpisodeRetentionCount() {
        return getInt(KEY_PODCAST_EPISODE_RETENTION_COUNT, DEFAULT_PODCAST_EPISODE_RETENTION_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to keep (-1 to keep all).
     */
    public void setPodcastEpisodeRetentionCount(int count) {
        setInt(KEY_PODCAST_EPISODE_RETENTION_COUNT, count);
    }

    /**
     * Returns the number of Podcast episodes to download (-1 to download all).
     */
    public int getPodcastEpisodeDownloadCount() {
        return getInt(KEY_PODCAST_EPISODE_DOWNLOAD_COUNT, DEFAULT_PODCAST_EPISODE_DOWNLOAD_COUNT);
    }

    /**
     * Sets the number of Podcast episodes to download (-1 to download all).
     */
    public void setPodcastEpisodeDownloadCount(int count) {
        setInt(KEY_PODCAST_EPISODE_DOWNLOAD_COUNT, count);
    }

    /**
     * Returns the Podcast download folder.
     */
    public String getPodcastFolder() {
        return getProperty(KEY_PODCAST_FOLDER, DEFAULT_PODCAST_FOLDER);
    }

    /**
     * Sets the Podcast download folder.
     */
    public void setPodcastFolder(String folder) {
        setProperty(KEY_PODCAST_FOLDER, folder);
    }

    /**
     * @return The download bitrate limit in Kbit/s. Zero if unlimited.
     */
    public long getDownloadBitrateLimit() {
        return Long.parseLong(getProperty(KEY_DOWNLOAD_BITRATE_LIMIT, Long.toString(DEFAULT_DOWNLOAD_BITRATE_LIMIT)));
    }

    /**
     * @param limit
     *            The download bitrate limit in Kbit/s. Zero if unlimited.
     */
    public void setDownloadBitrateLimit(long limit) {
        setProperty(KEY_DOWNLOAD_BITRATE_LIMIT, String.valueOf(limit));
    }

    /**
     * @return The upload bitrate limit in Kbit/s. Zero if unlimited.
     */
    public long getUploadBitrateLimit() {
        return getLong(KEY_UPLOAD_BITRATE_LIMIT, DEFAULT_UPLOAD_BITRATE_LIMIT);
    }

    /**
     * @param limit
     *            The upload bitrate limit in Kbit/s. Zero if unlimited.
     */
    public void setUploadBitrateLimit(long limit) {
        setLong(KEY_UPLOAD_BITRATE_LIMIT, limit);
    }

    public int getBufferSize() {
        return getInt(KEY_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    public void setBufferSize(int bufferSize) {
        setInt(KEY_BUFFER_SIZE, bufferSize);
    }

    public String getHlsCommand() {
        return getProperty(KEY_HLS_COMMAND, DEFAULT_HLS_COMMAND);
    }

    public void setHlsCommand(String command) {
        setProperty(KEY_HLS_COMMAND, command);
    }

    String getJukeboxCommand() {
        return getProperty(KEY_JUKEBOX_COMMAND, DEFAULT_JUKEBOX_COMMAND);
    }

    public boolean isLdapEnabled() {
        return getBoolean(KEY_LDAP_ENABLED, DEFAULT_LDAP_ENABLED);
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        setBoolean(KEY_LDAP_ENABLED, ldapEnabled);
    }

    public String getLdapUrl() {
        return getProperty(KEY_LDAP_URL, DEFAULT_LDAP_URL);
    }

    public void setLdapUrl(String ldapUrl) {
        setProperty(KEY_LDAP_URL, ldapUrl);
    }

    public String getLdapSearchFilter() {
        return getProperty(KEY_LDAP_SEARCH_FILTER, DEFAULT_LDAP_SEARCH_FILTER);
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        setProperty(KEY_LDAP_SEARCH_FILTER, ldapSearchFilter);
    }

    public String getLdapManagerDn() {
        return getProperty(KEY_LDAP_MANAGER_DN, DEFAULT_LDAP_MANAGER_DN);
    }

    public void setLdapManagerDn(String ldapManagerDn) {
        setProperty(KEY_LDAP_MANAGER_DN, ldapManagerDn);
    }

    public String getLdapManagerPassword() {
        String s = getProperty(KEY_LDAP_MANAGER_PASSWORD, DEFAULT_LDAP_MANAGER_PASSWORD);
        try {
            return StringUtil.utf8HexDecode(s);
        } catch (DecoderException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to decode LDAP manager password.", e);
            }
            return s;
        }
    }

    public void setLdapManagerPassword(final String ldapManagerPassword) {
        setProperty(KEY_LDAP_MANAGER_PASSWORD, StringUtil.utf8HexEncode(ldapManagerPassword));
    }

    public boolean isLdapAutoShadowing() {
        return getBoolean(KEY_LDAP_AUTO_SHADOWING, DEFAULT_LDAP_AUTO_SHADOWING);
    }

    public void setLdapAutoShadowing(boolean ldapAutoShadowing) {
        setBoolean(KEY_LDAP_AUTO_SHADOWING, ldapAutoShadowing);
    }

    public boolean isGettingStartedEnabled() {
        return getBoolean(KEY_GETTING_STARTED_ENABLED, DEFAULT_GETTING_STARTED_ENABLED);
    }

    public void setGettingStartedEnabled(boolean isGettingStartedEnabled) {
        setBoolean(KEY_GETTING_STARTED_ENABLED, isGettingStartedEnabled);
    }

    public long getSettingsChanged() {
        return getLong(KEY_SETTINGS_CHANGED, DEFAULT_SETTINGS_CHANGED);
    }

    public boolean isOrganizeByFolderStructure() {
        return getBoolean(KEY_ORGANIZE_BY_FOLDER_STRUCTURE, DEFAULT_ORGANIZE_BY_FOLDER_STRUCTURE);
    }

    public void setOrganizeByFolderStructure(boolean b) {
        setBoolean(KEY_ORGANIZE_BY_FOLDER_STRUCTURE, b);
    }

    public boolean isIndexEnglishPrior() {
        return getBoolean(KEY_INDEX_ENGLISH_PRIOR, DEFAULT_INDEX_ENGLISH_PRIOR);
    }

    public void setIndexEnglishPrior(boolean b) {
        setBoolean(KEY_INDEX_ENGLISH_PRIOR, b);
    }

    public boolean isSortAlbumsByYear() {
        return getBoolean(KEY_SORT_ALBUMS_BY_YEAR, DEFAULT_SORT_ALBUMS_BY_YEAR);
    }

    public void setSortAlbumsByYear(boolean b) {
        setBoolean(KEY_SORT_ALBUMS_BY_YEAR, b);
    }

    public boolean isSortGenresByAlphabet() {
        return getBoolean(KEY_SORT_GENRES_BY_ALPHABET, DEFAULT_SORT_GENRES_BY_ALPHABET);
    }

    public void setSortGenresByAlphabet(boolean b) {
        setBoolean(KEY_SORT_GENRES_BY_ALPHABET, b);
    }

    public boolean isProhibitSortVarious() {
        return getBoolean(KEY_PROHIBIT_SORT_VARIOUS, DEFAULT_PROHIBIT_SORT_VARIOUS);
    }

    public void setProhibitSortVarious(boolean b) {
        setBoolean(KEY_PROHIBIT_SORT_VARIOUS, b);
    }

    public boolean isSortAlphanum() {
        return getBoolean(KEY_SORT_ALPHANUM, DEFAULT_SORT_ALPHANUM);
    }

    public void setSortAlphanum(boolean b) {
        setBoolean(KEY_SORT_ALPHANUM, b);
    }

    public boolean isSortStrict() {
        return getBoolean(KEY_SORT_STRICT, DEFAULT_SORT_STRICT);
    }

    public void setSortStrict(boolean b) {
        setBoolean(KEY_SORT_STRICT, b);
    }

    public boolean isSearchComposer() {
        return getBoolean(KEY_SEARCH_COMPOSER, DEFAULT_SEARCH_COMPOSER);
    }

    public void setSearchComposer(boolean b) {
        setBoolean(KEY_SEARCH_COMPOSER, b);
    }

    public boolean isOutputSearchQuery() {
        return getBoolean(KEY_OUTPUT_SEARCH_QUERY, DEFAULT_OUTPUT_SEARCH_QUERY);
    }

    public void setOutputSearchQuery(boolean b) {
        setBoolean(KEY_OUTPUT_SEARCH_QUERY, b);
    }

    public boolean isSearchMethodLegacy() {
        return getBoolean(KEY_SEARCH_METHOD_LEGACY, DEFAULT_SEARCH_METHOD_LEGACY);
    }

    public void setSearchMethodLegacy(boolean b) {
        setBoolean(KEY_SEARCH_METHOD_LEGACY, b);
    }

    public boolean isSearchMethodChanged() {
        return getBoolean(KEY_SEARCH_METHOD_CHANGED, DEFAULT_SEARCH_METHOD_CHANGED);
    }

    public void setSearchMethodChanged(boolean b) {
        setBoolean(KEY_SEARCH_METHOD_CHANGED, b);
    }

    public boolean isIgnoreSymLinks() {
        return getBoolean(KEY_IGNORE_SYMLINKS, DEFAULT_IGNORE_SYMLINKS);
    }

    public boolean isAnonymousTranscoding() {
        return getBoolean(KEY_ANONYMOUS_TRANSCODING, DEFAULT_ANONYMOUS_TRANSCODING);
    }

    public void setAnonymousTranscoding(boolean b) {
        setBoolean(KEY_ANONYMOUS_TRANSCODING, b);
    }

    public void setIgnoreSymLinks(boolean b) {
        setBoolean(KEY_IGNORE_SYMLINKS, b);
    }

    public String getExcludePatternString() {
        return getString(KEY_EXCLUDE_PATTERN_STRING, DEFAULT_EXCLUDE_PATTERN_STRING);
    }

    public void setExcludePatternString(String s) {
        setString(KEY_EXCLUDE_PATTERN_STRING, s);
        compileExcludePattern();
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

    public Pattern getExcludePattern() {
        if (excludePattern == null && getExcludePatternString() != null) {
            compileExcludePattern();
        }
        return excludePattern;
    }

    /**
     * Returns whether we are running in Development mode.
     *
     * @return true if we are in Development mode.
     */
    public static boolean isDevelopmentMode() {
        return System.getProperty("airsonic.development") != null;
    }

    /**
     * Returns the custom 'remember me' key used for generating authentication tokens.
     *
     * @return The 'remember me' key.
     */
    public String getRememberMeKey() {
        String key = null;
        if (StringUtils.isBlank(key)) {
            key = getString(KEY_REMEMBER_ME_KEY, null);
        }
        if (StringUtils.isBlank(key)) {
            key = System.getProperty("airsonic.rememberMeKey");
        }
        return key;
    }

    /**
     * Returns the locale (for language, date format etc).
     *
     * @return The locale.
     */
    public Locale getLocale() {
        if (isEmpty(locale)) {
            String language = getProperty(KEY_LOCALE_LANGUAGE, DEFAULT_LOCALE_LANGUAGE);
            String country = getProperty(KEY_LOCALE_COUNTRY, DEFAULT_LOCALE_COUNTRY);
            String variant = getProperty(KEY_LOCALE_VARIANT, DEFAULT_LOCALE_VARIANT);
            locale = new Locale(language, country, variant);
        }
        return locale;
    }

    /**
     * Sets the locale (for language, date format etc.)
     *
     * @param locale
     *            The locale.
     */
    @SuppressWarnings("PMD.NullAssignment") // (locale) Intentional allocation to clear cache
    public void setLocale(Locale locale) {
        this.locale = null;
        setProperty(KEY_LOCALE_LANGUAGE, locale.getLanguage());
        setProperty(KEY_LOCALE_COUNTRY, locale.getCountry());
        setProperty(KEY_LOCALE_VARIANT, locale.getVariant());
    }

    /**
     * Returns the ID of the theme to use.
     *
     * @return The theme ID.
     */
    public String getThemeId() {
        return getProperty(KEY_THEME_ID, DEFAULT_THEME_ID);
    }

    /**
     * Sets the ID of the theme to use.
     *
     * @param themeId
     *            The theme ID
     */
    public void setThemeId(String themeId) {
        setProperty(KEY_THEME_ID, themeId);
    }

    /**
     * Returns a list of available themes.
     *
     * @return A list of available themes.
     */
    @SuppressFBWarnings(value = { "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            "MS_EXPOSE_REP" }, justification = "False positive by try with resources. Returns an immutable list without unnecessary copying.")
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
                        if (elements.length == ELEMENT_COUNT_IN_LINE_OF_DEFAULT_THEME) {
                            l.add(new Theme(elements[0], elements[1]));
                        } else if (elements.length == ELEMENT_COUNT_IN_LINE_OF_EXTENDS_THEME) {
                            l.add(new Theme(elements[0], elements[1], elements[2]));
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

    /**
     * Returns a list of available locales.
     *
     * @return A list of available locales.
     */
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
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

    /**
     * Returns the "brand" name. Normally, this is just "Jpsonic".
     *
     * @return The brand name.
     */
    public String getBrand() {
        return "Jpsonic";
    }

    public boolean isDlnaEnabled() {
        return getBoolean(KEY_DLNA_ENABLED, DEFAULT_DLNA_ENABLED);
    }

    public void setDlnaEnabled(boolean dlnaEnabled) {
        setBoolean(KEY_DLNA_ENABLED, dlnaEnabled);
    }

    public String getDlnaServerName() {
        return getString(KEY_DLNA_SERVER_NAME, DEFAULT_DLNA_SERVER_NAME);
    }

    public void setDlnaServerName(String dlnaServerName) {
        setString(KEY_DLNA_SERVER_NAME, dlnaServerName);
    }

    public String getDlnaBaseLANURL() {
        return getString(KEY_DLNA_BASE_LAN_URL, DEFAULT_DLNA_BASE_LAN_URL);
    }

    public void setDlnaBaseLANURL(String dlnaBaseLANURL) {
        setString(KEY_DLNA_BASE_LAN_URL, dlnaBaseLANURL);
    }

    public boolean isDlnaAlbumVisible() {
        return getBoolean(KEY_DLNA_ALBUM_VISIBLE, DEFAULT_DLNA_ALBUM_VISIBLE);
    }

    public void setDlnaAlbumVisible(boolean b) {
        setBoolean(KEY_DLNA_ALBUM_VISIBLE, b);
    }

    public boolean isDlnaArtistVisible() {
        return getBoolean(KEY_DLNA_ARTIST_VISIBLE, DEFAULT_DLNA_ARTIST_VISIBLE);
    }

    public void setDlnaArtistVisible(boolean b) {
        setBoolean(KEY_DLNA_ARTIST_VISIBLE, b);
    }

    public boolean isDlnaArtistByFolderVisible() {
        return getBoolean(KEY_DLNA_ARTIST_BY_FOLDER_VISIBLE, DEFAULT_DLNA_ARTIST_BY_FOLDER_VISIBLE);
    }

    public void setDlnaArtistByFolderVisible(boolean b) {
        setBoolean(KEY_DLNA_ARTIST_BY_FOLDER_VISIBLE, b);
    }

    public boolean isDlnaAlbumByGenreVisible() {
        return getBoolean(KEY_DLNA_ALBUM_BY_GENRE_VISIBLE, DEFAULT_DLNA_ALBUM_BY_GENRE_VISIBLE);
    }

    public void setDlnaAlbumByGenreVisible(boolean b) {
        setBoolean(KEY_DLNA_ALBUM_BY_GENRE_VISIBLE, b);
    }

    public boolean isDlnaSongByGenreVisible() {
        return getBoolean(KEY_DLNA_SONG_BY_GENRE_VISIBLE, DEFAULT_DLNA_SONG_BY_GENRE_VISIBLE);
    }

    public void setDlnaSongByGenreVisible(boolean b) {
        setBoolean(KEY_DLNA_SONG_BY_GENRE_VISIBLE, b);
    }

    public boolean isDlnaGenreCountVisible() {
        return getBoolean(KEY_DLNA_GENRE_COUNT_VISIBLE, DEFAULT_DLNA_GENRE_COUNT_VISIBLE);
    }

    public void setDlnaGenreCountVisible(boolean b) {
        setBoolean(KEY_DLNA_GENRE_COUNT_VISIBLE, b);
    }

    public boolean isDlnaFolderVisible() {
        return getBoolean(KEY_DLNA_FOLDER_VISIBLE, DEFAULT_DLNA_FOLDER_VISIBLE);
    }

    public void setDlnaFolderVisible(boolean b) {
        setBoolean(KEY_DLNA_FOLDER_VISIBLE, b);
    }

    public boolean isDlnaPlaylistVisible() {
        return getBoolean(KEY_DLNA_PLAYLIST_VISIBLE, DEFAULT_DLNA_PLAYLIST_VISIBLE);
    }

    public void setDlnaPlaylistVisible(boolean b) {
        setBoolean(KEY_DLNA_PLAYLIST_VISIBLE, b);
    }

    public boolean isDlnaRecentAlbumVisible() {
        return getBoolean(KEY_DLNA_RECENT_ALBUM_VISIBLE, DEFAULT_DLNA_RECENT_ALBUM_VISIBLE);
    }

    public void setDlnaRecentAlbumVisible(boolean b) {
        setBoolean(KEY_DLNA_RECENT_ALBUM_VISIBLE, b);
    }

    public boolean isDlnaRecentAlbumId3Visible() {
        return getBoolean(KEY_DLNA_RECENT_ALBUM_ID3_VISIBLE, DEFAULT_DLNA_RECENT_ALBUM_ID3_VISIBLE);
    }

    public void setDlnaRecentAlbumId3Visible(boolean b) {
        setBoolean(KEY_DLNA_RECENT_ALBUM_ID3_VISIBLE, b);
    }

    public boolean isDlnaIndexVisible() {
        return getBoolean(KEY_DLNA_INDEX_VISIBLE, DEFAULT_DLNA_INDEX_VISIBLE);
    }

    public void setDlnaIndexVisible(boolean b) {
        setBoolean(KEY_DLNA_INDEX_VISIBLE, b);
    }

    public boolean isDlnaIndexId3Visible() {
        return getBoolean(KEY_DLNA_INDEX_ID3_VISIBLE, DEFAULT_DLNA_INDEX_ID3_VISIBLE);
    }

    public void setDlnaIndexId3Visible(boolean b) {
        setBoolean(KEY_DLNA_INDEX_ID3_VISIBLE, b);
    }

    public boolean isDlnaPodcastVisible() {
        return getBoolean(KEY_DLNA_PODCAST_VISIBLE, DEFAULT_DLNA_PODCAST_VISIBLE);
    }

    public void setDlnaPodcastVisible(boolean b) {
        setBoolean(KEY_DLNA_PODCAST_VISIBLE, b);
    }

    public boolean isDlnaRandomAlbumVisible() {
        return getBoolean(KEY_DLNA_RANDOM_ALBUM_VISIBLE, DEFAULT_DLNA_RANDOM_ALBUM_VISIBLE);
    }

    public void setDlnaRandomAlbumVisible(boolean b) {
        setBoolean(KEY_DLNA_RANDOM_ALBUM_VISIBLE, b);
    }

    public boolean isDlnaRandomSongVisible() {
        return getBoolean(KEY_DLNA_RANDOM_SONG_VISIBLE, DEFAULT_DLNA_RANDOM_SONG_VISIBLE);
    }

    public void setDlnaRandomSongVisible(boolean b) {
        setBoolean(KEY_DLNA_RANDOM_SONG_VISIBLE, b);
    }

    public boolean isDlnaRandomSongByArtistVisible() {
        return getBoolean(KEY_DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE, DEFAULT_DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE);
    }

    public void setDlnaRandomSongByArtistVisible(boolean b) {
        setBoolean(KEY_DLNA_RANDOM_SONG_BY_ARTIST_VISIBLE, b);
    }

    public boolean isDlnaRandomSongByFolderArtistVisible() {
        return getBoolean(KEY_DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE,
                DEFAULT_DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE);
    }

    public void setDlnaRandomSongByFolderArtistVisible(boolean b) {
        setBoolean(KEY_DLNA_RANDOM_SONG_BY_FOLDER_ARTIST_VISIBLE, b);
    }

    public boolean isDlnaGuestPublish() {
        return getBoolean(KEY_DLNA_GUEST_PUBLISH, DEFAULT_DLNA_GUEST_PUBLISH);
    }

    public void setDlnaGuestPublish(boolean b) {
        setBoolean(KEY_DLNA_GUEST_PUBLISH, b);
    }

    public int getDlnaRandomMax() {
        return getInt(KEY_DLNA_RANDOM_MAX, DEFAULT_DLNA_RANDOM_MAX);
    }

    public void setDlnaRandomMax(int i) {
        if (0 < i) {
            setInt(KEY_DLNA_RANDOM_MAX, i);
        }
    }

    public boolean isPublishPodcast() {
        return getBoolean(KEY_PUBLISH_PODCAST, DEFAULT_PUBLISH_PODCAST);
    }

    public void setPublishPodcast(boolean b) {
        setBoolean(KEY_PUBLISH_PODCAST, b);
    }

    public boolean isShowJavaJukebox() {
        return getBoolean(KEY_SHOW_JAVAJUKE_BOX, DEFAULT_SHOW_JAVAJUKE_BOX);
    }

    public void setShowJavaJukebox(boolean b) {
        setBoolean(KEY_SHOW_JAVAJUKE_BOX, b);
    }

    public boolean isShowServerLog() {
        return getBoolean(KEY_SHOW_SERVER_LOG, DEFAULT_SHOW_SERVER_LOG);
    }

    public void setShowServerLog(boolean b) {
        setBoolean(KEY_SHOW_SERVER_LOG, b);
    }

    public boolean isShowStatus() {
        return getBoolean(KEY_SHOW_STATUS, DEFAULT_SHOW_STATUS);
    }

    public void setShowStatus(boolean b) {
        setBoolean(KEY_SHOW_STATUS, b);
    }

    public boolean isOthersPlayingEnabled() {
        return getBoolean(KEY_OTHERS_PLAYING_ENABLED, DEFAULT_OTHERS_PLAYING_ENABLED);
    }

    public void setOthersPlayingEnabled(boolean b) {
        setBoolean(KEY_OTHERS_PLAYING_ENABLED, b);
    }

    public boolean isShowRememberMe() {
        return getBoolean(KEY_SHOW_REMEMBER_ME, DEFAULT_SHOW_REMEMBER_ME);
    }

    public void setShowRememberMe(boolean b) {
        setBoolean(KEY_SHOW_REMEMBER_ME, b);
    }

    public boolean isSonosEnabled() {
        return getBoolean(KEY_SONOS_ENABLED, DEFAULT_SONOS_ENABLED);
    }

    public void setSonosEnabled(boolean sonosEnabled) {
        setBoolean(KEY_SONOS_ENABLED, sonosEnabled);
    }

    public String getSonosServiceName() {
        return getString(KEY_SONOS_SERVICE_NAME, DEFAULT_SONOS_SERVICE_NAME);
    }

    public void setSonosServiceName(String sonosServiceName) {
        setString(KEY_SONOS_SERVICE_NAME, sonosServiceName);
    }

    int getSonosServiceId() {
        return getInt(KEY_SONOS_SERVICE_ID, DEFAULT_SONOS_SERVICE_ID);
    }

    private void setProperty(String key, Object value) {
        if (value == null) {
            configurationService.clearProperty(key);
        } else {
            configurationService.setProperty(key, value);
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

    public String getSmtpServer() {
        return getProperty(KEY_SMTP_SERVER, DEFAULT_SMTP_SERVER);
    }

    public void setSmtpServer(String smtpServer) {
        setString(KEY_SMTP_SERVER, smtpServer);
    }

    public String getSmtpPort() {
        return getString(KEY_SMTP_PORT, DEFAULT_SMTP_PORT);
    }

    public void setSmtpPort(String smtpPort) {
        setString(KEY_SMTP_PORT, smtpPort);
    }

    public boolean isUseRadio() {
        return getBoolean(KEY_USE_RADIO, DEFAULT_USE_RADIO);
    }

    public void setUseRadio(boolean b) {
        setBoolean(KEY_USE_RADIO, b);
    }

    public boolean isUseSonos() {
        return getBoolean(KEY_USE_SONOS, DEFAULT_USE_SONOS);
    }

    public void setUseSonos(boolean b) {
        setBoolean(KEY_USE_SONOS, b);
    }

    public String getSmtpEncryption() {
        return getProperty(KEY_SMTP_ENCRYPTION, DEFAULT_SMTP_ENCRYPTION);
    }

    public void setSmtpEncryption(String encryptionMethod) {
        setString(KEY_SMTP_ENCRYPTION, encryptionMethod);
    }

    public String getSmtpUser() {
        return getProperty(KEY_SMTP_USER, DEFAULT_SMTP_USER);
    }

    public void setSmtpUser(String smtpUser) {
        setString(KEY_SMTP_USER, smtpUser);
    }

    public String getSmtpPassword() {
        String s = getProperty(KEY_SMTP_PASSWORD, DEFAULT_SMTP_PASSWORD);
        try {
            return StringUtil.utf8HexDecode(s);
        } catch (DecoderException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to decode Smtp password.", e);
            }
            return s;
        }
    }

    public void setSmtpPassword(String smtpPassword) {
        setProperty(KEY_SMTP_PASSWORD, StringUtil.utf8HexEncode(smtpPassword));
    }

    public String getSmtpFrom() {
        return getProperty(KEY_SMTP_FROM, DEFAULT_SMTP_FROM);
    }

    public void setSmtpFrom(String smtpFrom) {
        setString(KEY_SMTP_FROM, smtpFrom);
    }

    public boolean isCaptchaEnabled() {
        return getBoolean(KEY_CAPTCHA_ENABLED, DEFAULT_CAPTCHA_ENABLED);
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        setBoolean(KEY_CAPTCHA_ENABLED, captchaEnabled);
    }

    public String getRecaptchaSiteKey() {
        return getProperty(KEY_RECAPTCHA_SITE_KEY, DEFAULT_RECAPTCHA_SITE_KEY);
    }

    public void setRecaptchaSiteKey(String recaptchaSiteKey) {
        setString(KEY_RECAPTCHA_SITE_KEY, recaptchaSiteKey);
    }

    public String getRecaptchaSecretKey() {
        return getProperty(KEY_RECAPTCHA_SECRET_KEY, DEFAULT_RECAPTCHA_SECRET_KEY);
    }

    public void setRecaptchaSecretKey(String recaptchaSecretKey) {
        setString(KEY_RECAPTCHA_SECRET_KEY, recaptchaSecretKey);
    }

    public DataSourceConfigType getDatabaseConfigType() {
        String raw = getString(KEY_DATABASE_CONFIG_TYPE, DEFAULT_DATABASE_CONFIG_TYPE.name());
        return DataSourceConfigType.valueOf(StringUtils.upperCase(raw));
    }

    public void setDatabaseConfigType(DataSourceConfigType databaseConfigType) {
        setString(KEY_DATABASE_CONFIG_TYPE, databaseConfigType.name());
    }

    public String getDatabaseConfigEmbedDriver() {
        return getString(KEY_DATABASE_CONFIG_EMBED_DRIVER, DEFAULT_DATABASE_CONFIG_EMBED_DRIVER);
    }

    public void setDatabaseConfigEmbedDriver(String embedDriver) {
        setString(KEY_DATABASE_CONFIG_EMBED_DRIVER, embedDriver);
    }

    public String getDatabaseConfigEmbedUrl() {
        return getString(KEY_DATABASE_CONFIG_EMBED_URL, DEFAULT_DATABASE_CONFIG_EMBED_URL);
    }

    public void setDatabaseConfigEmbedUrl(String url) {
        setString(KEY_DATABASE_CONFIG_EMBED_URL, url);
    }

    public String getDatabaseConfigEmbedUsername() {
        return getString(KEY_DATABASE_CONFIG_EMBED_USERNAME, DEFAULT_DATABASE_CONFIG_EMBED_USERNAME);
    }

    public void setDatabaseConfigEmbedUsername(String username) {
        setString(KEY_DATABASE_CONFIG_EMBED_USERNAME, username);
    }

    public String getDatabaseConfigEmbedPassword() {
        return getString(KEY_DATABASE_CONFIG_EMBED_PASSWORD, DEFAULT_DATABASE_CONFIG_EMBED_PASSWORD);
    }

    public void setDatabaseConfigEmbedPassword(String password) {
        setString(KEY_DATABASE_CONFIG_EMBED_PASSWORD, password);
    }

    public String getDatabaseConfigJNDIName() {
        return getString(KEY_DATABASE_CONFIG_JNDI_NAME, DEFAULT_DATABASE_CONFIG_JNDI_NAME);
    }

    public void setDatabaseConfigJNDIName(String jndiName) {
        setString(KEY_DATABASE_CONFIG_JNDI_NAME, jndiName);
    }

    public Integer getDatabaseMysqlVarcharMaxlength() {
        return getInt(KEY_DATABASE_MYSQL_VARCHAR_MAXLENGTH, DEFAULT_DATABASE_MYSQL_VARCHAR_MAXLENGTH);
    }

    public void setDatabaseMysqlVarcharMaxlength(int maxlength) {
        setInt(KEY_DATABASE_MYSQL_VARCHAR_MAXLENGTH, maxlength);
    }

    public String getDatabaseUsertableQuote() {
        return getString(KEY_DATABASE_USERTABLE_QUOTE, DEFAULT_DATABASE_USERTABLE_QUOTE);
    }

    public void setDatabaseUsertableQuote(String usertableQuote) {
        setString(KEY_DATABASE_USERTABLE_QUOTE, usertableQuote);
    }

    public String getJWTKey() {
        return getString(KEY_JWT_KEY, DEFAULT_JWT_KEY);
    }

    public void setJWTKey(String jwtKey) {
        setString(KEY_JWT_KEY, jwtKey);
    }

    public boolean isDefaultSortAlbumsByYear() {
        return DEFAULT_SORT_ALBUMS_BY_YEAR;
    }

    public boolean isDefaultSortGenresByAlphabet() {
        return DEFAULT_SORT_GENRES_BY_ALPHABET;
    }

    public boolean isDefaultProhibitSortVarious() {
        return DEFAULT_PROHIBIT_SORT_VARIOUS;
    }

    public boolean isDefaultSortAlphanum() {
        return DEFAULT_SORT_ALPHANUM;
    }

    public boolean isDefaultSortStrict() {
        return DEFAULT_SORT_STRICT;
    }

    public void resetDatabaseToDefault() {
        setDatabaseConfigEmbedDriver(DEFAULT_DATABASE_CONFIG_EMBED_DRIVER);
        setDatabaseConfigEmbedPassword(DEFAULT_DATABASE_CONFIG_EMBED_PASSWORD);
        setDatabaseConfigEmbedUrl(DEFAULT_DATABASE_CONFIG_EMBED_URL);
        setDatabaseConfigEmbedUsername(DEFAULT_DATABASE_CONFIG_EMBED_USERNAME);
        setDatabaseConfigJNDIName(DEFAULT_DATABASE_CONFIG_JNDI_NAME);
        setDatabaseMysqlVarcharMaxlength(DEFAULT_DATABASE_MYSQL_VARCHAR_MAXLENGTH);
        setDatabaseUsertableQuote(DEFAULT_DATABASE_USERTABLE_QUOTE);
        setDatabaseConfigType(DEFAULT_DATABASE_CONFIG_TYPE);
    }

    String getPlaylistExportFormat() {
        return getProperty(KEY_EXPORT_PLAYLIST_FORMAT, DEFAULT_EXPORT_PLAYLIST_FORMAT);
    }
}
