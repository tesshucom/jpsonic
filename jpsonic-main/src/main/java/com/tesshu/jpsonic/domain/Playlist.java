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

import com.tesshu.jpsonic.util.StringUtil;

/**
 * @author Sindre Mehus
 */
public class Playlist {

    private int id;
    private String username;
    private boolean shared;
    private String name;
    private String comment;
    private int fileCount;
    private int durationSeconds;
    private Date created;
    private Date changed;
    private String importedFrom;
    private transient String reading;

    public Playlist() {
    }

    public Playlist(int id, String username, boolean shared, String name, String comment, int fileCount,
            int durationSeconds, Date created, Date changed, String importedFrom) {
        this.id = id;
        this.username = username;
        this.shared = shared;
        this.name = name;
        this.comment = comment;
        this.fileCount = fileCount;
        this.durationSeconds = durationSeconds;
        this.created = created;
        this.changed = changed;
        this.importedFrom = importedFrom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDurationAsString() {
        return StringUtil.formatDurationMSS(durationSeconds);
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getChanged() {
        return changed;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }

    public String getImportedFrom() {
        return importedFrom;
    }

    public void setImportedFrom(String importedFrom) {
        this.importedFrom = importedFrom;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

}
