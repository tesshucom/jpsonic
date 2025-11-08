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
 * (C) 2025 Sindre Mehus
 */

package com.tesshu.jpsonic.spring;

/**
 * Enum defining lifecycle phases for application startup and shutdown.
 *
 * <p>
 * Higher numeric values indicate earlier startup and later shutdown. This
 * ensures resources are initialized and destroyed in a safe order according to
 * their dependencies.
 */
public enum LifecyclePhase {

    SCAN(Integer.MAX_VALUE / 2), CACHE(Integer.MAX_VALUE / 2 - 100),
    DATABASE(Integer.MAX_VALUE / 2 - 200);

    public final int value;

    LifecyclePhase(int phase) {
        this.value = phase;
    }

    public int getValue() {
        return this.value;
    }
}
