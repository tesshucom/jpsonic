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

 Copyright 2020 (C) tesshu.com
 */
package com.tesshu.jpsonic.domain;

import org.airsonic.player.controller.MainController;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MusicIndexService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.service.search.IndexType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration test where JpsonicComparators are used. This is where legacy
 * sorting behavior changes for some data.
 */
@SpringBootTest
public class JpsonicComparatorsIntegrationTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    private final static List<String> indexList = // @formatter:off
            unmodifiableList(Arrays.asList(
                    "abcde",
                    "abcいうえおあ", // Turn over by reading
                    "abc亜伊鵜絵尾", // Turn over by reading
                    "ＢＣＤＥＡ",
                    "ĆḊÉÁḂ",
                    "DEABC",
                    "the eabcd",
                    "episode 1",
                    "episode 2",
                    "episode 19",
                    "亜伊鵜絵尾",
                    "αβγ",
                    "いうえおあ",
                    "ｴｵｱｲｳ",
                    "オアイウエ",
                    "春夏秋冬",
                    "貼られる",
                    "パラレル",
                    "馬力",
                    "張り切る",
                    "はるなつあきふゆ",
                    "10", // # Num
                    "20", // # Num
                    "50", // # Num
                    "60", // # Num
                    "70", // # Num
                    "98", // # Num
                    "99", // # Num
                    "ゥェォァィ", // # SmallKana　(Not used at the beginning of a word/Generally prohibited characters in index)
                    "ｪｫｧｨｩ", // # SmallKana
                    "ぉぁぃぅぇ", // # SmallKana
                    "♂くんつ") // # Symbol
                    ); // @formatter:on

    public static boolean validateIndexList(List<String> l) {
        return indexList.equals(l);
    }

    {
        musicFolders = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath.apply("Sort/Compare"));
        musicFolders.add(new MusicFolder(1, musicDir, "test date for sorting", true, new Date()));
    }

    @Autowired
    private MainController mainController;

    @Autowired
    private MusicIndexService musicIndexService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private PlaylistDao playlistDao;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private JpsonicComparators comparators;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        settingsService.setSortStrict(true);
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);

        Function<String, Playlist> toPlaylist = (title) -> {
            Date now = new Date();
            Playlist playlist = new Playlist();
            playlist.setName(title);
            playlist.setUsername("admin");
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(false);
            try {
                Thread.sleep(200);
                // Creating a large number of playlists in an instant can be inconsistent with
                // consistency ...
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return playlist;
        };

        populateDatabaseOnlyOnce(() -> {
            if (0 == playlistDao.getAllPlaylists().size()) {
                List<String> shallow = new ArrayList<>();
                shallow.addAll(JpsonicComparatorsTestUtils.jPSonicNaturalList);
                Collections.shuffle(shallow);
                shallow.stream().map(toPlaylist).forEach(p -> playlistDao.createPlaylist(p));
            }
            return true;
        });

        /*
         * Should be more than 30 elements.
         */
        assertEquals(32, indexList.size());
        assertEquals(32, JpsonicComparatorsTestUtils.jPSonicNaturalList.size());
    }

    /**
     * {@link MainController#getMultiFolderChildren}
     * 
     * @throws IOException
     */
    @Test
    public void testGetMultiFolderChildren() throws IOException {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("10");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFolders, IndexType.ARTIST);
        List<MediaFile> artists = mainController.getMultiFolderChildren(result.getMediaFiles());
        List<String> artistNames = artists.stream().map(m -> m.getName()).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(artistNames);
    }

    /**
     * {@link PlaylistService#getAllPlaylists()}
     */
    @Test
    public void testGetAllPlaylists() {
        List<Playlist> all = playlistService.getAllPlaylists();
        List<String> names = all.stream().map(p -> p.getName()).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(names, 8, 9);
        /*
         * Since the reading of playlist name cannot be registered, it is sorted
         * according to the reading analysis of the server.
         */
        assertEquals("abc亜伊鵜絵尾", names.get(8));
        assertEquals("abcいうえおあ", names.get(9));
    }

    /**
     * {@link MediaFileService#getChildrenOf(MediaFile, boolean, boolean, boolean, boolean)}
     * 
     * @throws IOException
     */
    @Test
    public void testGetChildrenOf() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("10");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFolders, IndexType.ARTIST);
        List<MediaFile> files = mediaFileService.getChildrenOf(result.getMediaFiles().get(0), true, true, true);
        List<String> albums = files.stream().map(m -> m.getName()).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(albums);
    }

    /**
     * {@link MusicIndexService#getIndexedArtists(List, boolean)}
     * 
     * @throws IOException
     */
    @Test
    public void testGetIndexedArtists() throws Exception { // @formatter:off
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> m =
                musicIndexService.getIndexedArtists(musicFoldersToUse, true);
        List<String> artists = m.values().stream()
                .flatMap(files -> files.stream())
                .flatMap(files -> files.getMediaFiles().stream())
                .map(file -> file.getName())
                .collect(Collectors.toList());
        assertTrue(validateIndexList(artists));
    } // @formatter:on

    /**
     * {@link PlayQueue#sort(java.util.Comparator)}
     */
    @Test
    public void testPlayQueueSort() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("empty");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFolders, IndexType.SONG);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(comparators.mediaFileOrderBy(ARTIST));
        List<String> artists = playQueue.getFiles().stream().map(m -> m.getArtist()).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(artists);
    }

}
