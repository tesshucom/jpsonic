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

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.text.Collator;
import java.util.Comparator;
import java.util.regex.Pattern;

import static org.springframework.util.ObjectUtils.isEmpty;

@Component
@DependsOn({ "settingsService", "japaneseReadingUtils" })
public class JpsonicComparators {

    private final Pattern isVarious = Pattern.compile("^various.*$");

    private final SettingsService settingsService;

    private final JapaneseReadingUtils utils;

    public JpsonicComparators(SettingsService settingsService, JapaneseReadingUtils utils) {
        super();
        this.settingsService = settingsService;
        this.utils = utils;
    }

    public Collator createCollator() {
        Collator collator = Collator.getInstance(settingsService.getLocale());
        return settingsService.isSortAlphanum() ? new AlphanumWrapper(collator) : collator;
    }

    public Comparator<Artist> artistOrder() {
        return new Comparator<Artist>() {

            private final Collator c = createCollator();

            @Override
            public int compare(Artist o1, Artist o2) {
                if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                    return o1.getOrder() - o2.getOrder();
                }
                return c.compare(o1.getReading(), o2.getReading());
            }
        };
    }

    /**
     * Returns a comparator that sorts in dictionary order regardless of the setting.
     * (Used places are limited)
     * @return Comparator
     */
    public Comparator<Album> albumAlphabeticalOrder() {
        return new Comparator<Album>() {

            private final Collator c = createCollator();

            @Override
            public int compare(Album o1, Album o2) {
                if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                    return o1.getOrder() - o2.getOrder();
                }
                return c.compare(o1.getNameReading(), o2.getNameReading());
            }
        };
    }

    /**
     * Returns a Comparator that sorts by year or dictionary order according to the settings
     * @return Comparator
     */
    public Comparator<Album> albumOrder() {
        return new Comparator<Album>() {

            private final Comparator<Object> c = createCollator();

            private final boolean isByYear = settingsService.isSortAlbumsByYear();

            @Override
            public int compare(Album o1, Album o2) {
                if (isByYear) {
                    return nullSafeCompare(o1.getYear(), o2.getYear(), false);
                } else if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                    return o1.getOrder() - o2.getOrder();
                }
                return c.compare(o1.getNameReading(), o2.getNameReading());
            }
        };
    }

    private <T extends Comparable<T>> int nullSafeCompare(T a, T b, boolean nullIsSmaller) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return nullIsSmaller ? -1 : 1;
        }
        if (b == null) {
            return nullIsSmaller ? 1 : -1;
        }
        return a.compareTo(b);
    }

    public boolean isSortAlbumsByYear(String artist) {
        return settingsService.isSortAlbumsByYear()
                && (isEmpty(artist) || !(settingsService.isProhibitSortVarious()
                        && isVarious.matcher(artist.toLowerCase()).matches()));
    }
    
    public boolean isSortAlbumsByYear(MediaFile parent) {
        return settingsService.isSortAlbumsByYear()
                && (isEmpty(parent) || isSortAlbumsByYear(parent.getArtist()));
    }

    public MediaFileComparator mediaFileOrder() {
        return mediaFileOrder(null);
    }

    public MediaFileComparator mediaFileOrder(MediaFile parent) {
        return new JpMediaFileComparator(isSortAlbumsByYear(parent), createCollator());
    }

    /**
     * Returns a comparator that sorts in dictionary order regardless of the setting.
     * (Used places are limited)
     * @return Comparator
     */
    public MediaFileComparator mediaFileAlphabeticalOrder() {
        return new JpMediaFileComparator(false, createCollator());
    }

    public Comparator<Playlist> playlistOrder() {
        return new Comparator<Playlist>() {

            private final Comparator<Object> c = createCollator();

            @Override
            public int compare(Playlist o1, Playlist o2) {
                utils.analyze(o1);
                utils.analyze(o2);
                return c.compare(o1.getReading(), o2.getReading());
            }
        };
    }

    public Comparator<Genre> genreOrder(boolean sortByAlbum) {
        return (Genre o1, Genre o2) -> sortByAlbum ? o2.getAlbumCount() - o1.getAlbumCount() : o2.getSongCount() - o1.getSongCount();
    }

    public Comparator<Genre> genreAlphabeticalOrder() {
        return new Comparator<Genre>() {

            private final Comparator<Object> c = createCollator();

            @Override
            public int compare(Genre o1, Genre o2) {
                utils.analyze(o1);
                utils.analyze(o2);
                return c.compare(o1.getReading(), o2.getReading());
            }
        };
    }

}
