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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PodcastChannel", propOrder = { "episode" })
public class PodcastChannel {

    protected List<PodcastEpisode> episode;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "url", required = true)
    protected String url;
    @XmlAttribute(name = "title")
    protected String title;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;
    @XmlAttribute(name = "originalImageUrl")
    protected String originalImageUrl;
    @XmlAttribute(name = "status", required = true)
    protected PodcastStatus status;
    @XmlAttribute(name = "errorMessage")
    protected String errorMessage;

    public List<PodcastEpisode> getEpisode() {
        if (episode == null) {
            episode = new ArrayList<>();
        }
        return this.episode;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String value) {
        this.url = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String value) {
        this.originalImageUrl = value;
    }

    public PodcastStatus getStatus() {
        return status;
    }

    public void setStatus(PodcastStatus value) {
        this.status = value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String value) {
        this.errorMessage = value;
    }
}
