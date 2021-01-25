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

package com.tesshu.jpsonic.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.function.BiFunction;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = { "org.airsonic.player", "com.tesshu.jpsonic" })
@SpringBootTest
public class JapaneseReadingUtilsTest extends AbstractAirsonicHomeTest {

    @Autowired
    private JapaneseReadingUtils utils;

    @Test
    public void testCreateReading() {
        assertEquals("アイウエオ", utils.createReading("あいうえお"));
        assertEquals("アイウエオ", utils.createReading("アイウエオ"));
        assertEquals("ァィゥェォ", utils.createReading("ァィゥェォ"));
        assertEquals("ァィゥェォ", utils.createReading("ｧｨｩｪｫ"));
        assertEquals("アイウエオ", utils.createReading("ｱｲｳｴｵ"));
        assertEquals("アイウエオ", utils.createReading("亜伊鵜絵尾"));
        assertEquals("ABCDE", utils.createReading("ABCDE"));
        assertEquals("ABCDE", utils.createReading("ＡＢＣＤＥ"));
        assertEquals("アルファベータガンマ", utils.createReading("αβγ"));
        assertEquals("ツンク♂", utils.createReading("つんく♂"));
        assertEquals("bad communication", utils.createReading("bad communication"));
        assertEquals("BAD COMMUNICATION", utils.createReading("BAD COMMUNICATION"));
        assertEquals("BAD COMMUNICATION", utils.createReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals("イヌトネコ", utils.createReading("犬とネコ"));
        assertEquals(" ｢｣()()[][];!!??##123", utils.createReading("　「」（）()［］[]；！!？?＃#１２３"));
        assertEquals("Cæsar", utils.createReading("Cæsar"));
        assertEquals("Alfee", utils.createReading("The Alfee"));
        assertEquals("コンピューター", utils.createReading("コンピューター"));
        assertEquals("アイ～ウエ", utils.createReading("あい～うえ"));
        assertEquals("アイウエ～", utils.createReading("あいうえ～"));
        assertEquals("～アイウエ", utils.createReading("～あいうえ"));
        assertEquals("ア～イ～ウ～エ", utils.createReading("あ～い～う～え"));
        assertEquals("     ", utils.createReading("　　　　　"));
        assertEquals("[Disc 3]", utils.createReading("[Disc 3]"));
        assertEquals("Best ～first things～", utils.createReading("Best ～first things～"));
        assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                utils.createReading("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertEquals("Dvořák: Symphonies #7-9", utils.createReading("Dvořák: Symphonies #7-9"));
        assertEquals("フクヤママサハル", utils.createReading("福山雅治"));// Readable case
        assertEquals("サシハラ莉乃", utils.createReading("サシハラ莉乃"));// Unreadable case
        assertEquals("倖タ來ヒツジ", utils.createReading("倖田來未"));// Unreadable case
        assertEquals("オクダ ミンセイ", utils.createReading("奥田　民生"));// Unreadable case
    }

    @Test
    public void testAnalyzeSort() {
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
        assertEquals("[Disc 3]", utils.createReading("[Disc 3]"));
        assertEquals("Best ～first things～", utils.normalize("Best ～first things～"));
        assertEquals("B'z The Best \"ULTRA Pleasure\" -The Second RUN-",
                utils.normalize("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertEquals("Dvořák: Symphonies #7-9", utils.normalize("Dvořák: Symphonies #7-9"));
        assertEquals("福山雅治", utils.normalize("福山雅治"));
        assertEquals("サシハラ莉乃", utils.normalize("サシハラ莉乃"));
        assertEquals("倖田來未", utils.normalize("倖田來未"));
    }

    private BiFunction<String, String, MediaFile> toMediaFile = (artist, artistSort) -> {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist(artist);
        mediaFile.setArtistSort(artistSort);
        return mediaFile;
    };

    private BiFunction<String, String, MediaFile> toAnalyzedMediaFile = (artist, path) -> {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist(artist);
        mediaFile.setArtistSort(artist);
        mediaFile.setPath(path);
        utils.analyze(mediaFile);
        return mediaFile;
    };

    @Test
    public void testAnalyzeIfSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply(null, "あいうえお");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("あいうえお", artist.getArtistSort());

        artist = toMediaFile.apply(null, "アイウエオ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ァィゥェォ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｧｨｩｪｫ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｱｲｳｴｵ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "亜伊鵜絵尾");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("亜伊鵜絵尾", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ABCDE");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＡＢＣＤＥ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "αβγ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertEquals("αβγ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "つんく♂");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertEquals("つんく♂", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertEquals("BAD COMMUNICATION", artist.getArtistSort());

        artist = toMediaFile.apply(null, "部屋とYシャツと私");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertEquals("部屋とYシャツと私", artist.getArtistSort());

        artist = toMediaFile.apply(null, "　「」（）()［］[]；！!？?＃#１２３");
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals(" ｢｣()()[][];!!??##123", artist.getArtistReading());
        assertEquals(" ｢｣()()[][];!!??##123", artist.getArtistSort());

    }

    @Test
    public void testAnalyzeIfNoSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        utils.analyze(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("あいうえお", null);
        utils.analyze(artist);
        assertEquals("あいうえお", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("アイウエオ", null);
        utils.analyze(artist);
        assertEquals("アイウエオ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ァィゥェォ", null);
        utils.analyze(artist);
        assertEquals("ァィゥェォ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｧｨｩｪｫ", null);
        utils.analyze(artist);
        assertEquals("ｧｨｩｪｫ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｱｲｳｴｵ", null);
        utils.analyze(artist);
        assertEquals("ｱｲｳｴｵ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("亜伊鵜絵尾", null);
        utils.analyze(artist);
        assertEquals("亜伊鵜絵尾", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ABCDE", null);
        utils.analyze(artist);
        assertEquals("ABCDE", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＡＢＣＤＥ", null);
        utils.analyze(artist);
        assertEquals("ＡＢＣＤＥ", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("αβγ", null);
        utils.analyze(artist);
        assertEquals("αβγ", artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("つんく♂", null);
        utils.analyze(artist);
        assertEquals("つんく♂", artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", null);
        utils.analyze(artist);
        assertEquals("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("部屋とYシャツと私", null);
        utils.analyze(artist);
        assertEquals("部屋とYシャツと私", artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("　「」（）()［］[]；！!？?＃#１２３", null);
        utils.analyze(artist);
        assertEquals("　「」（）()［］[]；！!？?＃#１２３", artist.getArtist());
        assertEquals(" ｢｣()()[][];!!??##123", artist.getArtistReading());
        assertNull(artist.getArtistSort());

    }

    @Test
    public void testCreateIndexableName4MediaFile() {

        MediaFile file = toAnalyzedMediaFile.apply(null, "  ");
        assertEquals("  ", file.getPath());
        assertEquals("アイウエオ", toAnalyzedMediaFile.apply("あいうえお", "dummyPath").getArtistReading());

        assertEquals("dummyPath", utils.createIndexableName(toAnalyzedMediaFile.apply("あいうえお", "dummyPath")));
        assertEquals("アイウエオ", utils.createIndexableName(toAnalyzedMediaFile.apply("あいうえお", "非alpha")));
        assertEquals("アイウエオ", utils.createIndexableName(toAnalyzedMediaFile.apply("アイウエオ", "非alpha")));
        assertEquals("ァィゥェォ", utils.createIndexableName(toAnalyzedMediaFile.apply("ァィゥェォ", "非alpha")));
        assertEquals("ァィゥェォ", utils.createIndexableName(toAnalyzedMediaFile.apply("ｧｨｩｪｫ", "非alpha")));
        assertEquals("アイウエオ", utils.createIndexableName(toAnalyzedMediaFile.apply("ｱｲｳｴｵ", "非alpha")));
        assertEquals("アイウエオ", utils.createIndexableName(toAnalyzedMediaFile.apply("亜伊鵜絵尾", "非alpha")));

        assertEquals("ABCDE", utils.createIndexableName(toAnalyzedMediaFile.apply("ABCDE", "非alpha")));
        assertEquals("ABCDE", utils.createIndexableName(toAnalyzedMediaFile.apply("ＡＢＣＤＥ", "非alpha")));
        assertEquals("アルファベータガンマ", utils.createIndexableName(toAnalyzedMediaFile.apply("αβγ", "非alpha")));
        assertEquals("ツンク♂", utils.createIndexableName(toAnalyzedMediaFile.apply("つんく♂", "非alpha")));

        assertEquals("BAD COMMUNICATION",
                utils.createIndexableName(toAnalyzedMediaFile.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", "非alpha")));
        assertEquals(" ｢｣()()[][];!!??##123",
                utils.createIndexableName(toAnalyzedMediaFile.apply("　「」（）()［］[]；！!？?＃#１２３", "非alpha")));

        assertEquals("ゴウヒロミ", utils.createIndexableName(toAnalyzedMediaFile.apply("ゴウヒロミ", "非alpha")));
        assertEquals("パミュパミュ", utils.createIndexableName(toAnalyzedMediaFile.apply("ぱみゅぱみゅ", "非alpha")));

        file = toAnalyzedMediaFile.apply("倖田來未", "非alpha");
        file.setArtistSort("こうだくみ");
        file.setPath("非alpha");
        utils.analyze(file);
        assertEquals("コウダクミ", utils.createIndexableName(file));

        file = toAnalyzedMediaFile.apply("DJ FUMI★YEAH!", "alpha");
        file.setArtistSort("DJ FUMIYA");
        utils.analyze(file);
        assertEquals("DJ FUMI★YEAH!", file.getArtist());
        assertEquals("DJ FUMI★YEAH!", file.getArtistReading());
        assertEquals("DJ FUMIYA", file.getArtistSort());
        assertEquals("alpha", utils.createIndexableName(file));
    }

    @Test
    public void testAnalyzeSortCandidate() {

        SortCandidate candidate = new SortCandidate("Antonín Dvořák", null);
        utils.analyze(candidate);
        assertEquals("Antonín Dvořák", candidate.getName());
        assertEquals("Antonín Dvořák", candidate.getReading());
        assertEquals("Antonín Dvořák", candidate.getSort());

        candidate = new SortCandidate("奥田民生", null);
        utils.analyze(candidate);
        assertEquals("奥田民生", candidate.getName());
        assertEquals("オクダタミオ", candidate.getReading());
        assertEquals("オクダタミオ", candidate.getSort());

    }

    @Test
    public void testIsJapaneseReadable() {
        assertTrue(utils.isJapaneseReadable("あいうえお"));
        assertTrue(utils.isJapaneseReadable("アイウエオ"));
        assertTrue(utils.isJapaneseReadable("ァィゥェォ"));
        assertTrue(utils.isJapaneseReadable("ｧｨｩｪｫ"));
        assertTrue(utils.isJapaneseReadable("ｱｲｳｴｵ"));
        assertTrue(utils.isJapaneseReadable("亜伊鵜絵尾"));
        assertFalse(utils.isJapaneseReadable("ABCDE"));
        assertTrue(utils.isJapaneseReadable("ＡＢＣＤＥ"));
        assertTrue(utils.isJapaneseReadable("αβγ"));
        assertTrue(utils.isJapaneseReadable("つんく♂"));
        assertFalse(utils.isJapaneseReadable("bad communication"));
        assertFalse(utils.isJapaneseReadable("BAD COMMUNICATION"));
        assertTrue(utils.isJapaneseReadable("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertTrue(utils.isJapaneseReadable("犬とネコ"));
        assertTrue(utils.isJapaneseReadable("読み"));
        assertTrue(utils.isJapaneseReadable("(読み)"));
        assertTrue(utils.isJapaneseReadable("　「」（）()［］[]；！!？?＃#１２３"));
        assertFalse(utils.isJapaneseReadable("Cæsar"));
        assertFalse(utils.isJapaneseReadable("The Alfee"));
        assertTrue(utils.isJapaneseReadable("コンピューター"));
        assertTrue(utils.isJapaneseReadable("あい～うえ"));
        assertTrue(utils.isJapaneseReadable("あいうえ～"));
        assertTrue(utils.isJapaneseReadable("～あいうえ"));
        assertTrue(utils.isJapaneseReadable("あ～い～う～え"));
        assertTrue(utils.isJapaneseReadable("　　　　　"));
        assertFalse(utils.isJapaneseReadable("[Disc 3]"));
        assertTrue(utils.isJapaneseReadable("Best ～first things～"));
        assertFalse(utils.isJapaneseReadable("B'z The Best \"ULTRA Pleasure\" -The Second RUN-"));
        assertFalse(utils.isJapaneseReadable("Dvořák: Symphonies #7-9"));
        assertTrue(utils.isJapaneseReadable("福山雅治"));
        assertTrue(utils.isJapaneseReadable("サシハラ莉乃"));
        assertTrue(utils.isJapaneseReadable("倖田來未"));
        assertTrue(utils.isJapaneseReadable("奥田民生"));

        /*
         * Some words are misjudged, but need not be complete. No problem if reading is generated correctly.
         */
        assertEquals("ABCDE", utils.createReading("ＡＢＣＤＥ"));
        assertEquals("BAD COMMUNICATION", utils.createReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals(" ｢｣()()[][];!!??##123", utils.createReading("　「」（）()［］[]；！!？?＃#１２３"));

    }

    @Test
    public void testRemovePunctuationFromJapaneseReading() {
        assertEquals("あいうえお", utils.removePunctuationFromJapaneseReading("あいうえお"));
        assertEquals("アイウエオ", utils.removePunctuationFromJapaneseReading("アイウエオ"));
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
    }

}
