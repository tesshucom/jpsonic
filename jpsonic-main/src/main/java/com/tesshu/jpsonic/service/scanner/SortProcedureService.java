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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.JAlbumDao;
import com.tesshu.jpsonic.dao.JArtistDao;
import com.tesshu.jpsonic.dao.JMediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.search.IndexManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * The class to complement sorting and reading. It does not exist in conventional Sonic servers. This class has a large
 * impact on sorting and searching accuracy.
 */
@Service
@DependsOn({ "musicFolderService", "jmediaFileDao", "jartistDao", "jalbumDao", "japaneseReadingUtils", "indexManager",
        "jpsonicComparators" })
public class SortProcedureService {

    private final MusicFolderService musicFolderService;
    private final MediaFileService mediaFileService;
    private final WritableMediaFileService writableMediaFileService;
    private final JMediaFileDao jMediaFileDao;
    private final ArtistDao artistDao;
    private final JArtistDao jArtistDao;
    private final AlbumDao albumDao;
    private final JAlbumDao jAlbumDao;
    private final JapaneseReadingUtils utils;
    private final IndexManager indexManager;
    private final JpsonicComparators comparators;

    public SortProcedureService(MusicFolderService musicFolderService, MediaFileService mediaFileService,
            WritableMediaFileService writableMediaFileService, JMediaFileDao jMediaFileDao, ArtistDao artistDao,
            JArtistDao jArtistDao, AlbumDao albumDao, JAlbumDao jAlbumDao, JapaneseReadingUtils utils,
            IndexManager indexManager, JpsonicComparators jpsonicComparator) {
        super();
        this.musicFolderService = musicFolderService;
        this.mediaFileService = mediaFileService;
        this.writableMediaFileService = writableMediaFileService;
        this.jMediaFileDao = jMediaFileDao;
        this.artistDao = artistDao;
        this.jArtistDao = jArtistDao;
        this.albumDao = albumDao;
        this.jAlbumDao = jAlbumDao;
        this.utils = utils;
        this.indexManager = indexManager;
        this.comparators = jpsonicComparator;
    }

    public void clearMemoryCache() {
        utils.clear();
    }

    public void clearOrder() {
        jMediaFileDao.clearOrder();
        jArtistDao.clearOrder();
        jAlbumDao.clearOrder();
    }

    FixedIds compensateSortOfAlbum() {
        List<SortCandidate> candidates = jMediaFileDao.getSortForAlbumWithoutSorts();
        candidates.forEach(utils::analyze);
        return updateSortOfAlbum(candidates);
    }

    FixedIds compensateSortOfArtist() {
        List<SortCandidate> candidates = jMediaFileDao.getSortForPersonWithoutSorts();
        candidates.forEach(utils::analyze);
        return updateSortOfArtist(candidates);
    }

    FixedIds copySortOfAlbum() {
        List<SortCandidate> candidates = jMediaFileDao.getCopyableSortForAlbums();
        candidates.forEach(utils::analyze);
        return updateSortOfAlbum(candidates);
    }

    FixedIds copySortOfArtist() {
        List<SortCandidate> candidates = jMediaFileDao.getCopyableSortForPersons();
        candidates.forEach(utils::analyze);
        return updateSortOfArtist(candidates);
    }

    FixedIds mergeSortOfAlbum() {
        List<SortCandidate> candidates = jMediaFileDao.guessAlbumSorts();
        candidates.forEach(utils::analyze);
        return updateSortOfAlbum(candidates);
    }

    FixedIds mergeSortOfArtist() {
        List<SortCandidate> candidates = jMediaFileDao.guessPersonsSorts();
        candidates.forEach(utils::analyze);
        return updateSortOfArtist(candidates);
    }

    private void updateIndexOfAlbum(FixedIds... fixedIds) {
        FixedIds fixedIdAll = new FixedIds();
        for (FixedIds toBeFixed : fixedIds) {
            fixedIdAll.getMediaFileIds().addAll(toBeFixed.getMediaFileIds());
            fixedIdAll.getArtistIds().addAll(toBeFixed.getArtistIds());
            fixedIdAll.getAlbumIds().addAll(toBeFixed.getAlbumIds());
        }
        fixedIdAll.getMediaFileIds().stream().map(id -> mediaFileService.getMediaFile(id))
                .forEach(mediaFile -> indexManager.index(mediaFile));
        fixedIdAll.getAlbumIds().stream().map(id -> jAlbumDao.getAlbum(id)).forEach(album -> indexManager.index(album));
    }

