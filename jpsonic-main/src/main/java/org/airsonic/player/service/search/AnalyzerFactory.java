/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */

package org.airsonic.player.service.search;

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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Analyzer provider.
 * This class is a division of what was once part of SearchService and added functionality.
 * This class provides Analyzer which is used at index generation
 * and QueryAnalyzer which analyzes the specified query at search time.
 * Analyzer can be closed but is a reuse premise.
 * It is held in this class.
 */
@Component
public final class AnalyzerFactory {

    private Analyzer analyzer;

    private Analyzer queryAnalyzer;

    public static final String STOP_TAGS = "org/apache/lucene/analysis/ja/stoptags.txt";

    public static final String STOP_WARDS = "com/tesshu/jpsonic/service/stopwords.txt";

    public static final String STOP_WARDS_FOR_ARTIST = "com/tesshu/jpsonic/service/stopwords4artist.txt";

    /*
     * XXX 3.x -> 8.x : Convert UAX#29 Underscore Analysis to Legacy Analysis
     * 
     * Because changes in underscores before and after words
     * have a major effect on user's forward match search.
     * 
     * @see AnalyzerFactoryTestCase
     */
    private void addTokenFilterForUnderscoreRemovalAroundToken(Builder builder) throws IOException {
        builder
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "^\\_", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\_$", "replacement", "", "replace", "all");
    }

    /*
     * XXX 3.x -> 8.x : Handle brackets correctly
     * 
     * Process the input value of Genre search for search of domain value.
     * 
     * The tag parser performs special character conversion
     * when converting input values ​​from a file.
     * Therefore, the domain value may be different from the original value.
     * This filter allows searching by user readable value (file tag value).
     * 
     * @see org.jaudiotagger.tag.id3.framebody.FrameBodyTCON#convertID3v23GenreToGeneric
     * (TCON stands for Genre with ID3 v2.3-v2.4)
     * Such processing exists because brackets in the Gener string have a special meaning.
     */
    private void addTokenFilterForTokenToDomainValue(Builder builder) throws IOException {
        builder
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\(", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\)$", "replacement", "", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\)", "replacement", " ", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\{\\}", "replacement", "\\{ \\}", "replace", "all")
            .addTokenFilter(PatternReplaceFilterFactory.class,
                    "pattern", "\\[\\]", "replacement", "\\[ \\]", "replace", "all");
    }

    private CustomAnalyzer.Builder basicFilters(CustomAnalyzer.Builder builder, boolean isArtist) throws IOException {
        builder.addTokenFilter(CJKWidthFilterFactory.class) // before StopFilter
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class, "words", isArtist ? STOP_WARDS_FOR_ARTIST : STOP_WARDS, "ignoreCase", "true")
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", STOP_TAGS);
                // .addTokenFilter(EnglishPossessiveFilterFactory.class); XXX airsonic -> jpsonic : No longer meaningful in JapaneseTokenizer
        addTokenFilterForUnderscoreRemovalAroundToken(builder);
        return builder;
    }

    private Builder createDefaultAnalyzerBuilder(boolean isArtist) throws IOException {
        CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class);
        builder = basicFilters(builder, isArtist);
        return builder;
    }

    private Builder createKeywordAnalyzerBuilder() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer(KeywordTokenizerFactory.class);
    }

    private Builder createGenreAnalyzerBuilder() throws IOException {
        Builder builder = CustomAnalyzer.builder().withTokenizer(GenreTokenizerFactory.class);
        addTokenFilterForTokenToDomainValue(builder);
        return builder.addTokenFilter(CJKWidthFilterFactory.class)
        .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal",
        "false");
    }


    private Builder createExceptionalAnalyzerBuilder() throws IOException {
        return createKeywordAnalyzerBuilder()
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
        builder = basicFilters(builder, true)
                .addTokenFilter(PunctuationStemFilterFactory.class)
                .addTokenFilter(ToHiraganaFilterFactory.class);
        return builder;
    }

    /**
     * Returns the Analyzer to use when generating the index.
     * 
     * Whether this analyzer is applied to input values ​​depends on
     * the definition of the document's fields.
     * 
     * @return analyzer for index
     * @see DocumentFactory
     */
    public Analyzer getAnalyzer() throws IOException {
        if (isEmpty(analyzer)) {
            try {

                Analyzer defaultAnalyzer = createDefaultAnalyzerBuilder(false).build();
                Analyzer artistAnalyzer = createDefaultAnalyzerBuilder(true).build();
                Analyzer key = createKeywordAnalyzerBuilder().build();
                Analyzer id3Artist = createId3ArtistAnalyzerBuilder().build();
                Analyzer genre = createGenreAnalyzerBuilder().build();
                Analyzer artistExceptional = createArtistExceptionalBuilder().build();
                Analyzer exceptional = createExceptionalBuilder().build();

                Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
                analyzerMap.put(FieldNames.GENRE_KEY, key);
                analyzerMap.put(FieldNames.GENRE, genre);
                analyzerMap.put(FieldNames.ARTIST, artistAnalyzer);
                analyzerMap.put(FieldNames.ARTIST_READING, id3Artist);
                analyzerMap.put(FieldNames.COMPOSER_READING, id3Artist);
                analyzerMap.put(FieldNames.ARTIST_EX, artistExceptional);
                analyzerMap.put(FieldNames.ALBUM_EX, exceptional);
                analyzerMap.put(FieldNames.TITLE_EX, exceptional);

                this.analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, analyzerMap);

            } catch (IOException e) {
                throw new IOException("Error when initializing Analyzer.", e);
            }
        }
        return analyzer;
    }

    /**
     * Returns the analyzer to use when generating a query for index search.
     * 
     * String processing handled by QueryFactory
     * is limited to Lucene's modifier.
     * 
     * The processing of the operands is expressed
     * in the AnalyzerFactory implementation.
     * Rules for tokenizing/converting input values ​
     * should not be described in QueryFactory.
     * 
     * @return analyzer for query
     * @see QueryFactory
     */
    public Analyzer getQueryAnalyzer() throws IOException {
        if (isEmpty(queryAnalyzer)) {
            try {

                Analyzer defaultAnalyzer = createDefaultAnalyzerBuilder(false).build();
                Analyzer artistAnalyzer = createDefaultAnalyzerBuilder(true).build();
                Analyzer genre = createGenreAnalyzerBuilder().build();
                Analyzer id3Artist = createId3ArtistAnalyzerBuilder().build();
                Analyzer artistExceptional = createArtistExceptionalBuilder().build();
                Analyzer exceptional = createExceptionalBuilder().build();

                Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
                analyzerMap.put(FieldNames.GENRE, genre);
                analyzerMap.put(FieldNames.ARTIST, artistAnalyzer);
                analyzerMap.put(FieldNames.ARTIST_READING, id3Artist);
                analyzerMap.put(FieldNames.COMPOSER_READING, id3Artist);
                analyzerMap.put(FieldNames.ARTIST_EX, artistExceptional);
                analyzerMap.put(FieldNames.ALBUM_EX, exceptional);
                analyzerMap.put(FieldNames.TITLE_EX, exceptional);

                this.queryAnalyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, analyzerMap);

            } catch (IOException e) {
                throw new IOException("Error when initializing Analyzer.", e);
            }
        }
        return queryAnalyzer;
    }

}
