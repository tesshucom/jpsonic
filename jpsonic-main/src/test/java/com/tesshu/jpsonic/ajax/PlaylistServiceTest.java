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

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.i18n.AirsonicLocaleResolver;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class PlaylistServiceTest {

    private static final String ADMIN_NAME = "admin";

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private com.tesshu.jpsonic.service.PlaylistService deligate;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private AirsonicLocaleResolver airsonicLocaleResolver;
    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpServletRequest httpServletRequest;

    private PlaylistService playlistService;

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getHttpServletRequest()).thenReturn(httpServletRequest);
        playlistService = new PlaylistService(mediaFileService, securityService, deligate, mediaFileDao,
                settingsService, playerService, airsonicLocaleResolver, ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testCreatePlaylistForStarredSongs() {
        assertNotEquals(-1, playlistService.createPlaylistForStarredSongs());
    }
}
