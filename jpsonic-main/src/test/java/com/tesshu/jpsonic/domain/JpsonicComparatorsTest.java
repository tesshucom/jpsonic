package com.tesshu.jpsonic.domain;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.annotation.Documented;
import java.util.Collections;
import java.util.List;

import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ALBUM;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.ARTIST;
import static com.tesshu.jpsonic.domain.JpsonicComparators.OrderBy.TRACK;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertAlbumOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertAlphanumArtistOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertArtistOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertGenreOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertMediafileOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertPlaylistOrder;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class JpsonicComparatorsTest {

    @Documented
    private @interface ComparatorsDecisions { // @formatter:off
        @interface Conditions {
            @interface isProhibitSortVarious {} // GlobalOption
            @interface isSortAlbumsByYear {} // GlobalOption
            @interface isSortAlphanum {} // GlobalOption
            @interface Target {
                @interface Artist {}
                @interface Album {}
                @interface MediaFile {
                    @interface mediaType {
                        @interface ARTIST {}
                        @interface ALBUM {}
                        @interface Ignore {
                            @interface FieldOrderBy {
                                @interface Artist {}
                                @interface Album {}
                                @interface Track {}
                            }
                        }
                    }
                    @interface parent {
                        @interface isEmpty {}
                        @interface isVariablePrefix {}
                    }
                }
                @interface Playlist {}
                @interface Genre {
                    @interface isSortByAlbum {}
                }
            }
        }
        @interface Actions {
            @interface artistOrder {}
            @interface albumOrder {}
            @interface albumOrderByAlpha {}
            @interface mediaFileOrder {}
            @interface mediaFileOrderWithParent {}
            @interface mediaFileOrderBy {}
            @interface mediaFileOrderByAlpha {}
            @interface playlistOrder {}
            @interface genreOrder {}
            @interface genreOrderByAlpha {}
        }
    } // @formatter:on

    @ClassRule
    public static final HomeRule classRule = new HomeRule();

    @Autowired
    private JpsonicComparatorsTestUtils testUtils;

    @Autowired
    private JpsonicComparators comparators;

    @Autowired
    private SettingsService settingsService;

    @ComparatorsDecisions.Actions.artistOrder
    @ComparatorsDecisions.Conditions.Target.Artist
    @Test
    public void c00() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Artist> artists = testUtils.createReversedArtists();
        Collections.sort(artists, comparators.artistOrder());
        assertArtistOrder(artists, 14, 15, 16);
        assertEquals("episode 1", artists.get(14).getName());
        assertEquals("episode 19", artists.get(15).getName());
        assertEquals("episode 2", artists.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.Artist
    @ComparatorsDecisions.Actions.artistOrder
    @Test
    public void c01() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Artist> artists = testUtils.createReversedArtists();
        Collections.sort(artists, comparators.artistOrder());
        assertArtistOrder(artists, 14, 15, 16);
        assertEquals("episode 1", artists.get(14).getName());
        assertEquals("episode 2", artists.get(15).getName());
        assertEquals("episode 19", artists.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Album
    @ComparatorsDecisions.Actions.albumOrderByAlpha
    @Test
    public void c02() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrderByAlpha());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 19", albums.get(15).getName());
        assertEquals("episode 2", albums.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.Album
    @ComparatorsDecisions.Actions.albumOrderByAlpha
    @Test
    public void c03() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrderByAlpha());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 2", albums.get(15).getName());
        assertEquals("episode 19", albums.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Artist
    @ComparatorsDecisions.Actions.albumOrder
    @Test
    public void c04() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrder());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 19", albums.get(15).getName());
        assertEquals("episode 2", albums.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.Artist
    @ComparatorsDecisions.Actions.albumOrder
    @Test
    public void c05() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrder());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 2", albums.get(15).getName());
        assertEquals("episode 19", albums.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.Album
    @ComparatorsDecisions.Actions.albumOrder
    @Test
    public void c06() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrder());
        assertEquals("98", albums.get(0).getName());
        assertEquals("99", albums.get(1).getName());
        assertEquals("10", albums.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.Album
    @ComparatorsDecisions.Actions.albumOrder
    @Test
    public void c07() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumOrder());
        assertEquals("98", albums.get(0).getName());
        assertEquals("99", albums.get(1).getName());
        assertEquals("10", albums.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c08() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c09() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c10() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c11() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c12() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c13() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c14() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder());
        assertEquals("98", files.get(0).getName());
        assertEquals("99", files.get(1).getName());
        assertEquals("10", files.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrder
    @Test
    public void c15() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder());
        assertEquals("98", files.get(0).getName());
        assertEquals("99", files.get(1).getName());
        assertEquals("10", files.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c16() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c17() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c18() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c19() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c20() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c21() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c22() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c23() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c24() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c25() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c26() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertEquals("98", files.get(0).getName());
        assertEquals("99", files.get(1).getName());
        assertEquals("10", files.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isEmpty
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c27() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(new MediaFile()));
        assertEquals("98", files.get(0).getName());
        assertEquals("99", files.get(1).getName());
        assertEquals("10", files.get(2).getName());
    }

    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c28() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c29() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c30() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Conditions.Target.MediaFile.parent.isVariablePrefix
    @ComparatorsDecisions.Actions.mediaFileOrderWithParent
    @Test
    public void c31() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(true);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrder(testUtils.createVariousMedifile()));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c32() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c33() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c34() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ARTIST
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c35() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c36() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c37() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c38() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.ALBUM
    @ComparatorsDecisions.Actions.mediaFileOrderByAlpha
    @Test
    public void c39() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileOrderByAlpha());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Artist
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c40() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(ARTIST));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Artist
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c41() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(ARTIST));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Album
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c42() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(ALBUM));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Album
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c43() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(ALBUM));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Track
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c44() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(TRACK));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.MediaFile
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore
    @ComparatorsDecisions.Conditions.Target.MediaFile.mediaType.Ignore.FieldOrderBy.Track
    @ComparatorsDecisions.Actions.mediaFileOrderBy
    public void c45() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediaSongs();
        Collections.sort(files, comparators.mediaFileOrderBy(TRACK));
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Playlist
    @ComparatorsDecisions.Actions.playlistOrder
    @Test
    public void c46() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Playlist> playlists = testUtils.createReversedPlaylists();
        Collections.sort(playlists, comparators.playlistOrder());
        assertPlaylistOrder(playlists, 8, 9, 14, 15, 16);

        // Playlist can not be specified reading with tag (so alphabetical)
        assertEquals("abc亜伊鵜絵尾", playlists.get(8).getName());
        assertEquals("abcいうえおあ", playlists.get(9).getName());

        assertEquals("episode 1", playlists.get(14).getName());
        assertEquals("episode 19", playlists.get(15).getName());
        assertEquals("episode 2", playlists.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.Playlist
    @ComparatorsDecisions.Actions.playlistOrder
    @Test
    public void c47() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Playlist> playlists = testUtils.createReversedPlaylists();
        Collections.sort(playlists, comparators.playlistOrder());
        assertPlaylistOrder(playlists, 8, 9, 14, 15, 16);

        // Playlist can not be specified reading with tag (so alphabetical)
        assertEquals("abc亜伊鵜絵尾", playlists.get(8).getName());
        assertEquals("abcいうえおあ", playlists.get(9).getName());

        assertEquals("episode 1", playlists.get(14).getName());
        assertEquals("episode 2", playlists.get(15).getName());
        assertEquals("episode 19", playlists.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Genre
    @ComparatorsDecisions.Actions.genreOrder
    @Test
    public void c48() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrder(false));

        assertGenreOrder(genres, 8, 9);

        // Genre can not be specified reading with tag (so count)
        assertEquals("abcいうえおあ", genres.get(8).getName());
        assertEquals("abc亜伊鵜絵尾", genres.get(9).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Genre.isSortByAlbum
    @ComparatorsDecisions.Conditions.Target.Genre
    @ComparatorsDecisions.Actions.genreOrder
    @Test
    public void c49() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrder(true));

        assertGenreOrder(genres, 8, 9);
        // Genre can not be specified reading with tag (so count)
        assertEquals("abcいうえおあ", genres.get(8).getName());
        assertEquals("abc亜伊鵜絵尾", genres.get(9).getName());
    }

    @ComparatorsDecisions.Conditions.Target.Genre
    @ComparatorsDecisions.Actions.genreOrderByAlpha
    @Test
    public void c50() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrderByAlpha());

        assertGenreOrder(genres, 8, 9, 14, 15, 16);

        // Genre can not be specified reading with tag (so alphabetical)
        assertEquals("abc亜伊鵜絵尾", genres.get(8).getName());
        assertEquals("abcいうえおあ", genres.get(9).getName());

        assertEquals("episode 1", genres.get(14).getName());
        assertEquals("episode 19", genres.get(15).getName());
        assertEquals("episode 2", genres.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.Target.Genre
    @ComparatorsDecisions.Actions.genreOrderByAlpha
    @Test
    public void c51() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrderByAlpha());
        assertGenreOrder(genres, 8, 9, 14, 15, 16);

        // Genre can not be specified reading with tag (so alphabetical)
        assertEquals("abc亜伊鵜絵尾", genres.get(8).getName());
        assertEquals("abcいうえおあ", genres.get(9).getName());

        assertEquals("episode 1", genres.get(14).getName());
        assertEquals("episode 2", genres.get(15).getName());
        assertEquals("episode 19", genres.get(16).getName());
    }

    /*
     * Full pattern test for serial numbers.
     * Whether serial number processing has been performed can be determined
     * by some elements included in jPSonicNaturalList.
     */
    @Test
    public void testAlphanum() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Artist> artists = testUtils.createReversedAlphanum();
        Collections.sort(artists, comparators.artistOrder());
        assertAlphanumArtistOrder(artists);
    }

    /*
     * Quoted from MediaFileComparatorTestCase
     * Copyright 2019 (C) tesshu.com
     * Based upon Airsonic, Copyright 2016 (C) Airsonic Authors 
     * Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
     */
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
     * Quoted from MediaFileComparatorTestCase
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
}