    private void updateIndexOfArtist(FixedIds... fixedIds) {
        FixedIds fixedIdAll = new FixedIds();
        for (FixedIds toBeFixed : fixedIds) {
            fixedIdAll.getMediaFileIds().addAll(toBeFixed.getMediaFileIds());
            fixedIdAll.getArtistIds().addAll(toBeFixed.getArtistIds());
            fixedIdAll.getAlbumIds().addAll(toBeFixed.getAlbumIds());
        }
        fixedIdAll.getMediaFileIds().stream().map(id -> mediaFileService.getMediaFile(id))
                .forEach(mediaFile -> indexManager.index(mediaFile));
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, false);
        fixedIdAll.getArtistIds().forEach(id -> folders.forEach(m -> {
            Artist artist = jArtistDao.getArtist(id);
            if (artist != null) {
                indexManager.index(artist, m);
            }
        }));
        fixedIdAll.getAlbumIds().stream().map(id -> jAlbumDao.getAlbum(id)).forEach(album -> indexManager.index(album));
    }

    private void updateOrderOfAlbum() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, false);
        List<Album> albums = jAlbumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folders);
        albums.sort(comparators.albumOrderByAlpha());
        int i = 0;
        for (Album album : albums) {
            albumDao.updateOrder(album.getArtist(), album.getName(), i++);
        }
    }

    public void updateOrderOfAll() {
        updateOrderOfArtist();
        updateOrderOfAlbum();
        updateOrderOfFileStructure();
    }

    private void updateOrderOfArtist() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, false);
        List<Artist> artists = jArtistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        artists.sort(comparators.artistOrderByAlpha());
        int i = 0;
        for (Artist artist : artists) {
            artistDao.updateOrder(artist.getName(), i++);
        }
    }

    private void updateOrderOfFileStructure() {

        List<MusicFolder> folders = musicFolderService.getAllMusicFolders(false, false);
        List<MediaFile> artists = jMediaFileDao.getArtistAll(folders);
        artists.sort(comparators.mediaFileOrderByAlpha());

        int i = 0;
        for (MediaFile artist : artists) {
            artist.setOrder(i++);
            writableMediaFileService.updateOrder(artist);
        }

        List<MediaFile> albums = mediaFileService.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, folders);
        albums.sort(comparators.mediaFileOrderByAlpha());

        i = 0;
        for (MediaFile album : albums) {
            album.setOrder(i++);
            writableMediaFileService.updateOrder(album);
        }

    }

    public void updateSortOfAlbum() {
        FixedIds merged = mergeSortOfAlbum();
        FixedIds copied = copySortOfAlbum();
        FixedIds compensated = compensateSortOfAlbum();
        updateIndexOfAlbum(merged, copied, compensated);
    }

    private FixedIds updateSortOfAlbum(@NonNull List<SortCandidate> candidates) {
        FixedIds ids = new FixedIds();
        if (candidates.isEmpty()) {
            return ids;
        }
        ids.getMediaFileIds().addAll(jMediaFileDao.getSortOfAlbumToBeFixed(candidates));
        ids.getAlbumIds().addAll(jAlbumDao.getSortOfAlbumToBeFixed(candidates));
        candidates.forEach(c -> {
            jMediaFileDao.updateAlbumSort(c);
            jAlbumDao.updateAlbumSort(c);
        });
        return ids;
    }

    public void updateSortOfArtist() {
        jMediaFileDao.clearArtistReadingOfDirectory();
        FixedIds merged = mergeSortOfArtist();
        FixedIds copied = copySortOfArtist();
        FixedIds compensated = compensateSortOfArtist();
        updateIndexOfArtist(merged, copied, compensated);
    }

    private FixedIds updateSortOfArtist(@NonNull List<SortCandidate> candidates) {
        FixedIds ids = new FixedIds();
        if (candidates.isEmpty()) {
            return ids;
        }
        ids.getMediaFileIds().addAll(jMediaFileDao.getSortOfArtistToBeFixed(candidates));
        ids.getArtistIds().addAll(jArtistDao.getSortOfArtistToBeFixed(candidates));
        ids.getAlbumIds().addAll(jAlbumDao.getSortOfArtistToBeFixed(candidates));
        candidates.forEach(c -> {
            jMediaFileDao.updateArtistSort(c);
            jArtistDao.updateArtistSort(c);
            jAlbumDao.updateArtistSort(c);
        });
        return ids;
    }

    private static class FixedIds {
        private final Set<Integer> mediaFileIds = new LinkedHashSet<>();
        private final Set<Integer> artistIds = new LinkedHashSet<>();
        private final Set<Integer> albumIds = new LinkedHashSet<>();

        public Set<Integer> getMediaFileIds() {
            return mediaFileIds;
        }

        public Set<Integer> getArtistIds() {
            return artistIds;
        }

        public Set<Integer> getAlbumIds() {
            return albumIds;
        }
    }
}
