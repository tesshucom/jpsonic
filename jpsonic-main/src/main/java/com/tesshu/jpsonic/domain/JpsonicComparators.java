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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.text.Collator;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.domain.MusicIndex.SortableArtist;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.SettingsService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * This class provides Comparator for domain objects.
 *
 * The sorting rules can be changed with some global options and are dynamic. Executed through this class whenever
 * domain objects in the system are sorted.
 */
@Component
@DependsOn({ "settingsService", "japaneseReadingUtils" })
public class JpsonicComparators {

    public enum OrderBy {
        TRACK, ARTIST, ALBUM
    }

    private final SettingsService settingsService;
    private final JapaneseReadingUtils utils;
    private final Pattern various;

    public JpsonicComparators(SettingsService settingsService, JapaneseReadingUtils utils) {
        super();
        this.settingsService = settingsService;
        this.utils = utils;
        various = Pattern.compile("^various.*$");
    }

    /**
     * Returns a comparator that sorts in dictionary order.
     * 
     * @return Comparator
     */
    public Comparator<Album> albumOrderByAlpha() {
        return (o1, o2) -> {
            if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                return o1.getOrder() - o2.getOrder();
            }
            return createCollator().compare(o1.getNameReading(), o2.getNameReading());
        };
    }

    /**
     * Returns a comparator that sorts in dictionary order.
     * 
     * @return Comparator
     */
    public Comparator<Artist> artistOrderByAlpha() {
        return (o1, o2) -> {
            if (-1 != o1.getOrder() && -1 != o2.getOrder()) {
                return o1.getOrder() - o2.getOrder();
            }
            return createCollator().compare(o1.getReading(), o2.getReading());
        };
    }

    /**
     * Returns Collator which is used as standard in Jpsonic.
     */
    protected final Collator createCollator() {
        Collator collator = Collator.getInstance(settingsService.getLocale());
        return settingsService.isSortAlphanum() ? new AlphanumWrapper(collator) : collator;
    }

    public Comparator<Genre> genreOrder(boolean sortByAlbum) {
        return (Genre o1, Genre o2) -> sortByAlbum ? o2.getAlbumCount() - o1.getAlbumCount()
                : o2.getSongCount() - o1.getSongCount();
    }

    public Comparator<Genre> genreOrderByAlpha() {
        return new GenreComparator(utils, createCollator());
    }

    private boolean isSortAlbumsByYear(MediaFile parent) {
        return settingsService.isSortAlbumsByYear() && (isEmpty(parent) || isSortAlbumsByYear(parent.getArtist()));
    }

    public final boolean isSortAlbumsByYear(@Nullable String artist) {
        return settingsService.isSortAlbumsByYear() && (isEmpty(artist) || !(settingsService.isProhibitSortVarious()
                && various.matcher(artist.toLowerCase(settingsService.getLocale())).matches()));
    }

    /**
     * Returns the comparator that changes the sorting rules by hierarchy. Mainly used when expanding files. The result
     * is affected by the global settings related to sorting.
     *
     * @param parent
     *            The common parent of the list to sort. Null for hierarchy-independent or top-level sorting.
     */
    public MediaFileComparator mediaFileOrder(@Nullable MediaFile parent) {
        return new JpMediaFileComparator(isSortAlbumsByYear(parent), createCollator());
    }

    /**
     * Returns a comparator for sorting MediaFiles by specifying a field regardless of MediaType.
     */
    public Comparator<MediaFile> mediaFileOrderBy(@NonNull OrderBy orderBy) {
        return (a, b) -> {
            switch (orderBy) {
            case TRACK:
                Integer trackA = a.getTrackNumber();
                Integer trackB = b.getTrackNumber();
                if (trackA == null) {
                    trackA = 0;
                }
                if (trackB == null) {
                    trackB = 0;
                }
                return trackA.compareTo(trackB);
            case ARTIST:
                return createCollator().compare(a.getArtistReading(), b.getArtistReading());
            case ALBUM:
                return createCollator().compare(a.getAlbumReading(), b.getAlbumReading());
            default:
                return 0;
            }
        };
    }

    /**
     * Returns a comparator for dictionary order sorting MediaFiles with different MediaTypes. Ignores some of the
     * global settings related to sorting, and sorts naturally based on Type and name. It is mainly used for order index
     * during scanning.
     *
     * @return Comparator
     */
    public MediaFileComparator mediaFileOrderByAlpha() {
        return new JpMediaFileComparator(false, createCollator());
    }

    public Comparator<Playlist> playlistOrder() {
        return new PlaylistComparator(utils, createCollator());
    }

    public Comparator<SortableArtist> sortableArtistOrder() {
        return (o1, o2) -> createCollator().compare(o1.getSortableName(), o2.getSortableName());
    }

    private static class GenreComparator implements Comparator<Genre> {
        private final JapaneseReadingUtils utils;
        private final Collator collator;

        public GenreComparator(JapaneseReadingUtils utils, Collator collator) {
            super();
            this.utils = utils;
            this.collator = collator;
        }

        @Override
        public int compare(Genre o1, Genre o2) {
            this.utils.analyze(o1);
            this.utils.analyze(o2);
            return this.collator.compare(o1.getReading(), o2.getReading());
        }
    }

    private static class PlaylistComparator implements Comparator<Playlist> {
        private final JapaneseReadingUtils utils;
        private final Collator collator;

        public PlaylistComparator(JapaneseReadingUtils utils, Collator collator) {
            super();
            this.utils = utils;
            this.collator = collator;
        }

        @Override
        public int compare(Playlist o1, Playlist o2) {
            this.utils.analyze(o1);
            this.utils.analyze(o2);
            return this.collator.compare(o1.getReading(), o2.getReading());
        }
    }
}
