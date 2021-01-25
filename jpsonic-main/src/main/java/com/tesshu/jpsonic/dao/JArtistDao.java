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
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.util.LegacyMap;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

@Repository("jartistDao")
@DependsOn({ "artistDao" })
public class JArtistDao extends AbstractDao {

    private final ArtistDao deligate;

    public JArtistDao(ArtistDao deligate) {
        super();
        this.deligate = deligate;
    }

    public void clearOrder() {
        update("update artist set artist_order = -1");
        update("delete from artist where reading is null"); // #311
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
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders));
        return namedQueryForInt("select count(id) from artist " + "where present and folder_id in (:folders)", 0, args);
    }

    public List<Integer> getSortOfArtistToBeFixed(List<SortCandidate> candidates) {
        if (isEmpty(candidates) || 0 == candidates.size()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("names", candidates.stream().map(c -> c.getName()).collect(toList()),
                "sotes", candidates.stream().map(c -> c.getSort()).collect(toList()));
        return namedQuery(
                "select id from artist "
                        + "where present and name in (:names) and (sort is null or sort not in(:sotes)) order by id",
                (rs, rowNum) -> {
                    return rs.getInt(1);
                }, args);
    }

    public void updateArtistSort(SortCandidate candidate) {
        update("update artist set reading = ?, sort = ? "
                + "where present and name = ? and (sort <> ? or sort is null)", candidate.getReading(),
                candidate.getSort(), candidate.getName(), candidate.getSort());
    }

}
