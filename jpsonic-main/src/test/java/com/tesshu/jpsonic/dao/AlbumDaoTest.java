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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.dao.dialect.DialectAlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AlbumDaoTest {

    private TemplateWrapper templateWrapper;
    private AlbumDao albumDao;

    @BeforeEach
    public void setup() {
        this.templateWrapper = Mockito.mock(TemplateWrapper.class);
        albumDao = new AlbumDao(templateWrapper, Mockito.mock(DialectAlbumDao.class));
    }

    @Documented
    private @interface AlphabeticalOps {
        @interface Conditions {
            @interface ByArtist {
                @interface True {
                }

                @interface False {
                }
            }

            @interface IgnoreCase {
                @interface True {
                }

                /**
                 * Using this case is deprecated. #2441
                 */
                @interface False {
                }
            }
        }
    }

    @Nested
    class GetAlphabeticalAlbumsTest {

        List<MusicFolder> folders;

        @BeforeEach
        public void setup() {
            MusicFolder folder = new MusicFolder("/Music", "Music", true, null, false);
            folders = List.of(folder);
        }

        private String getJoin(ArgumentCaptor<String> queryCaptor) {
            String query = queryCaptor.getValue();
            return query.replaceAll("\n", "").replaceAll("^.*from\salbum", "").replaceAll("where.*$", "").trim();
        }

        private String getOrderBy(ArgumentCaptor<String> queryCaptor) {
            String query = queryCaptor.getValue();
            return query.replaceAll("\n", "").replaceAll("^.*order\sby", "").replaceAll("limit.*$", "").trim();
        }

        @SuppressWarnings("unchecked")
        private ArgumentCaptor<String> getQuery(boolean byArtist, boolean ignoreCase) {
            ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.when(
                    templateWrapper.namedQuery(queryCaptor.capture(), Mockito.any(RowMapper.class), Mockito.anyMap()))
                    .thenReturn(Collections.emptyList());
            albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, byArtist, ignoreCase, folders);
            return queryCaptor;
        }

        @AlphabeticalOps.Conditions.ByArtist.True
        @AlphabeticalOps.Conditions.IgnoreCase.True
        @Test
        void c00() {
            ArgumentCaptor<String> query = getQuery(true, true);
            assertEquals("left join artist on artist.present and artist.name = album.artist", getJoin(query));
            assertEquals("artist_order, album_order", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.True
        @AlphabeticalOps.Conditions.IgnoreCase.False
        @Test
        void c01() {
            ArgumentCaptor<String> query = getQuery(true, false);
            assertEquals("left join artist on artist.present and artist.name = album.artist", getJoin(query));
            assertEquals("artist.reading, album.name_reading", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.False
        @AlphabeticalOps.Conditions.IgnoreCase.True
        @Test
        void c02() {
            ArgumentCaptor<String> query = getQuery(false, true);
            assertEquals("", getJoin(query));
            assertEquals("album_order", getOrderBy(query));
        }

        @AlphabeticalOps.Conditions.ByArtist.False
        @AlphabeticalOps.Conditions.IgnoreCase.False
        @Test
        void c03() {
            ArgumentCaptor<String> query = getQuery(false, false);
            assertEquals("", getJoin(query));
            assertEquals("album.name_reading", getOrderBy(query));
        }
    }

    /*
     * Make sure that scanning ignoring timestamps will not delete starred albums.
     */
    @Nested
    class StarredPersistenceTest extends AbstractNeedsScan {

        public static final String ADMIN_NAME = "admin";

        @Autowired
        private AlbumDao albumDao;
        @Autowired
        private SettingsService settingsService;

        private final List<MusicFolder> folders = List
                .of(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 0, false));

        @Override
        public List<MusicFolder> getMusicFolders() {
            return folders;
        }

        @BeforeEach
        public void setup() throws IOException {
            populateDatabase();
        }

        @Test
        void testStarredPersistence() throws IOException, InterruptedException {

            // Checking registered data
            assertEquals(4, albumDao.getAlbumCount(folders));
            List<Album> albums = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, true, folders);
            assertEquals(4, albums.size());
            assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", albums.get(0).getName());
            assertEquals("_ID3_ALBUM_ Ravel - Chamber Music With Voice", albums.get(1).getName());
            assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", albums.get(2).getName());
            assertEquals("Complete Piano Works", albums.get(3).getName());

            // Give a Star
            albumDao.starAlbum(albums.get(0).getId(), ADMIN_NAME);
            Thread.sleep(200);
            albumDao.starAlbum(albums.get(2).getId(), ADMIN_NAME);
            List<Album> starred = albumDao.getStarredAlbums(0, Integer.MAX_VALUE, ADMIN_NAME, folders);
            assertEquals(2, starred.size());

            // Note that the order is 'created desc'
            assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", starred.get(0).getName());
            assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", starred.get(1).getName());

            // Run a scan
            TestCaseUtils.execScan(mediaScannerService);

            // Of course the result remains the same
            starred = albumDao.getStarredAlbums(0, Integer.MAX_VALUE, ADMIN_NAME, folders);
            assertEquals(2, starred.size());
            assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", starred.get(0).getName());
            assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", starred.get(1).getName());

            // Scan with IgnoreFileTimestamps enabled
            settingsService.setIgnoreFileTimestamps(true);
            settingsService.save();
            TestCaseUtils.execScan(mediaScannerService);

            // The result remains the same
            starred = albumDao.getStarredAlbums(0, Integer.MAX_VALUE, ADMIN_NAME, folders);
            assertEquals(2, starred.size());
            assertEquals("_ID3_ALBUM_ Sackcloth 'n' Ashes", starred.get(0).getName());
            assertEquals("_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]", starred.get(1).getName());
        }
    }

    /*
     * Even if the timestamp is ignored when scanning, it is confirmed that Created does not change.
     */
    @Nested
    class CreatedPersistenceTest extends AbstractNeedsScan {

        public static final String ADMIN_NAME = "admin";

        @Autowired
        private AlbumDao albumDao;
        @Autowired
        private SettingsService settingsService;

        private final List<MusicFolder> folders = List
                .of(new MusicFolder(1, resolveBaseMediaPath("Music"), "Music", true, now(), 0, false));

        @Override
        public List<MusicFolder> getMusicFolders() {
            return folders;
        }

        @BeforeEach
        public void setup() throws IOException {
            populateDatabase();
        }

        @Test
        void testCreatedPersistence() throws IOException, InterruptedException {

            // Checking registered data
            List<Album> albums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
            assertEquals(4, albums.size());

            // Run a scan
            TestCaseUtils.execScan(mediaScannerService);

            List<Album> scanedAlbums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
            assertEquals(4, scanedAlbums.size());
            assertEquals(albums.get(0).getCreated(), scanedAlbums.get(0).getCreated());
            assertEquals(albums.get(1).getCreated(), scanedAlbums.get(1).getCreated());
            assertEquals(albums.get(2).getCreated(), scanedAlbums.get(2).getCreated());
            assertEquals(albums.get(3).getCreated(), scanedAlbums.get(3).getCreated());

            // Scan with IgnoreFileTimestamps enabled
            settingsService.setIgnoreFileTimestamps(true);
            settingsService.save();
            TestCaseUtils.execScan(mediaScannerService);

            /*
             * Nothing changes. album#created comes from file#changed. (The Newest in ID3 indicates the most recent
             * Change. This has been the case since legacy servers.) Therefore, regardless of the scan logic, this value
             * will not change unless the file is changed.
             */
            List<Album> fullScanedAlbums = albumDao.getNewestAlbums(0, Integer.MAX_VALUE, folders);
            assertEquals(4, fullScanedAlbums.size());
            assertEquals(albums.get(0).getCreated(), fullScanedAlbums.get(0).getCreated());
            assertEquals(albums.get(1).getCreated(), fullScanedAlbums.get(1).getCreated());
            assertEquals(albums.get(2).getCreated(), fullScanedAlbums.get(2).getCreated());
            assertEquals(albums.get(3).getCreated(), fullScanedAlbums.get(3).getCreated());
        }
    }
}
