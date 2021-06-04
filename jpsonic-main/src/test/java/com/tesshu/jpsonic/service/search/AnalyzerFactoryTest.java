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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.service.SettingsService;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test case for Analyzer. These cases have the purpose of observing the current situation and observing the impact of
 * upgrading Lucene.
 */
@SpringBootTest
@ExtendWith(NeedsHome.class)
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert" })
/*
 * [AvoidDuplicateLiterals] In the testing class, it may be less readable. [JUnitTestsShouldIncludeAssert] dalse
 * positive
 */
public class AnalyzerFactoryTest {

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    public void setup() throws ExecutionException {
        Method setSearchMethodLegacy;
        try {
            setSearchMethodLegacy = analyzerFactory.getClass().getDeclaredMethod("setSearchMethodLegacy",
                    boolean.class);
            setSearchMethodLegacy.setAccessible(true);
            setSearchMethodLegacy.invoke(analyzerFactory, false);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new ExecutionException(e);
        }
        settingsService.setSearchMethodLegacy(false);
    }

    @Test
    public void testTokenCounts() {

        String queryEng = "The quick brown fox jumps over the lazy dog.";

        // no analyze field
        /*
         * FieldNamesConstants.ID, FieldNamesConstants.FOLDER_ID, FieldNamesConstants.YEAR FieldNamesConstants.GENRE,
         * FieldNamesConstants.GENRE_KEY, FieldNamesConstants.MEDIA_TYPE, FieldNamesConstants.FOLDER
         */

        // 4
        String[] multiTokenFields = { FieldNamesConstants.ARTIST, FieldNamesConstants.ARTIST_READING,
                FieldNamesConstants.ALBUM, FieldNamesConstants.TITLE };
        Arrays.stream(multiTokenFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals(7, terms.size(), "multiToken : " + n);
        });

