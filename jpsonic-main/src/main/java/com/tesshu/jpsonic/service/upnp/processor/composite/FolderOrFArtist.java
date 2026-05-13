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

import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;

public final class FolderOrFArtist {

    private final Object o;

    public FolderOrFArtist(MusicFolder folder) {
        this.o = folder;
    }

    public FolderOrFArtist(FolderArtist folderArtist) {
        this.o = folderArtist;
    }

    public MusicFolder getFolder() {
        return (MusicFolder) o;
    }

    public FolderArtist getFolderArtist() {
        return (FolderArtist) o;
    }

    public boolean isFolderArtist() {
        return o instanceof FolderArtist;
    }
}
