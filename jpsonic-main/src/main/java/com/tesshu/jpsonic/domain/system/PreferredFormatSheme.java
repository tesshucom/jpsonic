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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.domain.system;

/**
 * A method of determining the default value of the preferred format used for
 * transcoding.
 */
public enum PreferredFormatSheme {

    /**
     * For anonymous access, the preferred format defaults are applied uniformly. In
     * other words, access from UPnP and Share is targeted.
     */
    ANNOYMOUS,

    /**
     * For access other than access via API, the default value of the preferred
     * format is applied uniformly. Access from a browser is also included.
     */
    OTHER_THAN_REQUEST,

    /**
     * Exactly the same policy as the legacy server. Preferred format can be
     * specified only by request parameter. In other words, the priority format is
     * applied only to access via API.
     */
    REQUEST_ONLY;

    public static PreferredFormatSheme of(String s) {
        if (REQUEST_ONLY.name().equals(s)) {
            return REQUEST_ONLY;
        } else if (OTHER_THAN_REQUEST.name().equals(s)) {
            return OTHER_THAN_REQUEST;
        }
        return ANNOYMOUS;
    }
}
