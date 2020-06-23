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
            FieldNamesConstants.TITLE_EX,
            FieldNamesConstants.TITLE,
            FieldNamesConstants.ARTIST_READING,
            FieldNamesConstants.ARTIST_EX,
            FieldNamesConstants.ARTIST,
            FieldNamesConstants.COMPOSER_READING,
            FieldNamesConstants.COMPOSER),
        boosts(
            entry(FieldNamesConstants.TITLE_EX, 2.3F),
            entry(FieldNamesConstants.TITLE, 2.2F),
            entry(FieldNamesConstants.ARTIST_READING, 1.4F),
            entry(FieldNamesConstants.ARTIST_EX, 1.3F),
            entry(FieldNamesConstants.ARTIST, 1.2F),
            entry(FieldNamesConstants.COMPOSER_READING, 1.1F))),

    ALBUM(
        fieldNames(
            FieldNamesConstants.ALBUM_EX,
            FieldNamesConstants.ALBUM,
            FieldNamesConstants.ARTIST_READING,
            FieldNamesConstants.ARTIST_EX,
            FieldNamesConstants.ARTIST),
        boosts(
            entry(FieldNamesConstants.ALBUM_EX, 2.3F),
            entry(FieldNamesConstants.ALBUM, 2.3F),
            entry(FieldNamesConstants.ARTIST_READING, 1.1F))),

    ALBUM_ID3(
        fieldNames(
            FieldNamesConstants.ALBUM_EX,
            FieldNamesConstants.ALBUM,
            FieldNamesConstants.ARTIST_READING,
            FieldNamesConstants.ARTIST_EX,
            FieldNamesConstants.ARTIST),
        boosts(
            entry(FieldNamesConstants.ALBUM_EX, 2.3F),
            entry(FieldNamesConstants.ALBUM, 2.3F),
            entry(FieldNamesConstants.ARTIST_READING, 1.1F))),

    ARTIST(
        fieldNames(
            FieldNamesConstants.ARTIST_READING,
            FieldNamesConstants.ARTIST_EX,
            FieldNamesConstants.ARTIST),
        boosts(
            entry(FieldNamesConstants.ARTIST_READING, 1.1F))),

    ARTIST_ID3(
        fieldNames(
            FieldNamesConstants.ARTIST_READING,
            FieldNamesConstants.ARTIST_EX,
            FieldNamesConstants.ARTIST),
        boosts(
            entry(FieldNamesConstants.ARTIST_READING, 1.1F))),

    GENRE(
        fieldNames(
            FieldNamesConstants.GENRE_KEY,
            FieldNamesConstants.GENRE),
        boosts(
            entry(FieldNamesConstants.GENRE_KEY, 1.1F))),
    
    ;

    /**
     * Define the field's applied boost value when searching IndexType.
     * 
     * @param entry {@link #entry(String, float)}.
     *              When specifying multiple values, enumerate entries.
     * @return Map of boost values ​​to be applied to the field
     */
    @SafeVarargs
    private static final Map<String, Float> boosts(SimpleEntry<String, Float>... entry) {
        Map<String, Float> m = new HashMap<>();
        Arrays.stream(entry).forEach(kv -> m.put(kv.getKey(), kv.getValue()));
        return Collections.unmodifiableMap(m);
    }

    /**
     * Create an entry representing the boost value for the field.
     * 
     * @param k Field name defined by FieldNames
     * @param v Boost value
     * @return
     */
    private static final SimpleEntry<String, Float> entry(String k, float v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    /**
     * Defines the field that the input value is to search for
     * when searching IndexType.
     * If you specify multiple values, list the field names.
     * 
     * @param names
     * @return
     */
    private static final String[] fieldNames(String... names) {
        return Arrays.stream(names).toArray(String[]::new);
    }

    private final Map<String, Float> boosts;

    private final String[] fields;

    private IndexType(String[] fieldNames, Map<String, Float> boosts) {
        this.fields = fieldNames;
        this.boosts = boosts;
    }

    /**
     * Returns a map of fields and boost values.
     * 
     * @return Map of fields and boost values
     * @since legacy
     * @see BoostQuery
     */
    public Map<String, Float> getBoosts() {
        return boosts;
    }

    /**
     * Return some of the fields defined in the index.
     * 
     * @return Fields mainly used in multi-field search
     * @since legacy
     */
    public String[] getFields() {
        return fields;
    }

}
