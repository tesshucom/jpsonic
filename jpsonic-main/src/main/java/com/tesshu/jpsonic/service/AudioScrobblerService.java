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

package com.tesshu.jpsonic.service;

import java.util.Date;
import java.util.concurrent.Executor;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.UserSettings;
import com.tesshu.jpsonic.service.scrobbler.LastFMScrobbler;
import com.tesshu.jpsonic.service.scrobbler.ListenBrainzScrobbler;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Provides services for "audioscrobbling", which is the process of registering what songs are played at website.
 */
@Service
@DependsOn({ "settingsService", "shortExecutor" })
public class AudioScrobblerService {

    private static final Object FM_LOCK = new Object();
    private static final Object BRAINZ_LOCK = new Object();

    private final SecurityService securityService;

    private LastFMScrobbler lastFMScrobbler;
    private ListenBrainzScrobbler listenBrainzScrobbler;
    private Executor shortExecutor;

    public AudioScrobblerService(SecurityService securityService, Executor shortExecutor) {
        this.securityService = securityService;
        this.shortExecutor = shortExecutor;
    }

    /**
     * Registers the given media file at audio scrobble service. This method returns immediately, the actual
     * registration is done by separate threads.
     *
     * @param mediaFile
     *            The media file to register.
     * @param username
     *            The user which played the music file.
     * @param submission
     *            Whether this is a submission or a now playing notification.
     * @param time
     *            Event time, or {@code null} to use current time.
     */
    public void register(MediaFile mediaFile, String username, boolean submission, Date time) {
        if (mediaFile == null || mediaFile.isVideo()) {
            return;
        }

        UserSettings userSettings = securityService.getUserSettings(username);
        if (userSettings.isLastFmEnabled() && userSettings.getLastFmUsername() != null
                && userSettings.getLastFmPassword() != null) {
            synchronized (FM_LOCK) {
                if (lastFMScrobbler == null) {
                    lastFMScrobbler = new LastFMScrobbler();
                }
            }
            lastFMScrobbler.register(mediaFile, userSettings.getLastFmUsername(), userSettings.getLastFmPassword(),
                    submission, time, shortExecutor);
        }

        if (userSettings.isListenBrainzEnabled() && userSettings.getListenBrainzToken() != null) {
            synchronized (BRAINZ_LOCK) {
                if (listenBrainzScrobbler == null) {
                    listenBrainzScrobbler = new ListenBrainzScrobbler();
                }
            }
            listenBrainzScrobbler.register(mediaFile, userSettings.getListenBrainzToken(), submission, time,
                    shortExecutor);
        }
    }

}
