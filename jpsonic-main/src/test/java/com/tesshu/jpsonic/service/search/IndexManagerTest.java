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

package com.tesshu.jpsonic.service.search;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.RatingDao;
import com.tesshu.jpsonic.dao.TemplateWrapper;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals") // In the testing class, it may be less readable.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexManagerTest extends AbstractNeedsScan {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Autowired
    private IndexManager indexManager;

    @Autowired
    private SearchCriteriaDirector director;

    @Autowired
    private MediaScannerService mediaScannerService;

    @Autowired
    private TemplateWrapper template;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private RatingDao ratingDao;

    private static final String USER_NAME = "admin";

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (ObjectUtils.isEmpty(musicFolders)) {
            musicFolders = Arrays.asList(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 1));
        }
        return musicFolders;
    }

    @BeforeEach
    public void setup() {
        populateDatabaseOnlyOnce(() -> {
            return true;
        }, () -> {

            // #1842 Airsonic does not implement Rating expunge

            List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, true, musicFolders);
            assertEquals(4, albums.size());

            albums.forEach(m -> ratingDao.setRatingForUser(USER_NAME, m, 1));
            assertEquals(4, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders));
            int ratingsCount = template.getJdbcTemplate().queryForObject(
                    "select count(*) from user_rating where user_rating.username = ?", Integer.class, USER_NAME);
            assertEquals(4, ratingsCount, "Because explicitly registered 4 Ratings.");

            // Register a dummy rate (reproduce old path data by moving files)
            MediaFile dummyMediaFile = new MediaFile();
            dummyMediaFile.setPathString("oldPath");
            ratingDao.setRatingForUser(USER_NAME, dummyMediaFile, 1);

            assertEquals(4, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders),
                    "Because the SELECT condition only references real paths.");
            ratingsCount = template.getJdbcTemplate().queryForObject(
                    "select count(*) from user_rating where user_rating.username = ?", Integer.class, USER_NAME);
            assertEquals(5, ratingsCount, "Because counted directly, including non-existent paths.");

            return true;
        });

    }

    @Test
    @Order(1)
    void testExpunge() throws IOException {

        int offset = 0;
        int count = Integer.MAX_VALUE;

        final SearchCriteria criteriaArtist = director.construct("_DIR_ Ravel", offset, count, false, musicFolders,
                IndexType.ARTIST);
        final SearchCriteria criteriaAlbum = director.construct("Complete Piano Works", offset, count, false,
                musicFolders, IndexType.ALBUM);
        final SearchCriteria criteriaSong = director.construct("Gaspard", offset, count, false, musicFolders,
                IndexType.SONG);
        final SearchCriteria criteriaArtistId3 = director.construct("_DIR_ Ravel", offset, count, false, musicFolders,
                IndexType.ARTIST_ID3);
        final SearchCriteria criteriaAlbumId3 = director.construct("Complete Piano Works", offset, count, false,
                musicFolders, IndexType.ALBUM_ID3);

        /* Delete DB record. */

        // artist
        SearchResult result = searchService.search(criteriaArtist);
        assertEquals(1, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel", result.getMediaFiles().get(0).getName());

        List<Integer> candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

        candidates = mediaFileDao.getArtistExpungeCandidates();
        assertEquals(1, candidates.size());

        // album
        result = searchService.search(criteriaAlbum);
        assertEquals(1, result.getMediaFiles().size());
        assertEquals("_DIR_ Ravel - Complete Piano Works", result.getMediaFiles().get(0).getName());

        candidates = mediaFileDao.getAlbumExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

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
            Assertions.fail("Search results are not correct.");
        }

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(0, candidates.size());

        result.getMediaFiles().forEach(a -> mediaFileDao.deleteMediaFile(a.getId()));

        candidates = mediaFileDao.getSongExpungeCandidates();
        assertEquals(2, candidates.size());

        // artistid3
        result = searchService.search(criteriaArtistId3);
        assertEquals(1, result.getArtists().size());
        assertEquals("_DIR_ Ravel", result.getArtists().get(0).getName());

        // albumId3
        result = searchService.search(criteriaAlbumId3);
        assertEquals(1, result.getAlbums().size());
        assertEquals("Complete Piano Works", result.getAlbums().get(0).getName());

        /* Does not scan, only expunges the index. */
        mediaScannerService.expunge();

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

        // See this#setup
        assertEquals(3, ratingDao.getRatedAlbumCount(USER_NAME, musicFolders), "Because one album has been deleted.");
        int ratingsCount = template.getJdbcTemplate().queryForObject(
                "select count(*) from user_rating where user_rating.username = ?", Integer.class, USER_NAME);
        assertEquals(3, ratingsCount, "Will be removed, including oldPath");
    }

    @Test
    @Order(2)
    void testDeleteLegacyFiles() throws ExecutionException, IOException {
        // Remove the index used in the early days of Airsonic(Close to Subsonic)
        Path legacyFile = Path.of(SettingsService.getJpsonicHome().toString(), "lucene2");
        if (Files.createFile(legacyFile) != null) {
            assertTrue(Files.exists(legacyFile));
        } else {
            Assertions.fail();
        }
        Path legacyDir = Path.of(SettingsService.getJpsonicHome().toString(), "lucene3");
        FileUtil.createDirectories(legacyDir);

        indexManager.deleteLegacyFiles();
        assertFalse(Files.exists(legacyFile));
        assertFalse(Files.exists(legacyDir));
    }

    @Test
    @Order(3)
    void testDeleteOldFiles() throws ExecutionException, IOException {
        // If the index version does not match, delete it
        Path oldDir = Path.of(SettingsService.getJpsonicHome().toString(), "index-JP22");
        if (FileUtil.createDirectories(oldDir) != null) {
            assertTrue(Files.exists(oldDir));
        } else {
            Assertions.fail();
        }
        indexManager.deleteOldFiles();
        assertFalse(Files.exists(oldDir));
    }
}
