/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service.search;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.service.SettingsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.util.ObjectUtils.isEmpty;

public class IndexManagerTest extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexManager indexManager;
    
    @Autowired
    private SearchCriteriaDirector director;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private ArtistDao artistDao;

    @Autowired
    private AlbumDao albumDao;

    @Autowired
    private SettingsService settingsService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    ResourceLoader resourceLoader;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir = new File(resolveBaseMediaPath.apply("Music"));
            musicFolders.add(new MusicFolder(1, musicDir, "Music", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        settingsService.setSearchMethodLegacy(false);
        populateDatabaseOnlyOnce();
    }

    @Test
    public void testExpunge() throws IOException {

        int offset = 0;
        int count = Integer.MAX_VALUE;
        boolean includeComposer = false;

        SearchCriteria criteriaArtist = director.construct("_DIR_ Ravel", offset, count, includeComposer, musicFolders, IndexType.ARTIST);
        SearchCriteria criteriaAlbum = director.construct("Complete Piano Works", offset, count, includeComposer, musicFolders, IndexType.ALBUM);
        SearchCriteria criteriaSong = director.construct("Gaspard", offset, count, includeComposer, musicFolders, IndexType.SONG);
        SearchCriteria criteriaArtistId3 = director.construct("_DIR_ Ravel", offset, count, includeComposer, musicFolders, IndexType.ARTIST_ID3);
        SearchCriteria criteriaAlbumId3 = director.construct("Complete Piano Works", offset, count, includeComposer, musicFolders, IndexType.ALBUM_ID3);

        /* Delete DB record. */

        // artist
        SearchResult result = searchService.search(criteriaArtist);
        assertEquals(1, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel", result.getMediaFiles().get(0).getName());

        List<Integer> candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(1, candidates.size());

        // album
        result = searchService.search(criteriaAlbum);
        assertEquals(1, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel - Complete Piano Works", result.getMediaFiles().get(0).getName());

        candidates = mediaFileDao.getAlbumExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getAlbumExpungeCandidates();
        assertEquals(1, candidates.size());

        // song
        result = searchService.search(criteriaSong);
        assertEquals(2, result.getMediaFiles().size());
        if ("01 - Gaspard de la Nuit - i. Ondine".equals(result.getMediaFiles().get(0).getName())) {
            assertEquals("02 - Gaspard de la Nuit - ii. Le Gibet", result.getMediaFiles().get(1).getName());
        } else if ("02 - Gaspard de la Nuit - ii. Le Gibet".equals(result.getMediaFiles().get(0).getName())) {
            assertEquals("01 - Gaspard de la Nuit - i. Ondine", result.getMediaFiles().get(1).getName());
        } else {
            fail("Search results are not correct.");
        }

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getPath()));

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(2, candidates.size());

        // artistid3
        result = searchService.search(criteriaArtistId3);
        assertEquals(1, result.getArtists().size());
        assertEquals("_DIR_ Ravel", result.getArtists().get(0).getName());

        candidates = artistDao.getExpungeCandidates();
        assertEquals(0, candidates.size());

        artistDao.markNonPresent(new Date());

        candidates = artistDao.getExpungeCandidates();
        assertEquals(4, candidates.size());

        // albumId3
        result = searchService.search(criteriaAlbumId3);
        assertEquals(1, result.getAlbums().size());
        assertEquals("Complete Piano Works", result.getAlbums().get(0).getName());

        candidates = albumDao.getExpungeCandidates();
        assertEquals(0, candidates.size());

        albumDao.markNonPresent(new Date());

        candidates = albumDao.getExpungeCandidates();
        assertEquals(4, candidates.size());

        /* Does not scan, only expunges the index. */
        indexManager.startIndexing();
        indexManager.expunge();
        indexManager.stopIndexing(indexManager.getStatistics());

        /*
         * Subsequent search results. Results can also be confirmed with Luke.
         */

        result = searchService.search(criteriaArtist);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteriaAlbum);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteriaSong);
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(criteriaArtistId3);
        assertEquals(0, result.getArtists().size());

        result = searchService.search(criteriaAlbumId3);
        assertEquals(0, result.getAlbums().size());

    }

}