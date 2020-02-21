/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import com.tesshu.jpsonic.service.MusicIndexServiceUtils;

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolderContent;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.MusicIndex.SortableArtist;
import org.airsonic.player.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Provides services for grouping artists by index.
 *
 * @author Sindre Mehus
 */
@Service
public class MusicIndexService {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MusicIndexServiceUtils utils;

    /**
     * Returns a map from music indexes to sorted lists of artists that are direct children of the given music folders.
     *
     * @param folders The music folders.
     * @param refresh Whether to look for updates by checking the last-modified timestamp of the music folders.
     * @return A map from music indexes to sets of artists that are direct children of this music file.
     */
    public SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> getIndexedArtists(List<MusicFolder> folders, boolean refresh) {
        List<MusicIndex.SortableArtistWithMediaFiles> artists = createSortableArtists(folders, refresh);
        return sortArtists(artists);
    }

    public SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> getIndexedArtists(List<Artist> artists) {
        List<MusicIndex.SortableArtistWithArtist> sortableArtists = createSortableArtists(artists);
        return sortArtists(sortableArtists);
    }

    public MusicFolderContent getMusicFolderContent(List<MusicFolder> musicFoldersToUse, boolean refresh) {
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> indexedArtists = getIndexedArtists(musicFoldersToUse, refresh);
        List<MediaFile> singleSongs = getSingleSongs(musicFoldersToUse, refresh);
        return new MusicFolderContent(indexedArtists, singleSongs);
    }

    private List<MediaFile> getSingleSongs(List<MusicFolder> folders, boolean refresh) {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (MusicFolder folder : folders) {
            MediaFile parent = mediaFileService.getMediaFile(folder.getPath(), !refresh);
            result.addAll(mediaFileService.getChildrenOf(parent, true, false, true, !refresh));
        }
        return result;
    }

    public List<MediaFile> getShortcuts(List<MusicFolder> musicFoldersToUse) {
        List<MediaFile> result = new ArrayList<MediaFile>();
        for (String shortcut : settingsService.getShortcutsAsArray()) {
            for (MusicFolder musicFolder : musicFoldersToUse) {
                File file = new File(musicFolder.getPath(), shortcut);
                if (FileUtil.exists(file)) {
                    result.add(mediaFileService.getMediaFile(file, true));
                }
            }
        }
        return result;
    }

    private <T extends SortableArtist> SortedMap<MusicIndex, List<T>> sortArtists(List<T> artists) {
        List<MusicIndex> indexes = createIndexesFromExpression(settingsService.getIndexString());
        Comparator<MusicIndex> indexComparator = new MusicIndexComparator(indexes);

        SortedMap<MusicIndex, List<T>> result = new TreeMap<MusicIndex, List<T>>(indexComparator);

        for (T artist : artists) {
            MusicIndex index = getIndex(artist, indexes);
            List<T> artistSet = result.computeIfAbsent(index, k -> new ArrayList<T>());
            artistSet.add(artist);
        }

        for (List<T> artistList : result.values()) {
            Collections.sort(artistList);
        }

        return result;
    }

    /**
     * Creates a new instance by parsing the given expression.  The expression consists of an index name, followed by
     * an optional list of one-character prefixes. For example:<p/>
     * <p/>
     * The expression <em>"A"</em> will create the index <em>"A" -&gt; ["A"]</em><br/>
     * The expression <em>"The"</em> will create the index <em>"The" -&gt; ["The"]</em><br/>
     * The expression <em>"A(A&Aring;&AElig;)"</em> will create the index <em>"A" -&gt; ["A", "&Aring;", "&AElig;"]</em><br/>
     * The expression <em>"X-Z(XYZ)"</em> will create the index <em>"X-Z" -&gt; ["X", "Y", "Z"]</em>
     *
     * @param expr The expression to parse.
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
     * Creates a list of music indexes by parsing the given expression.  The expression is a space-separated list of
     * sub-expressions, for which the rules described in {@link #createIndexFromExpression} apply.
     *
     * @param expr The expression to parse.
     * @return A list of music indexes.
     */
    protected List<MusicIndex> createIndexesFromExpression(String expr) {
        List<MusicIndex> result = new ArrayList<MusicIndex>();

        StringTokenizer tokenizer = new StringTokenizer(expr, " ");
        while (tokenizer.hasMoreTokens()) {
            MusicIndex index = createIndexFromExpression(tokenizer.nextToken());
            result.add(index);
        }

        return result;
    }

    // JP >>>>
    private List<MusicIndex.SortableArtistWithMediaFiles> createSortableArtists(List<MusicFolder> folders, boolean refresh) {
        return utils.createSortableArtists(folders, refresh);
    }

    private List<MusicIndex.SortableArtistWithArtist> createSortableArtists(List<Artist> artists) {
        return utils.createSortableArtists(artists);
    }
    // <<<< JP

    /**
     * Returns the music index to which the given artist belongs.
     *
     * @param artist  The artist in question.
     * @param indexes List of available indexes.
     * @return The music index to which this music file belongs, or {@link MusicIndex#OTHER} if no index applies.
     */
    private MusicIndex getIndex(SortableArtist artist, List<MusicIndex> indexes) {
        String sortableName = artist.getSortableName().toUpperCase();
        for (MusicIndex index : indexes) {
            for (String prefix : index.getPrefixes()) {
                if (sortableName.startsWith(prefix.toUpperCase())) {
                    return index;
                }
            }
        }
        return MusicIndex.OTHER;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    private static class MusicIndexComparator implements Comparator<MusicIndex>, Serializable {

        private List<MusicIndex> indexes;

        public MusicIndexComparator(List<MusicIndex> indexes) {
            this.indexes = indexes;
        }

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