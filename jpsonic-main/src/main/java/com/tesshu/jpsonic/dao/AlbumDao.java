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
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.util.LegacyMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Provides database services for albums.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Only DAO is allowed to exclude this rule #827
@Repository
public class AlbumDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "path, name, artist, song_count, duration_seconds, cover_art_path, "
            + "year, genre, play_count, last_played, comment, created, last_scanned, present, "
            + "folder_id, mb_release_id, artist_sort, name_sort, artist_reading, name_reading, album_order";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final RowMapper<Album> rowMapper;

    public AlbumDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new AlbumMapper();
    }

    public @Nullable Album getAlbum(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from album where id=?", rowMapper, id);
    }

    public @Nullable Album getAlbum(String artistName, String albumName) {
        return queryOne("select " + QUERY_COLUMNS + " from album where artist=? and name=?", rowMapper, artistName,
                albumName);
    }

    public List<Album> getAlbumsForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders));
        return namedQuery("select " + QUERY_COLUMNS
                + " from album where artist = :artist and present and folder_id in (:folders) "
                + "order by album_order, name", rowMapper, args);
    }

    public List<Album> getAlbumsForArtist(final long offset, final long count, final String artist, boolean byYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders),
                "offset", offset, "count", count);
        return namedQuery("select " + QUERY_COLUMNS + " from album "
                + "where artist = :artist and present and folder_id in (:folders) order by "
                + (byYear ? "year" : "album_order") + ", name limit :count offset :offset", rowMapper, args);
    }

    public @Nullable Album createAlbum(Album album) {
        String query = "insert into album (" + INSERT_COLUMNS + ") values (?, ?, ?, "
                + "(select count(*) from media_file where parent_path = ? and (type=? or type=? or type=?)), "
                + "(select sum(duration_seconds) from media_file where parent_path = ? and (type=? or type=? or type=?)), "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int c = update(query, album.getPath(), album.getName(), album.getArtist(), album.getPath(),
                MediaType.MUSIC.name(), MediaType.PODCAST.name(), MediaType.AUDIOBOOK.name(), album.getPath(),
                MediaType.MUSIC.name(), MediaType.PODCAST.name(), MediaType.AUDIOBOOK.name(), album.getCoverArtPath(),
                album.getYear(), album.getGenre(), album.getPlayCount(), album.getLastPlayed(), album.getComment(),
                album.getCreated(), album.getLastScanned(), album.isPresent(), album.getFolderId(),
                album.getMusicBrainzReleaseId(), album.getArtistSort(), album.getNameSort(), album.getArtistReading(),
                album.getNameReading(), -1);
        if (c > 0) {
            return getAlbum(album.getArtist(), album.getName());
        }
        return null;
    }

    public @Nullable Album updateAlbum(Album album) {
        String sql = "update album set path=?, "
                + "song_count= (select count(*) from media_file where parent_path = ? and (type=? or type=? or type=?)),"
                + "duration_seconds= (select sum(duration_seconds) from media_file where parent_path = ? and (type=? or type=? or type=?)),"
                + "cover_art_path=?, year=?, genre=?, play_count=?, last_played=?, comment=?, created=?,"
                + "last_scanned=?, present=?, folder_id=?, mb_release_id=?, artist_sort=?, "
                + "name_sort=?, artist_reading=?, name_reading=?, album_order=?  where artist=? and name=?";
        int c = update(sql, album.getPath(), album.getPath(), MediaType.MUSIC.name(), MediaType.PODCAST.name(),
                MediaType.AUDIOBOOK.name(), album.getPath(), MediaType.MUSIC.name(), MediaType.PODCAST.name(),
                MediaType.AUDIOBOOK.name(), album.getCoverArtPath(), album.getYear(), album.getGenre(),
                album.getPlayCount(), album.getLastPlayed(), album.getComment(), album.getCreated(),
                album.getLastScanned(), album.isPresent(), album.getFolderId(), album.getMusicBrainzReleaseId(),
                album.getArtistSort(), album.getNameSort(), album.getArtistReading(), album.getNameReading(),
                album.getOrder(), album.getArtist(), album.getName());
        if (c > 0) {
            return getAlbum(album.getArtist(), album.getName());
        }
        return null;
    }

    public void updateOrder(String artist, String name, int order) {
        update("update album set album_order = ? where artist=? and name=?", order, artist, name);
    }

    public void updateCoverArtPath(String artist, String name, String coverArtPath) {
        update("update album set cover_art_path = ? where artist=? and name=?", coverArtPath, artist, name);
    }

    public void updatePlayCount(String artist, String name, Instant lastPlayed, int playCount) {
        update("update album set last_played = ?, play_count = ? where artist=? and name=?", lastPlayed, playCount,
                artist, name);
    }

    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        String orderBy;
        if (ignoreCase) {
            orderBy = byArtist ? "LOWER(artist_reading), album_order, LOWER(name_reading)"
                    : "album_order, LOWER(name_reading)";
        } else {
            orderBy = byArtist ? "artist_reading, album_order, name_reading" : "album_order, name_reading";
        }

        return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) "
                + "order by " + orderBy + " limit :count offset :offset", rowMapper, args);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        List<Integer> ids = musicFolders.stream().map(MusicFolder::getId).collect(Collectors.toList());
        Map<String, Object> args = LegacyMap.of("folders", ids);
        Integer result = getNamedParameterJdbcTemplate().queryForObject(
                "select count(*) from album where present and folder_id in (:folders)", args, Integer.class);
        if (result != null) {
            return result;
        }
        return 0;
    }

    public List<Album> getMostFrequentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        return namedQuery(
                "select " + QUERY_COLUMNS + " from album where play_count > 0 and present and folder_id in (:folders) "
                        + "order by play_count desc limit :count offset :offset",
                rowMapper, args);
    }

    public List<Album> getMostRecentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        return namedQuery("select " + QUERY_COLUMNS
                + " from album where last_played is not null and present and folder_id in (:folders) "
                + "order by last_played desc limit :count offset :offset", rowMapper, args);
    }

    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        return namedQuery("select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) "
                + "order by created desc limit :count offset :offset", rowMapper, args);
    }

    public List<Album> getStarredAlbums(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset, "username", username);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "album")
                + " from starred_album, album where album.id = starred_album.album_id and "
                + "album.present and album.folder_id in (:folders) and starred_album.username = :username "
                + "order by starred_album.created desc limit :count offset :offset", rowMapper, args);
    }

    public List<Album> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset, "fromYear", fromYear, "toYear", toYear);
        if (fromYear <= toYear) {
            return namedQuery(
                    "select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) "
                            + "and year between :fromYear and :toYear order by year limit :count offset :offset",
                    rowMapper, args);
        } else {
            return namedQuery(
                    "select " + QUERY_COLUMNS + " from album where present and folder_id in (:folders) "
                            + "and year between :toYear and :fromYear order by year desc limit :count offset :offset",
                    rowMapper, args);
        }
    }

    public void iterateLastScanned(@NonNull Instant scanDate, boolean withPodcast) {
        String query = "update album set last_scanned = ?, present = ? "
                + "where id in (select distinct al.id from album al join media_file child "
                + "on child.present and child.album = al.name and child.album_artist = al.artist "
                + "and (child.type=? or child.type=? " + (withPodcast ? "or child.type=?" : "") + ")) ";
        if (withPodcast) {
            update(query, scanDate, true, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(), MediaType.PODCAST.name());
        } else {
            update(query, scanDate, true, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name());
        }
    }

    public List<Integer> getExpungeCandidates(@NonNull Instant scanDate) {
        return queryForInts("select id from album where last_scanned <> ? or not present", scanDate);
    }

    public void expunge(@NonNull Instant scanDate) {
        update("delete from album where last_scanned <> ? or not present", scanDate);
    }

    public void deleteAll() {
        update("delete from album");
    }

    public void starAlbum(int albumId, String username) {
        unstarAlbum(albumId, username);
        update("insert into starred_album(album_id, username, created) values (?,?,?)", albumId, username, now());
    }

    public void unstarAlbum(int albumId, String username) {
        update("delete from starred_album where album_id=? and username=?", albumId, username);
    }

    public Instant getAlbumStarredDate(int albumId, String username) {
        return queryForInstant("select created from starred_album where album_id=? and username=?", null, albumId,
                username);
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders));
        return namedQueryForInt(
                "select count(id) from album where artist = :artist and present and folder_id in (:folders)", 0, args);
    }

    private static class AlbumMapper implements RowMapper<Album> {
        @Override
        public Album mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Album(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                    rs.getInt(6), rs.getString(7), rs.getInt(8) == 0 ? null : rs.getInt(8), rs.getString(9),
                    rs.getInt(10), nullableInstantOf(rs.getTimestamp(11)), rs.getString(12),
                    nullableInstantOf(rs.getTimestamp(13)), nullableInstantOf(rs.getTimestamp(14)), rs.getBoolean(15),
                    rs.getInt(16), rs.getString(17), rs.getString(18), rs.getString(19), rs.getString(20),
                    rs.getString(21), rs.getInt(22));
        }
    }
}
