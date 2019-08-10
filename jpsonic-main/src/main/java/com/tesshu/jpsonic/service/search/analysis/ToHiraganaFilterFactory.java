package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class ToHiraganaFilterFactory extends TokenFilterFactory {

    public ToHiraganaFilterFactory(Map<String, String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public ToHiraganaFilter create(TokenStream input) {
        return new ToHiraganaFilter(input);
    }

}
