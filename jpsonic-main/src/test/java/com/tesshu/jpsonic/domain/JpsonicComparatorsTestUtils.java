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
    } // @formatter:on

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d10() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d20() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d60() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d70() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d80() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d90() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d98() {
    }

    @ComparatorsDecisions.Conditions.isNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void d99() {
    }

    @ComparatorsDecisions.Conditions.isAlpha
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void dabcde() {
    }

    @ComparatorsDecisions.Conditions.isAlpha
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isUpper
    private void dＢＣＤＥＡ() {
    }

    @ComparatorsDecisions.Conditions.isHalfWidth
    @ComparatorsDecisions.Conditions.isLigature
    private void dĆḊÉÁḂ() {
    }

    @ComparatorsDecisions.Conditions.isAlpha
    @ComparatorsDecisions.Conditions.isHalfWidth
    @ComparatorsDecisions.Conditions.isUpper
    private void dDEABC() {
    }

    @ComparatorsDecisions.Conditions.isAlpha
    @ComparatorsDecisions.Conditions.isHalfWidth
    @ComparatorsDecisions.Conditions.isWithArticle
    
    private void dtheeabcd() {
    }

    @ComparatorsDecisions.Conditions.isAlphaNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void depisode1() {
    }

    @ComparatorsDecisions.Conditions.isAlphaNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void depisode2() {
    }

    @ComparatorsDecisions.Conditions.isAlphaNum
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void depisode19() {
    }

    @ComparatorsDecisions.Conditions.isChiChar
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isRequireJapaneseReadingAnalysis
    private void d亜伊鵜絵尾() {
    }

    @ComparatorsDecisions.Conditions.isSymbol
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isRequireJapaneseReadingAnalysis
    private void dαβγ() {
    }

    @ComparatorsDecisions.Conditions.isHira
    @ComparatorsDecisions.Conditions.isFullWidth
    private void dいうえおあ() {
    }

    @ComparatorsDecisions.Conditions.isSmallKana
    @ComparatorsDecisions.Conditions.isFullWidth
    private void dゥェォァィ() {
    }

    @ComparatorsDecisions.Conditions.isKata
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void dｴｵｱｲｳ() {
    }

    @ComparatorsDecisions.Conditions.isSmallKana
    @ComparatorsDecisions.Conditions.isHalfWidth
    private void dｪｫｧｨｩ() {
    }

    @ComparatorsDecisions.Conditions.isSmallKana
    @ComparatorsDecisions.Conditions.isFullWidth
    private void dぉぁぃぅぇ() {
    }

    @ComparatorsDecisions.Conditions.isKata
    @ComparatorsDecisions.Conditions.isFullWidth
    private void dオアイウエ() {
    }

    @ComparatorsDecisions.Conditions.isChiChar
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isRequireJapaneseReadingAnalysis
    private void d春夏秋冬() {
    }

    @ComparatorsDecisions.Conditions.isChiChar
    @ComparatorsDecisions.Conditions.isFullWidth
    private void d貼られる() {
    }

    @ComparatorsDecisions.Conditions.isKata
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isPsound
    private void dパラレル() {
    }

    @ComparatorsDecisions.Conditions.isChiChar
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isDullness
    @ComparatorsDecisions.Conditions.isRequireJapaneseReadingAnalysis
    private void d馬力() {
    }

    @ComparatorsDecisions.Conditions.isChiChar
    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isRequireJapaneseReadingAnalysis
    private void d張り切る() {
    }

    @ComparatorsDecisions.Conditions.isHira
    @ComparatorsDecisions.Conditions.isFullWidth
    private void dはるなつあきふゆ() {
    }

    @ComparatorsDecisions.Conditions.isFullWidth
    @ComparatorsDecisions.Conditions.isSymbol
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
                    "80",
                    "90",
                    "98", // Enter year in year field
                    "99", // Enter year in year field
                    "abcde", // Enter Japanese in the sort field
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
            file.setAlbumArtist(name);
            file.setAlbumArtistSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setAlbumArtist(name);
            file.setAlbumArtistSort("ビーシーディーイーエー");
        } else {
            file.setAlbumArtist(name);
        }

        file.setArtist(name);
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
