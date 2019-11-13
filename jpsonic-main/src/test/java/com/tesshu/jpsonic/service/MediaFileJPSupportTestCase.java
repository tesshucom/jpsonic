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
package com.tesshu.jpsonic.service;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.BiFunction;

import static org.junit.Assert.*;

public class MediaFileJPSupportTestCase extends AbstractAirsonicHomeTest {

    @Autowired
    private MediaFileJPSupport support;

    @Test
    public void testCreateReading() {
        assertEquals("アイウエオ", support.createReading("あいうえお"));
        assertEquals("アイウエオ", support.createReading("アイウエオ"));
        assertEquals("ァィゥェォ", support.createReading("ァィゥェォ"));
        assertEquals("ァィゥェォ", support.createReading("ｧｨｩｪｫ"));
        assertEquals("アイウエオ", support.createReading("ｱｲｳｴｵ"));
        assertEquals("アイウエオ", support.createReading("亜伊鵜絵尾"));
        assertEquals("ABCDE", support.createReading("ABCDE"));
        assertEquals("ABCDE", support.createReading("ＡＢＣＤＥ"));
        assertEquals("アルファベータガンマ", support.createReading("αβγ"));
        assertEquals("ツンク♂", support.createReading("つんく♂"));
        assertEquals("bad communication", support.createReading("bad communication"));
        assertEquals("BAD COMMUNICATION", support.createReading("BAD COMMUNICATION"));
        assertEquals("BAD COMMUNICATION", support.createReading("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals("イヌトネコ", support.createReading("犬とネコ"));
        assertEquals(" 「」()()[][];!!??##123", support.createReading(" 「」（）()［］[]；！!？?＃#１２３"));
        assertEquals("Cæsar", support.createReading("Cæsar"));
        assertEquals("Alfee", support.createReading("The Alfee"));
    }

    @Test
    public void testAnalyzeSort() {
        assertEquals("あいうえお", support.normalize("あいうえお"));
        assertEquals("アイウエオ", support.normalize("アイウエオ"));
        assertEquals("ァィゥェォ", support.normalize("ァィゥェォ"));
        assertEquals("ァィゥェォ", support.normalize("ｧｨｩｪｫ"));
        assertEquals("アイウエオ", support.normalize("ｱｲｳｴｵ"));
        assertEquals("亜伊鵜絵尾", support.normalize("亜伊鵜絵尾"));
        assertEquals("ABCDE", support.normalize("ABCDE"));
        assertEquals("ABCDE", support.normalize("ＡＢＣＤＥ"));
        assertEquals("αβγ", support.normalize("αβγ"));
        assertEquals("つんく♂", support.normalize("つんく♂"));
        assertEquals("bad communication", support.normalize("bad communication"));
        assertEquals("BAD COMMUNICATION", support.normalize("BAD COMMUNICATION"));
        assertEquals("BAD COMMUNICATION", support.normalize("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals("犬とネコ", support.normalize("犬とネコ"));
        assertEquals("読み", support.normalize("読み"));
        assertEquals("(読み)", support.normalize("(読み)"));
        assertEquals(" 「」()()[][];!!??##123", support.createReading(" 「」（）()［］[]；！!？?＃#１２３"));
        assertEquals("Cæsar", support.createReading("Cæsar"));
        assertEquals("Alfee", support.createReading("The Alfee"));
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
        support.analyze(mediaFile);
        return mediaFile;
    };

    @Test
    public void testanalyzeIfSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply(null, "あいうえお");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("あいうえお", artist.getArtistSort());

        artist = toMediaFile.apply(null, "アイウエオ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ァィゥェォ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｧｨｩｪｫ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｱｲｳｴｵ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "亜伊鵜絵尾");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("亜伊鵜絵尾", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ABCDE");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＡＢＣＤＥ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "αβγ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertEquals("αβγ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "つんく♂");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertEquals("つんく♂", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertEquals("BAD COMMUNICATION", artist.getArtistSort());

        artist = toMediaFile.apply(null, "部屋とYシャツと私");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertEquals("部屋とYシャツと私", artist.getArtistSort());

        artist = toMediaFile.apply(null, " 「」（）()［］[]；;！!？?＃#１２３");
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistReading());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistSort());

    }

    @Test
    public void testanalyzeIfNoSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        support.analyze(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("あいうえお", null);
        support.analyze(artist);
        assertEquals("あいうえお", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("アイウエオ", null);
        support.analyze(artist);
        assertEquals("アイウエオ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ァィゥェォ", null);
        support.analyze(artist);
        assertEquals("ァィゥェォ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｧｨｩｪｫ", null);
        support.analyze(artist);
        assertEquals("ｧｨｩｪｫ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｱｲｳｴｵ", null);
        support.analyze(artist);
        assertEquals("ｱｲｳｴｵ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("亜伊鵜絵尾", null);
        support.analyze(artist);
        assertEquals("亜伊鵜絵尾", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ABCDE", null);
        support.analyze(artist);
        assertEquals("ABCDE", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＡＢＣＤＥ", null);
        support.analyze(artist);
        assertEquals("ＡＢＣＤＥ", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("αβγ", null);
        support.analyze(artist);
        assertEquals("αβγ", artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("つんく♂", null);
        support.analyze(artist);
        assertEquals("つんく♂", artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", null);
        support.analyze(artist);
        assertEquals("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("部屋とYシャツと私", null);
        support.analyze(artist);
        assertEquals("部屋とYシャツと私", artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply(" 「」（）()［］[]；;！!？?＃#１２３", null);
        support.analyze(artist);
        assertEquals(" 「」（）()［］[]；;！!？?＃#１２３", artist.getArtist());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistReading());
        assertNull(artist.getArtistSort());

    }

    @Test
    public void testCreateIndexableName4MediaFile() {

        MediaFile file = toAnalyzedMediaFile.apply(null, "  ");
        assertEquals("  ", file.getPath());
        assertEquals("アイウエオ", toAnalyzedMediaFile.apply("あいうえお", "dummyPath").getArtistReading());

        assertEquals("dummyPath", support.createIndexableName(toAnalyzedMediaFile.apply("あいうえお", "dummyPath")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedMediaFile.apply("あいうえお", "非alpha")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedMediaFile.apply("アイウエオ", "非alpha")));
        assertEquals("ァィゥェォ", support.createIndexableName(toAnalyzedMediaFile.apply("ァィゥェォ", "非alpha")));
        assertEquals("ァィゥェォ", support.createIndexableName(toAnalyzedMediaFile.apply("ｧｨｩｪｫ", "非alpha")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedMediaFile.apply("ｱｲｳｴｵ", "非alpha")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedMediaFile.apply("亜伊鵜絵尾", "非alpha")));

        assertEquals("ABCDE", support.createIndexableName(toAnalyzedMediaFile.apply("ABCDE", "非alpha")));
        assertEquals("ABCDE", support.createIndexableName(toAnalyzedMediaFile.apply("ＡＢＣＤＥ", "非alpha")));
        assertEquals("アルファベータガンマ", support.createIndexableName(toAnalyzedMediaFile.apply("αβγ", "非alpha")));
        assertEquals("ツンク♂", support.createIndexableName(toAnalyzedMediaFile.apply("つんく♂", "非alpha")));

        assertEquals("BAD COMMUNICATION",
                support.createIndexableName(toAnalyzedMediaFile.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", "非alpha")));
        assertEquals("「」()()[][];;!!??##123",
                support.createIndexableName(toAnalyzedMediaFile.apply("「」（）()［］[]；;！!？?＃#１２３", "非alpha")));

        assertEquals("ゴウヒロミ", support.createIndexableName(toAnalyzedMediaFile.apply("ゴウヒロミ", "非alpha")));
        assertEquals("パミュパミュ", support.createIndexableName(toAnalyzedMediaFile.apply("ぱみゅぱみゅ", "非alpha")));

        file = toAnalyzedMediaFile.apply("倖田來未", "非alpha");
        file.setArtistSort("こうだくみ");
        file.setPath("非alpha");
        support.analyze(file);
        assertEquals("コウダクミ", support.createIndexableName(file));

    }

}
