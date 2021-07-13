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

import static org.junit.Assert.assertNotNull;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.domain.JpsonicComparators;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class UpnpProcessorUtilTest {

    @Mock
    private SettingsService settingsService;
    @Autowired
    private MusicFolderService musicFolderService;
    @Autowired
    private JpsonicComparators comparators;
    @Autowired
    private JWTSecurityService securityService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private MusicFolderDao musicFolderDao;

    @Test
    void testGetAllMusicFolders() {
        Mockito.when(settingsService.isDlnaGuestPublish()).thenReturn(true);
        UpnpProcessorUtil upnpProcessorUtil = new UpnpProcessorUtil(settingsService, musicFolderService, comparators,
                securityService, transcodingService, musicFolderDao);
        assertNotNull(upnpProcessorUtil.getAllMusicFolders());
    }
}
