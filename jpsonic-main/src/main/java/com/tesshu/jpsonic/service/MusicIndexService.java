
package com.tesshu.jpsonic.service;

import java.util.List;
import java.util.SortedMap;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;

/**
 * Provides services for grouping artists by index.
 *
 * @author Sindre Mehus
 */
public interface MusicIndexService {

    /**
     * Returns a map from music indexes to sorted lists of artists that are direct children of the given music folders.
     *
     * @param folders
     *            The music folders.
     *
     * @return A map from music indexes to sets of artists that are direct children of this music file.
     */
    SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> getIndexedArtists(List<MusicFolder> folders);

    SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> getIndexedId3Artists(List<Artist> artists);

    MusicFolderContent getMusicFolderContent(List<MusicFolder> folders);

    List<MediaFile> getShortcuts(List<MusicFolder> folders);
}
