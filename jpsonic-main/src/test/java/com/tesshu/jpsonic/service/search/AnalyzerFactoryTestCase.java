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

import static org.junit.Assert.assertNotEquals;
import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import java.io.IOException;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/*
 * Main differences
 * [Separation]
 * Unlike Subsonic, Jpsonic uses character separation of UAX#29.
 * 
 * [Stop word]
 * Also, due to the importance of query search, the stop word covers only articles.
 * Subsonic distinguishes uppercase and lowercase letters of Stopward,
 * but Jpsonic does not distinguish.
 * 
 * [Tokenize]
 * There is a rule for each field.
 */
public class AnalyzerFactoryTestCase extends TestCase {

    /*
     * Compound phrases of
     * hiragana and katakana are easy to analyze
     */
    public void testCompoundHiraganaKatana() {
        String query = "イキモノがかり";
        List<String> terms = toTermString(query);
        assertEquals("イキモノがかり", 2, terms.size());// 生き物係り
        assertEquals("イキモノ", terms.get(0));
        assertEquals("がかり", terms.get(1));
    }

    /*
     * Analysis accuracy ofkatakana does not matter so much.
     */
    public void testKatanaOnly() {
        String query = "イキモノガカリ";
        List<String> terms = toTermString(query);
        assertEquals("イキモノガカリ", 1, terms.size());// イキモノガカリ
    }

    /*
     * Hiragana's short flales are often difficult.
     * Avoid dictionary growth and complex logic.
     * Some fields are full name registration for completion.
     */
    public void testHiraganaWithJapaneseTokenizer() {

        String query = "い";
        List<String> terms = toTermString(query);
        assertEquals("い", 0, terms.size());// stopward

        query = "いき";
        terms = toTermString(query);
        assertEquals("いき", 1, terms.size());

        query = "いきも";
        terms = toTermString(query);
        assertEquals("いきも", 1, terms.size());

        query = "いきもの";
        terms = toTermString(query);
        assertEquals("いきもの", 1, terms.size());

        query = "いきものが";
        terms = toTermString(query);
        assertEquals("いきものが", 1, terms.size());
        assertEquals("いきものが", "いき", terms.get(0));

        query = "いきものがか";
        terms = toTermString(query);
        assertEquals("いきものがか", 1, terms.size());
        assertEquals("いきものがか", "いき", terms.get(0));

        query = "いきものがかり";
        terms = toTermString(query);
        assertEquals("いきものがかり", 2, terms.size());
        assertEquals("いき", terms.get(0));
        assertEquals("かり", terms.get(1));
        
        query = "いきものがかり";
        terms = toTermString(FieldNames.ARTIST_READING_HIRAGANA, query);
        assertEquals("いきものがかり", 1, terms.size());
        assertEquals(query, terms.get(0));

        query = "すももも　ももも　ももの　うち";
        terms = toTermString(query);
        assertEquals(3, terms.size());// including stopward

        query = "す桃も桃も桃のうち";
        terms = toTermString(query);
        assertEquals(4, terms.size());
        assertEquals("す", terms.get(0));
        assertEquals("桃", terms.get(1));
        assertEquals("桃", terms.get(2));
        assertEquals("桃", terms.get(3));

        query = "Both plum and peach are peach.";
        terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("both", terms.get(0));
        assertEquals("plum", terms.get(1));
        assertEquals("and", terms.get(2));
        assertEquals("peach", terms.get(3));
        assertEquals("are", terms.get(4));
        assertEquals("peach", terms.get(5));

    }

