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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.policy.RuntimeOrderPolicy;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.MusicFolderProvider;
import com.tesshu.jpsonic.feature.upnp.content.UPnPDIDLFactory;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SuppressWarnings("PMD.TooManyStaticImports")
class AlbumProcTest {

    private MediaFileProvider mediaFileProvider;
    private AlbumProc proc;

    @BeforeEach
    void setup() {
        SettingsFacade settingsFacade = SettingsFacadeBuilder.create().buildWithDefault();
        UPnPDIDLFactory factory = mock(UPnPDIDLFactory.class);
        mediaFileProvider = mock(MediaFileProvider.class);
        MusicFolderProvider musicFolderProvider = mock(MusicFolderProvider.class);
        proc = new AlbumProc(musicFolderProvider, mediaFileProvider, settingsFacade, factory);
    }

    @Test
    void testGetProcId() {
        assertEquals("al", proc.getProcId().getValue());
    }

    @Test
    void testGetDirectChildren() {
        assertEquals(0, proc.getDirectChildren(0, Integer.MAX_VALUE).size());
        verify(mediaFileProvider, times(1))
            .findAlbums(ArgumentMatchers.<MusicFolder>anyList(),
                    any(RuntimeOrderPolicy.AlbumSortOrder.class), anyLong(), anyLong());
    }

    @Test
    void testGetDirectChildrenCount() {
        assertEquals(0, proc.getDirectChildrenCount());
        verify(mediaFileProvider, times(1)).countAlbums(ArgumentMatchers.<MusicFolder>anyList());
    }
}
