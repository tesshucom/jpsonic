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

/**
 * Enum that symbolizes the field name used for lucene index. This class is a division of what was once part of
 * SearchService and added functionality.
 */
public final class FieldNamesConstants {

    /**
     * A field same to a legacy server, id field.
     *
     * @since legacy
     **/
    public static final String ID = "id";

    /**
     * A field same to a legacy server, id field.
     *
     * @since legacy
     **/
    public static final String FOLDER_ID = "fId";

    /**
     * A field same to a legacy server, numeric field.
     *
     * @since legacy
     **/
    public static final String YEAR = "y";

    /**
     * A field same to a legacy server, key field.
     *
     * @since legacy
     **/
    public static final String GENRE = "g";

    /**
     * A field same to a legacy server, key field.
     *
     * @since legacy
     **/
    public static final String MEDIA_TYPE = "m";

    /**
     * A field same to a legacy server, key field.
     *
     * @since legacy
     **/
    public static final String FOLDER = "f";

    /**
     * A field same to a legacy server, usually with common word parsing.
     *
     * @since legacy
     **/
    public static final String ARTIST = "art";

    /**
     * A field same to a legacy server, usually with common word parsing.
     *
     * @since legacy
     **/
    public static final String ALBUM = "alb";

    /**
     * A field same to a legacy server, usually with common word parsing.
     *
     * @since legacy
     **/
    public static final String TITLE = "tit";

    /**
     * Jpsonic specific assistance field. Key field that holds the normalized string.
     */
    public static final String GENRE_KEY = "gk";

    /**
     * Jpsonic specific reading field. Parse rules are expected to correspond to breaks according to id3 and also to
     * customary multi artists.
     */
    public static final String ARTIST_READING = "artR";

    public static final String ALBUM_READING = "albR";

    public static final String TITLE_READING = "titR";

    public static final String COMPOSER = "cmp";

    public static final String COMPOSER_READING = "cmpR";

    /**
     * Jpsonic specific assistance field. Phonological fields where syllable breaks are not always guaranteed.
     */
    public static final String ARTIST_READING_ROMANIZED = "artRR";

    /**
     * Jpsonic specific assistance field. Phonological fields where syllable breaks are not always guaranteed.
     */
    public static final String COMPOSER_READING_ROMANIZED = "cmpRR";

    private FieldNamesConstants() {
    }
}
