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

package org.airsonic.player.domain;

import java.util.Date;

/**
 * An icon representing a user.
 *
 * @author Sindre Mehus
 */
public class Avatar {

    private int id;
    private String name;
    private Date createdDate;
    private String mimeType;
    private int width;
    private int height;
    private byte[] data;

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    /*
     * False positive. The caller is guaranteed and cloning is a waste of time.
     */
    public Avatar(int id, String name, Date createdDate, String mimeType, int width, int height, byte[] data) {
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getData() {
        return data;
    }
}
