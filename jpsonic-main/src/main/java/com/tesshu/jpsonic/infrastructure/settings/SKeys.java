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

package com.tesshu.jpsonic.infrastructure.settings;

import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.BOOLEAN;
import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.INTEGER;
import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.LONG;
import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.STRING;

import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.domain.system.PreferredFormatSheme;
import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;

/**
 * Provides a hierarchical namespace for configuration keys used by Jpsonic.
 *
 * <ul>
 * <li>The current hierarchy does not represent the originally intended
 * functional structure; it reflects the structure inherited from the
 * Subsonic/Airsonic settings UI.</li>
 * <li>If the settings UI is redesigned in the future to be completely different
 * from Subsonic/Airsonic, the categories, hierarchy, and class structure may be
 * reorganized.</li>
 * <li>Persistent key names that were used in Subsonic/Airsonic will not be
 * changed in order to preserve those key names as they are.</li>
 * <li>While preserving the key names, the internal namespace, categories, and
 * hierarchy can be reorganized to match the presentation layer or service layer
 * as needed.</li>
 * </ul>
 */
@SuppressWarnings({ "PMD.ShortClassName", "PMD.ClassNamingConventions",
        "PMD.FieldNamingConventions", "PMD.MissingStaticMethodInNonInstantiatableClass" })
public final class SKeys {

    /**
     * Deprecated secrets (temporary keys, scheduled for removal)
     */
    public static final class deprecatedSecrets {

        /**
         * Temporary secret used for JWT signing.
         * <p>
         * This key is deprecated and will be removed once the secret is stored in the
         * database instead of the properties file.
         */
        @Deprecated
        public static final SettingKey<String> jwtKey = SKey.of("JWTKey", STRING, null);

        /**
         * Temporary secret used for Remember-Me token signing.
         * <p>
         * This key is deprecated and will be removed once the secret is stored in the
         * database instead of the properties file.
         */
        @Deprecated
        public static final SettingKey<String> rememberMeKey = SKey
            .of("RememberMeKey", STRING, null);
    }

    /** Settings for music folder configuration (UI category). */
    public static final class musicFolder {

        /** Scan-related settings (UI category). */
        public static final class scan {

            public static final SettingKey<Boolean> ignoreFileTimestamps = SKey
                .of("IgnoreFileTimestamps", BOOLEAN, false);

            public static final SettingKey<Integer> indexCreationInterval = SKey
                .of("IndexCreationInterval", INTEGER, 1);

            public static final SettingKey<Integer> indexCreationHour = SKey
                .of("IndexCreationHour", INTEGER, 3);

            private scan() {
            }
        }

        /** Exclusion rules for music folder scanning (UI category). */
        public static final class exclusion {

            public static final SettingKey<String> excludePatternString = SKey
                .of("ExcludePattern", STRING, null);

            public static final SettingKey<Boolean> ignoreSymlinks = SKey
                .of("IgnoreSymLinks", BOOLEAN, false);

            private exclusion() {
            }
        }

        private musicFolder() {
        }
    }

    /** General settings (UI category). */
    public static final class general {

        /*
         * It's EN and JP(syllabary)
         */
        private static final String DEFAULT_INDEX_STRING = """
                A B C D E F G H I J K L M N O P Q R S T U V W X-Z(XYZ) \
                \u3042(\u30A2) \u3044(\u30A4) \u3046(\u30A6) \u3048(\u30A8) \u304A(\u30AA) \
                \u304B(\u30AB) \u304D(\u30AD) \u304F(\u30AF) \u3051(\u30B1) \u3053(\u30B3) \
                \u3055(\u30B5) \u3057(\u30B7) \u3059(\u30B9) \u305B(\u30BB) \u305D(\u30BD) \
                \u305F(\u30BF) \u3061(\u30C1) \u3064(\u30C4) \u3066(\u30C6) \u3068(\u30C8) \
                \u306A(\u30CA) \u306B(\u30CB) \u306C(\u30CC) \u306D(\u30CD) \u306E(\u30CE) \
                \u306F(\u30CF) \u3072(\u30D2) \u3075(\u30D5) \u3078(\u30D8) \u307B(\u30DB) \
                \u307E(\u30DE) \u307F(\u30DF) \u3080(\u30E0) \u3081(\u30E1) \u3082(\u30E2) \
                \u3084(\u30E4) \u3086(\u30E6) \u3088(\u30E8) \
                \u3089(\u30E9) \u308A(\u30EA) \u308B(\u30EB) \u308C(\u30EC) \u308D(\u30ED) \
                \u308F(\u30EF) \u3092(\u30F2) \u3093(\u30F3)
                """; // JP Index

        private static final String DEFAULT_WELCOME_TITLE = "\u30DB\u30FC\u30E0"; // "Home" in Jp

        /** Index settings (UI category). */
        public static final class index {

            public static final SettingKey<String> indexString = SKey
                .of("IndexString", STRING, DEFAULT_INDEX_STRING);

            public static final SettingKey<String> ignoredArticles = SKey
                .of("IgnoredArticles", STRING, "The El La Las Le Les");

            private index() {
            }
        }

