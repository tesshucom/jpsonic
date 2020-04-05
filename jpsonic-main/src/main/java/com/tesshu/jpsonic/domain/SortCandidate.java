package com.tesshu.jpsonic.domain;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Commonization candidate for correcting sort-tag duplication.
 */
public class SortCandidate {

    /**
     * The value set in the name tag corresponding to be modified sort tag.
     * The element value of artist, album artist, composer, etc.
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
