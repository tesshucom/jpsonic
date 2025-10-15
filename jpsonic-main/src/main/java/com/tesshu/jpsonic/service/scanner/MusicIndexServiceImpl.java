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
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.Indexable;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicFolderContent.Counts;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicIndexService;
import com.tesshu.jpsonic.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service class for classifying and retrieving artists and directories in music
 * folders using indexed keys ({@link MusicIndex}).
 * <p>
 * Similar to Subsonic, it classifies entries based on artist names. However,
 * this implementation assumes a dual-language index (Japanese and English),
 * incorporating phonetic readings and alphabetical order. Optimizations are
 * applied to improve classification speed.
 *
 * <h3>Main Responsibilities</h3>
 * <ul>
 * <li>{@link #getMusicFolderContent(List, MediaType...)} Retrieves and
 * classifies directories and standalone files from the specified music folders,
 * returning them as a {@code MusicFolderContent} object.</li>
 * <li>{@link #getIndexedId3Artists(List)} Loads artists based on ID3 tags and
 * classifies them under each {@code MusicIndex}.</li>
 * <li>{@link #getShortcuts(List)} Retrieves shortcut information for the
 * specified music folders.</li>
 * </ul>
 *
 * @see MusicIndex
 * @see MusicFolderContent
 */
@Service
public class MusicIndexServiceImpl implements MusicIndexService {

    private final SettingsService settingsService;
    private final MediaFileService mediaFileService;
    private final ArtistDao artistDao;
    private final JapaneseReadingUtils readingUtils;

    private MusicIndexParser parser;

    public MusicIndexServiceImpl(SettingsService settingsService, MediaFileService mediaFileService,
            ArtistDao artistDao, JapaneseReadingUtils readingUtils) {
        super();
        this.settingsService = settingsService;
        this.mediaFileService = mediaFileService;
        this.artistDao = artistDao;
        this.readingUtils = readingUtils;
    }

    private <T extends Indexable> SortedMap<MusicIndex, List<T>> createIndexedArtistMap(
            List<T> indexables) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, List<T>> iaMap = new TreeMap<>(comparator);
        indexables.forEach(indexable -> {
            MusicIndex musicIndex = getParser().getIndex(indexable.getMusicIndex());
            if (!iaMap.containsKey(musicIndex)) {
                iaMap.put(musicIndex, new ArrayList<>());
            }
            iaMap.get(musicIndex).add(indexable);
        });
        return iaMap;
    }

    @Override
    public MusicFolderContent getMusicFolderContent(List<MusicFolder> folders,
            MediaType... excludes) {
        List<MediaFile> dirs = mediaFileService.getIndexedDirs(folders);
        SortedMap<MusicIndex, List<MediaFile>> indexedArtists = createIndexedArtistMap(dirs);
        List<MediaFile> singleSongs = mediaFileService
            .getDirectChildFiles(folders, 0, Integer.MAX_VALUE, excludes);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    @Override
    public SortedMap<MusicIndex, List<Artist>> getIndexedId3Artists(List<MusicFolder> folders) {
        List<Artist> indexedArtists = artistDao
            .getAlphabetialArtists(0, Integer.MAX_VALUE, folders);
        return createIndexedArtistMap(indexedArtists);
    }

    @Override
    public List<MediaFile> getShortcuts(List<MusicFolder> musicFolders) {
        List<MediaFile> result = new ArrayList<>();
        settingsService.getShortcutsAsArray().forEach(shortcuts -> {
            musicFolders.forEach(musicFolder -> {
                MediaFile shortcut = mediaFileService
                    .getMediaFile(Path.of(musicFolder.getPathString(), shortcuts));
                if (shortcut != null
                        && !mediaFileService.getChildrenOf(shortcut, true, true).isEmpty()
                        && !result.contains(shortcut)) {
                    result.add(shortcut);
                }
            });
        });
        return result;
    }

    MusicIndexParser getParser() {
        if (parser != null) {
            return parser;
        }
        parser = new MusicIndexParser(settingsService.getIndexString(), readingUtils);
        return parser;
    }

    @SuppressWarnings("PMD.NullAssignment") // (musicIndexParser) Intentional assignment
    @Override
    public void clear() {
        parser = null;
    }

    @Override
    public Counts getMusicFolderContentCounts(List<MusicFolder> folders, MediaType... excludes) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, Integer> indexCounts = new TreeMap<>(comparator);
        mediaFileService
            .getMudicIndexCounts(folders)
            .forEach(indexWithCount -> indexCounts
                .put(getParser().getIndex(indexWithCount.index()),
                        indexWithCount.directoryCount()));
        MediaType[] singleSongCountsExcludes = Stream
            .concat(Stream.of(excludes), Stream.of(MediaType.DIRECTORY, MediaType.ALBUM))
            .distinct()
            .toArray(size -> new MediaType[size]);
        return new Counts(indexCounts,
                mediaFileService.getChildSizeOf(folders, singleSongCountsExcludes));
    }

    @Override
    public SortedMap<MusicIndex, Integer> getIndexedId3ArtistCounts(List<MusicFolder> folders) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, Integer> result = new TreeMap<>(comparator);
        artistDao
            .getMudicIndexCounts(folders)
            .stream()
            .forEach(indexWithCount -> result
                .put(getParser().getIndex(indexWithCount.index()), indexWithCount.artistCount()));
        return result;
    }

    static class MusicIndexParser {

        List<MusicIndex> indexes;
        JapaneseReadingUtils readingUtils;

        private MusicIndexParser(String expr, JapaneseReadingUtils readingUtils) {
            indexes = createIndexesFromExpression(expr);
            this.readingUtils = readingUtils;
        }

        private List<MusicIndex> createIndexesFromExpression(String expr) {
            List<MusicIndex> result = new ArrayList<>();
            Stream.of(expr.replaceAll("\\s+", " ").split(" ")).forEach(token -> {
                int separatorIndex = token.indexOf('(');
                MusicIndex index = new MusicIndex(
                        separatorIndex == -1 ? token : token.substring(0, separatorIndex));
                if (separatorIndex == -1) {
                    index.addPrefix(token);
                } else {
                    Stream
                        .of(token.substring(separatorIndex + 1, token.length() - 1).split(""))
                        .forEach(index::addPrefix);
                }
                result.add(index);
            });
            return result;
        }

        public List<MusicIndex> getIndexes() {
            return indexes;
        }

        MusicIndex getIndex(Indexable indexable) {
            String indexableName = readingUtils.createIndexableName(indexable);
            return indexes
                .stream()
                .filter(musicIndex -> musicIndex
                    .getPrefixes()
                    .stream()
                    .filter(prefix -> StringUtils.startsWithIgnoreCase(indexableName, prefix))
                    .findFirst()
                    .isPresent())
                .findFirst()
                .orElse(MusicIndex.OTHER);
        }

        private MusicIndex getIndex(String index) {
            return indexes
                .stream()
                .filter(musicIndex -> musicIndex.getIndex().equals(index))
                .findFirst()
                .orElse(MusicIndex.OTHER);
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
