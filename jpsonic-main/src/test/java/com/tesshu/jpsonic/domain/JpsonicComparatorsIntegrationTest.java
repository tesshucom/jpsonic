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

package com.tesshu.jpsonic.domain;

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.airsonic.player.controller.MainController;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MusicIndexService;
import org.airsonic.player.service.PlaylistService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.service.search.IndexType;
import org.airsonic.player.service.search.SearchCriteria;
import org.airsonic.player.service.search.SearchCriteriaDirector;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test where JpsonicComparators are used. This is where legacy sorting behavior changes for some data.
 */
@SpringBootTest
public class JpsonicComparatorsIntegrationTest extends AbstractAirsonicHomeTest {

    private static final Logger LOG = LoggerFactory.getLogger(JpsonicComparatorsIntegrationTest.class);

    private static final List<MusicFolder> MUSIC_FOLDERS;

    static {
        MUSIC_FOLDERS = new ArrayList<>();
        File musicDir = new File(resolveBaseMediaPath("Sort/Compare"));
        MUSIC_FOLDERS.add(new MusicFolder(1, musicDir, "test date for sorting", true, new Date()));
    }

    private static final List<String> INDEX_LIST = unmodifiableList(Arrays.asList("abcde", "abcいうえおあ", // Turn over by
            // reading
            "abc亜伊鵜絵尾", // Turn over by reading
            "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC", "the eabcd", "episode 1", "episode 2", "episode 19", "亜伊鵜絵尾", "αβγ", "いうえおあ",
            "ｴｵｱｲｳ", "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ", "10", // # Num
            "20", // # Num
            "50", // # Num
            "60", // # Num
            "70", // # Num
            "98", // # Num
            "99", // # Num
            "ゥェォァィ", // # SmallKana (Not used at the beginning of a word/Generally prohibited characters in index)
            "ｪｫｧｨｩ", // # SmallKana
            "ぉぁぃぅぇ", // # SmallKana
            "♂くんつ") // # Symbol
    );

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

    @Autowired
    private SearchCriteriaDirector director;

    public static boolean validateIndexList(List<String> l) {
        return INDEX_LIST.equals(l);
    }

    @Override
    public List<MusicFolder> getMusicFolders() {
        return MUSIC_FOLDERS;
    }

    @Before
    public void setup() {
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
                LOG.error("It is possible that the playlist could not be initialized due to high load.", e);
            }
            return playlist;
        };

        populateDatabaseOnlyOnce(() -> {
            if (0 == playlistDao.getAllPlaylists().size()) {
                List<String> shallow = new ArrayList<>(JpsonicComparatorsTestUtils.JPSONIC_NATURAL_LIST);
                Collections.shuffle(shallow);
                shallow.stream().map(toPlaylist).forEach(p -> playlistDao.createPlaylist(p));
            }
            return true;
        });

        /*
         * Should be more than 30 elements.
         */
        assertEquals(32, INDEX_LIST.size());
        assertEquals(32, JpsonicComparatorsTestUtils.JPSONIC_NATURAL_LIST.size());
    }

    /**
     * {@link MainController#getMultiFolderChildren}
     */
    @Test
    public void testGetMultiFolderChildren() throws IOException {
        SearchCriteria criteria = director.construct("10", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS,
                IndexType.ARTIST);
        SearchResult result = searchService.search(criteria);
        List<MediaFile> artists = mainController.getMultiFolderChildren(result.getMediaFiles());
        List<String> artistNames = artists.stream().map(MediaFile::getName).collect(Collectors.toList());
        assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(artistNames));
    }

    /**
     * {@link PlaylistService#getAllPlaylists()}
     */
    @Test
    public void testGetAllPlaylists() {
        List<Playlist> all = playlistService.getAllPlaylists();
        List<String> names = all.stream().map(Playlist::getName).collect(Collectors.toList());
        JpsonicComparatorsTestUtils.validateNaturalList(names, 8, 9);
        /*
         * Since the reading of playlist name cannot be registered, it is sorted according to the reading analysis of
         * the server.
         */
        assertEquals("abc亜伊鵜絵尾", names.get(8));
        assertEquals("abcいうえおあ", names.get(9));
    }

    /**
     * {@link MediaFileService#getChildrenOf(MediaFile, boolean, boolean, boolean, boolean)}
     */
    @Test
    public void testGetChildrenOf() throws IOException {
        SearchCriteria criteria = director.construct("10", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS,
                IndexType.ARTIST);
        SearchResult result = searchService.search(criteria);
        List<MediaFile> files = mediaFileService.getChildrenOf(result.getMediaFiles().get(0), true, true, true);
        List<String> albums = files.stream().map(MediaFile::getName).collect(Collectors.toList());
        assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(albums));
    }

    /**
     * {@link MusicIndexService#getIndexedArtists(List, boolean)}
     */
    @Test
    public void testGetIndexedArtists() {
        List<MusicFolder> musicFoldersToUse = Arrays.asList(MUSIC_FOLDERS.get(0));
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> m = musicIndexService
                .getIndexedArtists(musicFoldersToUse, true);
        List<String> artists = m.values().stream().flatMap(Collection::stream)
                .flatMap(files -> files.getMediaFiles().stream()).map(MediaFile::getName).collect(Collectors.toList());
        assertTrue(validateIndexList(artists));
    }

    /**
     * {@link PlayQueue#sort(java.util.Comparator)}
     */
    @Test
    public void testPlayQueueSort() throws IOException {
        SearchCriteria criteria = director.construct("empty", 0, Integer.MAX_VALUE, false, MUSIC_FOLDERS,
                IndexType.SONG);
        SearchResult result = searchService.search(criteria);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(comparators.mediaFileOrderBy(ARTIST));
        List<String> artists = playQueue.getFiles().stream().map(MediaFile::getArtist).collect(Collectors.toList());
        assertTrue(JpsonicComparatorsTestUtils.validateNaturalList(artists));
    }

}
