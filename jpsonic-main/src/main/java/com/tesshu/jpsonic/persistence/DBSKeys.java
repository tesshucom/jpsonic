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

package com.tesshu.jpsonic.persistence;

import static com.tesshu.jpsonic.service.settings.SettingKey.ValueType.INTEGER;
import static com.tesshu.jpsonic.service.settings.SettingKey.ValueType.STRING;

import com.tesshu.jpsonic.infrastructure.db.DataSourceConfigType;
import com.tesshu.jpsonic.service.settings.SettingKey;

@SuppressWarnings({ "PMD.ShortClassName", "PMD.ClassNamingConventions",
        "PMD.FieldNamingConventions", "PMD.MissingStaticMethodInNonInstantiatableClass" })
public class DBSKeys {

    public static final SettingKey<String> databaseConfigType = SKey
        .of("DatabaseConfigType", STRING, DataSourceConfigType.HOST.name());

    public static final SettingKey<String> embedDriver = SKey
        .of("DatabaseConfigEmbedDriver", STRING, null);

    public static final SettingKey<String> embedUrl = SKey
        .of("DatabaseConfigEmbedUrl", STRING, null);

    public static final SettingKey<String> embedUsername = SKey
        .of("DatabaseConfigEmbedUsername", STRING, null);

    public static final SettingKey<String> embedPassword = SKey
        .of("DatabaseConfigEmbedPassword", STRING, null);

    public static final SettingKey<String> jndiName = SKey
        .of("DatabaseConfigJNDIName", STRING, null);

    public static final SettingKey<Integer> mysqlVarcharMaxlength = SKey
        .of("DatabaseMysqlMaxlength", INTEGER, 512);

    public static final SettingKey<String> usertableQuote = SKey
        .of("DatabaseUsertableQuote", STRING, null);

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

    private DBSKeys() {
    }
}
