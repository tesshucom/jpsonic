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
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Child")
@XmlSeeAlso({ NowPlayingEntry.class, PodcastEpisode.class })
public class Child {

    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "parent")
    protected String parent;
    @XmlAttribute(name = "isDir", required = true)
    protected boolean isDir;
    @XmlAttribute(name = "title", required = true)
    protected String title;
    @XmlAttribute(name = "album")
    protected String album;
    @XmlAttribute(name = "artist")
    protected String artist;
    @XmlAttribute(name = "track")
    protected Integer track;
    @XmlAttribute(name = "year")
    protected Integer year;
    @XmlAttribute(name = "genre")
    protected String genre;
    @XmlAttribute(name = "coverArt")
    protected String coverArt;
    @XmlAttribute(name = "size")
    protected Long size;
    @XmlAttribute(name = "contentType")
    protected String contentType;
    @XmlAttribute(name = "suffix")
    protected String suffix;
    @XmlAttribute(name = "transcodedContentType")
    protected String transcodedContentType;
    @XmlAttribute(name = "transcodedSuffix")
    protected String transcodedSuffix;
    @XmlAttribute(name = "duration")
    protected Integer duration;
    @XmlAttribute(name = "bitRate")
    protected Integer bitRate;
    @XmlAttribute(name = "path")
    protected String path;
    @XmlAttribute(name = "isVideo")
    protected Boolean isVideo;
    @XmlAttribute(name = "userRating")
    protected Integer userRating;
    @XmlAttribute(name = "averageRating")
    protected Double averageRating;
    @XmlAttribute(name = "playCount")
    protected Long playCount;
    @XmlAttribute(name = "discNumber")
    protected Integer discNumber;
    @XmlAttribute(name = "created")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar created;
    @XmlAttribute(name = "starred")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar starred;
    @XmlAttribute(name = "albumId")
    protected String albumId;
    @XmlAttribute(name = "artistId")
    protected String artistId;
    @XmlAttribute(name = "type")
    protected MediaType type;
    @XmlAttribute(name = "bookmarkPosition")
    protected Long bookmarkPosition;
    @XmlAttribute(name = "originalWidth")
    protected Integer originalWidth;
    @XmlAttribute(name = "originalHeight")
    protected Integer originalHeight;

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String value) {
        this.parent = value;
    }

    public boolean isIsDir() {
        return isDir;
    }

    public void setIsDir(boolean value) {
        this.isDir = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String value) {
        this.album = value;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String value) {
        this.artist = value;
    }

    public Integer getTrack() {
        return track;
    }

    public void setTrack(Integer value) {
        this.track = value;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer value) {
        this.year = value;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String value) {
        this.genre = value;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String value) {
        this.coverArt = value;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long value) {
        this.size = value;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String value) {
        this.contentType = value;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String value) {
        this.suffix = value;
    }

    public String getTranscodedContentType() {
        return transcodedContentType;
    }

    public void setTranscodedContentType(String value) {
        this.transcodedContentType = value;
    }

    public String getTranscodedSuffix() {
        return transcodedSuffix;
    }

    public void setTranscodedSuffix(String value) {
        this.transcodedSuffix = value;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer value) {
        this.duration = value;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer value) {
        this.bitRate = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String value) {
        this.path = value;
    }

    public Boolean isIsVideo() {
        return isVideo;
    }

    public void setIsVideo(Boolean value) {
        this.isVideo = value;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer value) {
        this.userRating = value;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double value) {
        this.averageRating = value;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long value) {
        this.playCount = value;
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer value) {
        this.discNumber = value;
    }

    public XMLGregorianCalendar getCreated() {
        return created;
    }

    public void setCreated(XMLGregorianCalendar value) {
        this.created = value;
    }

    public XMLGregorianCalendar getStarred() {
        return starred;
    }

    public void setStarred(XMLGregorianCalendar value) {
        this.starred = value;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String value) {
        this.albumId = value;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String value) {
        this.artistId = value;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType value) {
        this.type = value;
    }

    public Long getBookmarkPosition() {
        return bookmarkPosition;
    }

    public void setBookmarkPosition(Long value) {
        this.bookmarkPosition = value;
    }

    public Integer getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(Integer value) {
        this.originalWidth = value;
    }

    public Integer getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(Integer value) {
        this.originalHeight = value;
    }
}
