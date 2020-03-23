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

import com.tesshu.jpsonic.domain.SortCandidate;

import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("jalbumDao")
@DependsOn({ "albumDao" })
public class JAlbumDao extends AbstractDao {

    private final AlbumDao deligate;

    public JAlbumDao(AlbumDao deligate) {
        super();
        this.deligate = deligate;
    }

    public void clearOrder() {
        update("update album set _order = -1");
        update("delete from album where name_reading is null or artist_reading is null ");// #311
    }

    public void createOrUpdateAlbum(Album album) {
        deligate.createOrUpdateAlbum(album);
    }

    public Album getAlbum(int id) {
        return deligate.getAlbum(id);
    }

    @Deprecated
    public Album getAlbum(String artistName, String albumName) {
        return deligate.getAlbum(artistName, albumName);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        return deligate.getAlbumCount(musicFolders);
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("artist", artist);
        args.put("folders", MusicFolder.toIdList(musicFolders));
        return namedQueryForInt("select count(id) from album where artist = :artist and present and folder_id in (:folders)", 0, args);
    }

    public List<Album> getAlbumsForArtist(final long offset, final long count, final String artist, boolean byYear, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("artist", artist);
        args.put("folders", MusicFolder.toIdList(musicFolders));
        args.put("offset", offset);
        args.put("count", count);
        // @formatter:off
        return namedQuery("select " + deligate.getQueryColoms() +
                " from album where artist = :artist and present and folder_id in (:folders) " +
                "order by " + (byYear ? "year" : "_order") + ", name limit :count offset :offset",
                deligate.getAlbumMapper(), args);
        // @formatter:on
    }

    @Deprecated
    public List<Album> getAlbumsForArtist(final String artist, final List<MusicFolder> musicFolders) {
        return deligate.getAlbumsForArtist(artist, musicFolders);
    }

    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase, final List<MusicFolder> musicFolders) {
        return deligate.getAlphabeticalAlbums(offset, count, byArtist, ignoreCase, musicFolders);
    }

    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        return deligate.getNewestAlbums(offset, count, musicFolders);
    }

    public void updateArtistSort(SortCandidate candidate) { // @formatter:off
        update("update album set artist_reading = ?, artist_sort = ? where present and artist = ? and (artist_sort <> ? or artist_sort is null)",
                candidate.getReading(),
                candidate.getSort(),
                candidate.getName(),
                candidate.getSort());
    } // @formatter:on

}
