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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import com.tesshu.jpsonic.domain.MediaFile;
import org.subsonic.restapi.IndexID3;

public interface Id3Wrapper {

    boolean isIndex();

    boolean isAlbum();

    boolean isArtist();

    boolean isSong();

    String getId();

    String getName();

    String getArtist();

    String getComment();

    IndexID3 getIndexId3();

    MediaFile getSong();

    int getChildSize();

}
