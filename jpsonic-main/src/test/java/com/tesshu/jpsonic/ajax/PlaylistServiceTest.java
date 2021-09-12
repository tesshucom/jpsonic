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

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.MusicFolderService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class PlaylistServiceTest {

    private PlaylistService playlistService;

    @BeforeEach
    public void setup() {
        playlistService = new PlaylistService(mock(MusicFolderService.class), mock(SecurityService.class),
                mock(MediaFileService.class), mock(com.tesshu.jpsonic.service.PlaylistService.class),
                mock(MediaFileDao.class), mock(PlayerService.class), mock(AirsonicLocaleResolver.class),
                AjaxMockUtils.mock(AjaxHelper.class));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testCreatePlaylistForStarredSongs() {
        assertNotEquals(-1, playlistService.createPlaylistForStarredSongs());
    }
}
