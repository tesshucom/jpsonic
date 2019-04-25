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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class AnalyzerFactoryTestCase extends TestCase {

    private static Analyzer analyzer = AnalyzerFactory.getInstance().getAnalyzer();

    private static Analyzer queryAnalyzer = AnalyzerFactory.getInstance().getQueryAnalyzer();

    /*
     * Fields with the same name as legacy servers are expected to have roughly
     * similar uses / actions.
     * 
     * Many of the Fields added by Jpsonic perform 1token processing.
     */
    public void testTokenize() {

        String queryEng = "The quick brown fox jumps over the lazy dog.";

        // 3
        // FieldNames.ID, FieldNames.FOLDER_ID, FieldNames.YEAR

        // 3
        String[] oneTokenFields = { FieldNames.GENRE, FieldNames.MEDIA_TYPE, FieldNames.FOLDER, };
        Arrays.stream(oneTokenFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("oneTokenFields : " + n, 1, terms.size());
        });

        // 4
        String[] multiTokenFields = { FieldNames.ARTIST, FieldNames.ARTIST_READING, FieldNames.ALBUM, FieldNames.TITLE };
        Arrays.stream(multiTokenFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("multiToken : " + n, 7, terms.size());
        });

        // 2
        String queryHira = "くいっくぶらうん";
        String[] oneTokenHiraStopOnlyFields = { FieldNames.ALBUM_EX, FieldNames.TITLE_EX };
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("oneTokenHira(Eng) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            assertEquals("oneTokenHira(Hira) : " + n, 1, terms.size());
        });
        String queryStops = "LaLa";
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStops);
            assertEquals("oneTokenHira(Eng) : " + n, 1, terms.size());
        });

        // 1
        String[] oneTokenStopOnlyFields = { FieldNames.ARTIST_EX };
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("oneTokenHira(Eng) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            assertEquals("oneTokenHira(Hira) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStops);
            assertEquals("oneTokenHira(Eng) : " + n, 1, terms.size());
        });

    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    public void testCJKWidth() {
        String query = "ＡＢＣａｂｃｱｲｳ";
        // String apply1 = "abcabcアイウ";
        String apply1a = "abcabc";
        String apply1b = "アイウ";
        String apply2 = "abcabcあいう";
        String query2 = "ぁぃぅ";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.ARTIST_EX:
                    assertEquals("no case : " + n, 0, terms.size());
                    break;
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                    terms = toTermString(n, query2);
                    assertEquals("no case : " + n, 1, terms.size());
                    assertEquals("no case " + n, query2, terms.get(0));
                    break;
                case FieldNames.ARTIST_READING:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, apply2, terms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("apply : " + n, 2, terms.size());
                    assertEquals("apply : " + n, apply1a, terms.get(0));
                    assertEquals("apply : " + n, apply1b, terms.get(1));
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Currently, Stoppward applies only to Tokenized fields.
     */
    public void testStopward() {

        /*
         * article
         */
        String queryArticle = "a an the";

        /*
         * It is not included in the Java default stopword. Default set as Airsonic
         * index stop word.
         */
        String queryIndexArticle = "el la los las le les";

        /*
         * Non-article in the default Stopward. In cases, it may be used for song names
         * of 2 to 3 words. Stop words are essential for newspapers and documents, but
         * they are over-processed for song titles.
         */
        String queryNoStop = "and are as at be but by for if in into is it no not of on " //
                + "or such that their then there these they this to was will with";

        /*
         * Japanese Stopward. Like the English stopward, there may be cases of
         * over-deletion in some cases. However, unlike the English stopward. Japanese
         * Stopward is placed less frequently at the beginning of sentences.
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
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, articleTerms.size());
                    assertEquals("through : " + n, queryArticle, articleTerms.get(0));
                    assertEquals("through : " + n, 1, indexArticleTerms.size());
                    assertEquals("through : " + n, queryIndexArticle, indexArticleTerms.get(0));
                    assertEquals("through : " + n, 1, noStopTerms.size());
                    assertEquals("through : " + n, queryNoStop, noStopTerms.get(0));
                    assertEquals("through : " + n, 1, jpStopTerms.size());
                    assertEquals("through : " + n, queryJpStop, jpStopTerms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ARTIST_READING:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("apply : " + n, 0, articleTerms.size());
                    assertEquals("apply : " + n, 0, indexArticleTerms.size());
                    assertEquals("through : " + n, 30, noStopTerms.size());
                    assertEquals("apply : " + n, 0, jpStopTerms.size());
                    break;
                default:
                    break;
            }
        });

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            // to be affected by other filters
            List<String> articleTerms = toTermString(n, queryArticle.replaceAll(" ", ""));
            List<String> indexArticleTerms = toTermString(n, queryIndexArticle.replaceAll(" ", ""));
            List<String> noStopTerms = toTermString(n, queryNoStop.replaceAll(" ", ""));
            List<String> jpStopTerms = toTermString(n, queryJpStop.replaceAll(" ", ""));
            switch (n) {
                case FieldNames.ARTIST_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.TITLE_EX:
                    assertEquals("through : " + n, 1, articleTerms.size());
                    assertEquals("through : " + n, 1, indexArticleTerms.size());
                    assertEquals("apply : " + n, 0, noStopTerms.size());
                    assertEquals("through : " + n, 1, jpStopTerms.size());
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    public void testJapanesePartOfSpeechStop() {

        // Filter operation check only. Verify only some settings.
        String query = "{'“『【【】】[○◎@ $〒→+]";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                default:
                    assertEquals("apply : " + n, 0, terms.size());
                    break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE. In UAX#29, determination of
     * non-practical word boundaries is not considered. Languages ​​that use special
     * strings require practical verification.
     */
    public void testASCIIFoldingStop() {

        // Filter operation check only. Verify only some settings.
        String query = "Cæsarシーザー";
        // String expected1 = "caesarシーザー";
        String expected1a = "caesar";
        String expected1b = "シーザー";
        String expected2 = "caesarしいざあ";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("no case : " + n, 0, terms.size());
                    break;
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.ARTIST_READING:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, expected2, terms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("apply : " + n, 2, terms.size());
                    assertEquals("apply : " + n, expected1a, terms.get(0));
                    assertEquals("apply : " + n, expected1b, terms.get(1));
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    public void testLowerCase() {

        // Filter operation check only. Verify only some settings.
        String query = "ABCDEFGふ";
        String expected1 = "abcdefgふ";
        String expected1a = "abcdefg";
        String expected1b = "ふ";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("no case : " + n, 0, terms.size());
                    break;
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.ARTIST_READING:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, expected1, terms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("apply : " + n, 2, terms.size());
                    assertEquals("apply : " + n, expected1a, terms.get(0));
                    assertEquals("apply : " + n, expected1b, terms.get(1));
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Applies to all except FOLDER, GENRE, MEDIA_TYPE.
     */
    public void testPunctuationStem() {

        // Filter operation check only. Verify only some settings.
        String query = "B︴C";
        String expected2 = "bc";
        String expected2a = "b";
        String expected2b = "c";
        String queryJp = "ふ︴い";
        String expectedJp = "ふい";
        String queryStop = "the︴これら";
        String expectedStop = "theこれら";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.ARTIST_READING:
                    assertEquals("token through filtered : " + n, 1, terms.size());
                    assertEquals("token through filtered : " + n, expected2, terms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("tokend : " + n, 2, terms.size());
                    assertEquals("tokend : " + n, expected2a, terms.get(0));
                    assertEquals("tokend : " + n, expected2b, terms.get(1));
                    break;
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                    terms = toTermString(n, queryJp);
                    assertEquals("token through filtered : " + n, 1, terms.size());
                    assertEquals("token through filtered : " + n, expectedJp, terms.get(0));
                case FieldNames.ARTIST_EX:
                    terms = toTermString(n, queryStop);
                    assertEquals("token through filtered : " + n, 1, terms.size());
                    assertEquals("token through filtered : " + n, expectedStop, terms.get(0));
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * ALBUM_EX and TITLE_EX only allow Hiragana.
     * Because hiragana-only high-precision word analysis is difficult, the purpose
     * is to support the full text. (Hiragana-only album names and song names may be
     * used to give a particularly soft impression, but are generally not used very
     * often.)
     */
    public void testHiraganaTermStemOnlyHiragana() {

        String notPass1 = "THE BLUE HEARTS";
        String notPass2 = "ABC123";
        String notPass3 = "ABC123あいう";
        String passable = "あいう";

        String[] otherThanHiraganaFields = { FieldNames.ALBUM_EX, FieldNames.TITLE_EX };
        Arrays.stream(otherThanHiraganaFields).forEach(n -> {
            List<String> terms = toTermString(n, notPass1);
            assertEquals("all Alpha : " + n, 0, terms.size());
            terms = toTermString(n, notPass2);
            assertEquals("AlphaNum : " + n, 0, terms.size());
            terms = toTermString(n, notPass3);
            assertEquals("AlphaNumHiragana : " + n, 0, terms.size());
            terms = toTermString(n, passable);
            assertEquals("Hiragana : " + n, 1, terms.size());
            assertEquals("Hiragana : " + n, passable, terms.get(0));
        });
    }

    /*
     * Katakana is converted to hiragana. This is primarily intended for CDDB input.
     * (It is not decided whether to register in CDDB with Katakana/Hiragana)
     */
    public void testToHiragana() {

        String notChange1 = "THE BLUE HEARTS";
        String notChange2 = "ABC123";
        String passable1 = "ABC123あいう";
        String expected1 = "abc123あいう";
        String passable2 = "ABC123アイウ";
        String expected2 = "abc123あいう";

        String n = FieldNames.ARTIST_READING;

        List<String> terms = toTermString(n, notChange1);
        assertEquals("all Alpha : " + n, 2, terms.size());
        assertEquals("all Alpha : " + n, "blue", terms.get(0));
        assertEquals("all Alpha : " + n, "hearts", terms.get(1));
        terms = toTermString(n, notChange2);
        assertEquals("AlphaNum : " + n, 1, terms.size());
        terms = toTermString(n, passable1);
        assertEquals("AlphaNumHiragana : " + n, 1, terms.size());
        assertEquals("AlphaNumHiragana : " + n, expected1, terms.get(0));
        terms = toTermString(n, passable2);
        assertEquals("Hiragana : " + n, 1, terms.size());
        assertEquals("Hiragana : " + n, expected2, terms.get(0));

    }

    /*
     * All fields except FOLDER ignore characters required escape.
     */
    public void testLuceneEscapeRequires() {

        String escapeRequires = "+-&&||!(){}[]^\"~*?:\\/";
        String fileUsable = "+-&&!(){}[]^~";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, escapeRequires);
            if (FieldNames.FOLDER.equals(n)) {
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, escapeRequires, terms.get(0));
                terms = toTermString(n, fileUsable);
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, fileUsable, terms.get(0));
            } else {
                assertEquals("trancate : " + n, 0, terms.size());
            }
        });
    }

    /*
     * All fields except FOLDER ignore characters required escape.
     */
    public void testWildCard() {

        String query = "ORANGE RANGE";
        // String expected1 = "orangerange";
        // String expected1Wild = "orangerange*";
        String expected2a = "orange";
        String expected2b = "range";
        String expected2aWild = "orange*";
        String expected2bWild = "range*";
        String queryJp2 = "ゆず";
        String expectedJp2 = "ゆず";
        String expectedJp2Wild = "ゆず*";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            List<String> queryTerms = toQueryTermString(n, query);
            switch (n) {
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("normal : " + n, 1, terms.size());
                    assertEquals("normal : " + n, 1, queryTerms.size());
                    assertEquals("normal : " + n, terms.get(0), queryTerms.get(0));
                    break;
                case FieldNames.ARTIST:
                case FieldNames.ARTIST_READING:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("normal : " + n, 2, terms.size());
                    assertEquals("normal : " + n, expected2a, terms.get(0));
                    assertEquals("normal : " + n, expected2b, terms.get(1));
                    assertEquals("wild : " + n, 2, queryTerms.size());
                    assertEquals("wild : " + n, expected2aWild, queryTerms.get(0));
                    assertEquals("wild : " + n, expected2bWild, queryTerms.get(1));
                    break;
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                    assertEquals("trancate : " + n, 0, terms.size());
                    assertEquals("trancate : " + n, 0, queryTerms.size());
                    terms = toTermString(n, queryJp2);
                    queryTerms = toQueryTermString(n, queryJp2);
                    assertEquals("normal : " + n, 1, terms.size());
                    assertEquals("normal : " + n, expectedJp2, terms.get(0));
                    assertEquals("wild : " + n, 1, queryTerms.size());
                    assertEquals("wild : " + n, expectedJp2Wild, queryTerms.get(0));
                    break;
                case FieldNames.ARTIST_EX:
                    assertEquals("trancate : " + n, 0, terms.size());
                    assertEquals("trancate : " + n, 0, queryTerms.size());
                    terms = toTermString(n, queryJp2);
                    queryTerms = toQueryTermString(n, queryJp2);
                    assertEquals("trancate : " + n, 0, terms.size());
                    assertEquals("trancate : " + n, 0, queryTerms.size());
                    break;
                default:
                    break;
            }
        });
    }

    /*
     * Use JapaneseTokenizer to analyze English queries.
     */
    public void testResource() {

        /*
         * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg
         * Variations, Canons [Disc 1] /01 - Bach- Goldberg Variations, BWV 988 -
         * Aria.flac
         * 
         */

        // title
        String query = "Bach: Goldberg Variations, BWV 988 - Aria";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("bach", terms.get(0));
        assertEquals("goldberg", terms.get(1));
        assertEquals("variations", terms.get(2));
        assertEquals("bwv", terms.get(3));
        assertEquals("988", terms.get(4));
        assertEquals("aria", terms.get(5));

        // artist
        query = "_ID3_ARTIST_ Céline Frisch: Café Zimmermann";
        terms = toTermString(query);
        assertEquals(7, terms.size());

        /*
         * (lucene 3.0) id3_artist (kuromoji) id 3 artist
         */
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("artist", terms.get(2));
        assertEquals("celine", terms.get(3));
        assertEquals("frisch", terms.get(4));
        assertEquals("cafe", terms.get(5));
        assertEquals("zimmermann", terms.get(6));

    }

    /*
     * Subsonic can avoid Stopward if it is upper case. It is unclear whether it is
     * a specification or not. Jpsonic's Stopward is not case sensitive.
     */
    public void testFullWidth() {
        String query = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
        List<String> terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0));// removal target is ignored
        assertEquals("is", terms.get(1));
        assertEquals("full", terms.get(2));
        assertEquals("width", terms.get(3));
        assertEquals("sentences", terms.get(4));
    }

    public void testPossessiveCase() {
        String query = "This is Jpsonic's analysis.";
        List<String> terms = toTermString(query);
        assertEquals(5, terms.size());

        /*
         * (lucene 3.0) jpsonic (kuromoji) jpsonic s
         */
        assertEquals("this", terms.get(0));// currently not stopward
        assertEquals("is", terms.get(1));// currently not stopward
        assertEquals("jpsonic", terms.get(2));// removal of apostrophes
        assertEquals("s", terms.get(3)); // "s" remain.
        assertEquals("analysis", terms.get(4));
    }

    /*
     * Do not convert to present form; present tense
     */
    public void testPastParticiple() {
        String query = "This is formed with a form of the verb \"have\" and a past participl.";
        List<String> terms = toTermString(query);
        assertEquals(11, terms.size());
        assertEquals("this", terms.get(0));// currently not stopward
        assertEquals("is", terms.get(1));// currently not stopward
        assertEquals("formed", terms.get(2));// leave passive / not "form"
        assertEquals("with", terms.get(3));// currently not stopward
        assertEquals("form", terms.get(4));
        assertEquals("of", terms.get(5));// currently not stopward
        assertEquals("verb", terms.get(6));
        assertEquals("have", terms.get(7));
        assertEquals("and", terms.get(8));// currently not stopward
        assertEquals("past", terms.get(9));
        assertEquals("participl", terms.get(10));
    }

    /*
     * Do not convert to singular
     */
    public void testNumeral() {
        String query = "books boxes cities leaves men glasses";
        List<String> terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("books", terms.get(0));// leave numeral / not singular
        assertEquals("boxes", terms.get(1));
        assertEquals("cities", terms.get(2));
        assertEquals("leaves", terms.get(3));
        assertEquals("men", terms.get(4));
        assertEquals("glasses", terms.get(5));
    }

    public static List<String> toTermString(String str) {
        return toTermString(null, str);
    }

    public static List<String> toTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzer.tokenStream(field, new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class).error("Error during Token processing.", e);
        }
        return result;
    }

    public static List<String> toQueryTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = queryAnalyzer.tokenStream(field, new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class).error("Error during Token processing.", e);
        }
        return result;
    }

}