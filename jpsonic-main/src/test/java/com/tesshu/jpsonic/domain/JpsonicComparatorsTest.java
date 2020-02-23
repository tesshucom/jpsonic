package com.tesshu.jpsonic.domain;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
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

import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertAlbumOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertArtistOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertGenreOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertMediafileOrder;
import static com.tesshu.jpsonic.domain.JpsonicComparatorsTestUtils.assertPlaylistOrder;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class JpsonicComparatorsTest {

    @Autowired
    private JpsonicComparatorsTestUtils testUtils;

    @Documented
    private @interface ComparatorsDecisions { // @formatter:off
        @interface Conditions {
            @interface isProhibitSortVarious {}
            @interface isSortAlbumsByYear {}
            @interface isSortAlphanum {}
            @interface Parent {
                @interface isEmpty {}
                @interface isVariablePrefix {}
            }
            @interface Mediatype {
                @interface ARTIST {}
                @interface ALBUM {}
            }
            @interface GenreOrder {
                @interface isSortByAlbum {}
            }
        }
        @interface Actions {
            @interface artistOrder {}
            @interface albumAlphabeticalOrder {}
            @interface albumOrder {}
            @interface mediaFileOrder {}
            @interface mediaFileOrderWithParent {}
            @interface mediaFileAlphabeticalOrder {}
            @interface playlistOrder {}
            @interface genreOrder {}
            @interface genreAlphabeticalOrder {}
        }
    } // @formatter:on

    @ClassRule
    public static final HomeRule classRule = new HomeRule();

    @Autowired
    private JpsonicComparators comparators;

    @Autowired
    private SettingsService settingsService;

    @ComparatorsDecisions.Actions.artistOrder
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

    @ComparatorsDecisions.Actions.albumAlphabeticalOrder
    @Test
    public void c02() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumAlphabeticalOrder());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 19", albums.get(15).getName());
        assertEquals("episode 2", albums.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Actions.albumAlphabeticalOrder
    @Test
    public void c03() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Album> albums = testUtils.createReversedAlbums();
        Collections.sort(albums, comparators.albumAlphabeticalOrder());
        assertAlbumOrder(albums, 14, 15, 16);
        assertEquals("episode 1", albums.get(14).getName());
        assertEquals("episode 2", albums.get(15).getName());
        assertEquals("episode 19", albums.get(16).getName());
    }

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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlphanum
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
    @ComparatorsDecisions.Conditions.isSortAlphanum
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
    @ComparatorsDecisions.Conditions.isSortAlphanum
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isEmpty
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
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

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Conditions.isProhibitSortVarious
    @ComparatorsDecisions.Conditions.Parent.isVariablePrefix
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

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c32() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c33() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c34() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ARTIST
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c35() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediArtists();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c36() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c37() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c38() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 19", files.get(15).getName());
        assertEquals("episode 2", files.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.Mediatype.ALBUM
    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Conditions.isSortAlbumsByYear
    @ComparatorsDecisions.Actions.mediaFileAlphabeticalOrder
    @Test
    public void c39() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(true);
        settingsService.setProhibitSortVarious(false);
        List<MediaFile> files = testUtils.createReversedMediAlbums();
        Collections.sort(files, comparators.mediaFileAlphabeticalOrder());
        assertMediafileOrder(files, 14, 15, 16);
        assertEquals("episode 1", files.get(14).getName());
        assertEquals("episode 2", files.get(15).getName());
        assertEquals("episode 19", files.get(16).getName());
    }

    @ComparatorsDecisions.Actions.playlistOrder
    @Test
    public void c40() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Playlist> playlists = testUtils.createReversedPlaylists();
        Collections.sort(playlists, comparators.playlistOrder());
        assertPlaylistOrder(playlists, 14, 15, 16);
        assertEquals("episode 1", playlists.get(14).getName());
        assertEquals("episode 19", playlists.get(15).getName());
        assertEquals("episode 2", playlists.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Actions.playlistOrder
    @Test
    public void c41() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Playlist> playlists = testUtils.createReversedPlaylists();
        Collections.sort(playlists, comparators.playlistOrder());
        assertPlaylistOrder(playlists, 14, 15, 16);
        assertEquals("episode 1", playlists.get(14).getName());
        assertEquals("episode 2", playlists.get(15).getName());
        assertEquals("episode 19", playlists.get(16).getName());
    }

    @ComparatorsDecisions.Actions.genreOrder
    @Test
    public void c42() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrder(false));
        assertGenreOrder(genres);
    }

    @ComparatorsDecisions.Conditions.GenreOrder.isSortByAlbum
    @ComparatorsDecisions.Actions.genreOrder
    @Test
    public void c43() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreOrder(true));
        assertGenreOrder(genres);
    }

    @ComparatorsDecisions.Actions.genreAlphabeticalOrder
    @Test
    public void c44() {
        settingsService.setSortAlphanum(false);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreAlphabeticalOrder());
        assertGenreOrder(genres, 14, 15, 16);
        assertEquals("episode 1", genres.get(14).getName());
        assertEquals("episode 19", genres.get(15).getName());
        assertEquals("episode 2", genres.get(16).getName());
    }

    @ComparatorsDecisions.Conditions.isSortAlphanum
    @ComparatorsDecisions.Actions.genreAlphabeticalOrder
    @Test
    public void c45() {
        settingsService.setSortAlphanum(true);
        settingsService.setSortAlbumsByYear(false);
        settingsService.setProhibitSortVarious(false);
        List<Genre> genres = testUtils.createReversedGenres();
        Collections.sort(genres, comparators.genreAlphabeticalOrder());
        assertGenreOrder(genres, 14, 15, 16);
        assertEquals("episode 1", genres.get(14).getName());
        assertEquals("episode 2", genres.get(15).getName());
        assertEquals("episode 19", genres.get(16).getName());
    }

}
