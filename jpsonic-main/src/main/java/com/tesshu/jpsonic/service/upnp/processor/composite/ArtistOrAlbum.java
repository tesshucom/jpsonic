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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor.composite;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;

public class ArtistOrAlbum {

    private final Object o;

    public ArtistOrAlbum(Artist artist) {
        this.o = artist;
    }

    public ArtistOrAlbum(Album album) {
        this.o = album;
    }

    public Artist getArtist() {
        return (Artist) o;
    }

    public Album getAlbum() {
        return (Album) o;
    }

    public boolean isArtist() {
        return o instanceof Artist;
    }
}
