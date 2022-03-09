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

import java.io.IOException;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class ToHiraganaFilter extends TokenFilter {

    private final Transliterator transform;
    private final Transliterator.Position position;
    private final CharTermAttribute termAtt;
    private final ReplaceableTermAttribute replaceableAttribute;

    /**
     * Create a Filter that transforms text on the given stream.
     *
     * @param input
     *            {@link TokenStream} to filter.
     */
    @SuppressWarnings("deprecation")
    public ToHiraganaFilter(TokenStream input) {
        super(input);
        this.transform = Transliterator.getInstance("Katakana-Hiragana");
        this.position = new Transliterator.Position();
        this.termAtt = addAttribute(CharTermAttribute.class);
        this.replaceableAttribute = new ReplaceableTermAttribute();
        if (transform.getFilter() == null && transform instanceof com.ibm.icu.text.RuleBasedTransliterator) {
            final UnicodeSet sourceSet = transform.getSourceSet();
            if (!sourceSet.isEmpty()) {
                transform.setFilter(sourceSet);
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            replaceableAttribute.setText(termAtt);
            final int length = termAtt.length();
            position.start = 0;
            position.limit = length;
            position.contextStart = 0;
            position.contextLimit = length;
            transform.filteredTransliterate(replaceableAttribute, position, false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Wrap a {@link CharTermAttribute} with the Replaceable API.
     */
    private static class ReplaceableTermAttribute implements Replaceable {
        private char[] buffer;
        private int unitLength;
        private CharTermAttribute token;

        @Override
        public int char32At(int pos) {
            return UTF16.charAt(buffer, 0, unitLength, pos);
        }

        @Override
        public char charAt(int pos) {
            return buffer[pos];
        }

        @Override
        public void copy(int start, int limit, int dest) {
            char[] text = new char[limit - start];
            getChars(start, limit, text, 0);
            replace(dest, dest, text, 0, limit - start);
        }

        @Override
        public void getChars(int srcStart, int srcLimit, char[] dst, int dstStart) {
            System.arraycopy(buffer, srcStart, dst, dstStart, srcLimit - srcStart);
        }

        @Override
        public boolean hasMetaData() {
            return false;
        }

        @Override
        public int length() {
            return unitLength;
        }

        @Override
        public void replace(int start, int limit, char[] text, int charsStart, int charsLen) {
            final int newLength = shiftForReplace(start, limit, charsLen);
            System.arraycopy(text, charsStart, buffer, start, charsLen);
            unitLength = newLength;
            token.setLength(unitLength);
        }

        @Override
        public void replace(int start, int limit, String text) {
            final int charsLen = text.length();
            final int newLength = shiftForReplace(start, limit, charsLen);
            text.getChars(0, charsLen, buffer, start);
            unitLength = newLength;
            token.setLength(unitLength);
        }

        protected void setText(final CharTermAttribute token) {
            this.token = token;
            this.buffer = token.buffer();
            this.unitLength = token.length();
        }

        /** shift text (if necessary) for a replacement operation */
        private int shiftForReplace(int start, int limit, int charsLen) {
            final int replacementLength = limit - start;
            final int newLength = unitLength - replacementLength + charsLen;
            if (newLength > unitLength) {
                buffer = token.resizeBuffer(newLength);
            }
            if (replacementLength != charsLen && limit < unitLength) {
                System.arraycopy(buffer, limit, buffer, start + charsLen, unitLength - limit);
            }
            return newLength;
        }
    }
}
