package com.tesshu.jpsonic.service.search.analysis;

import com.ibm.icu.text.Transliterator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.regex.Pattern;

public class ToHiraganaFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private static final Pattern ALPHANUMERIC_CHARACTERS = Pattern.compile("^[a-zA-Z0-9]+$");

    public ToHiraganaFilter(TokenStream in) {
        super(in);
    }

    private boolean containsKatakana(char[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            if (UnicodeBlock.of(buffer[i]) == Character.UnicodeBlock.KATAKANA) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (ALPHANUMERIC_CHARACTERS.matcher(termAtt).matches()) {
                input.end();
                return false;
            } else if (containsKatakana(termAtt.buffer())) {
                String s = Transliterator.getInstance("Katakana-Hiragana").transliterate(termAtt.toString());
                clearAttributes();
                termAtt.append(s);
            }
            return true;
        } else
            return false;
    }

}