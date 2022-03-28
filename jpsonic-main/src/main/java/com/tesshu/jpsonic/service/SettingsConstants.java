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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.FileModifiedCheckScheme;
import com.tesshu.jpsonic.domain.IndexScheme;
import com.tesshu.jpsonic.domain.PreferredFormatSheme;
import com.tesshu.jpsonic.spring.DataSourceConfigType;
import com.tesshu.jpsonic.util.PlayerUtils;

/**
 * Literals and initial values used for property keys in SettingsService. Most items are registered / changed from the
 * web settings page, with a few exceptions.
 */
/*
 * SettingsConstants and SettingsService need to be modified if the key used for the properties file changes.
 */
@SuppressWarnings({ "PMD.ClassNamingConventions", "PMD.ShortClassName" }) // It's inner constants class
final class SettingsConstants {

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

    private static final String DEFAULT_WELCOME_TITLE = "\u30DB\u30FC\u30E0"; // "Home" in Jp

    static final Pair<String> JWT_KEY = Pair.of("JWTKey", null);
    static final Pair<String> REMEMBER_ME_KEY = Pair.of("RememberMeKey", null);
    static final Pair<Long> SETTINGS_CHANGED = Pair.of("SettingsChanged", 0L);

    static class MusicFolder {

        static class Scan {
            static final Pair<Integer> INDEX_CREATION_INTERVAL = Pair.of("IndexCreationInterval", 1);
            static final Pair<Integer> INDEX_CREATION_HOUR = Pair.of("IndexCreationHour", 3);
            static final Pair<Boolean> SHOW_REFRESH = Pair.of("ShowRefresh", false);

            private Scan() {
            }
        }

        static class Exclusion {
            static final Pair<String> EXCLUDE_PATTERN_STRING = Pair.of("ExcludePattern", null);
            static final Pair<Boolean> IGNORE_SYMLINKS = Pair.of("IgnoreSymLinks", false);

            private Exclusion() {
            }
        }

        static class Others {
            static final Pair<Boolean> FAST_CACHE_ENABLED = Pair.of("FastCacheEnabled", true);
            static final Pair<String> FILE_MODIFIED_CHECK_SCHEME_NAME = Pair.of("FileModifiedCheckSchemeName",
                    FileModifiedCheckScheme.LAST_MODIFIED.name());
            static final Pair<Boolean> IGNORE_FILE_TIMESTAMPS = Pair.of("IgnoreFileTimestamps", false);
            static final Pair<Boolean> IGNORE_FILE_TIMESTAMPS_NEXT = Pair.of("IgnoreFileTimestampsNext", false);
            static final Pair<Boolean> IGNORE_FILE_TIMESTAMPS_FOR_EACH_ALBUM = Pair
                    .of("IgnoreFileTimestampsForEachAlbum", false);

            private Others() {
            }
        }

        private MusicFolder() {
        }
    }

    static class General {

        static class ThemeAndLang {
            static final Pair<String> LOCALE_LANGUAGE = Pair.of("LocaleLanguage", "ja");
            static final Pair<String> LOCALE_COUNTRY = Pair.of("LocaleCountry", "jp");
            static final Pair<String> LOCALE_VARIANT = Pair.of("LocaleVariant", "");
            static final Pair<String> THEME_ID = Pair.of("Theme", "jpsonic");

            private ThemeAndLang() {
            }
        }

        static class Index {
            static final Pair<String> INDEX_STRING = Pair.of("IndexString", DEFAULT_INDEX_STRING);
            static final Pair<String> IGNORED_ARTICLES = Pair.of("IgnoredArticles", "The El La Las Le Les");

            private Index() {
            }
        }

        static class Sort {
            static final Pair<Boolean> ALBUMS_BY_YEAR = Pair.of("SortAlbumsByYear", true);
            static final Pair<Boolean> GENRES_BY_ALPHABET = Pair.of("SortGenresByAlphabet", true);
            static final Pair<Boolean> PROHIBIT_SORT_VARIOUS = Pair.of("ProhibitSortVarious", true);
            static final Pair<Boolean> ALPHANUM = Pair.of("SortAlphanum", true);
            static final Pair<Boolean> STRICT = Pair.of("SortStrict", true);

