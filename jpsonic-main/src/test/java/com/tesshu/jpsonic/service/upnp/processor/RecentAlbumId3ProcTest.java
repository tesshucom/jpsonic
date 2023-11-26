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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.dao.AlbumDao;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.util.LegacyMap;
import org.fourthline.cling.support.model.BrowseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("PMD.TooManyStaticImports")
class RecentAlbumId3ProcTest {

    @Nested
    class UnitTest {

        private AlbumDao albumDao;
        private RecentAlbumId3Proc proc;

        @BeforeEach
        public void setup() {
            UpnpProcessorUtil util = mock(UpnpProcessorUtil.class);
            UpnpDIDLFactory factory = mock(UpnpDIDLFactory.class);
            MediaFileService mediaFileService = mock(MediaFileService.class);
            albumDao = mock(AlbumDao.class);
            proc = new RecentAlbumId3Proc(util, factory, mediaFileService, albumDao);
        }

        @Test
        void testGetProcId() {
            assertEquals("recentId3", proc.getProcId().getValue());
        }

        @Test
        void testBrowseRoot() throws ExecutionException {
            when(albumDao.getNewestAlbums(anyInt(), anyInt(), anyList())).thenReturn(List.of(new Album()));
            when(albumDao.getAlbumCount(anyList())).thenReturn(99);
            BrowseResult result = proc.browseRoot(null, 0, 0);
            assertEquals(1, result.getCountLong());
            assertEquals(50, result.getTotalMatches().getValue()); // RECENT_COUNT
            verify(albumDao, times(1)).getNewestAlbums(anyInt(), anyInt(), anyList());
            verify(albumDao, times(1)).getAlbumCount(anyList());
        }

        @Test
        void testGetDirectChildren() {
            assertEquals(0, proc.getDirectChildren(0, 0).size());
            verify(albumDao, times(1)).getNewestAlbums(anyInt(), anyInt(), anyList());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(0, proc.getDirectChildrenCount());
            verify(albumDao, times(1)).getAlbumCount(anyList());
        }
    }

    @Nested
    class IntegrationTest extends AbstractNeedsScan {

        private static final List<MusicFolder> MUSIC_FOLDERS = Arrays
                .asList(new MusicFolder(1, resolveBaseMediaPath("Sort/Pagination/Albums"), "Albums", true, now(), 1));

        @Autowired
        private RecentAlbumId3Proc processor;

        @Override
        public List<MusicFolder> getMusicFolders() {
            return MUSIC_FOLDERS;
        }

        @BeforeEach
        public void setup() {
            setSortStrict(true);
            setSortAlphanum(true);
            populateDatabaseOnlyOnce();
        }

        @Test
        void testGetDirectChildren() {
            assertEquals(30, processor.getDirectChildren(0, 30).size());
            assertEquals(1, processor.getDirectChildren(30, 30).size());

            Map<Integer, Album> c = LegacyMap.of();

            List<Album> items = processor.getDirectChildren(0, 10);
            items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(10, c.size());

            items = processor.getDirectChildren(10, 10);
            items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(20, c.size());

            items = processor.getDirectChildren(20, 100);
            assertEquals(11, items.size());
            items.stream().filter(m -> !c.containsKey(m.getId())).forEach(m -> c.put(m.getId(), m));
            assertEquals(31, c.size());

            assertEquals(4, processor.getDirectChildren(0, 4).size());
            assertEquals(3, processor.getDirectChildren(0, 3).size());
            assertEquals(2, processor.getDirectChildren(0, 2).size());
            assertEquals(1, processor.getDirectChildren(0, 1).size());

            assertEquals(4, processor.getDirectChildren(1, 4).size());
            assertEquals(3, processor.getDirectChildren(1, 3).size());
            assertEquals(2, processor.getDirectChildren(1, 2).size());
            assertEquals(1, processor.getDirectChildren(1, 1).size());
        }

        @Test
        void testGetDirectChildrenCount() {
            assertEquals(31, processor.getDirectChildrenCount());
        }

        @Test
        void testGetChildSizeOf() {
            List<Album> albums = processor.getDirectChildren(1, 1);
            assertEquals(1, albums.size());
            assertEquals(1, processor.getChildSizeOf(albums.get(0)));
        }
    }
}
