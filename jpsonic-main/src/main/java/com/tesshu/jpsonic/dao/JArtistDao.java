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

import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("jartistDao")
@DependsOn({ "artistDao" })
public class JArtistDao extends AbstractDao {

    private static class SortCandidateMapper implements RowMapper<Artist> {
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            Artist artist = new Artist();
            artist.setName(rs.getString(1));
            artist.setSort(rs.getString(2));
            return artist;
        }
    }

    private final ArtistDao deligate;
    private final RowMapper<Artist> rowMapper;
    private final RowMapper<Artist> sortCandidateMapper;

    public JArtistDao(ArtistDao deligate) {
        super();
        this.deligate = deligate;
        rowMapper = deligate.getArtistMapper();
        sortCandidateMapper = new SortCandidateMapper();
    }

    public void clearOrder() {
        update("update artist set _order = -1");
        update("delete from artist where reading is null");// #311
    }

    public void createOrUpdateArtist(Artist artist) {
        deligate.createOrUpdateArtist(artist);
    }

    public List<Artist> getAlphabetialArtists(final int offset, final int count, final List<MusicFolder> musicFolders) {
        return deligate.getAlphabetialArtists(offset, count, musicFolders);
    }

    public Artist getArtist(int id) {
        return deligate.getArtist(id);
    }

    public Artist getArtist(String artistName) {
        return deligate.getArtist(artistName);
    }

    public int getArtistsCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(musicFolders));
        return namedQueryForInt("select count(id) from artist where present and folder_id in (:folders)", 0, args);
    }

    public List<Artist> getSortCandidate() { // @formatter:off
        return query("select distinct a.name ,m.album_artist_sort from artist a \n" +
                " join media_file m " +
                " on a.name = m.album_artist " +
                " where  " +
                " a.reading is not null and a.sort is null " +
                " and a.present and m.present and m.type=? " +
                " and m.album_artist_sort is not null " +
                " and m.artist_sort <> m.album_artist_sort " +
                " and a.reading <> m.album_artist_sort " +
                " and m.album_artist = a.name ",
                sortCandidateMapper, MediaFile.MediaType.MUSIC.name());
    } // @formatter:on

    public List<Artist> getSortedArtists() { // @formatter:off
        return query("select " + deligate.getQueryColoms() +
                " from artist" +
                " where" +
                " reading is not null" +
                " or sort is not null" +
                " and present",
                rowMapper);
    } // @formatter:on

}
