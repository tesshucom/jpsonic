package com.tesshu.jpsonic.service;

import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MusicFolderService {

    List<MusicFolder> getAllMusicFolders();

    List<MusicFolder> getAllMusicFolders(boolean includeDisabled, boolean includeNonExisting);

    List<MusicFolder> getMusicFoldersForUser(@NonNull String username);

    List<MusicFolder> getMusicFoldersForUser(@NonNull String username, @Nullable Integer selectedMusicFolderId);

    MusicFolder getMusicFolderById(int id);

    void setMusicFoldersForUser(@NonNull String username, List<Integer> musicFolderIds);
}
