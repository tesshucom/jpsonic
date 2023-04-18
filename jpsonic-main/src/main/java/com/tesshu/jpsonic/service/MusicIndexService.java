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

package com.tesshu.jpsonic.service;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.domain.MusicIndex.SortableArtist;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Provides services for grouping artists by index.
 *
 * @author Sindre Mehus
 */
@Service
public class MusicIndexService {

    private final SettingsService settingsService;
    private final MediaFileService mediaFileService;
    private final MusicIndexServiceUtils utils;

    public MusicIndexService(SettingsService settingsService, MediaFileService mediaFileService,
            MusicIndexServiceUtils utils) {
        super();
        this.settingsService = settingsService;
        this.mediaFileService = mediaFileService;
        this.utils = utils;
    }

    /**
     * Returns a map from music indexes to sorted lists of artists that are direct children of the given music folders.
     *
     * @param folders
     *            The music folders.
     *
     * @return A map from music indexes to sets of artists that are direct children of this music file.
     */
    public SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> getIndexedArtists(
            List<MusicFolder> folders) {
        List<MusicIndex.SortableArtistWithMediaFiles> artists = createSortableArtists(folders);
        return sortArtists(artists);
    }

    public SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> getIndexedId3Artists(List<Artist> artists) {
        List<MusicIndex.SortableArtistWithArtist> sortableArtists = createSortableId3Artists(artists);
        return sortArtists(sortableArtists);
    }

    public MusicFolderContent getMusicFolderContent(List<MusicFolder> musicFoldersToUse) {
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> indexedArtists = getIndexedArtists(
                musicFoldersToUse);
        List<MediaFile> singleSongs = getSingleSongs(musicFoldersToUse);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    List<MediaFile> getSingleSongs(List<MusicFolder> folders) {
        List<MediaFile> result = new ArrayList<>();
        for (MusicFolder folder : folders) {
            MediaFile parent = mediaFileService.getMediaFile(folder.toPath());
            if (parent != null) {
                result.addAll(mediaFileService.getChildrenOf(parent, true, false));
            }
        }
        return result;
    }

    public List<MediaFile> getShortcuts(List<MusicFolder> musicFoldersToUse) {
        List<MediaFile> result = new ArrayList<>();
        for (String shortcuts : settingsService.getShortcutsAsArray()) {
            for (MusicFolder musicFolder : musicFoldersToUse) {
                Path shortcutPath = Path.of(musicFolder.getPathString(), shortcuts);
                MediaFile shortcut = mediaFileService.getMediaFile(shortcutPath);
                if (shortcut != null && mediaFileService.getChildrenOf(shortcut, true, true).size() > 0
                        && !result.contains(shortcut)) {
                    result.add(shortcut);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (ArrayList) Not reusable
    private <T extends SortableArtist> SortedMap<MusicIndex, List<T>> sortArtists(List<T> artists) {
        List<MusicIndex> indexes = createIndexesFromExpression(settingsService.getIndexString());
        Comparator<MusicIndex> indexComparator = new MusicIndexComparator(indexes);

        SortedMap<MusicIndex, List<T>> result = new TreeMap<>(indexComparator);

        for (T artist : artists) {
            MusicIndex index = getIndex(artist, indexes);
            List<T> artistSet = result.computeIfAbsent(index, k -> new ArrayList<>());
            artistSet.add(artist);
        }

        for (List<T> artistList : result.values()) {
            Collections.sort(artistList);
        }

        return result;
    }

    /**
     * Creates a new instance by parsing the given expression. The expression consists of an index name, followed by an
     * optional list of one-character prefixes. For example:
     * <p/>
     * <p/>
     * The expression <em>"A"</em> will create the index <em>"A" -&gt; ["A"]</em><br/>
     * The expression <em>"The"</em> will create the index <em>"The" -&gt; ["The"]</em><br/>
     * The expression <em>"A(A&Aring;&AElig;)"</em> will create the index <em>"A" -&gt; ["A", "&Aring;",
     * "&AElig;"]</em><br/>
     * The expression <em>"X-Z(XYZ)"</em> will create the index <em>"X-Z" -&gt; ["X", "Y", "Z"]</em>
     *
     * @param expr
     *            The expression to parse.
     *
     * @return A new instance.
     */
    protected MusicIndex createIndexFromExpression(String expr) {
        int separatorIndex = expr.indexOf('(');
        if (separatorIndex == -1) {

            MusicIndex index = new MusicIndex(expr);
            index.addPrefix(expr);
            return index;
        }

        MusicIndex index = new MusicIndex(expr.substring(0, separatorIndex));
        String prefixString = expr.substring(separatorIndex + 1, expr.length() - 1);
        for (int i = 0; i < prefixString.length(); i++) {
            index.addPrefix(prefixString.substring(i, i + 1));
        }
        return index;
    }

    /**
     * Creates a list of music indexes by parsing the given expression. The expression is a space-separated list of
     * sub-expressions, for which the rules described in {@link #createIndexFromExpression} apply.
     *
     * @param expr
     *            The expression to parse.
     *
     * @return A list of music indexes.
     */
    protected List<MusicIndex> createIndexesFromExpression(String expr) {
        List<MusicIndex> result = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(expr, " ");
        while (tokenizer.hasMoreTokens()) {
            MusicIndex index = createIndexFromExpression(tokenizer.nextToken());
            result.add(index);
        }

        return result;
    }

    // JP >>>>
    private List<MusicIndex.SortableArtistWithMediaFiles> createSortableArtists(List<MusicFolder> folders) {
        return utils.createSortableArtists(folders);
    }

    private List<MusicIndex.SortableArtistWithArtist> createSortableId3Artists(List<Artist> artists) {
        return utils.createSortableId3Artists(artists);
    }
    // <<<< JP

    /**
     * Returns the music index to which the given artist belongs.
     *
     * @param artist
     *            The artist in question.
     * @param indexes
     *            List of available indexes.
     *
     * @return The music index to which this music file belongs, or {@link MusicIndex#OTHER} if no index applies.
     */
    MusicIndex getIndex(SortableArtist artist, List<MusicIndex> indexes) {
        for (MusicIndex index : indexes) {
            for (String prefix : index.getPrefixes()) {
                if (StringUtils.startsWithIgnoreCase(artist.getSortableName(), prefix)) {
                    return index;
                }
            }
        }
        return MusicIndex.OTHER;
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
