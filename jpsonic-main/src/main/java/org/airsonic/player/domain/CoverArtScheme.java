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
 * Enumeration of cover art schemes. Each value contains a size, which indicates how big the scaled covert art images
 * should be.
 *
 * @author Sindre Mehus
 */
public enum CoverArtScheme {

    OFF(0), SMALL(110), MEDIUM(160), LARGE(300);

    private final int size;

    CoverArtScheme(int size) {
        this.size = size;
    }

    /**
     * Returns the covert art size for this scheme.
     *
     * @return the covert art size for this scheme.
     */
    public int getSize() {
        return size;
    }

}
