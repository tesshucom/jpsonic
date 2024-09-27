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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.util.connector.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NowPlayingEntry")
public class NowPlayingEntry extends Child {

    @XmlAttribute(name = "username", required = true)
    protected String username;
    @XmlAttribute(name = "minutesAgo", required = true)
    protected int minutesAgo;
    @XmlAttribute(name = "playerId", required = true)
    protected int playerId;
    @XmlAttribute(name = "playerName")
    protected String playerName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public int getMinutesAgo() {
        return minutesAgo;
    }

    public void setMinutesAgo(int value) {
        this.minutesAgo = value;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int value) {
        this.playerId = value;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String value) {
        this.playerName = value;
    }
}
