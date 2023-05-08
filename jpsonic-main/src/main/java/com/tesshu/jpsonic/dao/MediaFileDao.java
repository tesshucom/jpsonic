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

import static com.tesshu.jpsonic.util.PlayerUtils.FAR_FUTURE;
import static com.tesshu.jpsonic.util.PlayerUtils.FAR_PAST;
import static com.tesshu.jpsonic.util.PlayerUtils.now;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.RandomSearchCriteria;
import com.tesshu.jpsonic.domain.SortCandidate;
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Only DAO is allowed to exclude this rule #827
@Repository
public class MediaFileDao extends AbstractDao {

    private static final String INSERT_COLUMNS = "path, folder, type, format, title, album, artist, album_artist, disc_number, "
            + "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, "
            + "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, "
            + "version, mb_release_id, mb_recording_id, composer, artist_sort, album_sort, title_sort, album_artist_sort, composer_sort, "
            + "artist_reading, album_reading, album_artist_reading, "
            + "artist_sort_raw, album_sort_raw, album_artist_sort_raw, composer_sort_raw, media_file_order";
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private static final String GENRE_COLUMNS = "name, song_count, album_count";

    // Expected maximum number of album child elements (can be expanded)
    private static final int ALBUM_CHILD_MAX = 10_000;

    private static final int JP_VERSION = 9;
    public static final int VERSION = 4 + JP_VERSION;

    private final RowMapper<MediaFile> rowMapper;
    private final RowMapper<MediaFile> artistId3Mapper;
    private final RowMapper<Genre> genreRowMapper;
    private final RowMapper<MediaFile> iRowMapper;
    private final RowMapper<SortCandidate> sortCandidateMapper;

