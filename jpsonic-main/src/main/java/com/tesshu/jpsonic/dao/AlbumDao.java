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

import static com.tesshu.jpsonic.dao.base.DaoUtils.prefix;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.dao.base.DaoUtils;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.dao.dialect.DialectAlbumDao;
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
public class AlbumDao {

    private static final String INSERT_COLUMNS = DaoUtils.getInsertColumns(Album.class);
    private static final String QUERY_COLUMNS = DaoUtils.getQueryColumns(Album.class);

    private final RowMapper<Album> rowMapper = DaoUtils.createRowMapper(Album.class);
    private final TemplateWrapper template;

    private final DialectAlbumDao dialect;

    public AlbumDao(TemplateWrapper templateWrapper, DialectAlbumDao dialect) {
        template = templateWrapper;
        this.dialect = dialect;
    }

    public @Nullable Album getAlbum(int id) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from album
                where id=?
                """, rowMapper, id);
    }

    public @Nullable Album getAlbum(String artistName, String albumName) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from album
                where artist=? and name=?
                """, rowMapper, artistName, albumName);
    }

    public List<Album> getAlbumsForArtist(final long offset, final long count, final String artist, boolean byYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders),
                "offset", offset, "count", count);
        String orderWithYear = byYear ? "year is null, year, " : "";
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from album
                where artist = :artist and present and folder_id in (:folders)
                order by %s album_order
                limit :count offset :offset
                """.formatted(orderWithYear), rowMapper, args);
    }

    public @Nullable Album createAlbum(Album album) {
        String query = "insert into album (" + INSERT_COLUMNS + """
                ) values (?, ?, ?,
                        (select count(*)
                            from media_file
                            where parent_path = ? and (type=? or type=? or type=?)),
                        (select sum(duration_seconds)
                            from media_file
                            where parent_path = ? and (type=? or type=? or type=?)),
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        int c = template.update(query, album.getPath(), album.getName(), album.getArtist(), album.getPath(),
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
        String sql = """
                update album
                set path=?,
                        song_count=
                                (select count(*)
                                from media_file
                                where parent_path = ? and (type=? or type=? or type=?)),
                        duration_seconds=
                                (select sum(duration_seconds)
                                from media_file
                                where parent_path = ? and (type=? or type=? or type=?)),
                        cover_art_path=?, year=?, genre=?, play_count=?, last_played=?,
                        comment=?, created=?, last_scanned=?, present=?, folder_id=?,
                        mb_release_id=?, artist_sort=?, name_sort=?, artist_reading=?,
                        name_reading=?, album_order=?  where artist=? and name=?
                """;
        int c = template.update(sql, album.getPath(), album.getPath(), MediaType.MUSIC.name(), MediaType.PODCAST.name(),
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

    public int updateOrder(int id, int order) {
        return template.update("""
                update album
                set album_order = ?
                where id=?
                """, order, id);
    }

    public void updateCoverArtPath(String artist, String name, String coverArtPath) {
        template.update("""
                update album
                set cover_art_path = ?
                where artist=? and name=?
                """, coverArtPath, artist, name);
    }

    public void updatePlayCount(String artist, String name, Instant lastPlayed, int playCount) {
        template.update("""
                update album
                set last_played = ?, play_count = ?
                where artist=? and name=?
                """, lastPlayed, playCount, artist, name);
    }

    public List<Album> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, boolean ignoreCase,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);

        String join = "";
        String order;
        if (byArtist && ignoreCase) {
            join = "left join artist on artist.present and artist.name = album.artist";
            order = "artist_order, album_order";
        } else if (byArtist && !ignoreCase) {
            join = "left join artist on artist.present and artist.name = album.artist";
            order = "artist.reading, album.name_reading";
        } else if (!byArtist && ignoreCase) {
            order = "album_order";
        } else {
            order = "album.name_reading";
        }

        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "album") + """
                from album
                %s
                where album.present and album.folder_id in (:folders)
                order by %s
                limit :count offset :offset
                """.formatted(join, order), rowMapper, args);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        List<Integer> ids = musicFolders.stream().map(MusicFolder::getId).collect(Collectors.toList());
        Map<String, Object> args = LegacyMap.of("folders", ids);
        Integer result = template.getNamedParameterJdbcTemplate().queryForObject("""
                select count(*)
                from album
                where present and folder_id in (:folders)
                """, args, Integer.class);
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
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from album
                where play_count > 0 and present and folder_id in (:folders)
                order by play_count desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<Album> getMostRecentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from album
                where last_played is not null and present and folder_id in (:folders)
                order by last_played desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<Album> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from album
                where present and folder_id in (:folders)
                order by created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<Album> getStarredAlbums(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset, "username", username);
        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "album") + """
                from starred_album, album
                where album.id = starred_album.album_id and album.present
                        and album.folder_id in (:folders) and starred_album.username = :username
                order by starred_album.created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<Album> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toIdList(musicFolders), "count", count, "offset",
                offset, "fromYear", fromYear, "toYear", toYear);
        if (fromYear <= toYear) {
            return template.namedQuery("select " + QUERY_COLUMNS + """
                    from album
                    where present and folder_id in (:folders) and year between :fromYear and :toYear
                    order by year, album_order limit :count offset :offset
                    """, rowMapper, args);
        } else {
            return template.namedQuery("select " + QUERY_COLUMNS + """
                    from album
                    where present and folder_id in (:folders) and year between :toYear and :fromYear
                    order by year desc, album_order
                    limit :count offset :offset
                    """, rowMapper, args);
        }
    }

    public List<Album> getAlbumsByGenre(int offset, int count, List<String> genres, List<MusicFolder> folders) {
        return dialect.getAlbumsByGenre(offset, count, genres, folders);
    }

    public void iterateLastScanned(@NonNull Instant scanDate, boolean withPodcast) {
        String podcastQuery = withPodcast ? "or child.type=?" : "";
        String query = """
                update album
                set last_scanned = ?, present = ?
                where id in
                        (select distinct al.id
                            from album al
                            join media_file child
                            on child.present and child.album = al.name
                                    and child.album_artist = al.artist
                                    and (child.type=? or child.type=? %s))
                """.formatted(podcastQuery);
        if (withPodcast) {
            template.update(query, scanDate, true, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                    MediaType.PODCAST.name());
        } else {
            template.update(query, scanDate, true, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name());
        }
    }

    public List<Integer> getExpungeCandidates(@NonNull Instant scanDate) {
        return template.queryForInts("""
                select id
                from album
                where last_scanned <> ? or not present
                """, scanDate);
    }

    public void expunge(@NonNull Instant scanDate) {
        template.update("""
                delete from album
                where last_scanned <> ? or not present
                """, scanDate);
    }

    public void deleteAll() {
        template.update("delete from album");
    }

    public void starAlbum(int albumId, String username) {
        unstarAlbum(albumId, username);
        template.update("""
                insert into starred_album(album_id, username, created)
                values (?,?,?)
                """, albumId, username, now());
    }

    public void unstarAlbum(int albumId, String username) {
        template.update("""
                delete from starred_album
                where album_id=? and username=?
                """, albumId, username);
    }

    public Instant getAlbumStarredDate(int albumId, String username) {
        return template.queryForInstant("""
                select created
                from starred_album
                where album_id=? and username=?
                """, null, albumId, username);
    }

    public int getAlbumsCountForArtist(final String artist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "folders", MusicFolder.toIdList(musicFolders));
        return template.namedQueryForInt("""
                select count(id)
                from album
                where artist = :artist and present and folder_id in (:folders)
                """, 0, args);
    }
}
