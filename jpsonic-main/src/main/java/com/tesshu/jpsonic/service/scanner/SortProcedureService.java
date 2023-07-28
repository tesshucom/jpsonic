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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.Orderable;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
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

    private final MusicFolderService musicFolderService;
    private final MediaFileService mediaFileService;
    private final WritableMediaFileService writableMediaFileService;
    private final MediaFileDao mediaFileDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;
    private final JapaneseReadingUtils utils;
    private final JpsonicComparators comparators;

    public SortProcedureService(MusicFolderService musicFolderService, MediaFileService mediaFileService,
            WritableMediaFileService writableMediaFileService, MediaFileDao mediaFileDao, ArtistDao artistDao,
            AlbumDao albumDao, JapaneseReadingUtils utils, JpsonicComparators jpsonicComparator) {
        super();
        this.musicFolderService = musicFolderService;
        this.mediaFileService = mediaFileService;
        this.writableMediaFileService = writableMediaFileService;
        this.mediaFileDao = mediaFileDao;
        this.artistDao = artistDao;
        this.albumDao = albumDao;
        this.utils = utils;
        this.comparators = jpsonicComparator;
    }

    void clearMemoryCache() {
        utils.clear();
    }

    List<Integer> compensateSortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> candidates = mediaFileDao.getSortForAlbumWithoutSorts(folders);
        candidates.forEach(utils::analyze);
        return updateSortOfAlbums(candidates);
    }

    List<Integer> compensateSortOfArtist(List<MusicFolder> folders) {
        List<SortCandidate> candidatesWithId = mediaFileDao.getSortForPersonWithoutSorts(folders);
        candidatesWithId.forEach(utils::analyze);
        return updateSortOfArtistWithId(candidatesWithId);
    }

    List<Integer> copySortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> candidatesWithId = mediaFileDao.getCopyableSortForAlbums(folders);
        candidatesWithId.forEach(utils::analyze);
        return updateSortOfAlbumsWithId(candidatesWithId);
    }

    List<Integer> copySortOfArtist(List<MusicFolder> folders) {
        List<SortCandidate> candidatesWithId = mediaFileDao.getCopyableSortForPersons(folders);
        candidatesWithId.forEach(utils::analyze);
        return updateSortOfArtistWithId(candidatesWithId);
    }

    List<Integer> mergeSortOfAlbum(List<MusicFolder> folders) {
        List<SortCandidate> candidatesWithId = mediaFileDao.guessAlbumSorts(folders);
        candidatesWithId.forEach(utils::analyze);
        return updateSortOfAlbumsWithId(candidatesWithId);
    }

    List<Integer> mergeSortOfArtist(List<MusicFolder> folders) {
        List<SortCandidate> candidatesWithoutId = mediaFileDao.guessPersonsSorts(folders);
        if (candidatesWithoutId.isEmpty()) {
            return Collections.emptyList();
        }
        List<SortCandidate> candidatesWithId = mediaFileDao.getSortOfArtistToBeFixedWithId(candidatesWithoutId);
        candidatesWithId.forEach(utils::analyze);
        return updateSortOfArtistWithId(candidatesWithId);
    }

    <T extends Orderable> List<T> getToBeOrderUpdate(List<T> list, Comparator<T> comparator) {
        List<Integer> rawOrders = list.stream().map(Orderable::getOrder).collect(Collectors.toList());
        Collections.sort(list, comparator);
        List<T> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i + 1 != rawOrders.get(i)) {
                list.get(i).setOrder(i + 1);
                result.add(list.get(i));
            }
        }
        return result;
    }

    int updateOrderOfAlbumID3() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folders);
        LongAdder count = new LongAdder();
        getToBeOrderUpdate(albums, comparators.albumOrderByAlpha()).forEach(
                album -> count.add(albumDao.updateOrder(album.getArtist(), album.getName(), album.getOrder())));
        return count.intValue();
    }

    int updateOrderOfArtistID3() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        LongAdder count = new LongAdder();
        getToBeOrderUpdate(artists, comparators.artistOrderByAlpha())
                .forEach(artist -> count.add(artistDao.updateOrder(artist.getName(), artist.getOrder())));
        return count.intValue();
    }

    int updateOrderOfArtist() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> artists = mediaFileDao.getArtistAll(folders);
        LongAdder count = new LongAdder();
        getToBeOrderUpdate(artists, comparators.mediaFileOrderByAlpha())
                .forEach(artist -> count.add(writableMediaFileService.updateOrder(artist)));
        return count.intValue();
    }

    int updateOrderOfAlbum() {
        List<MusicFolder> folders = musicFolderService.getAllMusicFolders();
        List<MediaFile> albums = mediaFileService.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folders);
        LongAdder count = new LongAdder();
        getToBeOrderUpdate(albums, comparators.mediaFileOrderByAlpha())
                .forEach(album -> count.add(writableMediaFileService.updateOrder(album)));
        return count.intValue();
    }

    int updateOrderOfSongs(MediaFile parent) {
        List<MediaFile> songs = mediaFileDao.getChildrenWithOrderOf(parent.getPathString()).stream()
                .filter(child -> mediaFileService.isAudioFile(child.getFormat())
                        || mediaFileService.isVideoFile(child.getFormat()))
                .collect(Collectors.toList());
        LongAdder count = new LongAdder();
        getToBeOrderUpdate(songs, comparators.songsDefault())
                .forEach(song -> count.add(writableMediaFileService.updateOrder(song)));
        return count.intValue();
    }

    private List<Integer> updateSortOfAlbums(@NonNull List<SortCandidate> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> toBeFixed = mediaFileDao.getSortOfAlbumToBeFixed(candidates);
        candidates.forEach(c -> mediaFileDao.updateAlbumSort(c));
        return toBeFixed;
    }

    private List<Integer> updateSortOfAlbumsWithId(@NonNull List<SortCandidate> candidatesWithId) {
        if (candidatesWithId.isEmpty()) {
            return Collections.emptyList();
        }
        candidatesWithId.forEach(c -> mediaFileDao.updateAlbumSortWithId(c));
        return candidatesWithId.stream().map(SortCandidate::getId).collect(Collectors.toList());
    }

    private List<Integer> updateSortOfArtistWithId(@NonNull List<SortCandidate> candidatesWithId) {
        if (candidatesWithId.isEmpty()) {
            return Collections.emptyList();
        }
        candidatesWithId.forEach(c -> mediaFileDao.updateArtistSortWithId(c));
        return candidatesWithId.stream().map(SortCandidate::getId).collect(Collectors.toList());
    }
}
