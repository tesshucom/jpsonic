package com.tesshu.jpsonic.service.search.analysis;

import com.ibm.icu.text.Transliterator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.lang.Character.UnicodeBlock;

public class ToHiraganaFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

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
            if (containsKatakana(termAtt.buffer())) {
                String s = Transliterator.getInstance("Katakana-Hiragana").transliterate(termAtt.toString());
                clearAttributes();
                termAtt.append(s);
            }
            return true;
        } else
            return false;
    }

}