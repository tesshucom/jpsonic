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
import jakarta.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Genre", propOrder = { "content" })
public class Genre {

    @XmlValue
    protected String content;
    @XmlAttribute(name = "songCount", required = true)
    protected int songCount;
    @XmlAttribute(name = "albumCount", required = true)
    protected int albumCount;

    public String getContent() {
        return content;
    }

    public void setContent(String value) {
        this.content = value;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int value) {
        this.songCount = value;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int value) {
        this.albumCount = value;
    }
}