    /*
     * Use JapaneseTokenizer to analyze English queries.
     */
    public void testEnglishWithJapaneseTokenizer() {
        // JapaneseTokenizer
        String query = "Both";
        List<String> terms = toTermString(query);
        assertEquals("Both", 1, terms.size());

        query = "Both plum";
        terms = toTermString(query);
        assertEquals("Both plum", 2, terms.size());

        query = "Both plum and";
        terms = toTermString(query);
        assertEquals("Both plum and", 3, terms.size());

        query = "Both plum and peach";
        terms = toTermString(query);
        assertEquals("Both plum and peach", 4, terms.size());

        query = "Both plum and peach are";
        terms = toTermString(query);
        assertEquals("Both plum and peach are", 5, terms.size());

        query = "Both plum and peach are peach.";
        terms = toTermString(query);
        assertEquals("Both plum and peach are peach.", 6, terms.size());
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
         * (lucene 3.0) id3_artist
         * (kuromoji) id 3 artist
         */
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("artist", terms.get(2));
        assertEquals("celine", terms.get(3));
        assertEquals("frisch", terms.get(4));
        assertEquals("cafe", terms.get(5));
        assertEquals("zimmermann", terms.get(6));

        // album
        query = "_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]";
        terms = toTermString(query);
        assertEquals(9, terms.size());

        /*
         * (lucene 3.0) id3_artist
         * (kuromoji) id 3 artist
         */
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("album", terms.get(2));
        assertEquals("bach", terms.get(3));
        assertEquals("goldberg", terms.get(4));
        assertEquals("variations", terms.get(5));
        assertEquals("canons", terms.get(6));
        assertEquals("disc", terms.get(7));
        assertEquals("1", terms.get(8));

        /*
         * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg
         * Variations, Canons [Disc 1] /02 - Bach- Goldberg Variations, BWV 988 -
         * Variatio 1 A 1 Clav..flac
         */
        // title
        query = "Bach: Goldberg Variations, BWV 988 - Variatio 1 A 1 Clav.";
        terms = toTermString(query);
        assertEquals(9, terms.size());
        assertEquals("bach", terms.get(0));
        assertEquals("goldberg", terms.get(1));
        assertEquals("variations", terms.get(2));
        assertEquals("bwv", terms.get(3));
        assertEquals("988", terms.get(4));
        assertEquals("variatio", terms.get(5));
        assertEquals("1", terms.get(6));
        assertEquals("1", terms.get(7));
        assertEquals("clav", terms.get(8));

        /*
         * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice /01 - Sonata
         * Violin & Cello I. Allegro.ogg
         */
        // title
        query = "Sonata Violin & Cello I. Allegro";
        terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals("sonata", terms.get(0));
        assertEquals("violin", terms.get(1));
        assertEquals("cello", terms.get(2));
        assertEquals("i", terms.get(3));
        assertEquals("allegro", terms.get(4));

        // artist
        query = "_ID3_ARTIST_ Sarah Walker/Nash Ensemble";
        terms = toTermString(query);
        assertEquals(7, terms.size());

        /*
         * (lucene 3.0) id3_artist
         * (kuromoji) id 3 artist
         */
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("artist", terms.get(2));
        assertEquals("sarah", terms.get(3));
        assertEquals("walker", terms.get(4));
        assertEquals("nash", terms.get(5));
        assertEquals("ensemble", terms.get(6));

        // album
        query = "_ID3_ALBUM_ Ravel - Chamber Music With Voice";
        terms = toTermString(query);
        assertEquals(8, terms.size());

        /*
         * (lucene 3.0) id3_artist
         * (kuromoji) id 3 artist
         */
        assertEquals("id", terms.get(0));
        assertEquals("3", terms.get(1));
        assertEquals("album", terms.get(2));
        assertEquals("ravel", terms.get(3));
        assertEquals("chamber", terms.get(4));
        assertEquals("music", terms.get(5));
        assertEquals("with", terms.get(6));// currently not stopward
        assertEquals("voice", terms.get(7));

        /*
         * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice /01 - Sonata
         * Violin & Cello I. Allegro.ogg
         */

        // title
        query = "Sonata Violin & Cello II. Tres Vif";
        terms = toTermString(query);
        assertEquals(6, terms.size());
        assertEquals("sonata", terms.get(0));
        assertEquals("violin", terms.get(1));
        assertEquals("cello", terms.get(2));
        assertEquals("ii", terms.get(3));
        assertEquals("tres", terms.get(4));
        assertEquals("vif", terms.get(5));
    }

