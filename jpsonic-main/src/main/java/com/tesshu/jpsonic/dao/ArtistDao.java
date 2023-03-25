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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.util.LegacyMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Provides database services for artists.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Only DAO is allowed to exclude this rule #827
@Repository
public class ArtistDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "name, cover_art_path, album_count, last_scanned, present, folder_id, "
            // JP >>>>
            + "sort, reading, artist_order"; // <<<< JP
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final RowMapper<Artist> rowMapper;

    public ArtistDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new ArtistMapper();
    }

    /**
     * Returns the artist with the given name.
     *
     * @param artistName
     *            The artist name.
     *
     * @return The artist or null.
     */
    public Artist getArtist(String artistName) {
        return queryOne("select " + QUERY_COLUMNS + " from artist where name=?", rowMapper, artistName);
    }

    /**
     * Returns the artist with the given name.
     *
     * @param artistName
     *            The artist name.
     * @param musicFolders
     *            Only return artists that have at least one album in these folders.
     *
     * @return The artist or null.
     */
    public Artist getArtist(final String artistName, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = LegacyMap.of("name", artistName, "folders", MusicFolder.toIdList(musicFolders));

        return namedQueryOne("select " + QUERY_COLUMNS + " from artist where name = :name and folder_id in (:folders)",
                rowMapper, args);
    }

    /**
     * Returns the artist with the given ID.
     *
     * @param id
     *            The artist ID.
     *
     * @return The artist or null.
     */
    public Artist getArtist(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from artist where id=?", rowMapper, id);
    }

    public @Nullable Artist updateArtist(Artist artist) {
        String sql = "update artist set " + "cover_art_path=?," + "album_count=?," + "last_scanned=?," + "present=?,"
                + "folder_id=?, sort=?, " + "reading=?," + "artist_order=? where name=?";
        int c = update(sql, artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(),
                artist.isPresent(), artist.getFolderId(), artist.getSort(), artist.getReading(), artist.getOrder(),
                artist.getName());
        if (c > 0) {
            return artist;
        }
        return null;
    }

    public @Nullable Artist createArtist(Artist artist) {
        int c = update("insert into artist (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                artist.getName(), artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(),
                artist.isPresent(), artist.getFolderId(), artist.getSort(), artist.getReading(), -1);
        Integer id = queryForInt("select id from artist where name=?", null, artist.getName());
        if (c > 0 && id != null) {
            artist.setId(id);
            return artist;
        }
        return null;
    }

    public void updateOrder(String name, int order) {
        update("update artist set artist_order=? where name=?", order, name);
    }

    /**
     * Returns artists in alphabetical order.
     *
     * @param offset
     *            Number of artists to skip.
     * @param count
     *            Maximum number of artists to return.
     * @param musicFolders
     *            Only return artists that have at least one album in these folders.
     *
     * @return Artists in alphabetical order.
     */
    public List<Artist> getAlphabetialArtists(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);

        return namedQuery("select " + QUERY_COLUMNS + " from artist where present and folder_id in (:folders) "
                + "order by artist_order, reading, name limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently starred artists.
     *
     * @param offset
     *            Number of artists to skip.
     * @param count
     *            Maximum number of artists to return.
     * @param username
     *            Returns artists starred by this user.
     * @param musicFolders
     *            Only return artists that have at least one album in these folders.
     *
     * @return The most recently starred artists for this user.
     */
    public List<Artist> getStarredArtists(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "username", username,
                "count", count, "offset", offset);

        return namedQuery("select " + prefix(QUERY_COLUMNS, "artist") + " from starred_artist, artist "
                + "where artist.id = starred_artist.artist_id and "
                + "artist.present and starred_artist.username = :username and " + "artist.folder_id in (:folders) "
                + "order by starred_artist.created desc limit :count offset :offset", rowMapper, args);
    }

    public void iterateLastScanned(@NonNull Instant scanDate, boolean withPodcast) {
        String query = "update artist set present = ?, last_scanned = ? "
                + "where artist.id in(select distinct artist.id from artist "
                + "join media_file on (media_file.type = ? or media_file.type = ? "
                + (withPodcast ? "or media_file.type = ?" : "") + ") "
                + "and artist.name = media_file.album_artist and media_file.present)";
        if (withPodcast) {
            update(query, true, scanDate, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(), MediaType.PODCAST.name());
        } else {
            update(query, true, scanDate, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name());
        }
    }

    public List<Integer> getExpungeCandidates(@NonNull Instant scanDate) {
        return queryForInts(
                "select id from artist where last_scanned <> ? or not present or "
                        + "name not in (select distinct album_artist from media_file where present "
                        + "and media_file.type = ? or media_file.type = ? or media_file.type = ?)",
                scanDate, MediaType.MUSIC.name(), MediaType.PODCAST.name(), MediaType.AUDIOBOOK.name());
    }

    public void expunge(@NonNull Instant scanDate) {
        update("delete from artist where last_scanned <> ? or not present", scanDate);
    }

    public void setNonPresentAll() {
        update("update artist set present = ?", false);
    }

    public void starArtist(int artistId, String username) {
        unstarArtist(artistId, username);
        update("insert into starred_artist(artist_id, username, created) values (?,?,?)", artistId, username, now());
    }

    public void unstarArtist(int artistId, String username) {
        update("delete from starred_artist where artist_id=? and username=?", artistId, username);
    }

    public Instant getArtistStarredDate(int artistId, String username) {
        return queryForInstant("select created from starred_artist where artist_id=? and username=?", null, artistId,
                username);
    }

    public List<Artist> getAlbumCounts() {
        return query(
                "select artist.id, count(album.id) as album_count from artist join album on artist.name = album.artist "
                        + "group by artist.id, artist.name",
                new ArtistCountMapper());
    }

    public void updateAlbumCount(int id, int count) {
        update("update artist set album_count=? where id=?", count, id);
    }

    private static class ArtistMapper implements RowMapper<Artist> {
        @Override
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                    nullableInstantOf(rs.getTimestamp(5)), rs.getBoolean(6), rs.getInt(7),
                    // JP >>>>
                    rs.getString(8), rs.getString(9), rs.getInt(10)); // <<<< JP
        }
    }

    private static class ArtistCountMapper implements RowMapper<Artist> {
        @Override
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(rs.getInt(1), null, null, rs.getInt(2), null, false, null, null, null, -1);
        }
    }

    public String getQueryColoms() {
        return QUERY_COLUMNS;
    }

}
