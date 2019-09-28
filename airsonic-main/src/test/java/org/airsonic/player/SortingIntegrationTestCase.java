package org.airsonic.player;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicIndex;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.PlayQueue.SortOrder;
import org.airsonic.player.domain.SearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MusicIndexService;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.airsonic.player.service.search.IndexType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Test to correct sort inconsistencies.
 */
public class SortingIntegrationTestCase extends AbstractAirsonicHomeTest {

    private static List<MusicFolder> musicFolders;

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

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @Before
    public void setup() throws Exception {
        populateDatabaseOnlyOnce();
    }

    private void assertAirsonicNaturalList(List<String> l) {
        assertEquals("abcde", l.get(0));
        assertEquals("DEABC", l.get(1));
        assertEquals("eabcd", l.get(2));
        assertEquals("ĆḊÉÁḂ", l.get(3));
        assertEquals("αβγ", l.get(4));
        assertEquals("♂くんつ", l.get(5));
        assertEquals("いうえおあ", l.get(6));
        assertEquals("ぉぁぃぅぇ", l.get(7));
        assertEquals("はるなつあきふゆ", l.get(8));
        assertEquals("ゥェォァィ", l.get(9));
        assertEquals("オアイウエ", l.get(10));
        assertEquals("パラレル", l.get(11));
        assertEquals("亜伊鵜絵尾", l.get(12));
        assertEquals("張り切る", l.get(13));
        assertEquals("春夏秋冬", l.get(14));
        assertEquals("貼られる", l.get(15));
        assertEquals("馬力", l.get(16));
        assertEquals("ＢＣＤＥＡ", l.get(17));
        assertEquals("ｪｫｧｨｩ", l.get(18));
        assertEquals("ｴｵｱｲｳ", l.get(19));
    }

    /*
     * This is probably a bug.
     * https://github.com/airsonic/airsonic/issues/620
     */
    private void assertPlayQueueList(List<String> l) {
        assertEquals("DEABC", l.get(0));
        assertEquals("abcde", l.get(1));
        assertEquals("eabcd", l.get(2));
        assertEquals("ĆḊÉÁḂ", l.get(3));
        assertEquals("αβγ", l.get(4));
        assertEquals("♂くんつ", l.get(5));
        assertEquals("いうえおあ", l.get(6));
        assertEquals("ぉぁぃぅぇ", l.get(7));
        assertEquals("はるなつあきふゆ", l.get(8));
        assertEquals("ゥェォァィ", l.get(9));
        assertEquals("オアイウエ", l.get(10));
        assertEquals("パラレル", l.get(11));
        assertEquals("亜伊鵜絵尾", l.get(12));
        assertEquals("張り切る", l.get(13));
        assertEquals("春夏秋冬", l.get(14));
        assertEquals("貼られる", l.get(15));
        assertEquals("馬力", l.get(16));
        assertEquals("ＢＣＤＥＡ", l.get(17));
        assertEquals("ｪｫｧｨｩ", l.get(18));
        assertEquals("ｴｵｱｲｳ", l.get(19));
    }

