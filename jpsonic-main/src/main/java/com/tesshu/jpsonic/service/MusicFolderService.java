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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.MusicFolder;
import net.sf.ehcache.Ehcache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;

/**
 * The class containing MusicFolder-related methods extracted from the legacy server's SettingsService.
 */
@Service
public class MusicFolderService {

    private final ConcurrentMap<String, List<MusicFolder>> cachedUserFolders;
    private List<MusicFolder> cachedMusicFolders;

    private final MusicFolderDao musicFolderDao;
    private final SettingsService settingsService;
    private final Ehcache indexCache;
    private final Object lock = new Object();

    public MusicFolderService(MusicFolderDao musicFolderDao, SettingsService settingsService, Ehcache indexCache) {
        this.musicFolderDao = musicFolderDao;
        this.settingsService = settingsService;
        this.indexCache = indexCache;
        cachedUserFolders = new ConcurrentHashMap<>();
    }

    public List<MusicFolder> getAllMusicFolders() {
        return getAllMusicFolders(false, !settingsService.isRedundantFolderCheck());
    }

    public List<MusicFolder> getAllMusicFolders(boolean includeDisabled, boolean includeNonExisting) {
        synchronized (lock) {
            if (cachedMusicFolders == null) {
                cachedMusicFolders = musicFolderDao.getAllMusicFolders();
            }
            return cachedMusicFolders.stream().filter(folder -> includeDisabled || folder.isEnabled())
                    .filter(folder -> includeNonExisting || Files.exists(folder.toPath())).collect(Collectors.toList());
        }
    }

    public List<MusicFolder> getMusicFoldersForUser(@NonNull String username) {
        synchronized (lock) {
            List<MusicFolder> result = cachedUserFolders.get(username);
            if (result == null) {
                result = musicFolderDao.getMusicFoldersForUser(username);
                result.retainAll(getAllMusicFolders());
                cachedUserFolders.put(username, result);
            }
            return result;
        }
    }

    public List<MusicFolder> getMusicFoldersForUser(@NonNull String username, @Nullable Integer selectedMusicFolderId) {
        List<MusicFolder> allowed = getMusicFoldersForUser(username);
        if (selectedMusicFolderId == null) {
            return allowed;
        }
        MusicFolder selected = getMusicFolderById(selectedMusicFolderId);
        return allowed.contains(selected) ? Collections.singletonList(selected) : Collections.emptyList();
    }

    public void setMusicFoldersForUser(@NonNull String username, List<Integer> musicFolderIds) {
        musicFolderDao.setMusicFoldersForUser(username, musicFolderIds);
        synchronized (lock) {
            cachedUserFolders.remove(username);
        }
        indexCache.removeAll();
    }

    public @Nullable MusicFolder getMusicFolderById(int id) {
        return getAllMusicFolders().stream().filter(folder -> folder.getId().equals(id)).findFirst().orElse(null);
    }

    public void createMusicFolder(@NonNull MusicFolder musicFolder) {
        musicFolderDao.createMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    public void deleteMusicFolder(int id) {
        musicFolderDao.deleteMusicFolder(id);
        clearMusicFolderCache();
    }

    public void updateMusicFolder(@NonNull MusicFolder musicFolder) {
        musicFolderDao.updateMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    @SuppressWarnings("PMD.NullAssignment") // (cachedMusicFolders) Intentional allocation to clear cache
    public void clearMusicFolderCache() {
        synchronized (lock) {
            cachedMusicFolders = null;
            cachedUserFolders.clear();
        }
        indexCache.removeAll();
    }
}
