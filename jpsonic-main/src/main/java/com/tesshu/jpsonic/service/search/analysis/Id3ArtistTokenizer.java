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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;

/**
 * A tokenizer that divides artist text at devide characters defined by id3, or whitespace and comma.
 */
public class Id3ArtistTokenizer extends CharTokenizer {

    /*
     * see http://id3.org/ ; v2.2 / v2.3 \0 v2.4 (Required for security)
     */
    private static final int ID3_DELIM = ';' | '/' | '\0';

    private static final int SPACE_SEPARATOR = (int) Character.SPACE_SEPARATOR;

    /**
     * Construct a new Id3ArtistTokenizer.
     */
    public Id3ArtistTokenizer() {
    }

    /**
     * Construct a new Id3ArtistTokenizer using a given {@link org.apache.lucene.util.AttributeFactory}.
     *
     * @param factory
     *            the attribute factory to use for this {@link Tokenizer}
     */
    public Id3ArtistTokenizer(AttributeFactory factory) {
        super(factory);
    }

    /**
     * Construct a new Id3ArtistTokenizer using a given {@link org.apache.lucene.util.AttributeFactory}.
     *
     * @param factory
     *            the attribute factory to use for this {@link Tokenizer}
     * @param maxTokenLen
     *            maximum token length the tokenizer will emit. Must be greater than 0 and less than
     *            MAX_TOKEN_LENGTH_LIMIT (1024*1024)
     * 
     * @throws IllegalArgumentException
     *             if maxTokenLen is invalid.
     */
    public Id3ArtistTokenizer(AttributeFactory factory, int maxTokenLen) {
        super(factory, maxTokenLen);
    }

    /**
     * Collects only characters which do not satisfy
     */
    @Override
    protected boolean isTokenChar(int c) {
        return !(ID3_DELIM == c || SPACE_SEPARATOR == Character.getType(c) || ',' == c);
    }

}
