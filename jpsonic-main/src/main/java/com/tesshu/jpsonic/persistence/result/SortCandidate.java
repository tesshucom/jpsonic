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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.persistence.result;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A class that represents suggestions for correction of sort tags.
 */
public class SortCandidate implements DuplicateSort {

    private String name;
    private String sort;
    private String reading;
    private int targetId;

    public SortCandidate(String name, String sort, int targetId) {
        super();
        this.name = name;
        this.sort = sort;
        this.targetId = targetId;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public @Nullable String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public @Nullable String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}
