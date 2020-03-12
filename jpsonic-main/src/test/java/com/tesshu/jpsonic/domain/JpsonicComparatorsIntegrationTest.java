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

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.domain;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest
public class JpsonicComparatorsIntegrationTest extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

    public final static List<String> indexList = Collections.unmodifiableList(
            Arrays.asList(
                    "abcde", "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC", "eabcd", "亜伊鵜絵尾", "αβγ", "いうえおあ", "ｴｵｱｲｳ", "オアイウエ",
                    "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ",
                    "10", "20", "30", "40", "50", "60", "70", "80", "90", "98", "99", "ゥェォァィ", "ｪｫｧｨｩ", "ぉぁぃぅぇ", "♂くんつ"));

    public final static List<String> jPSonicNaturalList = Collections.unmodifiableList(
            Arrays.asList(
                    "10", "20", "30", "40", "50", "60", "70", "80", "90", "98","99",
                    "abcde", "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC", "eabcd", "亜伊鵜絵尾", "αβγ", "いうえおあ", "ゥェォァィ", "ｴｵｱｲｳ",
                    "ｪｫｧｨｩ", "ぉぁぃぅぇ", "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ", "♂くんつ"));;

    public final static List<String> childrenList = Collections.unmodifiableList(
            Arrays.asList(
                    "empty30", "empty29", "empty28", "empty27", "empty26", "empty25", "empty24", "empty23", "empty22", "empty21", 
                    "empty20", "empty19", "empty18", "empty17", "empty16", "empty15", "empty14", "empty13", "empty12", "empty11", 
                    "empty10", "empty09", "empty08", "empty07", "empty06", "empty05", "empty04", "empty03", "empty02", "empty01", "empty00"));

    {
        musicFolders = new ArrayList<>();
        File musicDir1 = new File(resolveBaseMediaPath.apply("Sort/Artists"));
        musicFolders.add(new MusicFolder(1, musicDir1, "Artists", true, new Date()));
        File musicDir2 = new File(resolveBaseMediaPath.apply("Sort/Albums"));
        musicFolders.add(new MusicFolder(2, musicDir2, "Albums", true, new Date()));
        File musicDir3 = new File(resolveBaseMediaPath.apply("Sort/ArtistsAlphaNum"));
        musicFolders.add(new MusicFolder(3, musicDir3, "ArtistsAlphaNum", true, new Date()));
        File musicDir4 = new File(resolveBaseMediaPath.apply("Sort/AlbumsAlphaNum"));
        musicFolders.add(new MusicFolder(4, musicDir4, "AlbumsAlphaNum", true, new Date()));
    }

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
        populateDatabaseOnlyOnce();

        Function<String, Playlist> toPlaylist = (title) -> {
            Date now = new Date();
            Playlist playlist = new Playlist();
            playlist.setName(title);
            playlist.setUsername("admin");
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(false);
            return playlist;
        };

        if (0 == playlistDao.getAllPlaylists().size()) {
            List<String> shallow = new ArrayList<>();
            shallow.addAll(jPSonicNaturalList);
            Collections.shuffle(shallow);
            shallow.stream().map(toPlaylist).forEach(p -> playlistDao.createPlaylist(p));
        }

        /*
         * Should be more than 30 elements.
         */
        assertEquals(31, indexList.size());
        assertEquals(31, jPSonicNaturalList.size());
        assertEquals(31, childrenList.size());

    }

    public static boolean validateIndexList(List<String> l) {
        return indexList.equals(l);
    }

    public static boolean validateJPSonicNaturalList(List<String> l) {
        return jPSonicNaturalList.equals(l);
    }

    @Test
    public void testIndex() throws Exception {
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> m = musicIndexService.getIndexedArtists(musicFoldersToUse, true);
        List<String> artists = m.values().stream()
                .flatMap(files -> files.stream())
                .flatMap(files -> files.getMediaFiles().stream())
                .map(file -> file.getName())
                .collect(Collectors.toList());
        assertTrue(validateIndexList(artists));
    }

    @Test
    public void testAlbumDirectory() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(1));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ARTIST);
        List<MediaFile> files = mediaFileService.getChildrenOf(result.getMediaFiles().get(0), true, true, true);
        List<String> albums = files.stream().map(m -> m.getName()).collect(Collectors.toList());
        assertTrue(validateJPSonicNaturalList(albums));
    }

    @Test
    public void testPlayQueueSortByArtist() throws IOException {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ALBUM abcde");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(comparators.mediaFileOrderBy(ARTIST));
        List<String> artists = playQueue.getFiles().stream().map(m -> m.getArtist()).collect(Collectors.toList());
        assertTrue(validateJPSonicNaturalList(artists));
    }

    @Test
    public void testPlaylistServiceGetAll() throws Exception {
        List<Playlist> all = playlistService.getAllPlaylists();
        assertTrue(validateJPSonicNaturalList(all.stream().map(p -> p.getName()).collect(Collectors.toList())));
    }

    /*
     * DB dependent. Sort rules vary depending on the DB.
     */
    public void testAlbumAllOfHome() {
        // pending!
    }

}
