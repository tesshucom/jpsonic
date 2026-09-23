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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.tesshu.jpsonic.domain.contract.Indexable;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.MusicFolderContent;
import com.tesshu.jpsonic.domain.model.MusicFolderContent.Counts;
import com.tesshu.jpsonic.domain.model.MusicIndex;
import com.tesshu.jpsonic.domain.provider.resource.ArtistProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicIndexProvider;
import com.tesshu.jpsonic.infrastructure.language.MetadataReadingProcessor;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
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
 * <li>{@link #findMusicFolderContent(List, MediaFile.Type...)} Retrieves and
 * classifies directories and standalone files from the specified music folders,
 * returning them as a {@code MusicFolderContent} object.</li>
 * <li>{@link #findIndexedId3Artists(List)} Loads artists based on ID3 tags and
 * classifies them under each {@code MusicIndex}.</li>
 * </ul>
 *
 * @see MusicIndex
 * @see MusicFolderContent
 */
@Service
public class MusicIndexProviderImpl implements MusicIndexProvider {

    private final MediaFileProvider mediaFileProvider;
    private final ArtistProvider artistProvider;
    private final SettingsFacade settingsFacade;
    private final MetadataReadingProcessor readingProcessor;

    private MusicIndexParser parser;

    // spotless:off
    private static final MediaFile.Type[] EXCLUDES = {
            MediaFile.Type.AUDIOBOOK,
            MediaFile.Type.PODCAST,
            MediaFile.Type.VIDEO // Not yet supported. 
            };
    // spotless:on

    public MusicIndexProviderImpl(MediaFileProvider mediaFileProvider,
            ArtistProvider artistProvider, SettingsFacade settingsFacade,
            MetadataReadingProcessor readingProcessor) {
        super();
        this.mediaFileProvider = mediaFileProvider;
        this.artistProvider = artistProvider;
        this.settingsFacade = settingsFacade;
        this.readingProcessor = readingProcessor;
    }

    private <T extends Indexable> SortedMap<MusicIndex, List<T>> createIndexedArtistMap(
            List<T> indexables) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, List<T>> iaMap = new TreeMap<>(comparator);
        indexables.forEach(indexable -> {
            indexable.musicIndex().ifPresent(index -> {
                MusicIndex musicIndex = getParser().getIndex(index);
                if (!iaMap.containsKey(musicIndex)) {
                    iaMap.put(musicIndex, new ArrayList<>());
                }
                iaMap.get(musicIndex).add(indexable);
            });
        });
        return iaMap;
    }

    @Override
    public MusicFolderContent findMusicFolderContent(List<MusicFolder> folders,
            MediaFile.Type... excludes) {
        List<MediaFile> dirs = mediaFileProvider.findIndexedDirectories(folders);
        SortedMap<MusicIndex, List<MediaFile>> indexedArtists = createIndexedArtistMap(dirs);
        List<MediaFile> singleSongs = mediaFileProvider
            .findChildren(folders, 0, Integer.MAX_VALUE, EXCLUDES);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    @Override
    public SortedMap<MusicIndex, List<Artist>> findIndexedId3Artists(List<MusicFolder> folders) {
        List<Artist> indexedArtists = artistProvider.findArtists(folders, 0, Integer.MAX_VALUE);
        return createIndexedArtistMap(indexedArtists);
    }

    MusicIndexParser getParser() {
        if (parser != null) {
            return parser;
        }

        parser = new MusicIndexParser(settingsFacade.get(SKeys.general.index.indexString),
                readingProcessor);
        return parser;
    }

    @SuppressWarnings("PMD.NullAssignment") // (musicIndexParser) Intentional assignment
    @Override
    public void invalidate() {
        parser = null;
    }

    @Override
    public Counts countMusicFolderContent(List<MusicFolder> folders, MediaFile.Type... excludes) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, Integer> indexCounts = new TreeMap<>(comparator);
        mediaFileProvider
            .findIndexWithCounts(folders)
            .forEach(indexWithCount -> indexCounts
                .put(getParser().getIndex(indexWithCount.index()), indexWithCount.count()));
        MediaFile.Type[] singleSongCountsExcludes = Stream
            .concat(Stream.of(excludes), Stream.of(MediaFile.Type.DIRECTORY, MediaFile.Type.ALBUM))
            .distinct()
            .toArray(size -> new MediaFile.Type[size]);
        return new Counts(indexCounts,
                mediaFileProvider.countChildren(folders, singleSongCountsExcludes));
    }

    @Override
    public SortedMap<MusicIndex, Integer> countIndexedId3Artists(List<MusicFolder> folders) {
        Comparator<MusicIndex> comparator = new MusicIndexComparator(getParser().getIndexes());
        SortedMap<MusicIndex, Integer> result = new TreeMap<>(comparator);
        artistProvider
            .findIndexWithCounts(folders)
            .stream()
            .forEach(indexWithCount -> result
                .put(getParser().getIndex(indexWithCount.index()), indexWithCount.count()));
        return result;
    }

    static class MusicIndexParser {

        List<MusicIndex> indexes;
        MetadataReadingProcessor readingProcessor;

        private MusicIndexParser(String expr, MetadataReadingProcessor readingProcessor) {
            indexes = createIndexesFromExpression(expr);
            this.readingProcessor = readingProcessor;
        }

        private List<MusicIndex> createIndexesFromExpression(String expr) {
            List<MusicIndex> result = new ArrayList<>();
            Stream.of(expr.replaceAll("\\s+", " ").split(" ")).forEach(token -> {
                int separatorIndex = token.indexOf('(');
                String index = separatorIndex == -1 ? token : token.substring(0, separatorIndex);
                List<String> prefixes = new ArrayList<>();
                if (separatorIndex == -1) {
                    prefixes.add(token);
                } else {
                    Stream
                        .of(token.substring(separatorIndex + 1, token.length() - 1).split(""))
                        .forEach(prefixes::add);
                }
                result.add(new MusicIndex(index, prefixes));
            });
            return result;
        }

        List<MusicIndex> getIndexes() {
            return indexes;
        }

        MusicIndex getIndex(Indexable indexable) {
            String indexableName = readingProcessor.createIndexableName(indexable);
            return indexes
                .stream()
                .filter(musicIndex -> musicIndex
                    .prefixes()
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
                .filter(musicIndex -> musicIndex.index().equals(index))
                .findFirst()
                .orElse(MusicIndex.OTHER);
        }
    }

    @SuppressWarnings("serial")
    private static class MusicIndexComparator implements Comparator<MusicIndex>, Serializable {

        private final List<MusicIndex> indexes;

        MusicIndexComparator(List<MusicIndex> indexes) {
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
