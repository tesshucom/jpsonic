/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */
package com.tesshu.jpsonic.domain;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Commonization candidate for correcting sort-tag duplication.
 */
public class SortCandidate {

    /**
     * The value set in the name tag corresponding to be modified sort tag. The element value of artist, album artist,
     * composer, etc.
     */
    private String name;

    private String reading;

    /**
     * Correction value for sort tag
     */
    private String sort;

    public SortCandidate(String name, String sort) {
        super();
        this.name = name;
        this.sort = sort;
    }

    public @NonNull String getName() {
        return name;
    }

    public String getReading() {
        return reading;
    }

    public @NonNull String getSort() {
        return sort;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

}
