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

package com.tesshu.jpsonic.infrastructure.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.system.IndexScheme;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class JapaneseReadingUtilsTest {

    private SettingsFacade settingsFacade;

    private JapaneseReadingUtils utils;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.NATIVE_JAPANESE.name())
            .build();
        utils = new JapaneseReadingUtils(settingsFacade);
    }

    @AfterEach
    public void afterAll() {
        utils.clear();
    }

    @Order(0)
    @Test
    void testIsStartWithAlpha() {
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("a"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("z"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("A"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("Z"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("ａ"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("ｚ"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("Ａ"));
        assertTrue(JapaneseReadingUtils.isStartWithAlpha("Ｚ"));

        assertFalse(JapaneseReadingUtils.isStartWithAlpha("\\"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("^"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("_"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("`"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("``"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("."));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha(","));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("-"));
        assertFalse(JapaneseReadingUtils.isStartWithAlpha("_"));
    }

    @Order(1)
    @Test
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

    @Order(2)
    @Test
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
        assertEquals(" ｢｣()()[][];!!??##123",
                utils.removePunctuationFromJapaneseReading(" ｢｣()()[][];!!??##123"));
        assertEquals("コンピューター", utils.removePunctuationFromJapaneseReading("コンピューター"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイ～ウエ"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("アイウエ～"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("～アイウエ"));
        assertEquals("アイウエ", utils.removePunctuationFromJapaneseReading("ア～イ～ウ～エ"));
        assertEquals("     ", utils.removePunctuationFromJapaneseReading("     "));
        assertEquals("[Disc 3]", utils.removePunctuationFromJapaneseReading("[Disc 3]"));
        assertEquals("Best ～first things～",
                utils.removePunctuationFromJapaneseReading("Best ～first things～"));
        assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                utils
                    .removePunctuationFromJapaneseReading(
                            "B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertEquals("Dvořák: Symphonies #7-9",
                utils.removePunctuationFromJapaneseReading("Dvořák: Symphonies #7-9"));
        assertEquals("フクヤママサハル", utils.removePunctuationFromJapaneseReading("フクヤママサハル"));
        assertEquals("サシハラ莉乃", utils.removePunctuationFromJapaneseReading("サシハラ莉乃"));
        assertEquals("倖タ來ヒツジ", utils.removePunctuationFromJapaneseReading("倖タ來ヒツジ"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ ローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
        assertEquals("シンディローパー", utils.removePunctuationFromJapaneseReading("シンディ・ローパー"));
        assertEquals("ｼﾝﾃﾞｨﾛｰﾊﾟｰ", utils.removePunctuationFromJapaneseReading("ｼﾝﾃﾞｨ･ﾛｰﾊﾟｰ"));
    }

    @Order(3)
    @Test
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

    @Order(4)
    @Test
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

    @Order(5)
    @Test
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

        // NATIVE_JAPANESE
        assertTrue(utils.isJapaneseReadable("αβγ"));

        // ROMANIZED_JAPANESE
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName, IndexScheme.ROMANIZED_JAPANESE.name())
            .build();
        utils = new JapaneseReadingUtils(settingsFacade);
        assertFalse(utils.isJapaneseReadable("αβγ"));

        // WITHOUT_JP_LANG_PROCESSING
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(I18nSKeys.localeLanguage, "ja")
            .withString(I18nSKeys.localeCountry, "ja")
            .withString(SKeys.advanced.index.indexSchemeName,
                    IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
            .build();
        utils = new JapaneseReadingUtils(settingsFacade);
        assertFalse(utils.isJapaneseReadable("αβγ"));
    }

    @Order(6)
    @Nested
    class CreateJapaneseReadingTest {

        @Test
        void testNullParam() throws ExecutionException {
            assertNull(utils.createJapaneseReading(null));
        }

        @Nested
        class NativeJapaneseTest {

            @Test
            void testCreateReading() throws ExecutionException {

                // NATIVE_JAPANESE

                /*
                 * Kuromoji will read the full-width alphabet in Japanese. ＢＢＣ(It's not bbc but
                 * ビービーシー.) When this is done in the field of Japanese music, it is often not
                 * very good. This conversion is suppressed in Jpsonic. Full-width alphabets
                 * will not been read in Japanese.
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
                assertEquals(" ｢｣()()[][];!!??##123",
                        utils.createJapaneseReading("　「」（）()［］[]；！!？?＃#１２３"));
                assertEquals("Cæsar", utils.createJapaneseReading("Cæsar"));
                assertEquals("Alfee", utils.createJapaneseReading("The Alfee"));
                assertEquals("コンピューター", utils.createJapaneseReading("コンピューター"));
                assertEquals("アイ～ウエ", utils.createJapaneseReading("あい～うえ"));
                assertEquals("アイウエ～", utils.createJapaneseReading("あいうえ～"));
                assertEquals("～アイウエ", utils.createJapaneseReading("～あいうえ"));
                assertEquals("ア～イ～ウ～エ", utils.createJapaneseReading("あ～い～う～え"));
                assertEquals("     ", utils.createJapaneseReading("　　　　　"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Best ～first things～",
                        utils.createJapaneseReading("Best ～first things～"));
                assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-", utils
                    .createJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
                assertEquals("Dvořák: Symphonies #7-9",
                        utils.createJapaneseReading("Dvořák: Symphonies #7-9"));
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

                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);

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
                assertEquals(" ｢｣()()[][];!!??##123",
                        utils.createJapaneseReading("　「」（）()［］[]；！!？?＃#１２３"));
                assertEquals("Cæsar", utils.createJapaneseReading("Cæsar"));
                assertEquals("Alfee", utils.createJapaneseReading("The Alfee"));
                assertEquals("Konpyuta", utils.createJapaneseReading("コンピューター"));
                assertEquals("Ai～ue", utils.createJapaneseReading("あい～うえ"));
                assertEquals("Aiue～", utils.createJapaneseReading("あいうえ～"));
                assertEquals("～aiue", utils.createJapaneseReading("～あいうえ"));
                assertEquals("A～i～u ～e", utils.createJapaneseReading("あ～い～う～え"));
                assertEquals("     ", utils.createJapaneseReading("　　　　　"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Best ～first things～",
                        utils.createJapaneseReading("Best ～first things～"));
                assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-", utils
                    .createJapaneseReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
                // Normalized in the 'reading' field
                assertEquals("Dvorak: Symphonies #7-9",
                        utils.createJapaneseReading("Dvořák: Symphonies #7-9"));
                assertEquals("[Disc 3]", utils.createJapaneseReading("[Disc 3]"));
                assertEquals("Fukuyamamasaharu", utils.createJapaneseReading("福山雅治")); // Readable
                                                                                       // case

                /*
                 * Unreadable (rare) case. Not in the dictionary. No problem. If we don't know
                 * it in advance, we won't know if even the Japanese can read it.
                 */
                assertEquals("Sashihara莉乃", utils.createJapaneseReading("サシハラ莉乃"));
                assertEquals("倖ta來hitsuji", utils.createJapaneseReading("倖田來未"));

                /*
                 * Unreadable case. The reading of the first candidate is different from what we
                 * expected. Not 'Minsei' but 'Tamio'. This is often the case in a person's
                 * name. The names of entertainers and younger generations may not be resolved
                 * by dictionaries. No additional dictionaries are used. Because it is
                 * reasonable to quote from CDDB.
                 */
                assertEquals("Okuda Minsei", utils.createJapaneseReading("奥田　民生"));

                /*
                 * Unreadable case. The reading of the first candidate is different from what we
                 * expected. Not 'Tsuge-rasetai' but 'Koku-rasetai'. 'Koku-rasetai' is a slang.
                 */
                assertEquals("Kagu ya Sama wa Tsuge-rasetai?",
                        utils.createJapaneseReading("かぐや様は告らせたい?"));

                /*
                 * Unreadable case. Not 'Kyaputentsubasa' but 'Captain Tsubasa'. Romaji used
                 * overseas seems to actively use English for words that can be replaced with
                 * English. This is not possible with current morphological analyzers. This case
                 * will be reasonable to quote from CDDB.
                 */
                assertEquals("Kyaputentsubasa", utils.createJapaneseReading("キャプテン翼"));
            }

            @Test
            void testReading2() throws ExecutionException {

                /*
                 * Jpsonic's romanization features is an improved version of the Hepburn
                 * romanization, and the notation of MyAnimeList is used as a reference. Due to
                 * the nature of Japanese, a general dictionary alone cannot cover everything,
                 * but it can provide a relatively natural conversion than a simple exchange
                 * like ICU. The correct answer rate is expected to be 90% or more, and if the
                 * user feels uncomfortable, it can be corrected by updating the tag
                 * information.
                 */

                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);

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
                assertEquals("Mirai, Kaete Mitakunattayo!",
                        utils.createJapaneseReading("みらい、変えてみたくなったよ!"));

                // SENTENCE_ENDING_PARTICLE, POSTPOSITIONAL_PARTICLE, MULTI_PARTICLE
                assertEquals("Chippokenajibun ga Doko e Tobidaserukana",
                        utils.createJapaneseReading("ちっぽけな自分がどこへ飛び出せるかな"));

                // ADVERB
                assertEquals("(nantoka Naru-sa to ) Ah! Hajimeyou",
                        utils.createJapaneseReading("(なんとかなるさと) Ah! はじめよう"));

                // (small tsu)
                assertEquals("Issaigassaibonyona", utils.createJapaneseReading("一切合切凡庸な"));

                // ADVERBIAL_PARTICLE, SENTENCE_ENDING_PARTICLE
                assertEquals("Anataja Wakaranaikamo Ne",
                        utils.createJapaneseReading("あなたじゃ分からないかもね"));

                // (small tsu)
                assertEquals("Gatchaman", utils.createJapaneseReading("ガッチャマン"));
                assertEquals("Hatchaku-eki", utils.createJapaneseReading("発着駅"));

                // (small tsu) needs romanize
                assertEquals("Minna Maruta wa Mottana!!",
                        utils.createJapaneseReading("みんな 丸太は持ったな!!"));
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

    @Order(7)
    @Nested
    class CreateReading {

        @Nested
        class NativeJapaneseTest {

            @Order(1)
            @Test
            void testNullSort() throws ExecutionException {
                assertEquals("name", utils.createReading("name", null));
                assertEquals("ニホンゴメイ", utils.createReading("日本語名", null));
                assertEquals("ニホンゴメイ", utils.createReading("日本語名", null)); // from map
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.SortDirived
            @Order(2)
            @Test
            void c01() throws ExecutionException {
                assertEquals("It's ニホンゴノヨミ", utils.createReading("abc", "It's 日本語の読み"));
            }

            /**
             * Dirty data case. Most recently, CDDB data has been separated into national
             * languages. Old tags created based on old CDDB data may have alphabet readings
             * registered for Japanese songs. When used by Japanese people, alphabet-only
             * sort tags are almost always meaningless or when romaji is mistakenly
             * registered. In this case, the sort tag is not used and reading is generated
             * internally. Reading is used for sorting and indexing in later processing. On
             * the other hand, sort tags are used for the search-index.
             */
            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.N
            @CreateReadingDecisions.Result.NameDirived
            @Order(3)
            @Test
            void c02() throws ExecutionException {
                assertEquals("abc", utils.createReading("abc", "It is an English reading"));
                assertEquals("abcニホンゴ", utils.createReading("abc日本語", "It is an English reading"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.N
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.NameDirived
            @Order(4)
            @Test
            void c03() throws ExecutionException {
                assertEquals("abc", utils.createReading("abc", "日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.N
            @Order(5)
            @Test
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
            @Order(2)
            @Test
            void c01() throws ExecutionException {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);
                assertEquals("It's Nihongo no Yomi", utils.createReading("abc", "It's 日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.N
            @CreateReadingDecisions.Result.NameDirived
            @Order(3)
            @Test
            /**
             * In the case of NATIVE_JAPANESE This case is a dirty case, but in the case of
             * ROMANIZED_JAPANESE, it is a normal process.
             */
            void c02() throws ExecutionException {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);
                assertEquals("It is an English reading", utils
                    .createReading("It is an English reading", "It is an English reading"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.Y
            @CreateReadingDecisions.Conditions.Sort.StartWithAlpha.N
            @CreateReadingDecisions.Conditions.Sort.JapaneseReadable.Y
            @CreateReadingDecisions.Result.NameDirived
            @Order(4)
            @Test
            void c03() throws ExecutionException {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);
                assertEquals("Nihongo no Yomi", utils.createReading("abc", "日本語の読み"));
            }

            @CreateReadingDecisions.Conditions.Name.StartWithAlpha.N
            @Order(5)
            @Test
            void c04() throws ExecutionException {
                // ROMANIZED_JAPANESE
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.ROMANIZED_JAPANESE.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);
                assertEquals("Nihongo no Yomi", utils.createReading("日本語名", "日本語の読み"));
                assertEquals("Nihongo-mei", utils.createReading("日本語名", null));
            }
        }

        @Nested
        class WithoutJpTest {

            @Test
            void testDonithing() throws ExecutionException {
                // WITHOUT_JP_LANG_PROCESSING
                settingsFacade = SettingsFacadeBuilder
                    .create()
                    .withString(I18nSKeys.localeLanguage, "ja")
                    .withString(I18nSKeys.localeCountry, "ja")
                    .withString(SKeys.advanced.index.indexSchemeName,
                            IndexScheme.WITHOUT_JP_LANG_PROCESSING.name())
                    .build();
                utils = new JapaneseReadingUtils(settingsFacade);

                /*
                 * Nothing is done. The value has been transferred only.
                 */
                assertEquals("sort", utils.createReading("name", "sort"));
                assertEquals("ソート", utils.createReading("name", "ソート"));
            }

        }
    }
}
