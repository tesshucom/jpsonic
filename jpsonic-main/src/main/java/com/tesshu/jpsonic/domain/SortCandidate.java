package com.tesshu.jpsonic.domain;

/**
 * Commonization candidate for correcting sort-tag duplication.
 */
public class SortCandidate {

    /**
     * The value set in the name tag corresponding to be modified sort tag.
     * The element value of artist, album artist, composer, etc.
     */
    private String name;

    /**
     * Correction value for sort tag
     */
    private String sort;

    public SortCandidate(String name, String sort) {
        super();
        this.name = name;
        this.sort = sort;
    }

    public String getName() {
        return name;
    }

    public String getSort() {
        return sort;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

}
