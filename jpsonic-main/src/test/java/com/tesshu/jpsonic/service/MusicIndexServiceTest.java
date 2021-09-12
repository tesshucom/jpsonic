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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicFolderContent;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit test of {@link MusicIndex}.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
class MusicIndexServiceTest {

    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private MusicIndexService musicIndexService;

    @BeforeEach
    public void setup() throws ExecutionException {
        mediaFileService = mock(MediaFileService.class);
        settingsService = mock(SettingsService.class);
        String articles = SettingsConstants.General.Index.IGNORED_ARTICLES.defaultValue;
        Mockito.when(settingsService.getIgnoredArticles()).thenReturn(articles);
        Mockito.when(settingsService.getIgnoredArticlesAsArray()).thenReturn(articles.split("\\s+"));
        Mockito.when(settingsService.getIndexString())
                .thenReturn(SettingsConstants.General.Index.INDEX_STRING.defaultValue);
        String language = SettingsConstants.General.ThemeAndLang.LOCALE_LANGUAGE.defaultValue;
        String country = SettingsConstants.General.ThemeAndLang.LOCALE_COUNTRY.defaultValue;
        String variant = SettingsConstants.General.ThemeAndLang.LOCALE_VARIANT.defaultValue;
        Mockito.when(settingsService.getLocale()).thenReturn(new Locale(language, country, variant));
        JapaneseReadingUtils readingUtils = new JapaneseReadingUtils(settingsService);
        JpsonicComparators comparators = new JpsonicComparators(settingsService, readingUtils);
        MusicIndexServiceUtils utils = new MusicIndexServiceUtils(settingsService, mediaFileService, readingUtils,
                comparators);

        musicIndexService = new MusicIndexService(settingsService, mediaFileService, utils);
    }

