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

package com.tesshu.jpsonic.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.stereotype.Repository;

@Repository("jalbumDao")
public class JAlbumDao extends AbstractDao {

    private final AlbumDao deligate;

    public JAlbumDao(DaoHelper daoHelper, AlbumDao deligate) {
        super(daoHelper);
        this.deligate = deligate;
    }

    public Album getAlbum(int id) {
        return deligate.getAlbum(id);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        return deligate.getAlbumCount(musicFolders);
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders));
        return namedQueryForInt(
                "select count(id) from album " + "where artist = :artist and present and folder_id in (:folders)", 0,
                args);
    }

    public List<Album> getAlbumsForArtist(final long offset, final long count, final String artist, boolean byYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders),
                "offset", offset, "count", count);
        return namedQuery(
                "select " + deligate.getQueryColoms() + " from album "
                        + "where artist = :artist and present and folder_id in (:folders) " + "    order by "
                        + (byYear ? "year" : "album_order") + ", name limit :count offset :offset",
                deligate.getAlbumMapper(), args);
    }

    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase,
            final List<MusicFolder> musicFolders) {
        return deligate.getAlphabeticalAlbums(offset, count, byArtist, ignoreCase, musicFolders);
    }

    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        return deligate.getNewestAlbums(offset, count, musicFolders);
    }
}
