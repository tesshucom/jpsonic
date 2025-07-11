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

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class AlbumByFolderProcTest {

    private MediaFileService mediaFileService;
    private UpnpProcessorUtil util;
    private AlbumByFolderProc proc;

    @BeforeEach
    public void setup() {
        mediaFileService = mock(MediaFileService.class);
        util = mock(UpnpProcessorUtil.class);
        proc = new AlbumByFolderProc(util, mock(UpnpDIDLFactory.class), mediaFileService);
    }

    @Test
    void testGetProcId() {
        assertEquals("albf", proc.getProcId().getValue());
    }

    @Nested
    class GetDirectChildrenTest {

        private final MusicFolder folder1 = new MusicFolder("path1", "name1", true, null, false);
        private final MusicFolder folder2 = new MusicFolder("path2", "name2", true, null, false);

        @Test
        void testNoFolder() {
            assertTrue(proc.getDirectChildren(0, 30).isEmpty());
        }

        @Test
        void testWithSingleFolder() {
            when(util.getGuestFolders()).thenReturn(List.of(folder1));
            assertEquals(0, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
            verify(mediaFileService, times(1))
                .getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(),
                        ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testMultiFolder() {
            when(util.getGuestFolders()).thenReturn(List.of(folder1, folder2));
            assertEquals(2, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
            verify(mediaFileService, times(2)).getMediaFile(any(Path.class));
        }
    }

    @Nested
    class GetDirectChildrenCountTest {

        private final MusicFolder folder1 = new MusicFolder("path1", "name1", true, null, false);
        private final MusicFolder folder2 = new MusicFolder("path2", "name2", true, null, false);

        @Test
        void testNoFolder() {
            when(util.getGuestFolders()).thenReturn(Collections.emptyList());
            assertEquals(0, proc.getDirectChildrenCount());
        }

        @Test
        void testWithSingleFolder() {
            when(util.getGuestFolders()).thenReturn(List.of(folder1));
            assertEquals(0, proc.getDirectChildrenCount());
            verify(mediaFileService, times(1))
                .getAlbumCount(ArgumentMatchers.<MusicFolder>anyList());
        }

        @Test
        void testMultiFolder() {
            when(util.getGuestFolders()).thenReturn(List.of(folder1, folder2));
            assertEquals(2, proc.getDirectChildrenCount());
            verify(mediaFileService, never())
                .getAlbumCount(ArgumentMatchers.<MusicFolder>anyList());
        }
    }
}
