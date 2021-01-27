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

/**
 * Represents a musical genre.
 *
 * @author Sindre Mehus
 * 
 * @version $Revision: 1.2 $ $Date: 2005/12/25 13:48:46 $
 */
public class Genre {

    private final String name;
    private transient String reading;
    private int songCount;
    private int albumCount;

    public Genre(String name) {
        this.name = name;
    }

    public Genre(String name, int songCount, int albumCount) {
        this.name = name;
        this.songCount = songCount;
        this.albumCount = albumCount;
    }

    public String getName() {
        return name;
    }

    public int getSongCount() {
        return songCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void incrementAlbumCount() {
        albumCount++;
    }

    public void incrementSongCount() {
        songCount++;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

}
