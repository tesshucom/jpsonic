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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.theme;

import java.awt.Color;

/**
 * Default chart colors used across all chart types.
 */
public enum ChartColor {
    BACKGROUND(new Color(0xFF, 0xFF, 0xFF)), // ffffff
    TEXT(new Color(0x33, 0x33, 0x33)), // 333333
    STROKE(new Color(0x79, 0xA2, 0xD4)); // 79a2d4

    private final Color color;

    ChartColor(Color color) {
        this.color = color;
    }

    public Color get() {
        return color;
    }
}
