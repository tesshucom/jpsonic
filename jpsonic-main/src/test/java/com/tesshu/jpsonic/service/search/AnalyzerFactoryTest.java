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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test case for Analyzer. Lucene versions are very different between Subsonic and Airsonic/Jpsonic. There are some
 * differences in the meta processing specifications, but the specifications have been resolved at a level where
 * problems are unlikely to occur in daily use.
 */
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert" })
class AnalyzerFactoryTest {

    private AnalyzerFactory analyzerFactory;

    @BeforeEach
    public void setup() throws ExecutionException {
        analyzerFactory = new AnalyzerFactory();
    }

    @AfterEach
    void testsGetAnalyzer() {
        analyzerFactory.getAnalyzer().close();
    }

    @Test
    @SuppressWarnings("PMD.CloseResource") // @AfterEach
    void testGetAnalyzer() {
        Analyzer analyzer = analyzerFactory.getAnalyzer();
        assertNotNull(analyzer);
    }

    /**
     * Jpsonic's Analyzer has a different configuration than the default Analyzer (for English-speaking countries) and
     * is configured to handle Japanese and English well. Since it is barren to cover all cases, here we list cases
     * where results are likely to vary due to past version upgrades, Subsonic derived issues, and differences from the
     * default analyzer.
     */
    @Nested
    class TokenStreamTest {

