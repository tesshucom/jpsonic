
package org.airsonic.player.service.search;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * Test case for Analyzer.
 * These cases have the purpose of observing the current situation
 * and observing the impact of upgrading Lucene.
 */
public class AnalyzerFactoryTestCase {

    private AnalyzerFactory analyzerFactory = new AnalyzerFactory();


    /*
     * Fields with the same name as legacy servers are expected to have roughly
     * similar uses / actions.
     * 
     * Many of the Fields added by Jpsonic perform 1token processing.
     */
    @Test
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
    @Test
    public void testCJKWidth() {
        String query = "ＡＢＣａｂｃｱｲｳ";
        String apply1 = "ABCabcアイウ";
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
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE_KEY:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.GENRE:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, apply1, terms.get(0));
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
     * 
     * XXX Contents are different from Airsonic because the specifications are different.
     */
    @Test
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
                case FieldNames.MEDIA_TYPE:
                case FieldNames.GENRE_KEY:
                    assertEquals("through : " + n, 1, articleTerms.size());
                    assertEquals("through : " + n, queryArticle, articleTerms.get(0));
                    assertEquals("through : " + n, 1, indexArticleTerms.size());
                    assertEquals("through : " + n, queryIndexArticle, indexArticleTerms.get(0));
                    assertEquals("through : " + n, 1, noStopTerms.size());
                    assertEquals("through : " + n, queryNoStop, noStopTerms.get(0));
                    assertEquals("through : " + n, 1, jpStopTerms.size());
                    assertEquals("through : " + n, queryJpStop, jpStopTerms.get(0));
                    break;
                case FieldNames.GENRE:
                    assertEquals("through : " + n, 1, articleTerms.size());
                    assertEquals("through : " + n, queryArticle, articleTerms.get(0));
                    assertEquals("through : " + n, 1, indexArticleTerms.size());
                    assertEquals("through : " + n, queryIndexArticle, indexArticleTerms.get(0));
                    assertEquals("through : " + n, 1, noStopTerms.size());
                    assertEquals("through : " + n, queryNoStop, noStopTerms.get(0));
                    //false positives?
                    assertEquals("??????? : " + n, 2, jpStopTerms.size());
                    assertEquals("??????? : " + n, "の に は を た が で て と し れ さ ある いる も する から な こと として い や れる など なっ ない この ため その あっ よう また もの という あり まで られ なる へ か だ これ によって により おり より による ず なり られる において ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して 特に せる 及び これら", jpStopTerms.get(0));
                    assertEquals("??????? : " + n, " とき では にて ほか ながら うち そして とともに ただし かつて それぞれ または お ほど ものの に対する ほとんど と共に といった です とも ところ ここ", jpStopTerms.get(1));
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
    @Test
    public void testJapanesePartOfSpeechStop() {

        // Filter operation check only. Verify only some settings.
        String query = "{'“『【【】】[○◎@ $〒→+]";
        String expected1 = "{'\"『【【】】[○◎@ $〒→+]";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                // Do nothing
                case FieldNames.FOLDER:
                case FieldNames.GENRE_KEY:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.GENRE:
                    // Some character strings are replaced within the range that does not affect display
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("apply : " + n, expected1, terms.get(0));
                    break;
                default:
                    // Strings not relevant to the search are removed
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
    @Test
    public void testASCIIFoldingStop() {

        // Filter operation check only. Verify only some settings.
        String query = "Cæsarシーザー";
        // String expected1 = "caesarシーザー";
        String expected1a = "caesar";
        String expected1b = "シーザー";
        String expected2 = "caesarしいざあ";
        String expected3 = "Caesarシーザー";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("no case : " + n, 0, terms.size());
                    break;
                case FieldNames.FOLDER:
                case FieldNames.GENRE_KEY:
                case FieldNames.MEDIA_TYPE:
                    assertEquals("through : " + n, 1, terms.size());
                    assertEquals("through : " + n, query, terms.get(0));
                    break;
                case FieldNames.ARTIST_READING:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, expected2, terms.get(0));
                    break;
                case FieldNames.GENRE:
                    assertEquals("apply : " + n, 1, terms.size());
                    assertEquals("apply : " + n, expected3, terms.get(0));
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
     * 
     * XXX Contents are different from Airsonic because the specifications are different.
     */
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testLuceneEscapeRequires() {

        String escapeRequires = "+-&&||!(){}[]^\"~*?:\\/";
        String fileUsable = "+-&&!(){}[]^~";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, escapeRequires);
            // TODO These fields handle escape strings as they are.
            if (FieldNames.FOLDER.equals(n) || FieldNames.GENRE_KEY.equals(n) || FieldNames.GENRE.equals(n)) {
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, escapeRequires, terms.get(0));
                terms = toTermString(n, fileUsable);
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, fileUsable, terms.get(0));
            } else {
                // Strings that require escape for most fields are removed during parsing.
                assertEquals("trancate : " + n, 0, terms.size());
            }
        });
    }

    /*
     * All fields except FOLDER ignore characters required escape.
     */
    @Test
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
    @Test
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
     * 
     * XXX Contents are different from Airsonic because the specifications are different.
     */
    @Test
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

    @Test
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
     * 
     * XXX Contents are different from Airsonic because the specifications are different.
     */
    @Test
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
    @Test
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

    
    /**
     * Test for the number of character separators per field.
     */
    @Test
    public void testTokenCounts() {

        /*
         * Analyzer used in legacy uses the same Tokenizer for all fields.
         * (Some fields are converted to their own input string for integrity.)
         * As a result, specifications for strings are scattered and difficult to understand.
         * Using PerFieldAnalyzerWrapper,
         * it is possible to use different Analyzer (Tokenizer/Filter) for each field.
         * This allows consistent management of parsing definitions.
         * It is also possible to apply definitions such as "id3 delimiters Tokenizer" to specific fields.
         */

        // The number of words excluding articles is 7.
        String query = "The quick brown fox jumps over the lazy dog.";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {

                /*
                 * In the legacy, these field divide input into 7. It is not necessary to delimit
                 * this field originally.
                 */
                case FieldNames.MEDIA_TYPE:
                    assertEquals("oneTokenFields : " + n, 7, terms.size());
                    break;

                /*
                 * These should be divided into 7.
                 */
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("multiTokenFields : " + n, 7, terms.size());
                    break;

                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("prohibit : " + n, 0, terms.size());
                    break;

                case FieldNames.FOLDER:
                case FieldNames.GENRE_KEY:
                case FieldNames.GENRE:
                    assertEquals("remain : " + n, 1, terms.size());
                    break;
                    

                /*
                 * ID,  YEAR
                 * This is not a problem because the input value does not contain a delimiter.
                 */
                default:
                    assertEquals("oneTokenFields : " + n, 7, terms.size());
                    break;
            }
        });

    }

    /**
     * Detailed tests on Punctuation.
     * In addition to the common delimiters, there are many delimiters.
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
                 * In the legacy, these field divide input into 2.
                 * It is not necessary to delimit
                 * this field originally.
                 */
                case FieldNames.MEDIA_TYPE:
                    assertEquals("tokenized : " + n, 2, terms.size());
                    assertEquals("tokenized : " + n, expected1, terms.get(0));
                    assertEquals("tokenized : " + n, expected2, terms.get(1));
                    break;

                /*
                 * What should the fields of this be?
                 * Generally discarded.
                 */
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("tokenized : " + n, 2, terms.size());
                    assertEquals("tokenized : " + n, expected1, terms.get(0));
                    assertEquals("tokenized : " + n, expected2, terms.get(1));
                    break;

                case FieldNames.TITLE_EX:
                case FieldNames.ALBUM_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("tokenized : " + n, 0, terms.size());
                    break;

                case FieldNames.ARTIST_READING:
                    assertEquals("tokenized : " + n, 1, terms.size());
                    assertEquals("tokenized : " + n, expected3, terms.get(0));
                    break;

                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.GENRE_KEY:
                    assertEquals("tokenized : " + n, 1, terms.size());
                    assertEquals("tokenized : " + n, query, terms.get(0));
                    break;

                    /*
                 * ID, FOLDER_ID, YEAR
                 * This is not a problem because the input value does not contain a delimiter.
                 */
                default:
                    assertEquals("tokenized : " + n, 2, terms.size());
                    break;
            }
        });
    }

    /*
     * Detailed tests on Punctuation.
     * Many of the symbols are delimiters or target to be removed.
     */
    @Test
    public void testPunctuation2() {

        String query = "{'“『【【】】[︴○◎@ $〒→+]";
        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> terms = toTermString(n, query);
            switch (n) {
                case FieldNames.MEDIA_TYPE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("removed : " + n, 0, terms.size());
                    break;
                case FieldNames.FOLDER:
                case FieldNames.GENRE:
                case FieldNames.GENRE_KEY:
                    assertEquals("remain : " + n, 1, terms.size());
                    break;

                
                default:
                    assertEquals("removed : " + n, 0, terms.size());
            }
        });
    }

    /**
     * Combined case of Stop and full-width.
     */
    @Test
    public void testStopwardAndFullWidth() {

        /*
         * Legacy can avoid Stopward if it is full width.
         * It is unclear whether it is a specification or not.
         * (Problems due to a defect in filter application order?
         * or
         * Is it popular in English speaking countries?)
         */
        String queryFullWidth = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
        List<String> terms = toTermString(queryFullWidth);
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0));// removal target is ignored
        assertEquals("is", terms.get(1));
        assertEquals("full", terms.get(2));
        assertEquals("width", terms.get(3));
        assertEquals("sentences", terms.get(4));

    }

    /**
     * Tests on ligature and diacritical marks.
     * In UAX#29, determination of non-practical word boundaries is not considered.
     * Languages ​​that use special strings require "practical word" sample.
     * Unit testing with only ligature and diacritical marks is not possible.
     */
    @Test
    public void testAsciiFoldingStop() {

        String queryLigature = "Cæsar";
        String expectedLigature1 = "caesar";
        String expectedLigature2 = "Caesar";

        String queryDiacritical = "Café";
        String expectedDiacritical1 = "cafe";
        String expectedDiacritical2 = "Cafe";

        Arrays.stream(IndexType.values()).flatMap(i -> Arrays.stream(i.getFields())).forEach(n -> {
            List<String> termsLigature = toTermString(n, queryLigature);
            List<String> termsDiacritical = toTermString(n, queryDiacritical);
            switch (n) {

                /*
                 * It is decomposed into the expected string.
                 */
                case FieldNames.MEDIA_TYPE:
                case FieldNames.ARTIST:
                case FieldNames.ALBUM:
                case FieldNames.TITLE:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("Cæsar : " + n, expectedLigature1, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, expectedDiacritical1, termsDiacritical.get(0));
                    break;

                case FieldNames.ALBUM_EX:
                case FieldNames.TITLE_EX:
                case FieldNames.ARTIST_EX:
                    assertEquals("remove : " + n, 0, termsLigature.size());
                    break;

                case FieldNames.FOLDER:
                case FieldNames.GENRE_KEY:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("remain : " + n, queryLigature, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, queryDiacritical, termsDiacritical.get(0));
                    break;

                case FieldNames.GENRE:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("Cæsar : " + n, expectedLigature2, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, expectedDiacritical2, termsDiacritical.get(0));
                    break;

                // Legacy has common behavior for all fields.
                default:
                    assertEquals("Cæsar : " + n, 1, termsLigature.size());
                    assertEquals("Cæsar : " + n, expectedLigature1, termsLigature.get(0));
                    assertEquals("Café : " + n, 1, termsDiacritical.size());
                    assertEquals("Café : " + n, expectedDiacritical1, termsDiacritical.get(0));
                    break;

            }
        });

    }
 
    /**
     * Create an example that makes UAX 29 differences easy to understand.
     * 
     * XXX Contents are different from Airsonic because the specifications are different.
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
        assertEquals(asList("abc", "1", "def"), toTermString(" ABC1 DEF "));

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
        assertEquals(asList("abc", "def"), toTermString(":ABC:DEF:"));
        assertEquals(asList("abc", "def"), toTermString("-ABC-DEF-"));
        assertEquals(asList("abc", "def"), toTermString("/ABC/DEF/"));
        assertEquals(asList("abc", "def"), toTermString("_ABC_DEF_"));
        assertEquals(asList("abc", "def"), toTermString(",ABC,DEF,"));
        assertEquals(asList("abc", "def"), toTermString(".ABC.DEF."));
        assertEquals(asList("abc", "def"), toTermString("&ABC&DEF&"));
        assertEquals(asList("abc", "def"), toTermString("@ABC@DEF@"));
        assertEquals(asList("abc", "def"), toTermString("'ABC'DEF'"));

        // trim and delimiter and number
        assertEquals(asList("abc", "1", "def"), toTermString("+ABC1+DEF+"));
        assertEquals(asList("abc", "1", "def"), toTermString("|ABC1|DEF|"));
        assertEquals(asList("abc", "1", "def"), toTermString("!ABC1!DEF!"));
        assertEquals(asList("abc", "1", "def"), toTermString("(ABC1(DEF("));
        assertEquals(asList("abc", "1", "def"), toTermString(")ABC1)DEF)"));
        assertEquals(asList("abc", "1", "def"), toTermString("{ABC1{DEF{"));
        assertEquals(asList("abc", "1", "def"), toTermString("}ABC1}DEF}"));
        assertEquals(asList("abc", "1", "def"), toTermString("[ABC1[DEF["));
        assertEquals(asList("abc", "1", "def"), toTermString("]ABC1]DEF]"));
        assertEquals(asList("abc", "1", "def"), toTermString("^ABC1^DEF^"));
        assertEquals(asList("abc", "1", "def"), toTermString("\\ABC1\\DEF\\"));
        assertEquals(asList("abc", "1", "def"), toTermString("\"ABC1\"DEF\""));
        assertEquals(asList("abc", "1", "def"), toTermString("~ABC1~DEF~"));
        assertEquals(asList("abc", "1", "def"), toTermString("*ABC1*DEF*"));
        assertEquals(asList("abc", "1", "def"), toTermString("?ABC1?DEF?"));
        assertEquals(asList("abc", "1", "def"), toTermString(":ABC1:DEF:"));
        assertEquals(asList("abc", "1", "def"), toTermString(",ABC1,DEF,"));
        assertEquals(asList("abc", "1", "def"), toTermString("-ABC1-DEF-"));
        assertEquals(asList("abc", "1", "def"), toTermString("/ABC1/DEF/"));
        assertEquals(asList("abc", "1", "def"), toTermString("_ABC1_DEF_"));
        assertEquals(asList("abc", "1", "def"), toTermString(".ABC1.DEF."));
        assertEquals(asList("abc", "1", "def"), toTermString("&ABC1&DEF&"));
        assertEquals(asList("abc", "1", "def"), toTermString("@ABC1@DEF@"));
        assertEquals(asList("abc", "1", "def"), toTermString("'ABC1'DEF'"));

    }

    /**
     * Special handling of single quotes.
     */
    @Test
    public void testSingleQuotes() {

        /*
         * A somewhat cultural that seems to be related to a specific language.
         * 
         * XXX Contents are different from Airsonic because the specifications are different.
         */
        String query = "This is Airsonic's analysis.";
        List<String> terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0));
        assertEquals("is", terms.get(1));
        assertEquals("airsonic", terms.get(2));
        assertEquals("s", terms.get(3));
        assertEquals("analysis", terms.get(4));

        query = "We’ve been here before.";
        terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals("we", terms.get(0));
        assertEquals("ve", terms.get(1));
        assertEquals("been", terms.get(2));
        assertEquals("here", terms.get(3));
        assertEquals("before", terms.get(4));

        query = "LʼHomme";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("l", terms.get(0));
        assertEquals("homme", terms.get(1));

        query = "L'Homme";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("l", terms.get(0));
        assertEquals("homme", terms.get(1));

        query = "aujourd'hui";
        terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("aujourd", terms.get(0));
        assertEquals("hui", terms.get(1));

        query = "fo'c'sle";
        terms = toTermString(query);
        assertEquals(3, terms.size());
        assertEquals("fo", terms.get(0));
        assertEquals("c", terms.get(1));
        assertEquals("sle", terms.get(2));

    }

    private List<String> toTermString(String str) {
        return toTermString(null, str);
    }

    private List<String> toTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field,
                    new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString()
                        .replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class)
                    .error("Error during Token processing.", e);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private List<String> toQueryTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(field,
                    new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString()
                        .replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTestCase.class)
                    .error("Error during Token processing.", e);
        }
        return result;
    }

}
