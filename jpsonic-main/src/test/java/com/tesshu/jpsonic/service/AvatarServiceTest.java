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

package com.tesshu.jpsonic.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.AvatarDao;
import com.tesshu.jpsonic.domain.Avatar;
import com.tesshu.jpsonic.domain.AvatarScheme;
import com.tesshu.jpsonic.domain.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class AvatarServiceTest {

    @Mock
    private AvatarDao avatarDao;

    private AvatarService avatarService;

    @BeforeEach
    public void setup() {
        Mockito.when(avatarDao.getCustomAvatar(Mockito.anyString()))
                .thenReturn(new Avatar(0, null, null, null, 0, 0, null));
        avatarService = new AvatarService(avatarDao);
    }

    @Test
    void testCreateAvatarUrl() throws ExecutionException {
        UserSettings userSettings = new UserSettings();
        userSettings.setUsername("admin");
        userSettings.setAvatarScheme(AvatarScheme.CUSTOM);
        assertNotNull(avatarService.createAvatarUrl("", userSettings));
    }

    @Test
    void testCreateAvatarResized() throws IOException {
        try (InputStream inputStream = AvatarServiceTest.class
                .getResourceAsStream("/org/airsonic/player/dao/schema/avatar01.png")) {
            String fileName = "avatar01.png";
            long size = 2_117;
            boolean resized = avatarService.createAvatar(fileName, inputStream, size, fileName);
            assertTrue(resized);
        }
    }

    @Test
    void testCreateAvatarNotResized() throws IOException {
        try (InputStream inputStream = AvatarServiceTest.class
                .getResourceAsStream("/org/airsonic/player/dao/schema/Vinyl.png")) {
            String fileName = "Vinyl.png";
            long size = 2_058;
            boolean resized = avatarService.createAvatar(fileName, inputStream, size, fileName);
            assertFalse(resized);
        }
    }
}
