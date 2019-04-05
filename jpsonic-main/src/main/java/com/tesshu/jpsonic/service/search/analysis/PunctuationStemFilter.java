package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

import java.io.IOException;

public class PunctuationStemFilter extends TokenFilter {

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public PunctuationStemFilter(TokenStream in) {
        super(in);
    }

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

    private boolean isPunctuation(char ch) {
        switch (Character.getType(ch)) {
            case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
            case Character.CONTROL:
            case Character.FORMAT:
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.CONNECTOR_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.MATH_SYMBOL:
            case Character.CURRENCY_SYMBOL:
            case Character.MODIFIER_SYMBOL:
            case Character.OTHER_SYMBOL:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
                return true;
            default:
                return false;
        }
    }

}