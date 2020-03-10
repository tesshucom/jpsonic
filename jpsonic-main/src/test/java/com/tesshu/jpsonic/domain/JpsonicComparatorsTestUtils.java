package com.tesshu.jpsonic.domain;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.Playlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.reverse;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.assertEquals;

@Component
public class JpsonicComparatorsTestUtils {

    @Documented
    private @interface ComparatorsDecisions { // @formatter:off
        @interface Conditions {
            @interface name {
                @interface isAlpha {}
                @interface isNum {}
                @interface isAlphaNum {}
                @interface isFullWidth {}
                @interface isHalfWidth {}
                @interface isUpper {}
                @interface isLigature {}
                @interface isChiChar {}
                @interface isHira {}
                @interface isKata {}
                @interface isSmallKana {}
                @interface isDullness {}
                @interface isPsound {}
                @interface isSymbol {}
                @interface isWithArticle {}
                @interface isRequireJapaneseReadingAnalysis {}
            }
            @interface year {
                @interface isSpecified {}
            }
            @interface reading {
                @interface isJapaneseAll {}
                @interface isFirstWithEnAndContainsJp {}
            }
        }
    } // @formatter:on

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.year.isSpecified
    private void d10() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void d20() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void d60() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void d70() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.year.isSpecified
    private void d98() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.year.isSpecified
    private void d99() {
    }

    @ComparatorsDecisions.Conditions.name.isAlpha
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.reading.isJapaneseAll
    private void dabcde() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.name.isHira
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.reading.isFirstWithEnAndContainsJp
    private void dabcいうえおあ() {
    }

    @ComparatorsDecisions.Conditions.name.isNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.reading.isFirstWithEnAndContainsJp
    private void dabc亜伊鵜絵尾() {
    }

    @ComparatorsDecisions.Conditions.name.isAlpha
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isUpper
    @ComparatorsDecisions.Conditions.reading.isJapaneseAll
    private void dＢＣＤＥＡ() {
    }

    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.name.isLigature
    private void dĆḊÉÁḂ() {
    }

    @ComparatorsDecisions.Conditions.name.isAlpha
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.name.isUpper
    private void dDEABC() {
    }

    @ComparatorsDecisions.Conditions.name.isAlpha
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    @ComparatorsDecisions.Conditions.name.isWithArticle
    
    private void dtheeabcd() {
    }

    @ComparatorsDecisions.Conditions.name.isAlphaNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void depisode1() {
    }

    @ComparatorsDecisions.Conditions.name.isAlphaNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void depisode2() {
    }

    @ComparatorsDecisions.Conditions.name.isAlphaNum
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void depisode19() {
    }

    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isRequireJapaneseReadingAnalysis
    private void d亜伊鵜絵尾() {
    }

    @ComparatorsDecisions.Conditions.name.isSymbol
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isRequireJapaneseReadingAnalysis
    private void dαβγ() {
    }

    @ComparatorsDecisions.Conditions.name.isHira
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void dいうえおあ() {
    }

    @ComparatorsDecisions.Conditions.name.isSmallKana
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void dゥェォァィ() {
    }

    @ComparatorsDecisions.Conditions.name.isKata
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void dｴｵｱｲｳ() {
    }

    @ComparatorsDecisions.Conditions.name.isSmallKana
    @ComparatorsDecisions.Conditions.name.isHalfWidth
    private void dｪｫｧｨｩ() {
    }

    @ComparatorsDecisions.Conditions.name.isSmallKana
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void dぉぁぃぅぇ() {
    }

    @ComparatorsDecisions.Conditions.name.isKata
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void dオアイウエ() {
    }

    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isRequireJapaneseReadingAnalysis
    private void d春夏秋冬() {
    }

    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void d貼られる() {
    }

    @ComparatorsDecisions.Conditions.name.isKata
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isPsound
    private void dパラレル() {
    }

    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isDullness
    @ComparatorsDecisions.Conditions.name.isRequireJapaneseReadingAnalysis
    private void d馬力() {
    }

    @ComparatorsDecisions.Conditions.name.isChiChar
    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isRequireJapaneseReadingAnalysis
    private void d張り切る() {
    }

    @ComparatorsDecisions.Conditions.name.isHira
    @ComparatorsDecisions.Conditions.name.isFullWidth
    private void dはるなつあきふゆ() {
    }

    @ComparatorsDecisions.Conditions.name.isFullWidth
    @ComparatorsDecisions.Conditions.name.isSymbol
    private void dSymbolくんつ() {
    }