            private Sort() {
            }
        }

        static class Search {
            static final Pair<Boolean> SEARCH_COMPOSER = Pair.of("SearchComposer", false);
            static final Pair<Boolean> OUTPUT_SEARCH_QUERY = Pair.of("OutputSearchQuery", false);

            private Search() {
            }
        }

        static class Legacy {
            static final Pair<Boolean> SHOW_SERVER_LOG = Pair.of("ShowServerLog", false);
            static final Pair<Boolean> SHOW_STATUS = Pair.of("ShowStatus", false);
            static final Pair<Boolean> OTHERS_PLAYING_ENABLED = Pair.of("OthersPlayingEnabled", false);
            static final Pair<Boolean> SHOW_REMEMBER_ME = Pair.of("ShowRememberMe", false);
            static final Pair<Boolean> PUBLISH_PODCAST = Pair.of("PublishPodcast", false);
            static final Pair<Boolean> USE_RADIO = Pair.of("UseRadio", false);
            static final Pair<Boolean> USE_EXTERNAL_PLAYER = Pair.of("UseExternalPlayer", false);

            private Legacy() {
            }
        }

        static class Extension {
            static final Pair<String> MUSIC_FILE_TYPES = Pair.of("MusicFileTypes",
                    "mp3 ogg oga aac m4a m4b flac wav wma aif aiff aifc ape mpc shn mka opus dsf dsd");
            static final Pair<String> VIDEO_FILE_TYPES = Pair.of("VideoFileTypes",
                    "flv avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts webm");
            static final Pair<String> COVER_ART_FILE_TYPES = Pair.of("CoverArtFileTypes2",
                    "cover.jpg cover.png cover.gif folder.jpg jpg jpeg gif png");
            static final Pair<String> PLAYLIST_FOLDER = Pair.of("PlaylistFolder", System.getProperty(
                    "jpsonic.defaultPlaylistFolder", PlayerUtils.isWindows() ? "c:\\playlists" : "/var/playlists"));
            static final Pair<String> SHORTCUTS = Pair.of("Shortcuts", "\"New Incoming\" Podcast");

            private Extension() {
            }
        }

        static class Welcome {
            static final Pair<Boolean> GETTING_STARTED_ENABLED = Pair.of("GettingStartedEnabled", true);
            static final Pair<String> TITLE = Pair.of("WelcomeTitle", DEFAULT_WELCOME_TITLE);
            static final Pair<String> SUBTITLE = Pair.of("WelcomeSubtitle", null);
            static final Pair<String> MESSAGE = Pair.of("WelcomeMessage2", null);
            static final Pair<String> LOGIN_MESSAGE = Pair.of("LoginMessage", null);

            private Welcome() {
            }
        }

        private General() {
        }
    }

    static class Advanced {

        private Advanced() {
        }

        static class VerboseLog {
            static final Pair<Boolean> START = Pair.of("VerboseLogStart", true);
            static final Pair<Boolean> SCANNING = Pair.of("VerboseLogScanning", true);
            static final Pair<Boolean> PLAYING = Pair.of("VerboseLogPlaying", true);
            static final Pair<Boolean> SHUTDOWN = Pair.of("VerboseLogShutdown", true);

            private VerboseLog() {
            }
        }

        static class Bandwidth {

            static final Pair<Long> DOWNLOAD_BITRATE_LIMIT = Pair.of("DownloadBitrateLimit", 0L);
            static final Pair<Long> UPLOAD_BITRATE_LIMIT = Pair.of("UploadBitrateLimit", 0L);
            static final Pair<Integer> BUFFER_SIZE = Pair.of("BufferSize", 4096);

            private Bandwidth() {
            }
        }

