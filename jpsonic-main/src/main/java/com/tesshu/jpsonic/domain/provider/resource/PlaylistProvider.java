package com.tesshu.jpsonic.domain.provider.resource;

import java.util.List;

import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.Playlist;

public interface PlaylistProvider {

    int countPlaylists();

    int countPublishedPlaylists();

    List<MediaFile> findChildren(List<MusicFolder> folders, Playlist playlist, long offset,
            long count);

    List<Playlist> findPlaylists(long offset, long count);

    List<Playlist> findPublishedPlaylists(long offset, long count);

    Playlist requirePlaylist(int id);
}
