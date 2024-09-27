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

package com.tesshu.jpsonic.command;

import java.time.ZonedDateTime;
import java.util.List;

import com.tesshu.jpsonic.controller.PlayerSettingsController;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Command used in {@link PlayerSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PlayerSettingsCommand extends SettingsPageCommons {

    private List<Player> players;
    private Integer playerId;
    private boolean admin;
    private boolean sameSegment;
    private boolean transcodingSupported;

    // Player settings
    private String name;
    private String type;
    private String ipAddress;
    private boolean guest;
    private boolean anonymous;
    private List<Transcoding> allTranscodings;
    private TranscodeScheme maxBitrate;
    private TranscodeScheme transcodeScheme;
    private int[] activeTranscodingIds;
    private boolean dynamicIp;
    private ZonedDateTime lastSeen;

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isSameSegment() {
        return sameSegment;
    }

    public void setSameSegment(boolean sameSegment) {
        this.sameSegment = sameSegment;
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean isGuest) {
        this.guest = isGuest;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean isAnonymous) {
        this.anonymous = isAnonymous;
    }

    public List<Transcoding> getAllTranscodings() {
        return allTranscodings;
    }

    public void setAllTranscodings(List<Transcoding> allTranscodings) {
        this.allTranscodings = allTranscodings;
    }

    public TranscodeScheme getMaxBitrate() {
        return maxBitrate;
    }

    public void setMaxBitrate(TranscodeScheme maxBitrate) {
        this.maxBitrate = maxBitrate;
    }

    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    public int[] getActiveTranscodingIds() {
        return activeTranscodingIds;
    }

    public void setActiveTranscodingIds(int... activeTranscodingIds) {
        if (activeTranscodingIds != null) {
            this.activeTranscodingIds = activeTranscodingIds.clone();
        }
    }

    public boolean isDynamicIp() {
        return dynamicIp;
    }

    public void setDynamicIp(boolean dynamicIp) {
        this.dynamicIp = dynamicIp;
    }

    public @Nullable ZonedDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(ZonedDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Holds the transcoding and whether it is active for the given player.
     */
    public static class TranscodingHolder {
        private final Transcoding transcoding;
        private final boolean active;

        public TranscodingHolder(Transcoding transcoding, boolean isActive) {
            this.transcoding = transcoding;
            this.active = isActive;
        }

        public Transcoding getTranscoding() {
            return transcoding;
        }

        public boolean isActive() {
            return active;
        }
    }
}
