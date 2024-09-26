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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.MediaFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class AlbumProcTest {

    private MediaFileService mediaFileService;
    private AlbumProc proc;

    @BeforeEach
    public void setup() {
        mediaFileService = mock(MediaFileService.class);
        proc = new AlbumProc(mock(UpnpProcessorUtil.class), mock(UpnpDIDLFactory.class), mediaFileService);
    }

    @Test
    void testGetProcId() {
        assertEquals("al", proc.getProcId().getValue());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(0, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
        verify(mediaFileService, times(1)).getAlphabeticalAlbums(anyInt(), anyInt(), anyBoolean(),
                ArgumentMatchers.<MusicFolder> anyList());
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(0, proc.getDirectChildrenCount());
        verify(mediaFileService, times(1)).getAlbumCount(ArgumentMatchers.<MusicFolder> anyList());
    }
}
