/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter;
import com.tesshu.jpsonic.service.search.analysis.ComplementaryFilter.Mode;
import com.tesshu.jpsonic.service.search.analysis.GenreTokenizerFactory;
import com.tesshu.jpsonic.service.search.analysis.PunctuationStemFilter;
import com.tesshu.jpsonic.service.search.analysis.ToHiraganaFilter;
import com.tesshu.jpsonic.util.LegacyMap;
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
import org.springframework.stereotype.Component;

/**
 * Analyzer provider. This class is a division of what was once part of SearchService and added functionality. This
 * class provides Analyzer which is used at index generation and QueryAnalyzer which analyzes the specified query at
 * search time. Analyzer can be closed but is a reuse premise. It is held in this class.
 */
@Component
public final class AnalyzerFactory {

    private static final String STOP_WORDS = "com/tesshu/jpsonic/service/stopwords4phrase.txt";
    private static final String STOP_WARDS_FOR_ARTIST = "com/tesshu/jpsonic/service/stopwords4artist.txt";
    private static final String STOP_TAGS = "com/tesshu/jpsonic/service/stoptags4phrase.txt";
    private static final String FILTER_ATTR_PATTERN = "pattern";
    private static final String FILTER_ATTR_REPLACEMENT = "replacement";
    private static final String FILTER_ATTR_REPLACE = "replace";
    private static final String FILTER_ATTR_ALL = "all";

    private Analyzer analyzer;

    private static CharArraySet loadWords(String wordsFile) {
        try (Reader reader = IOUtils.getDecodingReader(AnalyzerFactory.class.getResourceAsStream("/".concat(wordsFile)),
                UTF_8)) {
            return WordlistLoader.getWordSet(reader, "#", new CharArraySet(16, true));
        } catch (IOException e) {
            // Usually unreachable due to classpath resources
            throw new IllegalArgumentException("Failed to get the stopword file.", e);
        }
    }

    private static Set<String> loadStopTags() {
        final Set<String> stopTagset = new HashSet<>();
        CharArraySet cas = loadWords(STOP_TAGS);
        if (cas != null) {
            cas.stream().forEach(o -> stopTagset.add(String.valueOf((char[]) o)));
        }
        return stopTagset;
    }