        /** Sorting behavior (UI category). */
        public static final class sort {

            public static final SettingKey<Boolean> albumsByYear = SKey
                .of("SortAlbumsByYear", BOOLEAN, true);

            public static final SettingKey<Boolean> genresByAlphabet = SKey
                .of("SortGenresByAlphabet", BOOLEAN, true);

            public static final SettingKey<Boolean> prohibitSortVarious = SKey
                .of("ProhibitSortVarious", BOOLEAN, true);

            private sort() {
            }
        }

        /** Search behavior (UI category). */
        public static final class search {

            public static final SettingKey<Boolean> searchComposer = SKey
                .of("SearchComposer", BOOLEAN, false);

            public static final SettingKey<Boolean> outputSearchQuery = SKey
                .of("OutputSearchQuery", BOOLEAN, false);

            private search() {
            }
        }

        /** Legacy UI options (UI category). */
        public static final class legacy {

            public static final SettingKey<Boolean> useRadio = SKey.of("UseRadio", BOOLEAN, false);

            public static final SettingKey<Boolean> useJsonp = SKey.of("UseJsonp", BOOLEAN, false);

            public static final SettingKey<Boolean> showIndexDetails = SKey
                .of("ShowIndexDetails", BOOLEAN, false);

            public static final SettingKey<Boolean> showDbDetails = SKey
                .of("ShowDBDetails", BOOLEAN, false);

            public static final SettingKey<Boolean> useCast = SKey.of("UseCast", BOOLEAN, false);

            public static final SettingKey<Boolean> usePartyMode = SKey
                .of("UsePartyMode", BOOLEAN, false);

            private legacy() {
            }
        }

        /** File type and extension settings (UI category). */
        public static final class extension {

            public static final SettingKey<String> musicFileTypes = SKey
                .of("MusicFileTypes", STRING,
                        "mp3 ogg oga aac m4a m4b flac wav wma aif aiff aifc ape mpc shn mka opus dsf dsd");

            public static final SettingKey<String> videoFileTypes = SKey
                .of("VideoFileTypes", STRING,
                        "flv avi mpg mpeg mp4 m4v mkv mov wmv ogv divx m2ts webm");

            public static final SettingKey<String> coverArtFileTypes = SKey
                .of("CoverArtFileTypes2", STRING,
                        "cover.jpg cover.png cover.gif folder.jpg jpg jpeg gif png");

            public static final SettingKey<String> excludedCoverArt = SKey
                .of("ExcludedCoverArt", STRING, "AlbumArtSmall.jpg small.jpg large.jpg");

            public static final SettingKey<String> playlistFolder = SKey
                .of("PlaylistFolder", STRING,
                        EnvironmentProvider.getInstance().getDefaultPlaylistFolder());

            public static final SettingKey<String> shortcuts = SKey
                .of("Shortcuts", STRING, "\"New Incoming\" Podcast");

            private extension() {
            }
        }

        /** Welcome page settings (UI category). */
        public static final class welcome {

            public static final SettingKey<Boolean> gettingStartedEnabled = SKey
                .of("GettingStartedEnabled", BOOLEAN, true);

            public static final SettingKey<String> title = SKey
                .of("WelcomeTitle", STRING, DEFAULT_WELCOME_TITLE);

            public static final SettingKey<String> subtitle = SKey
                .of("WelcomeSubtitle", STRING, null);

            public static final SettingKey<String> message = SKey
                .of("WelcomeMessage2", STRING, null);

            public static final SettingKey<String> loginMessage = SKey
                .of("LoginMessage", STRING, null);

            private welcome() {
            }
        }

        private general() {
        }
    }

    /** Advanced settings (UI category). */
    public static final class advanced {

        /** Bandwidth limits (UI category). */
        public static final class bandwidth {

            public static final SettingKey<Long> downloadBitrateLimit = SKey
                .of("DownloadBitrateLimit", LONG, 0L);

            public static final SettingKey<Long> uploadBitrateLimit = SKey
                .of("UploadBitrateLimit", LONG, 0L);

            public static final SettingKey<Integer> bufferSize = SKey
                .of("SendBufferSize", INTEGER, 32_768);

            private bandwidth() {
            }
        }

        /** SMTP mail settings (UI category). */
        public static final class smtp {

            public static final SettingKey<String> from = SKey
                .of("SmtpFrom", STRING, "jpsonic@tesshu.com");

            public static final SettingKey<String> server = SKey.of("SmtpServer", STRING, null);

            public static final SettingKey<String> port = SKey.of("SmtpPort", STRING, "25");

            public static final SettingKey<String> encryption = SKey
                .of("SmtpEncryption", STRING, "None");

            public static final SettingKey<String> user = SKey.of("SmtpUser", STRING, null);

            public static final SettingKey<String> password = SKey.of("SmtpPassword", STRING, null);

            private smtp() {
            }
        }

        /** LDAP authentication settings (UI category). */
        public static final class ldap {

            public static final SettingKey<Boolean> enabled = SKey
                .of("LdapEnabled", BOOLEAN, false);

            public static final SettingKey<String> url = SKey
                .of("LdapUrl", STRING, "ldap://host.domain.com:389/cn=Users,dc=domain,dc=com");

