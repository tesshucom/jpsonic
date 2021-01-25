package com.tesshu.jpsonic.service.search.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.util.Map;

import static org.apache.lucene.analysis.standard.StandardTokenizer.MAX_TOKEN_LENGTH_LIMIT;

public class GenreTokenizerFactory extends TokenizerFactory {

    private final int maxTokenLen;

    public GenreTokenizerFactory(Map<String, String> args) {
        super(args);
        maxTokenLen = getInt(args, "maxTokenLen", CharTokenizer.DEFAULT_MAX_WORD_LEN);
        if (maxTokenLen > MAX_TOKEN_LENGTH_LIMIT || maxTokenLen <= 0) {
            throw new IllegalArgumentException("maxTokenLen must be greater than 0 and less than "
                    + MAX_TOKEN_LENGTH_LIMIT + " passed: " + maxTokenLen);
        }
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        return new GenreTokenizer(factory, maxTokenLen);
    }

}