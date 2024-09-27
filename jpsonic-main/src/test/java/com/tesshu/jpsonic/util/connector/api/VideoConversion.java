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
@XmlType(name = "VideoConversion")
public class VideoConversion {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "bitRate")
    protected Integer bitRate;
    @XmlAttribute(name = "audioTrackId")
    protected Integer audioTrackId;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer value) {
        this.bitRate = value;
    }

    public Integer getAudioTrackId() {
        return audioTrackId;
    }

    public void setAudioTrackId(Integer value) {
        this.audioTrackId = value;
    }
}