    /**
     * Dictionary order that Japanese feel natural.
     * 
     *  - Generally, it is divided into "Eng/Num" and Japanese.
     *  - In general, ligatures are read in English
     *    (unless ligatures are intentionally separated in a dictionary).
     *  - Frequently used symbols are read in Japanese.
     *  - "Eng/Num" is arranged based on the notation.
     *    However, Japanese is arranged based on the readings.
     *  - Popular English words are given Japanese readings and are treated in much the
     *    same way as Japanese. However,
     *    the japanese distinguish between alphabetic and Japanese notation.
     * 
     * If arranged in code order simply,
     * it will be difficult for the Japanese people to identify the order.
     * Especially in the case of UNICODE,
     * chinese characters is managed using a common table in multiple countries.
     * Therefore, in order to arrange correctly in Japanese,
     * a function to convert to Japanese reading and support for sort tags are required.
     */
    private final static List<String> jPSonicNaturalList = // @formatter:off
            unmodifiableList(Arrays.asList(
                    "10", // Enter year in year field
                    "20",
                    "50",
                    "60",
                    "70",
                    "98", // Enter year in year field
                    "99", // Enter year in year field
                    "abcde", // Enter Japanese in the sort field
                    "abcいうえおあ", // Turn over by reading
                    "abc亜伊鵜絵尾", // Turn over by reading
                    "ＢＣＤＥＡ", // Enter Japanese in the sort field
                    "ĆḊÉÁḂ",
                    "DEABC",
                    "the eabcd",
                    "episode 1",
                    "episode 2",
                    "episode 19",
                    "亜伊鵜絵尾",
                    "αβγ",
                    "いうえおあ",
                    "ゥェォァィ",
                    "ｴｵｱｲｳ",
                    "ｪｫｧｨｩ",
                    "ぉぁぃぅぇ",
                    "オアイウエ",
                    "春夏秋冬",
                    "貼られる",
                    "パラレル",
                    "馬力",
                    "張り切る",
                    "はるなつあきふゆ",
                    "♂くんつ")); // @formatter:on

    @Autowired
    private JapaneseReadingUtils utils;

    private final Function<String, MediaFile> toMediaArtist = (name) -> {

        MediaFile file = new MediaFile();
        if ("abcde".equals(name)) {
            file.setArtist(name);
            file.setAlbumArtist(name);
            file.setArtistSort("エービーシーディーイー");
            file.setAlbumArtistSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setArtist(name);
            file.setAlbumArtist(name);
            file.setArtistSort("ビーシーディーイーエー");
            file.setAlbumArtistSort("ビーシーディーイーエー");
        } else if ("abcいうえおあ".equals(name)) {
            file.setArtist(name);
            file.setAlbumArtist(name);
            file.setArtistSort("abcあいうえお");
            file.setAlbumArtistSort("abcあいうえお");
        } else if ("abc亜伊鵜絵尾".equals(name)) {
            file.setArtist(name);
            file.setAlbumArtist(name);
            file.setArtistSort("abcいうえおあ");
            file.setAlbumArtistSort("abcいうえおあ");
        } else {
            file.setArtist(name);
            file.setAlbumArtist(name);
        }

        file.setTitle(name);
        file.setPath(name);
        file.setMediaType(MediaType.DIRECTORY);

        utils.analyze(file);

        return file;

    };

    private final BiFunction<String, Integer, Genre> toGenre = (name, count) -> {
        return new Genre(name, count, count);
    };

    private final Function<String, Playlist> toPlaylist = (name) -> {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        return playlist;
    };

    private final Function<String, Artist> toArtist = (name) -> {
        MediaFile file = toMediaArtist.apply(name);
        Artist artist = new Artist();
        artist.setOrder(-1);
        artist.setName(file.getAlbumArtist());
        artist.setReading(file.getAlbumArtistReading());
        artist.setSort(file.getAlbumArtistSort());
        return artist;
    };

    private final Function<String, MediaFile> toMediaAlbum = (name) -> {

        MediaFile file = new MediaFile();

        if ("abcde".equals(name)) {
            file.setAlbumName(name);
            file.setAlbumSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setAlbumName(name);
            file.setAlbumSort("ビーシーディーイーエー");
        } else if ("abcいうえおあ".equals(name)) {
            file.setAlbumName(name);
            file.setAlbumSort("abcあいうえお");
        } else if ("abc亜伊鵜絵尾".equals(name)) {
            file.setAlbumName(name);
            file.setAlbumSort("abcいうえおあ");
        } else {
            file.setAlbumName(name);
        }

        if ("98".equals(name)) {
            file.setYear(1998);
        } else if ("99".equals(name)) {
            file.setYear(1999);
        } else if ("10".equals(name)) {
            file.setYear(2010);
        } else {
            file.setAlbumName(name);
        }

        file.setTitle(name);
        file.setPath(name);
        file.setMediaType(MediaType.ALBUM);

        utils.analyze(file);

        return file;
    };