        // 2
        String queryHira = "くいっくぶらうん";
        String[] oneTokenHiraStopOnlyFields = { FieldNamesConstants.ALBUM_EX, FieldNamesConstants.TITLE_EX };
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals(0, terms.size(), "oneTokenHira(Eng) : " + n);
        });
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            // Because different from legacy, which is a prefix match of tokens, and bigram is used.
            assertEquals(7, terms.size(), "bigram(Hira) : " + n);
        });
        String queryStopsOnly = "La La La";
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStopsOnly);
            // Because the Stopward-only case is currently omitted as rare-case
            assertEquals(0, terms.size(), "Cut off : " + n);
        });

        // 1
        String[] oneTokenStopOnlyFields = { FieldNamesConstants.ARTIST_EX };
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals(0, terms.size(), "oneTokenHira(Eng) : " + n);
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            // Because different from legacy, which is a prefix match of tokens, and bigram is used.
            assertEquals(7, terms.size(), "bigram(Hira) : " + n);
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStopsOnly);
            // Because the Stopward-only case is currently omitted as rare-case
            assertEquals(0, terms.size(), "Cut off :" + n);
        });

    }

    /**
     * Detailed tests on Punctuation. In addition to the common delimiters, there are many delimiters.
     */
    @Test
    public void testPunctuation1() {

        String query = "B︴C";
        String expected1 = "b";
        String expected2 = "c";
        String expected3 = "bc";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {

            /*
             * In the legacy, these field divide input into 2. It is not necessary to delimit this field originally.
             */
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals(2, terms.size(), "tokenized : " + n);
                assertEquals(expected1, terms.get(0), "tokenized : " + n);
                assertEquals(expected2, terms.get(1), "tokenized : " + n);
                break;

            /*
             * What should the fields of this be? Generally discarded.
             */
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
            case FieldNamesConstants.COMPOSER:
                assertEquals(2, terms.size(), "tokenized : " + n);
                assertEquals(expected1, terms.get(0), "tokenized : " + n);
                assertEquals(expected2, terms.get(1), "tokenized : " + n);
                break;

            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.ARTIST_EX:
                assertEquals(0, terms.size(), "tokenized : " + n);
                break;

            case FieldNamesConstants.ARTIST_READING:
            case FieldNamesConstants.COMPOSER_READING:
                assertEquals(1, terms.size(), "tokenized : " + n);
                assertEquals(expected3, terms.get(0), "tokenized : " + n);
                break;

            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals(1, terms.size(), "tokenized : " + n);
                assertEquals(query, terms.get(0), "tokenized : " + n);
                break;

            /*
             * ID, FOLDER_ID, YEAR This is not a problem because the input value does not contain a delimiter.
             */
            default:
                assertEquals(2, terms.size(), "tokenized : " + n);
                break;
            }
        });
    }

    /*
     * Detailed tests on Punctuation. Many of the symbols are delimiters or target to be removed.
     */
    @Test
    public void testPunctuation2() {

        String query = "{'“『【【】】[︴○◎@ $〒→+]";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
            case FieldNamesConstants.MEDIA_TYPE:
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(0, terms.size(), "removed : " + n);
                break;
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
     * Detailed tests on Stopward.
     * 
     * @see org.apache.lucene.analysis.StopAnalyzer#ENGLISH_STOP_WORDS_SET
     */
    @Test
    public void testStopward() {

        /*
         * article
         */
        String queryArticle = "a an the";

        /*
         * It is not included in the Java default stopword. Default set as Airsonic index stop word.
         */
        String queryIndexArticle = "el la las le les";

        /*
         * Non-article in the default Stopward. In cases, it may be used for song names of 2 to 3 words. Stop words are
         * essential for newspapers and documents, but they are over-processed for song titles.
         */
        String queryNoStop = "and are as at be but by for if in into is it no not of on " //
                + "or such that their then there these they this to was will with";

        /*
         * Japanese Stopward. Like the English stopward, there may be cases of over-deletion in some cases. However,
         * unlike the English stopward. Japanese Stopward is placed less frequently at the beginning of sentences.
         */
        String queryJpStop = "の に は を た が で て と し れ さ ある いる も する から な こと として い " //
                + "や れる など なっ ない この ため その あっ よう また もの という あり まで " //
                + "られ なる へ か だ これ によって により おり より による ず なり られる において " //
                + "ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き " //
                + "つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して " //
                + "特に せる 及び これら とき では にて ほか ながら うち そして とともに ただし かつて " //
                + "それぞれ または お ほど ものの に対する ほとんど と共に といった です とも" //
                + " ところ ここ"; //

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> articleTerms = toTermString(n, queryArticle);
            List<String> indexArticleTerms = toTermString(n, queryIndexArticle);
            List<String> noStopTerms = toTermString(n, queryNoStop);
            List<String> jpStopTerms = toTermString(n, queryJpStop);
            switch (n) {
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.MEDIA_TYPE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals(1, articleTerms.size(), "through : " + n);
                assertEquals(queryArticle, articleTerms.get(0), "through : " + n);
                assertEquals(1, indexArticleTerms.size(), "through : " + n);
                assertEquals(queryIndexArticle, indexArticleTerms.get(0), "through : " + n);
                assertEquals(1, noStopTerms.size(), "through : " + n);
                assertEquals(queryNoStop, noStopTerms.get(0), "through : " + n);
                assertEquals(1, jpStopTerms.size(), "through : " + n);
                assertEquals(queryJpStop, jpStopTerms.get(0), "through : " + n);
                break;
            case FieldNamesConstants.GENRE:
                assertEquals(1, articleTerms.size(), "through : " + n);
                assertEquals(queryArticle, articleTerms.get(0), "through : " + n);
                assertEquals(1, indexArticleTerms.size(), "through : " + n);
                assertEquals(queryIndexArticle, indexArticleTerms.get(0), "through : " + n);
                assertEquals(1, noStopTerms.size(), "through : " + n);
                assertEquals(queryNoStop, noStopTerms.get(0), "through : " + n);
                // false positives?
                assertEquals(2, jpStopTerms.size(), "??????? : " + n);
                assertEquals(
                        "の に は を た が で て と し れ さ ある いる も する から な こと として い や れる など なっ ない この ため その あっ よう また もの という あり まで られ なる へ か だ これ によって により おり より による ず なり られる において ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して 特に せる 及び これら",
                        jpStopTerms.get(0), "??????? : " + n);
                assertEquals(" とき では にて ほか ながら うち そして とともに ただし かつて それぞれ または お ほど ものの に対する ほとんど と共に といった です とも ところ ここ",
                        jpStopTerms.get(1), "??????? : " + n);
                break;
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(0, articleTerms.size(), "apply : " + n);
                assertEquals(0, indexArticleTerms.size(), "apply : " + n);
                assertEquals(30, noStopTerms.size(), "through : " + n);
                assertEquals(110, jpStopTerms.size(), "apply : " + n); // XXX Legacy(0) -> Phrase(110)
                break;
            case FieldNamesConstants.ARTIST:
                assertEquals(0, articleTerms.size(), "apply : " + n);
                assertEquals(0, indexArticleTerms.size(), "apply : " + n);
                assertEquals(29, noStopTerms.size(), "through : " + n); // with is removed
                assertEquals(110, jpStopTerms.size(), "apply : " + n); // XXX Legacy(53) -> Phrase(110)
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals(0, articleTerms.size(), "article : " + n);
                assertEquals(0, indexArticleTerms.size(), "indexArticle : " + n);
                assertEquals(29, noStopTerms.size(), "noStop : " + n); // with is removed
                assertEquals(152, jpStopTerms.size(), "jpStop : " + n); // XXX Legacy(109) -> Phrase(152)
                break;

            default:
                break;
            }
        });

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            // to be affected by other filters
            switch (n) {
            case FieldNamesConstants.ARTIST_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.TITLE_EX:
                // #482 No longer used
                break;
            default:
                break;
            }
        });
    }

    /**
     * Detailed tests on Artist Stopward.
     */
    @Test
    public void testArtistStopward() {
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST, "CV").size());
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST, "feat").size());
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST, "with").size());
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST_READING, "CV").size());
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST_READING, "feat").size());
        assertEquals(0, toTermString(FieldNamesConstants.ARTIST_READING, "with").size());
    }

    /**
     * Simple test on FullWidth.
     */
    @Test
    public void testFullWidth() {
        String query = "ＦＵＬＬ－ＷＩＤＴＨ";
        List<String> terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("full", terms.get(0));
        assertEquals("width", terms.get(1));
    }

    /**
     * Combined case of Stop and full-width.
     */
    @Test
    public void testStopwardAndFullWidth() {

        /*
         * Stop word is removed.
         */
        String queryHalfWidth = "THIS IS FULL-WIDTH SENTENCES.";
        List<String> terms = toTermString(queryHalfWidth);
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0)); // removal target is ignored
        assertEquals("is", terms.get(1));
        assertEquals("full", terms.get(2));
        assertEquals("width", terms.get(3));
        assertEquals("sentences", terms.get(4));

        /*
         * Legacy can avoid Stopward if it is full width. It is unclear whether it is a specification or not. (Problems
         * due to a defect in filter application order? or Is it popular in English speaking countries?)
         */
        String queryFullWidth = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
        terms = toTermString(queryFullWidth);
        /*
         * XXX 3.x -> 8.x :
         * 
         * This is not a change due to the library but an intentional change. The filter order has been changed properly
         * as it is probably not a deliberate specification.
         */
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0)); // removal target is ignored
        assertEquals("is", terms.get(1));
        assertEquals("full", terms.get(2));
        assertEquals("width", terms.get(3));
        assertEquals("sentences", terms.get(4));

    }

    /**
     * Tests on ligature and diacritical marks. In UAX#29, determination of non-practical word boundaries is not
     * considered. Languages ​​that use special strings require "practical word" sample. Unit testing with only ligature
     * and diacritical marks is not possible.
     */
    @Test
    public void testASCIIFoldingStop() {

        // Filter operation check only. Verify only some settings.
        String query = "Cæsarシーザー";
        String expected1a = "caesar";
        String expected1b = "シーザー";
        String expected3 = "Caesarシーザー";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.ARTIST_EX:
                assertEquals(0, terms.size(), "no case : " + n);
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE_KEY:
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(query, terms.get(0), "through : " + n);
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals(4, terms.size(), "apply : " + n);
                assertEquals("caesar", terms.get(0), "apply : " + n);
                assertEquals("しい", terms.get(1), "apply : " + n);
                assertEquals("ーざ", terms.get(2), "apply : " + n);
                assertEquals("ざあ", terms.get(3), "apply : " + n);
                break;
            case FieldNamesConstants.GENRE:
                assertEquals(1, terms.size(), "apply : " + n);
                assertEquals(expected3, terms.get(0), "apply : " + n);
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(2, terms.size(), "apply : " + n);
                assertEquals(expected1a, terms.get(0), "apply : " + n);
                assertEquals(expected1b, terms.get(1), "apply : " + n);
                break;
            default:
                break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    @Test
    public void testJapanesePartOfSpeechStop() {

        // Filter operation check only. Verify only some settings.
        String query = "{'“『【【】】[○◎@ $〒→+]";
        String expected1 = "{'\"『【【】】[○◎@ $〒→+]";
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
                assertEquals(expected1, terms.get(0), "apply : " + n);
                break;
            default:
                // Strings not relevant to the search are removed
                assertEquals(0, terms.size(), "apply : " + n);
                break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    @Test
    public void testCJKWidth() {
        String query = "ＡＢＣａｂｃｱｲｳ";
        String apply1 = "ABCabcアイウ";
        String apply1a = "abcabc";
        String apply1b = "アイウ";
        String query2 = "ぁぃぅ";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
            case FieldNamesConstants.ARTIST_EX:
                assertEquals(0, terms.size(), "no case : " + n);
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.MEDIA_TYPE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(query, terms.get(0), "through : " + n);
                break;
            case FieldNamesConstants.GENRE:
                assertEquals(1, terms.size(), "apply : " + n);
                assertEquals(apply1, terms.get(0), "apply : " + n);
                break;
            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
                terms = toTermString(n, query2);
                assertEquals(2, terms.size(), "no case : " + n);
                assertEquals("ぁぃ", terms.get(0), "bigram : " + n);
                assertEquals("ぃぅ", terms.get(1), "bigram : " + n);
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals(3, terms.size(), "apply : " + n);
                assertEquals(apply1a, terms.get(0), "apply : " + n);
                assertEquals("あい", terms.get(1), "apply : " + n);
                assertEquals("いう", terms.get(2), "apply : " + n);
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(2, terms.size(), "apply : " + n);
                assertEquals(apply1a, terms.get(0), "apply : " + n);
                assertEquals(apply1b, terms.get(1), "apply : " + n);
                break;
            default:
                break;
            }
        });
    }

    /**
     * Detailed tests on LowerCase.
     */
    @Test
    public void testLowerCase() {

        // Filter operation check only. Verify only some settings.
        String query = "ABCDEFGふ";
        String expected1a = "abcdefg";
        String expected1b = "ふ";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.ARTIST_EX:
                assertEquals(0, terms.size(), "no case : " + n);
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(query, terms.get(0), "through : " + n);
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals(2, terms.size(), "apply : " + n);
                assertEquals("abcdefg", terms.get(0), "apply : " + n);
                assertEquals("ふ", terms.get(1), "apply : " + n);
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(2, terms.size(), "apply : " + n);
                assertEquals(expected1a, terms.get(0), "apply : " + n);
                assertEquals(expected1b, terms.get(1), "apply : " + n);
                break;
            default:
                break;
            }
        });
    }

    /**
     * Detailed tests on EscapeRequires. The reserved string is discarded unless it is purposely Escape. This is fine as
     * a search specification(if it is considered as a kind of reserved stop word). However, in the case of file path,
     * it may be a problem.
     */
    @Test
    public void testLuceneEscapeRequires() {

        String escapeRequires = "+-&&||!(){}[]^\"~*?:\\/";
        String fileUsable = "+-&&!(){}[]^~";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, escapeRequires);
            // TODO These fields handle escape strings as they are.
            if (FieldNamesConstants.FOLDER.equals(n) || FieldNamesConstants.GENRE_KEY.equals(n)) {
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(escapeRequires, terms.get(0), "through : " + n);
                terms = toTermString(n, fileUsable);
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(fileUsable, terms.get(0), "through : " + n);
            } else if (FieldNamesConstants.GENRE.equals(n)) {
                // XXX @see AnalyzerFactory#addTokenFilterForTokenToDomainValue
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals("+-&&||! { }[ ]^\"~*?:\\/", terms.get(0), "through : " + n);
                terms = toTermString(n, fileUsable);
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals("+-&&! { }[ ]^~", terms.get(0), "through : " + n);
            } else {
                // Strings that require escape for most fields are removed during parsing.
                assertEquals(0, terms.size(), "trancate : " + n);
            }
        });

    }

    /**
     * Create an example that makes UAX 29 differences easy to understand.
     */
    @Test
    public void testUax29() {

        /*
         * Case using test resource name
         */

        // Semicolon, comma and hyphen.
        String query = "Bach: Goldberg Variations, BWV 988 - Aria";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("bach", terms.get(0));
        assertEquals("goldberg", terms.get(1));
        assertEquals("variations", terms.get(2));
        assertEquals("bwv", terms.get(3));
        assertEquals("988", terms.get(4));
        assertEquals("aria", terms.get(5));

        // Underscores around words, ascii and semicolon.
        query = "_ID3_ARTIST_ Céline Frisch: Café Zimmermann";
        terms = toTermString(query);
        assertEquals(7, terms.size());
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("artist", terms.get(2));
        assertEquals("celine", terms.get(3));
        assertEquals("frisch", terms.get(4));
        assertEquals("cafe", terms.get(5));
        assertEquals("zimmermann", terms.get(6));

        // Underscores around words and slashes.
        query = "_ID3_ARTIST_ Sarah Walker/Nash Ensemble";
        terms = toTermString(query);
        assertEquals(7, terms.size());
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("artist", terms.get(2));
        assertEquals("sarah", terms.get(3));
        assertEquals("walker", terms.get(4));
        assertEquals("nash", terms.get(5));
        assertEquals("ensemble", terms.get(6));

        // Space
        assertEquals(asList("abc", "def"), toTermString(" ABC DEF "));
        assertEquals(asList("abc", "1", "def"), toTermString(" ABC1 DEF ")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def

        // trim and delimiter
        assertEquals(asList("abc", "def"), toTermString("+ABC+DEF+"));
        assertEquals(asList("abc", "def"), toTermString("|ABC|DEF|"));
        assertEquals(asList("abc", "def"), toTermString("!ABC!DEF!"));
        assertEquals(asList("abc", "def"), toTermString("(ABC(DEF("));
        assertEquals(asList("abc", "def"), toTermString(")ABC)DEF)"));
        assertEquals(asList("abc", "def"), toTermString("{ABC{DEF{"));
        assertEquals(asList("abc", "def"), toTermString("}ABC}DEF}"));
        assertEquals(asList("abc", "def"), toTermString("[ABC[DEF["));
        assertEquals(asList("abc", "def"), toTermString("]ABC]DEF]"));
        assertEquals(asList("abc", "def"), toTermString("^ABC^DEF^"));
        assertEquals(asList("abc", "def"), toTermString("\\ABC\\DEF\\"));
        assertEquals(asList("abc", "def"), toTermString("\"ABC\"DEF\""));
        assertEquals(asList("abc", "def"), toTermString("~ABC~DEF~"));
        assertEquals(asList("abc", "def"), toTermString("*ABC*DEF*"));
        assertEquals(asList("abc", "def"), toTermString("?ABC?DEF?"));
        assertEquals(asList("abc", "def"), toTermString(":ABC:DEF:")); // XXX 3.x -> 8.x : abc def -> abc:def // XXX
                                                                       // standard -> jp : abc:def -> abc def
        assertEquals(asList("abc", "def"), toTermString("-ABC-DEF-"));
        assertEquals(asList("abc", "def"), toTermString("/ABC/DEF/"));
        /*
         * XXX 3.x -> 8.x : _abc_def_ in UAX#29. Since the effect is large, trim with Filter.
         */
        assertEquals(asList("abc", "def"), toTermString("_ABC_DEF_")); // XXX 3.x -> 8.x : abc def -> abc_def // XXX
                                                                       // standard -> jp : abc_def -> abc def
        assertEquals(asList("abc", "def"), toTermString(",ABC,DEF,"));
        assertEquals(asList("abc", "def"), toTermString(".ABC.DEF.")); // XXX standard -> jp : abc.def -> abc def
        assertEquals(asList("abc", "def"), toTermString("&ABC&DEF&")); // XXX 3.x -> 8.x : abc&def -> abc def
        assertEquals(asList("abc", "def"), toTermString("@ABC@DEF@")); // XXX 3.x -> 8.x : abc@def -> abc def
        assertEquals(asList("abc", "def"), toTermString("'ABC'DEF'")); // XXX standard -> jp : abc'def -> abc def

        // trim and delimiter and number
        assertEquals(asList("abc", "1", "def"), toTermString("+ABC1+DEF+")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("|ABC1|DEF|")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("!ABC1!DEF!")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("(ABC1(DEF(")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString(")ABC1)DEF)")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("{ABC1{DEF{")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("}ABC1}DEF}")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("[ABC1[DEF[")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("]ABC1]DEF]")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("^ABC1^DEF^")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("\\ABC1\\DEF\\")); // XXX standard -> jp : abc1 def -> abc
                                                                                // 1 def
        assertEquals(asList("abc", "1", "def"), toTermString("\"ABC1\"DEF\"")); // XXX standard -> jp : abc1 def -> abc
                                                                                // 1 def
        assertEquals(asList("abc", "1", "def"), toTermString("~ABC1~DEF~")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("*ABC1*DEF*")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("?ABC1?DEF?")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString(":ABC1:DEF:")); // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString(",ABC1,DEF,")); // XXX 3.x -> 8.x : abc1,def -> abc1 def //
                                                                             // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("-ABC1-DEF-")); // XXX 3.x -> 8.x : abc1-def -> abc1 def //
                                                                             // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("/ABC1/DEF/")); // XXX 3.x -> 8.x : abc1/def -> abc1 def //
                                                                             // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        /*
         * XXX 3.x -> 8.x : _abc1_def_ in UAX#29. Since the effect is large, trim with Filter.
         */
        assertEquals(asList("abc", "1", "def"), toTermString("_ABC1_DEF_")); // XXX standard -> jp : abc1_def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString(".ABC1.DEF.")); // XXX 3.x -> 8.x : abc1.def -> abc1 def //
                                                                             // XXX standard -> jp : abc1 def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("&ABC1&DEF&")); // XXX standard -> jp : abc1_def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("@ABC1@DEF@")); // XXX standard -> jp : abc1_def -> abc 1
                                                                             // def
        assertEquals(asList("abc", "1", "def"), toTermString("'ABC1'DEF'")); // XXX standard -> jp : abc1_def -> abc 1
                                                                             // def

    }

    /**
     * Special handling of single quotes.
     */
    @Test
    public void testSingleQuotes() {

        /*
         * A somewhat cultural that seems to be related to a specific language.
         * 
         * XXX Contents are different from Airsonic because the tokenyzer specifications are different.
         */
        String query = "This is Jpsonic's analysis.";
        List<String> terms = toTermString(query);
        assertEquals(4, terms.size());
        assertEquals(terms.get(0), "this");
        assertEquals(terms.get(1), "is");
        assertEquals(terms.get(2), "jpsonic");
        // assertEquals("s", terms.get(3)); issues#290
        assertEquals(terms.get(3), "analysis");

        query = "We’ve been here before.";
        terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals(terms.get(0), "we");
        assertEquals(terms.get(1), "ve");
        assertEquals(terms.get(2), "been");
        assertEquals(terms.get(3), "here");
        assertEquals(terms.get(4), "before");

        query = "LʼHomme";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals(terms.get(0), "l");
        assertEquals(terms.get(1), "homme");

        query = "L'Homme";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals(terms.get(0), "l");
        assertEquals(terms.get(1), "homme");

        query = "aujourd'hui";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals(terms.get(0), "aujourd");
        assertEquals(terms.get(1), "hui");

        query = "fo'c'sle";
        terms = toTermString(query);
        assertEquals(3, terms.size());
        assertEquals(terms.get(0), "fo");
        assertEquals(terms.get(1), "c");
        assertEquals(terms.get(2), "sle");

    }

    /*
     * There is also a filter that converts the tense to correspond to the search by the present tense.
     */
    @Test
    public void testPastParticiple() {

        /*
         * Confirming no conversion to present tense.
         */
        String query = "This is formed with a form of the verb \"have\" and a past participl.";
        List<String> terms = toTermString(query);
        assertEquals(11, terms.size());
        assertEquals(terms.get(0), "this"); // currently not stopward
        assertEquals(terms.get(1), "is"); // currently not stopward
        assertEquals(terms.get(2), "formed"); // leave passive / not "form"
        assertEquals(terms.get(3), "with"); // currently not stopward
        assertEquals(terms.get(4), "form");
        assertEquals(terms.get(5), "of"); // currently not stopward
        assertEquals(terms.get(6), "verb");
        assertEquals(terms.get(7), "have");
        assertEquals(terms.get(8), "and"); // currently not stopward
        assertEquals(terms.get(9), "past");
        assertEquals(terms.get(10), "participl");

    }

    /*
     * There are also filters that convert plurals to singular.
     */
    @Test
    public void testNumeral() {

        /*
         * Confirming no conversion to singular.
         */

        String query = "books boxes cities leaves men glasses";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals(terms.get(0), "books"); // leave numeral / not singular
        assertEquals(terms.get(1), "boxes");
        assertEquals(terms.get(2), "cities");
        assertEquals(terms.get(3), "leaves");
        assertEquals(terms.get(4), "men");
        assertEquals(terms.get(5), "glasses");
    }

    /*
     * Katakana is converted to hiragana. This is primarily intended for CDDB input. (It is not decided whether to
     * register in CDDB with Katakana/Hiragana)
     */
    @Test
    public void testToHiragana() {

        String n = FieldNamesConstants.ARTIST_READING;

        List<String> terms = toTermString(n, "THE BLUE HEARTS");
        assertEquals(2, terms.size(), "all Alpha : " + n);
        assertEquals("blue", terms.get(0), "all Alpha : " + n);
        assertEquals("hearts", terms.get(1), "all Alpha : " + n);

        terms = toTermString(n, "ABC123");
        assertEquals(1, terms.size(), "AlphaNum : " + n);

        String passable1 = "ABC123あいう";
        terms = toTermString(n, passable1);
        assertEquals(3, terms.size(), "ABC123あいう : " + n);
        assertEquals("abc123", terms.get(0), "ABC123あいう : " + n);
        assertEquals("あい", terms.get(1), "ABC123あいう : " + n);
        assertEquals("いう", terms.get(2), "ABC123あいう : " + n);

        terms = toTermString(n, "abc123あいう");
        assertEquals(3, terms.size(), "abc123あいう : " + n);
        assertEquals("abc123", terms.get(0), "abc123あいう : " + n);
        assertEquals("あい", terms.get(1), "abc123あいう : " + n);
        assertEquals("いう", terms.get(2), "abc123あいう : " + n);

        terms = toTermString(n, "ABC123アイウ");
        assertEquals(3, terms.size(), "ABC123アイウ : " + n);
        assertEquals("abc123", terms.get(0), "abc123アイウ : " + n);
        assertEquals("あい", terms.get(1), "abc123アイウ: " + n);
        assertEquals("いう", terms.get(2), "abc123アイウ : " + n);

    }

    /*
     * ALBUM_EX and TITLE_EX only allow Hiragana. Because hiragana-only high-precision word analysis is difficult, the
     * purpose is to support the full text. (Hiragana-only album names and song names may be used to give a particularly
     * soft impression, but are generally not used very often.)
     */
    @Test
    public void testHiraganaTermStemOnlyHiragana() {
        // #482 No longer used
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    @Test
    public void testPunctuationStem() {

        // Filter operation check only. Verify only some settings.
        String query = "B︴C";
        String expected2 = "bc";
        String expected2a = "b";
        String expected2b = "c";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals(1, terms.size(), "through : " + n);
                assertEquals(query, terms.get(0), "through : " + n);
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals(1, terms.size(), "token through filtered : " + n);
                assertEquals(expected2, terms.get(0), "token through filtered : " + n);
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals(2, terms.size(), "tokend : " + n);
                assertEquals(expected2a, terms.get(0), "tokend : " + n);
                assertEquals(expected2b, terms.get(1), "tokend : " + n);
                break;
            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.ARTIST_EX:
                // #482 No longer used
                break;
            default:
                break;
            }
        });
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
