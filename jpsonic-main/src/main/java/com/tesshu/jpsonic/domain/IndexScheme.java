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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.domain;

/**
 * Indexing method. Only one policy is adopted for the entire server.
 */
public enum IndexScheme {

    /**
     * An English / Japanese index for users whose native language is Japanese. In addition to English, sorting with
     * Japanese phonological analysis and creation of normalized indexes etc will be supported.
     */
    NATIVE_JAPANESE,

    /**
     * An alphabet index that can treat Japanese, for users whose native language is not Japanese. It guarantees the
     * same quality of processing as NATIVE_JAPANESE, but the read analysis uses Romaji Japanese instead of Japanese.
     */
    ROMANIZED_JAPANESE,

    /**
     * A mode that does not analyze Japanese.
     */
    WITHOUT_JP_LANG_PROCESSING;

    public String getName() {
        return name();
    }

    public static IndexScheme of(String s) {
        if (NATIVE_JAPANESE.name().equals(s)) {
            return NATIVE_JAPANESE;
        } else if (ROMANIZED_JAPANESE.name().equals(s)) {
            return ROMANIZED_JAPANESE;
        }
        return WITHOUT_JP_LANG_PROCESSING;
    }
}
