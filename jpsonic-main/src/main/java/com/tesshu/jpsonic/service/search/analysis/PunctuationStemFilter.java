
package com.tesshu.jpsonic.service.search.analysis;

import static com.tesshu.jpsonic.domain.JapaneseReadingUtils.isPunctuation;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

public class PunctuationStemFilter extends TokenFilter {

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public PunctuationStemFilter(TokenStream in) {
        super(in);
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
