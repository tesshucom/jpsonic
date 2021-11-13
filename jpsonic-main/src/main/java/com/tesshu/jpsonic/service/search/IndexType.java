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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.lucene.search.BoostQuery;

/**
 * Enum that symbolizes the each lucene index entity. This class is a division of what was once part of SearchService
 * and added functionality.
 * 
 * @since legacy
 */
public enum IndexType {

    SONG(fieldNames(FieldNamesConstants.TITLE, //
            FieldNamesConstants.TITLE_READING, //
            FieldNamesConstants.ARTIST, //
            FieldNamesConstants.ARTIST_READING, //
            FieldNamesConstants.ARTIST_READING_ROMANIZED, //
            FieldNamesConstants.COMPOSER, //
            FieldNamesConstants.COMPOSER_READING, //
            FieldNamesConstants.COMPOSER_READING_ROMANIZED), //
            boosts(entry(FieldNamesConstants.TITLE, 3.0F), //
                    entry(FieldNamesConstants.TITLE_READING, 3.1F), //
                    entry(FieldNamesConstants.ARTIST, 2.0F), //
                    entry(FieldNamesConstants.ARTIST_READING, 2.1F), //
                    entry(FieldNamesConstants.ARTIST_READING_ROMANIZED, 2.1F), //
                    entry(FieldNamesConstants.COMPOSER_READING, 1.1F), //
                    entry(FieldNamesConstants.COMPOSER_READING_ROMANIZED, 1.1F))),

    ALBUM(fieldNames(FieldNamesConstants.ALBUM, //
            FieldNamesConstants.ALBUM_READING, //
            FieldNamesConstants.ARTIST, //
            FieldNamesConstants.ARTIST_READING, //
            FieldNamesConstants.ARTIST_READING_ROMANIZED), //
            boosts(entry(FieldNamesConstants.ALBUM, 2.0F), //
                    entry(FieldNamesConstants.ALBUM_READING, 2.1F), //
                    entry(FieldNamesConstants.ARTIST_READING, 1.1F),
                    entry(FieldNamesConstants.ARTIST_READING_ROMANIZED, 1.1F))),

    ALBUM_ID3(fieldNames(FieldNamesConstants.ALBUM, //
            FieldNamesConstants.ALBUM_READING, //
            FieldNamesConstants.ARTIST, //
            FieldNamesConstants.ARTIST_READING, //
            FieldNamesConstants.ARTIST_READING_ROMANIZED), //
            boosts(entry(FieldNamesConstants.ALBUM, 2.0F), //
                    entry(FieldNamesConstants.ALBUM_READING, 2.1F), //
                    entry(FieldNamesConstants.ARTIST_READING, 1.1F), //
                    entry(FieldNamesConstants.ARTIST_READING_ROMANIZED, 1.1F))),

    ARTIST(fieldNames(FieldNamesConstants.ARTIST, //
            FieldNamesConstants.ARTIST_READING, //
            FieldNamesConstants.ARTIST_READING_ROMANIZED), //
            boosts(entry(FieldNamesConstants.ARTIST_READING, 1.1F), //
                    entry(FieldNamesConstants.ARTIST_READING_ROMANIZED, 1.1F))),

    ARTIST_ID3(fieldNames(FieldNamesConstants.ARTIST, //
            FieldNamesConstants.ARTIST_READING, //
            FieldNamesConstants.ARTIST_READING_ROMANIZED), //
            boosts(entry(FieldNamesConstants.ARTIST_READING, 1.1F), //
                    entry(FieldNamesConstants.ARTIST_READING_ROMANIZED, 1.1F))),

    GENRE(fieldNames(FieldNamesConstants.GENRE_KEY, //
            FieldNamesConstants.GENRE), //
            boosts(entry(FieldNamesConstants.GENRE_KEY, 1.1F))),

    ;

    private final Map<String, Float> boosts;

    private final String[] fields;

    /**
     * Define the field's applied boost value when searching IndexType.
     * 
     * @param entry
     *            {@link #entry(String, float)}. When specifying multiple values, enumerate entries.
     * 
     * @return Map of boost values ​​to be applied to the field
     */
    @SafeVarargs
    private static Map<String, Float> boosts(SimpleEntry<String, Float>... entry) {
        Map<String, Float> m = LegacyMap.of();
        Arrays.stream(entry).forEach(kv -> m.put(kv.getKey(), kv.getValue()));
        return Collections.unmodifiableMap(m);
    }

    /**
     * Create an entry representing the boost value for the field.
     * 
     * @param k
     *            Field name defined by FieldNames
     * @param v
     *            Boost value
     */
    private static SimpleEntry<String, Float> entry(String k, float v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    /**
     * Defines the field that the input value is to search for when searching IndexType. If you specify multiple values,
     * list the field names.
     */
    private static String[] fieldNames(String... names) {
        return Arrays.stream(names).toArray(String[]::new);
    }

    IndexType(String[] fieldNames, Map<String, Float> boosts) {
        this.fields = fieldNames.clone();
        this.boosts = boosts;
    }

    /**
     * Returns a map of fields and boost values.
     * 
     * @return Map of fields and boost values
     * 
     * @since legacy
     * 
     * @see BoostQuery
     */
    public Map<String, Float> getBoosts() {
        return boosts;
    }

    /**
     * Return some of the fields defined in the index.
     * 
     * @return Fields mainly used in multi-field search
     * 
     * @since legacy
     */
    public String[] getFields() {
        return fields;
    }

}
