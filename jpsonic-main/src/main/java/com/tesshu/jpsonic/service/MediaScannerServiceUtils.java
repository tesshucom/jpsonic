/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.SortCandidate;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.IndexManager;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

//@formatter:off
/**
 * Utility class for injecting into legacy MediaScannerService.
 * 
 * Supplement processing that is lacking in legacy services.
 * 
 *  - Unify Sort tags for names.
 *  - Determines the order of all media from the sort key.
 *
 * There are three steps to integrating sort-tags:
 *
 * [merge]
 * If multiple Sort tags exist for one name, unify them in order of priority.
 *
 * [copy]
 * Copy if null-tag exists and can be resolved with the merged tag.
 *
 * [compensation]
 * If null-tag is cannot be resolved by merge/copy, generate it from the name.
 * If it is Japanese, it is converted from notation to phoneme.
 *
 * - Eliminate inconsistencies and dropouts in search results
 * - Compress index size by unifying data
 * - Perform a perfect sort
 * - In particular, in the case of Japanese, a sort search considering phonemes is realized.
 *   Prevent dropouts when searching by voice.
 * - Perform a perfect sort
 * - Realize high-speed paging with WEB/REST/UPnP
 *
 * This class has a great influence on the accuracy of sorting and searching.
 */
// @formatter:on
@Component
@DependsOn({ "settingsService", "jmediaFileDao", "jartistDao", "jalbumDao", "japaneseReadingUtils", "indexManager", "jpsonicComparators" })
@SuppressWarnings("PMD.AccessorMethodGeneration")
public class MediaScannerServiceUtils {

    private final SettingsService settingsService;
    private final JMediaFileDao mediaFileDao;
    private final JArtistDao artistDao;
    private final JAlbumDao albumDao;
    private final JapaneseReadingUtils utils;
    private final IndexManager indexManager;
    private final JpsonicComparators comparators;

    public MediaScannerServiceUtils(// @formatter:off
            SettingsService settingsService,
            JMediaFileDao mediaFileDao,
            JArtistDao artistDao,
            JAlbumDao albumDao,
            JapaneseReadingUtils utils,
            IndexManager indexManager,
            JpsonicComparators jpsonicComparator) {
        super();
        this.settingsService = settingsService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.utils = utils;
        this.indexManager = indexManager;
        this.comparators = jpsonicComparator;
    } // @formatter:on

    /**
     * Update the order of all mediaFile records.
     */
    public void clearMemoryCache() {
        utils.clear();
    }

    /**
     * Clears all orders registered in the repository.
     */
    public void clearOrder() {
        mediaFileDao.clearOrder();
        artistDao.clearOrder();
        albumDao.clearOrder();
    }

    private FixedIds compensateSortOfAlbum() {
        List<SortCandidate> candidates = mediaFileDao.getSortForAlbumWithoutSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfAlbum(candidates);
    }

    private FixedIds compensateSortOfArtist() {
        List<SortCandidate> candidates = mediaFileDao.getSortForPersonWithoutSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfArtist(candidates);
    }

    private FixedIds copySortOfAlbum() {
        List<SortCandidate> candidates = mediaFileDao.getCopyableSortForAlbums();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfAlbum(candidates);
    }

    private FixedIds copySortOfArtist() {
        List<SortCandidate> candidates = mediaFileDao.getCopyableSortForPersons();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfArtist(candidates);
    }

    private FixedIds mergeSortOfAlbum() {
        List<SortCandidate> candidates = mediaFileDao.guessAlbumSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfAlbum(candidates);
    }

    private FixedIds mergeSortOfArtist() {
        List<SortCandidate> candidates = mediaFileDao.guessPersonsSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updateSortOfArtist(candidates);
    }

    private void updateIndexOfAlbum(FixedIds... fixedIds) {
        FixedIds fixedIdAll = new FixedIds();
        for (FixedIds toBeFixed : fixedIds) {
            fixedIdAll.mediaFileIds.addAll(toBeFixed.mediaFileIds);
            fixedIdAll.artistIds.addAll(toBeFixed.artistIds);
            fixedIdAll.albumIds.addAll(toBeFixed.albumIds);
        }
        fixedIdAll.mediaFileIds.forEach(id -> indexManager.index(mediaFileDao.getMediaFile(id)));
        fixedIdAll.albumIds.forEach(id -> indexManager.index(albumDao.getAlbum(id)));
    }

