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

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SearchService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;
import org.mockito.Mockito;

class RandomSongProcTest {

    private SearchService searchService;
    private SettingsService settingsService;
    private RandomSongProc proc;

    @BeforeEach
    public void setup() {
        searchService = mock(SearchService.class);
        settingsService = mock(SettingsService.class);
        proc = new RandomSongProc(mock(UpnpProcessorUtil.class), mock(UpnpDIDLFactory.class),
                mock(MediaFileService.class), searchService, settingsService);
    }

    @Test
    void testGetProcId() {
        assertEquals("randomSong", proc.getProcId().getValue());
    }

    @Test
    void testBrowseRoot() throws ExecutionException {
        BrowseResult result = proc.browseRoot(null, 0, 0);
        assertEquals(0, result.getCount().getValue());
        Mockito.verify(settingsService, Mockito.times(2)).getDlnaRandomMax();
        Mockito.verify(searchService, Mockito.times(1)).getRandomSongs(Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyList());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(0, proc.getDirectChildren(0, 100).size());
        Mockito.verify(searchService, Mockito.times(1)).getRandomSongs(Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.anyList());
    }

    @Test
    void testGetDirectChildrenCount() {
        Mockito.when(settingsService.getDlnaRandomMax()).thenReturn(1_000);
        assertEquals(1_000, proc.getDirectChildrenCount());
    }
}