    /**
     * Create a generic Analyzer to which the most basic filter set applies. The stop-word changes depending on whether
     * it is the artist field or not. Also, related to UAX#29, processing that makes Underscore processing similar to
     * the specifications of legacy servers is applied.
     */
    private Analyzer createDefaultAnalyzer(boolean isArtist) throws IOException {
        CustomAnalyzer.Builder builder = CustomAnalyzer.builder().withTokenizer(JapaneseTokenizerFactory.class)
                .addTokenFilter(CJKWidthFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false")
                .addTokenFilter(LowerCaseFilterFactory.class) //
                .addTokenFilter(StopFilterFactory.class, //
                        "words", isArtist ? STOP_WARDS_FOR_ARTIST : STOP_WORDS, //
                        "ignoreCase", "true")
                .addTokenFilter(JapanesePartOfSpeechStopFilterFactory.class, "tags", STOP_TAGS)
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "^\\_", //
                        FILTER_ATTR_REPLACEMENT, "", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL) //
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\_$", //
                        FILTER_ATTR_REPLACEMENT, "", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL);
        return builder.build();
    }

    /**
     * Create an Analyzer dedicated to Artist Reading. This analyzer is highly dependent on the characteristics of the
     * language of interest. Consideration should also be given to how the voice input engine used handles foreign
     * words, especially when supporting voice input searches.
     */
    private Analyzer createArtistReadingAnalyzer() throws IOException {
        CharArraySet stopWords4Artist = loadWords(STOP_WARDS_FOR_ARTIST);
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

    /**
     * Create an Analyzer dedicated to the Genre field. This Analyzer currently includes parsing processing that takes
     * into account third-party implementations. The implementation of the tag parser performs special character
     * conversion under special conditions (the square brackets in the Gener string have a unique meaning). Therefore,
     * the parsed value may differ from the original value. Some filter will be applied to suppress this issue when
     * searching.
     * 
     * @see org.jaudiotagger.tag.id3.framebody.FrameBodyTCON#convertID3v23GenreToGeneric
     */
    private Analyzer createGenreAnalyzer() throws IOException {
        Builder builder = CustomAnalyzer.builder().withTokenizer(GenreTokenizerFactory.class) //
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\(", FILTER_ATTR_REPLACEMENT, "", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\)$", FILTER_ATTR_REPLACEMENT, "", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\)", FILTER_ATTR_REPLACEMENT, " ", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\{\\}", FILTER_ATTR_REPLACEMENT, "\\{ \\}", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(PatternReplaceFilterFactory.class, //
                        FILTER_ATTR_PATTERN, "\\[\\]", FILTER_ATTR_REPLACEMENT, "\\[ \\]", //
                        FILTER_ATTR_REPLACE, FILTER_ATTR_ALL)
                .addTokenFilter(CJKWidthFilterFactory.class) //
                .addTokenFilter(ASCIIFoldingFilterFactory.class, "preserveOriginal", "false");
        return builder.build();
    }

    /**
     * Create a genre-key analyzer to reference the genre on the database. GenreAnalyser generates a multi-genre index
     * on the search index. Used to create an internal key that will be used when referencing records in the database
     * after searching in multiple genres.
     */
    private Analyzer createGenreKeyAnalyzer() throws IOException {
        return CustomAnalyzer.builder().withTokenizer(KeywordTokenizerFactory.class).build();
    }

    /**
     * Create an analyzer to complement the analysis that is difficult to deal with with a normal analyzer. This
     * analyzer is highly dependent on the characteristics of the language of interest.As a result of normal
     * morphological analysis and Stopward analysis, the case of a special pattern in which the index is completely
     * missing is extracted and processed by Bigram.
     */
    private Analyzer createExAnalyzer() throws IOException {
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

    /**
     * Analysers are the factory class for TokenStreams and thread-safe. Loaded only once at startup and used for
     * scanning and searching.
     */
    @SuppressWarnings("PMD.CloseResource") // False positive. Stream is reused by ReuseStrategy.
    public Analyzer getAnalyzer() {
        if (isEmpty(analyzer)) {
            try {

                Analyzer defaultAnalyzer = createDefaultAnalyzer(false);

                Analyzer artistAnalyzer = createDefaultAnalyzer(true);
                Analyzer artistReadingAnalyzer = createArtistReadingAnalyzer();
                Analyzer exceptionalAnalyzer = createExAnalyzer();
                Map<String, Analyzer> fieldAnalyzers = LegacyMap.of(FieldNamesConstants.ARTIST, artistAnalyzer,
                        FieldNamesConstants.COMPOSER, artistAnalyzer, //
                        FieldNamesConstants.ARTIST_READING, artistReadingAnalyzer, //
                        FieldNamesConstants.COMPOSER_READING, artistReadingAnalyzer, //
                        FieldNamesConstants.ALBUM_EX, exceptionalAnalyzer, //
                        FieldNamesConstants.TITLE_EX, exceptionalAnalyzer, //
                        FieldNamesConstants.ARTIST_EX, exceptionalAnalyzer, //
                        FieldNamesConstants.GENRE_KEY, createGenreKeyAnalyzer(), //
                        FieldNamesConstants.GENRE, createGenreAnalyzer());

                this.analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzers);

            } catch (IOException e) {
                // Usually unreachable due to classpath resources
                throw new IllegalArgumentException("Error when initializing Analyzer.", e);
            }
        }
        return analyzer;
    }

    /*
     * Currently no different from analyzer
     */
    public Analyzer getQueryAnalyzer() {
        if (isEmpty(analyzer)) {
            analyzer = getAnalyzer();
        }
        return analyzer;
    }
}