    @SuppressWarnings("unused")
    private void assertAlphaNumList(List<String> l) {
        assertEquals("09X Radonius", l.get(0));
        assertEquals("10X Radonius", l.get(1));
        assertEquals("20X Radonius", l.get(2));
        assertEquals("20X Radonius Prime", l.get(3));
        assertEquals("30X Radonius", l.get(4));
        assertEquals("40X Radonius", l.get(5));
        assertEquals("200X Radonius", l.get(6));
        assertEquals("1000X Radonius Maximus", l.get(7));
        assertEquals("Allegia 6R Clasteron", l.get(8));
        assertEquals("Allegia 50B Clasteron", l.get(9));
        assertEquals("Allegia 50 Clasteron", l.get(10));
        assertEquals("Allegia 51 Clasteron", l.get(11));
        assertEquals("Allegia 500 Clasteron", l.get(12));
        assertEquals("Alpha 2", l.get(13));
        assertEquals("Alpha 2A", l.get(14));
        assertEquals("Alpha 2A-900", l.get(15));
        assertEquals("Alpha 2A-8000", l.get(16));
        assertEquals("Alpha 100", l.get(17));
        assertEquals("Alpha 200", l.get(18));
        assertEquals("Callisto Morphamax", l.get(19));
        assertEquals("Callisto Morphamax 500", l.get(20));
        assertEquals("Callisto Morphamax 600", l.get(21));
        assertEquals("Callisto Morphamax 700", l.get(22));
        assertEquals("Callisto Morphamax 5000", l.get(23));
        assertEquals("Callisto Morphamax 6000 SE", l.get(24));
        assertEquals("Callisto Morphamax 6000 SE2", l.get(25));
        assertEquals("Callisto Morphamax 7000", l.get(26));
        assertEquals("Xiph Xlater 5", l.get(27));
        assertEquals("Xiph Xlater 40", l.get(28));
        assertEquals("Xiph Xlater 50", l.get(29));
        assertEquals("Xiph Xlater 58", l.get(30));
        assertEquals("Xiph Xlater 300", l.get(31));
        assertEquals("Xiph Xlater 500", l.get(32));
        assertEquals("Xiph Xlater 2000", l.get(33));
        assertEquals("Xiph Xlater 5000", l.get(34));
        assertEquals("Xiph Xlater 10000", l.get(35));
    }

    @Test
    public void testIndex() throws Exception {
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SortedMap<MusicIndex, List<MusicIndex.SortableArtistWithMediaFiles>> m = musicIndexService
                .getIndexedArtists(musicFoldersToUse, true);
        List<String> artists = m.values().stream().flatMap(files -> files.stream())
                .flatMap(files -> files.getMediaFiles().stream()).map(file -> file.getName())
                .collect(Collectors.toList());
        assertAirsonicNaturalList(artists);
    }

    @Test
    public void testAlbumDirectory() {
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(1));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ARTIST);
        List<MediaFile> files = mediaFileService.getChildrenOf(result.getMediaFiles().get(0), true, true, true);
        List<String> albums = files.stream().map(m -> m.getName()).collect(Collectors.toList());
        assertAirsonicNaturalList(albums);
    }

    public void testAlbumDirectoryWithAlphaNum() {
        // to be none
    }

    public void testNumAlbumDirectoryWithAlphaNum() {
        // to be none
    }

    @Test
    public void testPlayQueueSortByAlbum() throws IOException {
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(1));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ARTIST");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(SortOrder.ALBUM);
        List<String> albums = playQueue.getFiles().stream().map(m -> m.getAlbumName()).collect(Collectors.toList());
        albums.forEach(a -> System.out.println(a));
        assertPlayQueueList(albums);
    }

    public void testPlayQueueSortByAlbumWithAlphaNum() throws IOException {
        // to be none
    }

    @Test
    public void testPlayQueueSortByArtist() throws IOException {
        settingsService.setSortAlbumsByYear(false);
        List<MusicFolder> musicFoldersToUse = Arrays.asList(musicFolders.get(0));
        SearchCriteria criteria = new SearchCriteria();
        criteria.setQuery("ALBUM");
        criteria.setCount(Integer.MAX_VALUE);
        SearchResult result = searchService.search(criteria, musicFoldersToUse, IndexType.ALBUM);
        PlayQueue playQueue = new PlayQueue();
        playQueue.addFiles(true, result.getMediaFiles());
        playQueue.shuffle();
        playQueue.sort(SortOrder.ARTIST);
        List<String> artists = playQueue.getFiles().stream().map(m -> m.getArtist()).collect(Collectors.toList());
        assertPlayQueueList(artists);
    }

    public void testPlayQueueSortByArtistWithAlphaNum() throws IOException {
        // to be none
    }

    /*
     * DB dependent. Sort rules vary depending on the DB.
     */
    public void testAlbumAllOfHome() {
        // pending!
    }

}
