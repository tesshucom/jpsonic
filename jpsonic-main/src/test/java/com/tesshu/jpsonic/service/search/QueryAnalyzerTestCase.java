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

import com.tesshu.jpsonic.service.search.AnalyzerFactory;

import java.text.Normalizer;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;


import junit.framework.TestCase;


public class QueryAnalyzerTestCase extends TestCase {

  /*
   * Change Tokenizer by input when Jpsonic parses a query.
   * Use WhitespaceTokenizer for Hiragana (and blank) or Katakana (and blank).
   * 
   * (Based on the 100.0.0 implementation. Paramater will disappear in future
   * corrections)
   */
  public void testProperUseOfTokenizer() {

    // WhitespaceTokenizer
    String query = "すもももももももものうち";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());

    // WhitespaceTokenizer
    query = "すももも　ももも　ももの　うち";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(3, terms.size());// including stopward

    // JapaneseTokenizer
    query = "す桃も桃も桃のうち";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(4, terms.size());
    assertEquals("す*", terms.get(0));
    assertEquals("桃*", terms.get(1));
    assertEquals("桃*", terms.get(2));
    assertEquals("桃*", terms.get(3));

    // JapaneseTokenizer
    query = "Both plum and peach are peach.";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(6, terms.size());
    assertEquals("both*", terms.get(0));
    assertEquals("plum*", terms.get(1));
    assertEquals("and*", terms.get(2));
    assertEquals("peach*", terms.get(3));
    assertEquals("are*", terms.get(4));
    assertEquals("peach*", terms.get(5));
  }

  /*
   * Fake pattern.
   * Whether the word separation for Hiragana or Katakana only is correct 
   * depends on the analysis mode of the JapaneseTokenizer
   * and the accuracy of the dictionary.
   * 
   * In the default configuration, the analysis may or may not be correct.
   */
  public void testHiraganaWithJapaneseTokenizer() {

    // JapaneseTokenizer
    String query = "い";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("い", 0, terms.size());// stopward

    query = "いき";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いき", 1, terms.size());

    query = "いきも";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきも", 1, terms.size());

    query = "いきもの";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきもの", 1, terms.size());

    query = "いきものが";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものが", 1, terms.size());
    assertEquals("いきものが", "いき*", terms.get(0));

    query = "いきものがか";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものがか", 1, terms.size());
    assertEquals("いきものがか", "いき*", terms.get(0));

    query = "いきものがかり";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer("");
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものがかり", 2, terms.size());
    assertEquals("いき*", terms.get(0));
    assertEquals("かり*", terms.get(1));
  }

  /*
   * Expected behavior.
   * If the input is only hiragana (katakana), suppress token analysis of the query.
   * 
   * The search for hiragana (katakana) is not solved with queries alone. Create a
   * full-text field in the index.
   */
  public void testHiraganaWithWhitespaceTokenizer() {

    String query = "い";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("い", 0, terms.size());// stopward

    query = "いき";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いき", 1, terms.size());

    query = "いきも";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきも", 1, terms.size());

    query = "いきもの";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきもの", 1, terms.size());

    query = "いきものが";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものが", 1, terms.size());

    query = "いきものがか";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものがか", 1, terms.size());

    query = "いきものがかり";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("いきものがかり", 1, terms.size());
  }

  /*
   * JapaneseTokenizer is used for compound words of hiragana and katana.
   * (A certain degree of analysis can be expected)
   */
  public void testCompoundHiraganaKatana() {
    String query = "イキモノがかり";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("イキモノがかり", 2, terms.size());// 生き物係り
    assertEquals("イキモノ*", terms.get(0));
    assertEquals("がかり*", terms.get(1));
  }

  /*
   * WhitespaceTokenizer is used for word of katana(Expect not to split).
   */
  public void testKatanaOnly() {
    String query = "イキモノガカリ";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("イキモノガカリ", 1, terms.size());// イキモノガカリ
  }

  /*
   * Use JapaneseTokenizer to analyze English queries.
   * UAX # 29 level token analysis is expected.
   */
  public void testEnglishWithJapaneseTokenizer() {
    // JapaneseTokenizer
    String query = "Both";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both", 1, terms.size());

    query = "Both plum";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both plum", 2, terms.size());

    query = "Both plum and";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both plum and", 3, terms.size());

    query = "Both plum and peach";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both plum and peach", 4, terms.size());

    query = "Both plum and peach are";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both plum and peach are", 5, terms.size());

    query = "Both plum and peach are peach.";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals("Both plum and peach are peach.", 6, terms.size());
  }

    public void testResource() {

    /*
     * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg
     * Variations, Canons [Disc 1] /01 - Bach- Goldberg Variations, BWV 988 -
     * Aria.flac
     * 
     */

    // title
    String query = "Bach: Goldberg Variations, BWV 988 - Aria";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(6, terms.size());
    assertEquals("bach*", terms.get(0));
    assertEquals("goldberg*", terms.get(1));
    assertEquals("variations*", terms.get(2));
    assertEquals("bwv*", terms.get(3));
    assertEquals("988*", terms.get(4));
    assertEquals("aria*", terms.get(5));

    // artist
    query = "_ID3_ARTIST_ Céline Frisch: Café Zimmermann";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(7, terms.size());

    /*
     * (lucene 3.0) id3_artist
     * (UAX#29) id 3 artist
     */
    assertEquals("id*", terms.get(0));
    assertEquals("3*", terms.get(1));
    assertEquals("artist*", terms.get(2));
    assertEquals("celine*", terms.get(3));
    assertEquals("frisch*", terms.get(4));
    assertEquals("cafe*", terms.get(5));
    assertEquals("zimmermann*", terms.get(6));

    // album
    query = "_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(9, terms.size());

    /*
     * (lucene 3.0) id3_artist
     * (UAX#29) id 3 artist
     */
    assertEquals("id*", terms.get(0));
    assertEquals("3*", terms.get(1));
    assertEquals("album*", terms.get(2));
    assertEquals("bach*", terms.get(3));
    assertEquals("goldberg*", terms.get(4));
    assertEquals("variations*", terms.get(5));
    assertEquals("canons*", terms.get(6));
    assertEquals("disc*", terms.get(7));
    assertEquals("1*", terms.get(8));

    /*
     * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg
     * Variations, Canons [Disc 1] /02 - Bach- Goldberg Variations, BWV 988 -
     * Variatio 1 A 1 Clav..flac
     */
    // title
    query = "Bach: Goldberg Variations, BWV 988 - Variatio 1 A 1 Clav.";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(9, terms.size());
    assertEquals("bach*", terms.get(0));
    assertEquals("goldberg*", terms.get(1));
    assertEquals("variations*", terms.get(2));
    assertEquals("bwv*", terms.get(3));
    assertEquals("988*", terms.get(4));
    assertEquals("variatio*", terms.get(5));
    assertEquals("1*", terms.get(6));
    assertEquals("1*", terms.get(7));
    assertEquals("clav*", terms.get(8));

    /*
     * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice /01 - Sonata
     * Violin & Cello I. Allegro.ogg
     */
    // title
    query = "Sonata Violin & Cello I. Allegro";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(5, terms.size());
    assertEquals("sonata*", terms.get(0));
    assertEquals("violin*", terms.get(1));
    assertEquals("cello*", terms.get(2));
    assertEquals("i*", terms.get(3));
    assertEquals("allegro*", terms.get(4));

    // artist
    query = "_ID3_ARTIST_ Sarah Walker/Nash Ensemble";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(7, terms.size());

    /*
     * (lucene 3.0) id3_artist
     * (UAX#29) id 3 artist
     */
    assertEquals("id*", terms.get(0));
    assertEquals("3*", terms.get(1));
    assertEquals("artist*", terms.get(2));
    assertEquals("sarah*", terms.get(3));
    assertEquals("walker*", terms.get(4));
    assertEquals("nash*", terms.get(5));
    assertEquals("ensemble*", terms.get(6));

    // album
    query = "_ID3_ALBUM_ Ravel - Chamber Music With Voice";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(8, terms.size());

    /*
     * (lucene 3.0) id3_artist
     * (UAX#29) id 3 artist
     */
    assertEquals("id*", terms.get(0));
    assertEquals("3*", terms.get(1));
    assertEquals("album*", terms.get(2));
    assertEquals("ravel*", terms.get(3));
    assertEquals("chamber*", terms.get(4));
    assertEquals("music*", terms.get(5));
    assertEquals("with*", terms.get(6));// currently not stopward
    assertEquals("voice*", terms.get(7));

    /*
     * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice /01 - Sonata
     * Violin & Cello I. Allegro.ogg
     */

    // title
    query = "Sonata Violin & Cello II. Tres Vif";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(6, terms.size());
    assertEquals("sonata*", terms.get(0));
    assertEquals("violin*", terms.get(1));
    assertEquals("cello*", terms.get(2));
    assertEquals("ii*", terms.get(3));
    assertEquals("tres*", terms.get(4));
    assertEquals("vif*", terms.get(5));
  }

  public void testHyphen() {
    String query = "FULL-WIDTH.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(2, terms.size());
    assertEquals("full*", terms.get(0));// divided position
    assertEquals("width*", terms.get(1));
  }

  public void testSymbol() {
    String query = "!\"#$%&\'()=~|-^\\@[`{;:]+*},.///\\<>?_\'";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(0, terms.size());// remove symbols
  }

  public void testHalfWidth() {
    String query = "THIS IS HALF-WIDTH SENTENCES.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(5, terms.size());
    assertEquals("this*", terms.get(0));// currently not stopward
    assertEquals("is*", terms.get(1));// currently not stopward
    assertEquals("half*", terms.get(2));
    assertEquals("width*", terms.get(3));
    assertEquals("sentences*", terms.get(4));
  }

  public void testFullWidth() {
    String query = "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(5, terms.size());
    assertEquals("this*", terms.get(0));// removal target is ignored
    assertEquals("is*", terms.get(1));
    assertEquals("full*", terms.get(2));
    assertEquals("width*", terms.get(3));
    assertEquals("sentences*", terms.get(4));
  }

  /*
   * air -> jp (lucene 3.0 -> lucene 3.1 and above)
   */
  public void testPossessiveCase() {
    String query = "This is Jpsonic's analysis.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(5, terms.size());

    /*
     * (lucene 3.0) airsonic
     * (UAX#29) airsonic s
     */
    assertEquals("this*", terms.get(0));// currently not stopward
    assertEquals("is*", terms.get(1));// currently not stopward
    assertEquals("jpsonic*", terms.get(2));// removal of apostrophes
    assertEquals("s*", terms.get(3)); // "s" remain.
    assertEquals("analysis*", terms.get(4));
  }

  public void testPastParticiple() {
    String query = "This is formed with a form of the verb \"have\" and a past participl.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(11, terms.size());
    assertEquals("this*", terms.get(0));// currently not stopward
    assertEquals("is*", terms.get(1));// currently not stopward
    assertEquals("formed*", terms.get(2));// leave passive / not "form"
    assertEquals("with*", terms.get(3));// currently not stopward
    assertEquals("form*", terms.get(4));
    assertEquals("of*", terms.get(5));// currently not stopward
    assertEquals("verb*", terms.get(6));
    assertEquals("have*", terms.get(7));
    assertEquals("and*", terms.get(8));// currently not stopward
    assertEquals("past*", terms.get(9));
    assertEquals("participl*", terms.get(10));
  }

  public void testNumeral() {
    String query = "books boxes cities leaves men glasses";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(6, terms.size());
    assertEquals("books*", terms.get(0));// leave numeral / not singular
    assertEquals("boxes*", terms.get(1));
    assertEquals("cities*", terms.get(2));
    assertEquals("leaves*", terms.get(3));
    assertEquals("men*", terms.get(4));
    assertEquals("glasses*", terms.get(5));
  }

  public void testNumbers() {
    String query = "Olympic Games in 2020.";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(4, terms.size());
    assertEquals("olympic*", terms.get(0));
    assertEquals("games*", terms.get(1));
    assertEquals("in*", terms.get(2));// currently not stopward
    assertEquals("2020*", terms.get(3));// numbers are not removed
  }

  /*
   * air -> jp
   * The case is changed because syntax processing of kanji is possible.
   * However, because it is a Japanese parsing analysis, it may be crowded with
   * Chinese.
   */
  public void testAsianCharacters() {
    String query = "大丈夫";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());
    assertEquals("大丈夫*", terms.get(0));
  }

  /*
   * An example of correct Japanese analysis.
   */
  public void testJapaneseCharacters() {
    String query = "日本語は大丈夫";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(2, terms.size());
    assertEquals("日本語*", terms.get(0));
    assertEquals("大丈夫*", terms.get(1));
  }

  /*
   * air -> jp
   * Airsonic removes all common stops.
   * Jpsonic removes only articles.
   */
  public void testStopward() {
    String query = "a an the";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(0, terms.size());

    query = "el la los las le les";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(0, terms.size());

    query = "and are as at be but by for if in into is it no not of on "
        + "or such that their then there these they this to was will with";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(30, terms.size());

    /*
     * This server is different in character from general document search.
     * Like the English stopward, there may be cases of over-deletion in some cases.
     * (However, unlike the English stopward, it is placed less frequently at the
     * beginning of sentences)
     */
    query = "の に は を た が で て と し れ さ ある いる も する から な こと として い "
        + "や れる など なっ ない この ため その あっ よう また もの という あり まで "
        + "られ なる へ か だ これ によって により おり より による ず なり られる において "
        + "ば なかっ なく しかし について せ だっ その後 できる それ う ので なお のみ でき き "
        + "つ における および いう さらに でも ら たり その他 に関する たち ます ん なら に対して "
        + "特に せる 及び これら とき では にて ほか ながら うち そして とともに ただし かつて "
        + "それぞれ または お ほど ものの に対する ほとんど と共に といった です とも"
        + " ところ ここ";// 108 (it is the number of troubles. In Buddhism.)
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(0, terms.size());

  }

  public void testLigature() {
    String query = "Cæsar";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());
    assertEquals("caesar*", terms.get(0));// substitution

    query = "cœur";
    analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());
    assertEquals("coeur*", terms.get(0));// substitution
  }

  /*
   * From here only observing the current situation.
   * Dedicated parsing is required for complete processing.
   */
  public void testGreekAcute() {
    String query = "ΆάΈέΉήΊίΐΌόΎύΰϓΏώ";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());// may be difficult
    assertNotEquals("ΆάΈέΉήΊίΐΌόΎύΰϓΏώ*", terms.get(0));
  }

  public void testGreekAcute2() {
    String query = "ΆάΈέΉήΊίΐΌόΎύΰϓΏώ";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer,
        Normalizer.normalize(query, java.text.Normalizer.Form.NFC));
    assertEquals(1, terms.size());// may be difficult
    assertNotEquals("ΆάΈέΉήΊίΐΌόΎύΰϓΏώ*", terms.get(0));
  }

  public void testCyrillicAcute() {
    String query = "ЃѓЌќ";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());// may be difficult
    assertNotEquals("ЃѓЌќ*", terms.get(0));
  }

  public void testLatinAcuteFailPattern() {
    String query = "ÁáĀāǺǻĄą Ćć Ć̣ć̣";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(4, terms.size());// may be difficult
    assertEquals("aaaaaaaa*", terms.get(0));
    assertEquals("cc*", terms.get(1));
    assertEquals("c*", terms.get(2));
    assertEquals("c*", terms.get(3));
  }

  /*
   * In UAX # 29, determination of non-practical word boundaries is not considered.
   * Languages ​​that use special strings require practical verification.
   */
  public void testLatinAcuteSuccessPattern() {
    String query = "Céline";
    Analyzer analyzer = AnalyzerFactory.getInstance().getQueryAnalyzer(query);
    List<String> terms = AnalyzerUtil.toTermString(analyzer, query);
    assertEquals(1, terms.size());// no problem depending on how to input
    assertEquals("celine*", terms.get(0));
  }

}