            public static final SettingKey<String> searchFilter = SKey
                .of("LdapSearchFilter", STRING, "(sAMAccountName={0})");

            public static final SettingKey<String> managerDn = SKey
                .of("LdapManagerDn", STRING, null);

            public static final SettingKey<String> managerPassword = SKey
                .of("LdapManagerPassword", STRING, null);

            public static final SettingKey<Boolean> autoShadowing = SKey
                .of("LdapAutoShadowing", BOOLEAN, false);

            private ldap() {
            }
        }

        /** Captcha / reCAPTCHA settings (UI category). */
        public static final class captcha {

            public static final SettingKey<Boolean> enabled = SKey
                .of("CaptchaEnabled", BOOLEAN, false);

            public static final SettingKey<String> siteKey = SKey
                .of("ReCaptchaSiteKey", STRING, "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI");

            public static final SettingKey<String> secretKey = SKey
                .of("ReCaptchaSecretKey", STRING, "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe");

            private captcha() {
            }
        }

        /** Scan log and diagnostics (UI category). */
        public static final class scanLog {

            public static final SettingKey<Boolean> useScanLog = SKey
                .of("UseScanLog", BOOLEAN, false);

            public static final SettingKey<Integer> scanLogRetention = SKey
                .of("ScanLogRetention", INTEGER, -1);

            public static final SettingKey<Boolean> useScanEvents = SKey
                .of("UseScanEvents", BOOLEAN, false);

            public static final SettingKey<Boolean> measureMemory = SKey
                .of("MeasureMemory", BOOLEAN, false);

            private scanLog() {
            }
        }

        /** Indexing behavior (UI category). */
        public static final class index {

            public static final SettingKey<String> indexSchemeName = SKey
                .of("IndexSchemeName", STRING, IndexScheme.NATIVE_JAPANESE.name());

            public static final SettingKey<Boolean> forceInternalValueInsteadOfTags = SKey
                .of("ForceInternalValueInsteadOfTags", BOOLEAN, false);

            public static final SettingKey<Boolean> ignoreFullWidth = SKey
                .of("IgnoreFullWidth", BOOLEAN, true);

            public static final SettingKey<Boolean> deleteDiacritic = SKey
                .of("DeleteDiacritic", BOOLEAN, true);

            private index() {
            }
        }

        /** Sorting behavior (UI category). */
        public static final class sort {

            public static final SettingKey<Boolean> alphanum = SKey
                .of("SortAlphanum", BOOLEAN, true);

            public static final SettingKey<Boolean> strict = SKey.of("SortStrict", BOOLEAN, true);

            private sort() {
            }
        }

        private advanced() {
        }
    }

    /** Podcast settings (UI category). */
    public static final class podcast {

        public static final SettingKey<String> folder = SKey
            .of("PodcastFolder", STRING,
                    EnvironmentProvider.getInstance().getDefaultPodcastFolder());

        public static final SettingKey<Integer> updateInterval = SKey
            .of("PodcastUpdateInterval", INTEGER, 24);

        public static final SettingKey<Integer> episodeRetentionCount = SKey
            .of("PodcastEpisodeRetentionCount", INTEGER, 10);

        public static final SettingKey<Integer> episodeDownloadCount = SKey
            .of("PodcastEpisodeDownloadCount", INTEGER, 1);

        private podcast() {
        }
    }

    /** Transcoding settings (UI category). */
    public static final class transcoding {

        public static final SettingKey<String> preferredFormatShemeName = SKey
            .of("PreferredFormatShemeName", STRING, PreferredFormatSheme.ANNOYMOUS.name());

        public static final SettingKey<String> preferredFormat = SKey
            .of("PreferredFormatSheme", STRING, "mp3");

        public static final SettingKey<String> hlsCommand = SKey.of("HlsCommand3", STRING, """
                ffmpeg -ss %o -t %d -i %s \
                -async 1 -b:v %bk -s %wx%h \
                -ar 44100 -ac 2 -v 0 \
                -f mpegts -c:v libx264 -preset superfast \
                -c:a libmp3lame -threads 0 -
                """);

        private transcoding() {
        }
    }

    /**
     * Represents a single configuration key.
     * <p>
     * A {@code SettingsKey} defines:
     * <ul>
     * <li>the key name used for persistence</li>
     * <li>the value type {@code V}</li>
     * <li>the default value applied when no user-defined value exists</li>
     * </ul>
     *
     * @param <V> The type of the configuration value.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    static final class SKey<V> implements SettingKey<V> {

        private final String name;
        private final ValueType valueType;
        private final V defaultValue;

        private SKey(String name, ValueType valueType, V defaultValue) {
            super();
            this.name = name;
            this.valueType = valueType;
            this.defaultValue = defaultValue;
        }

        static <V> SKey<V> of(String name, ValueType valueType, V defaultValue) {
            return new SKey<>(name, valueType, defaultValue);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ValueType valueType() {
            return valueType;
        }

        @Override
        public V defaultValue() {
            return defaultValue;
        }
    }

    private SKeys() {
    }
}
