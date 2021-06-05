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

import java.util.Date;

/**
 * @author Sindre Mehus
 */
public class Album {

    private int id;
    private String path;
    private String name;
    private String artist;
    private int songCount;
    private int durationSeconds;
    private String coverArtPath;
    private Integer year;
    private String genre;
    private int playCount;
    private Date lastPlayed;
    private String comment;
    private Date created;
    private Date lastScanned;
    private boolean present;
    private Integer folderId;
    private String musicBrainzReleaseId;

    // JP >>>>

    // Tags newly supported by Jpsonic.
    private String artistSort;
    private String nameSort;

    // Cleansing or analysis results.
    private String artistReading;
    private String nameReading;

    // Default is -1. Registered when scanning by if option selected.
    private int order;

    // <<<< JP

    public Album() {
    }

    public Album(int id, String path, String name, String artist, int songCount, int durationSeconds,
            String coverArtPath, Integer year, String genre, int playCount, Date lastPlayed, String comment,
            Date created, Date lastScanned, boolean present, Integer folderId, String musicBrainzReleaseId,
            // JP >>>>
            String artistSort, String nameSort, String artistReading, String nameReading, int order // <<<< JP
    ) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.artist = artist;
        this.songCount = songCount;
        this.durationSeconds = durationSeconds;
        this.coverArtPath = coverArtPath;
        this.year = year;
        this.genre = genre;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.lastScanned = lastScanned;
        this.folderId = folderId;
        this.present = present;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
        // JP >>>>
        this.artistSort = artistSort;
        this.nameSort = nameSort;
        this.artistReading = artistReading;
        this.nameReading = nameReading;
        this.order = order;
        // <<<< JP
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Date lastScanned) {
        this.lastScanned = lastScanned;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public String getMusicBrainzReleaseId() {
        return musicBrainzReleaseId;
    }

    public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public String getArtistSort() {
        return artistSort;
    }

    public void setArtistSort(String artistSort) {
        this.artistSort = artistSort;
    }

    public String getNameSort() {
        return nameSort;
    }

    public void setNameSort(String nameSort) {
        this.nameSort = nameSort;
    }

    public String getArtistReading() {
        return artistReading;
    }

    public void setArtistReading(String artistReading) {
        this.artistReading = artistReading;
    }

    public String getNameReading() {
        return nameReading;
    }

    public void setNameReading(String nameReading) {
        this.nameReading = nameReading;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
