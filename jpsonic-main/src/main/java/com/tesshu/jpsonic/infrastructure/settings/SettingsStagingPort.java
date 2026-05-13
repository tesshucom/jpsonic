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

/**
 * A functional interface providing write-only access to the staging area of the
 * configuration system.
 * 
 * This interface serves as a "Port" to decouple business logic components (such
 * as {@link StagingApplier}) from the full administrative capabilities of
 * {@link SettingsFacade}. By using this port, components can stage new values
 * or reset them to defaults without needing to know how the settings are
 * persisted or read.
 * 
 * <p>
 * Changes made through this port are kept in a volatile staging state and must
 * be explicitly committed by the caller (typically via the facade) to be
 * persisted to permanent storage.
 * </p>
 */
public interface SettingsStagingPort {

    /**
     * Stages a value for the specified setting key. The value is applied to the
     * temporary staging area and will not be persisted until a formal commit
     * operation is triggered.
     *
     * @param <T>   The type of the setting value.
     * @param key   The unique key identifying the setting.
     * @param value The value to be staged.
     */
    <T> void staging(SettingKey<T> key, T value);

    /**
     * Resets the specified setting keys to their default values in the staging
     * area.
     *
     * @param <T>  The type of the setting values.
     * @param keys One or more keys to be reset to defaults.
     */
    @SuppressWarnings("unchecked")
    <T> void stagingDefault(SettingKey<T>... keys);
}
