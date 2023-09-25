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

import static com.tesshu.jpsonic.dao.DaoUtils.nullableInstantOf;
import static com.tesshu.jpsonic.dao.DaoUtils.prefix;
import static com.tesshu.jpsonic.dao.DaoUtils.questionMarks;
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_FUTURE;
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.domain.SortCandidate.CandidateField;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals", "PMD.TooManyStaticImports" })
@Repository
public class MediaFileDao {

    private static final String INSERT_COLUMNS = """
            path, folder, type, format, title, album, artist, album_artist, disc_number,
            track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds,
            file_size, width, height, cover_art_path, parent_path, play_count, last_played,
            comment, created, changed, last_scanned, children_last_updated, present, version,
            mb_release_id, mb_recording_id, composer, artist_sort, album_sort, title_sort,
            album_artist_sort, composer_sort, artist_reading, album_reading, album_artist_reading,
            artist_sort_raw, album_sort_raw, album_artist_sort_raw, composer_sort_raw,
            media_file_order\s
            """;
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    // Expected maximum number of album child elements (can be expanded)
    private static final int ALBUM_CHILD_MAX = 10_000;

    private static final int JP_VERSION = 9;
    public static final int VERSION = 4 + JP_VERSION;

    private final TemplateWrapper template;
    private final RowMapper<MediaFile> rowMapper = (resultSet, rowNum) -> new MediaFile(resultSet.getInt(1),
            resultSet.getString(2), resultSet.getString(3), MediaFile.MediaType.valueOf(resultSet.getString(4)),
            resultSet.getString(5), resultSet.getString(6), resultSet.getString(7), resultSet.getString(8),
            resultSet.getString(9), resultSet.getInt(10) == 0 ? null : resultSet.getInt(10),
            resultSet.getInt(11) == 0 ? null : resultSet.getInt(11),
            resultSet.getInt(12) == 0 ? null : resultSet.getInt(12), resultSet.getString(13),
            resultSet.getInt(14) == 0 ? null : resultSet.getInt(14), resultSet.getBoolean(15),
            resultSet.getInt(16) == 0 ? null : resultSet.getInt(16),
            resultSet.getLong(17) == 0 ? null : resultSet.getLong(17),
            resultSet.getInt(18) == 0 ? null : resultSet.getInt(18),
            resultSet.getInt(19) == 0 ? null : resultSet.getInt(19), resultSet.getString(20), resultSet.getString(21),
            resultSet.getInt(22), nullableInstantOf(resultSet.getTimestamp(23)), resultSet.getString(24),
            nullableInstantOf(resultSet.getTimestamp(25)), nullableInstantOf(resultSet.getTimestamp(26)),
            nullableInstantOf(resultSet.getTimestamp(27)), nullableInstantOf(resultSet.getTimestamp(28)),
            resultSet.getBoolean(29), resultSet.getInt(30), resultSet.getString(31), resultSet.getString(32),
            resultSet.getString(33), resultSet.getString(34), resultSet.getString(35), resultSet.getString(36),
            resultSet.getString(37), resultSet.getString(38), resultSet.getString(39), resultSet.getString(40),
            resultSet.getString(41), resultSet.getString(42), resultSet.getString(43), resultSet.getString(44),
            resultSet.getString(45), resultSet.getInt(46));
    private final RowMapper<MediaFile> iRowMapper;
    private final RowMapper<MediaFile> artistId3Mapper = (resultSet, rowNum) -> new MediaFile(-1, null,
            resultSet.getString(1), null, null, null, null, null, resultSet.getString(2), null, null, null, null, null,
            false, null, null, null, null, resultSet.getString(5), null, -1, null, null, null, null, null, null, false,
            -1, null, null, null, null, null, null, resultSet.getString(4), null, null, null, resultSet.getString(3),
            null, null, null, null, -1);
    private final RowMapper<Genre> genreRowMapper = (rs, rowNum) -> new Genre(rs.getString(1), rs.getInt(2),
            rs.getInt(3));
    private final RowMapper<SortCandidate> sortCandidateMapper = (rs, rowNum) -> new SortCandidate(rs.getInt(1),
            rs.getString(2), rs.getString(3));
    private final RowMapper<SortCandidate> sortCandidateWithIdMapper = (rs, rowNum) -> new SortCandidate(rs.getInt(1),
            rs.getString(2), rs.getString(3), rs.getInt(4));

