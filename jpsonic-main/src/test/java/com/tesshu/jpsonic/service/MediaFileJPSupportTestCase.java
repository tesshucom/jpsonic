package com.tesshu.jpsonic.service;

import static org.junit.Assert.*;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.airsonic.player.domain.Artist;
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

    private BiFunction<String, String, MediaFile> toMediaFile = (artist, artistSort) -> {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist(artist);
        mediaFile.setArtistSort(artistSort);
        ;
        return mediaFile;
    };

    private BiFunction<String, String, MediaFile> toAnalyzedMediaFile = (artist, path) -> {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setArtist(artist);
        mediaFile.setArtistSort(artist);
        mediaFile.setPath(path);
        support.analyzeArtist(mediaFile);
        return mediaFile;
    };

    private Function<String, Artist> toAnalyzedArtist = (name) -> {
        Artist artist = new Artist();
        artist.setName(name);
        artist.setReading(name);
        MediaFile parent = toAnalyzedMediaFile.apply(name, name);
        parent.setAlbumArtist(name);
        parent.setAlbumArtistSort(name);
        support.analyzeArtist(parent, artist);        
        return artist;
    };

    @Test
    public void testAnalyzeArtistIfSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply(null, "あいうえお");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("あいうえお", artist.getArtistSort());

        artist = toMediaFile.apply(null, "アイウエオ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ァィゥェォ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｧｨｩｪｫ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertEquals("ァィゥェォ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ｱｲｳｴｵ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("アイウエオ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "亜伊鵜絵尾");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertEquals("亜伊鵜絵尾", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ABCDE");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＡＢＣＤＥ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertEquals("ABCDE", artist.getArtistSort());

        artist = toMediaFile.apply(null, "αβγ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertEquals("αβγ", artist.getArtistSort());

        artist = toMediaFile.apply(null, "つんく♂");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertEquals("つんく♂", artist.getArtistSort());

        artist = toMediaFile.apply(null, "ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertEquals("BAD COMMUNICATION", artist.getArtistSort());

        artist = toMediaFile.apply(null, "部屋とYシャツと私");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertEquals("部屋とYシャツと私", artist.getArtistSort());

        artist = toMediaFile.apply(null, " 「」（）()［］[]；;！!？?＃#１２３");
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistReading());
        assertEquals(" 「」()()[][];;!!??##123", artist.getArtistSort());

    }

    @Test
    public void testAnalyzeArtistIfNoSortExists() {

        MediaFile artist = toMediaFile.apply(null, null);
        support.analyzeArtist(artist);
        assertNull(artist.getArtist());
        assertNull(artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("あいうえお", null);
        support.analyzeArtist(artist);
        assertEquals("あいうえお", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("アイウエオ", null);
        support.analyzeArtist(artist);
        assertEquals("アイウエオ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ァィゥェォ", null);
        support.analyzeArtist(artist);
        assertEquals("ァィゥェォ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｧｨｩｪｫ", null);
        support.analyzeArtist(artist);
        assertEquals("ｧｨｩｪｫ", artist.getArtist());
        assertEquals("ァィゥェォ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ｱｲｳｴｵ", null);
        support.analyzeArtist(artist);
        assertEquals("ｱｲｳｴｵ", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("亜伊鵜絵尾", null);
        support.analyzeArtist(artist);
        assertEquals("亜伊鵜絵尾", artist.getArtist());
        assertEquals("アイウエオ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ABCDE", null);
        support.analyzeArtist(artist);
        assertEquals("ABCDE", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＡＢＣＤＥ", null);
        support.analyzeArtist(artist);
        assertEquals("ＡＢＣＤＥ", artist.getArtist());
        assertEquals("ABCDE", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("αβγ", null);
        support.analyzeArtist(artist);
        assertEquals("αβγ", artist.getArtist());
        assertEquals("アルファベータガンマ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("つんく♂", null);
        support.analyzeArtist(artist);
        assertEquals("つんく♂", artist.getArtist());
        assertEquals("ツンク♂", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", null);
        support.analyzeArtist(artist);
        assertEquals("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ", artist.getArtist());
        assertEquals("BAD COMMUNICATION", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply("部屋とYシャツと私", null);
        support.analyzeArtist(artist);
        assertEquals("部屋とYシャツと私", artist.getArtist());
        assertEquals("ヘヤトYシャツトワタシ", artist.getArtistReading());
        assertNull(artist.getArtistSort());

        artist = toMediaFile.apply(" 「」（）()［］[]；;！!？?＃#１２３", null);
        support.analyzeArtist(artist);
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
        support.analyzeArtist(file);
        assertEquals("コウダクミ", support.createIndexableName(file));

    }

    @Test
    public void testCreateIndexableName4Artist() {

        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedArtist.apply("あいうえお")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedArtist.apply("アイウエオ")));
        assertEquals("ァィゥェォ", support.createIndexableName(toAnalyzedArtist.apply("ァィゥェォ")));
        assertEquals("ァィゥェォ", support.createIndexableName(toAnalyzedArtist.apply("ｧｨｩｪｫ")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedArtist.apply("ｱｲｳｴｵ")));
        assertEquals("アイウエオ", support.createIndexableName(toAnalyzedArtist.apply("亜伊鵜絵尾")));
        assertEquals("ABCDE", support.createIndexableName(toAnalyzedArtist.apply("ABCDE")));
        assertEquals("ABCDE", support.createIndexableName(toAnalyzedArtist.apply("ＡＢＣＤＥ")));
        assertEquals("アルファベータガンマ", support.createIndexableName(toAnalyzedArtist.apply("αβγ")));
        assertEquals("ツンク♂", support.createIndexableName(toAnalyzedArtist.apply("つんく♂")));
        assertEquals("BAD COMMUNICATION", support.createIndexableName(toAnalyzedArtist.apply("ＢＡＤ　ＣＯＭＭＵＮＩＣＡＴＩＯＮ")));
        assertEquals("「」()()[][];;!!??##123",
                support.createIndexableName(toAnalyzedArtist.apply("「」（）()［］[]；;！!？?＃#１２３")));
        assertEquals("ゴウヒロミ", support.createIndexableName(toAnalyzedArtist.apply("ゴウヒロミ")));
        assertEquals("パミュパミュ", support.createIndexableName(toAnalyzedArtist.apply("ぱみゅぱみゅ")));

        Artist artist = new Artist();
        artist.setName("倖田來未");
        artist.setSort("コウダクミ");
        MediaFile parent = toAnalyzedMediaFile.apply("倖田來未", "非alpha");
        parent.setAlbumArtist("倖田來未");
        parent.setAlbumArtistSort("コウダクミ");
        support.analyzeArtist(parent, artist);
        assertEquals("コウダクミ", support.createIndexableName(artist));

    }
    
}
