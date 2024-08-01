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

package com.tesshu.jpsonic.service.upnp.processor.composite;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;

public class FolderOrAlbum {

    private static final String TYPE_PREFIX_ALBUM = "album:";

    private final Object o;

    public FolderOrAlbum(MusicFolder folder) {
        this.o = folder;
    }

    public FolderOrAlbum(Album album) {
        this.o = album;
    }

    public MusicFolder getFolder() {
        return (MusicFolder) o;
    }

    public Album getAlbum() {
        return (Album) o;
    }

    public boolean isAlbum() {
        return o instanceof Album;
    }

    public String createCompositeId() {
        return TYPE_PREFIX_ALBUM.concat(Integer.toString(getAlbum().getId()));
    }

    public static boolean isAlbumId(String compositeId) {
        return compositeId.startsWith(TYPE_PREFIX_ALBUM);
    }

    public static int toId(String compositeId) {
        return Integer.parseInt(compositeId.replaceAll("^.*:", ""));
    }
}
