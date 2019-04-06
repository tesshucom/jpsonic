package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.regex.Pattern;

public class HiraganaTermStemFilter extends TokenFilter {

    private static final Pattern ONLY_HIRAGANA = Pattern.compile("^[\\u3040-\\u309F]+$");

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final boolean passableOnlyAllHiragana;

    public HiraganaTermStemFilter(TokenStream in) {
        super(in);
        passableOnlyAllHiragana = false;
    }

    public HiraganaTermStemFilter(TokenStream in, boolean passableOnlyAllHiragana) {
        super(in);
        this.passableOnlyAllHiragana = passableOnlyAllHiragana;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (ONLY_HIRAGANA.matcher(termAtt).matches()) {
                if (!passableOnlyAllHiragana) {
                    input.end();
                    return false;
                }
            } else {
                if (passableOnlyAllHiragana) {
                    input.end();
                    return false;
                }
            }
            return true;
        } else
            return false;
    }

}