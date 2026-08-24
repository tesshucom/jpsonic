package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.IndexWithCount;
import com.tesshu.jpsonic.domain.model.MusicFolder;

public interface ArtistProvider {

    int countArtists(List<MusicFolder> folders);

    int countChildren(List<MusicFolder> folders, String artistNname);

    int countMudicIndexes(List<MusicFolder> folders);

    List<Artist> findArtists(List<MusicFolder> folders, long offset, long count);

    List<Artist> findArtists(List<MusicFolder> folders, String musicIndex, long offset, long count);

    List<IndexWithCount> findIndexWithCounts(List<MusicFolder> folders);

    Artist requireArtist(int id);
}
