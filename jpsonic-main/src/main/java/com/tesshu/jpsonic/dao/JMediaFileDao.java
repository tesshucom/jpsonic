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

import com.tesshu.jpsonic.domain.SortCandidate;
import org.airsonic.player.dao.AbstractDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;

import static org.airsonic.player.dao.MediaFileDao.getGenreColoms;
import static org.airsonic.player.dao.MediaFileDao.getQueryColoms;
import static org.springframework.util.ObjectUtils.isEmpty;

@Repository("jmediaFileDao")
@DependsOn({ "mediaFileDao" })
public class JMediaFileDao extends AbstractDao {

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

    private static class MediaFileInternalRowMapper implements RowMapper<MediaFile> {

        private RowMapper<MediaFile> mediaFileMapper;

        public MediaFileInternalRowMapper(RowMapper<MediaFile> m) {
            super();
            this.mediaFileMapper = m;
        }

        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile mediaFile = mediaFileMapper.mapRow(rs, rowNum);
            mediaFile.setRownum(rs.getInt("irownum"));
            return mediaFile;
        }

    }

    private static class SortCandidateMapper implements RowMapper<SortCandidate> {
        public SortCandidate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SortCandidate(rs.getString(1), rs.getString(2));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JMediaFileDao.class);

    private final MediaFileDao deligate;
    private final RowMapper<MediaFile> rowMapper;
    private final RowMapper<Genre> genreRowMapper;
    private final RowMapper<MediaFile> iRowMapper;
    private final RowMapper<MediaFile> artistSortCandidateMapper;
    private final SortCandidateMapper sortCandidateMapper;
    private final RowMapper<MediaFile> albumSortCandidateMapper;

    public JMediaFileDao(MediaFileDao deligate) {
        super();
        this.deligate = deligate;
        rowMapper = deligate.getMediaFileMapper();
        genreRowMapper = deligate.getGenreMapper();
        iRowMapper = new MediaFileInternalRowMapper(rowMapper);
        artistSortCandidateMapper = new ArtistSortCandidateMapper();
        albumSortCandidateMapper = new AlbumSortCandidateMapper();
        sortCandidateMapper = new SortCandidateMapper();
    }

    public void clearOrder() {
        update("update media_file set _order = -1");
    }

    public void createOrUpdateMediaFile(MediaFile file) {
        deligate.createOrUpdateMediaFile(file);
    }

    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final List<String> genres, final List<MusicFolder> musicFolders) {
        // @formatter:off
        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.ALBUM.name());
        args.put("genres", genres);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        args.put("count", count);
        args.put("offset", offset);
        return namedQuery(
                "select " + getQueryColoms() + " from media_file " +
                "where type = :type and folder in (:folders) and present and genre in (:genres) " +
                "order by _order limit :count offset :offset", rowMapper, args);
    } // @formatter:on

    @Deprecated
    public List<MediaFile> getAlbumSortCandidate() { // @formatter:off
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
    } // @formatter:on

    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist, final List<MusicFolder> musicFolders) {
        return deligate.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
    }

    public List<MediaFile> getArtistAll(final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("type", MediaFile.MediaType.DIRECTORY.name());
        args.put("folders", MusicFolder.toPathList(musicFolders));
        return namedQuery(// @formatter:off
                "select " + getQueryColoms() + " from media_file " +
                "where " +
                "type = :type and " +
                "folder in (:folders) and " +
                "present and " +
                "artist is not null", rowMapper, args); // @formatter:on
    }

    @Deprecated
    public List<MediaFile> getArtistSortCandidate() { // @formatter:off
        return query(
                "select m.id as id, m.artist as artist, artist_reading, dic.artist_sort as artist_sort from media_file m "
                        + "join (select distinct  artist, artist_sort from media_file where artist_sort is not null  and present order by artist) dic "
                        + "on dic.artist = m.artist "
                        + "where type = ? and present "
                        + "and artist_reading is not null "
                        + "order by artist, artist_sort",
                        artistSortCandidateMapper,
                        MediaFile.MediaType.DIRECTORY.name());
    } // @formatter:on

    /**
     * Returns the media file that are direct children of the given path.
     * 
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(final long offset, final long count, String path, boolean byYear) { // @formatter:off
        String order = byYear ? "year" : "_order";
        return query("select " + getQueryColoms() + " from media_file " +
                "where parent_path=? and present " +
                "order by " + order + " limit ? offset ?", rowMapper, path, count, offset);
    } // @formatter:on

    public int getChildSizeOf(String path) {
        return queryForInt("select count(id) from media_file where parent_path=? and present", 0, path);
    }

    public List<SortCandidate> getCopyableSorts() { // @formatter:off
        return query(
                "select known.name , known.sort from ( " +
                "    select distinct artist as name from media_file where present and type in ('DIRECTORY', 'ALBUM') and (artist is not null and artist_sort is null)  " +
                "    union select distinct album_artist as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (album_artist is not null and album_artist_sort is null)  " +
                "    union select distinct composer as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (composer is not null and composer_sort is null) ) unknown " +
                "join " +
                "    (select distinct name, sort from " +
                "        (select distinct album_artist as name, album_artist_sort as sort from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present " +
                "        union select distinct artist as name, artist_sort as sort from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present " +
                "        union select distinct composer as name, composer_sort as sort from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present) person_union " +
                "    ) known " +
                "on known.name = unknown.name ",
                sortCandidateMapper); // @formatter:on
    }

    public int getCountInPlaylist(int playlistId) { // @formatter:off
        return queryForInt("select count(*) from playlist_file, media_file " +
                "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present ", 0, playlistId);
    } // @formatter:on

    /*
     * Returns records where Sort does not match for the specified name and Sort.
     * Takes a single argument because UNNEST is not available.
     */
    public List<MediaFile> getDirtySorts(SortCandidate candidates) {
        Map<String, Object> args = new HashMap<>();
        args.put("name", candidates.getName());
        args.put("sort", candidates.getSort());
        // @formatter:off
        return namedQuery(
                "select " + getQueryColoms() + " from media_file " +
                "where " +
                "    (artist = :name and (artist_sort <> :sort or artist_sort is null )) or " +
                "    (album_artist = :name and (album_artist_sort <> :sort or album_artist_sort is null )) or " +
                "    (composer = :name and (composer_sort <> :sort or composer_sort is null )) ",
                rowMapper, args);
        // @formatter:on
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return deligate.getFilesInPlaylist(playlistId);

    }

    public List<MediaFile> getFilesInPlaylist(int playlistId, long offset, long count) { // @formatter:off
        return query("select " + prefix(getQueryColoms(), "media_file") + " from playlist_file, media_file " +
                     "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present " +
                     "order by playlist_file.id limit ? offset ?", rowMapper, playlistId, count, offset);
    } // @formatter:on

    public List<Genre> getGenres(boolean sortByAlbum, long offset, long count) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + getGenreColoms() + " from genre order by " + orderBy + " desc limit ? offset ?", genreRowMapper, count, offset);
    }

    public int getGenresCount() {
        return queryForInt("select count(*) from genre", 0);
    }

    public MediaFile getMediaFile(int id) {
        return deligate.getMediaFile(id);
    }

    public MediaFile getMediaFile(String path) {
        return deligate.getMediaFile(path);
    }

    public List<SortCandidate> getNoSorts() { // @formatter:off
        return query(
                "select name, null as sort from( " +
                        "   select distinct artist as name from media_file where present and type in ('DIRECTORY', 'ALBUM') and (artist is not null and artist_sort is null)  " +
                        "   union select distinct album_artist as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (album_artist is not null and album_artist_sort is null)  " +
                        "   union select distinct composer as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (composer is not null and composer_sort is null)  " +
                        "   ) no_sorts ",
                sortCandidateMapper); // @formatter:on
    }

    public List<MediaFile> getRandomSongsForAlbumArtist(// @formatter:off
            int limit, String albumArtist, List<MusicFolder> musicFolders,
            BiFunction< /** range */ Integer, /** limit */ Integer,
            List<Integer>> randomCallback) {

        String type = MediaFile.MediaType.MUSIC.name();

        /* Run the query twice. */

        /*
         * Get the number of records that match the conditions, to generate a set of
         * random numbers according to the number. Therefore, if the number of cases at
         * this time is too large, the subsequent performance is likely to be affected.
         * If the number isn't too large, it doesn't matter much.
         */
        int countAll = queryForInt("select count(*) from media_file where present and type = ? and album_artist = ?", 0, type, albumArtist);
        List<Integer> randomRownum = randomCallback.apply(countAll, limit);

        Map<String, Object> args = new HashMap<>();
        args.put("type", type);
        args.put("artist", albumArtist);
        args.put("randomRownum", randomRownum);
        args.put("limit", limit);

        /*
         * Perform a conditional search and add a row number.
         * Returns the result whose row number is included in the random number set.
         * 
         * There are some technical barriers to this query.
         * 
         *  (1) It must be a row number acquisition method that can be executed in all DBs.
         *  (2) It is simpler to join using UNNEST.
         *  However, hsqldb traditionally has a problem with UNNEST, and the operation specification differs depending on the version.
         *  In addition, compatibility of each DB may be affected.
         *  
         *  Therefore, we use a very primitive query that combines COUNT and IN here.
         *  
         *  IN allows you to get the smallest song subset corresponding to random numbers,
         *  but unlike JOIN&UNNEST, the order of random numbers is destroyed.
         */
        List<MediaFile> tmpResult = namedQuery(
                "select " + getQueryColoms() + ", foo.irownum from " +
                "    (select " +
                "        (select count(id) from media_file where id < boo.id and type = :type and album_artist = :artist) as irownum, * " +
                "    from (select * " +
                "        from media_file " +
                "        where type = :type " +
                "        and album_artist = :artist " +
                "        order by _order, album_artist, album) boo " +
                ") as foo " + 
                "where foo.irownum in ( :randomRownum ) limit :limit ", iRowMapper, args);

        /* Restore the order lost in IN. */
        Map<Integer, MediaFile> map = new HashMap<>();
        tmpResult.forEach(m -> map.put(m.getRownum(), m));
        List<MediaFile> result = new ArrayList<>();
        randomRownum.forEach(i -> {
            MediaFile m = map.get(i);
            if (!isEmpty(m)) {
                result.add(m);
            }
        });

        return Collections.unmodifiableList(result);
    } // @formatter:on

    public List<MediaFile> getSongsByGenre(final List<String> genres, final int offset, final int count, final List<MusicFolder> musicFolders) {
        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("types", Arrays.asList(MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.PODCAST.name(), MediaFile.MediaType.AUDIOBOOK.name()));
        args.put("genres", genres);
        args.put("count", count);
        args.put("offset", offset);
        args.put("folders", MusicFolder.toPathList(musicFolders));
        // @formatter:off
        return namedQuery("select " + prefix(getQueryColoms(), "s") + " from media_file s " +
                          "join media_file al on s.parent_path = al.path " + 
                          "join media_file ar on al.parent_path = ar.path " +
                          "where s.type in (:types) and s.genre in (:genres) " +
                          "and s.present and s.folder in (:folders) " +
                          "order by ar._order, al._order, s.track_number " +
                          "limit :count offset :offset ", rowMapper, args);
    } // @formatter:on

    public int getSongsCountForAlbum(String artist, String album) { // @formatter:off
        return queryForInt(
                "select count(id) from media_file " +
                "where album_artist=? and album=? and present and type in (?,?,?)", 0,
                     artist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(), MediaFile.MediaType.PODCAST.name());
    } // @formatter:on

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) { // @formatter:off
        return query(
                "select " + getQueryColoms() + " from media_file " +
                "where parent_path=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, album.getPath(), MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name(), count, offset); // @formatter:on
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) { // @formatter:off
        return query(
                "select " + getQueryColoms() + " from media_file " +
                "where album_artist=? and album=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, albumArtist, album, MediaFile.MediaType.MUSIC.name(), MediaFile.MediaType.AUDIOBOOK.name(),
                MediaFile.MediaType.PODCAST.name(), count, offset);
    } // @formatter:on

    @Deprecated
    public List<MediaFile> getSortedAlbums() { // @formatter:off
        return query("select " + getQueryColoms() +
                " from media_file" +
                " where album_reading is not null" +
                " or album_sort is not null" +
                " and type = ? and present",
                rowMapper, MediaFile.MediaType.ALBUM.name());
    } // @formatter:on

    /*
     * Looks for duplicate sort tags, creates and returns a list in the order in
     * which corrections are desired. The validate target is
     * Persons(albumArtist/Artist/Composer) and sort-tags for them.
     *
     * If there are multiple sort tags against one artist, an adverse effect occurs.
     * Index bloat of Lucene, search dropouts, etc. are direct consequences. Also,
     * when creating sort keys using sort tags, there is a problem that if the tags
     * are not uniform, consistency will be lost. Therefore, multiple sort tags are
     * merged internally. These are determined at the time of scanning.
     *
     * If there are multiple sort tags, they are processed according to the rules
     * that determine which is correct. Jpsonic uses the following concept to
     * determine the correct tag.
     *
     * - The latest(changed) file should be more reliable. - Most reliable in the
     * following order: album_artist_sort/artist_sort/composer_sort
     *
     * This is because the priorities are easy to recursively reflect the user's
     * intentions.
     */
    public List<SortCandidate> guessSorts() { // @formatter:off
        List<SortCandidate> candidates = query(
            "select name, sort, source, duplicate_persons_with_priority.changed from " +
            "   (select distinct name, sort, source, changed " +
            "   from " +
            "       (select distinct album_artist as name, album_artist_sort as sort, 1 as source, type, changed from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present " +
            "       union select distinct artist as name, artist_sort as sort, 2 as source, type, changed from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present " +
            "       union select distinct composer as name, composer_sort as sort, 3 as source, type, changed from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present " +
            "       ) as person_all_with_priority " +
            "       where name in " +
            "           (select name from " +
            "               (select name, count(sort) from " +
            "                   (select distinct name, sort from " +
            "                       (select distinct album_artist as name, album_artist_sort as sort from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present " +
            "                       union select distinct artist as name, artist_sort as sort from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present " +
            "                       union select distinct composer as name, composer_sort as sort from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present " +
            "                       ) person_union " +
            "                   ) duplicate " +
            "               group by name " +
            "               having 1 < count(sort) " +
            "           ) duplicate_names) " +
            "   ) duplicate_persons_with_priority " +
            "   join media_file m on type = 'MUSIC' and name in(album_artist, artist ,composer) " +
            "   group by name, sort, source, duplicate_persons_with_priority.changed " +
            "   having max(m.changed) = duplicate_persons_with_priority.changed " +
            "order by name, changed desc, source ",
            sortCandidateMapper); // @formatter:on

        List<SortCandidate> result = new ArrayList<>();
        candidates.forEach((candidate) -> {
            if (!result.stream().anyMatch(r -> r.getName().equals(candidate.getName()))) {
                result.add(candidate);
            }
        });

        return result;
    }

    public void markNonPresent(Date lastScanned) {
        deligate.markNonPresent(lastScanned);
    }

    public void markPresent(String path, Date lastScanned) {
        deligate.markPresent(path, lastScanned);
    }

    /**
     * Update albumArtistSorts all.
     * 
     * @param artist          The artist to update.
     * @param albumArtistSort Update value.
     */
    @Deprecated
    @Transactional
    public int updateAlbumArtistSort(String artist, String albumArtistSort) {
        LOG.trace("Updating media file at {}", artist);
        String sql = "update media_file set album_artist_sort = ? where artist = ? and artist_reading <> ? and type in (?, ?)";
        LOG.trace("Updating media file {}", artist);
        return update(sql, albumArtistSort, artist, albumArtistSort, MediaFile.MediaType.DIRECTORY.name(), MediaFile.MediaType.ALBUM.name());
    }

    /**
     * Update albumSorts all.
     * 
     * @param album     The artist to update.
     * @param albumSort Update value.
     */
    @Deprecated
    @Transactional
    public int updateAlbumSort(String album, String albumSort) {
        LOG.trace("Updating media file at {}", album);
        String sql = "update media_file set album_sort = ? where album = ? and type = ?";
        LOG.trace("Updating media file {}", album);
        return update(sql, albumSort, album, MediaFile.MediaType.ALBUM.name());
    }

    public void updateArtistSort(SortCandidate candidate) { // @formatter:off
        update("update media_file set artist_reading = ?, artist_sort = ? " +
                "where present and artist = ? and (artist_sort is null or artist_sort <> ?)",
                candidate.getReading(), candidate.getSort(),
                candidate.getName(), candidate.getSort());
        update("update media_file set album_artist_reading = ?, album_artist_sort = ? " +
                "where present and type not in ('DIERECTORY', 'ALBUM') and album_artist = ? and (album_artist_sort is null or album_artist_sort <> ?)",
                candidate.getReading(),
                candidate.getSort(),
                candidate.getName(),
                candidate.getSort());
        update("update media_file set composer_sort = ? " +
                "where present and type not in ('DIERECTORY', 'ALBUM') and composer = ? and (composer_sort is null or composer_sort <> ?)",
                candidate.getSort(),
                candidate.getName(),
                candidate.getSort());
    } // @formatter:on

    public void updateGenres(List<Genre> genres) {
        deligate.updateGenres(genres);
    }
}