    public MediaFileDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new MediaFileMapper();
        artistId3Mapper = (resultSet, rowNum) -> new MediaFile(-1, null, resultSet.getString(1), null, null, null, null,
                null, resultSet.getString(2), null, null, null, null, null, false, null, null, null, null,
                resultSet.getString(5), null, -1, null, null, null, null, null, null, false, -1, null, null, null, null,
                null, null, resultSet.getString(4), null, null, null, resultSet.getString(3), null, null, null, null,
                -1);
        genreRowMapper = new GenreMapper();
        iRowMapper = new MediaFileInternalRowMapper(rowMapper);
        sortCandidateMapper = (rs, rowNum) -> new SortCandidate(rs.getString(1), rs.getString(2));
    }

    /**
     * Returns the media file for the given path.
     *
     * @param path
     *            The path.
     *
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + QUERY_COLUMNS + " from media_file where path=?", rowMapper, path);
    }

    public @Nullable MediaFile getMediaFile(@NonNull Path path) {
        return queryOne("select " + QUERY_COLUMNS + " from media_file where path=?", rowMapper, path.toString());
    }

    /**
     * Returns the media file for the given ID.
     *
     * @param id
     *            The ID.
     *
     * @return The media file or null.
     */
    public MediaFile getMediaFile(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from media_file where id=?", rowMapper, id);
    }

    public List<MediaFile> getMediaFile(MediaType mediaType, long count, long offset, List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", mediaType.name(), "count", count, "offset", offset, "folders",
                MusicFolder.toPathList(folders));
        return namedQuery("select " + getQueryColoms()
                + " from media_file where present and type= :type and folder in(:folders)　order by media_file_order limit :count offset :offset",
                rowMapper, args);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path
     *            The path.
     *
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + QUERY_COLUMNS + " from media_file where parent_path=? and present", rowMapper, path);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path
     *            The path.
     *
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(final long offset, final long count, String path, boolean byYear) {
        String order = byYear ? "year" : "media_file_order";
        return query("select " + getQueryColoms() + " from media_file " + "where parent_path=? and present "
                + "order by " + order + " limit ? offset ?", rowMapper, path, count, offset);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return query("select " + prefix(QUERY_COLUMNS, "media_file") + " from playlist_file, media_file where "
                + "media_file.id = playlist_file.media_file_id and " + "playlist_file.playlist_id = ? "
                + "order by playlist_file.id", rowMapper, playlistId);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId, long offset, long count) {
        return query("select " + prefix(getQueryColoms(), "media_file") + " from playlist_file, media_file "
                + "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present "
                + "order by playlist_file.id limit ? offset ?", rowMapper, playlistId, count, offset);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return query(
                "select " + QUERY_COLUMNS + " from media_file where album_artist=? and album=? and present "
                        + "and type in (?,?,?) order by disc_number, track_number",
                rowMapper, artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name());
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) {
        return query(
                "select " + getQueryColoms() + " from media_file "
                        + "where parent_path=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, album.getPathString(), MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) {
        return query("select " + getQueryColoms() + " from media_file "
                + "where album_artist=? and album=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, albumArtist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getVideos(final int count, final int offset, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.VIDEO.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return namedQuery(
                "select " + QUERY_COLUMNS + " from media_file where type = :type and present and folder in (:folders) "
                        + "order by title limit :count offset :offset",
                rowMapper, args);
    }

    public MediaFile getArtistByName(final String name, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.DIRECTORY.name(), "name", name, "folders",
                MusicFolder.toPathList(musicFolders));
        return namedQueryOne("select " + QUERY_COLUMNS + " from media_file where type = :type and artist = :name "
                + "and present and folder in (:folders)", rowMapper, args);
    }

    public boolean exists(Path path) {
        return 0 < queryForInt("select count(path) from media_file where path = ?", 0, path.toString());
    }

    public boolean existsNonPresent() {
        return 0 < queryForInt("select count(*) from media_file where not present", 1);
    }

    public @Nullable MediaFile createMediaFile(MediaFile file) {
        int c = update("insert into media_file (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
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
        Integer id = queryForInt("select id from media_file where path=?", null, file.getPathString());
        if (c > 0 && id != null) {
            file.setId(id);
            return file;
        }
        return null;
    }

    public @Nullable MediaFile updateMediaFile(MediaFile file) {
        String sql = "update media_file set folder=?, type=?, format=?, title=?, album=?, "
                + "artist=?, album_artist=?, disc_number=?, track_number=?, year=?, genre=?, "
                + "bit_rate=?, variable_bit_rate=?, duration_seconds=?, file_size=?, width=?, "
                + "height=?, cover_art_path=?, parent_path=?, play_count=?, last_played=?, "
                + "comment=?, changed=?, last_scanned=?, children_last_updated=?, present=?, "
                + "version=?, mb_release_id=?, mb_recording_id=?, "
                + "composer=?, artist_sort=?, album_sort=?, title_sort=?, "
                + "album_artist_sort=?, composer_sort=?, artist_reading=?, album_reading=?, "
                + "album_artist_reading=?, artist_sort_raw=?, album_sort_raw=?, "
                + "album_artist_sort_raw=?, composer_sort_raw=?, media_file_order=? " + "where path=?";
        int c = update(sql, file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(),
                file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(),
                file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(),
                file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPathString(), file.getParentPathString(), file.getPlayCount(), file.getLastPlayed(),
                file.getComment(), file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(),
                file.isPresent(), VERSION, file.getMusicBrainzReleaseId(), file.getMusicBrainzRecordingId(),
                file.getComposer(), file.getArtistSort(), file.getAlbumSort(), file.getTitleSort(),
                file.getAlbumArtistSort(), file.getComposerSort(), file.getArtistReading(), file.getAlbumReading(),
                file.getAlbumArtistReading(), file.getArtistSortRaw(), file.getAlbumSortRaw(),
                file.getAlbumArtistSortRaw(), file.getComposerSortRaw(), file.getOrder(), file.getPathString());
        if (c > 0) {
            return file;
        }
        return null;
    }

    public void updateChildrenLastUpdated(String pathString, Instant childrenLastUpdated) {
        update("update media_file set children_last_updated = ?, present=? where path=?", childrenLastUpdated, true,
                pathString);
    }

    public void updateOrder(String pathString, int order) {
        update("update media_file set media_file_order = ? where path=?", order, pathString);
    }

    public void updateCoverArtPath(String pathString, String coverArtPath) {
        update("update media_file set cover_art_path = ? where path=?", coverArtPath, pathString);
    }

    public void updatePlayCount(String pathString, Instant lastPlayed, int playCount) {
        update("update media_file set last_played = ?, play_count = ? where path=?", lastPlayed, playCount, pathString);
    }

    public void updateComment(String pathString, String comment) {
        update("update media_file set comment = ? where path=?", comment, pathString);
    }

    public int deleteMediaFile(int id) {
        return update("update media_file set present=false, children_last_updated=? where id=?", FAR_PAST, id);
    }

    public List<Genre> getGenreCounts() {
        return query("select song_genre.name, song_count, album_count from ( "
                + "select genres.name, count(*) as song_count from (select distinct genre name from media_file where present and type = 'ALBUM' or type = 'MUSIC') as genres "
                + "left join media_file as songs "
                + "on songs.present and genres.name = songs.genre and songs.type = 'MUSIC' "
                + "group by genres.name) as song_genre " + "join ( "
                + "select genres.name, count(*) as album_count from ( "
                + "select distinct genre name from media_file where present and type = 'ALBUM' or type = 'MUSIC') as genres "
                + "left join media_file as albums "
                + "on albums.present and genres.name = albums.genre and albums.type = 'ALBUM' "
                + "group by genres.name) as album_genre " + "on song_genre.name = album_genre.name " + "order by name",
                genreRowMapper);
    }

    public void updateGenres(List<Genre> genres) {
        update("delete from genre");
        for (Genre genre : genres) {
            update("insert into genre(" + GENRE_COLUMNS + ") values(?, ?, ?)", genre.getName(), genre.getSongCount(),
                    genre.getAlbumCount());
        }
    }

    public long getTotalBytes(MusicFolder folder) {
        return queryForLong(
                "select sum(file_size) from media_file " + "where present and folder = ? and type = 'MUSIC'", 0L,
                folder.getPathString());
    }

    public long getTotalSeconds(MusicFolder folder) {
        return queryForLong(
                "select sum(duration_seconds) from media_file " + "where present and folder = ? and type = 'MUSIC'", 0L,
                folder.getPathString());
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);

        return namedQuery("select " + QUERY_COLUMNS
                + " from media_file where type = :type and play_count > 0 and present and folder in (:folders) "
                + "order by play_count desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return namedQuery(
                "select " + QUERY_COLUMNS
                        + " from media_file where type = :type and last_played is not null and present "
                        + "and folder in (:folders) order by last_played desc limit :count offset :offset",
                rowMapper, args);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);

        return namedQuery(
                "select " + QUERY_COLUMNS + " from media_file where type = :type and folder in (:folders) and present "
                        + "order by created desc limit :count offset :offset",
                rowMapper, args);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param byArtist
     *            Whether to sort by artist name
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        String namedQuery = "select " + QUERY_COLUMNS
                + " from media_file where type = :type and folder in (:folders) and present "
                + "order by media_file_order, album_reading limit :count offset :offset";
        if (byArtist) {
            namedQuery = "select distinct " + prefix(QUERY_COLUMNS, "al")
                    + ", ar.media_file_order as ar_order, al.media_file_order as al_order "
                    + "from media_file al join media_file ar on ar.path = al.parent_path "
                    + "where al.type = :type and al.folder in (:folders) and al.present "
                    + "order by ar_order, al.artist_reading, al_order, al.album_reading limit :count offset :offset";
        }

        return namedQuery(namedQuery, rowMapper, args);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param fromYear
     *            The first year in the range.
     * @param toYear
     *            The last year in the range.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "fromYear", fromYear, "toYear", toYear, "count", count, "offset",
                offset);

        if (fromYear <= toYear) {
            return namedQuery(
                    "select " + QUERY_COLUMNS
                            + " from media_file where type = :type and folder in (:folders) and present "
                            + "and year between :fromYear and :toYear order by year limit :count offset :offset",
                    rowMapper, args);
        } else {
            return namedQuery(
                    "select " + QUERY_COLUMNS
                            + " from media_file where type = :type and folder in (:folders) and present "
                            + "and year between :toYear and :fromYear order by year desc limit :count offset :offset",
                    rowMapper, args);
        }
    }

    public List<MediaFile> getUnparsedVideos(final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        return query("select " + QUERY_COLUMNS + " from media_file where type = ? and last_scanned = ? limit ?",
                rowMapper, MediaFile.MediaType.VIDEO.name(), FAR_FUTURE, count);
    }

    public List<MediaFile> getChangedId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(folders));

        String query = "select distinct music_folder.path as folder, first_fetch.album_artist, first_fetch.album_artist_reading, first_fetch.album_artist_sort, mf_ar.cover_art_path "
                + "from (select distinct mf.album_artist, mf.album_artist_reading, mf.album_artist_sort, min(music_folder.folder_order) as folder_order, min(mf_ar.media_file_order) as mf_ar_order "
                + "from media_file mf join music_folder on mf.present and mf.type in (:types) and mf.album_artist is not null "
                + "and mf.folder = music_folder.path and music_folder.enabled and mf.folder in (:folders) "
                + "left join artist ar on ar.name = mf.album_artist "
                + "join media_file mf_al on mf_al.path = mf.parent_path and ar.name is not null "
                + "left join media_file mf_ar on mf_ar.path = mf_al.parent_path "
                + "group by mf.album_artist, mf.album_artist_reading, mf.album_artist_sort) first_fetch "
                + "join media_file mf on mf.album_artist = first_fetch.album_artist "
                + "join music_folder on music_folder.folder_order = first_fetch.folder_order "
                + "left join artist ar on ar.name = mf.album_artist "
                + "join media_file mf_al on mf_al.path = mf.parent_path "
                + "left join media_file mf_ar on mf_ar.path = mf_al.parent_path "
                + "where mf_ar.media_file_order = first_fetch.mf_ar_order "
                // Diff comparison
                + "and ((mf.album_artist_reading is null and ar.reading is not null) " // album_artist_reading
                + "or (mf.album_artist_reading is not null and ar.reading is null) "
                + "or mf.album_artist_reading <> ar.reading "
                + "or (mf.album_artist_sort is null and ar.sort is not null) " // album_artist_sort
                + "or (mf.album_artist_sort is not null and ar.sort is null) " + "or mf.album_artist_sort <> ar.sort "
                + "or (mf_ar.cover_art_path is not null and ar.cover_art_path is null) " // cover_art_path
                + "or (mf_ar.cover_art_path is null and ar.cover_art_path is not null) "
                + "or mf_ar.cover_art_path <> ar.cover_art_path) limit :count";
        return namedQuery(query, artistId3Mapper, args);
    }

    public List<MediaFile> getUnregisteredId3Artists(final int count, List<MusicFolder> folders, boolean withPodcast) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(folders));
        String query = "select distinct music_folder.path as folder, first_fetch.album_artist, first_fetch.album_artist_reading, first_fetch.album_artist_sort, mf_ar.cover_art_path "
                + "from (select distinct mf.album_artist, mf.album_artist_reading, mf.album_artist_sort, min(music_folder.folder_order) as folder_order, min(mf_ar.media_file_order) as mf_ar_order "
                + "from media_file mf join music_folder on mf.present and mf.type in (:types) and mf.album_artist is not null "
                + "and mf.folder = music_folder.path and music_folder.enabled and mf.folder in (:folders) "
                + "left join artist ar on ar.name = mf.album_artist "
                + "join media_file mf_al on mf_al.path = mf.parent_path and ar.name is null "
                + "left join media_file mf_ar on mf_ar.path = mf_al.parent_path "
                + "group by mf.album_artist, mf.album_artist_reading, mf.album_artist_sort) first_fetch "
                + "join media_file mf on mf.album_artist = first_fetch.album_artist "
                + "join music_folder on music_folder.folder_order = first_fetch.folder_order "
                + "left join artist ar on ar.name = mf.album_artist "
                + "join media_file mf_al on mf_al.path = mf.parent_path "
                + "left join media_file mf_ar on mf_ar.path = mf_al.parent_path "
                + "where mf_ar.media_file_order = first_fetch.mf_ar_order limit :count ";
        return namedQuery(query, artistId3Mapper, args);
    }

    public List<MediaFile> getChangedAlbums(final int count, final List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders), "future", FAR_FUTURE, "count", count);
        return namedQuery("select " + QUERY_COLUMNS
                + " from media_file where type = :type and present and folder in (:folders) and children_last_updated = :future limit :count",
                rowMapper, args);
    }

    public List<MediaFile> getUnparsedAlbums(final int count, final List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders), "future", FAR_FUTURE, "count", count);
        return namedQuery("select " + QUERY_COLUMNS
                + " from media_file where type = :type and present and folder in (:folders) and last_scanned = :future limit :count",
                rowMapper, args);
    }

    public @Nullable MediaFile getFetchedFirstChildOf(MediaFile album) {
        Map<String, Object> args = Map.of("types",
                Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                        MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.VIDEO.name()),
                "albumpath", album.getPathString());
        return namedQueryOne("select " + QUERY_COLUMNS + ", "
                + "case when album_artist is null then 1 when album is null then 2 else 0 end is_valid "
                + "from media_file where present and parent_path=:albumpath and type in (:types) order by is_valid, media_file_order limit 1",
                rowMapper, args);
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
        String query = "select " + prefix(QUERY_COLUMNS, "mf_fetched") + " from (select registered.* from "
                + "(select mf.*, mf.album as mf_album, mf.album_artist as mf_album_artist, music_folder.folder_order, "
                + "mf_al.media_file_order al_order, mf.media_file_order as mf_order from media_file mf "
                + "join music_folder on music_folder.path = mf.folder "
                + "join media_file mf_al on mf_al.path = mf.parent_path) registered "
                + "join (select album, album_artist, min(file_order) as file_order from "
                + "(select mf.album, mf.album_artist, mf_al.media_file_order * :childMax + mf.media_file_order + music_folder.folder_order * :childMax as file_order from media_file mf "
                + "join album al on al.name = mf.album and al.artist = mf.album_artist "
                + "join music_folder on music_folder.path = mf.folder "
                + "join media_file mf_al on mf_al.path = mf.parent_path "
                + "where mf.folder in (:folders) and mf.present and mf.type in (:types)) registered "
                + "group by album, album_artist) fetched "
                + "on fetched.album = mf_album and fetched.album_artist = mf_album_artist "
                + "and fetched.file_order = registered.al_order * :childMax + registered.mf_order + registered.folder_order * :childMax * 10) mf_fetched "
                + "join music_folder mf_folder on mf_fetched.folder = mf_folder.path "
                + "join album al on al.name = mf_fetched.album and al.artist = mf_fetched.album_artist "
                + "join media_file mf_al on mf_al.path = mf_fetched.parent_path and mf_fetched.present and ("
                // Diff comparison
                + "mf_fetched.parent_path <> al.path " // path
                + "or mf_fetched.changed <> al.created " // changed
                + "or mf_folder.id <> al.folder_id " // folder_id
                + "or (mf_al.cover_art_path is not null and al.cover_art_path is null) or (mf_al.cover_art_path is null and al.cover_art_path is not null) "
                + "or mf_al.cover_art_path <> al.cover_art_path " // cover_art_path
                + "or (mf_fetched.year is not null and al.year is null) or mf_fetched.year <> al.year " // year
                + "or (mf_fetched.genre is not null and al.genre is null) or mf_fetched.genre <> al.genre " // genre
                + "or (mf_fetched.album_artist_reading is not null and al.artist_reading is null) or mf_fetched.album_artist_reading <> al.artist_reading " // artist_reading
                + "or (mf_fetched.album_artist_sort is not null and al.artist_sort is null) or mf_fetched.album_artist_sort <> al.artist_sort " // artist_sort
                + "or (mf_fetched.album_reading is not null and al.name_reading is null) or mf_fetched.album_reading <> al.name_reading " // album_reading
                + "or (mf_fetched.album_sort is not null and al.name_sort is null) or mf_fetched.album_sort <> al.name_sort " // album_sort
                + "or (mf_fetched.mb_release_id is not null and al.mb_release_id is null) or mf_fetched.mb_release_id <> al.mb_release_id" // mb_release_id
                + ") limit :count";
        return namedQuery(query, rowMapper, args);
    }

    public List<MediaFile> getUnregisteredId3Albums(final int count, List<MusicFolder> musicFolders,
            boolean withPodcast) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types", getValidTypes4ID3(withPodcast), "count", count, "folders",
                MusicFolder.toPathList(musicFolders), "childMax", ALBUM_CHILD_MAX);
        String query = "select " + prefix(QUERY_COLUMNS, "unregistered") + " from "
                + "(select mf.*, mf.album as mf_album, mf.album_artist as mf_album_artist, music_folder.folder_order, "
                + "mf_al.media_file_order al_order, mf.media_file_order as mf_order from media_file mf "
                + "join music_folder on music_folder.path = mf.folder "
                + "join media_file mf_al on mf_al.path = mf.parent_path) unregistered "
                + "join (select album, album_artist, min(file_order) as file_order from "
                + "(select mf.album, mf.album_artist, mf_al.media_file_order * :childMax + mf.media_file_order + music_folder.folder_order * :childMax * 10 as file_order from media_file mf "
                + "left join album al on al.name = mf.album and al.artist = mf.album_artist "
                + "join music_folder on music_folder.path = mf.folder "
                + "join media_file mf_al on mf_al.path = mf.parent_path "
                + "where mf.folder in (:folders) and mf.present and mf.type in (:types) "
                + "and mf.album is not null and mf.album_artist is not null and al.name is null and al.artist is null) gap "
                + "group by album, album_artist) fetched "
                + "on fetched.album = mf_album and fetched.album_artist = mf_album_artist "
                + "and fetched.file_order = unregistered.al_order * :childMax + unregistered.mf_order + unregistered.folder_order * :childMax * 10 limit :count";
        return namedQuery(query, rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final String genre, final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types",
                Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                        MediaFile.MediaType.AUDIOBOOK.name()),
                "genre", genre, "count", count, "offset", offset, "folders", MusicFolder.toPathList(musicFolders));
        return namedQuery("select " + QUERY_COLUMNS + " from media_file where type in (:types) and genre = :genre "
                + "and present and folder in (:folders) limit :count offset :offset", rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final List<String> genres, final int offset, final int count,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("types",
                Arrays.asList(MediaType.MUSIC.name(), MediaType.PODCAST.name(), MediaType.AUDIOBOOK.name()), "genres",
                genres, "count", count, "offset", offset, "folders", MusicFolder.toPathList(musicFolders));
        return namedQuery("select " + prefix(getQueryColoms(), "s") + " from media_file s "
                + "join media_file al on s.parent_path = al.path " + "join media_file ar on al.parent_path = ar.path "
                + "where s.type in (:types) and s.genre in (:genres) " + "and s.present and s.folder in (:folders) "
                + "order by ar.media_file_order, al.media_file_order, s.track_number " + "limit :count offset :offset ",
                rowMapper, args);
    }

    public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
        return query(
                "select " + QUERY_COLUMNS
                        + " from media_file where type in (?,?,?) and artist=? and present limit ? offset ?",
                rowMapper, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                MediaFile.MediaType.AUDIOBOOK.name(), artist, count, offset);
    }

    public MediaFile getSongByArtistAndTitle(final String artist, final String title,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || StringUtils.isBlank(title) || StringUtils.isBlank(artist)) {
            return null;
        }
        Map<String, Object> args = LegacyMap.of("artist", artist, "title", title, "type",
                MediaFile.MediaType.MUSIC.name(), "folders", MusicFolder.toPathList(musicFolders));
        return namedQueryOne("select " + QUERY_COLUMNS + " from media_file where artist = :artist "
                + "and title = :title and type = :type and present and folder in (:folders)", rowMapper, args);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param username
     *            Returns albums starred by this user.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username, "count", count, "offset", offset);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file")
                + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and "
                + "media_file.present and media_file.type = :type and media_file.folder in (:folders) and starred_media_file.username = :username "
                + "order by starred_media_file.created desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently starred directories.
     *
     * @param offset
     *            Number of directories to skip.
     * @param count
     *            Maximum number of directories to return.
     * @param username
     *            Returns directories starred by this user.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently starred directories for this user.
     */
    public List<MediaFile> getStarredDirectories(final int offset, final int count, final String username,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username, "count", count, "offset", offset);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file") + " from starred_media_file, media_file "
                + "where media_file.id = starred_media_file.media_file_id and "
                + "media_file.present and media_file.type = :type and starred_media_file.username = :username and "
                + "media_file.folder in (:folders) "
                + "order by starred_media_file.created desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently starred files.
     *
     * @param offset
     *            Number of files to skip.
     * @param count
     *            Maximum number of files to return.
     * @param username
     *            Returns files starred by this user.
     * @param musicFolders
     *            Only return albums in these folders.
     *
     * @return The most recently starred files for this user.
     */
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
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file")
                + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and "
                + "media_file.present and media_file.type in (:types) and starred_media_file.username = :username and "
                + "media_file.folder in (:folders) "
                + "order by starred_media_file.created desc limit :count offset :offset", rowMapper, args);
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

        return namedQuery(queryBuilder.build(), rowMapper, args);
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return namedQueryForInt(
                "select count(*) from media_file where type = :type and folder in (:folders) and present", 0, args);
    }

    public int getAlbumCount(MusicFolder folder) {
        return queryForInt(
                "select count(*) from media_file " + "right join music_folder on music_folder.path = media_file.folder "
                        + "where present and folder = ? and type = ?",
                0, folder.getPathString(), MediaFile.MediaType.ALBUM.name());
    }

    public int getArtistCount(MusicFolder folder) {
        return queryForInt(
                "select count(*) from media_file " + "right join music_folder on music_folder.path = media_file.folder "
                        + "where present and folder = ? and type = ? and media_file.path <> folder",
                0, folder.getPathString(), MediaFile.MediaType.DIRECTORY.name());
    }

    public int getSongCount(MusicFolder folder) {
        return queryForInt("select count(*) from media_file where present and folder = ? and type = ?", 0,
                folder.getPathString(), MediaFile.MediaType.MUSIC.name());
    }

    public int getVideoCount(MusicFolder folder) {
        return queryForInt("select count(*) from media_file where present and folder = ? and type = ?", 0,
                folder.getPathString(), MediaFile.MediaType.VIDEO.name());
    }

    public int getPlayedAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return namedQueryForInt("select count(*) from media_file where type = :type "
                + "and play_count > 0 and present and folder in (:folders)", 0, args);
    }

    public int getStarredAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(musicFolders), "username", username);
        return namedQueryForInt("select count(*) from starred_media_file, media_file "
                + "where media_file.id = starred_media_file.media_file_id " + "and media_file.type = :type "
                + "and media_file.present " + "and media_file.folder in (:folders) "
                + "and starred_media_file.username = :username", 0, args);
    }

    @Transactional
    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        update("insert into starred_media_file(media_file_id, username, created) values (?,?,?)", id, username, now());
    }

    public void unstarMediaFile(int id, String username) {
        update("delete from starred_media_file where media_file_id=? and username=?", id, username);
    }

    public Instant getMediaFileStarredDate(int id, String username) {
        return queryForInstant("select created from starred_media_file where media_file_id=? and username=?", null, id,
                username);
    }

    public void resetLastScanned() {
        update("update media_file set last_scanned = ?, children_last_updated = ? where present", FAR_PAST, FAR_PAST);
    }

    public void resetLastScanned(int id) {
        update("update media_file set last_scanned = ?, children_last_updated = ? where present and id = ?", FAR_PAST,
                FAR_PAST, id);
    }

    public void updateLastScanned(int id, Instant lastScanned) {
        update("update media_file set last_scanned = ? where present and id = ?", lastScanned, id);
    }

    public void markNonPresent(Instant lastScanned) {
        int minId = queryForInt("select min(id) from media_file where last_scanned < ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from media_file where last_scanned < ? and present", 0, lastScanned);

        final int batchSize = 1000;
        Instant childrenLastUpdated = FAR_PAST; // Used to force a children rescan if file is later resurrected.
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update media_file set present=false, children_last_updated=? where id between ? and ? and "
                    + "last_scanned < ? and present", childrenLastUpdated, id, id + batchSize, lastScanned);
        }
    }

    public List<Integer> getArtistExpungeCandidates() {
        return queryForInts("select id from media_file where media_file.type = ? and not present",
                MediaFile.MediaType.DIRECTORY.name());
    }

    public List<Integer> getAlbumExpungeCandidates() {
        return queryForInts("select id from media_file where media_file.type = ? and not present",
                MediaFile.MediaType.ALBUM.name());
    }

    public List<Integer> getSongExpungeCandidates() {
        return queryForInts("select id from media_file where media_file.type in (?,?,?,?) and not present",
                MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(),
                MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.VIDEO.name());
    }

    public void expunge() {
        int minId = queryForInt("select min(id) from media_file where not present", 0);
        int maxId = queryForInt("select max(id) from media_file where not present", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from media_file where id between ? and ? and not present", id, id + batchSize);
        }
    }

    public List<MediaFile> getArtistAll(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.DIRECTORY.name(), "folders",
                MusicFolder.toPathList(musicFolders));
        return namedQuery(
                "select " + getQueryColoms() + " from media_file "
                        + "where type = :type and folder in (:folders) and present and artist is not null",
                rowMapper, args);
    }

    public int getChildSizeOf(String path) {
        return queryForInt("select count(id) from media_file where parent_path=? and present", 0, path);
    }

    public List<SortCandidate> getCopyableSortForAlbums(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders));
        return namedQuery("select known.name , known.sort from (select distinct album as name from media_file "
                + "where folder in (:folders) and present and type = :type and (album is not null and album_sort is null)) unknown "
                + "join (select distinct album as name, album_sort as sort from media_file "
                + "where folder in (:folders) and type = :type and album is not null and album_sort is not null and present) known "
                + "on known.name = unknown.name ", sortCandidateMapper, args);
    }

    public List<SortCandidate> getCopyableSortForPersons(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("typeDirAndAlbum",
                Arrays.asList(MediaType.DIRECTORY.name(), MediaType.ALBUM.name()), "typeMusic",
                MediaFile.MediaType.MUSIC.name(), "folders", MusicFolder.toPathList(folders));
        String query = "select known.name , known.sort from (select distinct artist as name from media_file "
                + "where folder in (:folders) and present and type in (:typeDirAndAlbum) and (artist is not null and artist_sort is null) "
                + "union select distinct album_artist as name from media_file "
                + "where folder in (:folders) and present and type not in (:typeDirAndAlbum) and (album_artist is not null and album_artist_sort is null) "
                + "union select distinct composer as name from media_file "
                + "where folder in (:folders) and present and type not in (:typeDirAndAlbum) and (composer is not null and composer_sort is null)) unknown "
                + "join (select distinct name, sort from "
                + "(select distinct album_artist as name, album_artist_sort as sort from media_file "
                + "where folder in (:folders) and type = :typeMusic and album_artist is not null and album_artist_sort is not null and present "
                + "union select distinct artist as name, artist_sort as sort from media_file "
                + "where folder in (:folders) and type = :typeMusic and artist is not null and artist_sort is not null and present "
                + "union select distinct composer as name, composer_sort as sort from media_file "
                + "where folder in (:folders) and type = :typeMusic and composer is not null and composer_sort is not null and present"
                + ") person_union) known on known.name = unknown.name";
        return namedQuery(query, sortCandidateMapper, args);
    }

    public int getCountInPlaylist(int playlistId) {
        return queryForInt("select count(*) from playlist_file, media_file "
                + "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present ", 0,
                playlistId);
    }

    public long countMediaFile(MediaType mediaType, List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = LegacyMap.of("type", mediaType.name(), "folders", MusicFolder.toPathList(folders));
        long defaultValue = 0;
        String sql = "select count(*) from media_file where present and type= :type and folder in(:folders)";
        List<Long> list = getNamedParameterJdbcTemplate().queryForList(sql, args, Long.class);
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
        int countAll = queryForInt("select count(*) from media_file where present and type = ? and album_artist = ?", 0,
                type, albumArtist);
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
        List<MediaFile> tmpResult = namedQuery("select " + getQueryColoms() + ", foo.irownum from " + "    (select "
                + "        (select count(id) from media_file where id < boo.id and type = :type and album_artist = :artist) as irownum, boo.* "
                + "    from (select * " + "        from media_file " + "        where type = :type "
                + "        and album_artist = :artist " + "        order by media_file_order, album_artist, album) boo "
                + ") as foo " + "where foo.irownum in ( :randomRownum ) limit :limit ", iRowMapper, args);

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
        return queryForInt(
                "select count(id) from media_file "
                        + "where album_artist=? and album=? and present and type in (?,?,?)",
                0, artist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(), MediaType.PODCAST.name());
    }

    public List<SortCandidate> getSortForPersonWithoutSorts(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("typeDirAndAlbum",
                Arrays.asList(MediaType.DIRECTORY.name(), MediaType.ALBUM.name()), "folders",
                MusicFolder.toPathList(folders));
        String query = "select name, null as sort from(select distinct artist as name from media_file "
                + "where folder in (:folders) and present and type not in (:typeDirAndAlbum) and (artist is not null and artist_sort is null) "
                + "union select distinct album_artist as name from media_file "
                + "where folder in (:folders) and present and type not in (:typeDirAndAlbum) and (album_artist is not null and album_artist_sort is null) "
                + "union select distinct composer as name from media_file "
                + "where folder in (:folders) and present and type not in (:typeDirAndAlbum) and (composer is not null and composer_sort is null)) no_sorts";
        return namedQuery(query, sortCandidateMapper, args);
    }

    public List<Integer> getSortOfAlbumToBeFixed(List<SortCandidate> candidates) {
        Map<String, Object> args = LegacyMap.of("names",
                candidates.stream().map(SortCandidate::getName).collect(Collectors.toList()), "sotes",
                candidates.stream().map(SortCandidate::getSort).collect(Collectors.toList()));
        return namedQuery("select distinct id from media_file "
                + "where present and album in (:names) and (album_sort is null or album_sort not in(:sotes))  "
                + "order by id ", (rs, rowNum) -> rs.getInt(1), args);
    }

    public List<Integer> getSortOfArtistToBeFixed(@NonNull List<SortCandidate> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("names",
                candidates.stream().map(SortCandidate::getName).collect(Collectors.toList()), "sotes",
                candidates.stream().map(SortCandidate::getSort).collect(Collectors.toList()));
        return namedQuery(
                "select distinct id " + "from (select id " + "   from media_file " + "   where present "
                        + "   and artist in (:names) " + "   and (artist_sort is null "
                        + "       or artist_sort not in(:sotes)) " + "   union " + "   select id "
                        + "   from media_file " + "   where present " + "   and type not in ('DIERECTORY', 'ALBUM') "
                        + "   and album_artist in (:names) " + "   and (album_artist_sort is null "
                        + "       or album_artist_sort not in(:sotes)) " + "   union " + "   select id "
                        + "   from media_file " + "   where present " + "   and type not in ('DIERECTORY', 'ALBUM') "
                        + "   and composer in (:names) " + "   and (composer_sort is null "
                        + "       or composer_sort not in(:sotes))) to_be_fixed " + "order by id",
                (rs, rowNum) -> rs.getInt(1), args);
    }

    public List<SortCandidate> getSortForAlbumWithoutSorts(List<MusicFolder> folders) {
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders));
        return namedQuery("select distinct album as name, null as sort from media_file "
                + "where present and folder in (:folders) and type = :type and (album is not null and album_sort is null) ",
                sortCandidateMapper, args);
    }

    public List<SortCandidate> guessAlbumSorts(List<MusicFolder> folders) {
        List<SortCandidate> result = new ArrayList<>();
        if (folders.isEmpty()) {
            return result;
        }
        Map<String, Object> args = Map.of("type", MediaFile.MediaType.ALBUM.name(), "folders",
                MusicFolder.toPathList(folders));
        List<SortCandidate> candidates = namedQuery("select name, sort, duplicates_with_changed.changed "
                + "from (select distinct album as name, album_sort as sort, changed from media_file m1 "
                + "join (select name, count(sort) from ( select distinct album as name, album_sort as sort from media_file  "
                + "where album is not null and album_sort is not null and folder in (:folders)) named_album group by name having 1 < count(sort)) duplicates "
                + "on m1.album = duplicates.name) duplicates_with_changed "
                + "join media_file m on type = :type and folder in (:folders) and name = album "
                + "group by name, sort, duplicates_with_changed.changed having max(m.changed) = duplicates_with_changed.changed",
                sortCandidateMapper, args);
        candidates.forEach((candidate) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(candidate.getName()))) {
                result.add(candidate);
            }
        });
        return result;
    }

    public List<SortCandidate> guessPersonsSorts(List<MusicFolder> folders) {
        List<SortCandidate> result = new ArrayList<>();
        if (folders.isEmpty()) {
            return result;
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.MUSIC.name(), "folders",
                MusicFolder.toPathList(folders));
        String query = "select name, sort, source, duplicate_persons_with_priority.changed from "
                + "(select distinct name, sort, source, changed from "
                + "(select distinct album_artist as name, album_artist_sort as sort, 1 as source, type, changed from media_file "
                + "where folder in(:folders) and type = :type and album_artist is not null and album_artist_sort is not null and present "
                + "union select distinct artist as name, artist_sort as sort, 2 as source, type, changed from media_file "
                + "where folder in(:folders) and type = :type and artist is not null and artist_sort is not null and present "
                + "union select distinct composer as name, composer_sort as sort, 3 as source, type, changed from media_file "
                + "where folder in(:folders) and type = :type and composer is not null and composer_sort is not null and present) as person_all_with_priority "
                + "where name in (select name from (select name, count(sort) from (select distinct name, sort from "
                + "(select distinct album_artist as name, album_artist_sort as sort from media_file "
                + "where folder in(:folders) and type = :type and album_artist is not null and album_artist_sort is not null and present "
                + "union select distinct artist as name, artist_sort as sort from media_file "
                + "where folder in(:folders) and type = :type and artist is not null and artist_sort is not null and present "
                + "union select distinct composer as name, composer_sort as sort from media_file "
                + "where folder in(:folders) and type = :type and composer is not null and composer_sort is not null and present) person_union) duplicate "
                + "group by name having 1 < count(sort)) duplicate_names)) duplicate_persons_with_priority "
                + "join media_file m on folder in(:folders) and type = :type and name in(album_artist, artist ,composer) "
                + "group by name, sort, source, duplicate_persons_with_priority.changed "
                + "having max(m.changed) = duplicate_persons_with_priority.changed "
                + "order by name, changed desc, source";
        List<SortCandidate> candidates = namedQuery(query, sortCandidateMapper, args);
        candidates.forEach((candidate) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(candidate.getName()))) {
                result.add(candidate);
            }
        });
        return result;
    }

    public void updateAlbumSort(SortCandidate candidate) {
        update("update media_file set album_reading = ?, album_sort = ? "
                + "where present and album = ? and (album_sort is null or album_sort <> ?)", candidate.getReading(),
                candidate.getSort(), candidate.getName(), candidate.getSort());
    }

    public void updateArtistSort(SortCandidate candidate) {
        update("update media_file set artist_reading = ?, artist_sort = ? "
                + "where present and artist = ? and (artist_sort is null or artist_sort <> ?)", candidate.getReading(),
                candidate.getSort(), candidate.getName(), candidate.getSort());
        update("update media_file set album_artist_reading = ?, album_artist_sort = ? "
                + "where present and type not in ('DIERECTORY', 'ALBUM') and album_artist = ? and (album_artist_sort is null or album_artist_sort <> ?)",
                candidate.getReading(), candidate.getSort(), candidate.getName(), candidate.getSort());
        update("update media_file set composer_sort = ? "
                + "where present and type not in ('DIERECTORY', 'ALBUM') and composer = ? and (composer_sort is null or composer_sort <> ?)",
                candidate.getSort(), candidate.getName(), candidate.getSort());
    }

    private static class MediaFileMapper implements RowMapper<MediaFile> {
        @SuppressWarnings("PMD.NPathComplexity") // #863
        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(rs.getInt(1), rs.getString(2), rs.getString(3),
                    MediaFile.MediaType.valueOf(rs.getString(4)), rs.getString(5), rs.getString(6), rs.getString(7),
                    rs.getString(8), rs.getString(9), rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11), rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13), rs.getInt(14) == 0 ? null : rs.getInt(14), rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16), rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18), rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20), rs.getString(21), rs.getInt(22), nullableInstantOf(rs.getTimestamp(23)),
                    rs.getString(24), nullableInstantOf(rs.getTimestamp(25)), nullableInstantOf(rs.getTimestamp(26)),
                    nullableInstantOf(rs.getTimestamp(27)), nullableInstantOf(rs.getTimestamp(28)), rs.getBoolean(29),
                    rs.getInt(30), rs.getString(31), rs.getString(32),
                    // JP >>>>
                    rs.getString(33), rs.getString(34), rs.getString(35), rs.getString(36), rs.getString(37),
                    rs.getString(38), rs.getString(39), rs.getString(40), rs.getString(41), rs.getString(42),
                    rs.getString(43), rs.getString(44), rs.getString(45), rs.getInt(46)); // <<<< JP
        }
    }

    static class RandomSongsQueryBuilder {

        private final RandomSearchCriteria criteria;

        public RandomSongsQueryBuilder(RandomSearchCriteria criteria) {
            this.criteria = criteria;
        }

        public String build() {
            StringBuilder query = new StringBuilder(1024); // 988 + param
            query.append("select ").append(prefix(QUERY_COLUMNS, "media_file")).append(" from media_file");
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
                return Optional
                        .of(" left outer join starred_media_file on media_file.id = starred_media_file.media_file_id"
                                + " and starred_media_file.username = :username");
            }
            return Optional.empty();
        }

        Optional<String> getIfJoinAlbumRating() {
            boolean joinAlbumRating = criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null;
            if (joinAlbumRating) {
                return Optional.of(" left outer join media_file media_album on media_album.type = 'ALBUM'"
                        + " and media_album.album = media_file.album " + "and media_album.artist = media_file.artist "
                        + "left outer join user_rating on user_rating.path = media_album.path"
                        + " and user_rating.username = :username");
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

    private static class MediaFileInternalRowMapper implements RowMapper<MediaFile> {

        private final RowMapper<MediaFile> deligate;

        public MediaFileInternalRowMapper(RowMapper<MediaFile> m) {
            super();
            this.deligate = m;
        }

        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile mediaFile = deligate.mapRow(rs, rowNum);
            if (mediaFile != null) {
                mediaFile.setRownum(rs.getInt("irownum"));
            }
            return mediaFile;
        }

    }

    private static class GenreMapper implements RowMapper<Genre> {
        @Override
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Genre(rs.getString(1), rs.getInt(2), rs.getInt(3));
        }
    }

    public RowMapper<Genre> getGenreMapper() {
        return genreRowMapper;
    }

    public RowMapper<MediaFile> getMediaFileMapper() {
        return rowMapper;
    }

    public static final String getQueryColoms() {
        return QUERY_COLUMNS;
    }

    public static final String getGenreColoms() {
        return GENRE_COLUMNS;
    }

}