    public void testHyphen() {
        String query = "FULL-WIDTH.";
        List<String> terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("full", terms.get(0));// divided position
        assertEquals("width", terms.get(1));
    }

    public void testSymbol() {
        String query = "!\"#$%&\'()=~|-^\\@[`{;:]+*},.///\\<>?_\'";
        List<String> terms = toTermString(query);
        assertEquals(0, terms.size());// remove symbols
    }

    public void testHalfWidth() {
        String query = "THIS IS HALF-WIDTH SENTENCES.";
        List<String> terms = toTermString(query);
        assertEquals(5, terms.size());
        assertEquals("this", terms.get(0));// currently not stopward
        assertEquals("is", terms.get(1));// currently not stopward
        assertEquals("half", terms.get(2));
        assertEquals("width", terms.get(3));
        assertEquals("sentences", terms.get(4));
    }

    /*
     * Subsonic can avoid Stopward if it is upper case.
     * It is unclear whether it is a specification or not.
     * Jpsonic's Stopward is not case sensitive.
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
         * (lucene 3.0) jpsonic
         * (kuromoji) jpsonic s
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

    /*
     * The number is not deleted. Month and year have important meanings and are
     * often included in the song title.
     */
    public void testNumbers() {
        String query = "Olympic Games in 2020.";
        List<String> terms = toTermString(query);
        assertEquals(4, terms.size());
        assertEquals("olympic", terms.get(0));
        assertEquals("games", terms.get(1));
        assertEquals("in", terms.get(2));// currently not stopward
        assertEquals("2020", terms.get(3));// numbers are not removed
    }

    /*
     * air -> jp
     * The case is changed because syntax processing of kanji is possible.
     * However, because it is a Japanese parsing analysis,
     * it may be crowded with Chinese.
     */
    public void testAsianCharacters() {
        String query = "大丈夫";
        List<String> terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("大丈夫", terms.get(0));
    }

    /*
     * An example of correct Japanese analysis.
     */
    public void testJapaneseCharacters() {
        String query = "日本語は大丈夫";
        List<String> terms = toTermString(query);
        assertEquals(2, terms.size());
        assertEquals("日本語", terms.get(0));
        assertEquals("大丈夫", terms.get(1));
    }

  /*
   * air -> jp
   * Airsonic removes all common stops.
   * Jpsonic removes only articles.
   */
  public void testStopward() {

        /*
         * article
         */
        String query = "a an the";
        List<String> terms = toTermString(query);
        assertEquals(0, terms.size());

        /*
         * It is not included in the Java default stopword.
         * Default set as Airsonic index stop word.
         */
        query = "el la los las le les";
        terms = toTermString(query);
        assertEquals(0, terms.size());

        /*
         * Non-article in the default Stopward.
         * In cases, it may be used for song names of 2 to 3 words.
         * Stop words are essential for newspapers and documents,
         * but they are over-processed for song titles.
         */
        query = "and are as at be but by for if in into is it no not of on "
         + "or such that their then there these they this to was will with";
        terms = toTermString(query);
        assertEquals(30, terms.size());

        /*
         * Japanese Stopward.
         * Like the English stopward,
         * there may be cases of over-deletion in some cases.
         * However, unlike the English stopward.
         * Japanese Stopward is placed less frequently at the beginning of sentences.
         */
        query = "の に は を た が で て と し れ さ ある いる も する から な こと として い "
                + "や れる など なっ ない この ため その あっ よう また もの という あり まで "
                + "られ なる へ か だ これ によって により おり より による ず なり られる において "
                + "ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き "
                + "つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して "
                + "特に せる 及び これら とき では にて ほか ながら うち そして とともに ただし かつて "
                + "それぞれ または お ほど ものの に対する ほとんど と共に といった です とも"
                + " ところ ここ";// 108 (it is the number of troubles. In Buddhism.)
        terms = toTermString(query);
        assertEquals(0, terms.size());

    }

