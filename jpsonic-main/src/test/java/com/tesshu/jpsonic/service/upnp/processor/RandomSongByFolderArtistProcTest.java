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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static com.tesshu.jpsonic.util.PlayerUtils.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

import java.util.Arrays;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.persistence.api.entity.Artist;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.api.repository.ArtistDao;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.processor.composite.FArtistOrSong;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderArtist;
import com.tesshu.jpsonic.service.upnp.processor.composite.FolderOrFArtist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.DIDLContent;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class RandomSongByFolderArtistProcTest {

    @Nested
    class UnitTest {

        private UpnpProcessorUtil util;
        private ArtistDao artistDao;
        private SearchService searchService;
        private SettingsService settingsService;
        private FolderOrArtistLogic folderOrArtistProc;
        private RandomSongByFolderArtistProc proc;

        @BeforeEach
        public void setup() {
            util = mock(UpnpProcessorUtil.class);
            artistDao = mock(ArtistDao.class);
            searchService = mock(SearchService.class);
            settingsService = mock(SettingsService.class);
            UpnpDIDLFactory factory = new UpnpDIDLFactory(settingsService,
                    mock(JWTSecurityService.class), mock(MediaFileService.class),
                    mock(PlayerService.class), mock(TranscodingService.class));
            folderOrArtistProc = new FolderOrArtistLogic(util, factory, artistDao);
            proc = new RandomSongByFolderArtistProc(util, factory, artistDao, searchService,
                    settingsService, folderOrArtistProc);
        }

        @Test
        void testGetProcId() {
            assertEquals("rsbfar", proc.getProcId().getValue());
        }

        @Test
        void testGetChildrenWithArtist() {
            int id = 99;
            Artist artist = new Artist();
            artist.setId(id);
            artist.setName("artist");
            Mockito.when(artistDao.getArtist(id)).thenReturn(artist);
            MusicFolder folder = new MusicFolder(99, "/Music", "Music", true, null, null, false);
            FolderOrFArtist folderOrArtist = new FolderOrFArtist(new FolderArtist(folder, artist));

            assertEquals(0, proc.getChildren(folderOrArtist, 0, 2).size());
            Mockito
                .verify(searchService, Mockito.times(1))
                .getRandomSongsByArtist(any(Artist.class), anyInt(), anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetChildrenWithFolder() {
            MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1, false);
            Mockito
                .when(artistDao.getAlphabetialArtists(anyInt(), anyInt(), anyList()))
                .thenReturn(List.of(new Artist()));
            FolderOrFArtist folderOrArtist = new FolderOrFArtist(folder);
            assertEquals(1, proc.getChildren(folderOrArtist, 0, 2).size());
            Mockito
                .verify(artistDao, Mockito.times(1))
                .getAlphabetialArtists(anyInt(), anyInt(), anyList());
        }

        @Test
        void testAddChild() {
            MusicFolder folder = new MusicFolder(99, "/Music", "Music", true, null, null, false);
            Artist artist = new Artist();
            artist.setId(0);
            artist.setName("artist");
            artist.setAlbumCount(3);
            FArtistOrSong artistOrSong = new FArtistOrSong(new FolderArtist(folder, artist));

            DIDLContent content = new DIDLContent();
            assertEquals(0, content.getContainers().size());
            proc.addChild(content, artistOrSong);
            assertEquals(1, content.getContainers().size());

            content = new DIDLContent();
            MediaFile song = new MediaFile();
            artistOrSong = new FArtistOrSong(song);

            proc = new RandomSongByFolderArtistProc(util, mock(UpnpDIDLFactory.class), artistDao,
                    searchService, settingsService, folderOrArtistProc);
            assertEquals(0, content.getItems().size());
            proc.addChild(content, artistOrSong);
            assertEquals(1, content.getItems().size());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
            .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists",
                    true, now(), 1, false));

        @Autowired
        private RandomSongByFolderArtistProc proc;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            settingsService.setSortAlbumsByYear(false);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetDirectChildren() {
            List<FolderOrFArtist> items = proc.getDirectChildren(0, 10);
            assertEquals(10, items.size());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(31, proc.getDirectChildrenCount());
        }

        @Test
        void testGetChildren() {
            List<FolderOrFArtist> folderOrArtists = proc.getDirectChildren(0, 10);
            assertEquals(10, folderOrArtists.size());
            assertEquals("10", folderOrArtists.get(0).getFolderArtist().artist().getName());
            assertEquals("20", folderOrArtists.get(1).getFolderArtist().artist().getName());
            assertEquals("30", folderOrArtists.get(2).getFolderArtist().artist().getName());

            List<FArtistOrSong> songs = proc
                .getChildren(new FolderOrFArtist(folderOrArtists.get(0).getFolderArtist()), 0,
                        Integer.MAX_VALUE);
            assertEquals(31, songs.size());
        }

        @Test
        void testGetChildSizeOf() {
            List<FolderOrFArtist> artists = proc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            assertEquals(31, proc.getChildSizeOf(artists.get(0)));
        }
    }
}
