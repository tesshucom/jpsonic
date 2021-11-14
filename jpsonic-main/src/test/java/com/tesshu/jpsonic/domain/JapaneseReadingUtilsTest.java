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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.domain;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JapaneseReadingUtilsTest {

    private SettingsService settingsService;

    private JapaneseReadingUtils utils;

    @BeforeEach
    public void setup() {
        settingsService = mock(SettingsService.class);
        String language = "ja";
        String country = "jp";
        String variant = "";
        Mockito.when(settingsService.getLocale()).thenReturn(new Locale(language, country, variant));
        Mockito.when(settingsService.isReadGreekInJapanese()).thenReturn(true);
        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
        utils = new JapaneseReadingUtils(settingsService);
    }

    @AfterEach
    public void afterAll() {
        utils.clear();
    }

    @Test
    @Order(1)
    void testIsPunctuation() {
        assertFalse(JapaneseReadingUtils.isPunctuation('a'));
        assertTrue(JapaneseReadingUtils.isPunctuation('*'));
        assertTrue(JapaneseReadingUtils.isPunctuation('+'));
        assertTrue(JapaneseReadingUtils.isPunctuation('-'));
        assertTrue(JapaneseReadingUtils.isPunctuation(' '));
        assertTrue(JapaneseReadingUtils.isPunctuation('\n'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\t'));
        assertTrue(JapaneseReadingUtils.isPunctuation(','));
        assertTrue(JapaneseReadingUtils.isPunctuation('.'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\''));
        assertTrue(JapaneseReadingUtils.isPunctuation('`'));
        assertTrue(JapaneseReadingUtils.isPunctuation('\"'));
        assertTrue(JapaneseReadingUtils.isPunctuation('。'));
        assertTrue(JapaneseReadingUtils.isPunctuation('、'));
        assertTrue(JapaneseReadingUtils.isPunctuation('('));
        assertTrue(JapaneseReadingUtils.isPunctuation(')'));
        assertTrue(JapaneseReadingUtils.isPunctuation('['));
        assertTrue(JapaneseReadingUtils.isPunctuation(']'));
        assertTrue(JapaneseReadingUtils.isPunctuation('【'));
        assertTrue(JapaneseReadingUtils.isPunctuation('】'));
    }

    @Test
    @Order(2)
    void testRemovePunctuationFromJapaneseReading() {
        assertNull(utils.removePunctuationFromJapaneseReading(null));
        assertEquals("あいうえお", utils.removePunctuationFromJapaneseReading("あいうえお"));
        assertEquals("アイウエオ", utils.removePunctuationFromJapaneseReading("アイウエオ"));
        assertEquals("ｱｲｳｴｵ", utils.removePunctuationFromJapaneseReading("ｱｲｳｴｵ"));
        assertEquals("ｱｲｳｴｵ", utils.removePunctuationFromJapaneseReading("｢ｱｲｳｴｵ｣"));
        assertEquals("ァィゥェォ", utils.removePunctuationFromJapaneseReading("ァィゥェォ"));
        assertEquals("アルファベータガンマ", utils.removePunctuationFromJapaneseReading("アルファベータガンマ"));
        assertEquals("ツンク", utils.removePunctuationFromJapaneseReading("ツンク♂"));
        assertEquals("イヌトネコ", utils.removePunctuationFromJapaneseReading("イヌトネコ"));
        assertEquals(" ｢｣()()[][];!!??##123", utils.removePunctuationFromJapaneseReading(" ｢｣()()[][];!!??##123"));
        assertEquals("コンピューター", utils.removePunctuationFromJapaneseReading("コンピューター"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイ～ウエ"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイウエ～"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("～アイウエ"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("ア～イ～ウ～エ"));
        assertEquals("     ", utils.removePunctuationFromJapaneseReading("     "));
        assertEquals("[Disc 3]", utils.removePunctuationFromJapaneseReading("[Disc 3]"));
        assertEquals("Best ～first things～", utils.removePunctuationFromJapaneseReading("Best ～first things～"));
        assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                utils.removePunctuationFromJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertEquals("Dvořák: Symphonies #7-9", utils.removePunctuationFromJapaneseReading("Dvořák: Symphonies #7-9"));
        assertEquals("フクヤママサハル", utils.removePunctuationFromJapaneseReading("フクヤママサハル"));
        assertEquals("サシハラ莉乃", utils.removePunctuationFromJapaneseReading("サシハラ莉乃"));
        assertEquals("倖タ來ヒツジ", utils.removePunctuationFromJapaneseReading("倖タ來ヒツジ"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ ローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
        assertEquals("ｼﾝﾃﾞｨﾛｰﾊﾟｰ", utils.removePunctuationFromJapaneseReading("ｼﾝﾃﾞｨ･ﾛｰﾊﾟｰ"));
    }

    @Test
    @Order(3)
    void testNormalize() throws ExecutionException {
        assertEquals("あいうえお", utils.normalize("あいうえお"));
        assertEquals("アイウエオ", utils.normalize("アイウエオ"));
        assertEquals("ァィゥェォ", utils.normalize("ァィゥェォ"));
        assertEquals("ァィゥェォ", utils.normalize("ｧｨｩｪｫ"));
        assertEquals("アイウエオ", utils.normalize("ｱｲｳｴｵ"));
        assertEquals("亜伊鵜絵尾", utils.normalize("亜伊鵜絵尾"));
        assertEquals("ABCDE", utils.normalize("ABCDE"));
        assertEquals("ABCDE", utils.normalize("ＡＢＣＤＥ"));
        assertEquals("αβγ", utils.normalize("αβγ"));
        assertEquals("つんく♂", utils.normalize("つんく♂"));
        assertEquals("bad communication", utils.normalize("bad communication"));
        assertEquals("BAD COMMUNICATION", utils.normalize("BAD COMMUNICATION"));
        assertEquals("BAD COMMUNICATION", utils.normalize("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals("犬とネコ", utils.normalize("犬とネコ"));
        assertEquals("読み", utils.normalize("読み"));
        assertEquals("(読み)", utils.normalize("(読み)"));
        assertEquals(" ｢｣()()[][];!!??##123", utils.normalize("　「」（）()［］[]；！!？?＃#１２３"));
        assertEquals("Cæsar", utils.normalize("Cæsar"));
        assertEquals("The Alfee", utils.normalize("The Alfee"));
        assertEquals("コンピューター", utils.normalize("コンピューター"));
        assertEquals("あい～うえ", utils.normalize("あい～うえ"));
        assertEquals("あいうえ～", utils.normalize("あいうえ～"));
        assertEquals("～あいうえ", utils.normalize("～あいうえ"));
        assertEquals("あ～い～う～え", utils.normalize("あ～い～う～え"));
        assertEquals("     ", utils.normalize("　　　　　"));
        assertEquals("Best ～first things～", utils.normalize("Best ～first things～"));
        assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                utils.normalize("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertEquals("Dvořák: Symphonies #7-9", utils.normalize("Dvořák: Symphonies #7-9"));
        assertEquals("福山雅治", utils.normalize("福山雅治"));
        assertEquals("サシハラ莉乃", utils.normalize("サシハラ莉乃"));
        assertEquals("倖田來未", utils.normalize("倖田來未"));
    }

    @Test
    @Order(4)
    void testRomanize() throws ExecutionException {
        assertEquals("hannari", JapaneseReadingUtils.romanize("han'nari"));
        assertEquals("a~b~c", JapaneseReadingUtils.romanize("~a~b~c"));
        assertEquals("aiueo", JapaneseReadingUtils.romanize("~a~i~u~e~o"));
        assertEquals("attakai", JapaneseReadingUtils.romanize("a~tsutakai"));
        assertEquals("a", JapaneseReadingUtils.romanize("~a"));
        assertEquals("a~", JapaneseReadingUtils.romanize("a~"));
        assertEquals("~", JapaneseReadingUtils.romanize("~"));
        assertEquals("", JapaneseReadingUtils.romanize(""));
        assertEquals("Aa", JapaneseReadingUtils.romanize("Ā"));
        assertEquals("Ii", JapaneseReadingUtils.romanize("Ī"));
        assertEquals("Uu", JapaneseReadingUtils.romanize("Ū"));
        assertEquals("Ee", JapaneseReadingUtils.romanize("Ē"));
        assertEquals("Oo", JapaneseReadingUtils.romanize("Ō"));
        assertEquals("a", JapaneseReadingUtils.romanize("ā"));
        assertEquals("i", JapaneseReadingUtils.romanize("ī"));
        assertEquals("u", JapaneseReadingUtils.romanize("ū"));
        assertEquals("e", JapaneseReadingUtils.romanize("ē"));
        assertEquals("o", JapaneseReadingUtils.romanize("ō"));
    }

    @Test
    @Order(5)
    void testIsJapaneseReadable() {

        assertTrue(utils.isJapaneseReadable("abc あいう"));
        assertTrue(utils.isJapaneseReadable("あいうえお"));
        assertTrue(utils.isJapaneseReadable("アイウエオ"));
        assertTrue(utils.isJapaneseReadable("ァィゥェォ"));
        assertTrue(utils.isJapaneseReadable("ｧｨｩｪｫ"));
        assertTrue(utils.isJapaneseReadable("ｱｲｳｴｵ"));
        assertTrue(utils.isJapaneseReadable("亜伊鵜絵尾"));
        assertTrue(utils.isJapaneseReadable("ＡＢＣＤＥ"));
        assertTrue(utils.isJapaneseReadable("αβγ"));
        assertTrue(utils.isJapaneseReadable("つんく♂"));
        assertTrue(utils.isJapaneseReadable("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertTrue(utils.isJapaneseReadable("犬とネコ"));
        assertTrue(utils.isJapaneseReadable("読み"));
        assertTrue(utils.isJapaneseReadable("(読み)"));
        assertTrue(utils.isJapaneseReadable("　「」（）()［］[]；！!？?＃#１２３"));
        assertTrue(utils.isJapaneseReadable("コンピューター"));
        assertTrue(utils.isJapaneseReadable("あい～うえ"));
        assertTrue(utils.isJapaneseReadable("あいうえ～"));
        assertTrue(utils.isJapaneseReadable("～あいうえ"));
        assertTrue(utils.isJapaneseReadable("あ～い～う～え"));
        assertTrue(utils.isJapaneseReadable("　　　　　"));
        assertTrue(utils.isJapaneseReadable("福山雅治"));
        assertTrue(utils.isJapaneseReadable("サシハラ莉乃"));
        assertTrue(utils.isJapaneseReadable("倖田來未"));
        assertTrue(utils.isJapaneseReadable("奥田民生"));
        assertTrue(utils.isJapaneseReadable("Best ～first things～"));

        assertFalse(utils.isJapaneseReadable(null));
        assertFalse(utils.isJapaneseReadable("ABCDE"));
        assertFalse(utils.isJapaneseReadable("bad communication"));
        assertFalse(utils.isJapaneseReadable("BAD COMMUNICATION"));
        assertFalse(utils.isJapaneseReadable("Cæsar"));
        assertFalse(utils.isJapaneseReadable("The Alfee"));
        assertFalse(utils.isJapaneseReadable("[Disc 3]"));
        assertFalse(utils.isJapaneseReadable("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertFalse(utils.isJapaneseReadable("Dvořák: Symphonies #7-9"));

        Mockito.when(settingsService.isReadGreekInJapanese()).thenReturn(false);
        assertFalse(utils.isJapaneseReadable("αβγ"));
    }

    @Nested
    @Order(6)
    class CreateJapaneseReadingTest {

        @Test
        void testNullParam() throws ExecutionException {
            assertNull(utils.createJapaneseReading(null));
        }

        @Nested
        class NativeJapaneseTest {

            @Test
            void testCreateReading() throws ExecutionException {

                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());
                Mockito.when(settingsService.isReadGreekInJapanese()).thenReturn(true);

                /*
                 * Kuromoji will read the full-width alphabet in Japanese. ＢＢＣ(It's not bbc but ビービーシー.) When this is
                 * done in the field of Japanese music, it is often not very good. This conversion is suppressed in
                 * Jpsonic. Full-width alphabets will not been read in Japanese.
                 */

                assertEquals("アイウエオ", utils.createJapaneseReading("The あいうえお"));
                assertEquals("アイウエオ", utils.createJapaneseReading("あいうえお"));
                assertEquals("アイウエオ", utils.createJapaneseReading("アイウエオ"));
                assertEquals("ァィゥェォ", utils.createJapaneseReading("ァィゥェォ"));
                assertEquals("ァィゥェォ", utils.createJapaneseReading("ｧｨｩｪｫ"));
                assertEquals("アイウエオ", utils.createJapaneseReading("ｱｲｳｴｵ"));
                assertEquals("アイウエオ", utils.createJapaneseReading("亜伊鵜絵尾"));
                assertEquals("ABCDE", utils.createJapaneseReading("ABCDE"));
                assertEquals("ABCDE", utils.createJapaneseReading("ＡＢＣＤＥ"));
                assertEquals("アルファベータガンマ", utils.createJapaneseReading("αβγ"));
                assertEquals("ツンク♂", utils.createJapaneseReading("つんく♂"));
                assertEquals("bad communication", utils.createJapaneseReading("bad communication"));
                assertEquals("BAD COMMUNICATION", utils.createJapaneseReading("BAD COMMUNICATION"));
                assertEquals("BAD COMMUNICATION", utils.createJapaneseReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
                assertEquals("イヌトネコ", utils.createJapaneseReading("犬とネコ"));
                assertEquals(" ｢｣()()[][];!!??##123", utils.createJapaneseReading("　「」（）()［］[]；！!？?＃#１２３"));
                assertEquals("Cæsar", utils.createJapaneseReading("Cæsar"));
                assertEquals("Alfee", utils.createJapaneseReading("The Alfee"));
                assertEquals("コンピューター", utils.createJapaneseReading("コンピューター"));
                assertEquals("アイ～ウエ", utils.createJapaneseReading("あい～うえ"));
                assertEquals("アイウエ～", utils.createJapaneseReading("あいうえ～"));
                assertEquals("～アイウエ", utils.createJapaneseReading("～あいうえ"));
                assertEquals("ア～イ～ウ～エ", utils.createJapaneseReading("あ～い～う～え"));
                assertEquals("     ", utils.createJapaneseReading("　　　　　"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Best ～first things～", utils.createJapaneseReading("Best ～first things～"));
                assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                        utils.createJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
                assertEquals("Dvořák: Symphonies #7-9", utils.createJapaneseReading("Dvořák: Symphonies #7-9"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("フクヤママサハル", utils.createJapaneseReading("福山雅治")); // Readable case

                assertEquals("サシハラ莉乃", utils.createJapaneseReading("サシハラ莉乃")); // Unreadable case
                assertEquals("倖タ來ヒツジ", utils.createJapaneseReading("倖田來未")); // Unreadable case
                assertEquals("オクダ ミンセイ", utils.createJapaneseReading("奥田　民生")); // Unreadable case
            }

        }

        @Nested
        class RomanizedJapaneseTestTest {

            @Test
            void testReading1() throws ExecutionException {

                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                Mockito.when(settingsService.isReadGreekInJapanese()).thenReturn(false);

                assertEquals("Aiueo", utils.createJapaneseReading("The あいうえお"));
                assertEquals("Aiueo", utils.createJapaneseReading("あいうえお"));
                assertEquals("Aiueo", utils.createJapaneseReading("アイウエオ"));
                assertEquals("aiueo", utils.createJapaneseReading("ァィゥェォ"));
                assertEquals("aiueo", utils.createJapaneseReading("ｧｨｩｪｫ"));
                assertEquals("Aiueo", utils.createJapaneseReading("ｱｲｳｴｵ"));
                assertEquals("Aiu-eo", utils.createJapaneseReading("亜伊鵜絵尾"));
                assertEquals("ABCDE", utils.createJapaneseReading("ABCDE"));
                assertEquals("ABCDE", utils.createJapaneseReading("ＡＢＣＤＥ"));
                assertEquals("ΑβΓ", utils.createJapaneseReading("αβΓ"));
                assertEquals("Tsunku♂", utils.createJapaneseReading("つんく♂"));
                assertEquals("Bad communication", utils.createJapaneseReading("bad communication"));
                assertEquals("BAD COMMUNICATION", utils.createJapaneseReading("BAD COMMUNICATION"));
                assertEquals("BAD COMMUNICATION", utils.createJapaneseReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
                assertEquals("Inu to Neko", utils.createJapaneseReading("犬とネコ"));
                assertEquals(" ｢｣()()[][];!!??##123", utils.createJapaneseReading("　「」（）()［］[]；！!？?＃#１２３"));
                assertEquals("Cæsar", utils.createJapaneseReading("Cæsar"));
                assertEquals("Alfee", utils.createJapaneseReading("The Alfee"));
                assertEquals("Konpyuta", utils.createJapaneseReading("コンピューター"));
                assertEquals("Ai～ue", utils.createJapaneseReading("あい～うえ"));
                assertEquals("Aiue～", utils.createJapaneseReading("あいうえ～"));
                assertEquals("～aiue", utils.createJapaneseReading("～あいうえ"));
                assertEquals("A～i～u ～e", utils.createJapaneseReading("あ～い～う～え"));
                assertEquals("     ", utils.createJapaneseReading("　　　　　"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Best ～first things～", utils.createJapaneseReading("Best ～first things～"));
                assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                        utils.createJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
                // Normalized in the 'reading' field
                assertEquals("Dvorak: Symphonies #7-9", utils.createJapaneseReading("Dvořák: Symphonies #7-9"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Fukuyamamasaharu", utils.createJapaneseReading("福山雅治")); // Readable case

                /*
                 * Unreadable (rare) case. Not in the dictionary. No problem. If we don't know it in advance, we won't
                 * know if even the Japanese can read it.
                 */
                assertEquals("Sashihara莉乃", utils.createJapaneseReading("サシハラ莉乃"));
                assertEquals("倖ta來hitsuji", utils.createJapaneseReading("倖田來未"));

                /*
                 * Unreadable case. The reading of the first candidate is different from what we expected. Not 'Minsei'
                 * but 'Tamio'. This is often the case in a person's name. The names of entertainers and younger
                 * generations may not be resolved by dictionaries. No additional dictionaries are used. Because it is
                 * reasonable to quote from CDDB.
                 */
                assertEquals("Okuda Minsei", utils.createJapaneseReading("奥田　民生"));

                /*
                 * Unreadable case. The reading of the first candidate is different from what we expected. Not
                 * 'Tsuge-rasetai' but 'Koku-rasetai'. 'Koku-rasetai' is a slang.
                 */
                assertEquals("Kagu ya Sama wa Tsuge-rasetai?", utils.createJapaneseReading("かぐや様は告らせたい?"));

                /*
                 * Unreadable case. Not 'Kyaputentsubasa' but 'Captain Tsubasa'. Romaji used overseas seems to actively
                 * use English for words that can be replaced with English. This is not possible with current
                 * morphological analyzers. This case will be reasonable to quote from CDDB.
                 */
                assertEquals("Kyaputentsubasa", utils.createJapaneseReading("キャプテン翼"));
            }

            @Test
            void testReading2() throws ExecutionException {

                /*
                 * Jpsonic's romanization features is an improved version of the Hepburn romanization, and the notation
                 * of MyAnimeList is used as a reference. Due to the nature of Japanese, a general dictionary alone
                 * cannot cover everything, but it can provide a relatively natural conversion than a simple exchange
                 * like ICU. The correct answer rate is expected to be 90% or more, and if the user feels uncomfortable,
                 * it can be corrected by updating the tag information.
                 */

                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                Mockito.when(settingsService.isReadGreekInJapanese()).thenReturn(false);

                assertEquals("Kimi no Na wa", utils.createJapaneseReading("君の名は"));

                // INTERJECTION
                assertEquals("Sa Utage no Hajimarida", utils.createJapaneseReading("さぁ宴の始まりだ"));

                // SYMBOL:COMMA, PERIOD: Not 'Doragon' but 'Dragon'.
                assertEquals("Doragon, Ie wo Kau.", utils.createJapaneseReading("ドラゴン、家を買う。"));
                // SYMBOL&capitalize
                assertEquals("Tsunoda☆Hiro", utils.createJapaneseReading("つのだ☆ひろ"));

                // NOUN: SUFFIX
                assertEquals("Tonari no Kaibutsu-kun", utils.createJapaneseReading("となりの怪物くん"));
                assertEquals("Denki-gai no Honya-san", utils.createJapaneseReading("デンキ街の本屋さん"));

                // (small tsu)
                assertEquals("Mirai, Kaete Mitakunattayo!", utils.createJapaneseReading("みらい、変えてみたくなったよ!"));

                // SENTENCE_ENDING_PARTICLE, POSTPOSITIONAL_PARTICLE, MULTI_PARTICLE
                assertEquals("Chippokenajibun ga Doko e Tobidaserukana",
                        utils.createJapaneseReading("ちっぽけな自分がどこへ飛び出せるかな"));

                // ADVERB
                assertEquals("(nantoka Naru-sa to ) Ah! Hajimeyou",
                        utils.createJapaneseReading("(なんとかなるさと) Ah! はじめよう"));

                // (small tsu)
                assertEquals("Issaigassaibonyona", utils.createJapaneseReading("一切合切凡庸な"));

                // ADVERBIAL_PARTICLE, SENTENCE_ENDING_PARTICLE
                assertEquals("Anataja Wakaranaikamo Ne", utils.createJapaneseReading("あなたじゃ分からないかもね"));

                // (small tsu)
                assertEquals("Gatchaman", utils.createJapaneseReading("ガッチャマン"));
                assertEquals("Hatchaku-eki", utils.createJapaneseReading("発着駅"));

                // (small tsu) needs romanize
                assertEquals("Minna Maruta wa Mottana!!", utils.createJapaneseReading("みんな 丸太は持ったな!!"));
                assertEquals("Mottamotta", utils.createJapaneseReading("持った持った"));
                assertEquals("Somosan Seppa", utils.createJapaneseReading("そもさん せっぱっ"));
                assertEquals("Daibu Hatcha Keta", utils.createJapaneseReading("大分はっちゃけた"));
            }
        }
    }

    @Documented
    private @interface CreateReadingDecisions {
        @interface Conditions {
            @interface Name {
                @interface StartWithAlpha {
                    @interface Y {
                    }

                    @interface N {
                    }
                }
            }

            @interface Sort {
                @interface StartWithAlpha {
                    @interface Y {
                    }

                    @interface N {
                    }
                }

                @interface JapaneseReadable {
                    @interface Y {
                    }

                    @interface N {
                    }
                }
            }
        }

        @interface Result {
            @interface NameDirived {
            }

            @interface SortDirived {
            }

        }
    }

    @Nested
    @Order(7)
    class CreateReading {

        @Nested
        class NativeJapaneseTest {

            @Test
            @Order(1)
            void testNullSort() throws ExecutionException {
                assertEquals("name", utils.createReading("name", null));
                assertEquals("ニホンゴメイ", utils.createReading("日本語名", null));
                assertEquals("ニホンゴメイ", utils.createReading("日本語名", null)); // from map
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.SortDirived
            @Test
            @Order(2)
            void c01() throws ExecutionException {
                assertEquals("It's ニホンゴノヨミ", utils.createReading("abc", "It's 日本語の読み"));
            }

            /**
             * Dirty data case. Most recently, CDDB data has been separated into national languages. Old tags created
             * based on old CDDB data may have alphabet readings registered for Japanese songs. When used by Japanese
             * people, alphabet-only sort tags are almost always meaningless or when romaji is mistakenly registered. In
             * this case, the sort tag is not used and reading is generated internally. Reading is used for sorting and
             * indexing in later processing. On the other hand, sort tags are used for the search-index.
             */
            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.N
            @CreateReadingDecisions.Result.NameDirived
            @Test
            @Order(3)
            void c02() throws ExecutionException {
                assertEquals("abc", utils.createReading("abc", "It is an English reading"));
                assertEquals("abcニホンゴ", utils.createReading("abc日本語", "It is an English reading"));
            }

            @Order(4)
            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.N
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.NameDirived
            @Test
            void c03() throws ExecutionException {
                assertEquals("abc", utils.createReading("abc", "日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.N
            @Test
            @Order(5)
            void c04() throws ExecutionException {
                assertEquals("ニホンゴノヨミ", utils.createReading("日本語名", "日本語の読み"));
            }
        }

        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class RomanizedJapaneseTest {

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.SortDirived
            @Test
            @Order(2)
            void c01() throws ExecutionException {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                assertEquals("It's Nihongo no Yomi", utils.createReading("abc", "It's 日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.N
            @CreateReadingDecisions.Result.NameDirived
            @Test
            /**
             * In the case of NATIVE_JAPANESE This case is a dirty case, but in the case of ROMANIZED_JAPANESE, it is a
             * normal process.
             */
            @Order(3)
            void c02() throws ExecutionException {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                assertEquals("It is an English reading",
                        utils.createReading("It is an English reading", "It is an English reading"));
            }

            @Order(4)
            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.N
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.NameDirived
            @Test
            void c03() throws ExecutionException {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                assertEquals("Nihongo no Yomi", utils.createReading("abc", "日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.N
            @Test
            @Order(5)
            void c04() throws ExecutionException {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                assertEquals("Nihongo no Yomi", utils.createReading("日本語名", "日本語の読み"));
                assertEquals("Nihongo-mei", utils.createReading("日本語名", null));
            }
        }

        @Nested
        class WithoutJpTest {

            @Test
            void testDonithing() throws ExecutionException {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());

                /*
                 * Nothing is done. The value has been transferred only.
                 */
                assertEquals("sort", utils.createReading("name", "sort"));
                assertEquals("ソート", utils.createReading("name", "ソート"));
            }

        }
    }

    @Test
    @Order(8)
    void testAnalyzeGenre() {

        String genreName = "現代邦楽";

        Genre genre = new Genre(genreName);
        assertEquals(genreName, genre.getName());
        assertNull(genre.getReading());

        utils.analyze(genre);
        utils.clear();
        assertEquals("ゲンダイホウガク", genre.getReading());

        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
        utils.analyze(genre);
        utils.clear();
        assertEquals("Gendaihogaku", genre.getReading());

        genre = new Genre(genreName);
        utils.analyze(genre);
        utils.clear();
        assertEquals("Gendaihogaku", genre.getReading());

        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
        utils.clear();
        genre = new Genre(genreName);
        utils.analyze(genre);
        assertEquals(genreName, genre.getReading());
    }

    @Documented
    private @interface AnalyzeMediaFileDecisions {
        @interface Conditions {
            @interface Name {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Reading {
                @interface Null {
                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface NotNull {
                }
            }
        }

        @interface Result {
            @interface Name {
                @interface Null {
                }

                @interface NotNull {
                }
            }

            @interface Reading {
                @interface Null {
                }

                @interface NotNull {
                    @interface ReadingExpected {
                    }

                    @interface ReadingAnalyzed {
                    }
                }
            }

            @interface Sort {
                @interface Null {
                }

                @interface NotNull {
                    @interface SortExpected {

                    }
                }
            }
        }
    }

    @Nested
    @Order(9)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AnalyzeMediaFileTest {

        @Nested
        class NativeJapaneseTest {

            // name
            private final String nameRaw = "The 「あいｱｲ・愛」";
            // So-called sort tag
            private final String sortRaw = "あいあい・愛";

            private final String readingExpected = "アイアイ・アイ";
            private final String sortExpected = "あいあい・愛";

            /*
             * Character string when parsed. Search system analysis and filter specifications exist separately. Note
             * that the analysis at this point does not over-cleanse so as not to affect the morphological analysis
             * specifications when creating the search index.
             */
            private final String readingAnalyzed = "｢アイアイ・アイ｣";

            private MediaFile toMediaFile(String artist, String artistSort) {
                MediaFile mediaFile = new MediaFile();
                mediaFile.setArtist(artist);
                mediaFile.setArtistSort(artistSort);
                return mediaFile;
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Null
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.Null
            @AnalyzeMediaFileDecisions.Result.Reading.Null
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            /*
             * If there is no tag.
             */
            @Order(1)
            void a01() {
                MediaFile mediaFile = toMediaFile(null, null);
                utils.analyze(mediaFile);
                assertNull(mediaFile.getArtist());
                assertNull(mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Null
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingExpected
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            /*
             * If it is rare but only the sort tag is registered. Even if the name is not registered, the name will not
             * be created from the sort tag. The user should not overlook that the tags are in an irregular state.
             */
            @Order(2)
            void a02() {
                MediaFile mediaFile = toMediaFile(null, sortRaw);
                utils.analyze(mediaFile);
                assertNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortExpected, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            /*
             * For Japanese, it is desirable that both name/sort-tag are entered.
             */
            @Order(3)
            void a03() {
                MediaFile mediaFile = toMediaFile(nameRaw, sortRaw);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortExpected, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            /*
             * For names only.Tentatively analyze readings. (In the case of Japanese, 100% correct answer is impossible,
             * so comparison and correction merging are performed after all data scans.) Sort is null, but may be
             * complemented later.
             */
            @Order(4)
            void a04() {
                MediaFile mediaFile = toMediaFile(nameRaw, null);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingAnalyzed, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }
        }

        /*
         * Even if Japanese is used for the Sort tag, the reading will be set to the Romanized string. If nothing is set
         * in the Sort tag, the reading value will be set to the Roman alphabet string created from the name. Reading is
         * used for most subsequent processing.
         */
        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class RomanizedJapaneseTest {

            // name
            private final String nameRaw = "The 「あいｱｲ・愛」";
            /*
             * So-called sort tag(This test assuming that Japanese is also used for the tag.) The result should have
             * been converted to romaji.
             */
            private final String sortRaw = "あいあい・愛";

            private final String readingExpected = "Aiai・Ai";
            private final String sortExpected = "あいあい・愛";
            private final String readingAnalyzed = "｢aiai・-ai｣";

            private MediaFile toMediaFile(String artist, String artistSort) {
                MediaFile mediaFile = new MediaFile();
                mediaFile.setArtist(artist);
                mediaFile.setArtistSort(artistSort);
                return mediaFile;
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Null
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingExpected
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(1)
            void a02() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                MediaFile mediaFile = toMediaFile(null, sortRaw);
                utils.analyze(mediaFile);
                assertNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortExpected, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(2)
            void a03a() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                MediaFile mediaFile = toMediaFile(nameRaw, sortRaw);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortExpected, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(3)
            void a03b() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                // If the sort tag uses romaji. (Nothing is done.)
                MediaFile mediaFile = toMediaFile(nameRaw, "Aiai・Ai");
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals("Aiai・Ai", mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            @Order(4)
            void a04() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                MediaFile mediaFile = toMediaFile(nameRaw, null);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                assertEquals(readingAnalyzed, mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }
        }

        /*
         * do nothing.
         */
        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class WithoutJpTest {

            // name
            private final String nameRaw = "The 「あいｱｲ・愛」";

            /*
             * So-called sort tag(This test assuming that Japanese is also used for the tag.) Japanese-specific
             * conversions should not be done.
             */
            private final String sortRaw = "あいあい・愛";
            private final String readingExpected = sortRaw;

            private MediaFile toMediaFile(String artist, String artistSort) {
                MediaFile mediaFile = new MediaFile();
                mediaFile.setArtist(artist);
                mediaFile.setArtistSort(artistSort);
                return mediaFile;
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.Null
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingExpected
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(1)
            void a02() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                MediaFile mediaFile = toMediaFile(null, sortRaw);
                utils.analyze(mediaFile);
                assertNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                // The value of the sort tag is used as it is
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortRaw, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(2)
            void a03a() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                MediaFile mediaFile = toMediaFile(nameRaw, sortRaw);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                // The value of the sort tag is used as it is
                assertEquals(readingExpected, mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                assertEquals(sortRaw, mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.NotNull
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.NotNull.SortExpected
            @Test
            @Order(3)
            void a03b() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                // If the sort tag uses romaji. (Nothing is done.)
                MediaFile mediaFile = toMediaFile(nameRaw, "Aiai・Ai");
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                // Sort tag is used as it is
                assertEquals("Aiai・Ai", mediaFile.getArtistReading());
                assertNotNull(mediaFile.getArtistSort());
                // Sort tag is used as it is
                assertEquals("Aiai・Ai", mediaFile.getArtistSort());
            }

            @AnalyzeMediaFileDecisions.Conditions.Name.NotNull
            @AnalyzeMediaFileDecisions.Conditions.Reading.Null
            @AnalyzeMediaFileDecisions.Conditions.Sort.Null
            @AnalyzeMediaFileDecisions.Result.Name.NotNull
            @AnalyzeMediaFileDecisions.Result.Reading.NotNull.ReadingAnalyzed
            @AnalyzeMediaFileDecisions.Result.Sort.Null
            @Test
            @Order(4)
            void a04() {
                /*
                 * If there is no sort tag in Japanese processing, the name will be parsed and the reading will be
                 * created. In case of WITHOUT_JP_LANG_PROCESSING, the name is used as it is.
                 */
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                MediaFile mediaFile = toMediaFile(nameRaw, null);
                utils.analyze(mediaFile);
                assertNotNull(mediaFile.getArtist());
                assertNotNull(mediaFile.getArtistReading());
                // However, the article is removed.
                assertEquals("「あいｱｲ・愛」", mediaFile.getArtistReading());
                assertNull(mediaFile.getArtistSort());
            }
        }
    }

    @Test
    @Order(10)
    void testAnalyzePlaylist() {
        final String playlistName = "2021/07/21 22:40 お気に入り";

        Playlist playlist = new Playlist();
        playlist.setName(playlistName);
        assertEquals(playlistName, playlist.getName());
        assertNull(playlist.getReading());

        utils.analyze(playlist);
        utils.clear();
        assertEquals("2021/07/21 22:40 オキニイリ", playlist.getReading());

        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
        utils.analyze(playlist);
        utils.clear();
        assertEquals("2021/07/21 22:40 Okiniiri", playlist.getReading());

        playlist = new Playlist();
        playlist.setName(playlistName);
        utils.analyze(playlist);
        utils.clear();
        assertEquals("2021/07/21 22:40 Okiniiri", playlist.getReading());

        Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
        utils.analyze(playlist);
        utils.clear();
        assertEquals(playlistName, playlist.getReading());
    }

    @Documented
    private @interface AnalyzeSortCandidateDecisions {
        @interface Conditions {
            @interface SortCandidate {
                @interface Name {
                    @interface NotNull {
                    }
                }

                @interface Sort {
                    @interface Null {
                    }

                    @interface NotNull {
                    }
                }
            }
        }

        @interface Result {
            @interface SortCandidate {
                @interface Reading {
                    @interface NotNull {
                        @interface ReadingExpected {
                        }

                        @interface ReadingAnalyzed {
                        }
                    }
                }

                @interface Sort {
                    @interface NotNull {
                        @interface ReadingAnalyzed {
                        }

                        @interface SortExpected {
                        }

                    }
                }
            }
        }
    }

    @Nested
    @Order(11)
    class AnalyzeSortCandidate {

        @Nested
        class NativeJapaneseTest {

            private final String nameRaw = "The 「あいｱｲ・愛」";
            private final String sortRaw = "あいあい・あい";
            private final String readingExpected = "アイアイ・アイ";
            private final String sortExpected = "あいあい・あい";
            private final String readingAnalyzed = "｢アイアイ・アイ｣";

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.Null
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingAnalyzed
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.ReadingAnalyzed
            @Test
            void a01() {
                SortCandidate candidate = new SortCandidate(nameRaw, null);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingAnalyzed, candidate.getReading());
                assertEquals(readingAnalyzed, candidate.getSort());
            }

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
            @Test
            void a02() {
                SortCandidate candidate = new SortCandidate(nameRaw, sortRaw);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingExpected, candidate.getReading());
                assertEquals(sortExpected, candidate.getSort());
            }

        }

        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class RomanizedJapaneseTest {

            private final String nameRaw = "The 「あいｱｲ・愛」";
            private final String sortRaw = "あいあい・あい";
            private final String readingExpected = "Aiai・Ai";
            private final String sortExpected = "あいあい・あい";
            private final String readingAnalyzed = "｢aiai・-ai｣";

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.Null
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingAnalyzed
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.ReadingAnalyzed
            @Test
            @Order(1)
            void a01() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                SortCandidate candidate = new SortCandidate(nameRaw, null);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingAnalyzed, candidate.getReading());
                assertEquals(readingAnalyzed, candidate.getSort());
            }

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
            @Test
            @Order(2)
            void a02a() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                SortCandidate candidate = new SortCandidate(nameRaw, sortRaw);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingExpected, candidate.getReading());
                assertEquals(sortExpected, candidate.getSort());
            }

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
            @Test
            @Order(3)
            void a02b() {
                Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
                SortCandidate candidate = new SortCandidate(nameRaw, "Aiai・Ai");
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingExpected, candidate.getReading());
                assertEquals("Aiai・Ai", candidate.getSort());
            }
        }

        @Nested
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class WithoutJpTest {

            private final String nameRaw = "The 「あいｱｲ・愛」";
            private final String sortRaw = "あいあい・あい";
            private final String readingExpected = sortRaw;
            private final String sortExpected = "あいあい・あい";
            private final String readingAnalyzed = "「あいｱｲ・愛」";

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.Null
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingAnalyzed
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.ReadingAnalyzed
            @Test
            @Order(1)
            void a01() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                SortCandidate candidate = new SortCandidate(nameRaw, null);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingAnalyzed, candidate.getReading());
                assertEquals(readingAnalyzed, candidate.getSort());
            }

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
            @Test
            @Order(2)
            void a02a() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                SortCandidate candidate = new SortCandidate(nameRaw, sortRaw);
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals(readingExpected, candidate.getReading());
                assertEquals(sortExpected, candidate.getSort());
            }

            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Name.NotNull
            @AnalyzeSortCandidateDecisions.Conditions.SortCandidate.Sort.NotNull
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Reading.NotNull.ReadingExpected
            @AnalyzeSortCandidateDecisions.Result.SortCandidate.Sort.NotNull.SortExpected
            @Test
            @Order(3)
            void a02b() {
                Mockito.when(settingsService.getIndexSchemeName())
                        .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
                SortCandidate candidate = new SortCandidate(nameRaw, "Aiai・Ai");
                utils.analyze(candidate);
                assertEquals(nameRaw, candidate.getName());
                assertEquals("Aiai・Ai", candidate.getReading());
                assertEquals("Aiai・Ai", candidate.getSort());
            }
        }
    }

    @Documented
    private @interface CreateIndexableNameArtistDecisions {
        @interface Conditions {

            @interface Artist {
                @interface Name {
                    @interface isStartWithAlpha {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistReading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistSort {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }
            }
        }

        @interface Result {
            @interface IndexableName {
                @interface NameDerived {
                }

                @interface NormalizedReading {
                }

            }
        }
    }

    @Nested
    @Order(12)
    class CreateIndexableNameArtistTest {

        private Artist createArtist(String name, String sort) throws ExecutionException {
            Artist artist = new Artist();
            artist.setName(name);
            artist.setReading(utils.createReading(name, sort));
            artist.setSort(sort);
            return artist;
        }

        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.True
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c01() throws ExecutionException {
            Artist artist = createArtist("abcde", null);
            assertEquals("abcde", artist.getName());
            assertEquals("abcde", artist.getReading());
            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("abcde", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.False
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c02() throws ExecutionException {
            Artist artist = createArtist("日本語名", null);
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());
            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistSort.Empty.False
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c03() throws ExecutionException {
            Artist artist = createArtist("日本語名", "にほんごめい");
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());

            artist.setReading(null);
            assertNull(artist.getReading());

            assertEquals("にほんごめい", artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameArtistDecisions.Conditions.Artist.Name.isStartWithAlpha.False
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistReading.Empty.True
        @CreateIndexableNameArtistDecisions.Conditions.Artist.ArtistSort.Empty.True
        @CreateIndexableNameArtistDecisions.Result.IndexableName.NameDerived
        @Test
        void c04() throws ExecutionException {
            Artist artist = createArtist("日本語名", null);
            assertEquals("日本語名", artist.getName());
            assertEquals("ニホンゴメイ", artist.getReading());

            artist.setReading(null);
            assertNull(artist.getReading());

            assertNull(artist.getSort());
            String indexableNameString = utils.createIndexableName(artist);

            assertEquals("日本語名", indexableNameString);
        }
    }

    @Documented
    private @interface CreateIndexableNameMediaFileDecisions {
        @interface Conditions {

            @interface MediaFile {
                @interface Name {
                    @interface isStartWithAlpha {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistReading {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }

                @interface ArtistSort {
                    @interface Empty {
                        @interface True {
                        }

                        @interface False {
                        }
                    }
                }
            }
        }

        @interface Result {
            @interface IndexableName {
                @interface PathDerived {
                }

                @interface NormalizedReading {
                }

            }
        }
    }

    /**
     * Since it is backward compatible, there are many cases, but in reality it is c02 in the case of Japanese. In most
     * cases, the scan is finished when the index is created. That is, the readings have been resolved and there are no
     * null cases.
     */
    @Nested
    @Order(13)
    class CreateIndexableNameMediaFile {

        private MediaFile createAnalyzedMediaFile(String name, String sort, String path) {
            MediaFile mediaFile = new MediaFile();
            mediaFile.setArtist(name);
            mediaFile.setArtistSort(sort);
            mediaFile.setPath(path);
            utils.analyze(mediaFile);
            return mediaFile;
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.True
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c01() {
            MediaFile file = createAnalyzedMediaFile("abcde", null, "abcde-path");
            assertEquals("abcde", file.getArtist());
            assertEquals("abcde", file.getArtistReading());
            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            // path. (All legacy servers have this specification)
            assertEquals("abcde-path", indexableNameString);
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.False
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c02() {
            MediaFile file = createAnalyzedMediaFile("日本語名", null, "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());
            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            // Normalized reading, not path. Most phonological index languages require this
            assertEquals("ニホンゴメイ", indexableNameString);
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistSort.Empty.False
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.NormalizedReading
        @Test
        void c03() {
            MediaFile file = createAnalyzedMediaFile("日本語名", "にほんごめい", "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());

            /*
             * Now, This case rarely occurs when normal processing is performed. (For example, older versions of the
             * data. Or if the index was created while the scan was in progress)
             */
            file.setArtistReading(null);
            assertNull(file.getArtistReading());

            assertEquals("にほんごめい", file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            assertEquals("ニホンゴメイ", indexableNameString); // Normalized reading, not path
        }

        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.Name.isStartWithAlpha.False
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistReading.Empty.True
        @CreateIndexableNameMediaFileDecisions.Conditions.MediaFile.ArtistSort.Empty.True
        @CreateIndexableNameMediaFileDecisions.Result.IndexableName.PathDerived
        @Test
        void c04() {
            MediaFile file = createAnalyzedMediaFile("日本語名", null, "日本語名-path");
            assertEquals("日本語名", file.getArtist());
            assertEquals("ニホンゴメイ", file.getArtistReading());

            /*
             * Now, This case rarely occurs when normal processing is performed. (For example, older versions of the
             * data. Or if the index was created while the scan was in progress)
             */
            file.setArtistReading(null);
            assertNull(file.getArtistReading());

            assertNull(file.getArtistSort());
            assertNull(file.getAlbumArtist());
            assertNull(file.getAlbumArtistReading());
            assertNull(file.getAlbumArtistSort());
            String indexableNameString = utils.createIndexableName(file);

            assertEquals("日本語名-path", indexableNameString); // Normalized reading, not path
        }
    }

    @Documented
    private @interface CreateIndexableNameDecisions {
        @interface Conditions {
            @interface IndexScheme {
                @interface NativeJapanese {
                }

                @interface RomanizedJapanese {
                }

                @interface WithoutJp {
                }
            }
        }
    }

    @Nested
    @Order(14)
    class CreateIndexableName {

        void assertDeleteDiacritic() {
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", utils.createIndexableName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
            assertEquals("ACEGIKLMNOPRSUWYZ", utils.createIndexableName("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ"));
            assertEquals("AEINOUWY", utils.createIndexableName("ÀÈÌǸÒÙẀỲ"));
            assertEquals("ACEGHIJNOSUWYZ", utils.createIndexableName("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ"));
            assertEquals("AEHIOTUWXY", utils.createIndexableName("ÄËḦÏÖT̈ÜẄẌŸ"));
            assertEquals("AUWY", utils.createIndexableName("ÅŮW̊Y̊"));
            assertEquals("ACDEGHIKLNORSTUZ", utils.createIndexableName("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ"));
            assertEquals("AEINOUVY", utils.createIndexableName("ÃẼĨÑÕŨṼỸ"));
            assertEquals("CDEGHKLNRST", utils.createIndexableName("ÇḐȨĢḨĶĻŅŖŞŢ"));
            assertEquals("ST", utils.createIndexableName("ȘȚ"));
            assertEquals("AEGIOU", utils.createIndexableName("ĂĔĞĬŎŬ"));
            assertEquals("AEGINOUY", utils.createIndexableName("ĀĒḠĪN̄ŌŪȲ"));
            assertEquals("AEIOU", utils.createIndexableName("ĄĘĮǪŲ"));
            assertEquals("OU", utils.createIndexableName("ŐŰ"));
            assertEquals("ABCDEFGHIĿMNOPRSTWXYZ", utils.createIndexableName("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ"));
            assertEquals("OU", utils.createIndexableName("ƠƯ"));

            // Currently does not support stroke deletion
            assertEquals("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ", utils.createIndexableName("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ"));
        }

        void assertRemainDiacritic() {
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", utils.createIndexableName("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
            assertEquals("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ", utils.createIndexableName("ÁĆÉǴÍḰĹḾŃÓṔŔŚÚẂÝŹ"));
            assertEquals("ÀÈÌǸÒÙẀỲ", utils.createIndexableName("ÀÈÌǸÒÙẀỲ"));
            assertEquals("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ", utils.createIndexableName("ÂĈÊĜĤÎĴN̂ÔŜÛŴŶẐ"));
            assertEquals("ÄËḦÏÖT̈ÜẄẌŸ", utils.createIndexableName("ÄËḦÏÖT̈ÜẄẌŸ"));
            assertEquals("ÅŮW̊Y̊", utils.createIndexableName("ÅŮW̊Y̊"));
            assertEquals("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ", utils.createIndexableName("ǍČĎĚǦȞǏǨĽŇǑŘŠŤǓŽ"));
            assertEquals("ÃẼĨÑÕŨṼỸ", utils.createIndexableName("ÃẼĨÑÕŨṼỸ"));
            assertEquals("ÇḐȨĢḨĶĻŅŖŞŢ", utils.createIndexableName("ÇḐȨĢḨĶĻŅŖŞŢ"));
            assertEquals("ȘȚ", utils.createIndexableName("ȘȚ"));
            assertEquals("ĂĔĞĬŎŬ", utils.createIndexableName("ĂĔĞĬŎŬ"));
            assertEquals("ĀĒḠĪN̄ŌŪȲ", utils.createIndexableName("ĀĒḠĪN̄ŌŪȲ"));
            assertEquals("ĄĘĮǪŲ", utils.createIndexableName("ĄĘĮǪŲ"));
            assertEquals("ŐŰ", utils.createIndexableName("ŐŰ"));
            assertEquals("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ", utils.createIndexableName("ȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ"));
            assertEquals("ƠƯ", utils.createIndexableName("ƠƯ"));

            // Currently does not support stroke deletion
            assertEquals("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ", utils.createIndexableName("ɃĐǤĦƗɈŁØⱣɌŦɄɎƵ"));
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.NativeJapanese
        @Test
        void c01() throws ExecutionException {

            Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.NATIVE_JAPANESE.name());

            assertEquals("ABCDE", utils.createIndexableName("ABCDE")); // no change
            assertEquals("アイウエオ", utils.createIndexableName("アイウエオ")); // no change
            assertEquals("ァィゥェォ", utils.createIndexableName("ァィゥェォ")); // no change
            assertEquals("ァィゥェォ", utils.createIndexableName("ｧｨｩｪｫ")); // to ** Fullwidth **
            assertEquals("アイウエオ", utils.createIndexableName("ｱｲｳｴｵ")); // to ** Fullwidth **
            assertEquals("ツンク♂", utils.createIndexableName("つんく♂")); // to Katakana
            assertEquals("アイウエオ", utils.createIndexableName("あいうえお")); // to Katakana
            assertEquals("ゴウヒロミ", utils.createIndexableName("ゴウヒロミ")); // NFD
            assertEquals("パミュパミュ", utils.createIndexableName("ぱみゅぱみゅ")); // NFD
            assertEquals("コウダクミ", utils.createIndexableName("コウダクミ")); // NFD

            // Half-width conversion is forcibly executed in this schema.
            assertEquals("ABCDE", utils.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth

            // Diacritic removed is forcibly executed in this schema.
            assertDeleteDiacritic();
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.RomanizedJapanese
        @Test
        void c02() throws ExecutionException {

            Mockito.when(settingsService.getIndexSchemeName()).thenReturn(IndexScheme.ROMANIZED_JAPANESE.name());
            Mockito.when(settingsService.isDeleteDiacritic()).thenReturn(true);

            assertEquals("ABCDE", utils.createIndexableName("ABCDE"));

            // Half-width conversion is forcibly executed in this schema.
            assertEquals("ABCDE", utils.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth

            // Diacritic
            assertDeleteDiacritic();
            Mockito.when(settingsService.isDeleteDiacritic()).thenReturn(false);
            assertRemainDiacritic();
        }

        @CreateIndexableNameDecisions.Conditions.IndexScheme.WithoutJp
        @Test
        void c03() throws ExecutionException {

            Mockito.when(settingsService.getIndexSchemeName())
                    .thenReturn(IndexScheme.WITHOUT_JP_LANG_PROCESSING.name());
            Mockito.when(settingsService.isIgnoreFullWidth()).thenReturn(true);
            Mockito.when(settingsService.isDeleteDiacritic()).thenReturn(true);

            assertEquals("DJ FUMI★YEAH!", utils.createIndexableName("DJ FUMI★YEAH!")); // no change
            assertEquals("ABCDE", utils.createIndexableName("ABCDE")); // no change
            assertEquals("ｱｲｳｴｵ", utils.createIndexableName("アイウエオ")); // to Halfwidth
            assertEquals("ｧｨｩｪｫ", utils.createIndexableName("ァィゥェォ")); // to Halfwidth
            assertEquals("ｧｨｩｪｫ", utils.createIndexableName("ｧｨｩｪｫ")); // no change
            assertEquals("ｱｲｳｴｵ", utils.createIndexableName("ｱｲｳｴｵ")); // no change
            assertEquals("つんく♂", utils.createIndexableName("つんく♂")); // no change
            assertEquals("あいうえお", utils.createIndexableName("あいうえお")); // no change
            assertEquals("ｺﾞｳﾋﾛﾐ", utils.createIndexableName("ゴウヒロミ")); // to Halfwidth
            assertEquals("ぱみゅぱみゅ", utils.createIndexableName("ぱみゅぱみゅ")); // NFD
            assertEquals("ｺｳﾀﾞｸﾐ", utils.createIndexableName("コウダクミ")); // to Halfwidth

            // Halfwidth
            assertEquals("ABCDE", utils.createIndexableName("ＡＢＣＤＥ")); // to Halfwidth
            Mockito.when(settingsService.isIgnoreFullWidth()).thenReturn(false);
            assertEquals("ＡＢＣＤＥ", utils.createIndexableName("ＡＢＣＤＥ")); // no change

            // Diacritic
            assertDeleteDiacritic();
            Mockito.when(settingsService.isDeleteDiacritic()).thenReturn(false);
            assertRemainDiacritic();
        }
    }
}
