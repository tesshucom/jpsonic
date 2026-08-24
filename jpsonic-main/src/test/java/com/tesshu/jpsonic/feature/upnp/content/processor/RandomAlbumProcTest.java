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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.tesshu.jpsonic.domain.provider.resource.AlbumProvider;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.search.MediaSearchProvider;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.support.model.BrowseResult;

class RandomAlbumProcTest {

    private MediaSearchProvider mediaSearchProvider;
    private SettingsFacade settingsFacade;
    private RandomAlbumProc proc;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder.create().build();
        init();
    }

    @Ignore
    void init() {
        mediaSearchProvider = mock(MediaSearchProvider.class);
        MusicFolderProvider musicFolderProvider = mock(MusicFolderProvider.class);
        AlbumProvider albumProvider = mock(AlbumProvider.class);
        MediaFileProvider mediaFileProvider = mock(MediaFileProvider.class);
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        proc = new RandomAlbumProc(musicFolderProvider, albumProvider, mediaFileProvider, factory,
                mediaSearchProvider, settingsFacade);
    }

    @Test
    void testGetProcId() {
        assertEquals("ral", proc.getProcId().getValue());
    }

    @Test
    void testBrowseRoot() throws ExecutionException {
        AtomicInteger randomMaxCount = new AtomicInteger();
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withIntAnswer(UPnPSKeys.options.randomMax, invocation -> {
                randomMaxCount.incrementAndGet();
                return 0;
            })
            .build();
        init();

        BrowseResult result = proc.browseRoot(null, 0, 0);
        assertEquals(0, result.getCount().getValue());
        assertEquals(2, randomMaxCount.get());

        verify(mediaSearchProvider, times(1))
            .getRandomAlbumsId3(anyList(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(0, proc.getDirectChildren(0, 100).size());
        verify(mediaSearchProvider, times(1))
            .getRandomAlbumsId3(anyList(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void testGetDirectChildrenCount() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withInt(UPnPSKeys.options.randomMax, 1_000)
            .build();
        init();
        assertEquals(1_000, proc.getDirectChildrenCount());
    }
}
