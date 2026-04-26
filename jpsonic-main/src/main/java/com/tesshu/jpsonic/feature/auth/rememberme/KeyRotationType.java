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

public enum KeyRotationType {

    RESTART(0), PERIOD(1), FIXED(2);

    private final int type;

    KeyRotationType(int value) {
        this.type = value;
    }

    /**
     * Returns the covert art type for this scheme.
     *
     * @return the covert art type for this scheme.
     */
    public int value() {
        return type;
    }

    public static KeyRotationType of(int value) {
        return switch (value) {
        case 0 -> RESTART;
        case 1 -> PERIOD;
        case 2 -> FIXED;
        default -> RESTART;
        };
    }

}