    /*
     * Seems to be able.
     */
    public void testLigature() {
        String query = "Cæsar";
        List<String> terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("caesar", terms.get(0));// substitution

        query = "cœur";
        terms = toTermString(query);
        assertEquals(1, terms.size());
        assertEquals("coeur", terms.get(0));// substitution
    }

    /*
     * From here only observing the current situation.
     * Dedicated parsing is required for complete processing.
     */
    public void testGreekAcute() {
        String query = "ΆάΈέΉήΊίΐΌόΎύΰϓΏώ";
        List<String> terms = toTermString(query);
        assertEquals(1, terms.size());// may be difficult
        assertNotEquals("ΆάΈέΉήΊίΐΌόΎύΰϓΏώ", terms.get(0));
    }

    public void testGreekAcute2() {
        String query = "ΆάΈέΉήΊίΐΌόΎύΰϓΏώ";
        List<String> terms = toTermString(Normalizer.normalize(query, java.text.Normalizer.Form.NFC));
        assertEquals(1, terms.size());// may be difficult
        assertNotEquals("ΆάΈέΉήΊίΐΌόΎύΰϓΏώ", terms.get(0));
    }

    public void testCyrillicAcute() {
        String query = "ЃѓЌќ";
        List<String> terms = toTermString(query);
        assertEquals(1, terms.size());// may be difficult
        assertNotEquals("ЃѓЌќ", terms.get(0));
    }

    public void testLatinAcuteFailPattern() {
        String query = "ÁáĀāǺǻĄą Ćć Ć̣ć̣";
        List<String> terms = toTermString(query);
        assertEquals(4, terms.size());// may be difficult
        assertEquals("aaaaaaaa", terms.get(0));
        assertEquals("cc", terms.get(1));
        assertEquals("c", terms.get(2));
        assertEquals("c", terms.get(3));
    }

    /*
     * In UAX#29, determination of non-practical word boundaries is not considered.
     * Languages ​​that use special strings require practical verification.
     */
    public void testLatinAcuteSuccessPattern() {
        String query = "Céline";
        List<String> terms = toTermString(query);
        assertEquals(1, terms.size());// no problem depending on how to input
        assertEquals("celine", terms.get(0));
    }

    public static List<String> toTermString(String str) {
        return toTermString(null, str);
    }


    /*
     * Each field has different tokenizer
     */
    public void testProperTokenizer() {

        String query = "すもももももももものうち";

        assertEquals("ID", 3, toTermString(FieldNames.ID, query).size());
        assertEquals("TITLE", 3, toTermString(FieldNames.TITLE, query).size());
        assertEquals("ALBUM", 3, toTermString(FieldNames.ALBUM, query).size());
        assertEquals("ALBUM_FULL", 1, toTermString(FieldNames.ALBUM_FULL, query).size());

        assertEquals("ARTIST", 3, toTermString(FieldNames.ARTIST, query).size());
        assertEquals("ARTIST_FULL", 1, toTermString(FieldNames.ARTIST_FULL, query).size());
        assertEquals("ARTIST_READING", 3, toTermString(FieldNames.ARTIST_READING, query).size());
        assertEquals("ARTIST_READING_HIRAGANA", 1, toTermString(FieldNames.ARTIST_READING_HIRAGANA, query).size());

        assertEquals("GENRE", 1, toTermString(FieldNames.GENRE, query).size());
        assertEquals("FOLDER", 1, toTermString(FieldNames.FOLDER, query).size());
        assertEquals("YEAR", 3, toTermString(FieldNames.YEAR, query).size());
        assertEquals("MEDIA_TYPE", 1, toTermString(FieldNames.MEDIA_TYPE, query).size());

        assertEquals("FOLDER_ID", 3, toTermString(FieldNames.FOLDER_ID, query).size());

    }