    public MediaFileDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        iRowMapper = (resultSet, rowNum) -> {
            MediaFile mediaFile = rowMapper.mapRow(resultSet, rowNum);
            if (mediaFile != null) {
                mediaFile.setRownum(resultSet.getInt("irownum"));
            }
            return mediaFile;
        };
    }

    public @Nullable MediaFile getMediaFile(String path) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from media_file
                where path=?
                """, rowMapper, path);
    }

    public @Nullable MediaFile getMediaFile(@NonNull Path path) {
        return template.queryOne("select " + QUERY_COLUMNS + " from media_file where path=?", rowMapper,
                path.toString());
    }

    public @Nullable MediaFile getMediaFile(int id) {
        return template.queryOne("select " + QUERY_COLUMNS + """
                from media_file
                where id=?
                """, rowMapper, id);
    }

    public List<MediaFile> getMediaFile(MediaType mediaType, long count, long offset, List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", mediaType.name(), "count", count, "offset", offset, "folders",
                MusicFolder.toPathList(folders));
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where present and type= :type and folder in(:folders)
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getChildrenOf(String path) {
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where parent_path=? and present
                """, rowMapper, path);
    }

    public List<MediaFile> getChildrenOf(final long offset, final long count, String path, boolean byYear) {
        String order = (byYear ? "year is null, year, " : "") + "media_file_order";
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where parent_path=? and present
                order by %s
                limit ? offset ?
                """.formatted(order), rowMapper, path, count, offset);
    }

    public List<MediaFile> getChildrenWithOrderOf(String path) {
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where parent_path=? and present
                order by media_file_order
                """, rowMapper, path);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId, long offset, long count) {
        return template.query("select " + prefix(QUERY_COLUMNS, "media_file") + """
                from playlist_file, media_file
                where media_file.id = playlist_file.media_file_id
                        and playlist_file.playlist_id = ? and present
                order by playlist_file.id
                limit ? offset ?
                """, rowMapper, playlistId, count, offset);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) {
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where parent_path=? and present and type in (?,?,?)
                order by track_number
                limit ? offset ?
                """, rowMapper, album.getPathString(), MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) {
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where album_artist=? and album=? and present and type in (?,?,?)
                order by track_number
                limit ? offset ?
                """, rowMapper, albumArtist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getVideos(final int count, final int offset, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.VIDEO.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and present and folder in (:folders)
                order by title
                limit :count offset :offset
                """, rowMapper, args);
    }

    public @Nullable MediaFile getArtistByName(final String name, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.DIRECTORY.name(), "name", name, "folders",
                MusicFolder.toPathList(musicFolders));
        return template.namedQueryOne("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and artist = :name and present and folder in (:folders)
                """, rowMapper, args);
    }

    public boolean exists(Path path) {
        return 0 < template.queryForInt("""
                select count(path)
                from media_file
                where path = ?
                """, 0, path.toString());
    }

    public boolean existsNonPresent() {
        return 0 < template.queryForInt("""
                select count(*)
                from media_file
                where not present
                """, 1);
    }

    public @Nullable MediaFile createMediaFile(MediaFile file) {
        int c = template.update(
                "insert into media_file (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                file.getPathString(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(),
                file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(),
                file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(),
                file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPathString(), file.getParentPathString(), file.getPlayCount(), file.getLastPlayed(),
                file.getComment(), file.getCreated(), file.getChanged(), file.getLastScanned(),
                file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getMusicBrainzReleaseId(),
                file.getMusicBrainzRecordingId(), file.getComposer(), file.getArtistSort(), file.getAlbumSort(),
                file.getTitleSort(), file.getAlbumArtistSort(), file.getComposerSort(), file.getArtistReading(),
                file.getAlbumReading(), file.getAlbumArtistReading(), file.getArtistSortRaw(), file.getAlbumSortRaw(),
                file.getAlbumArtistSortRaw(), file.getComposerSortRaw(), file.getOrder());
        Integer id = template.queryForInt("""
                select id
                from media_file
                where path=?
                """, null, file.getPathString());
        if (c > 0 && id != null) {
            file.setId(id);
            return file;
        }
        return null;
    }

    public Optional<MediaFile> updateMediaFile(MediaFile file) {
        String sql = """
                update media_file
                set folder=?, type=?, format=?, title=?, album=?,
                        artist=?, album_artist=?, disc_number=?, track_number=?, year=?, genre=?,
                        bit_rate=?, variable_bit_rate=?, duration_seconds=?, file_size=?, width=?,
                        height=?, cover_art_path=?, parent_path=?, play_count=?, last_played=?,
                        comment=?, changed=?, last_scanned=?, children_last_updated=?, present=?,
                        version=?, mb_release_id=?, mb_recording_id=?,
                        composer=?, artist_sort=?, album_sort=?, title_sort=?,
                        album_artist_sort=?, composer_sort=?, artist_reading=?, album_reading=?,
                        album_artist_reading=?, artist_sort_raw=?, album_sort_raw=?,
                        album_artist_sort_raw=?, composer_sort_raw=?, media_file_order=?
                where id=?
                """;
        int c = template.update(sql, file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(),
                file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(),
                file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(),
                file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPathString(), file.getParentPathString(), file.getPlayCount(), file.getLastPlayed(),
                file.getComment(), file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(),
                file.isPresent(), VERSION, file.getMusicBrainzReleaseId(), file.getMusicBrainzRecordingId(),
                file.getComposer(), file.getArtistSort(), file.getAlbumSort(), file.getTitleSort(),
                file.getAlbumArtistSort(), file.getComposerSort(), file.getArtistReading(), file.getAlbumReading(),
                file.getAlbumArtistReading(), file.getArtistSortRaw(), file.getAlbumSortRaw(),
                file.getAlbumArtistSortRaw(), file.getComposerSortRaw(), file.getOrder(), file.getId());
        if (c > 0) {
            return Optional.of(file);
        }
        return Optional.empty();
    }

    public void updateChildrenLastUpdated(String pathString, Instant childrenLastUpdated) {
        template.update("""
                update media_file
                set children_last_updated = ?, present=?
                where path=?
                """, childrenLastUpdated, true, pathString);
    }

    public int updateOrder(int id, int order) {
        return template.update("""
                update media_file
                set media_file_order = ?
                where id=?
                """, order, id);
    }

    public void updateCoverArtPath(String pathString, String coverArtPath) {
        template.update("""
                update media_file
                set cover_art_path = ?
                where path=?
                """, coverArtPath, pathString);
    }

    public void updatePlayCount(String pathString, Instant lastPlayed, int playCount) {
        template.update("""
                update media_file
                set last_played = ?, play_count = ?
                where path=?
                """, lastPlayed, playCount, pathString);
    }

    public void updateComment(String pathString, String comment) {
        template.update("""
                update media_file
                set comment = ?
                where path=?
                """, comment, pathString);
    }

    public int deleteMediaFile(int id) {
        return template.update("""
                update media_file
                set present=false, children_last_updated=?
                where id=?
                """, FAR_PAST, id);
    }

    public List<Genre> getGenreCounts() {
        Map<String, Object> args = Map.of("album", MediaType.ALBUM.name(), "music", MediaType.MUSIC.name());
        return template.namedQuery("""
                select song_genre.name, song_count, album_count
                from
                    (select genres.name, count(*) as song_count
                        from
                            (select distinct genre name
                                from media_file
                                where present and type = :album or type = :music) as genres
                        left join media_file as songs
                        on songs.present and genres.name = songs.genre and songs.type = :music
                        group by genres.name) as song_genre
                join
                    (select genres.name, count(*) as album_count
                        from
                            (select distinct genre name
                                from media_file
                                where present and type = :album or type = :music) as genres
                        left join media_file as albums
                        on albums.present and genres.name = albums.genre and albums.type = :album
                        group by genres.name) as album_genre
                on song_genre.name = album_genre.name
                order by name
                """, genreRowMapper, args);
    }

    public void updateGenres(List<Genre> genres) {
        template.update("delete from genre");
        for (Genre genre : genres) {
            template.update("""
                    insert into genre(name, song_count, album_count)
                    values(?, ?, ?)
                    """, genre.getName(), genre.getSongCount(), genre.getAlbumCount());
        }
    }

    public List<MediaFile> getMostFrequentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);

        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and play_count > 0 and present and folder in (:folders)
                order by play_count desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getMostRecentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and last_played is not null
                        and present and folder in (:folders)
                order by last_played desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);

        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and folder in (:folders) and present
                order by created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        String namedQuery = "select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and folder in (:folders) and present
                order by media_file_order
                limit :count offset :offset
                """;
        if (byArtist) {
            namedQuery = "select distinct " + prefix(QUERY_COLUMNS, "al") + """
                            , ar.media_file_order as ar_order, al.media_file_order as al_order
                    from media_file al
                    join media_file ar
                    on ar.path = al.parent_path
                    where al.type = :type and al.folder in (:folders) and al.present
                    order by ar_order, al_order
                    limit :count offset :offset
                    """;
        }

        return template.namedQuery(namedQuery, rowMapper, args);
    }

    public List<MediaFile> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "fromYear", fromYear, "toYear", toYear, "count", count, "offset",
                offset);

        if (fromYear <= toYear) {
            return template.namedQuery("select " + QUERY_COLUMNS + """
                    from media_file
                    where type = :type and folder in (:folders) and present
                            and year between :fromYear and :toYear
                    order by year, media_file_order
                    limit :count offset :offset
                    """, rowMapper, args);
        } else {
            return template.namedQuery("select " + QUERY_COLUMNS + """
                    from media_file
                    where type = :type and folder in (:folders) and present
                            and year between :toYear and :fromYear
                    order by year desc, media_file_order
                    limit :count offset :offset
                    """, rowMapper, args);
        }
    }

    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final List<String> genres,
            final List<MusicFolder> musicFolders) {

        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.ALBUM.name(), "genres", genres, "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and folder in (:folders) and present and genre in (:genres)
                order by media_file_order
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getUnparsedVideos(final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where type = ? and last_scanned = ?
                limit ?
                """, rowMapper, MediaFile.MediaType.VIDEO.name(), FAR_FUTURE, count);
    }

    public List<MediaFile> getChangedId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(folders), "childMax", ALBUM_CHILD_MAX);
        String query = """
                select music_folder.path as folder, first_fetch.album_artist,
                        mf.album_artist_reading, mf.album_artist_sort, mf_ar.cover_art_path
                from
                    (select mf.album_artist,
                            min(mf_al.media_file_order * :childMax + mf.media_file_order
                                    + music_folder.folder_order * :childMax * 10) as file_order
                        from media_file mf
                        join music_folder
                        on mf.present and mf.type in (:types) and mf.album_artist is not null
                                and mf.folder = music_folder.path
                                and music_folder.enabled and mf.folder in (:folders)
                        left join artist ar
                        on ar.name = mf.album_artist
                        join media_file mf_al
                        on mf_al.path = mf.parent_path and ar.name is not null
                        left join media_file mf_ar
                        on mf_ar.path = mf_al.parent_path
                        group by mf.album_artist) first_fetch
                join media_file mf
                on mf.album_artist = first_fetch.album_artist
                join music_folder
                on mf.folder = music_folder.path
                join media_file mf_al
                on mf_al.path = mf.parent_path
                left join media_file mf_ar
                on mf_ar.path = mf_al.parent_path
                left join artist ar
                on ar.name = mf.album_artist
                where mf_al.media_file_order * :childMax + mf.media_file_order
                        + music_folder.folder_order * :childMax * 10 = first_fetch.file_order
                    and ((mf.album_artist_reading is null and ar.reading is not null)
                        or (mf.album_artist_reading is not null and ar.reading is null)
                        or mf.album_artist_reading <> ar.reading
                        or (mf.album_artist_sort is null and ar.sort is not null)
                        or (mf.album_artist_sort is not null and ar.sort is null)
                        or mf.album_artist_sort <> ar.sort
                        or (mf_ar.cover_art_path is not null and ar.cover_art_path is null)
                        or (mf_ar.cover_art_path is null and ar.cover_art_path is not null)
                        or mf_ar.cover_art_path <> ar.cover_art_path)
                limit :count
                """;
        return template.namedQuery(query, artistId3Mapper, args);
    }

    public List<MediaFile> getUnregisteredId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(folders), "childMax", ALBUM_CHILD_MAX);
        String query = """
                select music_folder.path as folder, first_fetch.album_artist, mf.album_artist_reading,
                        mf.album_artist_sort, mf_ar.cover_art_path
                from
                    (select mf.album_artist,
                            min(mf_al.media_file_order * :childMax + mf.media_file_order
                                + music_folder.folder_order * :childMax * 10) as file_order
                        from media_file mf
                        join music_folder
                        on mf.present and mf.type in (:types) and mf.album_artist is not null
                                and mf.folder = music_folder.path
                                and music_folder.enabled and mf.folder in (:folders)
                        left join artist ar
                        on ar.name = mf.album_artist
                        join media_file mf_al
                        on mf_al.path = mf.parent_path and ar.name is null
                        left join media_file mf_ar
                        on mf_ar.path = mf_al.parent_path
                        group by mf.album_artist) first_fetch
                join media_file mf
                on mf.album_artist = first_fetch.album_artist
                join music_folder
                on mf.folder = music_folder.path
                join media_file mf_al
                on mf_al.path = mf.parent_path
                left join media_file mf_ar
                on mf_ar.path = mf_al.parent_path
                left join artist ar
                on ar.name = mf.album_artist
                where mf_al.media_file_order * :childMax + mf.media_file_order
                        + music_folder.folder_order * :childMax * 10 = first_fetch.file_order
                limit :count
                """;
        return template.namedQuery(query, artistId3Mapper, args);
    }

    public List<MediaFile> getChangedAlbums(final int count, final List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders), "future", FAR_FUTURE, "count", count);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and present and folder in (:folders)
                        and children_last_updated = :future
                limit :count
                """, rowMapper, args);
    }

    public List<MediaFile> getUnparsedAlbums(final int count, final List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders), "future", FAR_FUTURE, "count", count);
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and present and folder in (:folders) and last_scanned = :future
                limit :count
                """, rowMapper, args);
    }

    private List<String> getValidTypes4ID3(boolean withPodcast) {
        List<String> types = new ArrayList<>();
        types.add(MediaFile.MediaType.MUSIC.name());
        types.add(MediaFile.MediaType.AUDIOBOOK.name());
        if (withPodcast) {
            types.add(MediaFile.MediaType.PODCAST.name());
        }
        return types;
    }

    public List<MediaFile> getChangedId3Albums(final int count, List<MusicFolder> musicFolders, boolean withPodcast) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(musicFolders), "childMax", ALBUM_CHILD_MAX);
        String query = "select " + prefix(QUERY_COLUMNS, "mf_fetched") + """
                from (select registered.*
                    from
                        (select mf.*, mf.album as mf_album,
                                mf.album_artist as mf_album_artist, music_folder.folder_order,
                                mf_al.media_file_order al_order, mf.media_file_order as mf_order,
                                music_folder.id as mf_folder_id,
                                mf_al.cover_art_path as mf_al_cover_art_path
                        from media_file mf
                        join music_folder
                        on music_folder.path = mf.folder
                        join media_file mf_al
                        on mf_al.path = mf.parent_path) registered
                    join (select mf.album, mf.album_artist,
                            min(mf_al.media_file_order * :childMax
                                    + mf.media_file_order
                                    + music_folder.folder_order * :childMax * 10) as file_order
                        from media_file mf
                        join album al
                        on al.name = mf.album and al.artist = mf.album_artist
                        join music_folder
                        on music_folder.path = mf.folder
                        join media_file mf_al
                        on mf_al.path = mf.parent_path
                        where mf.present and mf.type in (:types) and mf.folder in (:folders)
                        group by mf.album, mf.album_artist) fetched
                    on fetched.album = mf_album and fetched.album_artist = mf_album_artist
                            and fetched.file_order = registered.al_order * :childMax
                                    + registered.mf_order
                                    + registered.folder_order * :childMax * 10) mf_fetched
                join album al
                on al.name = mf_fetched.album and al.artist = mf_fetched.album_artist
                    and (mf_fetched.parent_path <> al.path
                            or mf_fetched.changed <> al.created
                            or mf_folder_id <> al.folder_id
                            or (mf_al_cover_art_path is not null
                                    and al.cover_art_path is null)
                            or (mf_al_cover_art_path is null
                                    and al.cover_art_path is not null)
                            or mf_al_cover_art_path <> al.cover_art_path
                            or (mf_fetched.year is not null and al.year is null)
                            or mf_fetched.year <> al.year
                            or (mf_fetched.genre is not null and al.genre is null)
                            or mf_fetched.genre <> al.genre
                            or (mf_fetched.album_artist_reading is not null
                                    and al.artist_reading is null)
                            or mf_fetched.album_artist_reading <> al.artist_reading
                            or (mf_fetched.album_artist_sort is not null
                                    and al.artist_sort is null)
                            or mf_fetched.album_artist_sort <> al.artist_sort
                            or (mf_fetched.album_reading is not null
                                    and al.name_reading is null)
                            or mf_fetched.album_reading <> al.name_reading
                            or (mf_fetched.album_sort is not null and al.name_sort is null)
                            or mf_fetched.album_sort <> al.name_sort
                            or (mf_fetched.mb_release_id is not null
                                    and al.mb_release_id is null)
                            or mf_fetched.mb_release_id <> al.mb_release_id)
                limit :count
                """;
        return template.namedQuery(query, rowMapper, args);
    }

    public List<MediaFile> getUnregisteredId3Albums(final int count, List<MusicFolder> musicFolders,
            boolean withPodcast) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(musicFolders), "childMax", ALBUM_CHILD_MAX);
        String query = "select " + prefix(QUERY_COLUMNS, "unregistered") + """
                from
                    (select mf.*, mf.album as mf_album, mf.album_artist as mf_album_artist,
                            music_folder.folder_order, mf_al.media_file_order al_order,
                            mf.media_file_order as mf_order
                    from media_file mf
                    join music_folder
                    on music_folder.enabled and music_folder.path = mf.folder
                    join media_file mf_al
                    on mf_al.path = mf.parent_path) unregistered
                    join
                        (select mf.album, mf.album_artist,
                                min(mf_al.media_file_order * :childMax + mf.media_file_order
                                        + music_folder.folder_order * :childMax * 10) as file_order
                        from media_file mf
                        join music_folder
                        on music_folder.enabled and mf.present and mf.type in (:types)
                                and mf.folder in (:folders) and music_folder.path = mf.folder
                        left join album al
                        on al.name = mf.album and al.artist = mf.album_artist
                        join media_file mf_al
                        on mf.album is not null and mf.album_artist is not null and al.name is null
                                and al.artist is null and mf_al.path = mf.parent_path
                        group by album, album_artist) fetched
                    on fetched.album = mf_album and fetched.album_artist = mf_album_artist
                            and fetched.file_order = unregistered.al_order * :childMax
                            + unregistered.mf_order + unregistered.folder_order * :childMax * 10
                limit :count
                """;
        return template.namedQuery(query, rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final List<String> genres, final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types",
                Arrays.asList(MediaType.MUSIC.name(), MediaType.PODCAST.name(), MediaType.AUDIOBOOK.name()), "genres",
                genres, "count", count, "offset", offset, "folders", MusicFolder.toPathList(musicFolders));
        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "s") + """
                from media_file s
                join media_file al
                on s.parent_path = al.path join media_file ar on al.parent_path = ar.path
                where s.type in (:types) and s.genre in (:genres)
                        and s.present and s.folder in (:folders)
                order by ar.media_file_order, al.media_file_order, s.track_number
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
        return template.query("select " + QUERY_COLUMNS + """
                from media_file
                where type in (?,?,?) and artist=? and present
                limit ? offset ?
                """, rowMapper, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                MediaFile.MediaType.AUDIOBOOK.name(), artist, count, offset);
    }

    public @Nullable MediaFile getSongByArtistAndTitle(final String artist, final String title,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || StringUtils.isBlank(title) || StringUtils.isBlank(artist)) {
            return null;
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "title", title, "type",
                MediaFile.MediaType.MUSIC.name(), "folders", MusicFolder.toPathList(musicFolders));
        return template.namedQueryOne("select " + QUERY_COLUMNS + """
                from media_file
                where artist = :artist and title = :title
                        and type = :type and present and folder in (:folders)
                """, rowMapper, args);
    }

    public List<MediaFile> getStarredAlbums(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username, "count", count, "offset", offset);
        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "m") + """
                from starred_media_file, media_file m
                where m.id = starred_media_file.media_file_id and m.present
                        and m.type = :type and m.folder in (:folders)
                        and starred_media_file.username = :username
                order by starred_media_file.created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getStarredDirectories(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username, "count", count, "offset", offset);
        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "m") + """
                from starred_media_file, media_file m
                where m.id = starred_media_file.media_file_id and m.present
                        and m.type = :type and starred_media_file.username = :username
                        and m.folder in (:folders)
                order by starred_media_file.created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getStarredFiles(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types",
                Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                        MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.VIDEO.name()),
                "folders", MusicFolder.toPathList(musicFolders), "username", username, "count", count, "offset",
                offset);
        return template.namedQuery("select " + prefix(QUERY_COLUMNS, "m") + """
                from starred_media_file, media_file m
                where m.id = starred_media_file.media_file_id and m.present and m.type in (:types)
                        and starred_media_file.username = :username and m.folder in (:folders)
                order by starred_media_file.created desc
                limit :count offset :offset
                """, rowMapper, args);
    }

    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, final String username) {

        if (criteria.getMusicFolders().isEmpty()) {
            return Collections.emptyList();
        }

        RandomSongsQueryBuilder queryBuilder = new RandomSongsQueryBuilder(criteria);

        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toPathList(criteria.getMusicFolders()),
                "username", username, "fromYear", criteria.getFromYear(), "toYear", criteria.getToYear(), "genres",
                criteria.getGenres(), "minLastPlayed", criteria.getMinLastPlayedDate(), "maxLastPlayed",
                criteria.getMaxLastPlayedDate(), "minAlbumRating", criteria.getMinAlbumRating(), "maxAlbumRating",
                criteria.getMaxAlbumRating(), "minPlayCount", criteria.getMinPlayCount(), "maxPlayCount",
                criteria.getMaxPlayCount(), "starred", criteria.isShowStarredSongs(), "unstarred",
                criteria.isShowUnstarredSongs(), "format", criteria.getFormat());

        return template.namedQuery(queryBuilder.build(), rowMapper, args);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return template.namedQueryForInt("""
                select count(*)
                from media_file
                where type = :type and folder in (:folders) and present
                """, 0, args);
    }

    public int getPlayedAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return template.namedQueryForInt("""
                select count(*)
                from media_file
                where type = :type and play_count > 0 and present and folder in (:folders)
                """, 0, args);
    }

    public int getStarredAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username);
        return template.namedQueryForInt("""
                select count(*)
                from starred_media_file, media_file
                where media_file.id = starred_media_file.media_file_id
                        and media_file.type = :type
                        and media_file.present and media_file.folder in (:folders)
                        and starred_media_file.username = :username
                """, 0, args);
    }

    @Transactional
    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        template.update("""
                insert into starred_media_file(media_file_id, username, created)
                values (?,?,?)
                """, id, username, now());
    }

    public void unstarMediaFile(int id, String username) {
        template.update("""
                delete from
                starred_media_file
                where media_file_id=? and username=?
                """, id, username);
    }

    public @Nullable Instant getMediaFileStarredDate(int id, String username) {
        return template.queryForInstant("""
                select created
                from starred_media_file
                where media_file_id=? and username=?
                """, null, id, username);
    }

    public void resetLastScanned(@Nullable Integer id) {
        String query = """
                update media_file
                set last_scanned = ?, children_last_updated = ?
                where present
                """;
        if (id == null) {
            template.update(query, FAR_PAST, FAR_PAST);
        } else {
            template.update(query + " and id = ?", FAR_PAST, FAR_PAST, id);
        }
    }

    public void updateLastScanned(int id, Instant lastScanned) {
        template.update("""
                update media_file
                set last_scanned = ?
                where present and id = ?
                """, lastScanned, id);
    }

    public void markNonPresent(Instant lastScanned) {
        int minId = template.queryForInt("""
                select min(id)
                from media_file
                where last_scanned < ? and present
                """, 0, lastScanned);
        int maxId = template.queryForInt("""
                select max(id)
                from media_file
                where last_scanned < ? and present
                """, 0, lastScanned);

        final int batchSize = 1000;
        Instant childrenLastUpdated = FAR_PAST; // Used to force a children rescan if file is later resurrected.
        for (int id = minId; id <= maxId; id += batchSize) {
            template.update("""
                    update media_file
                    set present=false, children_last_updated=?
                    where id between ? and ? and last_scanned < ? and present
                    """, childrenLastUpdated, id, id + batchSize, lastScanned);
        }
    }

    public List<Integer> getArtistExpungeCandidates() {
        return template.queryForInts("""
                select id
                from media_file
                where media_file.type = ? and not present
                """, MediaFile.MediaType.DIRECTORY.name());
    }

    public List<Integer> getAlbumExpungeCandidates() {
        return template.queryForInts("""
                select id
                from media_file
                where media_file.type = ? and not present
                """, MediaFile.MediaType.ALBUM.name());
    }

    public List<Integer> getSongExpungeCandidates() {
        return template.queryForInts("""
                select id
                from media_file
                where media_file.type in (?,?,?,?) and not present
                """, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.VIDEO.name());
    }

    public int getMinId() {
        return template.queryForInt("""
                select min(id)
                from media_file
                where not present
                """, 0);
    }

    public int getMaxId() {
        return template.queryForInt("""
                select max(id)
                from media_file
                where not present
                """, 0);
    }

    public int expunge(int from, int to) {
        return template.update("""
                delete from media_file
                where id between ? and ? and not present
                """, from, to);
    }

    public List<MediaFile> getArtistAll(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return template.namedQuery("select " + QUERY_COLUMNS + """
                from media_file
                where type = :type and folder in (:folders) and present and artist is not null
                order by media_file_order
                """, rowMapper, args);
    }

    public int getChildSizeOf(String path) {
        return template.queryForInt("""
                select count(id)
                from media_file
                where parent_path=? and present
                """, 0, path);
    }

    public List<SortCandidate> getCopyableSortForAlbums(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(folders));
        return template.namedQuery("""
                select 3 as field, known.name , known.sort, id
                from
                        (select distinct album as name, id
                        from media_file
                        where folder in (:folders) and present
                                and album is not null and album_sort is null) unknown
                join
                        (select distinct album as name, album_sort as sort
                        from media_file
                        where folder in (:folders) and type <> :type and album is not null
                                and album_sort is not null and present) known
                on known.name = unknown.name
                """, sortCandidateWithIdMapper, args);
    }

    public List<SortCandidate> getCopyableSortForPersons(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("folders", MusicFolder.toPathList(folders), "type",
                MediaFile.MediaType.MUSIC.name());
        String query = """
                 select field, merged.name , merged.sort, id
                 from
                         (select distinct 0 as field, album_artist as name, id
                         from media_file
                         where folder in (:folders) and present and album_artist is not null
                                 and album_artist_sort is null
                         union
                         select 1 as field, artist as name, id
                         from media_file
                         where folder in (:folders) and present and artist is not null
                                 and artist_sort is null
                         union
                         select distinct 2 as field, composer as name, id
                         from media_file
                         where folder in (:folders) and present and composer is not null
                                 and composer_sort is null) no_sort
                 join
                         (select distinct name, sort
                         from
                                 (select distinct album_artist as name, album_artist_sort as sort
                                 from media_file
                                 where folder in (:folders) and type = :type and present
                                         and album_artist is not null
                                         and album_artist_sort is not null
                                 union
                                 select distinct artist as name, artist_sort as sort
                                 from media_file
                                 where folder in (:folders) and type = :type and present
                                         and artist is not null and artist_sort is not null
                                 union
                                 select distinct composer as name, composer_sort as sort
                                 from media_file
                                 where folder in (:folders) and type = :type and present
                                         and composer is not null
                                         and composer_sort is not null) merged_union) merged
                 on merged.name = no_sort.name
                """;
        return template.namedQuery(query, sortCandidateWithIdMapper, args);
    }

    public int getCountInPlaylist(int playlistId) {
        return template.queryForInt("""
                select count(*)
                from playlist_file, media_file
                where media_file.id = playlist_file.media_file_id
                        and playlist_file.playlist_id = ? and present
                """, 0, playlistId);
    }

    public long countMediaFile(MediaType mediaType, List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", mediaType.name(), "folders", MusicFolder.toPathList(folders));
        long defaultValue = 0;
        String query = """
                select count(*)
                from media_file
                where present and type= :type and folder in(:folders)
                """;
        List<Long> list = template.getNamedParameterJdbcTemplate().queryForList(query, args, Long.class);
        return list.isEmpty() ? defaultValue : list.get(0) == null ? defaultValue : list.get(0);
    }

    public List<MediaFile> getRandomSongsForAlbumArtist(int limit, String albumArtist, List<MusicFolder> musicFolders,
            BiFunction<Integer, Integer, List<Integer>> randomCallback) {
        String type = MediaType.MUSIC.name();

        /* Run the query twice. */

        /*
         * Get the number of records that match the conditions, to generate a set of random numbers according to the
         * number. Therefore, if the number of cases at this time is too large, the subsequent performance is likely to
         * be affected. If the number isn't too large, it doesn't matter much.
         */
        int countAll = template.queryForInt("""
                select count(*)
                from media_file
                where present and type = ? and album_artist = ?
                """, 0, type, albumArtist);
        if (0 == countAll) {
            return Collections.emptyList();
        }

        List<Integer> randomRownum = randomCallback.apply(countAll, limit);

        Map<String, Object> args = LegacyMap.of("type", type, "artist", albumArtist, "randomRownum", randomRownum,
                "limit", limit);

        /*
         * Perform a conditional search and add a row number. Returns the result whose row number is included in the
         * random number set.
         *
         * There are some technical barriers to this query.
         *
         * (1) It must be a row number acquisition method that can be executed in all DBs. (2) It is simpler to join
         * using UNNEST. However, hsqldb traditionally has a problem with UNNEST, and the operation specification
         * differs depending on the version. In addition, compatibility of each DB may be affected.
         *
         * Therefore, we use a very primitive query that combines COUNT and IN here.
         *
         * IN allows you to get the smallest song subset corresponding to random numbers, but unlike JOIN&UNNEST, the
         * order of random numbers is destroyed.
         */
        List<MediaFile> tmpResult = template.namedQuery("select " + QUERY_COLUMNS + """
                        , foo.irownum
                from
                        (select
                                (select count(id)
                                from media_file
                                where id < boo.id and type = :type
                                        and album_artist = :artist) as irownum,
                                boo.*
                        from
                                (select *
                                from media_file
                                where type = :type and album_artist = :artist) boo) as foo
                        where foo.irownum in ( :randomRownum )
                limit :limit
                """, iRowMapper, args);

        /* Restore the order lost in IN. */
        Map<Integer, MediaFile> map = LegacyMap.of();
        tmpResult.forEach(m -> map.put(m.getRownum(), m));
        List<MediaFile> result = new ArrayList<>();
        randomRownum.forEach(i -> {
            MediaFile m = map.get(i);
            if (!ObjectUtils.isEmpty(m)) {
                result.add(m);
            }
        });

        return Collections.unmodifiableList(result);
    }

    public int getSongsCountForAlbum(String artist, String album) {
        return template.queryForInt("""
                select count(id)
                from media_file
                where album_artist=? and album=? and present and type in (?,?,?)
                """, 0, artist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(), MediaType.PODCAST.name());
    }

    public List<SortCandidate> getSortForPersonWithoutSorts(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("typeDirAndAlbum",
                Arrays.asList(MediaType.DIRECTORY.name(), MediaType.ALBUM.name()), "folders",
                MusicFolder.toPathList(folders));
        String query = """
                select field, name, null as sort, id
                from
                        (select distinct 0 as field, album_artist as name, id
                        from media_file
                        where folder in (:folders) and present and type not in (:typeDirAndAlbum)
                                and album_artist is not null and album_artist_sort is null
                        union
                        select distinct 1 as field, artist as name, id
                        from media_file
                        where folder in (:folders) and folder <> path and present
                                and artist is not null and artist_sort is null
                        union
                        select distinct 2 as field, composer as name, id
                        from media_file
                        where folder in (:folders) and present and type not in (:typeDirAndAlbum)
                                and composer is not null and composer_sort is null) no_sorts
                """;
        return template.namedQuery(query, sortCandidateWithIdMapper, args);
    }

    public List<SortCandidate> getSortOfArtistToBeFixedWithId(@NonNull List<SortCandidate> candidates) {
        List<SortCandidate> result = new ArrayList<>();
        if (candidates.isEmpty()) {
            return result;
        }
        String query = """
                select 0 as field, :name, :sote, id
                from media_file
                where type != :directory and album_artist = :name
                        and album_artist_sort <> :sote and present
                union
                select 1 as field, :name, :sote, id
                from media_file
                where artist = :name and artist_sort <> :sote and present
                union
                select 2 as field, :name, :sote, id
                from media_file
                where type = :music and composer = :name
                        and composer_sort <> :sote and present
                """;
        Map<String, Object> args = new ConcurrentHashMap<>();
        args.put("directory", MediaType.DIRECTORY.name());
        args.put("music", MediaType.MUSIC.name());
        candidates.forEach(candidate -> {
            args.put("name", candidate.getName());
            args.put("sote", candidate.getSort());
            result.addAll(template.namedQuery(query, sortCandidateWithIdMapper, args));
        });
        return result;
    }

    public List<SortCandidate> getSortForAlbumWithoutSorts(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(folders));
        return template.namedQuery("""
                select distinct 3 as field, album as name, null as sort, id
                from media_file
                where present and folder in (:folders) and type <> :type
                        and (album is not null and album_sort is null)
                """, sortCandidateWithIdMapper, args);
    }

    public List<SortCandidate> guessAlbumSorts(List<MusicFolder> folders) {
        List<SortCandidate> result = new ArrayList<>();
        if (folders.isEmpty()) {
            return result;
        }
        Map<String, Object> args = Map.of("folders", MusicFolder.toPathList(folders));
        String query = """
                select 3 as field, source.album, source.album_sort, to_be_fixed.id
                from
                        (select distinct with_changed.album, with_sort.album_sort
                        from
                                (select fetched.album, max(fetched.changed) as changed
                                from
                                        (select album
                                        from
                                                (select distinct album, album_sort as sort
                                                from media_file
                                                where album is not null
                                                        and album_sort is not null
                                                        and folder in (:folders)) named_album
                                        group by album having 1 < count(sort)) duplicates
                                join media_file fetched
                                on fetched.album = duplicates.album
                                group by fetched.album) with_changed
                        join media_file with_sort
                        on with_sort.album = with_changed.album
                                and with_changed.changed = with_sort.changed) source
                join media_file to_be_fixed
                on source.album = to_be_fixed.album and source.album_sort <> to_be_fixed.album_sort
                """;
        return template.namedQuery(query, sortCandidateWithIdMapper, args);
    }

    public List<SortCandidate> guessPersonsSorts(List<MusicFolder> folders) {
        List<SortCandidate> result = new ArrayList<>();
        if (folders.isEmpty()) {
            return result;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.MUSIC.name(), "folders",
                MusicFolder.toPathList(folders));
        String query = """
                select min(field) as field, person_all_with_priority.name,
                        sort, max(changed) as changed
                from
                        (select 0 as field, album_artist as name,
                                album_artist_sort as sort, type, changed
                        from media_file
                        where folder in (:folders) and type = :type and album_artist is not null
                                and album_artist_sort is not null and present
                        union
                        select 1 as field, artist as name, artist_sort as sort, type, changed
                        from media_file
                        where folder in (:folders) and type = :type and artist is not null
                                and artist_sort is not null and present
                        union
                        select 2 as field, composer as name, composer_sort as sort, type, changed
                        from media_file
                        where folder in (:folders) and type = :type and composer is not null
                                and composer_sort is not null and present) as person_all_with_priority
                join
                        (select distinct name
                        from
                                (select album_artist as name, album_artist_sort as sort
                                from media_file
                                where folder in (:folders) and type = :type
                                        and album_artist is not null
                                        and album_artist_sort is not null and present
                                union
                                select artist as name, artist_sort as sort
                                from media_file
                                where folder in (:folders) and type = :type and artist is not null
                                        and artist_sort is not null and present
                                union
                                select composer as name, composer_sort as sort
                                from media_file
                                where folder in (:folders) and type = :type and composer is not null
                                        and composer_sort is not null and present) person_union
                        group by name having 1 < count(sort)) as duplicate_names
                on person_all_with_priority.name = duplicate_names.name
                group by field, person_all_with_priority.name, sort
                order by person_all_with_priority.name, field, changed desc
                """;
        List<SortCandidate> candidates = template.namedQuery(query, sortCandidateMapper, args);
        candidates.forEach((candidate) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(candidate.getName()))) {
                result.add(candidate);
            }
        });
        return result;
    }

    public void updateAlbumSortWithId(SortCandidate candidate) {
        template.update("""
                update media_file
                set album_reading = ?, album_sort = ?
                where present and id = ?
                """, candidate.getReading(), candidate.getSort(), candidate.getId());
    }

    public void updateArtistSortWithId(SortCandidate candidate) {
        if (candidate.getField() == CandidateField.ARTIST) {
            template.update("""
                    update media_file
                    set artist_reading = ?, artist_sort = ?
                    where id = ?
                    """, candidate.getReading(), candidate.getSort(), candidate.getId());
        } else if (candidate.getField() == CandidateField.ALBUM_ARTIST) {
            template.update("""
                    update media_file
                    set album_artist_reading = ?, album_artist_sort = ?
                    where id = ?
                    """, candidate.getReading(), candidate.getSort(), candidate.getId());
        } else if (candidate.getField() == CandidateField.COMPOSER) {
            template.update("""
                    update media_file
                    set composer_sort = ?
                    where id = ?
                    """, candidate.getSort(), candidate.getId());
        }
    }

    static class RandomSongsQueryBuilder {

        private final RandomSearchCriteria criteria;

        public RandomSongsQueryBuilder(RandomSearchCriteria criteria) {
            this.criteria = criteria;
        }

        public String build() {
            StringBuilder query = new StringBuilder(1024); // 988 + param
            query.append("select ").append(prefix(QUERY_COLUMNS, "media_file")).append("from media_file");
            getIfJoinStarred().ifPresent(query::append);
            getIfJoinAlbumRating().ifPresent(query::append);
            query.append(" where media_file.present and media_file.type = 'MUSIC'");
            getFolderCondition().ifPresent(query::append);
            getGenreCondition().ifPresent(query::append);
            getFormatCondition().ifPresent(query::append);
            getFromYearCondition().ifPresent(query::append);
            getToYearCondition().ifPresent(query::append);
            getMinLastPlayedDateCondition().ifPresent(query::append);
            getMaxLastPlayedDateCondition().ifPresent(query::append);
            getMinAlbumRatingCondition().ifPresent(query::append);
            getMaxAlbumRatingCondition().ifPresent(query::append);
            getMinPlayCountCondition().ifPresent(query::append);
            getMaxPlayCountCondition().ifPresent(query::append);
            getShowStarredSongsCondition().ifPresent(query::append);
            getShowUnstarredSongsCondition().ifPresent(query::append);
            query.append(" order by rand() limit ").append(criteria.getCount());
            return query.toString();
        }

        Optional<String> getIfJoinStarred() {
            boolean joinStarred = criteria.isShowStarredSongs() ^ criteria.isShowUnstarredSongs();
            if (joinStarred) {
                return Optional.of("""
                        \sleft outer join starred_media_file \
                        on media_file.id = starred_media_file.media_file_id \
                        and starred_media_file.username = :username\
                        """);
            }
            return Optional.empty();
        }

        Optional<String> getIfJoinAlbumRating() {
            boolean joinAlbumRating = criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null;
            if (joinAlbumRating) {
                return Optional.of("""
                         left outer join media_file media_album \
                        on media_album.type = 'ALBUM' and media_album.album = media_file.album \
                        and media_album.artist = media_file.artist \
                        left outer join user_rating \
                        on user_rating.path = media_album.path and user_rating.username = :username\
                        """);
            }
            return Optional.empty();
        }

        Optional<String> getFolderCondition() {
            if (!criteria.getMusicFolders().isEmpty()) {
                return Optional.of(" and media_file.folder in (:folders)");
            }
            return Optional.empty();
        }

        Optional<String> getGenreCondition() {
            if (criteria.getGenres() != null) {
                return Optional.of(" and media_file.genre in (:genres)");
            }
            return Optional.empty();
        }

        Optional<String> getFormatCondition() {
            if (criteria.getFormat() != null) {
                return Optional.of(" and media_file.format = :format");
            }
            return Optional.empty();
        }

        Optional<String> getFromYearCondition() {
            if (criteria.getFromYear() != null) {
                return Optional.of(" and media_file.year >= :fromYear");
            }
            return Optional.empty();
        }

        Optional<String> getToYearCondition() {
            if (criteria.getToYear() != null) {
                return Optional.of(" and media_file.year <= :toYear");
            }
            return Optional.empty();
        }

        Optional<String> getMinLastPlayedDateCondition() {
            if (criteria.getMinLastPlayedDate() != null) {
                return Optional.of(" and media_file.last_played >= :minLastPlayed");
            }
            return Optional.empty();
        }

        Optional<String> getMaxLastPlayedDateCondition() {
            if (criteria.getMaxLastPlayedDate() != null) {
                if (criteria.getMinLastPlayedDate() == null) {
                    return Optional
                            .of(" and (media_file.last_played is null or media_file.last_played <= :maxLastPlayed)");
                } else {
                    return Optional.of(" and media_file.last_played <= :maxLastPlayed");
                }
            }
            return Optional.empty();
        }

        Optional<String> getMinAlbumRatingCondition() {
            if (criteria.getMinAlbumRating() != null) {
                return Optional.of(" and user_rating.rating >= :minAlbumRating");
            }
            return Optional.empty();
        }

        Optional<String> getMaxAlbumRatingCondition() {
            if (criteria.getMaxAlbumRating() != null) {
                if (criteria.getMinAlbumRating() == null) {
                    return Optional.of(" and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)");
                } else {
                    return Optional.of(" and user_rating.rating <= :maxAlbumRating");
                }
            }
            return Optional.empty();
        }

        Optional<String> getMinPlayCountCondition() {
            if (criteria.getMinPlayCount() != null) {
                return Optional.of(" and media_file.play_count >= :minPlayCount");
            }
            return Optional.empty();
        }

        Optional<String> getMaxPlayCountCondition() {
            if (criteria.getMaxPlayCount() != null) {
                if (criteria.getMinPlayCount() == null) {
                    return Optional
                            .of(" and (media_file.play_count is null or media_file.play_count <= :maxPlayCount)");
                } else {
                    return Optional.of(" and media_file.play_count <= :maxPlayCount");
                }
            }
            return Optional.empty();
        }

        Optional<String> getShowStarredSongsCondition() {
            if (criteria.isShowStarredSongs() && !criteria.isShowUnstarredSongs()) {
                return Optional.of(" and starred_media_file.id is not null");
            }
            return Optional.empty();
        }

        Optional<String> getShowUnstarredSongsCondition() {
            if (criteria.isShowUnstarredSongs() && !criteria.isShowStarredSongs()) {
                return Optional.of(" and starred_media_file.id is null");
            }
            return Optional.empty();
        }
    }
}
