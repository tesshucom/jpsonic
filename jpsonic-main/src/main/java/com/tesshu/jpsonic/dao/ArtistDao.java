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

import static com.tesshu.jpsonic.dao.base.DaoUtils.nullableInstantOf;
import static com.tesshu.jpsonic.dao.base.DaoUtils.prefix;
import static com.tesshu.jpsonic.dao.base.DaoUtils.questionMarks;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicIndex;
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
public class ArtistDao {

    private static final String INSERT_COLUMNS = """
            name, cover_art_path, album_count, last_scanned, present, folder_id,
            sort, reading, artist_order, music_index\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    private final TemplateWrapper template;
    private final RowMapper<Artist> rowMapper;
    private final RowMapper<IndexWithCount> indexWithCountMapper = (ResultSet rs,
            int num) -> new IndexWithCount(rs.getString(1), rs.getInt(2));

    public ArtistDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        rowMapper = new ArtistMapper();
    }

    public @Nullable Artist getArtist(String artistName) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from artist
                where name=?
                """, rowMapper, artistName);
    }

    public @Nullable Artist getArtist(final String artistName,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = LegacyMap
            .of("name", artistName, "folders", MusicFolder.toIdList(musicFolders));

        return template.namedQueryOne("select " + QUERY_COLUMNS + """
                from artist
                where name = :name and folder_id in (:folders)
                """, rowMapper, args);
    }

    public @Nullable Artist getArtist(int id) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from artist
                where id=?
                """, rowMapper, id);
    }

    public List<Artist> getArtists(MusicIndex musicIndex, List<MusicFolder> folders, long offset,
            long count) {
        return template.query("select " + QUERY_COLUMNS + """
                from artist
                where music_index=?
                order by artist_order
                offset ? limit ?
                """, rowMapper, musicIndex.getIndex(), offset, count);
    }

    public @Nullable Artist updateArtist(Artist artist) {
        String sql = """
                update artist
                set cover_art_path=?, album_count=?, last_scanned=?, present=?,
                        folder_id=?, sort=?, reading=?, music_index = ?
                where name=?
                """;
        int c = template
            .update(sql, artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(),
                    artist.isPresent(), artist.getFolderId(), artist.getSort(), artist.getReading(),
                    artist.getMusicIndex(), artist.getName());
        if (c > 0) {
            return artist;
        }
        return null;
    }

    public @Nullable Artist createArtist(Artist artist) {
        int c = template
            .update("insert into artist (" + INSERT_COLUMNS + ") values ("
                    + questionMarks(INSERT_COLUMNS) + ")", artist.getName(),
                    artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(),
                    artist.isPresent(), artist.getFolderId(), artist.getSort(), artist.getReading(),
                    -1, artist.getMusicIndex());
        Integer id = template.queryForInt("""
                select id
                from artist
                where name=?
                """, null, artist.getName());
        if (c > 0 && id != null) {
            artist.setId(id);
            return artist;
        }
        return null;
    }

    public int updateOrder(int id, int order) {
        return template.update("""
                update artist
                set artist_order=?
                where id=?
                """, order, id);
    }

    public List<Artist> getAlphabetialArtists(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap
            .of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset", offset);

        return template.namedQuery("select " + QUERY_COLUMNS + """
                from artist
                where present and folder_id in (:folders)
                order by artist_order
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<Artist> getStarredArtists(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap
            .of("folders", MusicFolder.toIdList(musicFolders), "username", username, "count", count,
                    "offset", offset);

        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "artist") + """
                from starred_artist, artist
                where artist.id = starred_artist.artist_id and artist.present
                        and starred_artist.username = :username and artist.folder_id in (:folders)
                order by starred_artist.created desc limit :count offset :offset
                """, rowMapper, args);
    }

    public void iterateLastScanned(@NonNull Instant scanDate, boolean withPodcast) {
        String podcastQuery = withPodcast ? "or media_file.type = ?" : "";
        String query = """
                update artist
                set present = ?, last_scanned = ?
                where artist.id in
                        (select distinct artist.id
                        from artist
                        join media_file
                        on (media_file.type = ? or media_file.type = ? %s)
                                and artist.name = media_file.album_artist
                                and media_file.present)
                """.formatted(podcastQuery);
        if (withPodcast) {
            template
                .update(query, true, scanDate, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                        MediaType.PODCAST.name());
        } else {
            template
                .update(query, true, scanDate, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name());
        }
    }

    public List<Integer> getExpungeCandidates(@NonNull Instant scanDate) {
        return template
            .queryForInts("""
                    select id from artist
                    where last_scanned <> ? or not present or name not in
                            (select distinct album_artist
                            from media_file
                            where present and media_file.type = ? or
                                    media_file.type = ? or media_file.type = ?)
                    """, scanDate, MediaType.MUSIC.name(), MediaType.PODCAST.name(),
                    MediaType.AUDIOBOOK.name());
    }

    public void expunge(@NonNull Instant scanDate) {
        template.update("""
                delete from artist
                where last_scanned <> ? or not present
                """, scanDate);
    }

    public void deleteAll() {
        template.update("delete from artist");
    }

    public void starArtist(int artistId, String username) {
        unstarArtist(artistId, username);
        template.update("""
                insert into starred_artist(artist_id, username, created)
                values (?,?,?)
                """, artistId, username, now());
    }

    public void unstarArtist(int artistId, String username) {
        template.update("""
                delete from starred_artist
                where artist_id=? and username=?
                """, artistId, username);
    }

    public @Nullable Instant getArtistStarredDate(int artistId, String username) {
        return template.queryForInstant("""
                select created
                from starred_artist
                where artist_id=? and username=?
                """, null, artistId, username);
    }

    public List<Artist> getAlbumCounts() {
        return template.query("""
                select artist.id, count(album.id) as album_count
                from artist
                join album
                on artist.name = album.artist
                group by artist.id, artist.name
                """, new ArtistCountMapper());
    }

    public void updateAlbumCount(int id, int count) {
        template.update("""
                update artist
                set album_count=?
                where id=?
                """, count, id);
    }

    public int getMudicIndexCount(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap
            .of("folders", folders.stream().map(MusicFolder::getId).toList());
        Integer result = template.getNamedParameterJdbcTemplate().queryForObject("""
                select count(distinct music_index)
                from artist
                where present and folder_id in (:folders)
                """, args, Integer.class);
        if (result != null) {
            return result;
        }
        return 0;
    }

    public List<IndexWithCount> getMudicIndexCounts(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap
            .of("folders", folders.stream().map(MusicFolder::getId).toList());
        return template.namedQuery("""
                select distinct music_index, count(music_index)
                from artist
                where present and folder_id in(:folders)
                group by music_index
                order by music_index
                """, indexWithCountMapper, args);
    }

    private static class ArtistMapper implements RowMapper<Artist> {
        @Override
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                    nullableInstantOf(rs.getTimestamp(5)), rs.getBoolean(6), rs.getInt(7),
                    rs.getString(8), rs.getString(9), rs.getInt(10), rs.getString(11));
        }
    }

    private static class ArtistCountMapper implements RowMapper<Artist> {
        @Override
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(rs.getInt(1), null, null, rs.getInt(2), null, false, null, null, null,
                    -1, "");
        }
    }

    public int getArtistsCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders));
        return template.namedQueryForInt("""
                select count(id)
                from artist
                where present and folder_id in (:folders)
                """, 0, args);
    }

    public record IndexWithCount(String index, int artistCount) {
    }
}
