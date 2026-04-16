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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacadeBuilder;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import com.tesshu.jpsonic.service.search.UPnPSearchMethod;
import com.tesshu.jpsonic.service.upnp.UPnPSKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.SingularField")
class UpnpProcessorUtilTest {

    private SettingsFacade settingsFacade;
    private UpnpProcessorUtil util;

    @BeforeEach
    void setup() {
        settingsFacade = SettingsFacadeBuilder.create().build();
        util = new UpnpProcessorUtil(mock(MusicFolderService.class), mock(SecurityService.class),
                settingsFacade, mock(JpsonicComparators.class));
    }

    @Test
    void testGetAllMusicFolders() {
        settingsFacade = SettingsFacadeBuilder
            .create()
            .withBoolean(UPnPSKeys.options.guestPublish, true)
            .build();
        util = new UpnpProcessorUtil(mock(MusicFolderService.class), mock(SecurityService.class),
                settingsFacade, mock(JpsonicComparators.class));

        assertNotNull(util.getGuestFolders());
    }

    @Test
    void testGetUPnPSearchMethod() {
        assertEquals(UPnPSearchMethod.FILE_STRUCTURE, util.getUPnPSearchMethod());

        settingsFacade = SettingsFacadeBuilder
            .create()
            .withString(UPnPSKeys.search.upnpSearchMethod, UPnPSearchMethod.ID3.name())
            .build();
        util = new UpnpProcessorUtil(mock(MusicFolderService.class), mock(SecurityService.class),
                settingsFacade, mock(JpsonicComparators.class));

        assertEquals(UPnPSearchMethod.ID3, util.getUPnPSearchMethod());
    }
}
