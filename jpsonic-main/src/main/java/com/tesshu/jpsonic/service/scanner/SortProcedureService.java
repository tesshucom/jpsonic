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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.DuplicateSort;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.domain.SortCandidate.TargetField;
import org.apache.commons.lang3.exception.UncheckedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * The class to complement sorting and reading. It does not exist in conventional Sonic servers. This class has a large
 * impact on sorting and searching accuracy.
 */
@Service
@DependsOn({ "musicFolderService", "mediaFileDao", "artistDao", "albumDao", "japaneseReadingUtils", "indexManager",
        "jpsonicComparators" })
public class SortProcedureService {

    private static final int REPEAT_WAIT_MILLISECONDS = 50;

    private final MediaFileDao mediaFileDao;
    private final JapaneseReadingUtils utils;
    private final MusicIndexServiceImpl musicIndexService;

    public SortProcedureService(MediaFileDao mediaFileDao, JapaneseReadingUtils utils,
            MusicIndexServiceImpl musicIndexService) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.utils = utils;
        this.musicIndexService = musicIndexService;
    }

    void clearMemoryCache() {
        utils.clear();
    }

    private void repeatWait() {
        try {
            Thread.sleep(REPEAT_WAIT_MILLISECONDS);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    List<Integer> compensateSortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> cands = mediaFileDao.getNoSortAlbums(folders);
        cands.forEach(utils::analyze);
        return updateAlbumSort(cands);
    }

    List<Integer> compensateSortOfArtist(List<MusicFolder> folders) {
        List<SortCandidate> cands = mediaFileDao.getNoSortPersons(folders);
        cands.forEach(utils::analyze);
        return updateArtistSort(cands);
    }

    List<Integer> copySortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> cands = mediaFileDao.getCopyableSortAlbums(folders);
        cands.forEach(utils::analyze);
        return updateAlbumSort(cands);
    }

    List<Integer> copySortOfArtist(List<MusicFolder> folders) {
        List<SortCandidate> cands = mediaFileDao.getCopyableSortPersons(folders);
        cands.forEach(utils::analyze);
        return updateArtistSort(cands);
    }

    List<Integer> mergeSortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> cands = mediaFileDao.getDuplicateSortAlbums(folders);
        cands.forEach(utils::analyze);
        return updateAlbumSort(cands);
    }

    List<Integer> mergeSortOfArtist(List<MusicFolder> folders) {
        List<DuplicateSort> dups = mediaFileDao.getDuplicateSortPersons(folders);
        if (dups.isEmpty()) {
            return Collections.emptyList();
        }
        List<SortCandidate> cands = mediaFileDao.getSortCandidatePersons(dups);
        cands.forEach(utils::analyze);
        return updateArtistSort(cands);
    }

    private List<Integer> updateAlbumSort(@NonNull List<SortCandidate> cands) {
        if (cands.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < cands.size(); i++) {
            if (i % 20_000 == 0) {
                repeatWait();
            }
            mediaFileDao.updateAlbumSort(cands.get(i));
        }
        return cands.stream().map(SortCandidate::getTargetId).collect(Collectors.toList());
    }

    private List<Integer> updateArtistSort(@NonNull List<SortCandidate> cands) {
        if (cands.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < cands.size(); i++) {
            if (i % 20_000 == 0) {
                repeatWait();
            }
            SortCandidate cand = cands.get(i);
            if (cand.getTargetField() == TargetField.ARTIST) {
                String index = musicIndexService.getParser().getIndex(cand).getIndex();
                mediaFileDao.updateArtistSort(cand, index);
            } else {
                mediaFileDao.updateArtistSort(cand);
            }
        }
        return cands.stream().map(SortCandidate::getTargetId).collect(Collectors.toList());
    }
}
