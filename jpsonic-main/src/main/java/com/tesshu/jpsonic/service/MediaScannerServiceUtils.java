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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Utility class for injecting into legacy MediaScannerService. Supplement
 * processing that is lacking in legacy services.
 * 
 * These are the logics for determining the order of all records. It also
 * affects the speed of sorting, but the purpose is to respond to paging.
 *
 * Paging is used in REST and WEB. In addition, it is a mandatory requirement
 * for UPnP where there is a possibility of paging in all communications.
 */
@Component
@DependsOn({ "settingsService", "jmediaFileDao", "jartistDao", "jalbumDao", "japaneseReadingUtils", "indexManager", "jpsonicComparators" })
public class MediaScannerServiceUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JMediaFileService.class);

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

    /**
     * Generates a sort that has not been set for a tag.
     */
    private FixedIds compensatePersonsSort() {
        List<SortCandidate> candidates = mediaFileDao.getNoSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updatePersonsSort(candidates);
    }

    /**
     * Copy from other determined sorts for records where the name exists and the
     * sort is missing.
     */
    private FixedIds copyPersonsSort() {
        List<SortCandidate> candidates = mediaFileDao.getCopyableSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updatePersonsSort(candidates);
    }

    /**
     * Unify the multiple sort of Artist / AlbumArtist / Composer are set for the
     * sort with the highest priority.
     */
    FixedIds mergePersonsSort() {
        List<SortCandidate> candidates = mediaFileDao.guessSorts();
        candidates.forEach(c -> utils.analyze(c));
        return updatePersonsSort(candidates);
    }

    /**
     * Updates the sort / reading properties required for album sorting.
     */
    @Deprecated
    public void updateAlbumSort() {

        List<MediaFile> candidates = mediaFileDao.getAlbumSortCandidate();
        List<MediaFile> toBeUpdates = utils.createAlbumSortToBeUpdate(candidates);
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);

        for (MediaFile toBeUpdate : toBeUpdates) {
            // update db
            mediaFileDao.updateAlbumSort(toBeUpdate.getAlbumName(), toBeUpdate.getAlbumSort());
            // update index
            MediaFile album = mediaFileDao.getMediaFile(toBeUpdate.getId());
            indexManager.index(album);
        }
        LOG.info("{} records of album-sort were updated.", toBeUpdates.size());

        // The following processes require testing

        List<Artist> sortedArtists = artistDao.getSortedArtists();
        int maybe = 0;
        for (Artist artist : sortedArtists) {
            List<Album> albums = albumDao.getAlbumsForArtist(artist.getName(), folders);
            for (Album album : albums) {
                String sort = artist.getSort();
                if (sort != null && !sort.equals(album.getArtistSort())) {
                    album.setArtistReading(artist.getReading());
                    album.setArtistSort(artist.getSort());
                    // update db
                    albumDao.createOrUpdateAlbum(album);
                    if (album.getArtistSort() != null) {
                        indexManager.index(album);
                    }
                    maybe++;
                }
            }
        }
        LOG.debug(sortedArtists.size() + " sorted id3 artists. " + maybe + " id3 album rows reversal.");

        List<MediaFile> albums = mediaFileDao.getSortedAlbums();
        maybe = 0;
        for (MediaFile album : albums) {
            if (null != album.getAlbumArtist()) {
                Album albumid3 = albumDao.getAlbum(album.getAlbumArtist(), album.getAlbumName());
                if (null != albumid3) {
                    if (null != album.getAlbumSort() && !album.getAlbumSort().equals(albumid3.getNameSort())) {
                        albumid3.setNameReading(album.getAlbumReading());
                        albumid3.setNameSort(album.getAlbumSort());
                        // update db
                        albumDao.createOrUpdateAlbum(albumid3);
                        // update index
                        indexManager.index(albumid3);
                        maybe++;
                    }
                } else {
                    LOG.info(" > " + album.getAlbumName() + "@" + album.getAlbumArtist() + " does not exist in id3.");
                }
            }
        }
        LOG.debug(albums.size() + " sorted id3 albums. " + maybe + " id3 album rows reversal.");

    }

    public void updateArtistSort() {
        FixedIds merged = mergePersonsSort();
        FixedIds copied = copyPersonsSort();
        FixedIds compensated = compensatePersonsSort();
        updateIndex(merged, copied, compensated);
    }

    private void updateIndex(FixedIds... fixedIds) {
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

    /**
     * Update the order of all album records.
     */
    public void updateOrderOfAlbum() {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folders);
        albums.sort(comparators.albumOrderByAlpha());
        int i = 0;
        for (Album album : albums) {
            album.setOrder(i++);
            albumDao.createOrUpdateAlbum(album);
        }
    }

    /**
     * Update the order of all artist records.
     */
    public void updateOrderOfArtist() {
        List<MusicFolder> folders = settingsService.getAllMusicFolders(false, false);
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        artists.sort(comparators.artistOrderByAlpha());
        int i = 0;
        for (Artist artist : artists) {
            artist.setOrder(i++);
            artistDao.createOrUpdateArtist(artist);
        }
    }

    /**
     * Update the order of all mediaFile records.
     */
    public void updateOrderOfFileStructure() {

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

    private FixedIds updatePersonsSort(List<SortCandidate> candidates) {
        FixedIds ids = new FixedIds();
        ids.mediaFileIds.addAll(mediaFileDao.getToBeFixedSort(candidates));
        ids.artistIds.addAll(artistDao.getToBeFixedSort(candidates));
        ids.albumIds.addAll(albumDao.getToBeFixedSort(candidates));
        candidates.forEach(c -> {
            mediaFileDao.updateArtistSort(c);
            artistDao.updateArtistSort(c);
            albumDao.updateArtistSort(c);
        });
        return ids;
    }

    private final class FixedIds {
        private LinkedHashSet<Integer> mediaFileIds = new LinkedHashSet<Integer>();
        private LinkedHashSet<Integer> artistIds = new LinkedHashSet<Integer>();
        private LinkedHashSet<Integer> albumIds = new LinkedHashSet<Integer>();
    }

}
