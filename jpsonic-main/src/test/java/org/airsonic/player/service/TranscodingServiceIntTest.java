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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service;

import static org.mockito.Mockito.verify;

import org.airsonic.player.domain.Transcoding;
import org.airsonic.player.util.HomeRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TranscodingServiceIntTest {

    @Autowired
    private TranscodingService transcodingService;
    @SpyBean
    private PlayerService playerService;

    @ClassRule
    public static final HomeRule classRule = new HomeRule(); // sets jpsonic.home to a temporary dir

    @Test
    public void createTranscodingTest() {
        // Given
        Transcoding transcoding = new Transcoding(null, "test-transcoding", "mp3", "wav", "step1", "step2", "step3",
                true);

        transcodingService.createTranscoding(transcoding);
        verify(playerService).getAllPlayers();
    }
}