    public void testLower() {

        assertEquals("ID", "abc", toTermString(FieldNames.ID, "ABC").get(0));
        assertEquals("TITLE", "abc", toTermString(FieldNames.TITLE, "ABC").get(0));
        assertEquals("ALBUM", "abc", toTermString(FieldNames.ALBUM, "ABC").get(0));
        assertEquals("ALBUM_FULL", "abc", toTermString(FieldNames.ALBUM_FULL, "ABC").get(0));

        assertEquals("ARTIST", "abc", toTermString(FieldNames.ARTIST, "ABC").get(0));
        assertEquals("ARTIST_FULL", "abc", toTermString(FieldNames.ARTIST_FULL, "ABC").get(0));
        assertEquals("ARTIST_READING", "abc", toTermString(FieldNames.ARTIST_READING, "ABC").get(0));
        assertEquals("ARTIST_READING_HIRAGANA", "abc", toTermString(FieldNames.ARTIST_READING_HIRAGANA, "ABC").get(0));

        assertEquals("GENRE1", "abc", toTermString(FieldNames.GENRE, "ABC").get(0));
        assertEquals("FOLDER", "ABC", toTermString(FieldNames.FOLDER, "ABC").get(0));
        assertEquals("YEAR", "abc", toTermString(FieldNames.YEAR, "ABC").get(0));
        assertEquals("MEDIA_TYPE", "abc", toTermString(FieldNames.MEDIA_TYPE, "ABC").get(0));

        assertEquals("FOLDER_ID", "abc", toTermString(FieldNames.FOLDER_ID, "ABC").get(0));
    }

    public void testGenre() {
        // " " "-"
        assertEquals("GENRE1", "abc", toTermString(FieldNames.GENRE, "ABC").get(0));
        assertEquals("GENRE2", "abc", toTermString(FieldNames.GENRE, "AB C").get(0));
        assertEquals("GENRE3", "abc", toTermString(FieldNames.GENRE, "A  B C").get(0));
        assertEquals("GENRE4", "abc", toTermString(FieldNames.GENRE, "AB -C").get(0));
        assertEquals("GENRE4", "abc", toTermString(FieldNames.GENRE, "  Ab -C--").get(0));
    }

    public void testStop() {

        String query = "the";
        
        assertEquals("ID", 0, toTermString(FieldNames.ID, query).size());
        assertEquals("TITLE", 0, toTermString(FieldNames.TITLE, query).size());
        assertEquals("ALBUM", 0, toTermString(FieldNames.ALBUM, query).size());
        assertEquals("ALBUM_FULL", 0, toTermString(FieldNames.ALBUM_FULL, query).size());

        assertEquals("ARTIST", 0, toTermString(FieldNames.ARTIST, query).size());
        assertEquals("ARTIST_FULL", 0, toTermString(FieldNames.ARTIST_FULL, query).size());
        assertEquals("ARTIST_READING", 0, toTermString(FieldNames.ARTIST_READING, query).size());
        assertEquals("ARTIST_READING_HIRAGANA", 0, toTermString(FieldNames.ARTIST_READING_HIRAGANA, query).size());

        assertEquals("GENRE", 1, toTermString(FieldNames.GENRE, query).size());
        assertEquals("FOLDER", 1, toTermString(FieldNames.FOLDER, query).size());
        assertEquals("YEAR", 0, toTermString(FieldNames.YEAR, query).size());
        assertEquals("MEDIA_TYPE", 0, toTermString(FieldNames.MEDIA_TYPE, query).size());
        assertEquals("FOLDER_ID", 0, toTermString(FieldNames.FOLDER_ID, query).size());

    }
    
    public static List<String> toTermString(String field, String str) {
        List<String> result = new ArrayList<>();
        try {
            TokenStream stream = AnalyzerFactory.getInstance().getAnalyzer().tokenStream(field, new StringReader(str));
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