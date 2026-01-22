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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.AlbumDao;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Scope;
import com.tesshu.jpsonic.service.search.GenreMasterCriteria.Sort;
import com.tesshu.jpsonic.service.search.SearchServiceUtilities.LegacyGenreCriteria;
import com.tesshu.jpsonic.spring.EhcacheConfiguration.RandomCacheKey;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class SearchServiceUtilitiesTest {

    private ArtistDao artistDao;
    private AlbumDao albumDao;
    private Ehcache genreCache;
    private Ehcache randomCache;
    private MediaFileService mediaFileService;
    private SearchServiceUtilities utilities;

    @BeforeEach
    void setUp() {
        artistDao = mock(ArtistDao.class);
        albumDao = mock(AlbumDao.class);
        CacheManager cacheManager = CacheManager.create();
        genreCache = cacheManager.getCache("genreCache");
        genreCache.removeAll();
        randomCache = cacheManager.getCache("randomCache");
        randomCache.removeAll();
        mediaFileService = mock(MediaFileService.class);
        utilities = new SearchServiceUtilities(artistDao, albumDao, genreCache, randomCache,
                mediaFileService);
    }

    @Test
    void testPostConstruct() {
        SearchServiceUtilities utilities = new SearchServiceUtilities(artistDao, albumDao,
                genreCache, randomCache, mediaFileService);
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> utilities.nextInt(50));
        utilities.postConstruct();
        assertNotNull(utilities.nextInt(50));
    }

    @Test
    void testAddMediaFileIfAnyMatch() {
        List<MediaFile> dist = new ArrayList<>();
        Integer id = 1;
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);

        // Does not exist in DB/not added
        utilities.addMediaFileIfAnyMatch(dist, id);
        assertEquals(0, dist.size());

        // Exists in DB/Not added
        when(mediaFileService.getMediaFile(id)).thenReturn(mediaFile);
        utilities.addMediaFileIfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Exists in DB/Added
        when(mediaFileService.getMediaFile(id)).thenReturn(mediaFile);
        utilities.addMediaFileIfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Objects that exist in DB/have different IDs have been already added
        MediaFile secondMediaFile = new MediaFile();
        Integer secondId = 99;
        secondMediaFile.setId(secondId);
        when(mediaFileService.getMediaFile(secondId)).thenReturn(secondMediaFile);
        utilities.addMediaFileIfAnyMatch(dist, secondId);
        assertEquals(2, dist.size());
    }

    @Test
    void testAddArtistId3IfAnyMatch() {
        List<Artist> dist = new ArrayList<>();
        Integer id = 1;
        Artist artist = new Artist();
        artist.setId(id);

        // Does not exist in DB/not added
        utilities.addArtistId3IfAnyMatch(dist, id);
        assertEquals(0, dist.size());

        // Exists in DB/Not added
        when(artistDao.getArtist(id)).thenReturn(artist);
        utilities.addArtistId3IfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Exists in DB/Added
        when(artistDao.getArtist(id)).thenReturn(artist);
        utilities.addArtistId3IfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Objects that exist in DB/have different IDs have been already added
        Artist secondArtist = new Artist();
        Integer secondId = 99;
        secondArtist.setId(secondId);
        when(artistDao.getArtist(secondId)).thenReturn(secondArtist);
        utilities.addArtistId3IfAnyMatch(dist, secondId);
        assertEquals(2, dist.size());
    }

    @Test
    void testAddAlbumId3IfAnyMatch() {
        List<Album> dist = new ArrayList<>();
        Integer id = 1;
        Album album = new Album();
        album.setId(id);

        // Does not exist in DB/not added
        utilities.addAlbumId3IfAnyMatch(dist, id);
        assertEquals(0, dist.size());

        // Exists in DB/Not added
        when(albumDao.getAlbum(id)).thenReturn(album);
        utilities.addAlbumId3IfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Exists in DB/Added
        when(albumDao.getAlbum(id)).thenReturn(album);
        utilities.addAlbumId3IfAnyMatch(dist, id);
        assertEquals(1, dist.size());

        // Objects that exist in DB/have different IDs have been already added
        Album secondAlbum = new Album();
        Integer secondId = 99;
        secondAlbum.setId(secondId);
        when(albumDao.getAlbum(secondId)).thenReturn(secondAlbum);
        utilities.addAlbumId3IfAnyMatch(dist, secondId);
        assertEquals(2, dist.size());
    }

    @Test
    void testAddIgnoreNullCollectionObject() {
        List<MediaFile> dist = new ArrayList<>();
        utilities.addIgnoreNull(dist, null);
        assertEquals(0, dist.size());
        utilities.addIgnoreNull(dist, new MediaFile());
        assertEquals(1, dist.size());
    }

    @Test
    void testAddIgnoreNullCollectionOfQIndexTypeInt() {
        List<MediaFile> dist = new ArrayList<>();
        Integer id = 99;
        utilities.addMediaFileIfAnyMatch(dist, id);
        assertEquals(0, dist.size());

        List<Artist> distArtist = new ArrayList<>();
        utilities.addArtistId3IfAnyMatch(distArtist, id);
        assertEquals(0, distArtist.size());

        List<Album> distAlbum = new ArrayList<>();
        utilities.addAlbumId3IfAnyMatch(distAlbum, id);
        assertEquals(0, distAlbum.size());

        id = 1;
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);
        when(mediaFileService.getMediaFile(id)).thenReturn(mediaFile);
        utilities.addMediaFileIfAnyMatch(dist, id);
        assertEquals(1, dist.size());
    }

    @Test
    void testAddIgnoreNullParamSearchResultOfTIndexTypeIntClassOfT() {
        Integer id = 99;
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);

        ParamSearchResult<MediaFile> dist = new ParamSearchResult<>();
        utilities.addEntityIfPresent(dist, IndexType.SONG, id, MediaFile.class);
        assertEquals(0, dist.getItems().size());

        when(mediaFileService.getMediaFile(id)).thenReturn(mediaFile);
        utilities.addEntityIfPresent(dist, IndexType.SONG, id, MediaFile.class);
        assertEquals(1, dist.getItems().size());
    }

    @Test
    void testAddIfAnyMatch() {

        Integer songId = 1;
        MediaFile song = new MediaFile();
        song.setId(songId);
        Integer artistId = 2;
        MediaFile artist = new MediaFile();
        artist.setId(artistId);
        Integer albumId = 3;
        MediaFile album = new MediaFile();
        album.setId(albumId);
        Integer artistId3Id = 4;
        Artist artistId3 = new Artist();
        artistId3.setId(artistId3Id);
        Integer albumId3Id = 5;
        Album albumId3 = new Album();
        albumId3.setId(albumId3Id);

        when(mediaFileService.getMediaFile(songId)).thenReturn(song);
        when(mediaFileService.getMediaFile(artistId)).thenReturn(artist);
        when(mediaFileService.getMediaFile(albumId)).thenReturn(album);
        when(artistDao.getArtist(artistId3Id)).thenReturn(artistId3);
        when(albumDao.getAlbum(albumId3Id)).thenReturn(albumId3);

        SearchResult dist = new SearchResult();
        Document subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(songId.toString());
        utilities.addIfAnyMatch(dist, IndexType.SONG, subject);
        assertEquals(1, dist.getMediaFiles().size());
        assertEquals(songId, dist.getMediaFiles().get(0).getId());

        dist = new SearchResult();
        subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(artistId.toString());
        utilities.addIfAnyMatch(dist, IndexType.ARTIST, subject);
        assertEquals(1, dist.getMediaFiles().size());
        assertEquals(artistId, dist.getMediaFiles().get(0).getId());

        dist = new SearchResult();
        subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(albumId.toString());
        utilities.addIfAnyMatch(dist, IndexType.ALBUM, subject);
        assertEquals(1, dist.getMediaFiles().size());
        assertEquals(albumId, dist.getMediaFiles().get(0).getId());

        dist = new SearchResult();
        subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(artistId3Id.toString());
        utilities.addIfAnyMatch(dist, IndexType.ARTIST_ID3, subject);
        assertEquals(1, dist.getArtists().size());
        assertEquals(artistId3Id, dist.getArtists().get(0).getId());

        dist = new SearchResult();
        subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(albumId3Id.toString());
        utilities.addIfAnyMatch(dist, IndexType.ALBUM_ID3, subject);
        assertEquals(1, dist.getAlbums().size());
        assertEquals(albumId3Id, dist.getAlbums().get(0).getId());

        dist = new SearchResult();
        subject = mock(Document.class);
        when(subject.get(FieldNamesConstants.ID)).thenReturn(albumId3Id.toString());
        utilities.addIfAnyMatch(dist, IndexType.GENRE, subject);
        assertEquals(0, dist.getMediaFiles().size());
        assertEquals(0, dist.getArtists().size());
        assertEquals(0, dist.getAlbums().size());

    }

    @Test
    void testGetCacheRandomCacheKeyIntListOfMusicFolderStringArray() {
        String cacheKey = utilities
            .createCacheKey(RandomCacheKey.SONG, 10, Collections.emptyList());
        assertEquals("SONG,10[]", cacheKey);

        cacheKey = utilities
            .createCacheKey(RandomCacheKey.SONG, 10, Collections.emptyList(), "Additional1",
                    "Additional2");
        assertEquals("SONG,10[Additional1,Additional2]", cacheKey);

        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        cacheKey = utilities.createCacheKey(RandomCacheKey.SONG, 10, folders);
        assertEquals("SONG,10[99]", cacheKey);

        cacheKey = utilities
            .createCacheKey(RandomCacheKey.SONG, 10, folders, "Additional1", "Additional2");
        assertEquals("SONG,10[99,Additional1,Additional2]", cacheKey);
    }

    @Test
    void testGetCacheGenreMasterCriteria() {
        GenreMasterCriteria criteria = new GenreMasterCriteria(Collections.emptyList(), Scope.ALBUM,
                Sort.FREQUENCY);
        String cacheKey = utilities.createCacheKey(criteria);
        assertEquals("ALBUM,FREQUENCY,[],[]", cacheKey);

        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        criteria = new GenreMasterCriteria(folders, Scope.ALBUM, Sort.FREQUENCY);
        cacheKey = utilities.createCacheKey(criteria);
        assertEquals("ALBUM,FREQUENCY,[99],[]", cacheKey);
    }

    @Test
    void testLegacyGenreCache() {
        assertFalse(utilities.containsCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL));
        assertTrue(utilities.getCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL).isEmpty());
        utilities
            .putCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL, List.of(new Genre("genre", 0, 0)));
        assertTrue(utilities.containsCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL));
        assertFalse(utilities.getCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL).isEmpty());
        utilities.removeCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL);
        assertFalse(utilities.containsCache(LegacyGenreCriteria.ALBUM_ALPHABETICAL));
    }

    @Test
    void testGenreCache() {
        GenreMasterCriteria criteria = new GenreMasterCriteria(Collections.emptyList(), Scope.ALBUM,
                Sort.FREQUENCY);
        assertTrue(utilities.getCache(criteria).isEmpty());
        utilities.putCache(criteria, List.of(new Genre("genre", 0, 0)));
        assertFalse(utilities.getCache(criteria).isEmpty());
    }

    @Test
    void testRandomCache() {
        int casheMax = 30;
        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        assertTrue(utilities.getCache(RandomCacheKey.SONG, casheMax, folders).isEmpty());
        List<MediaFile> songs = List.of(new MediaFile());
        utilities.putCache(RandomCacheKey.SONG, casheMax, folders, songs);
        assertFalse(utilities.getCache(RandomCacheKey.SONG, casheMax, folders).isEmpty());
    }

    @Test
    void testRandomCacheAdditional() {
        int casheMax = 30;
        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        assertTrue(
                utilities.getCache(RandomCacheKey.SONG, casheMax, folders, "additional").isEmpty());
        List<MediaFile> songs = List.of(new MediaFile());
        utilities.putCache(RandomCacheKey.SONG, casheMax, folders, songs, "additional");
        assertFalse(
                utilities.getCache(RandomCacheKey.SONG, casheMax, folders, "additional").isEmpty());
    }

    @Test
    void testRandomIdsCache() {
        int casheMax = 30;
        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        List<Integer> ids = List.of(1, 2, 3);
        assertTrue(utilities.getCache(RandomCacheKey.ALBUM, casheMax, folders).isEmpty());
        utilities.putCache(RandomCacheKey.ALBUM, casheMax, folders, ids);
        assertFalse(utilities.getCache(RandomCacheKey.ALBUM, casheMax, folders).isEmpty());
    }

    @Test
    void testRemoveCacheAll() {
        GenreMasterCriteria criteria = new GenreMasterCriteria(Collections.emptyList(), Scope.ALBUM,
                Sort.FREQUENCY);
        assertTrue(utilities.getCache(criteria).isEmpty());
        utilities.putCache(criteria, List.of(new Genre("genre", 0, 0)));
        assertFalse(utilities.getCache(criteria).isEmpty());

        int casheMax = 30;
        MusicFolder folder = new MusicFolder(99, "path", "name", true, Instant.now(), 0, false);
        List<MusicFolder> folders = List.of(folder);
        assertTrue(utilities.getCache(RandomCacheKey.SONG, casheMax, folders).isEmpty());
        List<MediaFile> songs = List.of(new MediaFile());
        utilities.putCache(RandomCacheKey.SONG, casheMax, folders, songs);
        assertFalse(utilities.getCache(RandomCacheKey.SONG, casheMax, folders).isEmpty());

        utilities.removeCacheAll();
        assertTrue(utilities.getCache(criteria).isEmpty());
        assertTrue(utilities.getCache(RandomCacheKey.SONG, casheMax, folders).isEmpty());
    }
}
