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

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class NowPlayingServiceTest {

    private static final String ADMIN_NAME = "admin";

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Mock
    private StatusService statusService;
    @Autowired
    private MediaScannerService mediaScannerService;
    @Autowired
    private AvatarService avatarService;
    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpServletRequest httpServletRequest;
    @Autowired
    private MockHttpServletResponse httpServletResponse;

    private NowPlayingService nowPlayingService;

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getHttpServletRequest()).thenReturn(httpServletRequest);
        Mockito.when(ajaxHelper.getHttpServletResponse()).thenReturn(httpServletResponse);
        MediaFile file = new MediaFile();
        file.setId(0);
        Player player = new Player();
        player.setUsername(ADMIN_NAME);
        PlayStatus playStatus = new PlayStatus(file, player, new Date());
        Mockito.when(statusService.getPlayStatuses()).thenReturn(Arrays.asList(playStatus));

        nowPlayingService = new NowPlayingService(securityService, playerService, statusService, mediaScannerService,
                avatarService, ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGetNowPlaying() {
        assertNotNull(nowPlayingService.getNowPlaying());
    }
}
