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

package com.tesshu.jpsonic.domain;

import java.time.Instant;

import org.checkerframework.checker.nullness.qual.NonNull;

public class ScanLog {

    private Instant startDate;
    private ScanLogType type;

    public ScanLog(@NonNull Instant startDate, @NonNull ScanLogType type) {
        super();
        this.startDate = startDate;
        this.type = type;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public ScanLogType getType() {
        return type;
    }

    public void setType(ScanLogType type) {
        this.type = type;
    }

    public enum ScanLogType {
        SCAN_ALL, EXPUNGE, PODCAST_REFRESH_ALL, FOLDER_CHANGED
    }
}