    @Test
    void testGetIndexedArtistsListOfMusicFolderBoolean() {
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(File.class), Mockito.anyBoolean()))
                .thenReturn(new MediaFile());
        MediaFile child1 = new MediaFile();
        child1.setTitle("The Flipper's Guitar");
        child1.setPath("path1");
        MediaFile child2 = new MediaFile();
        child2.setTitle("abcde");
        child2.setPath("path2");
        List<MediaFile> children = Arrays.asList(child1, child2);
        Mockito.when(mediaFileService.getChildrenOf(Mockito.any(MediaFile.class), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(children);
        MusicFolder folder = new MusicFolder(0, new File("path"), "name", true, new Date());

        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> indexedArtists = musicIndexService
                .getIndexedArtists(Arrays.asList(folder), false);
        assertEquals(2, indexedArtists.size());
        Iterator<MusicIndex> iterator = indexedArtists.keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", indexedArtists.get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", indexedArtists.get(musicIndex).get(0).getName());
    }

    @Test
    void testGetIndexedArtistsListOfArtist() {
        Artist artist1 = new Artist(0, "The Flipper's Guitar", null, 0, new Date(), false, 0, null, null, -1);
        Artist artist2 = new Artist(0, "abcde", null, 0, new Date(), false, 0, null, null, -1);
        List<Artist> artists = Arrays.asList(artist1, artist2);

        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithArtist>> indexedArtists = musicIndexService
                .getIndexedArtists(artists);
        assertEquals(2, indexedArtists.size());
        Iterator<MusicIndex> iterator = indexedArtists.keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", indexedArtists.get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", indexedArtists.get(musicIndex).get(0).getName());
    }

    @Test
    void testGetMusicFolderContent() {
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(File.class), Mockito.anyBoolean()))
                .thenReturn(new MediaFile());
        MediaFile child1 = new MediaFile();
        child1.setTitle("The Flipper's Guitar");
        child1.setPath("path1");
        MediaFile child2 = new MediaFile();
        child2.setTitle("abcde");
        child2.setPath("path2");
        List<MediaFile> children = Arrays.asList(child1, child2);
        MediaFile song = new MediaFile();
        song.setTitle("It's file directly under the music folder");
        song.setPath("path3");
        List<MediaFile> songs = Arrays.asList(song);
        Mockito.when(mediaFileService.getChildrenOf(Mockito.any(MediaFile.class), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(children)
                .thenReturn(songs).thenThrow(new RuntimeException("Fail"));
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(File.class), Mockito.anyBoolean()))
                .thenReturn(new MediaFile());
        MusicFolder folder = new MusicFolder(0, new File("path"), "name", true, new Date());

        MusicFolderContent content = musicIndexService.getMusicFolderContent(Arrays.asList(folder), false);
        assertEquals(2, content.getIndexedArtists().size());
        Iterator<MusicIndex> iterator = content.getIndexedArtists().keySet().iterator();
        MusicIndex musicIndex = iterator.next();
        assertEquals("A", musicIndex.getIndex());
        assertEquals("abcde", content.getIndexedArtists().get(musicIndex).get(0).getName());
        musicIndex = iterator.next();
        assertEquals("F", musicIndex.getIndex());
        assertEquals("The Flipper's Guitar", content.getIndexedArtists().get(musicIndex).get(0).getName());

        assertEquals(1, content.getSingleSongs().size());
        assertEquals("It's file directly under the music folder", content.getSingleSongs().get(0).getTitle());
    }

    @Test
    void testGetShortcuts() throws URISyntaxException {
        Mockito.when(settingsService.getShortcutsAsArray())
                .thenReturn(StringUtil.split(SettingsConstants.General.Extension.SHORTCUTS.defaultValue + " Metadata"));
        MediaFile song = new MediaFile();
        song.setTitle("Files directly under the shortcut directory");
        song.setPath("path");
        Mockito.when(mediaFileService.getMediaFile(Mockito.any(File.class), Mockito.anyBoolean())).thenReturn(song);
        File dummy = new File(MusicIndexServiceTest.class.getResource("/MEDIAS").toURI());
        MusicFolder folder = new MusicFolder(dummy, "Music", true, new Date());

        List<MediaFile> shortcuts = musicIndexService.getShortcuts(Arrays.asList(folder));
        assertEquals(1, shortcuts.size());
    }

    @Test
    void testCreateIndexFromExpression() {
        MusicIndex index = musicIndexService.createIndexFromExpression("A");
        assertEquals("A", index.getIndex());
        assertEquals(1, index.getPrefixes().size());
        assertEquals("A", index.getPrefixes().get(0));

        index = musicIndexService.createIndexFromExpression("The");
        assertEquals("The", index.getIndex());
        assertEquals(1, index.getPrefixes().size());
        assertEquals("The", index.getPrefixes().get(0));

        index = musicIndexService.createIndexFromExpression("X-Z(XYZ)");
        assertEquals("X-Z", index.getIndex());
        assertEquals(3, index.getPrefixes().size());
        assertEquals("X", index.getPrefixes().get(0));
        assertEquals("Y", index.getPrefixes().get(1));
        assertEquals("Z", index.getPrefixes().get(2));
    }

    @Test
    void testCreateIndexesFromExpression() {
        List<MusicIndex> indexes = musicIndexService.createIndexesFromExpression("A B  The X-Z(XYZ)");
        assertEquals(4, indexes.size());

        assertEquals("A", indexes.get(0).getIndex());
        assertEquals(1, indexes.get(0).getPrefixes().size());
        assertEquals("A", indexes.get(0).getPrefixes().get(0));

        assertEquals("B", indexes.get(1).getIndex());
        assertEquals(1, indexes.get(1).getPrefixes().size());
        assertEquals("B", indexes.get(1).getPrefixes().get(0));

        assertEquals("The", indexes.get(2).getIndex());
        assertEquals(1, indexes.get(2).getPrefixes().size());
        assertEquals("The", indexes.get(2).getPrefixes().get(0));

        assertEquals("X-Z", indexes.get(3).getIndex());
        assertEquals(3, indexes.get(3).getPrefixes().size());
        assertEquals("X", indexes.get(3).getPrefixes().get(0));
        assertEquals("Y", indexes.get(3).getPrefixes().get(1));
        assertEquals("Z", indexes.get(3).getPrefixes().get(2));
    }
}