        static class Smtp {
            static final Pair<String> FROM = Pair.of("SmtpFrom", "jpsonic@tesshu.com");
            static final Pair<String> SERVER = Pair.of("SmtpServer", null);
            static final Pair<String> PORT = Pair.of("SmtpPort", "25");
            static final Pair<String> ENCRYPTION = Pair.of("SmtpEncryption", "None");
            static final Pair<String> USER = Pair.of("SmtpUser", null);
            static final Pair<String> PASSWORD = Pair.of("SmtpPassword", null);

            private Smtp() {
            }
        }

        static class Ldap {
            static final Pair<Boolean> ENABLED = Pair.of("LdapEnabled", false);
            static final Pair<String> URL = Pair.of("LdapUrl", "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com");
            static final Pair<String> SEARCH_FILTER = Pair.of("LdapSearchFilter", "(sAMAccountName={0})");
            static final Pair<String> MANAGER_DN = Pair.of("LdapManagerDn", null);
            static final Pair<String> MANAGER_PASSWORD = Pair.of("LdapManagerPassword", null);
            static final Pair<Boolean> AUTO_SHADOWING = Pair.of("LdapAutoShadowing", false);

            private Ldap() {
            }
        }

        static class Captcha {
            static final Pair<Boolean> ENABLED = Pair.of("CaptchaEnabled", false);
            static final Pair<String> SITE_KEY = Pair.of("ReCaptchaSiteKey",
                    "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI");
            static final Pair<String> SECRET_KEY = Pair.of("ReCaptchaSecretKey",
                    "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe");

            private Captcha() {
            }
        }

        static class Index {
            static final Pair<String> INDEX_SCHEME_NAME = Pair.of("IndexSchemeName",
                    IndexScheme.NATIVE_JAPANESE.name());
            static final Pair<Boolean> FORCE_INTERNAL_VALUE_INSTEAD_OF_TAGS = Pair.of("ForceInternalValueInsteadOfTags",
                    false);
            static final Pair<Boolean> IGNORE_FULL_WIDTH = Pair.of("IgnoreFullWidth", true);
            static final Pair<Boolean> DELETE_DIACRITIC = Pair.of("DeleteDiacritic", true);

            private Index() {
            }
        }
    }

    static class Podcast {
        static final Pair<String> FOLDER = Pair.of("PodcastFolder", System.getProperty("jpsonic.defaultPodcastFolder",
                PlayerUtils.isWindows() ? "c:\\music\\Podcast" : "/var/music/Podcast"));
        static final Pair<Integer> UPDATE_INTERVAL = Pair.of("PodcastUpdateInterval", 24);
        static final Pair<Integer> EPISODE_RETENTION_COUNT = Pair.of("PodcastEpisodeRetentionCount", 10);
        static final Pair<Integer> EPISODE_DOWNLOAD_COUNT = Pair.of("PodcastEpisodeDownloadCount", 1);

        private Podcast() {
        }
    }

    static class UPnP {

        private UPnP() {
        }

        static class Basic {
            static final Pair<Boolean> ENABLED = Pair.of("DlnaEnabled", false);
            static final Pair<String> SERVER_NAME = Pair.of("DlnaServerName", "Jpsonic");
            static final Pair<String> BASE_LAN_URL = Pair.of("DlnaBaseLANURL", null);
            static final Pair<Boolean> URI_WITH_FILE_EXTENSIONS = Pair.of("UriWithFileExtensions", true);

            private Basic() {
            }
        }

