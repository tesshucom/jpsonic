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

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicIndex.SortableArtist;
import org.airsonic.player.domain.MusicIndex.SortableArtistWithMediaFiles;
import org.airsonic.player.domain.Playlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
public class JpsonicComparatorsTestUtils {

    @Autowired
    private JpsonicComparators comparators;
    @Autowired
    private JapaneseReadingUtils utils;

    /*
     * Dictionary order that Japanese feel natural.
     * 
     * - Generally, it is divided into "Eng/Num" and Japanese. - In general, ligatures are read in English (unless
     * ligatures are intentionally separated in a dictionary). - Frequently used symbols are read in Japanese. -
     * "Eng/Num" is arranged based on the notation. However, Japanese is arranged based on the readings. - Popular
     * English words are given Japanese readings and are treated in much the same way as Japanese. However, the japanese
     * distinguish between alphabetic and Japanese notation.
     * 
     * If arranged in code order simply, it will be difficult for the Japanese people to identify the order. Especially
     * in the case of UNICODE, chinese characters is managed using a common table in multiple countries. Therefore, in
     * order to arrange correctly in Japanese, a function to convert to Japanese reading and support for sort tags are
     * required.
     */
    public static final List<String> JPSONIC_NATURAL_LIST = Collections.unmodifiableList(Arrays.asList("10", // Enter
                                                                                                             // year in
                                                                                                             // year
                                                                                                             // field
            "20", "50", "60", "70", "98", // Enter year in year field
            "99", // Enter year in year field
            "abcde", // Enter Japanese in the sort field
            "abcいうえおあ", // Turn over by reading
            "abc亜伊鵜絵尾", // Turn over by reading (Register by replacing "reading" intentionally)
            "ＢＣＤＥＡ", // Enter Japanese in the sort field
            "ĆḊÉÁḂ", "DEABC", "the eabcd", "episode 1", "episode 2", "episode 19", "亜伊鵜絵尾", "αβγ", "いうえおあ", "ゥェォァィ",
            "ｴｵｱｲｳ", "ｪｫｧｨｩ", "ぉぁぃぅぇ", "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ", "♂くんつ"));

    /*
     * Expected sequence number. Whether serial number processing has been performed can be determined by some elements
     * included in jPSonicNaturalList. Use this list if need to do a full pattern test.
     */
    private static final List<String> ALPHA_NUM_LIST = Collections
            .unmodifiableList(Arrays.asList("09X Radonius", "10X Radonius", "20X Radonius", "20X Radonius Prime",
                    "30X Radonius", "40X Radonius", "200X Radonius", "1000X Radonius Maximus", "Allegia 6R Clasteron",
                    "Allegia 50B Clasteron", "Allegia 50 Clasteron", "Allegia 51 Clasteron", "Allegia 500 Clasteron",
                    "Alpha 2", "Alpha 2A", "Alpha 2A-900", "Alpha 2A-8000", "Alpha 100", "Alpha 200",
                    "Callisto Morphamax", "Callisto Morphamax 500", "Callisto Morphamax 600", "Callisto Morphamax 700",
                    "Callisto Morphamax 5000", "Callisto Morphamax 6000 SE", "Callisto Morphamax 6000 SE2",
                    "Callisto Morphamax 7000", "Xiph Xlater 5", "Xiph Xlater 40", "Xiph Xlater 50", "Xiph Xlater 58",
                    "Xiph Xlater 300", "Xiph Xlater 500", "Xiph Xlater 2000", "Xiph Xlater 5000", "Xiph Xlater 10000"));

    private final Function<String, MediaFile> toMediaArtist = (name) -> {

        MediaFile file = new MediaFile();

        file.setArtist(name);
        file.setAlbumArtist(name);
        if ("abcde".equals(name)) {
            file.setArtistSort("エービーシーディーイー");
            file.setAlbumArtistSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setArtistSort("ビーシーディーイーエー");
            file.setAlbumArtistSort("ビーシーディーイーエー");
        } else if ("abcいうえおあ".equals(name)) {
            file.setArtistSort("abcあいうえお");
            file.setAlbumArtistSort("abcあいうえお");
        } else if ("abc亜伊鵜絵尾".equals(name)) {
            file.setArtistSort("abcいうえおあ");
            file.setAlbumArtistSort("abcいうえおあ");
        }

        file.setTitle(name);
        file.setPath(name);
        file.setMediaType(MediaType.DIRECTORY);

        utils.analyze(file);

        return file;
    };

    private final BiFunction<String, Integer, Genre> toGenre = (name, count) -> new Genre(name, count, count);

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

    private final Function<String, SortableArtist> toSortableArtist = (name) -> {
        MediaFile file = toMediaArtist.apply(name);
        return new SortableArtistWithMediaFiles(file.getArtist(), file.getArtistReading(),
                comparators.sortableArtistOrder());
    };

