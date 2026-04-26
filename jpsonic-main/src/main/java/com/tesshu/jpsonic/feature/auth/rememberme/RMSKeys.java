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

package com.tesshu.jpsonic.feature.auth.rememberme;

import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.BOOLEAN;
import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.INTEGER;

import com.tesshu.jpsonic.infrastructure.settings.SettingKey;

@SuppressWarnings({ "PMD.ShortClassName", "PMD.ClassNamingConventions",
        "PMD.FieldNamingConventions", "PMD.MissingStaticMethodInNonInstantiatableClass" })
public class RMSKeys {

    public static final SettingKey<Boolean> enable = SKey.of("RememberMeEnable", BOOLEAN, true);

    public static final SettingKey<Integer> rotationType = SKey
        .of("RememberMeKeyRotationType", INTEGER, KeyRotationType.PERIOD.value());

    public static final SettingKey<Integer> rotationPeriod = SKey
        .of("RememberMeKeyRotationPeriod", INTEGER, KeyRotationPeriod.MONTHLY.value());

    public static final SettingKey<Integer> tokenValidityPeriod = SKey
        .of("RememberMeTokenValidityPeriod", INTEGER, TokenValidityPeriod.TWO_WEEKS.value());

    public static final SettingKey<Boolean> slidingExpirationEnable = SKey
        .of("RememberMeSlidingExpirationEnabled", BOOLEAN, true);

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

    private RMSKeys() {
    }
}
