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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Enum representing the method used for UPnP search.
 *
 * <p>
 * There are two supported search methods:
 * <ul>
 * <li>FILE_STRUCTURE - Search based on the file system structure.</li>
 * <li>ID3 - Search based on ID3 metadata tags embedded in media files.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The static method {@code of(String)} returns the corresponding enum value
 * given a string input, defaulting to FILE_STRUCTURE if no match is found.
 * </p>
 */
public enum UPnPSearchMethod {

    FILE_STRUCTURE, ID3;

    /**
     * Returns the UPnPSearchMethod corresponding to the given string. If the input
     * does not match any known method, FILE_STRUCTURE is returned by default.
     *
     * @param s the string representation of the search method
     * @return the corresponding UPnPSearchMethod enum value
     */
    public static @NonNull UPnPSearchMethod of(String s) {
        if (FILE_STRUCTURE.name().equals(s)) {
            return FILE_STRUCTURE;
        } else if (ID3.name().equals(s)) {
            return ID3;
        }
        return FILE_STRUCTURE;
    }
}
