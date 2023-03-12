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

package com.tesshu.jpsonic.service;

/**
 * MediaScanner interface.
 */
public interface MediaScannerService extends ScannerStateService {

    /**
     * Returns whether a media library scan is currently in the process of being canceled.
     *
     * @since jpsonic
     */
    boolean isCancel();

    /**
     * Attempt to cancel a scan if it is running.
     *
     * @since jpsonic
     */
    void tryCancel();

    /**
     * Scans the media library. The scanning is done asynchronously, i.e., this method returns immediately.
     *
     * @since airsonic
     */
    void scanLibrary();

    /**
     * Remove unnecessary data from the database.
     *
     * @since airsonic
     */
    void expunge();
}
