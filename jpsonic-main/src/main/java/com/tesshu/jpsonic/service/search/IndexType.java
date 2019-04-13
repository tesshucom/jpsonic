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

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * IndexType.
 * This class is a division of what was once part of SearchService
 * and added functionality.
 */
public enum IndexType {

    SONG(
        fieldNames(
            FieldNames.TITLE_READING_HIRAGANA,
            FieldNames.TITLE,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST),
        boosts(
            entry(FieldNames.TITLE_READING_HIRAGANA, 2.4F),
            entry(FieldNames.TITLE, 2.3F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ALBUM(
        fieldNames(
            FieldNames.ALBUM_READING_HIRAGANA,
            FieldNames.ALBUM_FULL,
            FieldNames.ALBUM,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.FOLDER ),
        boosts(
            entry(FieldNames.ALBUM_READING_HIRAGANA, 2.4F),
            entry(FieldNames.ALBUM_FULL, 2.3F),
            entry(FieldNames.ALBUM, 2.2F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ALBUM_ID3(
        fieldNames(
            FieldNames.ALBUM_READING_HIRAGANA,
            FieldNames.ALBUM_FULL,
            FieldNames.ALBUM,
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.FOLDER_ID ),
        boosts(
            entry(FieldNames.ALBUM_READING_HIRAGANA, 2.4F),
            entry(FieldNames.ALBUM_FULL, 2.3F),
            entry(FieldNames.ALBUM, 2.2F),
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ARTIST(
        fieldNames(
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST,
            FieldNames.FOLDER),
        boosts(
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ARTIST_ID3(
        fieldNames(
            FieldNames.ARTIST_READING,
            FieldNames.ARTIST),
        boosts(
            entry(FieldNames.ARTIST_READING, 1.1F))),

    ;

    public static final class FieldNames {

        private FieldNames(){};

        /**
         * A field same to a legacy server, id field.
         * @since 1.0
         **/
        public static final String ID =                      "id";

        /**
         * A field same to a legacy server, id field.
         * @since 1.0
         **/
        public static final String FOLDER_ID =               "fId";

        /**
         * A field same to a legacy server, numeric field.
         * @since 1.0
         **/
        public static final String YEAR =                    "y";

        /**
         * A field same to a legacy server, key field that holds the normalized string.
         * @since 1.0
         **/
        public static final String GENRE =                   "g";

        /**
         * A field same to a legacy server, key field that holds the normalized string.
         * @since 1.0
         **/
        public static final String MEDIA_TYPE =              "m";

        /**
         * A field same to a legacy server, special key field to hold the path string.
         * @since 1.0
         **/
        public static final String FOLDER =                  "f";

        /**
         * A field same to a legacy server, usually with common word parsing.
         * @since 1.0
         **/
        public static final String ARTIST =                  "art";   

        /**
         * A field same to a legacy server, usually with common word parsing.
         * @since 1.0
         **/
        public static final String ALBUM =                   "alb";

        /**
         * A field same to a legacy server, usually with common word parsing.
         * @since 1.0
         **/
        public static final String TITLE =                   "tit";

        /**
         * Jpsonic specific reading field.
         * Parse rules are expected to correspond to breaks according to id3
         * and also to customary multi artists.
         * @since 1.0
         */
        public static final String ARTIST_READING =          "artR";

        /**
         * Jpsonic specific reading field.
         * Instead of a general-purpose role such as artist reading,
         * a string that is difficult for Japanese machine processing is registered as an aid.
         * Words that consists entirely of hiragana is a typical example.
         * @since 1.0
         * @deprecated Due to a change in analysis method, it may be discontinued in later versions.
         */
        public static final String ALBUM_READING_HIRAGANA =  "albRH";

        /**
         * Jpsonic specific reading field.
         * Instead of a general-purpose role such as artist reading,
         * a string that is difficult for Japanese machine processing is registered as an aid.
         * Words that consists entirely of hiragana is a typical example.
         * @since 1.0
         * @deprecated Due to a change in analysis method, it may be discontinued in later versions.
         */
        public static final String TITLE_READING_HIRAGANA =  "titRH";

        /**
         * Jpsonic specific full name field.
         * This is an area set for Japan-specific aid purposes.
         * Content/length such as album name is likely to reduce the accuracy of analysis.
         * 
         * @since 1.0
         * @deprecated Due to a change in analysis method, it may be discontinued in later versions.
         */
        @Deprecated
        public static final String ALBUM_FULL =              "albF";

    }

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
        return "1.0";
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
