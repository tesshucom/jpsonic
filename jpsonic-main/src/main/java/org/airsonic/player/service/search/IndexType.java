/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */

package org.airsonic.player.service.search;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum that symbolizes the each lucene index entity.
 * This class is a division of what was once part of SearchService and added functionality.
 * @since legacy
 */
public enum IndexType {

    SONG(
        fieldNames(
            FieldNames.TITLE,
            FieldNames.TITLE_EX,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.ARTIST_EX),
        boosts(
            entry(FieldNames.TITLE, 2.3F),
            entry(FieldNames.TITLE_EX, 2.3F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ALBUM(
        fieldNames(
            FieldNames.ALBUM,
            FieldNames.ALBUM_EX,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.ARTIST_EX,
            FieldNames.FOLDER ),
        boosts(
            entry(FieldNames.ALBUM, 2.3F),
            entry(FieldNames.ALBUM_EX, 2.3F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ALBUM_ID3(
        fieldNames(
            FieldNames.ALBUM,
            FieldNames.ALBUM_EX,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.ARTIST_EX,
            FieldNames.FOLDER_ID ),
        boosts(
            entry(FieldNames.ALBUM, 2.3F),
            entry(FieldNames.ALBUM_EX, 2.3F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ARTIST(
        fieldNames(
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.ARTIST_EX,
            FieldNames.FOLDER),
        boosts(
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ARTIST_ID3(
        fieldNames(
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.ARTIST_EX),
        boosts(
            entry(FieldNames.ARTIST_READING, 1.1F))),

    GENRE(
        fieldNames(
                FieldNames.GENRE_KEY,
                FieldNames.GENRE),
        boosts(
            entry(FieldNames.GENRE_KEY, 1.1F))),

    ;

    @SafeVarargs
    private static final Map<String, Float> boosts(SimpleEntry<String, Float>... entry) {
        Map<String, Float> m = new HashMap<>();
        Arrays.stream(entry).forEach(kv -> m.put(kv.getKey(), kv.getValue()));
        return Collections.unmodifiableMap(m);
    }

    private static final SimpleEntry<String, Float> entry(String k, float v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private static final String[] fieldNames(String... names) {
        return Arrays.stream(names).toArray(String[]::new);
    }

    /**
     * Returns the version string.
     * @since 1.0
     **/
    public static final String getVersion() {
        return "1.1";
    }

    private final Map<String, Float> boosts;

    private final String[] fields;;

    private IndexType(String[] fieldNames, Map<String, Float> boosts) {
        this.fields = fieldNames;
        this.boosts = boosts;
    }

    public Map<String, Float> getBoosts() {
        return boosts;
    }

    public String[] getFields() {
        return fields;
    }

}
