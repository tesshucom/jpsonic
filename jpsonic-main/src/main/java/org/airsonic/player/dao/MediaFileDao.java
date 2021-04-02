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

package org.airsonic.player.dao;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.util.LegacyMap;
import org.airsonic.player.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Only DAO is allowed to exclude this rule #827
@Repository
public class MediaFileDao extends AbstractDao {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileDao.class);
    private static final String INSERT_COLUMNS = "path, folder, type, format, title, album, artist, album_artist, disc_number, "
            + "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, "
            + "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, "
            + "version, mb_release_id, mb_recording_id"
            // JP >>>>
            + ", " + "composer, artist_sort, album_sort, title_sort, album_artist_sort, composer_sort, "
            + "artist_reading, album_reading, album_artist_reading, "
            + "artist_sort_raw, album_sort_raw, album_artist_sort_raw, composer_sort_raw, " + "media_file_order"; // <<<<
                                                                                                                  // JP
    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;
    private static final String GENRE_COLUMNS = "name, song_count, album_count";
    private static final int JP_VERSION = 8;
    public static final int VERSION = 4 + JP_VERSION;

    private final RowMapper<MediaFile> rowMapper;
    private final RowMapper<MediaFile> musicFileInfoRowMapper;
    private final RowMapper<Genre> genreRowMapper;

    public MediaFileDao(DaoHelper daoHelper) {
        super(daoHelper);
        rowMapper = new MediaFileMapper();
        musicFileInfoRowMapper = new MusicFileInfoMapper();
        genreRowMapper = new GenreMapper();
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

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return query("select " + prefix(QUERY_COLUMNS, "media_file") + " from playlist_file, media_file where "
                + "media_file.id = playlist_file.media_file_id and " + "playlist_file.playlist_id = ? "
                + "order by playlist_file.id", rowMapper, playlistId);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return query(
                "select " + QUERY_COLUMNS + " from media_file where album_artist=? and album=? and present "
                        + "and type in (?,?,?) order by disc_number, track_number",
                rowMapper, artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name());
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

    /**
     * Creates or updates a media file.
     *
     * @param file
     *            The media file to create/update.
     */
    @Transactional
    public void createOrUpdateMediaFile(MediaFile file) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating/Updating new media file at {}", file.getPath());
        }
        String sql = "update media_file set " + "folder=?," + "type=?," + "format=?," + "title=?," + "album=?,"
                + "artist=?," + "album_artist=?," + "disc_number=?," + "track_number=?," + "year=?," + "genre=?,"
                + "bit_rate=?," + "variable_bit_rate=?," + "duration_seconds=?," + "file_size=?," + "width=?,"
                + "height=?," + "cover_art_path=?," + "parent_path=?," + "play_count=?," + "last_played=?,"
                + "comment=?," + "changed=?," + "last_scanned=?," + "children_last_updated=?," + "present=?, "
                + "version=?, " + "mb_release_id=?, " + "mb_recording_id=? "
                // JP >>>>
                + ", " + "composer=?, " + "artist_sort=?, " + "album_sort=?, " + "title_sort=?, "
                + "album_artist_sort=?, " + "composer_sort=?, " + "artist_reading=?, " + "album_reading=?, "
                + "album_artist_reading=?, " + "artist_sort_raw=?, " + "album_sort_raw=?, "
                + "album_artist_sort_raw=?, " + "composer_sort_raw=?, " + "media_file_order=? " // <<<< JP
                + "where path=?";
        if (LOG.isTraceEnabled()) {
            LOG.trace("Updating media file {}", PlayerUtils.debugObject(file));
        }

        int n = update(sql, file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(),
                file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(),
                file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(),
                file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(),
                file.getComment(), file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(),
                file.isPresent(), VERSION, file.getMusicBrainzReleaseId(), file.getMusicBrainzRecordingId(),
                // JP >>>>
                file.getComposer(), file.getArtistSort(), file.getAlbumSort(), file.getTitleSort(),
                file.getAlbumArtistSort(), file.getComposerSort(), file.getArtistReading(), file.getAlbumReading(),
                file.getAlbumArtistReading(), file.getArtistSortRaw(), file.getAlbumSortRaw(),
                file.getAlbumArtistSortRaw(), file.getComposerSortRaw(), file.getOrder(), // <<<< JP
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
                    file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(),
                    file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(),
                    file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(), file.isVariableBitRate(),
                    file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                    file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(),
                    file.getComment(), file.getCreated(), file.getChanged(), file.getLastScanned(),
                    file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getMusicBrainzReleaseId(),
                    file.getMusicBrainzRecordingId(),
                    // JP >>>>
                    file.getComposer(), file.getArtistSort(), file.getAlbumSort(), file.getTitleSort(),
                    file.getAlbumArtistSort(), file.getComposerSort(), file.getArtistReading(), file.getAlbumReading(),
                    file.getAlbumArtistReading(), file.getArtistSortRaw(), file.getAlbumSortRaw(),
                    file.getAlbumArtistSortRaw(), file.getComposerSortRaw(), -1); // <<<< JP
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    private MediaFile getMusicFileInfo(String path) {
        return queryOne("select play_count, last_played, comment from music_file_info where path=?",
                musicFileInfoRowMapper, path);
    }

    public void deleteMediaFile(String path) {
        update("update media_file set present=false, children_last_updated=? where path=?", new Date(0L), path);
    }

    public List<Genre> getGenres(boolean sortByAlbum) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + GENRE_COLUMNS + " from genre order by " + orderBy + " desc", genreRowMapper);
    }

    public void updateGenres(List<Genre> genres) {
        update("delete from genre");
        for (Genre genre : genres) {
            update("insert into genre(" + GENRE_COLUMNS + ") values(?, ?, ?)", genre.getName(), genre.getSongCount(),
                    genre.getAlbumCount());
        }
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

    /**
     * Returns albums in a genre.
     *
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param genre
     *            The genre name.
     * @param musicFolders
     *            Only return albums in these folders.
     * 
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final String genre,
            final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaFile.MediaType.ALBUM.name(), "genre", genre, "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return namedQuery("select " + QUERY_COLUMNS + " from media_file where type = :type and folder in (:folders) "
                + "and present and genre = :genre limit :count offset :offset", rowMapper, args);
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

    @SuppressWarnings("PMD.NPathComplexity") // #862
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, final String username) {

        if (criteria.getMusicFolders().isEmpty()) {
            return Collections.emptyList();
        }

        RandomSongsQueryBuilder queryBuilder = new RandomSongsQueryBuilder(criteria);

        Map<String, Object> args = LegacyMap.of("folders", MusicFolder.toPathList(criteria.getMusicFolders()),
                "username", username, "fromYear", criteria.getFromYear(), "toYear", criteria.getToYear(), "genres",
                criteria.getGenres(), // TODO to be revert
                "minLastPlayed", criteria.getMinLastPlayedDate(), "maxLastPlayed", criteria.getMaxLastPlayedDate(),
                "minAlbumRating", criteria.getMinAlbumRating(), "maxAlbumRating", criteria.getMaxAlbumRating(),
                "minPlayCount", criteria.getMinPlayCount(), "maxPlayCount", criteria.getMaxPlayCount(), "starred",
                criteria.isShowStarredSongs(), "unstarred", criteria.isShowUnstarredSongs(), "format",
                criteria.getFormat());

        return namedQuery(queryBuilder.build(), rowMapper, args);
    }

    /**
     * Return a list of media file objects that don't belong to an existing music folder
     * 
     * @param count
     *            maximum number of media file objects to return
     * @param excludeFolders
     *            music folder paths excluded from the results
     * 
     * @return a list of media files, sorted by id
     */
    public List<MediaFile> getFilesInNonPresentMusicFolders(final int count, List<String> excludeFolders) {
        Map<String, Object> args = LegacyMap.of("excludeFolders", excludeFolders, "count", count);
        return namedQuery("SELECT " + prefix(QUERY_COLUMNS, "media_file") + " FROM media_file "
                + "LEFT OUTER JOIN music_folder ON music_folder.path = media_file.folder "
                + "WHERE music_folder.id IS NULL " + "AND media_file.folder NOT IN (:excludeFolders) "
                + "ORDER BY media_file.id LIMIT :count", rowMapper, args);
    }

    /**
     * Count the number of media files that don't belong to an existing music folder
     * 
     * @param excludeFolders
     *            music folder paths excluded from the results
     * 
     * @return a number of media file rows in the database
     */
    public int getFilesInNonPresentMusicFoldersCount(List<String> excludeFolders) {
        Map<String, Object> args = LegacyMap.of("excludeFolders", excludeFolders);
        return namedQueryForInt(
                "SELECT count(media_file.id) FROM media_file "
                        + "LEFT OUTER JOIN music_folder ON music_folder.path = media_file.folder "
                        + "WHERE music_folder.id IS NULL " + "AND media_file.folder NOT IN (:excludeFolders) ",
                0, args);
    }

    /**
     * Return a list of media file objects whose path don't math their music folder
     * 
     * @param count
     *            maximum number of media file objects to return
     * 
     * @return a list of media files, sorted by id
     */
    public List<MediaFile> getFilesWithMusicFolderMismatch(final int count) {
        return query("SELECT " + prefix(QUERY_COLUMNS, "media_file") + " FROM media_file "
                + "WHERE media_file.path != media_file.folder "
                + "AND media_file.path NOT LIKE concat(media_file.folder, concat(?, '%')) "
                + "ORDER BY media_file.id LIMIT ?", rowMapper, File.separator, count);
    }

    /**
     * Count the number of media files whose path don't math their music folder
     * 
     * @return a number of media file rows in the database
     */
    public int getFilesWithMusicFolderMismatchCount() {
        return queryForInt(
                "SELECT count(media_file.id) FROM media_file " + "WHERE media_file.path != media_file.folder "
                        + "AND media_file.path NOT LIKE concat(media_file.folder, '/%')",
                0);
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

    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        update("insert into starred_media_file(media_file_id, username, created) values (?,?,?)", id, username,
                new Date());
    }

    public void unstarMediaFile(int id, String username) {
        update("delete from starred_media_file where media_file_id=? and username=?", id, username);
    }

    public Date getMediaFileStarredDate(int id, String username) {
        return queryForDate("select created from starred_media_file where media_file_id=? and username=?", null, id,
                username);
    }

    public void markPresent(String path, Date lastScanned) {
        update("update media_file set present=?, last_scanned = ? where path=?", true, lastScanned, path);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select min(id) from media_file where last_scanned < ? and present", 0, lastScanned);
        int maxId = queryForInt("select max(id) from media_file where last_scanned < ? and present", 0, lastScanned);

        final int batchSize = 1000;
        Date childrenLastUpdated = new Date(0L); // Used to force a children rescan if file is later resurrected.
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

    @SuppressWarnings("PMD.NPathComplexity") // #863
    private static class MediaFileMapper implements RowMapper<MediaFile> {
        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(rs.getInt(1), rs.getString(2), rs.getString(3),
                    MediaFile.MediaType.valueOf(rs.getString(4)), rs.getString(5), rs.getString(6), rs.getString(7),
                    rs.getString(8), rs.getString(9), rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11), rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13), rs.getInt(14) == 0 ? null : rs.getInt(14), rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16), rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18), rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20), rs.getString(21), rs.getInt(22), rs.getTimestamp(23), rs.getString(24),
                    rs.getTimestamp(25), rs.getTimestamp(26), rs.getTimestamp(27), rs.getTimestamp(28),
                    rs.getBoolean(29), rs.getInt(30), rs.getString(31), rs.getString(32),
                    // JP >>>>
                    rs.getString(33), rs.getString(34), rs.getString(35), rs.getString(36), rs.getString(37),
                    rs.getString(38), rs.getString(39), rs.getString(40), rs.getString(41), rs.getString(42),
                    rs.getString(43), rs.getString(44), rs.getString(45), rs.getInt(46)); // <<<< JP
        }
    }

    private static class RandomSongsQueryBuilder {

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

        private Optional<String> getIfJoinStarred() {
            boolean joinStarred = criteria.isShowStarredSongs() ^ criteria.isShowUnstarredSongs();
            if (joinStarred) {
                return Optional
                        .of(" left outer join starred_media_file on media_file.id = starred_media_file.media_file_id"
                                + " and starred_media_file.username = :username");
            }
            return Optional.empty();
        }

        private Optional<String> getIfJoinAlbumRating() {
            boolean joinAlbumRating = criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null;
            if (joinAlbumRating) {
                return Optional.of(" left outer join media_file media_album on media_album.type = 'ALBUM'"
                        + " and media_album.album = media_file.album " + "and media_album.artist = media_file.artist "
                        + "left outer join user_rating on user_rating.path = media_album.path"
                        + " and user_rating.username = :username");
            }
            return Optional.empty();
        }

        private Optional<String> getFolderCondition() {
            if (!criteria.getMusicFolders().isEmpty()) {
                return Optional.of(" and media_file.folder in (:folders)");
            }
            return Optional.empty();
        }

        private Optional<String> getGenreCondition() {
            if (criteria.getGenres() != null) {
                return Optional.of(" and media_file.genre in (:genres)");
            }
            return Optional.empty();
        }

        private Optional<String> getFormatCondition() {
            if (criteria.getFormat() != null) {
                return Optional.of(" and media_file.format = :format");
            }
            return Optional.empty();
        }

        private Optional<String> getFromYearCondition() {
            if (criteria.getFromYear() != null) {
                return Optional.of(" and media_file.year >= :fromYear");
            }
            return Optional.empty();
        }

        private Optional<String> getToYearCondition() {
            if (criteria.getToYear() != null) {
                return Optional.of(" and media_file.year <= :toYear");
            }
            return Optional.empty();
        }

        private Optional<String> getMinLastPlayedDateCondition() {
            if (criteria.getMinLastPlayedDate() != null) {
                return Optional.of(" and media_file.last_played >= :minLastPlayed");
            }
            return Optional.empty();
        }

        private Optional<String> getMaxLastPlayedDateCondition() {
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

        private Optional<String> getMinAlbumRatingCondition() {
            if (criteria.getMinAlbumRating() != null) {
                return Optional.of(" and user_rating.rating >= :minAlbumRating");
            }
            return Optional.empty();
        }

        private Optional<String> getMaxAlbumRatingCondition() {
            if (criteria.getMaxAlbumRating() != null) {
                if (criteria.getMinAlbumRating() == null) {
                    return Optional.of(" and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)");
                } else {
                    return Optional.of(" and user_rating.rating <= :maxAlbumRating");
                }
            }
            return Optional.empty();
        }

        private Optional<String> getMinPlayCountCondition() {
            if (criteria.getMinPlayCount() != null) {
                return Optional.of(" and media_file.play_count >= :minPlayCount");
            }
            return Optional.empty();
        }

        private Optional<String> getMaxPlayCountCondition() {
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

        private Optional<String> getShowStarredSongsCondition() {
            if (criteria.isShowStarredSongs() && !criteria.isShowUnstarredSongs()) {
                return Optional.of(" and starred_media_file.id is not null");
            }
            return Optional.empty();
        }

        private Optional<String> getShowUnstarredSongsCondition() {
            if (criteria.isShowUnstarredSongs() && !criteria.isShowStarredSongs()) {
                return Optional.of(" and starred_media_file.id is null");
            }
            return Optional.empty();
        }
    }

    private static class MusicFileInfoMapper implements RowMapper<MediaFile> {
        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPlayCount(rs.getInt(1));
            file.setLastPlayed(rs.getTimestamp(2));
            file.setComment(rs.getString(3));
            return file;
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
