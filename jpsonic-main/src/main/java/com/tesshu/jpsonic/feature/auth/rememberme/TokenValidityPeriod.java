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

public enum TokenValidityPeriod {

    DAILY(0), TWO_WEEKS(1), MONTHLY(2), HALF_YEAR(3);

    private final int period;

    TokenValidityPeriod(int value) {
        this.period = value;
    }

    /**
     * Returns the covert art period for this scheme.
     *
     * @return the covert art period for this scheme.
     */
    public int value() {
        return period;
    }

    public static TokenValidityPeriod of(int value) {
        return switch (value) {
        case 0 -> DAILY;
        case 1 -> TWO_WEEKS;
        case 2 -> MONTHLY;
        case 3 -> HALF_YEAR;
        default -> TWO_WEEKS;
        };
    }

    public int getSeconds() {
        int days = switch (period) {
        case 0 -> 1;
        case 1 -> 14;
        case 2 -> 30;
        case 3 -> 180;
        default -> 14;
        };
        return days * 24 * 3600;
    }
}
