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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.tesshu.jpsonic.dao.MediaFileDao.ChildOrder;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class RecentAlbumByFolderProcTest {

    private UpnpProcessorUtil util;
    private MediaFileService mediaFileService;
    private RecentAlbumByFolderProc processor;

    @BeforeEach
    public void setup() {
        util = mock(UpnpProcessorUtil.class);
        UpnpDIDLFactory factory = mock(UpnpDIDLFactory.class);
        mediaFileService = mock(MediaFileService.class);
        processor = new RecentAlbumByFolderProc(util, factory, mediaFileService);

    }

    @Test
    void testGetProcId() {
        assertEquals("rbf", processor.getProcId().getValue());
    }

    @Nested
    class GetChildrenTest {

        private MediaFile folder;

        @BeforeEach
        public void setup() {
            folder = new MediaFile();
            folder.setMediaType(MediaType.DIRECTORY);
            folder.setPathString("pathString");
            when(util.getGuestFolders())
                .thenReturn(List
                    .of(new MusicFolder(folder.getPathString(), "name1", true, null, false)));
        }

        @Test
        void testAlbumChildren() {
            MediaFile album = new MediaFile();
            album.setMediaType(MediaType.ALBUM);
            assertTrue(processor.getChildren(album, 0, 0).isEmpty());
            verify(mediaFileService, times(1))
                .getChildrenOf(any(MediaFile.class), anyLong(), anyLong(), any(ChildOrder.class),
                        any(MediaType[].class));
            verify(mediaFileService, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testFolderChildrenWithCountZero() {
            assertTrue(processor.getChildren(folder, 0, 0).isEmpty());
            verify(mediaFileService, never())
                .getChildrenOf(any(MediaFile.class), anyLong(), anyLong(), any(ChildOrder.class),
                        any(MediaType[].class));
            verify(mediaFileService, never())
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testFolderChildrenWithValidValue() {
            when(mediaFileService.getAlbumCount(ArgumentMatchers.<MusicFolder>anyList()))
                .thenReturn(1L);
            assertTrue(processor.getChildren(folder, 0, 1).isEmpty());
            verify(mediaFileService, never())
                .getChildrenOf(any(MediaFile.class), anyLong(), anyLong(), any(ChildOrder.class),
                        any(MediaType[].class));
            verify(mediaFileService, times(1))
                .getNewestAlbums(anyInt(), anyInt(), ArgumentMatchers.<MusicFolder>anyList());
        }
    }
}