    private void updateIndexOfArtist(FixedIds... fixedIds) {
        FixedIds fixedIdAll = new FixedIds();
        for (FixedIds toBeFixed : fixedIds) {
            fixedIdAll.mediaFileIds.addAll(toBeFixed.mediaFileIds);
            fixedIdAll.artistIds.addAll(toBeFixed.artistIds);
            fixedIdAll.albumIds.addAll(toBeFixed.albumIds);
        }
        fixedIdAll.mediaFileIds.forEach(id -> indexManager.index(mediaFileDao.getMediaFile(id)));
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        fixedIdAll.artistIds.forEach(id -> folders.forEach(m -> indexManager.index(artistDao.getArtist(id), m)));
        fixedIdAll.albumIds.forEach(id -> indexManager.index(albumDao.getAlbum(id)));
    }

    private void updateOrderOfAlbum() {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folders);
        albums.sort(comparators.albumOrderByAlpha());
        int i = 0;
        for (Album album : albums) {
            album.setOrder(i++);
            albumDao.createOrUpdateAlbum(album);
        }
    }

    public void updateOrderOfAll() {
        updateOrderOfArtist();
        updateOrderOfAlbum();
        updateOrderOfFileStructure();
    }

    private void updateOrderOfArtist() {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        artists.sort(comparators.artistOrderByAlpha());
        int i = 0;
        for (Artist artist : artists) {
            artist.setOrder(i++);
            artistDao.createOrUpdateArtist(artist);
        }
    }

    private void updateOrderOfFileStructure() {

        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        List<MediaFile> artists = mediaFileDao.getArtistAll(folders);
        artists.sort(comparators.mediaFileOrderByAlpha());

        int i = 0;
        for (MediaFile artist : artists) {
            artist.setOrder(i++);
            mediaFileDao.createOrUpdateMediaFile(artist);
        }

        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, folders);
        albums.sort(comparators.mediaFileOrderByAlpha());

        i = 0;
        for (MediaFile album : albums) {
            album.setOrder(i++);
            mediaFileDao.createOrUpdateMediaFile(album);
        }

    }

    public void updateSortOfAlbum() {
        FixedIds merged = mergeSortOfAlbum();
        FixedIds copied = copySortOfAlbum();
        FixedIds compensated = compensateSortOfAlbum();
        updateIndexOfAlbum(merged, copied, compensated);
    }

    private FixedIds updateSortOfAlbum(List<SortCandidate> candidates) {
        FixedIds ids = new FixedIds();
        if (0 == candidates.size()) {
            return ids;
        }
        ids.mediaFileIds.addAll(mediaFileDao.getSortOfAlbumToBeFixed(candidates));
        ids.albumIds.addAll(albumDao.getSortOfAlbumToBeFixed(candidates));
        candidates.forEach(c -> {
            mediaFileDao.updateAlbumSort(c);
            albumDao.updateAlbumSort(c);
        });
        return ids;
    }

    public void updateSortOfArtist() {
        FixedIds merged = mergeSortOfArtist();
        FixedIds copied = copySortOfArtist();
        FixedIds compensated = compensateSortOfArtist();
        updateIndexOfArtist(merged, copied, compensated);
    }

    private FixedIds updateSortOfArtist(List<SortCandidate> candidates) {
        FixedIds ids = new FixedIds();
        if (0 == candidates.size()) {
            return ids;
        }
        ids.mediaFileIds.addAll(mediaFileDao.getSortOfArtistToBeFixed(candidates));
        ids.artistIds.addAll(artistDao.getSortOfArtistToBeFixed(candidates));
        ids.albumIds.addAll(albumDao.getSortOfArtistToBeFixed(candidates));
        candidates.forEach(c -> {
            mediaFileDao.updateArtistSort(c);
            artistDao.updateArtistSort(c);
            albumDao.updateArtistSort(c);
        });
        return ids;
    }

    private static class FixedIds {
        private LinkedHashSet<Integer> mediaFileIds = new LinkedHashSet<>();
        private LinkedHashSet<Integer> artistIds = new LinkedHashSet<>();
        private LinkedHashSet<Integer> albumIds = new LinkedHashSet<>();
    }

}
