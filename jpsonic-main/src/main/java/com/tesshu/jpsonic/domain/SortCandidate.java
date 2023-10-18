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

package com.tesshu.jpsonic.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A class that represents correction suggestions for sort tags.
 */
public final class SortCandidate implements ArtistIndexable, DuplicateSort {

    private String name;
    private String sort;
    private String reading;
    private int targetId;
    private TargetField targetField;

    public SortCandidate(int targetField, String name, String sort, int targetId) {
        super();
        this.targetField = TargetField.of(targetField);
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

    @Override
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

    public @NonNull TargetField getTargetField() {
        return targetField;
    }

    public void setTargetField(TargetField targetField) {
        this.targetField = targetField;
    }

    public enum TargetField {

        UNKNOWN(-1), ALBUM_ARTIST(0), ARTIST(1), COMPOSER(2), ALBUM(3);

        private final int value;

        TargetField(int value) {
            this.value = value;
        }

        static TargetField of(int v) {
            if (ALBUM_ARTIST.getValue() == v) {
                return ALBUM_ARTIST;
            } else if (ARTIST.getValue() == v) {
                return ARTIST;
            } else if (COMPOSER.getValue() == v) {
                return COMPOSER;
            }
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }
    }
}
