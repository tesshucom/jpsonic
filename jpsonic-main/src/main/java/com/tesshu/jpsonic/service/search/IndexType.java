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
/*
 * The following fields are not fully supported,
 * and are expected to be registered only when necessary.
 * Data entry rules may change
 * from version to version to reduce data usage charges.
 * 
 * _FULL
 * May be used to avoid Japanese-specific token analysis ambiguity.
 * 
 * _READING Basically
 * "reading". However, it may be tag input value instead of server analysis.
 *  It is not necessarily a general "reading".
 *  
 * _READING_HIRAGANA
 * This is a field that is expected to be "reading" in Hiragana.
 * Since Japanese-language analysis on servers mainly deals only with the first candidate,
 * there is a limit in pursuing accuracy by itself.
 * However, carrying a huge dictionary to pursue accuracy
 * and coding academically cleverly is wasteful.
 * (small is justice in this server)
 * Short phrases consisting of only hiragana / Katakana are high in analysis difficulty,
 * so they may be complemented in this field
 * without relying on analysis results.
 */
public enum IndexType {

	SONG(
        fieldNames(
			FieldNames.TITLE,
            FieldNames.TITLE_READING_HIRAGANA,
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA ),
        boosts(
            entry(FieldNames.TITLE_READING_HIRAGANA, 1.4F),
            entry(FieldNames.TITLE, 1.3F),
            entry(FieldNames.ARTIST_READING_HIRAGANA, 1.2F),
            entry(FieldNames.ARTIST_FULL, 1.1F))),

	ALBUM(
        fieldNames(
			FieldNames.ALBUM,
			FieldNames.ALBUM_FULL,
            FieldNames.ALBUM_READING_HIRAGANA,
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA,
			FieldNames.FOLDER ),
        boosts(
            entry(FieldNames.ALBUM_READING_HIRAGANA, 1.4F),
            entry(FieldNames.ARTIST_READING_HIRAGANA, 1.3F),
            entry(FieldNames.ALBUM_FULL, 1.2F),
            entry(FieldNames.ARTIST_FULL, 1.1F))),

	ALBUM_ID3(
        fieldNames(
			FieldNames.ALBUM,
			FieldNames.ALBUM_FULL,
			FieldNames.ALBUM_READING_HIRAGANA,
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA,
			FieldNames.FOLDER_ID ),
        boosts(
            entry(FieldNames.ALBUM_READING_HIRAGANA, 1.4F),
            entry(FieldNames.ARTIST_READING_HIRAGANA, 1.3F),
            entry(FieldNames.ALBUM_FULL, 1.2F),
            entry(FieldNames.ARTIST_FULL, 1.1F))),

	ARTIST(
	    fieldNames(
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA,
			FieldNames.FOLDER ),
	    boosts(
                entry(FieldNames.ARTIST_READING_HIRAGANA, 1.2F),
                entry(FieldNames.ARTIST_FULL, 1.1F))),

	ARTIST_ID3(
        fieldNames(
			FieldNames.ARTIST,
			FieldNames.ARTIST_FULL,
			FieldNames.ARTIST_READING,
			FieldNames.ARTIST_READING_HIRAGANA ),
		boosts(
	        entry(FieldNames.ARTIST_READING_HIRAGANA, 1.2F),
	        entry(FieldNames.ARTIST_FULL, 1.1F)));

    public static final class FieldNames {
	    /*
	     * The contents of analysis are different for each field.
	     * Defined in Analyzer.
	     * 
	     * Normal analysis              - Normal tokenizing and filtering
         * Other than Normal analysis   - No tokenize, special filtering
         * 
         * Asterisk is unconditional registration.
	     */

        /* Emphasis on complementation as artists are less data and more important */
        public static final String ARTIST =                  "art";   // * Normal analysis 
        public static final String ARTIST_FULL =             "artF";  // Registration when other than hiragana (possibility of various character types)
        public static final String ARTIST_READING =          "artR";  // * Sort key (possibility of various character types)
        public static final String ARTIST_READING_HIRAGANA = "artRH"; // Convert to Hiragana and register

        public static final String ALBUM =                   "alb";   // * Normal analysis
		public static final String ALBUM_FULL =              "albF";  // Registration when other than hiragana
        public static final String ALBUM_READING_HIRAGANA =  "albRH"; // Register when hiragana only

        public static final String TITLE =                   "tit";   // * Normal analysis
        //public static final String TITLE_FULL =            "titF";  // Do not register (consider the amount of data)
        public static final String TITLE_READING_HIRAGANA =  "titRH"; // Register when hiragana only

        public static final String ID =                      "id";
        public static final String GENRE =                   "g";
        public static final String YEAR =                    "y";
        public static final String MEDIA_TYPE =              "m";
        public static final String FOLDER =                  "f";
        public static final String FOLDER_ID =               "fId";
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
