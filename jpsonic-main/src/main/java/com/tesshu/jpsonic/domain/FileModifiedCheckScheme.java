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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.domain;

/**
 * File change check policy.
 */
public enum FileModifiedCheckScheme {

    /**
     * The method adopted by Subsonic and Airsonic. The record update target is determined based on the last modidied
     * date of the file. This method is primordial logic and cannot be removed. This is because there are definitely use
     * cases where you need to get the actual dates of all files. For example, the music directory may be mirrored by a
     * different application than this server.
     */
    LAST_MODIFIED,

    /**
     * A method of determining the scan target based on the date when each file was scanned. A method for pursuing high
     * speed and low load in more limited situations, not upward compatible with LAST_MODIFIED. This method requires a
     * user interface that allows the user to explicitly specify what to scan. It requires manual management by user,
     * but can reduce access to file entities with OS/hardware/network constraints.
     */
    LAST_SCANNED;

    public String getName() {
        return name();
    }
}
