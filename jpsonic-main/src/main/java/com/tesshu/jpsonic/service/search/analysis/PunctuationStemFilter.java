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

import static com.tesshu.jpsonic.domain.JapaneseReadingUtils.isPunctuation;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

public class PunctuationStemFilter extends TokenFilter {

    private final CharTermAttribute termAtt;

    public PunctuationStemFilter(TokenStream in) {
        super(in);
        termAtt = addAttribute(CharTermAttribute.class);
    }

    @SuppressWarnings("PMD.AvoidReassigningLoopVariables")
    /*
     * It's a complicated way of writing, but it has been confirmed to work. This rule can be operated as a normal rule
     * by ruleset.xml. <property name="forReassign" value="skip" /> However, it is rarely used unless performance is
     * required, so use annotation suppression to pay attention. If new code is added that issues this warning, it
     * should be scrutinized.
     */
    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            char[] buffer = termAtt.buffer();
            int length = termAtt.length();
            for (int i = 0; i < length; i++) {
                final char ch = buffer[i];
                if (isPunctuation(ch)) {
                    length = StemmerUtil.delete(buffer, i--, length);
                }
                if (0 == length) {
                    return false;
                }
            }
            termAtt.setLength(length);
            return true;
        } else {
            return false;
        }
    }

}
