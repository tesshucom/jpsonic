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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;
import java.util.SortedMap;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.MusicFolderContent;
import com.tesshu.jpsonic.domain.model.MusicIndex;

/**
 * Provides indexed music content based on either the file structure or ID3
 * metadata. File-structure-based content is represented by
 * {@link MusicFolderContent}, which distinguishes indexed entries, standalone
 * files directly under a music folder, and shortcut directories. In contrast,
 * ID3-based content consists only of artists classified by {@link MusicIndex},
 * as it does not depend on the directory structure.
 */
public interface MusicIndexProvider {

    SortedMap<MusicIndex, Integer> countIndexedId3Artists(List<MusicFolder> folders);

    MusicFolderContent.Counts countMusicFolderContent(List<MusicFolder> folders,
            MediaFile.Type... excludes);

    SortedMap<MusicIndex, List<Artist>> findIndexedId3Artists(List<MusicFolder> folders);

    MusicFolderContent findMusicFolderContent(List<MusicFolder> folders,
            MediaFile.Type... excludes);

    void invalidate();
}
