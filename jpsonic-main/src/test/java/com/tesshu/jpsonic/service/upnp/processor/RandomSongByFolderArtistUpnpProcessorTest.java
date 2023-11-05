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
import com.tesshu.jpsonic.dao.ArtistDao;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.composite.ArtistOrSong;
import com.tesshu.jpsonic.service.upnp.composite.FolderOrArtist;
import org.fourthline.cling.support.model.DIDLContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class RandomSongByFolderArtistUpnpProcessorTest {

    @Nested
    class UnitTest {

        private UpnpProcessorUtil util;
        private UpnpDIDLFactory factory;
        private ArtistDao artistDao;
        private MusicFolderDao musicFolderDao;
        private SearchService searchService;
        private SettingsService settingsService;
        private FolderOrArtistLogic folderOrArtistProc;
        private RandomSongByFolderArtistUpnpProcessor proc;

        @BeforeEach
        public void setup() {
            util = mock(UpnpProcessorUtil.class);
            factory = new UpnpDIDLFactory(settingsService, mock(JWTSecurityService.class), mock(MediaFileService.class),
                    mock(PlayerService.class), mock(TranscodingService.class));
            artistDao = mock(ArtistDao.class);
            musicFolderDao = mock(MusicFolderDao.class);
            searchService = mock(SearchService.class);
            settingsService = mock(SettingsService.class);
            folderOrArtistProc = new FolderOrArtistLogic(util, factory, musicFolderDao, artistDao);
            proc = new RandomSongByFolderArtistUpnpProcessor(util, factory, artistDao, searchService, settingsService,
                    folderOrArtistProc);
        }

        @Test
        void testGetProcId() {
            assertEquals("randomSongByFolderArtist", proc.getProcId().getValue());
        }

        @Test
        void testGetChildrenWithArtist() {
            int id = 99;
            Artist artist = new Artist();
            artist.setId(id);
            artist.setName("artist");
            Mockito.when(artistDao.getArtist(id)).thenReturn(artist);
            FolderOrArtist folderOrArtist = new FolderOrArtist(artist);

            assertEquals(0, proc.getChildren(folderOrArtist, 0, 2).size());
            Mockito.verify(searchService, Mockito.times(1)).getRandomSongsByArtist(any(Artist.class), anyInt(),
                    anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetChildrenWithFolder() {
            MusicFolder folder = new MusicFolder(0, "/folder1", "folder1", true, now(), 1);
            Mockito.when(artistDao.getAlphabetialArtists(anyInt(), anyInt(), anyList()))
                    .thenReturn(List.of(new Artist()));
            FolderOrArtist folderOrArtist = new FolderOrArtist(folder);
            assertEquals(1, proc.getChildren(folderOrArtist, 0, 2).size());
            Mockito.verify(artistDao, Mockito.times(1)).getAlphabetialArtists(anyInt(), anyInt(), anyList());
        }

        @Test
        void testAddChild() {
            Artist artist = new Artist();
            artist.setId(0);
            artist.setName("artist");
            artist.setAlbumCount(3);
            ArtistOrSong artistOrSong = new ArtistOrSong(artist);

            DIDLContent content = new DIDLContent();
            assertEquals(0, content.getContainers().size());
            proc.addChild(content, artistOrSong);
            assertEquals(1, content.getContainers().size());

            content = new DIDLContent();
            MediaFile song = new MediaFile();
            artistOrSong = new ArtistOrSong(song);

            proc = new RandomSongByFolderArtistUpnpProcessor(util, mock(UpnpDIDLFactory.class), artistDao,
                    searchService, settingsService, folderOrArtistProc);
            assertEquals(0, content.getItems().size());
            proc.addChild(content, artistOrSong);
            assertEquals(1, content.getItems().size());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Artists"), "Artists", true, now(), 1));

        @Autowired
        private RandomSongByFolderArtistUpnpProcessor proc;

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
            List<FolderOrArtist> items = proc.getDirectChildren(0, 10);
            assertEquals(10, items.size());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(31, proc.getDirectChildrenCount());
        }

        @Test
        void testGetChildren() {
            List<FolderOrArtist> folderOrArtists = proc.getDirectChildren(0, 10);
            assertEquals(10, folderOrArtists.size());
            assertEquals("10", folderOrArtists.get(0).getArtist().getName());
            assertEquals("20", folderOrArtists.get(1).getArtist().getName());
            assertEquals("30", folderOrArtists.get(2).getArtist().getName());

            List<ArtistOrSong> songs = proc.getChildren(new FolderOrArtist(folderOrArtists.get(0).getArtist()), 0,
                    Integer.MAX_VALUE);
            assertEquals(31, songs.size());
        }

        @Test
        void testGetChildSizeOf() {
            List<FolderOrArtist> artists = proc.getDirectChildren(0, 1);
            assertEquals(1, artists.size());
            assertEquals(31, proc.getChildSizeOf(artists.get(0)));
        }
    }
}
