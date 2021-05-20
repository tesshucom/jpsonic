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

package org.airsonic.player.service.jukebox;

import java.io.InputStream;
import java.util.concurrent.Executor;

import javax.sound.sampled.LineUnavailableException;

import org.airsonic.player.service.JukeboxLegacySubsonicService;
import org.airsonic.player.service.SettingsService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("jukeExecutor")
public class AudioPlayerFactory {

    private final Executor jukeExecutor;
    private final SettingsService settingsService;

    public AudioPlayerFactory(Executor jukeExecutor, SettingsService settingsService) {
        super();
        this.jukeExecutor = jukeExecutor;
        this.settingsService = settingsService;
    }

    public AudioPlayer createAudioPlayer(InputStream in, JukeboxLegacySubsonicService jukeboxLegacySubsonicService)
            throws LineUnavailableException {
        return new AudioPlayer(in, jukeboxLegacySubsonicService, jukeExecutor, settingsService.isVerboseLogShutdown());
    }
}
