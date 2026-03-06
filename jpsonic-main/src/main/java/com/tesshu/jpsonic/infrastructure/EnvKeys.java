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

import java.nio.file.Path;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines typed keys for external environment inputs such as system properties
 * and environment variables.
 *
 * <p>
 * EnvKeys provides only metadata for external overrides. It does not contain:
 * <ul>
 * <li>OS-specific paths</li>
 * <li>Application directory names</li>
 * <li>Derived environment values</li>
 * <li>Internal defaults unrelated to external inputs</li>
 * </ul>
 *
 * <p>
 * All environment-dependent logic (e.g. filesystem paths, default directories,
 * derived values) must be implemented in {@code EnvironmentProvider}, not in
 * this class.
 */
//PMD: Short names are intentional for repeated use.
//Lower camel case is used to match property-style notation.
@SuppressWarnings({ "PMD.ClassNamingConventions", "PMD.FieldNamingConventions",
        "PMD.ShortClassName" })
class EnvKeys {

    static final class filesystem {

        static final EnvKey<Path> appHome = EnvKey.of("jpsonic.home", null);

    }

    static final class database {

        static final EnvKey<String> localDBUsername = EnvKey.of(null, "sa");
        static final EnvKey<String> localDBPassword = EnvKey.of(null, "");

    }

    static final class network {

        static final EnvKey<Integer> upnpPort = EnvKey.of("UPNP_PORT", -1);

    }

    static final class application {
        static final EnvKey<Boolean> scanOnBoot = EnvKey.of("jpsonic.scan.onboot", false);
        static final EnvKey<Boolean> embeddedFont = EnvKey.of("jpsonic.embeddedfont", false);
        static final EnvKey<Boolean> suppressTomcatCaching = EnvKey
            .of("jpsonic.suppresstomcatcaching", false);
        static final EnvKey<String> rememberMeKey = EnvKey.of("jpsonic.rememberMeKey", null);
        static final EnvKey<String> defaultMusicFolder = EnvKey
            .of("jpsonic.defaultMusicFolder", null);
        static final EnvKey<String> defaultPodcastFolder = EnvKey
            .of("jpsonic.defaultPodcastFolder", null);
        static final EnvKey<String> defaultPlaylistFolder = EnvKey
            .of("jpsonic.defaultPlaylistFolder", null);
    }

    static final class misc {
        static final EnvKey<String> mimeDsf = EnvKey.of("jpsonic.mime.dsf", "audio/x-dsd");
        static final EnvKey<String> mimeDff = EnvKey.of("jpsonic.mime.dff", "audio/x-dsd");
    }

    static final class EnvKey<V> {

        @Nullable
        final String envVarName; // System property or env var name
        final V defaultValue;

        private EnvKey(String name, V defaultValue) {
            super();
            this.envVarName = name;
            this.defaultValue = defaultValue;
        }

        static <V> EnvKey<V> of(String name, V defaultValue) {
            return new EnvKey<>(name, defaultValue);
        }
    }
}
