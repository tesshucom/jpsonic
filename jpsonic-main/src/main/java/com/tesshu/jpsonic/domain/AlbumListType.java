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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.domain;

/**
 * @author Sindre Mehus
 */
public enum AlbumListType {

    RANDOM("random", "Random"), NEWEST("newest", "Recently Added"), STARRED("starred", "Starred"),
    HIGHEST("highest", "Top Rated"), FREQUENT("frequent", "Most Played"), RECENT("recent", "Recently Played"),
    DECADE("decade", "By Decade"), GENRE("genre", "By Genre"), ALPHABETICAL("alphabetical", "All"),
    // >>>> JP
    /*
     * #630 Not an album, but added to show index in Home.
     */
    INDEX("index", "Index all");
    // <<<< JP

    private final String id;
    private final String description;

    AlbumListType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static AlbumListType fromId(String id) {
        for (AlbumListType albumListType : values()) {
            if (albumListType.id.equals(id)) {
                return albumListType;
            }
        }
        return null;
    }
}
