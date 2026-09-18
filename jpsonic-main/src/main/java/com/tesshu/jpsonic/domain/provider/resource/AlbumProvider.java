package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;

import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;

public interface AlbumProvider {

    int countAlbums(List<MusicFolder> folders);

    List<Album> findAlbums(List<MusicFolder> folders, RuntimeOrderPolicy.AlbumSortOrder order,
            long offset, long count);

    List<Album> findChildren(List<MusicFolder> folders, Artist artist,
            RuntimeOrderPolicy.AlbumSortOrder order, long offset, long count);

    List<Album> findNewestAlbums(List<MusicFolder> folders, long offset, long count);

    Album requireAlbum(int id);
}