        static class Processor {
            static final Pair<Boolean> INDEX = Pair.of("DlnaIndexVisible", true);
            static final Pair<Boolean> INDEX_ID3 = Pair.of("DlnaIndexId3Visible", false);
            static final Pair<Boolean> FOLDER = Pair.of("DlnaFolderVisible", true);
            static final Pair<Boolean> ARTIST = Pair.of("DlnaArtistVisible", true);
            static final Pair<Boolean> ARTIST_BY_FOLDER = Pair.of("DlnaArtistByFolderVisible", false);
            static final Pair<Boolean> ALBUM = Pair.of("DlnaAlbumVisible", true);
            static final Pair<Boolean> PLAYLIST = Pair.of("DlnaPlaylistVisible", true);
            static final Pair<Boolean> ALBUM_BY_GENRE = Pair.of("DlnaAlbumByGenreVisible", true);
            static final Pair<Boolean> SONG_BY_GENRE = Pair.of("DlnaSongByGenreVisible", true);
            static final Pair<Boolean> RECENT_ALBUM = Pair.of("DlnaRecentAlbumVisible", true);
            static final Pair<Boolean> RECENT_ALBUM_ID3 = Pair.of("DlnaRecentAlbumId3Visible", false);
            static final Pair<Boolean> RANDOM_SONG = Pair.of("DlnaRandomSongVisible", true);
            static final Pair<Boolean> RANDOM_ALBUM = Pair.of("DlnaRandomAlbumVisible", true);
            static final Pair<Boolean> RANDOM_SONG_BY_ARTIST = Pair.of("DlnaRandomSongByArtistVisible", true);
            static final Pair<Boolean> RANDOM_SONG_BY_FOLDER_ARTIST = Pair.of("DlnaRandomSongByFolderArtistVisible",
                    false);
            static final Pair<Boolean> PODCAST = Pair.of("DlnaPodcastVisible", true);

            private Processor() {
            }
        }

        static class Options {
            static final Pair<Boolean> GENRE_COUNT = Pair.of("DlnaGenreCountVisible", false);
            static final Pair<Integer> RANDOM_MAX = Pair.of("DlnaRandomMax", 50);
            static final Pair<Boolean> GUEST_PUBLISH = Pair.of("DlnaGuestPublish", true);

            private Options() {
            }
        }
    }

    static class Sonos {
        static final Pair<Boolean> ENABLED = Pair.of("SonosEnabled", false);
        static final Pair<String> SERVICE_NAME = Pair.of("SonosServiceName", "Jpsonic");

        private Sonos() {
        }
    }

    static class Transcoding {
        static final Pair<String> PREFERRED_FORMAT_SHEME_NAME = Pair.of("PreferredFormatShemeName",
                PreferredFormatSheme.ANNOYMOUS.name());
        static final Pair<String> PREFERRED_FORMAT = Pair.of("PreferredFormatSheme", "mp3");
        static final Pair<String> HLS_COMMAND = Pair.of("HlsCommand3",
                "ffmpeg -ss %o -t %d -i %s -async 1 -b:v %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f mpegts -c:v libx264 -preset superfast -c:a libmp3lame -threads 0 -");

        private Transcoding() {
        }
    }

    static class Database {
        static final Pair<String> TYPE = Pair.of("DatabaseConfigType", DataSourceConfigType.LEGACY.name());
        static final Pair<String> EMBED_DRIVER = Pair.of("DatabaseConfigEmbedDriver", null);
        static final Pair<String> EMBED_URL = Pair.of("DatabaseConfigEmbedUrl", null);
        static final Pair<String> EMBED_USERNAME = Pair.of("DatabaseConfigEmbedUsername", null);
        static final Pair<String> EMBED_PASSWORD = Pair.of("DatabaseConfigEmbedPassword", null);
        static final Pair<String> JNDI_NAME = Pair.of("DatabaseConfigJNDIName", null);
        static final Pair<Integer> MYSQL_VARCHAR_MAXLENGTH = Pair.of("DatabaseMysqlMaxlength", 512);
        static final Pair<String> USERTABLE_QUOTE = Pair.of("DatabaseUsertableQuote", null);

        private Database() {
        }
    }

    static class Pair<V> {
        final String key;
        final V defaultValue;

        private Pair(String key, V defaultValue) {
            super();
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public static <V> Pair<V> of(String key, V defaultValue) {
            return new Pair<>(key, defaultValue);
        }
    }

    private SettingsConstants() {
    }
}
