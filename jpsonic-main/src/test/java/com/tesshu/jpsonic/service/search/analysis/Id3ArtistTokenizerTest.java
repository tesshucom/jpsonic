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

 Copyright 2020 (C) tesshu.com
 */

package com.tesshu.jpsonic.service.search.analysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Id3ArtistTokenizerTest {

    private Id3ArtistTokenizer tokenizer = new Id3ArtistTokenizer();

    @Test
    public void testIsTokenChar() {

        assertTrue(tokenizer.isTokenChar(';'));
        assertTrue(tokenizer.isTokenChar('/'));
        assertTrue(tokenizer.isTokenChar('\0'));

        assertFalse(tokenizer.isTokenChar(','));

        assertTrue(tokenizer.isTokenChar('.'));
        assertTrue(tokenizer.isTokenChar('。'));
        assertTrue(tokenizer.isTokenChar('、'));
        assertTrue(tokenizer.isTokenChar(':'));
        assertTrue(tokenizer.isTokenChar('\"'));
        assertTrue(tokenizer.isTokenChar('\''));
        assertTrue(tokenizer.isTokenChar('`'));
        assertTrue(tokenizer.isTokenChar(')'));
        assertTrue(tokenizer.isTokenChar('('));
        assertTrue(tokenizer.isTokenChar('*'));

    }

}
