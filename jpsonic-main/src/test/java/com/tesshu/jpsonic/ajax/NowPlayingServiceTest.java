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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.PlayStatus;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.MediaScannerService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import com.tesshu.jpsonic.service.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.test.context.support.WithMockUser;

class NowPlayingServiceTest {

    private NowPlayingService nowPlayingService;

    @BeforeEach
    public void setup() {
        StatusService statusService = mock(StatusService.class);
        MediaFile file = new MediaFile();
        file.setId(0);
        Player player = new Player();
        player.setUsername(ServiceMockUtils.ADMIN_NAME);
        PlayStatus playStatus = new PlayStatus(file, player, new Date());
        Mockito.when(statusService.getPlayStatuses()).thenReturn(Arrays.asList(playStatus));

        nowPlayingService = new NowPlayingService(mock(SecurityService.class), mock(PlayerService.class), statusService,
                mock(MediaScannerService.class), mock(AvatarService.class), AjaxMockUtils.mock(AjaxHelper.class));
    }

    @Test
    @WithMockUser(username = ServiceMockUtils.ADMIN_NAME)
    void testGetNowPlaying() {
        assertNotNull(nowPlayingService.getNowPlaying());
    }
}
