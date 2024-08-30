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
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "JukeboxStatus")
@XmlSeeAlso({ JukeboxPlaylist.class })
public class JukeboxStatus {

    @XmlAttribute(name = "currentIndex", required = true)
    protected int currentIndex;
    @XmlAttribute(name = "playing", required = true)
    protected boolean playing;
    @XmlAttribute(name = "gain", required = true)
    protected float gain;
    @XmlAttribute(name = "position")
    protected Integer position;

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int value) {
        this.currentIndex = value;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean value) {
        this.playing = value;
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float value) {
        this.gain = value;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer value) {
        this.position = value;
    }
}
