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

package com.tesshu.jpsonic.service.upnp.composite;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MusicFolder;

public final class FolderOrArtist {

    private static final String TYPE_PREFIX_ARTIST = "artist:";

    private final Object o;

    public FolderOrArtist(MusicFolder folder) {
        this.o = folder;
    }

    public FolderOrArtist(Artist artist) {
        this.o = artist;
    }

    public MusicFolder getFolder() {
        return (MusicFolder) o;
    }

    public Artist getArtist() {
        return (Artist) o;
    }

    public boolean isArtist() {
        return o instanceof Artist;
    }

    public String createCompositeId() {
        return TYPE_PREFIX_ARTIST.concat(Integer.toString(getArtist().getId()));
    }

    public static boolean isArtistId(String compositeId) {
        return compositeId.startsWith(TYPE_PREFIX_ARTIST);
    }

    public static int toId(String compositeId) {
        return Integer.parseInt(compositeId.replaceAll("^.*:", ""));
    }
}
