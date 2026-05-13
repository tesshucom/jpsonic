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

import static com.tesshu.jpsonic.infrastructure.settings.SettingKey.ValueType.LONG;

@SuppressWarnings({ "PMD.ShortClassName", "PMD.ClassNamingConventions",
        "PMD.FieldNamingConventions", "PMD.MissingStaticMethodInNonInstantiatableClass" })
class SystemSKeys {

    static final SKey<Long> savedAt = SKey.of("SettingsChanged", LONG, 0L);

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

    private SystemSKeys() {
    }
}
