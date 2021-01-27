/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2020 (C) tesshu.com
 */

package com.tesshu.jpsonic.dao;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.SortCandidate;
import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.LegacyMap;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

@Repository("jalbumDao")
@DependsOn({ "albumDao" })
public class JAlbumDao extends AbstractDao {

    private final AlbumDao deligate;

    public JAlbumDao(AlbumDao deligate) {
        super();
        this.deligate = deligate;
    }

    public void clearOrder() {
        update("update album set album_order = -1");
        update("delete from album where name_reading is null or artist_reading is null "); // #311
    }

    public void createOrUpdateAlbum(Album album) {
        deligate.createOrUpdateAlbum(album);
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

    public List<Integer> getSortOfAlbumToBeFixed(List<SortCandidate> candidates) {
        if (isEmpty(candidates) || 0 == candidates.size()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("names", candidates.stream().map(c -> c.getName()).collect(toList()),
                "sotes", candidates.stream().map(c -> c.getSort()).collect(toList()));
        return namedQuery("select id from album " + "where present and name in (:names) "
                + "and (name_sort is null or name_sort not in(:sotes)) order by id", (rs, rowNum) -> {
                    return rs.getInt(1);
                }, args);
    }

    public List<Integer> getSortOfArtistToBeFixed(List<SortCandidate> candidates) {
        if (isEmpty(candidates) || 0 == candidates.size()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("names", candidates.stream().map(c -> c.getName()).collect(toList()),
                "sotes", candidates.stream().map(c -> c.getSort()).collect(toList()));
        return namedQuery("select id from album " + "where present and artist in (:names) "
                + "    and (artist_sort is null or artist_sort not in(:sotes)) order by id", (rs, rowNum) -> {
                    return rs.getInt(1);
                }, args);
    }

    public void updateAlbumSort(SortCandidate candidate) {
        update("update album set name_reading = ?, name_sort = ? "
                + "where present and name = ? and (name_sort <> ? or name_sort is null)", candidate.getReading(),
                candidate.getSort(), candidate.getName(), candidate.getSort());
    }

    public void updateArtistSort(SortCandidate candidate) {
        update("update album set artist_reading = ?, artist_sort = ? "
                + "where present and artist = ? and (artist_sort <> ? or artist_sort is null)", candidate.getReading(),
                candidate.getSort(), candidate.getName(), candidate.getSort());
    }

}
