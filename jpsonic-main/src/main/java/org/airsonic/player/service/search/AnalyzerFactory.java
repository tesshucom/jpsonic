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

import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter;
import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter.Mode;
import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilterFactory;
import com.tesshu.jpsonic.service.search.analysis.GenreTokenizerFactory;
import com.tesshu.jpsonic.service.search.analysis.Id3ArtistTokenizerFactory;
import com.tesshu.jpsonic.service.search.analysis.PunctuationStemFilter;
import com.tesshu.jpsonic.service.search.analysis.PunctuationStemFilterFactory;
import com.tesshu.jpsonic.service.search.analysis.ToHiraganaFilter;
import com.tesshu.jpsonic.service.search.analysis.ToHiraganaFilterFactory;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.LegacyMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.IOUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Analyzer provider. This class is a division of what was once part of SearchService and added functionality. This
 * class provides Analyzer which is used at index generation and QueryAnalyzer which analyzes the specified query at
 * search time. Analyzer can be closed but is a reuse premise. It is held in this class.
 */
@Component
@DependsOn({ "settingsService" })
public final class AnalyzerFactory {

    private Analyzer analyzer;

    private Analyzer queryAnalyzer;

    private String stopTags;

    private static final String STOP_WARDS_FOR_ARTIST = "com/tesshu/jpsonic/service/stopwords4artist.txt";

    private String stopWords;

    boolean isSearchMethodLegacy;

    private static final String FILTER_ATTR_PATTERN = "pattern";

    private static final String FILTER_ATTR_REPLACEMENT = "replacement";

    private static final String FILTER_ATTR_REPLACE = "replace";

    private static final String FILTER_ATTR_ALL = "all";

    @SuppressWarnings("PMD.NullAssignment")
    /*
     * (analyzer, queryAnalyzer) Intentional allocation to clear cache. Dynamic analyzer changes require explicit cache
     * clearing. (The constructor is called by Spring, so changes are always dynamic.) However, this method is usually
     * executed only once when the server starts. The timing of initialization and dynamic changes should only be
     * considered during testing.
     */
    void setSearchMethodLegacy(boolean isSearchMethodLegacy) {
        this.isSearchMethodLegacy = isSearchMethodLegacy;
        if (!isSearchMethodLegacy) {
            stopWords = "com/tesshu/jpsonic/service/stopwords4phrase.txt";
            stopTags = "com/tesshu/jpsonic/service/stoptags4phrase.txt";
        } else {
            stopWords = "com/tesshu/jpsonic/service/stopwords.txt";
            stopTags = "org/apache/lucene/analysis/ja/stoptags.txt";
        }
        analyzer = null;
        queryAnalyzer = null;
    }

    public AnalyzerFactory(SettingsService settingsService) {
        setSearchMethodLegacy(settingsService.isSearchMethodLegacy());
    }

