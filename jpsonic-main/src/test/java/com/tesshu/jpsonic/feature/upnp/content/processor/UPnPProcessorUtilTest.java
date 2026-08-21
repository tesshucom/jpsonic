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

package com.tesshu.jpsonic.feature.upnp.content.processor;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNotNull;

import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.UserService;
import com.tesshu.jpsonic.service.language.JpsonicComparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.SingularField")
class UPnPProcessorUtilTest {

    private UPnPProcessorUtil util;

    @BeforeEach
    void setup() {
        util = new UPnPProcessorUtil(mock(MusicFolderService.class), mock(UserService.class),
                mock(JpsonicComparators.class));
    }

    @Test
    void testGetAllMusicFolders() {
        util = new UPnPProcessorUtil(mock(MusicFolderService.class), mock(UserService.class),
                mock(JpsonicComparators.class));

        assertNotNull(util.getGuestFolders());
    }
}
