
package org.airsonic.player.service.search;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Test case for Analyzer. These cases have the purpose of observing the current situation and observing the impact of
 * upgrading Lucene.
 */
@SpringBootTest
public class AnalyzerFactoryTest {

    @ClassRule
    public static final SpringClassRule classRule = new SpringClassRule() {
        HomeRule homeRule = new HomeRule();

        @Override
        public Statement apply(Statement base, Description description) {
            Statement spring = super.apply(base, description);
            return homeRule.apply(spring, description);
        }
    };

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AnalyzerFactory analyzerFactory;

    @Autowired
    private SettingsService settingsService;

    @Before
    public void setup() {
        analyzerFactory.setSearchMethodLegacy(false);
        settingsService.setSearchMethodLegacy(false);
    }

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
            assertEquals("multiToken : " + n, 7, terms.size());
        });

        // 2
        String queryHira = "くいっくぶらうん";
        String[] oneTokenHiraStopOnlyFields = { FieldNamesConstants.ALBUM_EX, FieldNamesConstants.TITLE_EX };
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("oneTokenHira(Eng) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            assertEquals("oneTokenHira(Hira) : " + n, 1, terms.size());
        });
        String queryStopsOnly = "La La La";
        Arrays.stream(oneTokenHiraStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStopsOnly);
            assertEquals("oneTokenHira(Eng) : " + n, 1, terms.size());
        });

        // 1
        String[] oneTokenStopOnlyFields = { FieldNamesConstants.ARTIST_EX };
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryEng);
            assertEquals("oneTokenHira(Eng) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryHira);
            assertEquals("oneTokenHira(Hira) : " + n, 0, terms.size());
        });
        Arrays.stream(oneTokenStopOnlyFields).forEach(n -> {
            List<String> terms = toTermString(n, queryStopsOnly);
            assertEquals("oneTokenHira(Eng) : " + n, 1, terms.size());
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
                assertEquals("tokenized : " + n, 2, terms.size());
                assertEquals("tokenized : " + n, expected1, terms.get(0));
                assertEquals("tokenized : " + n, expected2, terms.get(1));
                break;

            /*
             * What should the fields of this be? Generally discarded.
             */
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
            case FieldNamesConstants.COMPOSER:
                assertEquals("tokenized : " + n, 2, terms.size());
                assertEquals("tokenized : " + n, expected1, terms.get(0));
                assertEquals("tokenized : " + n, expected2, terms.get(1));
                break;

            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
            case FieldNamesConstants.ARTIST_EX:
                assertEquals("tokenized : " + n, 0, terms.size());
                break;

            case FieldNamesConstants.ARTIST_READING:
            case FieldNamesConstants.COMPOSER_READING:
                assertEquals("tokenized : " + n, 1, terms.size());
                assertEquals("tokenized : " + n, expected3, terms.get(0));
                break;

            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals("tokenized : " + n, 1, terms.size());
                assertEquals("tokenized : " + n, query, terms.get(0));
                break;

            /*
             * ID, FOLDER_ID, YEAR This is not a problem because the input value does not contain a delimiter.
             */
            default:
                assertEquals("tokenized : " + n, 2, terms.size());
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
                assertEquals("removed : " + n, 0, terms.size());
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals("remain : " + n, 1, terms.size());
                break;

            default:
                assertEquals("removed : " + n, 0, terms.size());
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
                assertEquals("through : " + n, 1, articleTerms.size());
                assertEquals("through : " + n, queryArticle, articleTerms.get(0));
                assertEquals("through : " + n, 1, indexArticleTerms.size());
                assertEquals("through : " + n, queryIndexArticle, indexArticleTerms.get(0));
                assertEquals("through : " + n, 1, noStopTerms.size());
                assertEquals("through : " + n, queryNoStop, noStopTerms.get(0));
                assertEquals("through : " + n, 1, jpStopTerms.size());
                assertEquals("through : " + n, queryJpStop, jpStopTerms.get(0));
                break;
            case FieldNamesConstants.GENRE:
                assertEquals("through : " + n, 1, articleTerms.size());
                assertEquals("through : " + n, queryArticle, articleTerms.get(0));
                assertEquals("through : " + n, 1, indexArticleTerms.size());
                assertEquals("through : " + n, queryIndexArticle, indexArticleTerms.get(0));
                assertEquals("through : " + n, 1, noStopTerms.size());
                assertEquals("through : " + n, queryNoStop, noStopTerms.get(0));
                // false positives?
                assertEquals("??????? : " + n, 2, jpStopTerms.size());
                assertEquals("??????? : " + n,
                        "の に は を た が で て と し れ さ ある いる も する から な こと として い や れる など なっ ない この ため その あっ よう また もの という あり まで られ なる へ か だ これ によって により おり より による ず なり られる において ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して 特に せる 及び これら",
                        jpStopTerms.get(0));
                assertEquals("??????? : " + n,
                        " とき では にて ほか ながら うち そして とともに ただし かつて それぞれ または お ほど ものの に対する ほとんど と共に といった です とも ところ ここ",
                        jpStopTerms.get(1));
                break;
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals("apply : " + n, 0, articleTerms.size());
                assertEquals("apply : " + n, 0, indexArticleTerms.size());
                assertEquals("through : " + n, 30, noStopTerms.size());
                assertEquals("apply : " + n, 110, jpStopTerms.size()); // XXX Legacy(0) -> Phrase(110)
                break;
            case FieldNamesConstants.ARTIST:
                assertEquals("apply : " + n, 0, articleTerms.size());
                assertEquals("apply : " + n, 0, indexArticleTerms.size());
                assertEquals("through : " + n, 29, noStopTerms.size()); // with is removed
                assertEquals("apply : " + n, 110, jpStopTerms.size()); // XXX Legacy(53) -> Phrase(110)
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals("article : " + n, 0, articleTerms.size());
                assertEquals("indexArticle : " + n, 0, indexArticleTerms.size());
                assertEquals("noStop : " + n, 29, noStopTerms.size()); // with is removed
                assertEquals("jpStop : " + n, 152, jpStopTerms.size()); // XXX Legacy(109) -> Phrase(152)
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
        assertEquals("this", terms.get(0));// removal target is ignored
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
        assertEquals("this", terms.get(0));// removal target is ignored
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
                assertEquals("no case : " + n, 0, terms.size());
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE_KEY:
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, query, terms.get(0));
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals("apply : " + n, 4, terms.size());
                assertEquals("apply : " + n, "caesar", terms.get(0));
                assertEquals("apply : " + n, "しい", terms.get(1));
                assertEquals("apply : " + n, "ーざ", terms.get(2));
                assertEquals("apply : " + n, "ざあ", terms.get(3));
                break;
            case FieldNamesConstants.GENRE:
                assertEquals("apply : " + n, 1, terms.size());
                assertEquals("apply : " + n, expected3, terms.get(0));
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
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
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, query, terms.get(0));
                break;
            case FieldNamesConstants.GENRE:
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
                assertEquals("no case : " + n, 0, terms.size());
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.MEDIA_TYPE:
            case FieldNamesConstants.GENRE_KEY:
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, query, terms.get(0));
                break;
            case FieldNamesConstants.GENRE:
                assertEquals("apply : " + n, 1, terms.size());
                assertEquals("apply : " + n, apply1, terms.get(0));
                break;
            case FieldNamesConstants.TITLE_EX:
            case FieldNamesConstants.ALBUM_EX:
                terms = toTermString(n, query2);
                assertEquals("no case : " + n, 2, terms.size());
                assertEquals("bigram : " + n, "ぁぃ", terms.get(0));
                assertEquals("bigram : " + n, "ぃぅ", terms.get(1));
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals("apply : " + n, 3, terms.size());
                assertEquals("apply : " + n, apply1a, terms.get(0));
                assertEquals("apply : " + n, "あい", terms.get(1));
                assertEquals("apply : " + n, "いう", terms.get(2));
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals("apply : " + n, 2, terms.size());
                assertEquals("apply : " + n, apply1a, terms.get(0));
                assertEquals("apply : " + n, apply1b, terms.get(1));
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
                assertEquals("no case : " + n, 0, terms.size());
                break;
            case FieldNamesConstants.FOLDER:
            case FieldNamesConstants.GENRE:
            case FieldNamesConstants.MEDIA_TYPE:
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, query, terms.get(0));
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals("apply : " + n, 2, terms.size());
                assertEquals("apply : " + n, "abcdefg", terms.get(0));
                assertEquals("apply : " + n, "ふ", terms.get(1));
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals("apply : " + n, 2, terms.size());
                assertEquals("apply : " + n, expected1a, terms.get(0));
                assertEquals("apply : " + n, expected1b, terms.get(1));
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
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, escapeRequires, terms.get(0));
                terms = toTermString(n, fileUsable);
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, fileUsable, terms.get(0));
            } else if (FieldNamesConstants.GENRE.equals(n)) {
                // XXX @see AnalyzerFactory#addTokenFilterForTokenToDomainValue
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, "+-&&||! { }[ ]^\"~*?:\\/", terms.get(0));
                terms = toTermString(n, fileUsable);
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, "+-&&! { }[ ]^~", terms.get(0));
            } else {
                // Strings that require escape for most fields are removed during parsing.
                assertEquals("trancate : " + n, 0, terms.size());
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
        assertEquals("this", terms.get(0));
        assertEquals("is", terms.get(1));
        assertEquals("jpsonic", terms.get(2));
        // assertEquals("s", terms.get(3)); issues#290
        assertEquals("analysis", terms.get(3));

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
        assertEquals("books", terms.get(0));// leave numeral / not singular
        assertEquals("boxes", terms.get(1));
        assertEquals("cities", terms.get(2));
        assertEquals("leaves", terms.get(3));
        assertEquals("men", terms.get(4));
        assertEquals("glasses", terms.get(5));
    }

    /*
     * Katakana is converted to hiragana. This is primarily intended for CDDB input. (It is not decided whether to
     * register in CDDB with Katakana/Hiragana)
     */
    @Test
    public void testToHiragana() {

        String notChange1 = "THE BLUE HEARTS";
        String notChange2 = "ABC123";
        String passable1 = "ABC123あいう";
        String expected1 = "abc123あいう";
        String passable2 = "ABC123アイウ";

        String n = FieldNamesConstants.ARTIST_READING;

        List<String> terms = toTermString(n, notChange1);
        assertEquals("all Alpha : " + n, 2, terms.size());
        assertEquals("all Alpha : " + n, "blue", terms.get(0));
        assertEquals("all Alpha : " + n, "hearts", terms.get(1));

        terms = toTermString(n, notChange2);
        assertEquals("AlphaNum : " + n, 1, terms.size());

        terms = toTermString(n, passable1);
        assertEquals("ABC123あいう : " + n, 3, terms.size());
        assertEquals("ABC123あいう : " + n, "abc123", terms.get(0));
        assertEquals("ABC123あいう : " + n, "あい", terms.get(1));
        assertEquals("ABC123あいう : " + n, "いう", terms.get(2));

        terms = toTermString(n, expected1);
        assertEquals("abc123あいう : " + n, 3, terms.size());
        assertEquals("abc123あいう : " + n, "abc123", terms.get(0));
        assertEquals("abc123あいう : " + n, "あい", terms.get(1));
        assertEquals("abc123あいう : " + n, "いう", terms.get(2));

        terms = toTermString(n, passable2);
        assertEquals("ABC123アイウ : " + n, 3, terms.size());
        assertEquals("abc123アイウ : " + n, "abc123", terms.get(0));
        assertEquals("abc123アイウ: " + n, "あい", terms.get(1));
        assertEquals("abc123アイウ : " + n, "いう", terms.get(2));

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
                assertEquals("through : " + n, 1, terms.size());
                assertEquals("through : " + n, query, terms.get(0));
                break;
            case FieldNamesConstants.ARTIST_READING:
                assertEquals("token through filtered : " + n, 1, terms.size());
                assertEquals("token through filtered : " + n, expected2, terms.get(0));
                break;
            case FieldNamesConstants.ARTIST:
            case FieldNamesConstants.ALBUM:
            case FieldNamesConstants.TITLE:
                assertEquals("tokend : " + n, 2, terms.size());
                assertEquals("tokend : " + n, expected2a, terms.get(0));
                assertEquals("tokend : " + n, expected2b, terms.get(1));
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
        try {
            TokenStream stream = analyzerFactory.getAnalyzer().tokenStream(field, new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString().replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTest.class).error("Error during Token processing.", e);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private List<String> toQueryTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = analyzerFactory.getQueryAnalyzer().tokenStream(field, new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString().replaceAll("^term\\=", ""));
            }
            stream.close();
        } catch (IOException e) {
            LoggerFactory.getLogger(AnalyzerFactoryTest.class).error("Error during Token processing.", e);
        }
        return result;
    }

}
