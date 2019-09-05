package com.tesshu.jpsonic.service;

import static org.junit.Assert.*;

import java.util.function.BiFunction;

import org.airsonic.player.domain.MediaFile;
import org.junit.Test;

public class MediaFileJPSupportTestCase {

    MediaFileJPSupport support = new MediaFileJPSupport();

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
    }

    @Test
    public void testAnalyzeSort() {
        assertEquals("あいうえお", support.analyzeSort("あいうえお"));
        assertEquals("アイウエオ", support.analyzeSort("アイウエオ"));
        assertEquals("ァィゥェォ", support.analyzeSort("ァィゥェォ"));
        assertEquals("ァィゥェォ", support.analyzeSort("ｧｨｩｪｫ"));
        assertEquals("アイウエオ", support.analyzeSort("ｱｲｳｴｵ"));
        assertEquals("亜伊鵜絵尾", support.analyzeSort("亜伊鵜絵尾"));
        assertEquals("ABCDE", support.analyzeSort("ABCDE"));
        assertEquals("ABCDE", support.analyzeSort("ＡＢＣＤＥ"));
        assertEquals("αβγ", support.analyzeSort("αβγ"));
        assertEquals("つんく♂", support.analyzeSort("つんく♂"));
        assertEquals("bad communication", support.analyzeSort("bad communication"));
        assertEquals("BAD COMMUNICATION", support.analyzeSort("BAD COMMUNICATION"));
        assertEquals("BAD COMMUNICATION", support.analyzeSort("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ"));
        assertEquals("犬とネコ", support.analyzeSort("犬とネコ"));
        assertEquals("読み", support.analyzeSort("読み"));
        assertEquals("(読み)", support.analyzeSort("(読み)"));
        assertEquals(" 「」()()[][];!!??##123", support.createReading(" 「」（）()［］[]；！!？?＃#１２３"));
    }

    private BiFunction<String, String, MediaFile> toArtist = (artist, artistSort) -> {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist(artist);
        mediaFile.setArtistSort(artistSort);
        ;
        return mediaFile;
    };

    @Test
    public void testAnalyzeArtistIfSortExists() {

        MediaFile artist = toArtist.apply(null, null);
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply(null, "あいうえお");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("あいうえお", artist.getArtistSort());

        artist = toArtist.apply(null, "アイウエオ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toArtist.apply(null, "ァィゥェォ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toArtist.apply(null, "ｧｨｩｪｫ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toArtist.apply(null, "ｱｲｳｴｵ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toArtist.apply(null, "亜伊鵜絵尾");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("亜伊鵜絵尾", artist.getArtistSort());

        artist = toArtist.apply(null, "ABCDE");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toArtist.apply(null, "ＡＢＣＤＥ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toArtist.apply(null, "αβγ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertEquals("αβγ", artist.getArtistSort());

        artist = toArtist.apply(null, "つんく♂");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertEquals("つんく♂", artist.getArtistSort());

        artist = toArtist.apply(null, "ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertEquals("BAD COMMUNICATION", artist.getArtistSort());

        artist = toArtist.apply(null, "部屋とYシャツと私");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertEquals("部屋とYシャツと私", artist.getArtistSort());

        artist = toArtist.apply(null, " 「」（）()［］[]；;！!？?＃#１２３");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistReading());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistSort());

    }

    @Test
    public void testAnalyzeArtistIfNoSortExists() {

        MediaFile artist = toArtist.apply(null, null);
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("あいうえお", null);
        support.analyzeArtist(artist);
        assertEquals("あいうえお", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("アイウエオ", null);
        support.analyzeArtist(artist);
        assertEquals("アイウエオ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ァィゥェォ", null);
        support.analyzeArtist(artist);
        assertEquals("ァィゥェォ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ｧｨｩｪｫ", null);
        support.analyzeArtist(artist);
        assertEquals("ｧｨｩｪｫ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ｱｲｳｴｵ", null);
        support.analyzeArtist(artist);
        assertEquals("ｱｲｳｴｵ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("亜伊鵜絵尾", null);
        support.analyzeArtist(artist);
        assertEquals("亜伊鵜絵尾", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ABCDE", null);
        support.analyzeArtist(artist);
        assertEquals("ABCDE", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ＡＢＣＤＥ", null);
        support.analyzeArtist(artist);
        assertEquals("ＡＢＣＤＥ", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("αβγ", null);
        support.analyzeArtist(artist);
        assertEquals("αβγ", artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("つんく♂", null);
        support.analyzeArtist(artist);
        assertEquals("つんく♂", artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", null);
        support.analyzeArtist(artist);
        assertEquals("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply("部屋とYシャツと私", null);
        support.analyzeArtist(artist);
        assertEquals("部屋とYシャツと私", artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toArtist.apply(" 「」（）()［］[]；;！!？?＃#１２３", null);
        support.analyzeArtist(artist);
        assertEquals(" 「」（）()［］[]；;！!？?＃#１２３", artist.getArtist());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistReading());
        assertNull(artist.getArtistSort());

    }

}
