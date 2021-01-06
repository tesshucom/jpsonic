package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

import java.io.IOException;

import static com.tesshu.jpsonic.domain.JapaneseReadingUtils.isPunctuation;

public class PunctuationStemFilter extends TokenFilter {

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public PunctuationStemFilter(TokenStream in) {
        super(in);
    }

    @SuppressWarnings("PMD.AvoidReassigningLoopVariables") // The only place using reassigning.
    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            char buffer[] = termAtt.buffer();
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