        @Test
        void helloWorld() {
            String queryEng = "The quick brown fox jumps over the lazy dog.";
            var tokenized = Arrays.asList("quick", "brown", "fox", "jumps", "over", "lazy", "dog");
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST, queryEng));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ALBUM, queryEng));
            assertEquals(tokenized, toTermString(FieldNamesConstants.TITLE, queryEng));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST_READING, queryEng));
            assertEquals(tokenized, toTermString(FieldNamesConstants.COMPOSER_READING, queryEng));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST_READING, queryEng));
            assertTrue(toTermString(FieldNamesConstants.ALBUM_READING, queryEng).isEmpty());
            assertTrue(toTermString(FieldNamesConstants.TITLE_READING, queryEng).isEmpty());

            String queryHira = "くいっくぶらうん";
            var bigramHira = Arrays.asList("くい", "いっ", "っく", "くぶ", "ぶら", "らう", "うん");
            assertEquals(bigramHira, toTermString(FieldNamesConstants.ARTIST_READING, queryHira));
            assertEquals(bigramHira, toTermString(FieldNamesConstants.ALBUM_READING, queryHira));
            assertEquals(bigramHira, toTermString(FieldNamesConstants.TITLE_READING, queryHira));

            String queryEngAndHira = "quick　ぶらうん";
            tokenized = Arrays.asList("quick", "ぶら", "うん");
            assertEquals(tokenized, toTermString(FieldNamesConstants.TITLE, queryEngAndHira));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ALBUM, queryEngAndHira));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST, queryEngAndHira));
            assertEquals(Arrays.asList("quick", "ぶら", "らう", "うん"),
                    toTermString(FieldNamesConstants.ARTIST_READING, queryEngAndHira));
            /*
             * In the case of Japanese-English hybrid, the accuracy drops a little in ALBUM and TITLE. (Depending on the
             * end position of the entered character) This issue is a trade-off with index size. If READING for this
             * field is later supported, it will resolve itself.
             */
            assertTrue(toTermString(FieldNamesConstants.ALBUM_READING, queryEngAndHira).isEmpty());
            assertTrue(toTermString(FieldNamesConstants.TITLE_READING, queryEngAndHira).isEmpty());

            String queryStopsOnly = "La La La"; // Currently omitted as rare-case
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING, queryStopsOnly).isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ALBUM_READING, queryStopsOnly).isEmpty());
            assertTrue(toTermString(FieldNamesConstants.TITLE_READING, queryStopsOnly).isEmpty());

            var bigramRoman = Arrays.asList("qui", "quic", "quick", "bro", "brow", "brown", "fox");
            String queryRoman = "The quick brown fox";
            assertEquals(bigramRoman, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, queryRoman));
            assertEquals(bigramRoman, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, queryRoman));
        }

        /**
         * In addition to the common delimiters, there are many delimiters.
         */
        @Test
        void testPunctuation1() {

            String query = "BBB︴CCC";

            var tokenized = Arrays.asList("bbb", "ccc");

            // Remains legacy specs. (It is not necessary to delimit this field originally.)
            assertEquals(tokenized, toTermString(FieldNamesConstants.MEDIA_TYPE, query));

            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ALBUM, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.TITLE, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.COMPOSER, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.FOLDER, query));

            assertTrue(toTermString(FieldNamesConstants.ALBUM_READING, query).isEmpty());
            assertTrue(toTermString(FieldNamesConstants.TITLE_READING, query).isEmpty());

            var stem = Arrays.asList("bbbccc");
            assertEquals(stem, toTermString(FieldNamesConstants.ARTIST_READING, query));
            assertEquals(stem, toTermString(FieldNamesConstants.COMPOSER_READING, query));

            var bigram = Arrays.asList("bbb", "bbbc", "bbbcc", "bbbccc");
            assertEquals(bigram, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, query));
            assertEquals(bigram, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, query));

            var noChange = Arrays.asList("BBB︴CCC");
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE, query));
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, query));
        }

        /**
         * Many symbols are treated as delimiters.
         */
        @Test
        void testPunctuation2() {

            String query = "{'“『【【】】[︴○◎@ $〒→+]";
            Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
                List<String> terms = toTermString(n, query);
                switch (n) {
                case FieldNamesConstants.FOLDER:
                case FieldNamesConstants.GENRE:
                case FieldNamesConstants.GENRE_KEY:
                    assertEquals(1, terms.size(), "remain : " + n);
                    break;

                default:
                    assertEquals(0, terms.size(), "removed : " + n);
                    break;
                }
            });
        }

        /**
         * Japanese has a lot of Stopwards.
         */
        @Test
        void testStopward() {

            /*
             * article
             */
            String queryArticle = "a an the";

            var none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.MEDIA_TYPE, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.FOLDER, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.TITLE, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, queryArticle));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, queryArticle));

            var noChange = Arrays.asList(queryArticle);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, queryArticle));
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE, queryArticle));

            /*
             * It's not included in the Java default stopword. It's set as Airsonic index stopword.
             */
            String queryIndexArticle = "el la las le les";

            none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.MEDIA_TYPE, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.FOLDER, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.TITLE, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, queryIndexArticle));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, queryIndexArticle));

            noChange = Arrays.asList(queryIndexArticle);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, queryIndexArticle));
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE, queryIndexArticle));

            /*
             * Non-article in the default Stopward. In cases, it may be used for song names of 2 to 3 words. Stop words
             * are essential for newspapers and documents, but they are over-processed for song titles.
             */
            String queryNoStop = "and are as at be but by for if in into is it no not of on " //
                    + "or such that their then there these they this to was will";
            var noStop = Arrays.asList(queryNoStop.split(" "));
            assertEquals(noStop, toTermString(FieldNamesConstants.ARTIST, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.MEDIA_TYPE, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.FOLDER, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.ALBUM, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.TITLE, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.ARTIST, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.ARTIST_READING, queryNoStop));
            assertEquals(noStop, toTermString(FieldNamesConstants.COMPOSER_READING, queryNoStop));

            var ngramNoStop = Arrays.asList("and", "are", "but", "for", "int", "into", "not", "suc", "such", "tha",
                    "that", "the", "thei", "their", "the", "then", "the", "ther", "there", "the", "thes", "these",
                    "the", "they", "thi", "this", "was", "wil", "will");
            assertEquals(ngramNoStop, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, queryNoStop));
            assertEquals(ngramNoStop, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, queryNoStop));

            noChange = Arrays.asList(queryNoStop);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, queryNoStop));
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE, queryNoStop));

            /*
             * In newspapers and documents, it is often defined as a stop word, but in jpsonic it is not. However, in
             * the artist field, it is defined as a stopward that represents featuring.
             */
            String queryLargelyNoStop = "with";

            // Stopward for artist
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, queryLargelyNoStop));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING, queryLargelyNoStop));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING, queryLargelyNoStop));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, queryLargelyNoStop));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, queryLargelyNoStop));

            // Should not be a Stopword
            var largelyNoStop = Arrays.asList("with");
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.MEDIA_TYPE, queryLargelyNoStop));
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.FOLDER, queryLargelyNoStop));
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.ALBUM, queryLargelyNoStop));
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.TITLE, queryLargelyNoStop));
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.GENRE_KEY, queryLargelyNoStop));
            assertEquals(largelyNoStop, toTermString(FieldNamesConstants.GENRE, queryLargelyNoStop));

            /*
             * Typical Japanese Stopward. These measures were necessary when using subsonic/airsonic multi-term search
             * to avoid false searches. In the case of phrase search, it is better not to delete it. (Conversely, search
             * omission occurs due to excessive deletion)
             */
            String queryJpStop = "の に は を た が で て と し れ さ ある いる も する から な こと として い " //
                    + "や れる など なっ ない この ため その あっ よう また もの あり まで " //
                    + "られ なる へ か だ これ によって により おり より による ず なり られる において " //
                    + "ば なかっ なく しかし について せ だっ できる それ う ので なお のみ でき き " //
                    + "つ における および いう さらに でも ら たり たち ます ん なら";

            var noChange1 = Arrays.asList(queryJpStop.split(" "));
            assertEquals(noChange1, toTermString(FieldNamesConstants.TITLE, queryJpStop));
            assertEquals(noChange1, toTermString(FieldNamesConstants.FOLDER, queryJpStop));
            assertEquals(noChange1, toTermString(FieldNamesConstants.ALBUM, queryJpStop));
            assertEquals(noChange1, toTermString(FieldNamesConstants.ARTIST, queryJpStop));
            assertEquals(noChange1, toTermString(FieldNamesConstants.MEDIA_TYPE, queryJpStop));

            var noChange2 = Arrays.asList(queryJpStop);
            assertEquals(noChange2, toTermString(FieldNamesConstants.GENRE_KEY, queryJpStop));
            assertEquals(noChange2, toTermString(FieldNamesConstants.GENRE, queryJpStop));

            var noChange3 = Arrays.asList("の", "に", "は", "を", "た", "が", "で", "て", "と", "し", "れ", "さ", "ある", "いる", "も",
                    "する", "から", "な", "こと", "とし", "して", "い", "や", "れる", "など", "なっ", "ない", "この", "ため", "その", "あっ", "よう",
                    "また", "もの", "あり", "まで", "られ", "なる", "へ", "か", "だ", "これ", "によ", "よっ", "って", "によ", "より", "おり", "より",
                    "によ", "よる", "ず", "なり", "られ", "れる", "にお", "おい", "いて", "ば", "なか", "かっ", "なく", "しか", "かし", "につ", "つい",
                    "いて", "せ", "だっ", "でき", "きる", "それ", "う", "ので", "なお", "のみ", "でき", "き", "つ", "にお", "おけ", "ける", "およ",
                    "よび", "いう", "さら", "らに", "でも", "ら", "たり", "たち", "ます", "ん", "なら");
            assertEquals(noChange3, toTermString(FieldNamesConstants.ARTIST_READING, queryJpStop));
            assertEquals(noChange3, toTermString(FieldNamesConstants.COMPOSER_READING, queryJpStop));

        }

        /**
         * The Artist field has a special Stopward. When Jpsonic reads multi-artist format data, an index is created for
         * each individual artist. Names connected by "feat" or "with" are treated as one artist in total.
         */
        @Test
        void testArtistStopward() {
            assertTrue(toTermString(FieldNamesConstants.ARTIST, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST, "with").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER, "with").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING, "with").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING, "with").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, "with").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, "CV").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, "feat").isEmpty());
            assertTrue(toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, "with").isEmpty());
        }

        /**
         * Simple test on FullWidth.
         */
        @Test
        void testFullWidth() {
            String query = "ＦＵＬＬ－ＷＩＤＴＨ";
            var tokenized = Arrays.asList("full", "width");
            assertEquals(tokenized, toTermString(query));
        }

        /**
         * Combined case of Stop and full-width.
         */
        @Test
        void testStopwardAndFullWidth() {

            /*
             * Stopwords are deleted in both full-width and half-width. Legacy servers had problems around here.
             */
            String queryHalfWidth = "THIS IS FULL-WIDTH SENTENCES.";
            String queryFullWidth = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
            var tokenized = Arrays.asList("this", "is", "full", "width", "sentences");
            assertEquals(tokenized, toTermString(queryHalfWidth));
            assertEquals(tokenized, toTermString(queryFullWidth));
        }

        /**
         * Tests on ligature and diacritical marks. In UAX#29, determination of non-practical word boundaries is not
         * considered. Languages ​​that use special strings require "practical word" sample. Unit testing with only
         * ligature and diacritical marks is not possible.
         */
        @Test
        void testASCIIFoldingStop() {

            String query = "Cæsarシーザー";

            // Since it is a field for completion, the index is not created unless it is a special case.
            var none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.TITLE_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM_READING, query));

            /*
             * Key field. This string can be used to search for records in the database. In Jpsonic, multi-genre is
             * reproduced on Lucene. Searches for individual genres use database records via this key
             */
            var noChange = Arrays.asList("Cæsarシーザー");
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, query));

            // Treat as one word with normalization
            var normalized = Arrays.asList("Caesarシーザー");
            assertEquals(normalized, toTermString(FieldNamesConstants.GENRE, query));

            // Fields with simple word splits
            var tokenized = Arrays.asList("caesar", "シーザー");
            assertEquals(tokenized, toTermString(FieldNamesConstants.FOLDER, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.MEDIA_TYPE, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ALBUM, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.TITLE, query));

            /*
             * Fields where bigram is used. Especially in this case it contains Long vowels. In the case of Japanese,
             * processing is insufficient in morphological analysis to support voice input.
             */
            var bigram = Arrays.asList("caesar", "しい", "いざ", "ざあ");
            assertEquals(bigram, toTermString(FieldNamesConstants.ARTIST_READING, query));
        }

        /**
         * This applies to everything except FOLDER, GENRE and MEDIA_TYPE.
         */
        @Test
        void testJapanesePartOfSpeechStop() {

            String query = "{'“『【【】】[○◎@ $〒→+]";
            String genreExpected = "{'\"『【【】】[○◎@ $〒→+]";

            Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
                List<String> terms = toTermString(n, query);
                switch (n) {
                // Do nothing
                case FieldNamesConstants.FOLDER:
                case FieldNamesConstants.GENRE_KEY:
                case FieldNamesConstants.MEDIA_TYPE:
                    assertEquals(1, terms.size(), "through : " + n);
                    assertEquals(query, terms.get(0), "through : " + n);
                    break;
                case FieldNamesConstants.GENRE:
                    // Some character strings are replaced within the range that does not affect display
                    assertEquals(1, terms.size(), "through : " + n);
                    assertEquals(genreExpected, terms.get(0), "apply : " + n);
                    break;
                default:
                    // Strings not relevant to the search are removed
                    assertEquals(0, terms.size(), "apply : " + n);
                    break;
                }
            });
        }

        /**
         * Related Half-width . This applies to everything except FOLDER, GENRE, MEDIA_TYPE. Affects search accuracy
         * when Japanese Half-width letters are included.
         */
        @Test
        void testCJKWidth() {

            String query = "ＡＢＣａｂｃｱｲｳ";

            var noChange = Arrays.asList(query);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, query));

            var normalized = Arrays.asList("ABCabcアイウ");
            assertEquals(normalized, toTermString(FieldNamesConstants.GENRE, query));

            var normalizedToken = Arrays.asList("abcabc", "アイウ");
            assertEquals(normalizedToken, toTermString(FieldNamesConstants.FOLDER, query));
            assertEquals(normalizedToken, toTermString(FieldNamesConstants.MEDIA_TYPE, query));
            assertEquals(normalizedToken, toTermString(FieldNamesConstants.ARTIST, query));
            assertEquals(normalizedToken, toTermString(FieldNamesConstants.ALBUM, query));
            assertEquals(normalizedToken, toTermString(FieldNamesConstants.TITLE, query));

            var normalizedBigram = Arrays.asList("abcabc", "あい", "いう");
            assertEquals(normalizedBigram, toTermString(FieldNamesConstants.ARTIST_READING, query));

            // Since it is a field for completion, the index is not created unless it is a special case.
            var none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.TITLE_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM_READING, query));

            // Cases where complementary fields work
            String queryLowerHalfHiraganaOnly = "ぁぃぅ";
            var lowerHalfHiraganaBigram = Arrays.asList("ぁぃ", "ぃぅ");
            assertEquals(lowerHalfHiraganaBigram,
                    toTermString(FieldNamesConstants.ARTIST_READING, queryLowerHalfHiraganaOnly));
            assertEquals(lowerHalfHiraganaBigram,
                    toTermString(FieldNamesConstants.TITLE_READING, queryLowerHalfHiraganaOnly));
            assertEquals(lowerHalfHiraganaBigram,
                    toTermString(FieldNamesConstants.ALBUM_READING, queryLowerHalfHiraganaOnly));
        }

        @Test
        void testLowerCase() {

            String query = "ABCDEFGふ";

            // Since it is a field for completion, the index is not created unless it is a special case.
            var none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.TITLE_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM_READING, query));

            var noChange = Arrays.asList(query);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE, query));

            var tokenized = Arrays.asList("abcdefg", "ふ");
            assertEquals(tokenized, toTermString(FieldNamesConstants.FOLDER, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.MEDIA_TYPE, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST_READING, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ARTIST, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.ALBUM, query));
            assertEquals(tokenized, toTermString(FieldNamesConstants.TITLE, query));
        }

        /**
         * Detailed tests on EscapeRequires. The reserved string is discarded unless it is purposely Escape. This is
         * fine as a search specification(if it is considered as a kind of reserved stop word). However, in the case of
         * file path, it may be a problem.
         */
        @Test
        void testLuceneEscapeRequires() {

            String query = "+-&&||!(){}[]^\"~*?:\\/";

            var noChange = Arrays.asList(query);
            assertEquals(noChange, toTermString(FieldNamesConstants.GENRE_KEY, query));

            // Special processing is applied to parentheses in the Genre field
            var domainValue = Arrays.asList("+-&&||! { }[ ]^\"~*?:\\/");
            assertEquals(domainValue, toTermString(FieldNamesConstants.GENRE, query));

            var none = Collections.emptyList();
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM, query));
            assertEquals(none, toTermString(FieldNamesConstants.ALBUM_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST, query));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.ARTIST_READING_ROMANIZED, query));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER, query));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.COMPOSER_READING_ROMANIZED, query));
            assertEquals(none, toTermString(FieldNamesConstants.FOLDER, query));
            assertEquals(none, toTermString(FieldNamesConstants.FOLDER_ID, query));
            assertEquals(none, toTermString(FieldNamesConstants.ID, query));
            assertEquals(none, toTermString(FieldNamesConstants.MEDIA_TYPE, query));
            assertEquals(none, toTermString(FieldNamesConstants.TITLE, query));
            assertEquals(none, toTermString(FieldNamesConstants.TITLE_READING, query));
            assertEquals(none, toTermString(FieldNamesConstants.YEAR, query));
        }

        /**
         * Create an example that makes UAX 29 differences easy to understand. The delimiter position changes depending
         * on the version of Lucene (before and after supporting UAX). Also, in Japanese Analyzer, the before and after
         * of the numbers are separated. Jpsonic treats underscores as delimiters.(Same as Subsonic)
         */
        @Test
        void testUax29() {

            /*
             * Case using test resource name
             */

            // Semicolon, comma and hyphen.
            String query = "Bach: Goldberg Variations, BWV 988 - Aria";
            var terms = Arrays.asList("bach", "goldberg", "variations", "bwv", "988", "aria");
            assertEquals(terms, toTermString(query));

            // Underscores around words, ascii and semicolon.
            query = "_ID3_ARTIST_ Céline Frisch: Café Zimmermann";
            terms = Arrays.asList("id", "3", "artist", "celine", "frisch", "cafe", "zimmermann");
            assertEquals(terms, toTermString(query));

            // Underscores around words and slashes.
            query = "_ID3_ARTIST_ Sarah Walker/Nash Ensemble";
            terms = Arrays.asList("id", "3", "artist", "sarah", "walker", "nash", "ensemble");
            assertEquals(terms, toTermString(query));

            // Space
            query = " ABC DEF ";
            terms = Arrays.asList("abc", "def");
            assertEquals(terms, toTermString(query));
            query = " ABC1 DEF ";
            terms = Arrays.asList("abc", "1", "def");
            assertEquals(terms, toTermString(query));

            // Delimiter and words
            terms = Arrays.asList("abc", "def");
            assertEquals(terms, toTermString("+ABC+DEF+"));
            assertEquals(terms, toTermString("|ABC|DEF|"));
            assertEquals(terms, toTermString("!ABC!DEF!"));
            assertEquals(terms, toTermString("(ABC(DEF("));
            assertEquals(terms, toTermString(")ABC)DEF)"));
            assertEquals(terms, toTermString("{ABC{DEF{"));
            assertEquals(terms, toTermString("}ABC}DEF}"));
            assertEquals(terms, toTermString("[ABC[DEF["));
            assertEquals(terms, toTermString("]ABC]DEF]"));
            assertEquals(terms, toTermString("^ABC^DEF^"));
            assertEquals(terms, toTermString("\\ABC\\DEF\\"));
            assertEquals(terms, toTermString("\"ABC\"DEF\""));
            assertEquals(terms, toTermString("~ABC~DEF~"));
            assertEquals(terms, toTermString("*ABC*DEF*"));
            assertEquals(terms, toTermString("?ABC?DEF?"));
            assertEquals(terms, toTermString(":ABC:DEF:"));
            assertEquals(terms, toTermString("-ABC-DEF-"));
            assertEquals(terms, toTermString("/ABC/DEF/"));
            assertEquals(terms, toTermString("_ABC_DEF_"));
            assertEquals(terms, toTermString(",ABC,DEF,"));
            assertEquals(terms, toTermString(".ABC.DEF."));
            assertEquals(terms, toTermString("&ABC&DEF&"));
            assertEquals(terms, toTermString("@ABC@DEF@"));
            assertEquals(terms, toTermString("'ABC'DEF'"));

            // Delimiter, words and number
            terms = Arrays.asList("abc", "1", "def");
            assertEquals(terms, toTermString("+ABC1+DEF+"));
            assertEquals(terms, toTermString("|ABC1|DEF|"));
            assertEquals(terms, toTermString("!ABC1!DEF!"));
            assertEquals(terms, toTermString("(ABC1(DEF("));
            assertEquals(terms, toTermString(")ABC1)DEF)"));
            assertEquals(terms, toTermString("{ABC1{DEF{"));
            assertEquals(terms, toTermString("}ABC1}DEF}"));
            assertEquals(terms, toTermString("[ABC1[DEF["));
            assertEquals(terms, toTermString("]ABC1]DEF]"));
            assertEquals(terms, toTermString("^ABC1^DEF^"));
            assertEquals(terms, toTermString("\\ABC1\\DEF\\"));
            assertEquals(terms, toTermString("\"ABC1\"DEF\""));
            assertEquals(terms, toTermString("~ABC1~DEF~"));
            assertEquals(terms, toTermString("*ABC1*DEF*"));
            assertEquals(terms, toTermString("?ABC1?DEF?"));
            assertEquals(terms, toTermString(":ABC1:DEF:"));
            assertEquals(terms, toTermString(",ABC1,DEF,"));
            assertEquals(terms, toTermString("-ABC1-DEF-"));
            assertEquals(terms, toTermString("/ABC1/DEF/"));
            assertEquals(terms, toTermString("_ABC1_DEF_"));
            assertEquals(terms, toTermString(".ABC1.DEF."));
            assertEquals(terms, toTermString("&ABC1&DEF&"));
            assertEquals(terms, toTermString("@ABC1@DEF@"));
            assertEquals(terms, toTermString("'ABC1'DEF'"));
        }

        /**
         * Special handling of single quotes.
         */
        @Test
        void testSingleQuotes() {

            /*
             * A somewhat cultural that seems to be related to a specific language. The use cases where differences are
             * likely to occur depending on the Analyzer used by the product.
             */

            // issues#290
            // That is, the possessive case is deleted
            String query = "This is Jpsonic's analysis.";
            var terms = Arrays.asList("this", "is", "jpsonic", "analysis");
            assertEquals(terms, toTermString(query));

            // Other than that, there is no problem
            query = "We’ve been here before.";
            terms = Arrays.asList("we", "ve", "been", "here", "before");
            assertEquals(terms, toTermString(query));

            query = "LʼHomme";
            terms = Arrays.asList("l", "homme");
            assertEquals(terms, toTermString(query));

            query = "aujourd'hui";
            terms = Arrays.asList("aujourd", "hui");
            assertEquals(terms, toTermString(query));

            query = "fo'c'sle";
            terms = Arrays.asList("fo", "c", "sle");
            assertEquals(terms, toTermString(query));
        }

        /**
         * Present tense filters should not be used
         */
        @Test
        void testPastParticiple() {
            // Confirming no conversion to present tense.
            // Also note that common stopwords have not been removed.
            String query = "This is formed with a form of the verb \"have\" and a past participl.";
            var terms = Arrays.asList("this", "is", "formed", "with", "form", "of", "verb", "have", "and", "past",
                    "participl");
            assertEquals(terms, toTermString(query));
        }

        /**
         * Filters that convert the plural to the singular should not be used.
         */
        @Test
        void testNumeral() {
            // Confirming no conversion to singular.
            String query = "books boxes cities leaves men glasses";
            var terms = Arrays.asList("books", "boxes", "cities", "leaves", "men", "glasses");
            assertEquals(terms, toTermString(query));
        }

        /**
         * Katakana is converted to hiragana. This is because it is a pattern that was often seen in CDDB before. I
         * don't know which one now.
         */
        @Test
        void testToHiragana() {
            String query = "THE BLUE HEARTS";
            var terms = Arrays.asList("blue", "hearts");
            assertEquals(terms, toTermString(FieldNamesConstants.ARTIST_READING, query));

            query = "ABC123";
            terms = Arrays.asList("abc123");
            assertEquals(terms, toTermString(FieldNamesConstants.ARTIST_READING, query));

            terms = Arrays.asList("abc123", "あい", "いう");
            assertEquals(terms, toTermString(FieldNamesConstants.ARTIST_READING, "ABC123あいう"));
            assertEquals(terms, toTermString(FieldNamesConstants.ARTIST_READING, "ABC123あいう"));
            assertEquals(terms, toTermString(FieldNamesConstants.ARTIST_READING, "ABC123アイウ"));
        }

        private List<String> toTermString(String str) {
            return toTermString(null, str);
        }

        private List<String> toTermString(String field, String str) {
            List<String> result = new ArrayList<>();
            try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field, new StringReader(str))) {
                stream.reset();
                while (stream.incrementToken()) {
                    result.add(stream.getAttribute(CharTermAttribute.class).toString().replaceAll("^term\\=", ""));
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(AnalyzerFactoryTest.class).error("Error during Token processing.", e);
            }
            return result;
        }

        @SuppressWarnings("unused")
        private List<String> toQueryTermString(String field, String str) {
            List<String> result = new ArrayList<>();
            try (TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field, new StringReader(str))) {
                stream.reset();
                while (stream.incrementToken()) {
                    result.add(stream.getAttribute(CharTermAttribute.class).toString().replaceAll("^term\\=", ""));
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(AnalyzerFactoryTest.class).error("Error during Token processing.", e);
            }
            return result;
        }
    }
}
