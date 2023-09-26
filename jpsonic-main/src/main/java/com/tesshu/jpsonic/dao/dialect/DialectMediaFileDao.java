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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.dao.dialect;

import java.util.List;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;

public interface DialectMediaFileDao {

    List<MediaFile> getChangedId3Albums(int count, List<MusicFolder> musicFolders, boolean withPodcast);

    List<MediaFile> getChangedId3Artists(int count, List<MusicFolder> folders, boolean withPodcast);

    List<MediaFile> getUnregisteredId3Albums(int count, List<MusicFolder> musicFolders, boolean withPodcast);

    List<MediaFile> getUnregisteredId3Artists(int count, List<MusicFolder> folders, boolean withPodcast);

    List<SortCandidate> getCopyableSortForAlbums(List<MusicFolder> folders);

    List<SortCandidate> getCopyableSortForPersons(List<MusicFolder> folders);

    List<MediaFile> getRandomSongsForAlbumArtist(int limit, String albumArtist, List<MusicFolder> musicFolders,
            BiFunction<Integer, Integer, List<Integer>> randomCallback);

    List<SortCandidate> getSortForPersonWithoutSorts(List<MusicFolder> folders);

    List<SortCandidate> getSortOfArtistToBeFixedWithId(List<SortCandidate> candidates);

    List<SortCandidate> getSortForAlbumWithoutSorts(List<MusicFolder> folders);

    List<SortCandidate> guessAlbumSorts(List<MusicFolder> folders);

    List<SortCandidate> guessPersonsSorts(List<MusicFolder> folders);
}
