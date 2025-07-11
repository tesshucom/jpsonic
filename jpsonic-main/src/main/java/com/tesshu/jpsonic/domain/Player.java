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

package com.tesshu.jpsonic.domain;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

/**
 * Represens a remote player. A player has a unique ID, a user-defined name, a
 * logged-on user, miscellaneous identifiers, and an associated playlist.
 *
 * @author Sindre Mehus
 */
public class Player {

    private Integer id;
    private String name;
    private String clientId;
    private String type;
    private String username;
    private String ipAddress;
    private boolean dynamicIp = true;
    private Instant lastSeen;
    private TranscodeScheme transcodeScheme = TranscodeScheme.OFF;
    private PlayQueue playQueue;

    /**
     * Returns the player ID.
     *
     * @return The player ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the player ID.
     *
     * @param id The player ID.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Returns the user-defined player name.
     *
     * @return The user-defined player name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user-defined player name.
     *
     * @param name The user-defined player name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the third-party client ID (used if this player is managed over the
     * Airsonic REST API).
     *
     * @return The client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the third-party client ID (used if this player is managed over the
     * Airsonic REST API).
     *
     * @param clientId The client ID.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the player type, e.g., WinAmp, iTunes.
     *
     * @return The player type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the player type, e.g., WinAmp, iTunes.
     *
     * @param type The player type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the logged-in user.
     *
     * @return The logged-in user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the logged-in username.
     *
     * @param username The logged-in username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the time when the player was last seen.
     *
     * @return The time when the player was last seen.
     */
    public Instant getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets the time when the player was last seen.
     *
     * @param lastSeen The time when the player was last seen.
     */
    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Returns the transcode scheme.
     *
     * @return The transcode scheme.
     */
    public TranscodeScheme getTranscodeScheme() {
        return transcodeScheme;
    }

    /**
     * Sets the transcode scheme.
     *
     * @param transcodeScheme The transcode scheme.
     */
    public void setTranscodeScheme(TranscodeScheme transcodeScheme) {
        this.transcodeScheme = transcodeScheme;
    }

    /**
     * Returns the IP address of the player.
     *
     * @return The IP address of the player.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address of the player.
     *
     * @param ipAddress The IP address of the player.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns whether this player has a dynamic IP address.
     *
     * @return Whether this player has a dynamic IP address.
     */
    public boolean isDynamicIp() {
        return dynamicIp;
    }

    /**
     * Sets whether this player has a dynamic IP address.
     *
     * @param dynamicIp Whether this player has a dynamic IP address.
     */
    public void setDynamicIp(boolean dynamicIp) {
        this.dynamicIp = dynamicIp;
    }

    /**
     * Returns the player's playlist.
     *
     * @return The player's playlist
     */
    public PlayQueue getPlayQueue() {
        return playQueue;
    }

    /**
     * Sets the player's playlist.
     *
     * @param playQueue The player's playlist.
     */
    public void setPlayQueue(PlayQueue playQueue) {
        this.playQueue = playQueue;
    }

    /**
     * Returns a long description of the player, e.g., <code>Player 3 [admin]</code>
     *
     * @return A long description of the player.
     */
    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        if (name == null) {
            builder.append("Player ").append(id);
        } else {
            builder.append(name);
        }

        builder.append(" [").append(username).append(']');
        return builder.toString();
    }

    /**
     * Returns a short description of the player, e.g., <code>Player 3</code>
     *
     * @return A short description of the player.
     */
    public String getShortDescription() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return "Player " + id;
    }

    /**
     * Returns a string representation of the player.
     *
     * @return A string representation of the player.
     *
     * @see #getDescription()
     */
    @Override
    public String toString() {
        return getDescription();
    }

}