    /*
     * XXX 3.x -> 8.x : Convert UAX#29 Underscore Analysis to Legacy Analysis
     *
     * Because changes in underscores before and after words have a major effect on user's forward match search.
     *
     * @see AnalyzerFactoryTest
     */
    private void addTokenFilterForUnderscoreRemovalAroundToken(Builder builder) throws IOException {
        builder.addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "^\\_", FILTER_ATTR_REPLACEMENT,
                "", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL).addTokenFilter(PatternReplaceFilterFactory.class,
                        FILTER_ATTR_PATTERN, "\\_$", FILTER_ATTR_REPLACEMENT, "", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL);
    }

    /*
     * XXX 3.x -> 8.x : Handle brackets correctly
     *
     * Process the input value of Genre search for search of domain value.
     *
     * The tag parser performs special character conversion when converting input values ​​from a file. Therefore, the
     * domain value may be different from the original value. This filter allows searching by user readable value (file
     * tag value).
     *
     * @see org.jaudiotagger.tag.id3.framebody.FrameBodyTCON#convertID3v23GenreToGeneric (TCON stands for Genre with ID3
     * v2.3-v2.4) Such processing exists because brackets in the Gener string have a special meaning.
     */
    private void addTokenFilterForTokenToDomainValue(Builder builder) throws IOException {
        builder.addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "\\(", FILTER_ATTR_REPLACEMENT,
                "", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "\\)$", FILTER_ATTR_REPLACEMENT,
                        "", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "\\)", FILTER_ATTR_REPLACEMENT,
                        " ", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "\\{\\}",
                        FILTER_ATTR_REPLACEMENT, "\\{ \\}", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, FILTER_ATTR_PATTERN, "\\[\\]",
                        FILTER_ATTR_REPLACEMENT, "\\[ \\]", FILTER_ATTR_REPLACE, FILTER_ATTR_ALL);
    }

    private CustomAnalyzer.Builder basicFilters(CustomAnalyzer.Builder builder, boolean isArtist) throws IOException {
        builder.addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class, "words", isArtist ? STOP_WARDS_FOR_ARTIST : stopWords,
                        "ignoreCase", "true")
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", stopTags);
        // .addTokenFilter(EnglishPossessiveFilterFactory.class); XXX airsonic -> jpsonic : possession(issues#290)
        addTokenFilterForUnderscoreRemovalAroundToken(builder);
        return builder;
    }

    private Builder createDefaultAnalyzerBuilder(boolean isArtist) throws IOException {
        CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class);
        builder = basicFilters(builder, isArtist);
        return builder;
    }

    private Builder createKeywordAnalyzerBuilder() throws IOException {
        return CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class);
    }

    private Analyzer createGenreAnalyzer() throws IOException {
        Builder builder = CustomAnalyzer.builder().withTokenizer(GenreTokenizerFactory.class);
        addTokenFilterForTokenToDomainValue(builder);
        return builder.addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false").build();
    }

    private Builder createExceptionalAnalyzerBuilder() throws IOException {
        return createKeywordAnalyzerBuilder().addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", stopTags)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class).addTokenFilter(PunctuationStemFilterFactory.class);
    }

    private Analyzer createReadingAnalyzer() throws IOException {
        if (isSearchMethodLegacy) {
            CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(Id3ArtistTokenizerFactory.class);
            builder = basicFilters(builder, true).addTokenFilter(PunctuationStemFilterFactory.class)
                    .addTokenFilter(ToHiraganaFilterFactory.class);
            return builder.build();
        }

        CharArraySet stopWords4Artist = getWords(STOP_WARDS_FOR_ARTIST);
        Set<String> stopTagset = loadStopTags();
        return new StopwordAnalyzerBase() {
            @SuppressWarnings("PMD.CloseResource") // False positive. Stream is reused by ReuseStrategy.
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                final Tokenizer source = new StandardTokenizer();
                TokenStream result = new CJKWidthFilter(source);
                result = new ASCIIFoldingFilter(result, false);
                result = new LowerCaseFilter(result);
                result = new StopFilter(result, stopWords4Artist);
                result = new JapanesePartOfSpeechStopFilter(result, stopTagset);
                result = new PunctuationStemFilter(result);
                result = new CJKBigramFilter(result);
                result = new ToHiraganaFilter(result);
                return new TokenStreamComponents(source, result);
            }

            @Override
            protected TokenStream normalize(String fieldName, TokenStream in) {
                return new LowerCaseFilter(new CJKWidthFilter(in));
            }

        };
    }

    private Analyzer createExAnalyzer(boolean isArtist) throws IOException {
        if (isSearchMethodLegacy) {
            ComplementaryFilter.Mode mode = isArtist ? Mode.STOP_WORDS_ONLY : Mode.STOP_WORDS_ONLY_AND_HIRA_KATA_ONLY;
            return createExceptionalAnalyzerBuilder()
                    .addTokenFilter(ComplementaryFilterFactory.class, "mode", mode.value(), "stopwards", stopWords)
                    .build();
        }

        Set<String> stopTagset = loadStopTags();

        return new StopwordAnalyzerBase() {

            @SuppressWarnings("PMD.CloseResource") // False positive. Stream is reused by ReuseStrategy.
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                final Tokenizer source = new StandardTokenizer();
                TokenStream result = new CJKWidthFilter(source);
                result = new ASCIIFoldingFilter(result, false);
                result = new LowerCaseFilter(result);
                result = new PunctuationStemFilter(result);
                result = new JapanesePartOfSpeechStopFilter(result, stopTagset);
                result = new ComplementaryFilter(result, Mode.HIRA_KATA_ONLY, null);
                result = new ToHiraganaFilter(result);
                result = new CJKBigramFilter(result);
                return new TokenStreamComponents(source, result);
            }

        };

    }

    private CharArraySet getWords(String wordsFile) throws IOException {
        try (Reader reader = IOUtils.getDecodingReader(getClass().getResourceAsStream("/".concat(wordsFile)), UTF_8)) {
            return WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, true));
        } catch (IOException e) {
            // Usually unreachable due to classpath resources
            throw new IOException("Failed to get the stopword file.", e);
        }
    }

    private Set<String> loadStopTags() throws IOException {
        Set<String> stopTagset = new HashSet<>();
        CharArraySet cas = getWords(stopTags);
        if (cas != null) {
            for (Object element : cas) {
                stopTagset.add(String.valueOf((char[]) element));
            }
        }
        return stopTagset;
    }

    @SuppressWarnings("PMD.CloseResource")
    /*
     * Analysers are the factory class for TokenStreams and thread-safe. Loaded only once at startup and used for
     * scanning and searching. Do not explicitly close now. Triaged by #829.
     */
    public Analyzer getAnalyzer() throws IOException {
        if (isEmpty(analyzer)) {
            try {

                Analyzer artist = createDefaultAnalyzerBuilder(true).build();
                Analyzer reading = createReadingAnalyzer();
                Analyzer exceptional = createExAnalyzer(false);
                Analyzer artistEx = createExAnalyzer(true);

                this.analyzer = new PerFieldAnalyzerWrapper(createDefaultAnalyzerBuilder(false).build(),
                        LegacyMap.of(FieldNamesConstants.GENRE_KEY, createKeywordAnalyzerBuilder().build(),
                                FieldNamesConstants.ARTIST, artist, FieldNamesConstants.COMPOSER, artist,
                                FieldNamesConstants.ARTIST_READING, reading, FieldNamesConstants.COMPOSER_READING,
                                reading, FieldNamesConstants.ALBUM_EX, exceptional, FieldNamesConstants.TITLE_EX,
                                exceptional, FieldNamesConstants.ARTIST_EX, artistEx, FieldNamesConstants.GENRE,
                                createGenreAnalyzer()));

            } catch (IOException e) {
                throw new IOException("Error when initializing Analyzer.", e);
            }
        }
        return analyzer;
    }

    public Analyzer getQueryAnalyzer() throws IOException {
        if (isEmpty(queryAnalyzer)) {
            // The definition is the same except for GENRE_KEY.
            queryAnalyzer = getAnalyzer();
        }
        return queryAnalyzer;
    }

}
