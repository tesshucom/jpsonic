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

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Playlist", propOrder = { "allowedUser" })
@XmlSeeAlso({ PlaylistWithSongs.class })
public class Playlist {

    protected List<String> allowedUser;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "comment")
    protected String comment;
    @XmlAttribute(name = "owner")
    protected String owner;
    @XmlAttribute(name = "public")
    @SuppressWarnings("PMD.FieldNamingConventions") // XJC Naming Conventions
    protected Boolean _public;
    @XmlAttribute(name = "songCount", required = true)
    protected int songCount;
    @XmlAttribute(name = "duration", required = true)
    protected int duration;
    @XmlAttribute(name = "created", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar created;
    @XmlAttribute(name = "changed", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar changed;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;

    public List<String> getAllowedUser() {
        if (allowedUser == null) {
            allowedUser = new ArrayList<>();
        }
        return this.allowedUser;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        this.comment = value;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String value) {
        this.owner = value;
    }

    public Boolean isPublic() {
        return _public;
    }

    public void setPublic(Boolean value) {
        this._public = value;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int value) {
        this.songCount = value;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int value) {
        this.duration = value;
    }

    public XMLGregorianCalendar getCreated() {
        return created;
    }

    public void setCreated(XMLGregorianCalendar value) {
        this.created = value;
    }

    public XMLGregorianCalendar getChanged() {
        return changed;
    }

    public void setChanged(XMLGregorianCalendar value) {
        this.changed = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }
}
