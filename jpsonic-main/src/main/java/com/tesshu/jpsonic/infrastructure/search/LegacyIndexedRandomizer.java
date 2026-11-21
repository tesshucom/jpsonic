package com.tesshu.jpsonic.infrastructure.search;

import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.param.ShuffleSelectionParam;

public interface LegacyIndexedRandomizer {

    List<MediaFile> getRandomSongs(ShuffleSelectionParam criteria);

    List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders);

    List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders);

    List<Album> getAlbumId3sByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders);

    List<MediaFile> getAlbumsByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders);

    List<MediaFile> getSongsByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders, MediaFile.MediaType... types);
}
