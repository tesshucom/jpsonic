/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.util.Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
@Repository
public class MediaFileDao extends AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(MediaFileDao.class);
    private static final String INSERT_COLUMNS = "path, folder, type, format, title, album, artist, album_artist, disc_number, " +
                                                "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
                                                "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, " +
                                                "version, artist_reading, title_sort, album_sort, artist_sort, album_artist_sort, album_reading, mb_release_id, " +
                                                "composer, composer_sort, album_artist_reading, _order";

    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private static final String GENRE_COLUMNS = "name, song_count, album_count";

    private static final int JP_VERSION = 6;
    public static final int VERSION = 4 + JP_VERSION;

    private final RowMapper<MediaFile> rowMapper = new MediaFileMapper();
    private final RowMapper<MediaFile> musicFileInfoRowMapper = new MusicFileInfoMapper();
    private final RowMapper<Genre> genreRowMapper = new GenreMapper();
    private final RowMapper<MediaFile> artistSortCandidateMapper = new ArtistSortCandidateMapper();
    private final RowMapper<MediaFile> albumSortCandidateMapper = new AlbumSortCandidateMapper();

    public void clearOrder() {
        update("update media_file set _order = -1");
    }

    /**
     * Returns the media file for the given path.
     *
     * @param path The path.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + QUERY_COLUMNS + " from media_file where path=?", rowMapper, path);
    }

    /**
     * Returns the media file for the given ID.
     *
     * @param id The ID.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(int id) {
        return queryOne("select " + QUERY_COLUMNS + " from media_file where id=?", rowMapper, id);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + QUERY_COLUMNS + " from media_file where parent_path=? and present", rowMapper, path);
    }
    
    /**
     * Returns the media file that are direct children of the given path.
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(final long offset, final long count, String path, boolean byYear) {
        String order = byYear ? "year" : "_order";
        return query("select " + QUERY_COLUMNS + " from media_file where parent_path=? and present order by " +
                order + " limit ? offset ?", rowMapper, path, count, offset);
    }

    public int getChildSizeOf(String path) {
        return queryForInt("select count(id) from media_file where parent_path=? and present", 0, path);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return query("select " + prefix(QUERY_COLUMNS, "media_file") + " from playlist_file, media_file where " +
                     "media_file.id = playlist_file.media_file_id and " +
                     "playlist_file.playlist_id = ? " +
                     "order by playlist_file.id", rowMapper, playlistId);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId, long offset, long count) {
        return query("select " + prefix(QUERY_COLUMNS, "media_file") + " from playlist_file, media_file where " +
                     "media_file.id = playlist_file.media_file_id and " +
                     "playlist_file.playlist_id = ? and present " +
                     "order by playlist_file.id limit ? offset ?", rowMapper, playlistId, count, offset);
    }

    public int getCountInPlaylist(int playlistId) {
        return queryForInt("select count(*) from playlist_file, media_file " +
                "where " +
                "media_file.id = playlist_file.media_file_id and " +
                "playlist_file.playlist_id = ? and present ", 0, playlistId);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return query("select " + QUERY_COLUMNS + " from media_file where album_artist=? and album=? and present " +
                     "and type in (?,?,?) order by track_number", rowMapper,
                     artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.PODCAST.name());
    }

    public List<MediaFile> getSongsForAlbum(MediaFile album, final long offset, final long count) {
        return query("select " + QUERY_COLUMNS + " from media_file where parent_path=? and present " +
                "and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, album.getPath(), MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album, final long offset, final long count) {
        return query("select " + QUERY_COLUMNS + " from media_file where album_artist=? and album=? and present " +
                "and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name(), count, offset);
    }

    public int getSongsCountForAlbum(String artist, String album) {
        return queryForInt("select count(id) from media_file where album_artist=? and album=? and present " +
                     "and type in (?,?,?)", 0,
                     artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.PODCAST.name());
    }

    public List<MediaFile> getVideos(final int count, final int offset, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.VIDEO.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS
                          + " from media_file where type = :type and present and folder in (:folders) " +
                          "order by title limit :count offset :offset", rowMapper, args);
    }

    public MediaFile getArtistByName(final String name, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.DIRECTORY.name());
        args.put("name", name);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQueryOne("select " + QUERY_COLUMNS + " from media_file where type = :type and artist = :name " +
                             "and present and folder in (:folders)", rowMapper, args);
    }

    /**
     * Creates or updates a media file.
     *
     * @param file The media file to create/update.
     */
    @Transactional
    public void createOrUpdateMediaFile(MediaFile file) {
        LOG.trace("Creating/Updating new media file at {}", file.getPath());
        String sql = "update media_file set " +
                     "folder=?," +
                     "type=?," +
                     "format=?," +
                     "title=?," +
                     "album=?," +
                     "artist=?," +
                     "album_artist=?," +
                     "disc_number=?," +
                     "track_number=?," +
                     "year=?," +
                     "genre=?," +
                     "bit_rate=?," +
                     "variable_bit_rate=?," +
                     "duration_seconds=?," +
                     "file_size=?," +
                     "width=?," +
                     "height=?," +
                     "cover_art_path=?," +
                     "parent_path=?," +
                     "play_count=?," +
                     "last_played=?," +
                     "comment=?," +
                     "changed=?," +
                     "last_scanned=?," +
                     "children_last_updated=?," +
                     "present=?, " +
                     "version=?, " +
                     "artist_reading=?, " +
                     "title_sort=?, " +
                     "album_sort=?, " +
                     "artist_sort=?, " +
                     "album_artist_sort=?, " +
                     "album_reading=?, " +
                     "mb_release_id=?, " +
                     "composer=?, " +
                     "composer_sort=?, " +
                     "album_artist_reading=?, " +
                     "_order=? " +
                     "where path=?";

        LOG.trace("Updating media file {}", Util.debugObject(file));

        int n = update(sql,
                       file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                       file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                       file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                       file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                       file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(), file.isPresent(),
                       VERSION,
                       file.getArtistReading(),
                       file.getTitleSort(),
                       file.getAlbumSort(),
                       file.getArtistSort(),
                       file.getAlbumArtistSort(),
                       file.getAlbumReading(),
                       file.getMusicBrainzReleaseId(),
                       file.getComposer(),
                       file.getComposerSort(),
                       file.getAlbumArtistReading(),
                       file.getOrder(),
                       file.getPath());

        if (n == 0) {

            // Copy values from obsolete table music_file_info.
            MediaFile musicFileInfo = getMusicFileInfo(file.getPath());
            if (musicFileInfo != null) {
                file.setComment(musicFileInfo.getComment());
                file.setLastPlayed(musicFileInfo.getLastPlayed());
                file.setPlayCount(musicFileInfo.getPlayCount());
            }

            update("insert into media_file (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                   file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                   file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                   file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                   file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                   file.getCreated(), file.getChanged(), file.getLastScanned(),
                   file.getChildrenLastUpdated(), file.isPresent(), VERSION,
                   file.getArtistReading(), file.getTitleSort(), file.getAlbumSort(), file.getArtistSort(), file.getAlbumArtistSort(), file.getAlbumReading(),
                   file.getMusicBrainzReleaseId(), file.getComposer(), file.getComposerSort(), file.getAlbumArtistReading(), -1);
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    /**
     * Update artistSorts all.
     * @param artist The artist to update.
     * @param artistSort Update value.
     */
    @Transactional
    @Deprecated
    public int updateArtistSort(String artist, String artistSort) {
        LOG.trace("Updating media file at {}", artist);
        String sql = "update media_file set artist_sort = ? where artist = ? and type in (?, ?)";
        LOG.trace("Updating media file {}", artist);
        return update(sql, artistSort, artist, MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
    }

    /**
     * Update albumSorts all.
     * @param album The artist to update.
     * @param albumSort Update value.
     */
    @Transactional
    public int updateAlbumSort(String album, String albumSort) {
        LOG.trace("Updating media file at {}", album);
        String sql = "update media_file set album_sort = ? where album = ? and type = ?";
        LOG.trace("Updating media file {}", album);
        return update(sql, albumSort, album, MediaFile.MediaType.ALBUM.name());
    }

    /**
     * Update albumArtistSorts all.
     * @param artist The artist to update.
     * @param albumArtistSort Update value.
     */
    @Transactional
    public int updateAlbumArtistSort(String artist, String albumArtistSort) {
        LOG.trace("Updating media file at {}", artist);
        String sql = "update media_file set album_artist_sort = ? where artist = ? and artist_reading <> ? and type in (?, ?)";
        LOG.trace("Updating media file {}", artist);
        return update(sql, albumArtistSort, artist, albumArtistSort, MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
    }

    private MediaFile getMusicFileInfo(String path) {
        return queryOne("select play_count, last_played, comment from music_file_info where path=?", musicFileInfoRowMapper, path);
    }

    public void deleteMediaFile(String path) {
        update("update media_file set present=false, children_last_updated=? where path=?", new Date(0L), path);
    }

    public int getGenresCount() {
        return queryForInt("select count(*) from genre", 0);
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + GENRE_COLUMNS + " from genre order by " + orderBy + " desc", genreRowMapper);
    }

    public List<Genre> getGenres(boolean sortByAlbum, long offset, long count) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + GENRE_COLUMNS + " from genre order by " + orderBy + " desc limit ? offset ?", genreRowMapper, count, offset);
    }

    public void updateGenres(List<Genre> genres) {
        update("delete from genre");
        for (Genre genre : genres) {
            update("insert into genre(" + GENRE_COLUMNS + ") values(?, ?, ?)",
                   genre.getName(), genre.getSongCount(), genre.getAlbumCount());
        }
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);

        return namedQuery("select " + QUERY_COLUMNS
                          + " from media_file where type = :type and play_count > 0 and present and folder in (:folders) " +
                          "order by play_count desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS
                          + " from media_file where type = :type and last_played is not null and present " +
                          "and folder in (:folders) order by last_played desc limit :count offset :offset", rowMapper, args);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(final long offset, final long count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);

        return namedQuery("select " + QUERY_COLUMNS
                          + " from media_file where type = :type and folder in (:folders) and present " +
                          "order by created desc limit :count offset :offset", rowMapper, args);
    }

    public List<MediaFile> getArtistAll(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.DIRECTORY.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQuery("select " + QUERY_COLUMNS + " from media_file where type = :type and folder in (:folders) and present ", rowMapper, args);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums in these folders.
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        String namedQuery =
                "select " + QUERY_COLUMNS + " from media_file where type = :type and folder in (:folders) and present " +
                "order by _order, album_reading limit :count offset :offset";
        if (byArtist) {
            namedQuery =
                    "select distinct " + prefix(QUERY_COLUMNS, "al") + ", ar._order as ar_order, al._order as al_order "
                            + "from media_file al join media_file ar on ar.path = al.parent_path "
                            + "where al.type = :type and al.folder in (:folders) and al.present " +
                    "order by ar_order, al.artist_reading, al_order, al.album_reading limit :count offset :offset";
        }

        return namedQuery(namedQuery, rowMapper, args);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(final int offset, final int count, final int fromYear, final int toYear,
                                           final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("fromYear", fromYear);
        args.put("toYear", toYear);
        args.put("count", count);
        args.put("offset", offset);

        if (fromYear <= toYear) {
            return namedQuery("select " + QUERY_COLUMNS
                              + " from media_file where type = :type and folder in (:folders) and present " +
                              "and year between :fromYear and :toYear order by year limit :count offset :offset",
                              rowMapper, args);
        } else {
            return namedQuery("select " + QUERY_COLUMNS
                              + " from media_file where type = :type and folder in (:folders) and present " +
                              "and year between :toYear and :fromYear order by year desc limit :count offset :offset",
                              rowMapper, args);
        }
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final String genre,
                                            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("genre", genre);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + QUERY_COLUMNS + " from media_file where type = :type and folder in (:folders) " +
                          "and present and genre = :genre limit :count offset :offset", rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final String genre, final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("types", Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(), MediaFile.MediaType.AUDIOBOOK.name()));
        args.put("genre", genre);
        args.put("count", count);
        args.put("offset", offset);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQuery("select " + QUERY_COLUMNS + " from media_file where type in (:types) and genre = :genre " +
                          "and present and folder in (:folders) limit :count offset :offset",
                          rowMapper, args);
    }

    public List<MediaFile> getSongsByGenre(final List<String> genres, final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("types", Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(), MediaFile.MediaType.AUDIOBOOK.name()));
        args.put("genres", genres);
        args.put("count", count);
        args.put("offset", offset);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQuery("select " + prefix(QUERY_COLUMNS, "s") + " from media_file s " +
                          "join media_file al on s.parent_path = al.path " + 
                          "join media_file ar on al.parent_path = ar.path " +
                          "where s.type in (:types) and s.genre in (:genres) " +
                          "and s.present and s.folder in (:folders) limit :count offset :offset " +
                          "order by ar._order, al._order, s.track_number", rowMapper, args);
    }

    public List<MediaFile> getSongsByArtist(String artist, int offset, int count) {
        return query("select " + QUERY_COLUMNS
                     + " from media_file where type in (?,?,?) and artist=? and present limit ? offset ?",
                     rowMapper, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(), MediaFile.MediaType.AUDIOBOOK.name(), artist, count, offset);
    }

    public MediaFile getSongByArtistAndTitle(final String artist, final String title, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || StringUtils.isBlank(title) || StringUtils.isBlank(artist)) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("artist", artist);
        args.put("title", title);
        args.put("type", MediaFile.MediaType.MUSIC.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQueryOne("select " + QUERY_COLUMNS + " from media_file where artist = :artist " +
                             "and title = :title and type = :type and present and folder in (:folders)",
                             rowMapper, args);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(final int offset, final int count, final String username,
                                            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("username", username);
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file") + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type = :type and media_file.folder in (:folders) and starred_media_file.username = :username " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    /**
     * Returns the most recently starred directories.
     *
     * @param offset       Number of directories to skip.
     * @param count        Maximum number of directories to return.
     * @param username     Returns directories starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred directories for this user.
     */
    public List<MediaFile> getStarredDirectories(final int offset, final int count, final String username,
                                                 final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.DIRECTORY.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("username", username);
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file") + " from starred_media_file, media_file " +
                          "where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type = :type and starred_media_file.username = :username and " +
                          "media_file.folder in (:folders) " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    /**
     * Returns the most recently starred files.
     *
     * @param offset       Number of files to skip.
     * @param count        Maximum number of files to return.
     * @param username     Returns files starred by this user.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently starred files for this user.
     */
    public List<MediaFile> getStarredFiles(final int offset, final int count, final String username,
                                           final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("types", Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(), MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.VIDEO.name()));
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("username", username);
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery("select " + prefix(QUERY_COLUMNS, "media_file") + " from starred_media_file, media_file where media_file.id = starred_media_file.media_file_id and " +
                          "media_file.present and media_file.type in (:types) and starred_media_file.username = :username and " +
                          "media_file.folder in (:folders) " +
                          "order by starred_media_file.created desc limit :count offset :offset",
                          rowMapper, args);
    }

    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, final String username) {
        if (criteria.getMusicFolders().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toPathList(criteria.getMusicFolders()));
        args.put("username", username);
        args.put("fromYear", criteria.getFromYear());
        args.put("toYear", criteria.getToYear());
        args.put("genres", criteria.getGenres());
        args.put("minLastPlayed", criteria.getMinLastPlayedDate());
        args.put("maxLastPlayed", criteria.getMaxLastPlayedDate());
        args.put("minAlbumRating", criteria.getMinAlbumRating());
        args.put("maxAlbumRating", criteria.getMaxAlbumRating());
        args.put("minPlayCount", criteria.getMinPlayCount());
        args.put("maxPlayCount", criteria.getMaxPlayCount());
        args.put("starred", criteria.isShowStarredSongs());
        args.put("unstarred", criteria.isShowUnstarredSongs());
        args.put("format", criteria.getFormat());

        boolean joinAlbumRating = (criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null);
        boolean joinStarred = (criteria.isShowStarredSongs() ^ criteria.isShowUnstarredSongs());

        String query = "select " + prefix(QUERY_COLUMNS, "media_file") + " from media_file ";

        if (joinStarred) {
            query += "left outer join starred_media_file on media_file.id = starred_media_file.media_file_id and starred_media_file.username = :username ";
        }

        if (joinAlbumRating) {
            query += "left outer join media_file media_album on media_album.type = 'ALBUM' and media_album.album = media_file.album and media_album.artist = media_file.artist ";
            query += "left outer join user_rating on user_rating.path = media_album.path and user_rating.username = :username ";
        }

        query += " where media_file.present and media_file.type = 'MUSIC'";

        if (!criteria.getMusicFolders().isEmpty()) {
            query += " and media_file.folder in (:folders)";
        }

        if (criteria.getGenres() != null) {
            query += " and media_file.genre in (:genres)";
        }

        if (criteria.getFormat() != null) {
            query += " and media_file.format = :format";
        }

        if (criteria.getFromYear() != null) {
            query += " and media_file.year >= :fromYear";
        }

        if (criteria.getToYear() != null) {
            query += " and media_file.year <= :toYear";
        }

        if (criteria.getMinLastPlayedDate() != null) {
            query += " and media_file.last_played >= :minLastPlayed";
        }

        if (criteria.getMaxLastPlayedDate() != null) {
            if (criteria.getMinLastPlayedDate() == null) {
                query += " and (media_file.last_played is null or media_file.last_played <= :maxLastPlayed)";
            } else {
                query += " and media_file.last_played <= :maxLastPlayed";
            }
        }

        if (criteria.getMinAlbumRating() != null) {
            query += " and user_rating.rating >= :minAlbumRating";
        }

        if (criteria.getMaxAlbumRating() != null) {
            if (criteria.getMinAlbumRating() == null) {
                query += " and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)";
            } else {
                query += " and user_rating.rating <= :maxAlbumRating";
            }
        }

        if (criteria.getMinPlayCount() != null) {
            query += " and media_file.play_count >= :minPlayCount";
        }

        if (criteria.getMaxPlayCount() != null) {
            if (criteria.getMinPlayCount() == null) {
                query += " and (media_file.play_count is null or media_file.play_count <= :maxPlayCount)";
            } else {
                query += " and media_file.play_count <= :maxPlayCount";
            }
        }

        if (criteria.isShowStarredSongs() && !criteria.isShowUnstarredSongs()) {
            query += " and starred_media_file.id is not null";
        }

        if (criteria.isShowUnstarredSongs() && !criteria.isShowStarredSongs()) {
            query += " and starred_media_file.id is null";
        }

        query += " order by rand()";

        return namedQueryWithLimit(query, rowMapper, args, criteria.getCount());
    }

    public int getAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQueryForInt("select count(*) from media_file where type = :type and folder in (:folders) and present", 0, args);
    }

    public int getPlayedAlbumCount(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQueryForInt("select count(*) from media_file where type = :type " +
                                "and play_count > 0 and present and folder in (:folders)", 0, args);
    }

    public int getStarredAlbumCount(final String username, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return 0;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("username", username);
        return namedQueryForInt("select count(*) from starred_media_file, media_file " +
                                "where media_file.id = starred_media_file.media_file_id " +
                                "and media_file.type = :type " +
                                "and media_file.present " +
                                "and media_file.folder in (:folders) " +
                                "and starred_media_file.username = :username",
                                0, args);
    }

    public List<MediaFile> getArtistSortCandidate() {
        return query(
                "select m.id as id, m.artist as artist, artist_reading, dic.artist_sort as artist_sort from media_file m "
                        + "join (select distinct  artist, artist_sort from media_file where artist_sort is not null  and present order by artist) dic "
                        + "on dic.artist = m.artist "
                        + "where type = ? and present "
                        + "and artist_reading is not null "
                        + "order by artist, artist_sort",
                        artistSortCandidateMapper,
                        MediaFile.MediaType.DIRECTORY.name());
    }

    public List<MediaFile> getAlbumSortCandidate() {
        return query(
                " select m.id as id, m.album as album, m.album_reading, dic.album_sort as album_sort"
                        + " from media_file m"
                        + " join (select distinct  album, album_sort from media_file where album_sort is not null and present order by album) dic"
                        + " on dic.album = m.album"
                        + " where type = ? and present"
                        + " and album_reading is not null"
                        + " and album_reading <> dic.album_sort order by album, album_sort",
                        albumSortCandidateMapper,
                        MediaFile.MediaType.ALBUM.name());
    }

    public List<MediaFile> getSortedAlbums() {
        return query("select " + QUERY_COLUMNS +
                " from media_file" +
                " where album_reading is not null" +
                " or album_sort is not null" +
                " and type = ? and present",
                rowMapper, MediaFile.MediaType.ALBUM.name());
    }

    @Deprecated
    public void clearSort() {
        update("update media_file set artist_sort = null where type in(?, ?) and present",
                MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
        update("update media_file set album_artist_sort = null where type in(?, ?) and present",
                MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
        update("update media_file set album_sort = null where type in(?, ?) and present",
                MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
    }

    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        update("insert into starred_media_file(media_file_id, username, created) values (?,?,?)", id, username, new Date());
    }

    public void unstarMediaFile(int id, String username) {
        update("delete from starred_media_file where media_file_id=? and username=?", id, username);
    }

    public Date getMediaFileStarredDate(int id, String username) {
        return queryForDate("select created from starred_media_file where media_file_id=? and username=?", null, id, username);
    }

    public void markPresent(String path, Date lastScanned) {
        update("update media_file set present=?, last_scanned = ? where path=?", true, lastScanned, path);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select min(id) from media_file where last_scanned < ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from media_file where last_scanned < ? and present", 0, lastScanned);

        final int batchSize = 1000;
        Date childrenLastUpdated = new Date(0L);  // Used to force a children rescan if file is later resurrected.
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update media_file set present=false, children_last_updated=? where id between ? and ? and " +
                            "last_scanned < ? and present",
                   childrenLastUpdated, id, id + batchSize, lastScanned);
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

    private static class MediaFileMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    MediaFile.MediaType.valueOf(rs.getString(4)),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11),
                    rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13),
                    rs.getInt(14) == 0 ? null : rs.getInt(14),
                    rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16),
                    rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18),
                    rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20),
                    rs.getString(21),
                    rs.getInt(22),
                    rs.getTimestamp(23),
                    rs.getString(24),
                    rs.getTimestamp(25),
                    rs.getTimestamp(26),
                    rs.getTimestamp(27),
                    rs.getTimestamp(28),
                    rs.getBoolean(29),
                    rs.getInt(30),
                    rs.getString(31),
                    rs.getString(32),
                    rs.getString(33),
                    rs.getString(34),
                    rs.getString(35),
                    rs.getString(36),
                    rs.getString(37),
                    rs.getString(38),
                    rs.getString(39),
                    rs.getString(40),
                    rs.getInt(41));
        }
    }

    private static class MusicFileInfoMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPlayCount(rs.getInt(1));
            file.setLastPlayed(rs.getTimestamp(2));
            file.setComment(rs.getString(3));
            return file;
        }
    }

    private static class GenreMapper implements RowMapper<Genre> {
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Genre(rs.getString(1), rs.getInt(2), rs.getInt(3));
        }
    }

    private static class ArtistSortCandidateMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setId(rs.getInt(1));
            file.setArtist(rs.getString(2));
            file.setArtistReading(rs.getString(3));
            file.setArtistSort(rs.getString(4));
            return file;
        }
    }

    private static class AlbumSortCandidateMapper implements RowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setId(rs.getInt(1));
            file.setAlbumName(rs.getString(2));
            file.setAlbumReading(rs.getString(3));
            file.setAlbumSort(rs.getString(4));
            return file;
        }
    }

}
