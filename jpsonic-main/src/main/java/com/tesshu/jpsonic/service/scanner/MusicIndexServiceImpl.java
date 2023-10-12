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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.domain.MusicIndex.SortableArtist;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.MusicIndexServiceUtils;
import com.tesshu.jpsonic.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MusicIndexServiceImpl implements MusicIndexService {

    private final SettingsService settingsService;
    private final MediaFileService mediaFileService;
    private final ArtistDao artistDao;
    private final MusicIndexServiceUtils utils;

    private MusicIndexParser parser;

    public MusicIndexServiceImpl(SettingsService settingsService, MediaFileService mediaFileService,
            ArtistDao artistDao, MusicIndexServiceUtils utils) {
        super();
        this.settingsService = settingsService;
        this.mediaFileService = mediaFileService;
        this.artistDao = artistDao;
        this.utils = utils;
    }

    List<MediaFile> getSingleSongs(List<MusicFolder> folders) {
        List<MediaFile> result = new ArrayList<>();
        folders.stream().forEach(folder -> {
            MediaFile parent = mediaFileService.getMediaFile(folder.toPath());
            if (parent != null) {
                result.addAll(mediaFileService.getChildrenOf(parent, true, false));
            }
        });
        return result;
    }

    @Override
    public MusicFolderContent getMusicFolderContent(List<MusicFolder> folders) {
        List<MusicIndex.SortableArtistWithMediaFiles> artists = utils.createSortableArtists(folders);
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> indexedArtists = sortArtists(artists);
        List<MediaFile> singleSongs = getSingleSongs(folders);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    @Override
    public SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> getIndexedId3Artists(
            List<MusicFolder> folders) {
        List<Artist> artists = artistDao.getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        List<MusicIndex.SortableArtistWithArtist> sortableArtists = utils.createSortableId3Artists(artists);
        return sortArtists(sortableArtists);
    }

    @Override
    public List<MediaFile> getShortcuts(List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<>();
        settingsService.getShortcutsAsArray().forEach(shortcuts -> {
            musicFolders.forEach(musicFolder -> {
                MediaFile shortcut = mediaFileService.getMediaFile(Path.of(musicFolder.getPathString(), shortcuts));
                if (shortcut != null && mediaFileService.getChildrenOf(shortcut, true, true).size() > 0
                        && !result.contains(shortcut)) {
                    result.add(shortcut);
                }
            });
        });
        return result;
    }

    /**
     * @deprecated Fix so that sorting is not done
     */
    @Deprecated
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (ArrayList) Not reusable
    private <T extends SortableArtist> SortedMap<MusicIndex, List<T>> sortArtists(List<T> artists) {

        Comparator<MusicIndex> indexComparator = new MusicIndexComparator(getParser().getIndexes());

        SortedMap<MusicIndex, List<T>> result = new TreeMap<>(indexComparator);

        for (T artist : artists) {
            MusicIndex index = getParser().getIndex(artist);
            List<T> artistSet = result.computeIfAbsent(index, k -> new ArrayList<>());
            artistSet.add(artist);
        }

        for (List<T> artistList : result.values()) {
            Collections.sort(artistList);
        }

        return result;
    }

    MusicIndexParser getParser() {
        if (parser != null) {
            return parser;
        }
        parser = new MusicIndexParser(settingsService.getIndexString());
        return parser;
    }

    @SuppressWarnings("PMD.NullAssignment") // (musicIndexParser) Intentional assignment
    @Override
    public void clear() {
        parser = null;
    }

    static class MusicIndexParser {

        List<MusicIndex> indexes;

        private MusicIndexParser(String expr) {
            indexes = createIndexesFromExpression(expr);
        }

        private List<MusicIndex> createIndexesFromExpression(String expr) {
            List<MusicIndex> result = new ArrayList<>();
            Stream.of(expr.replaceAll("\\s+", " ").split(" ")).forEach(token -> {
                int separatorIndex = token.indexOf('(');
                MusicIndex index = new MusicIndex(separatorIndex == -1 ? token : token.substring(0, separatorIndex));
                if (separatorIndex == -1) {
                    index.addPrefix(token);
                } else {
                    Stream.of(token.substring(separatorIndex + 1, token.length() - 1).split(""))
                            .forEach(prefix -> index.addPrefix(prefix));
                }
                result.add(index);
            });
            return result;
        }

        public List<MusicIndex> getIndexes() {
            return indexes;
        }

        /**
         * @deprecated Fix to not use SortableArtist
         */
        MusicIndex getIndex(SortableArtist artist) {
            for (MusicIndex index : indexes) {
                if (index.getPrefixes().stream()
                        .filter(prefix -> StringUtils.startsWithIgnoreCase(artist.getSortableName(), prefix))
                        .findFirst().isPresent()) {
                    return index;
                }
            }
            return MusicIndex.OTHER;
        }
    }

    @SuppressWarnings("serial")
    private static class MusicIndexComparator implements Comparator<MusicIndex>, Serializable {

        private final List<MusicIndex> indexes;

        public MusicIndexComparator(List<MusicIndex> indexes) {
            this.indexes = indexes;
        }

        @Override
        public int compare(MusicIndex a, MusicIndex b) {
            int indexA = indexes.indexOf(a);
            int indexB = indexes.indexOf(b);

            if (indexA == -1) {
                indexA = Integer.MAX_VALUE;
            }
            if (indexB == -1) {
                indexB = Integer.MAX_VALUE;
            }

            return Integer.compare(indexA, indexB);
        }
    }
}
