/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyzer provider. This class is a division of what was once part of
 * SearchService and added functionality.
 * 
 * Analyzer can be closed but is a reuse premise. It is held in this class.
 * 
 * Some of the query parses performed in legacy services are defined in
 * QueryAnalyzer. Most of the actual query parsing is done with QueryFactory.
 */
public final class AnalyzerFactory {

    private static AnalyzerFactory instance;

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerFactory.class);
    
    private static final Object l = new Object();

    /**
     * Returns an instance of AnalyzerFactory.
     * 
     * @return AnalyzerFactory instance
     */
    public static AnalyzerFactory getInstance() {
        if (null == instance) {
            synchronized (l) {
                if (instance == null) {
                    instance = new AnalyzerFactory();
                }
            }
        }
        return instance;
    }

    private Analyzer analyzer;

    private final String stopTags = "org/apache/lucene/analysis/ja/stoptags.txt";

    private final String stopWords = "com/tesshu/jpsonic/service/stopwords.txt";

    private AnalyzerFactory() {
    }

    private CustomAnalyzer.Builder filters(CustomAnalyzer.Builder builder) {
        try {
            builder
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", stopTags)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(CJKWidthFilterFactory.class) // before StopFilter
                .addTokenFilter(StopFilterFactory.class, "words", stopWords, "ignoreCase", "true");
        } catch (IOException e) {
            LOG.error("Error when initializing filters", e);
        }
        return builder;
    }

    private CustomAnalyzer.Builder whiteSpaceFilters(CustomAnalyzer.Builder builder) {
        try {
            builder
                .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", "(\\s)", "replacement", "", "replace", "all");
        } catch (IOException e) {
            LOG.error("Error when initializing filters", e);
        }
        return builder;
    }

    private CustomAnalyzer.Builder genrefilters(CustomAnalyzer.Builder builder) {
        try {
            builder
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(PatternReplaceFilterFactory.class, "pattern", "(\\s|-)", "replacement", "", "replace", "all");
        } catch (IOException e) {
            LOG.error("Error when initializing filters", e);
        }
        return builder;
    }

    /**
     * Return analyzer.
     * 
     * @return analyzer for index
     */
    public Analyzer getAnalyzer() {
        if (null == this.analyzer) {
            try {

                Analyzer jpDefault =
                        filters(CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class))
                        .build();
                Analyzer bareKeyword = CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class)
                        .build();
                Analyzer filteredKeyword =
                        filters(whiteSpaceFilters(CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class)))
                        .build();
                Analyzer genre =
                        genrefilters(CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class))
                        .build();

                Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
                analyzerMap.put(FieldNames.FOLDER, bareKeyword);
                analyzerMap.put(FieldNames.GENRE, genre);
                analyzerMap.put(FieldNames.MEDIA_TYPE, filteredKeyword);
                analyzerMap.put(FieldNames.ARTIST_FULL, filteredKeyword);
                analyzerMap.put(FieldNames.ARTIST_READING_HIRAGANA, filteredKeyword);
                analyzerMap.put(FieldNames.ALBUM_FULL, filteredKeyword);

                this.analyzer = new PerFieldAnalyzerWrapper(jpDefault, analyzerMap);

            } catch (IOException e) {
                LOG.error("Error when initializing Analyzer.", e);
            }
        }
        return this.analyzer;
    }

}