    private final BiFunction<String, Integer, MediaFile> toMediaSong = (name, trackNumber) -> {

        MediaFile file = new MediaFile();
        file.setArtist(name);
        file.setAlbumName(name);
        file.setTitle(name);
        if ("abcde".equals(name)) {
            file.setArtistSort("エービーシーディーイー");
            file.setAlbumSort("エービーシーディーイー");
            file.setTitleSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setArtistSort("ビーシーディーイーエー");
            file.setAlbumSort("ビーシーディーイーエー");
            file.setTitleSort("ビーシーディーイーエー");
        } else if ("abcいうえおあ".equals(name)) {
            file.setArtistSort("abcあいうえお");
            file.setAlbumSort("abcあいうえお");
            file.setTitleSort("abcあいうえお");
        } else if ("abc亜伊鵜絵尾".equals(name)) {
            file.setArtistSort("abcいうえおあ");
            file.setAlbumSort("abcいうえおあ");
            file.setTitleSort("abcいうえおあ");
        }
        file.setTrackNumber(trackNumber);
        file.setPath(name);
        file.setMediaType(MediaType.MUSIC);

        utils.analyze(file);

        return file;
    };

    private final Function<String, MediaFile> toMediaAlbum = (name) -> {

        MediaFile file = new MediaFile();

        file.setAlbumName(name);
        if ("abcde".equals(name)) {
            file.setAlbumSort("エービーシーディーイー");
        } else if ("ＢＣＤＥＡ".equals(name)) {
            file.setAlbumSort("ビーシーディーイーエー");
        } else if ("abcいうえおあ".equals(name)) {
            file.setAlbumSort("abcあいうえお");
        } else if ("abc亜伊鵜絵尾".equals(name)) {
            file.setAlbumSort("abcいうえおあ");
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

    public static void assertAlbumOrder(List<Album> albums, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), albums.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), albums.get(i).getName());
            }
        }
    }

    public static boolean assertAlphanumArtistOrder(List<Artist> artists, Integer... ignores) {
        assertEquals(ALPHA_NUM_LIST.size(), artists.size());
        for (int i = 0; i < ALPHA_NUM_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", ALPHA_NUM_LIST.get(i), artists.get(i).getName());
            }
        }
        return true;
    }

    public static void assertArtistOrder(List<Artist> artists, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), artists.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), artists.get(i).getName());
            }
        }
    }

    public static void assertGenreOrder(List<Genre> genres, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), genres.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), genres.get(i).getName());
            }
        }
    }

    public static void assertMediafileOrder(List<MediaFile> files, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), files.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), files.get(i).getName());
            }
        }
    }

    public static void assertPlaylistOrder(List<Playlist> playlists, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), playlists.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), playlists.get(i).getName());
            }
        }
    }

    public static void assertSortableArtistOrder(List<SortableArtist> artists, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), artists.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), artists.get(i).getName());
            }
        }
    }

    public static boolean validateNaturalList(List<String> l, Integer... ignores) {
        assertEquals(JPSONIC_NATURAL_LIST.size(), l.size());
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            if (0 > Arrays.binarySearch(ignores, i)) {
                assertEquals("(" + i + ") -> ", JPSONIC_NATURAL_LIST.get(i), l.get(i));
            }
        }
        return true;
    }

    public List<Album> createReversedAlbums() {
        List<Album> albums = JPSONIC_NATURAL_LIST.stream().map(toAlbum).collect(toList());
        reverse(albums);
        return albums;
    }

    public List<Artist> createReversedAlphanum() {
        List<Artist> artists = ALPHA_NUM_LIST.stream().map(toArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    public List<Artist> createReversedArtists() {
        List<Artist> artists = JPSONIC_NATURAL_LIST.stream().map(toArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    public List<Genre> createReversedGenres() {
        List<Genre> genres = new ArrayList<>();
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            genres.add(toGenre.apply(JPSONIC_NATURAL_LIST.get(i), JPSONIC_NATURAL_LIST.size() - i));
        }
        reverse(genres);
        return genres;
    }

    public List<MediaFile> createReversedMediaSongs() {
        List<MediaFile> songs = new ArrayList<>();
        for (int i = 0; i < JPSONIC_NATURAL_LIST.size(); i++) {
            songs.add(toMediaSong.apply(JPSONIC_NATURAL_LIST.get(i), JPSONIC_NATURAL_LIST.size() - i));
        }
        reverse(songs);
        return songs;
    }

    public List<MediaFile> createReversedMediAlbums() {
        List<MediaFile> albums = JPSONIC_NATURAL_LIST.stream().map(toMediaAlbum).collect(toList());
        reverse(albums);
        return albums;
    }

    public List<MediaFile> createReversedMediArtists() {
        List<MediaFile> artists = JPSONIC_NATURAL_LIST.stream().map(toMediaArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    public List<Playlist> createReversedPlaylists() {
        List<Playlist> playlists = JPSONIC_NATURAL_LIST.stream().map(toPlaylist).collect(toList());
        reverse(playlists);
        return playlists;
    }

    public List<SortableArtist> createReversedSortableArtists() {
        List<SortableArtist> artists = JPSONIC_NATURAL_LIST.stream().map(toSortableArtist).collect(toList());
        reverse(artists);
        return artists;
    }

    public MediaFile createVariousMedifile() {
        MediaFile file = new MediaFile();
        file.setArtist("various");
        return file;
    }

}
