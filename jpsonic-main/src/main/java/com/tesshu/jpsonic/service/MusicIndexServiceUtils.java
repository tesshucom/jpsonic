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

package com.tesshu.jpsonic.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.MusicIndex.SortableArtist;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Utility class for injecting into legacy MusicIndexService. Supplement processing that is lacking in legacy services.
 * 
 * Delegate the entire method, because the processing difference from the legacy code is large. The legacy index sort is
 * a "private", basic specification and cannot be changed. In practice, there is an inconsistency in the sorting
 * process, which makes people in languages ​​that are sensitive to sorting feel uncomfortable.
 * 
 * Jpsonic will change these and make the processing as common as possible so that they can be used naturally.
 */
@Component
@DependsOn({ "settingsService", "mediaFileService", "japaneseReadingUtils", "jpsonicComparators" })
public class MusicIndexServiceUtils {

    private final SettingsService settingsService;
    private final MediaFileService mediaFileService;
    private final JapaneseReadingUtils utils;
    private final JpsonicComparators comparators;

    public MusicIndexServiceUtils(SettingsService s, MediaFileService m, JapaneseReadingUtils utils,
            JpsonicComparators comp) {
        super();
        this.settingsService = s;
        this.mediaFileService = m;
        this.utils = utils;
        this.comparators = comp;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (MusicIndex.~) Not reusable
    public List<MusicIndex.SortableArtistWithArtist> createSortableArtists(List<Artist> artists) {
        List<MusicIndex.SortableArtistWithArtist> result = new ArrayList<>();
        String[] ignoredArticles = settingsService.getIgnoredArticlesAsArray();
        Comparator<SortableArtist> c = comparators.sortableArtistOrder();
        for (Artist artist : artists) {
            String sortableName = createSortableName(utils.createIndexableName(artist), ignoredArticles);
            result.add(new MusicIndex.SortableArtistWithArtist(artist.getName(), sortableName, artist, c));
        }
        return result;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (MusicIndex.~) Not reusable
    public List<MusicIndex.SortableArtistWithMediaFiles> createSortableArtists(List<MusicFolder> folders,
            boolean refresh) {
        String[] ignoredArticles = settingsService.getIgnoredArticlesAsArray();
        String[] shortcuts = settingsService.getShortcutsAsArray();
        SortedMap<String, MusicIndex.SortableArtistWithMediaFiles> artistMap = new TreeMap<>();
        Set<String> shortcutSet = new HashSet<>(Arrays.asList(shortcuts));

        Comparator<SortableArtist> c = comparators.sortableArtistOrder();
        for (MusicFolder folder : folders) {

            MediaFile root = mediaFileService.getMediaFile(folder.getPath(), !refresh);
            List<MediaFile> children = mediaFileService.getChildrenOf(root, false, true, true, !refresh);
            for (MediaFile child : children) {
                if (shortcutSet.contains(child.getName())) {
                    continue;
                }

                String sortableName = createSortableName(utils.createIndexableName(child), ignoredArticles);
                MusicIndex.SortableArtistWithMediaFiles artist = artistMap.get(sortableName);
                if (artist == null) {
                    artist = new MusicIndex.SortableArtistWithMediaFiles(child.getName(), sortableName, c);
                    artistMap.put(sortableName, artist);
                }
                artist.addMediaFile(child);
            }
        }

        return new ArrayList<>(artistMap.values());
    }

    private String createSortableName(String name, String... ignoredArticles) {
        String uppercaseName = name.toUpperCase(settingsService.getLocale());
        for (String article : ignoredArticles) {
            if (uppercaseName.startsWith(article.toUpperCase(settingsService.getLocale()) + " ")) {
                return name.substring(article.length() + 1) + ", " + article;
            }
        }
        return name;
    }

}
