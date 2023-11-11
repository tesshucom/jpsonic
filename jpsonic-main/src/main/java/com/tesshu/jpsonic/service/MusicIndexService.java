
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
     * @since Airsonic
     */
    MusicFolderContent getMusicFolderContent(List<MusicFolder> folders);

    /**
     * @since Airsonic
     */
    SortedMap<MusicIndex, List<Artist>> getIndexedId3Artists(List<MusicFolder> folders);

    /**
     * @since Airsonic
     */
    List<MediaFile> getShortcuts(List<MusicFolder> folders);

    /**
     * @since v113.0.0
     */
    void clear();

    /**
     * @since v113.0.0
     */
    MusicFolderContent.Counts getMusicFolderContentCounts(List<MusicFolder> folders);

    /**
     * @since v113.0.0
     */
    SortedMap<MusicIndex, Integer> getIndexedId3ArtistCounts(List<MusicFolder> folders);
}
