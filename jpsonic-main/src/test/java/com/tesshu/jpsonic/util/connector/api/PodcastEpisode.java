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

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PodcastEpisode")
public class PodcastEpisode extends Child {

    @XmlAttribute(name = "streamId")
    protected String streamId;
    @XmlAttribute(name = "channelId", required = true)
    protected String channelId;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "status", required = true)
    protected PodcastStatus status;
    @XmlAttribute(name = "publishDate")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar publishDate;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String value) {
        this.streamId = value;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String value) {
        this.channelId = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public PodcastStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastStatus value) {
        this.status = value;
    }

    public XMLGregorianCalendar getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(XMLGregorianCalendar value) {
        this.publishDate = value;
    }
}
