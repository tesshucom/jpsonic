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
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.PlayQueue.SortOrder;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * Test to correct sort inconsistencies.
 * Testing may not be possible on an OS that does not support fsync.
 */
@SpringBootTest
public class SortingIntegrationTestCase extends AbstractAirsonicHomeTest {

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

    public final static List<String> alphaNumList = Collections
            .unmodifiableList(Arrays.asList("09X Radonius", "10X Radonius", "20X Radonius", "20X Radonius Prime",
                    "30X Radonius", "40X Radonius", "200X Radonius", "1000X Radonius Maximus", "Allegia 6R Clasteron",
                    "Allegia 50B Clasteron", "Allegia 50 Clasteron", "Allegia 51 Clasteron", "Allegia 500 Clasteron",
                    "Alpha 2", "Alpha 2A", "Alpha 2A-900", "Alpha 2A-8000", "Alpha 100", "Alpha 200",
                    "Callisto Morphamax", "Callisto Morphamax 500", "Callisto Morphamax 600", "Callisto Morphamax 700",
                    "Callisto Morphamax 5000", "Callisto Morphamax 6000 SE", "Callisto Morphamax 6000 SE2",
                    "Callisto Morphamax 7000", "Xiph Xlater 5", "Xiph Xlater 40", "Xiph Xlater 50", "Xiph Xlater 58",
                    "Xiph Xlater 300", "Xiph Xlater 500", "Xiph Xlater 2000", "Xiph Xlater 5000", "Xiph Xlater 10000"));

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
        assertEquals(36, alphaNumList.size());

    }

    public static boolean validateIndexList(List<String> l) {
        return indexList.equals(l);
    }

    public static boolean validateJPSonicNaturalList(List<String> l) {
        return jPSonicNaturalList.equals(l);
    }

    public static boolean validateAlphaNumList(List<String> l) {
        return alphaNumList.equals(l);
    }

    @Test
    public void testCompareGenre() throws Exception {

        List<Genre> genres = Arrays.asList(new Genre("A", 1, 3), new Genre("B", 2, 2), new Genre("C", 3, 1));

        genres.sort(comparators.genreOrder(false));
        assertEquals("C", genres.get(0).getName());
        assertEquals("B", genres.get(1).getName());
        assertEquals("A", genres.get(2).getName());

        genres.sort(comparators.genreOrder(true));
        assertEquals("A", genres.get(0).getName());
        assertEquals("B", genres.get(1).getName());
        assertEquals("C", genres.get(2).getName());

    }

    @Test
    public void testCompareAlbums() throws Exception {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        MediaFileComparator comparator = comparators.mediaFileOrder();

        MediaFile albumA2012 = new MediaFile();
        albumA2012.setMediaType(MediaFile.MediaType.ALBUM);
        albumA2012.setPath("a");
        albumA2012.setYear(2012);

        MediaFile albumB2012 = new MediaFile();
        albumB2012.setMediaType(MediaFile.MediaType.ALBUM);
        albumB2012.setPath("b");
        albumB2012.setYear(2012);

        MediaFile album2013 = new MediaFile();
        album2013.setMediaType(MediaFile.MediaType.ALBUM);
        album2013.setPath("c");
        album2013.setYear(2013);

        MediaFile albumWithoutYear = new MediaFile();
        albumWithoutYear.setMediaType(MediaFile.MediaType.ALBUM);
        albumWithoutYear.setPath("c");

        assertEquals(0, comparator.compare(albumWithoutYear, albumWithoutYear));
        assertEquals(0, comparator.compare(albumA2012, albumA2012));

        assertEquals(-1, comparator.compare(albumA2012, albumWithoutYear));
        assertEquals(-1, comparator.compare(album2013, albumWithoutYear));
        assertEquals(1, comparator.compare(album2013, albumA2012));

        assertEquals(1, comparator.compare(albumWithoutYear, albumA2012));
        assertEquals(1, comparator.compare(albumWithoutYear, album2013));
        assertEquals(-1, comparator.compare(albumA2012, album2013));

        assertEquals(-1, comparator.compare(albumA2012, albumB2012));
        assertEquals(1, comparator.compare(albumB2012, albumA2012));
    }

    /*
     * Quoted from MediaFileComparatorTestCase for inject configuration.
     * Copyright 2019 (C) tesshu.com
     * Based upon Airsonic, Copyright 2016 (C) Airsonic Authors 
     * Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
     */
    @Test
    public void testCompareDiscNumbers() throws Exception {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        MediaFileComparator comparator = comparators.mediaFileOrder();

        MediaFile discXtrack1 = new MediaFile();
        discXtrack1.setMediaType(MediaFile.MediaType.MUSIC);
        discXtrack1.setPath("a");
        discXtrack1.setTrackNumber(1);

        MediaFile discXtrack2 = new MediaFile();
        discXtrack2.setMediaType(MediaFile.MediaType.MUSIC);
        discXtrack2.setPath("a");
        discXtrack2.setTrackNumber(2);

        MediaFile disc5track1 = new MediaFile();
        disc5track1.setMediaType(MediaFile.MediaType.MUSIC);
        disc5track1.setPath("a");
        disc5track1.setDiscNumber(5);
        disc5track1.setTrackNumber(1);

        MediaFile disc5track2 = new MediaFile();
        disc5track2.setMediaType(MediaFile.MediaType.MUSIC);
        disc5track2.setPath("a");
        disc5track2.setDiscNumber(5);
        disc5track2.setTrackNumber(2);

        MediaFile disc6track1 = new MediaFile();
        disc6track1.setMediaType(MediaFile.MediaType.MUSIC);
        disc6track1.setPath("a");
        disc6track1.setDiscNumber(6);
        disc6track1.setTrackNumber(1);

        MediaFile disc6track2 = new MediaFile();
        disc6track2.setMediaType(MediaFile.MediaType.MUSIC);
        disc6track2.setPath("a");
        disc6track2.setDiscNumber(6);
        disc6track2.setTrackNumber(2);

        assertEquals(0, comparator.compare(discXtrack1, discXtrack1));
        assertEquals(0, comparator.compare(disc5track1, disc5track1));

        assertEquals(-1, comparator.compare(discXtrack1, discXtrack2));
        assertEquals(1, comparator.compare(discXtrack2, discXtrack1));

        assertEquals(-1, comparator.compare(disc5track1, disc5track2));
        assertEquals(1, comparator.compare(disc6track2, disc5track1));

        assertEquals(-1, comparator.compare(disc5track1, disc6track1));
        assertEquals(1, comparator.compare(disc6track1, disc5track1));

        assertEquals(-1, comparator.compare(disc5track2, disc6track1));
        assertEquals(1, comparator.compare(disc6track1, disc5track2));

        assertEquals(-1, comparator.compare(discXtrack1, disc5track1));
        assertEquals(1, comparator.compare(disc5track1, discXtrack1));

        assertEquals(-1, comparator.compare(discXtrack1, disc5track2));
        assertEquals(1, comparator.compare(disc5track2, discXtrack1));
    }
    
    /*
     * Quoted from MediaFileComparatorTestCase for inject configuration.
     * Copyright 2019 (C) tesshu.com
     * Based upon Airsonic, Copyright 2016 (C) Airsonic Authors 
     * Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
     */
    @Test
    public void testIndex() throws Exception {
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> m = musicIndexService
                .getIndexedArtists(musicFoldersToUse, true);
        List<String> artists = m.values().stream().flatMap(files -> files.stream())
                .flatMap(files -> files.getMediaFiles().stream()).map(file -> file.getName())
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
    public void testAlbumDirectoryWithAlphaNum() {
        settingsService.setSortAlphanum(true);
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
    public void testNumAlbumDirectoryWithAlphaNum() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(3));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ARTIST);
        List<MediaFile> files = mediaFileService.getChildrenOf(result.getMediaFiles().get(0), true, true, true);
        List<String> albums = files.stream().map(m -> m.getName()).collect(Collectors.toList());
        assertTrue(validateAlphaNumList(albums));
    }

    @Test
    public void testPlayQueueSortByAlbum() throws IOException {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(1));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(SortOrder.ALBUM, comparators.createCollator());
        List<String> albums = playQueue.getFiles().stream().map(m -> m.getAlbumName()).collect(Collectors.toList());
        assertTrue(validateJPSonicNaturalList(albums));
    }

    @Test
    public void testPlayQueueSortByAlbumWithAlphaNum() throws IOException {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(3));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(SortOrder.ALBUM, comparators.createCollator());
        List<String> albums = playQueue.getFiles().stream().map(m -> m.getAlbumName()).collect(Collectors.toList());
        assertTrue(validateAlphaNumList(albums));
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
        playQueue.sort(SortOrder.ARTIST, comparators.createCollator());
        List<String> artists = playQueue.getFiles().stream().map(m -> m.getArtist()).collect(Collectors.toList());
        assertTrue(validateJPSonicNaturalList(artists));
    }

    @Test
    public void testPlayQueueSortByArtistWithAlphaNum() throws IOException {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(2));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ALBUM");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(SortOrder.ARTIST, comparators.createCollator());
        List<String> artists = playQueue.getFiles().stream().map(m -> m.getArtist()).collect(Collectors.toList());
        assertTrue(validateAlphaNumList(artists));
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
