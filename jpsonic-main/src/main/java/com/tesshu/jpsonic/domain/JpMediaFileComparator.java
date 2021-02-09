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

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.text.Collator;
import java.util.Comparator;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;

/**
 * Comparator for sorting media files.
 */
class JpMediaFileComparator implements MediaFileComparator {

    private final boolean sortAlbumsByYear;
    private final Comparator<Object> comparator;

    JpMediaFileComparator(boolean sortAlbumsByYear, Collator collator) {
        this.sortAlbumsByYear = sortAlbumsByYear;
        comparator = collator;
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    @Override
    public int compare(MediaFile a, MediaFile b) {

        // Directories before files.
        int i = compareDirectoryAndFile(a, b);
        if (i != 0) {
            return i;
        }

        // Non-album directories before album directories.
        i = compareAlbumAndNotAlbum(a, b);
        if (i != 0) {
            return i;
        }

        // Sort albums by year
        if (sortAlbumsByYear && a.isAlbum() && b.isAlbum()) {
            i = nullSafeCompare(a.getYear(), b.getYear(), false);
            if (i != 0) {
                return i;
            }
        }

        if (a.isDirectory() && b.isDirectory()) {
            return compareDirectory(a, b);
        }

        // Compare by disc and track numbers, if present.
        Integer trackA = getSortableDiscAndTrackNumber(a);
        Integer trackB = getSortableDiscAndTrackNumber(b);
        i = nullSafeCompare(trackA, trackB, false);
        if (i != 0) {
            return i;
        }

        return comparator.compare(a.getPath(), b.getPath());
    }

    private int compareDirectoryAndFile(MediaFile a, MediaFile b) {
        if (a.isFile() && b.isDirectory()) {
            return 1;
        }
        if (a.isDirectory() && b.isFile()) {
            return -1;
        }
        return 0;
    }

    private int compareAlbumAndNotAlbum(MediaFile a, MediaFile b) {
        if (a.isAlbum() && b.getMediaType() == MediaFile.MediaType.DIRECTORY) {
            return 1;
        }
        if (a.getMediaType() == MediaFile.MediaType.DIRECTORY && b.isAlbum()) {
            return -1;
        }
        return 0;
    }

    @SuppressWarnings("PMD.ConfusingTernary") // false positive
    private int compareDirectory(MediaFile a, MediaFile b) {
        int n;
        if (a.isAlbum() && b.isAlbum() && !isEmpty(a.getAlbumReading()) && !isEmpty(b.getAlbumReading())) {
            n = comparator.compare(a.getAlbumReading(), b.getAlbumReading());
        } else if (!a.isAlbum() && !b.isAlbum() && !isEmpty(a.getArtistReading()) && !isEmpty(b.getArtistReading())) {
            n = comparator.compare(a.getArtistReading(), b.getArtistReading());
        } else {
            n = comparator.compare(a.getName(), b.getName());
        }
        return n == 0 ? comparator.compare(a.getPath(), b.getPath()) : n; // To make it consistent to
                                                                          // MediaFile.equals()
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

    private Integer getSortableDiscAndTrackNumber(MediaFile file) {
        if (file.getTrackNumber() == null) {
            return null;
        }

        int discNumber = file.getDiscNumber() == null ? 1 : file.getDiscNumber();
        return discNumber * 1000 + file.getTrackNumber();
    }

}
