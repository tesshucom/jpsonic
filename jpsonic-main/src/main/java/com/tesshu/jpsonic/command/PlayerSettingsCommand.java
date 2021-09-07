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

import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.controller.PlayerSettingsController;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.PlayerTechnology;
import com.tesshu.jpsonic.domain.TranscodeScheme;
import com.tesshu.jpsonic.domain.Transcoding;

/**
 * Command used in {@link PlayerSettingsController}.
 *
 * @author Sindre Mehus
 */
public class PlayerSettingsCommand extends SettingsPageCommons {

    private Player[] players;
    private Integer playerId;
    private boolean admin;
    private boolean anonymousTranscoding;
    private boolean transcodingSupported;

    // Player settings
    private String type;
    private String name;
    private boolean guest;
    private boolean anonymous;
    private PlayerTechnology playerTechnology;
    private TranscodeScheme transcodeScheme;
    private List<Transcoding> allTranscodings;
    private int[] activeTranscodingIds;
    private boolean dynamicIp;
    private boolean autoControlEnabled;
    private boolean m3uBomEnabled;
    private Date lastSeen;

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
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

    public boolean isAnonymousTranscoding() {
        return anonymousTranscoding;
    }

    public void setAnonymousTranscoding(boolean anonymousTranscoding) {
        this.anonymousTranscoding = anonymousTranscoding;
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

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isDynamicIp() {
        return dynamicIp;
    }

    public void setDynamicIp(boolean dynamicIp) {
        this.dynamicIp = dynamicIp;
    }

    public boolean isAutoControlEnabled() {
        return autoControlEnabled;
    }

    public void setAutoControlEnabled(boolean autoControlEnabled) {
        this.autoControlEnabled = autoControlEnabled;
    }

    public boolean isM3uBomEnabled() {
        return m3uBomEnabled;
    }

    public void setM3uBomEnabled(boolean m3uBomEnabled) {
        this.m3uBomEnabled = m3uBomEnabled;
    }

    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    public boolean isTranscodingSupported() {
        return transcodingSupported;
    }

    public void setTranscodingSupported(boolean transcodingSupported) {
        this.transcodingSupported = transcodingSupported;
    }

    public List<Transcoding> getAllTranscodings() {
        return allTranscodings;
    }

    public void setAllTranscodings(List<Transcoding> allTranscodings) {
        this.allTranscodings = allTranscodings;
    }

    public int[] getActiveTranscodingIds() {
        return activeTranscodingIds;
    }

    public void setActiveTranscodingIds(int... activeTranscodingIds) {
        if (activeTranscodingIds != null) {
            this.activeTranscodingIds = activeTranscodingIds.clone();
        }
    }

    public PlayerTechnology getPlayerTechnology() {
        return playerTechnology;
    }

    public void setPlayerTechnology(PlayerTechnology playerTechnology) {
        this.playerTechnology = playerTechnology;
    }

    public Player[] getPlayers() {
        return players;
    }

    public void setPlayers(Player... players) {
        if (players != null) {
            this.players = players.clone();
        }
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
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
