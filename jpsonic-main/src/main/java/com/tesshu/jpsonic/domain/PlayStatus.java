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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

//Move this class to a static inner class of StatusService

package com.tesshu.jpsonic.domain;

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.Player;

/**
 * Represents the playback of a track, possibly remote (e.g., a cached song on a
 * mobile phone).
 *
 * @author Sindre Mehus
 */
public class PlayStatus {

    private final MediaFile mediaFile;
    private final Player player;
    private final Instant time;

    public PlayStatus(MediaFile mediaFile, Player player, Instant time) {
        this.mediaFile = mediaFile;
        this.player = player;
        this.time = time;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public Player getPlayer() {
        return player;
    }

    public Instant getTime() {
        return time;
    }

    public boolean isExpired() {
        return now().minus(6, ChronoUnit.HOURS).isBefore(time);
    }

    public long getMinutesAgo() {
        return Math.abs(ChronoUnit.MINUTES.between(time, now()));
    }
}