    private final Function<String, Album> toAlbum = (name) -> {

        MediaFile file = toMediaAlbum.apply(name);
        Album album = new Album();
        album.setOrder(-1);

        String artist;
        String reading;
        String sort;
        if (isEmpty(file.getAlbumArtist())) {
            artist = file.getArtist();
            reading = file.getArtistReading();
            sort = file.getArtistSort();
        } else {
            artist = file.getAlbumArtist();
            reading = file.getAlbumArtistReading();
            sort = file.getAlbumArtistSort();
        }

        album.setName(file.getAlbumName());
        album.setNameReading(file.getAlbumReading());
        album.setNameSort(file.getAlbumSort());
        album.setArtist(artist);
        album.setArtistReading(reading);
        album.setArtistSort(sort);
        album.setYear(file.getYear());

        return album;
    };

    List<Genre> createReversedGenres() {
        List<Genre> genres = new ArrayList<>();
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            genres.add(toGenre.apply(jPSonicNaturalList.get(i), jPSonicNaturalList.size() - i));
        }
        reverse(genres);
        return genres;
    }

    List<Playlist> createReversedPlaylists() {
        List<Playlist> playlists = jPSonicNaturalList.stream().map(toPlaylist).collect(toList());
        reverse(playlists);
        return playlists;
    }

    List<Artist> createReversedArtists() {
        List<Artist> artists = jPSonicNaturalList.stream().map(toArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    List<Album> createReversedAlbums() {
        List<Album> albums = jPSonicNaturalList.stream().map(toAlbum).collect(toList());
        reverse(albums);
        return albums;
    }

    List<MediaFile> createReversedMediArtists() {
        List<MediaFile> artists = jPSonicNaturalList.stream().map(toMediaArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    List<MediaFile> createReversedMediAlbums() {
        List<MediaFile> albums = jPSonicNaturalList.stream().map(toMediaAlbum).collect(toList());
        reverse(albums);
        return albums;
    }

    MediaFile createVariousMedifile() {
        MediaFile file = new MediaFile();
        file.setArtist("various");
        return file;
    }

    static void assertGenreOrder(List<Genre> genres, Integer... ignores) {
        assertEquals(jPSonicNaturalList.size(), genres.size());
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            if (!(0 <= Arrays.binarySearch(ignores, i))) {
                assertEquals("(" + i + ") -> ", jPSonicNaturalList.get(i), genres.get(i).getName());
            }
        }
    }

    static void assertPlaylistOrder(List<Playlist> playlists, Integer... ignores) {
        assertEquals(jPSonicNaturalList.size(), playlists.size());
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            if (!(0 <= Arrays.binarySearch(ignores, i))) {
                assertEquals("(" + i + ") -> ", jPSonicNaturalList.get(i), playlists.get(i).getName());
            }
        }
    }

    static void assertArtistOrder(List<Artist> artists, Integer... ignores) {
        assertEquals(jPSonicNaturalList.size(), artists.size());
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            if (!(0 <= Arrays.binarySearch(ignores, i))) {
                assertEquals("(" + i + ") -> ", jPSonicNaturalList.get(i), artists.get(i).getName());
            }
        }
    }

    static void assertAlbumOrder(List<Album> albums, Integer... ignores) {
        assertEquals(jPSonicNaturalList.size(), albums.size());
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            if (!(0 <= Arrays.binarySearch(ignores, i))) {
                assertEquals("(" + i + ") -> ", jPSonicNaturalList.get(i), albums.get(i).getName());
            }
        }
    }

    static void assertMediafileOrder(List<MediaFile> files, Integer... ignores) {
        assertEquals(jPSonicNaturalList.size(), files.size());
        for (int i = 0; i < jPSonicNaturalList.size(); i++) {
            if (!(0 <= Arrays.binarySearch(ignores, i))) {
                assertEquals("(" + i + ") -> ", jPSonicNaturalList.get(i), files.get(i).getName());
            }
        }
    }
}
