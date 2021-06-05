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

package com.tesshu.jpsonic.service.metadata;

/**
 * Contains meta-data (song title, artist, album etc) for a music file.
 * 
 * @author Sindre Mehus
 */
public class MetaData {

    private Integer discNumber;
    private Integer trackNumber;
    private String title;
    private String artist;
    private String albumArtist;
    private String albumName;
    private String genre;
    private Integer year;
    private Integer bitRate;
    private boolean variableBitRate;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private String titleSort;
    private String albumSort;
    private String artistSort;
    private String albumArtistSort;
    private String musicBrainzReleaseId;
    private String musicBrainzRecordingId;
    private String composer;
    private String composerSort;

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isVariableBitRate() {
        return variableBitRate;
    }

    public void setVariableBitRate(boolean variableBitRate) {
        this.variableBitRate = variableBitRate;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getTitleSort() {
        return titleSort;
    }

    public void setTitleSort(String titleSort) {
        this.titleSort = titleSort;
    }

    public String getAlbumSort() {
        return albumSort;
    }

    public void setAlbumSort(String albumSort) {
        this.albumSort = albumSort;
    }

    public String getArtistSort() {
        return artistSort;
    }

    public void setArtistSort(String artistRort) {
        this.artistSort = artistRort;
    }

    public String getAlbumArtistSort() {
        return albumArtistSort;
    }

    public void setAlbumArtistSort(String albumArtistSort) {
        this.albumArtistSort = albumArtistSort;
    }

    public String getMusicBrainzReleaseId() {
        return musicBrainzReleaseId;
    }

    public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public String getMusicBrainzRecordingId() {
        return musicBrainzRecordingId;
    }

    public void setMusicBrainzRecordingId(String musicBrainzRecordingId) {
        this.musicBrainzRecordingId = musicBrainzRecordingId;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getComposerSort() {
        return composerSort;
    }

    public void setComposerSort(String composerSort) {
        this.composerSort = composerSort;
    }

}
