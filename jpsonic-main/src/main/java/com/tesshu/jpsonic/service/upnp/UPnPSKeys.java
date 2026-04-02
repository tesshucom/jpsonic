package com.tesshu.jpsonic.service.upnp;

import static com.tesshu.jpsonic.service.settings.SettingKey.ValueType.BOOLEAN;
import static com.tesshu.jpsonic.service.settings.SettingKey.ValueType.INTEGER;
import static com.tesshu.jpsonic.service.settings.SettingKey.ValueType.STRING;

import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import com.tesshu.jpsonic.service.settings.SettingKey;

/* UPnP / DLNA settings (UI category). */
@SuppressWarnings({ "PMD.ShortClassName", "PMD.ClassNamingConventions",
        "PMD.FieldNamingConventions", "PMD.MissingStaticMethodInNonInstantiatableClass" })
public final class UPnPSKeys {

    /** Basic UPnP server settings (UI category). */
    public static final class basic {

        public static final SettingKey<Boolean> enabled = SKey.of("DlnaEnabled", BOOLEAN, false);

        public static final SettingKey<String> serverName = SKey
            .of("DlnaServerName", STRING, "Jpsonic");

        public static final SettingKey<String> baseLanUrl = SKey.of("DlnaBaseLANURL", STRING, null);

        public static final SettingKey<Boolean> enabledFilteredIp = SKey
            .of("DlnaEnabledFilteredIp", BOOLEAN, true);

        @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
        public static final SettingKey<String> filteredIp = SKey
            .of("DlnaFilteredIp", STRING, "172.17.16.1");

        public static final SettingKey<Boolean> uriWithFileExtensions = SKey
            .of("UriWithFileExtensions", BOOLEAN, true);

        private basic() {
        }
    }

    /** UPnP browsing and publishing options (UI category). */
    public static final class options {

        // This key will be reorganized in the future.
        public static final SettingKey<String> upnpAlbumGenreSort = SKey
            .of("UPnPAlbumGenreSort", STRING,
                    com.tesshu.jpsonic.service.search.GenreMasterCriteria.Sort.FREQUENCY.name());

        // This key will be reorganized in the future.
        public static final SettingKey<String> upnpSongGenreSort = SKey
            .of("UPnPSongGenreSort", STRING,
                    com.tesshu.jpsonic.service.search.GenreMasterCriteria.Sort.FREQUENCY.name());

        public static final SettingKey<Integer> randomMax = SKey.of("DlnaRandomMax", INTEGER, 50);

        public static final SettingKey<Boolean> guestPublish = SKey
            .of("DlnaGuestPublish", BOOLEAN, true);

        private options() {
        }
    }

    /** UPnP search behavior (UI category). */
    public static final class search {

        public static final SettingKey<String> upnpSearchMethod = SKey
            .of("UPnPSearchMethod", STRING, UPnPSearchMethod.FILE_STRUCTURE.name());

        private search() {
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

    private UPnPSKeys() {
    }
}
