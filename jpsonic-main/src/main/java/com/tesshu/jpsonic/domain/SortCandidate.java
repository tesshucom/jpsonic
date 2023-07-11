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

/**
 * Commonization candidate for correcting sort-tag duplication.
 */
public class SortCandidate {

    /**
     * The value set in the name tag corresponding to be modified sort tag. The element value of artist, album artist,
     * composer, etc.
     */
    private CandidateField field;
    private String name;
    private String reading;
    private int id;

    /**
     * Correction value for sort tag
     */
    private String sort;

    public SortCandidate(int field, String name, String sort, int... id) {
        super();
        this.field = CandidateField.of(field);
        this.name = name;
        this.sort = sort;
        if (id.length == 0) {
            this.id = -1;
        } else {
            this.id = id[0];
        }
    }

    public @NonNull CandidateField getField() {
        return field;
    }

    public void setField(CandidateField field) {
        this.field = field;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public enum CandidateField {

        UNKNOWN(-1), ALBUM_ARTIST(0), ARTIST(1), COMPOSER(2), ALBUM(3);

        private final int value;

        CandidateField(int value) {
            this.value = value;
        }

        static CandidateField of(int v) {
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
