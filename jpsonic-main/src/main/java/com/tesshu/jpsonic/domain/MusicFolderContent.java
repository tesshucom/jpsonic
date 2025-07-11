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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.domain;

import java.util.List;
import java.util.SortedMap;

public class MusicFolderContent {

    private final SortedMap<MusicIndex, List<MediaFile>> indexedArtists;
    private final List<MediaFile> singleSongs;

    public MusicFolderContent(SortedMap<MusicIndex, List<MediaFile>> indexedArtists,
            List<MediaFile> singleSongs) {
        this.indexedArtists = indexedArtists;
        this.singleSongs = singleSongs;
    }

    public SortedMap<MusicIndex, List<MediaFile>> getIndexedArtists() {
        return indexedArtists;
    }

    public List<MediaFile> getSingleSongs() {
        return singleSongs;
    }

    public record Counts(SortedMap<MusicIndex, Integer> indexCounts, int singleSongCounts) {
    }
}
