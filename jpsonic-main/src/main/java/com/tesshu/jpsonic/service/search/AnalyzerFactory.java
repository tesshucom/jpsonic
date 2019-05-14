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
import com.tesshu.jpsonic.service.search.analysis.GenreTokenizerFactory;
import com.tesshu.jpsonic.service.search.analysis.HiraganaStopFilterFactory;
import com.tesshu.jpsonic.service.search.analysis.Id3ArtistTokenizerFactory;
import com.tesshu.jpsonic.service.search.analysis.PunctuationStemFilterFactory;
import com.tesshu.jpsonic.service.search.analysis.ToHiraganaFilterFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder;
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

    private Analyzer queryAnalyzer;
    
    public static final String STOP_TAGS = "org/apache/lucene/analysis/ja/stoptags.txt";

    public static final String STOP_WARDS = "com/tesshu/jpsonic/service/stopwords.txt";

    private AnalyzerFactory() {
    }

    private CustomAnalyzer.Builder addWildCard(CustomAnalyzer.Builder builder) throws IOException {
        builder.addTokenFilter(PatternReplaceFilterFactory.class, "pattern", "(^(?!.*\\*$).+$)", "replacement", "$1*", "replace", "first");
        return builder;
    }

    private CustomAnalyzer.Builder basicFilters(CustomAnalyzer.Builder builder) throws IOException {
        builder.addTokenFilter(CJKWidthFilterFactory.class) // before StopFilter
                .addTokenFilter(StopFilterFactory.class, "words", STOP_WARDS, "ignoreCase", "true")
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", STOP_TAGS)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class);
        return builder;
    }

    private Builder createMediaAnalyzerBuilder() throws IOException {
        return createKeyAnalyzerBuilder().addTokenFilter(PunctuationStemFilterFactory.class);
    }

    private Builder createGenreAnalyzerBuilder() throws IOException {
        return CustomAnalyzer.builder().withTokenizer(GenreTokenizerFactory.class)
                .addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false");
    }

    private Builder createMultiTokenAnalyzerBuilder() throws IOException {
        CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class);
        builder = basicFilters(builder);
        return builder;
    }

    private Builder createKeyAnalyzerBuilder() throws IOException {
        return CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class);
    }

    private Builder createExceptionalAnalyzerBuilder() throws IOException {
        return createKeyAnalyzerBuilder()
            .addTokenFilter(CJKWidthFilterFactory.class)
            .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", STOP_TAGS)
            .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
            .addTokenFilter(LowerCaseFilterFactory.class)
            .addTokenFilter(PunctuationStemFilterFactory.class);
    }

    private Builder createArtistExceptionalBuilder() throws IOException {
        return createExceptionalAnalyzerBuilder()
                .addTokenFilter(HiraganaStopFilterFactory.class, "passableOnlyAllHiragana", "false");
    }

    private Builder createExceptionalBuilder() throws IOException {
        return createExceptionalAnalyzerBuilder()
                .addTokenFilter(HiraganaStopFilterFactory.class, "passableOnlyAllHiragana", "true");
    }

    private Builder createId3ArtistAnalyzerBuilder() throws IOException {  
        CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(Id3ArtistTokenizerFactory.class);
        builder = basicFilters(builder)
                .addTokenFilter(PunctuationStemFilterFactory.class)
                .addTokenFilter(ToHiraganaFilterFactory.class);
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

                Analyzer key = createKeyAnalyzerBuilder().build();
                Analyzer media = createMediaAnalyzerBuilder().build();
                Analyzer id3Artist = createId3ArtistAnalyzerBuilder().build();
                Analyzer genre = createGenreAnalyzerBuilder().build();
                Analyzer multiTerm = createMultiTokenAnalyzerBuilder().build();
                Analyzer artistExceptional = createArtistExceptionalBuilder().build();
                Analyzer exceptional = createExceptionalBuilder().build();

                Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
                analyzerMap.put(FieldNames.FOLDER, key);
                analyzerMap.put(FieldNames.GENRE_KEY, key);
                analyzerMap.put(FieldNames.GENRE, genre);
                analyzerMap.put(FieldNames.MEDIA_TYPE, media);
                analyzerMap.put(FieldNames.ARTIST_READING, id3Artist);
                analyzerMap.put(FieldNames.ARTIST_EX, artistExceptional);
                analyzerMap.put(FieldNames.ALBUM_EX, exceptional);
                analyzerMap.put(FieldNames.TITLE_EX, exceptional);

                this.analyzer = new PerFieldAnalyzerWrapper(multiTerm, analyzerMap);

            } catch (IOException e) {
                LOG.error("Error when initializing Analyzer.", e);
            }
        }
        return this.analyzer;
    }

    /**
     * Return analyzer.
     * 
     * @return analyzer for index
     */
    public Analyzer getQueryAnalyzer() {
        if (null == this.queryAnalyzer) {
            try {

                Analyzer key = createKeyAnalyzerBuilder().build();
                Analyzer media = createMediaAnalyzerBuilder().build();
                Analyzer genre = createGenreAnalyzerBuilder().build();
                Analyzer id3Artist = addWildCard(createId3ArtistAnalyzerBuilder()).build();
                Analyzer multiTerm = addWildCard(createMultiTokenAnalyzerBuilder()).build();
                Analyzer artistExceptional = addWildCard(createArtistExceptionalBuilder()).build();
                Analyzer exceptional = addWildCard(createExceptionalBuilder()).build();

                Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
                analyzerMap.put(FieldNames.FOLDER, key);
                analyzerMap.put(FieldNames.GENRE_KEY, key);
                analyzerMap.put(FieldNames.GENRE, genre);
                analyzerMap.put(FieldNames.MEDIA_TYPE, media);
                analyzerMap.put(FieldNames.ARTIST_READING, id3Artist);
                analyzerMap.put(FieldNames.ARTIST_EX, artistExceptional);
                analyzerMap.put(FieldNames.ALBUM_EX, exceptional);
                analyzerMap.put(FieldNames.TITLE_EX, exceptional);

                this.queryAnalyzer = new PerFieldAnalyzerWrapper(multiTerm, analyzerMap);

            } catch (IOException e) {
                LOG.error("Error when initializing Analyzer.", e);
            }
        }
        return this.queryAnalyzer;
    }

}
