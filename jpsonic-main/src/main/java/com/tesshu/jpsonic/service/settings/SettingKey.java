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

package com.tesshu.jpsonic.service.settings;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines the contract for a key used to access a configuration setting.
 * Implementations provide the information required to identify and interpret a
 * setting.
 */
public interface SettingKey<V> {

    enum ValueType {
        INTEGER, LONG, BOOLEAN, STRING
    }

    /**
     * Returns the unique name that identifies this setting.
     */
    @NonNull
    String name();

    /**
     * Returns the type of value associated with this setting.
     */
    @NonNull
    ValueType valueType();

    /**
     * Returns the default value used when the setting is not defined. May return
     * null if no default value exists.
     */
    @Nullable
    V defaultValue();
}
