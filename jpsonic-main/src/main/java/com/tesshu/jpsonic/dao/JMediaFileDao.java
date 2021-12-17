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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.dao;

import static com.tesshu.jpsonic.dao.MediaFileDao.getGenreColoms;
import static com.tesshu.jpsonic.dao.MediaFileDao.getQueryColoms;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.util.LegacyMap;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository("jmediaFileDao")
@SuppressWarnings("PMD.AvoidDuplicateLiterals") // Only DAO is allowed to exclude this rule #827
public class JMediaFileDao extends AbstractDao {

    private final MediaFileDao deligate;
    private final RowMapper<MediaFile> rowMapper;
    private final RowMapper<Genre> genreRowMapper;
    private final RowMapper<MediaFile> iRowMapper;
    private final RowMapper<SortCandidate> sortCandidateMapper;

    public JMediaFileDao(DaoHelper daoHelper, MediaFileDao deligate) {
        super(daoHelper);
        this.deligate = deligate;
        rowMapper = deligate.getMediaFileMapper();
        genreRowMapper = deligate.getGenreMapper();
        iRowMapper = new MediaFileInternalRowMapper(rowMapper);
        sortCandidateMapper = (rs, rowNum) -> new SortCandidate(rs.getString(1), rs.getString(2));
    }

    public void clearOrder() {
        update("update media_file set media_file_order = -1");
    }

    public void clearArtistReadingOfDirectory() {
        update("update media_file set artist_reading = null, artist_sort = null where present and type=?",
                MediaType.DIRECTORY.name());
    }

    public void createOrUpdateMediaFile(MediaFile file) {
        deligate.createOrUpdateMediaFile(file);
    }

    public List<MediaFile> getAlbumsByGenre(final int offset, final int count, final List<String> genres,
            final List<MusicFolder> musicFolders) {

        if (musicFolders.isEmpty() || genres.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("type", MediaType.ALBUM.name(), "genres", genres, "folders",
                MusicFolder.toPathList(musicFolders), "count", count, "offset", offset);
        return namedQuery("select " + getQueryColoms() + " from media_file "
                + "where type = :type and folder in (:folders) and present and genre in (:genres) "
                + "order by media_file_order limit :count offset :offset", rowMapper, args);
    }

    public List<MediaFile> getAlphabeticalAlbums(final int offset, final int count, boolean byArtist,
            final List<MusicFolder> musicFolders) {
        return deligate.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
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

    public int getChildSizeOf(String path) {
        return queryForInt("select count(id) from media_file where parent_path=? and present", 0, path);
    }

    public List<SortCandidate> getCopyableSortForAlbums() {
        return query("select known.name , known.sort from ( "
                + "    select distinct album as name from media_file where present and type = 'ALBUM' and (album is not null and album_sort is null)) unknown "
                + "   join (select distinct album as name, album_sort as sort from media_file where type = 'ALBUM' and album is not null and album_sort is not null and present) known "
                + "   on known.name = unknown.name ", sortCandidateMapper);
    }

    public List<SortCandidate> getCopyableSortForPersons() {
        return query("select known.name , known.sort from ( "
                + "    select distinct artist as name from media_file where present and type in ('DIRECTORY', 'ALBUM') and (artist is not null and artist_sort is null)  "
                + "    union select distinct album_artist as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (album_artist is not null and album_artist_sort is null)  "
                + "    union select distinct composer as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (composer is not null and composer_sort is null) ) unknown "
                + "join " + "    (select distinct name, sort from "
                + "        (select distinct album_artist as name, album_artist_sort as sort from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present "
                + "        union select distinct artist as name, artist_sort as sort from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present "
                + "        union select distinct composer as name, composer_sort as sort from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present) person_union "
                + "    ) known " + "on known.name = unknown.name ", sortCandidateMapper);
    }

    public int getCountInPlaylist(int playlistId) {
        return queryForInt("select count(*) from playlist_file, media_file "
                + "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present ", 0,
                playlistId);
    }

    /*
     * Returns records where Sort does not match for the specified name and Sort. Takes a single argument because UNNEST
     * is not available.
     */
    public List<MediaFile> getDirtySorts(SortCandidate candidates) {
        Map<String, Object> args = LegacyMap.of("name", candidates.getName(), "sort", candidates.getSort());
        return namedQuery("select " + getQueryColoms() + " from media_file " + "where "
                + "    (artist = :name and (artist_sort <> :sort or artist_sort is null )) or "
                + "    (album_artist = :name and (album_artist_sort <> :sort or album_artist_sort is null )) or "
                + "    (composer = :name and (composer_sort <> :sort or composer_sort is null )) ", rowMapper, args);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return deligate.getFilesInPlaylist(playlistId);

    }

    public List<MediaFile> getFilesInPlaylist(int playlistId, long offset, long count) {
        return query("select " + prefix(getQueryColoms(), "media_file") + " from playlist_file, media_file "
                + "where media_file.id = playlist_file.media_file_id and playlist_file.playlist_id = ? and present "
                + "order by playlist_file.id limit ? offset ?", rowMapper, playlistId, count, offset);
    }

    public List<Genre> getGenres(boolean sortByAlbum, long offset, long count) {
        String orderBy = sortByAlbum ? "album_count" : "song_count";
        return query("select " + getGenreColoms() + " from genre " + "order by " + orderBy + " desc limit ? offset ?",
                genreRowMapper, count, offset);
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
            if (!isEmpty(m)) {
                result.add(m);
            }
        });

        return Collections.unmodifiableList(result);
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

    public int getSongsCountForAlbum(String artist, String album) {
        return queryForInt(
                "select count(id) from media_file "
                        + "where album_artist=? and album=? and present and type in (?,?,?)",
                0, artist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(), MediaType.PODCAST.name());
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, MediaFile album) {
        return query(
                "select " + getQueryColoms() + " from media_file "
                        + "where parent_path=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, album.getPath(), MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<MediaFile> getSongsForAlbum(final long offset, final long count, String albumArtist, String album) {
        return query("select " + getQueryColoms() + " from media_file "
                + "where album_artist=? and album=? and present and type in (?,?,?) order by track_number limit ? offset ?",
                rowMapper, albumArtist, album, MediaType.MUSIC.name(), MediaType.AUDIOBOOK.name(),
                MediaType.PODCAST.name(), count, offset);
    }

    public List<SortCandidate> getSortForPersonWithoutSorts() {
        return query("select name, null as sort from( "
                + "   select distinct artist as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (artist is not null and artist_sort is null)  "
                + "   union select distinct album_artist as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (album_artist is not null and album_artist_sort is null)  "
                + "   union select distinct composer as name from media_file where present and type not in ('DIRECTORY', 'ALBUM') and (composer is not null and composer_sort is null)  "
                + "   ) no_sorts ", sortCandidateMapper);
    }

    public List<Integer> getSortOfAlbumToBeFixed(List<SortCandidate> candidates) {
        Map<String, Object> args = LegacyMap.of("names",
                candidates.stream().map(SortCandidate::getName).collect(toList()), "sotes",
                candidates.stream().map(SortCandidate::getSort).collect(toList()));
        return namedQuery("select distinct id from media_file "
                + "where present and album in (:names) and (album_sort is null or album_sort not in(:sotes))  "
                + "order by id ", (rs, rowNum) -> rs.getInt(1), args);
    }

    public List<Integer> getSortOfArtistToBeFixed(List<SortCandidate> candidates) {
        if (isEmpty(candidates) || 0 == candidates.size()) {
            return Collections.emptyList();
        }
        Map<String, Object> args = LegacyMap.of("names",
                candidates.stream().map(SortCandidate::getName).collect(toList()), "sotes",
                candidates.stream().map(SortCandidate::getSort).collect(toList()));
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

    public List<SortCandidate> getSortForAlbumWithoutSorts() {
        return query(
                "select distinct album as name, null as sort from media_file where present and type  = 'ALBUM' and (album is not null and album_sort is null) ",
                sortCandidateMapper);
    }

    public List<SortCandidate> guessAlbumSorts() {
        List<SortCandidate> candidates = query("select name, sort, duplicate_persons_with_changed.changed from ( "
                + "       select distinct album as name, album_sort as sort, changed " + "       from media_file m1 "
                + "       where album in " + "           (select name from "
                + "               (select name, count(sort) from "
                + "                   (select distinct album as name, album_sort as sort from media_file where type = 'ALBUM' and album is not null and album_sort is not null and present) named_album "
                + "               group by name having 1 < count(sort) " + "               ) duplicate_names "
                + "           ) " + "   ) " + "   duplicate_persons_with_changed "
                + "   join media_file m on type = 'ALBUM' and name in(album)  "
                + "   group by name, sort, duplicate_persons_with_changed.changed "
                + "   having max(m.changed) = duplicate_persons_with_changed.changed ", sortCandidateMapper);

        List<SortCandidate> result = new ArrayList<>();
        candidates.forEach((candidate) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(candidate.getName()))) {
                result.add(candidate);
            }
        });

        return result;
    }

    /*
     * Looks for duplicate sort tags, creates and returns a list in the order in which corrections are desired. The
     * validate target is Persons(albumArtist/Artist/Composer) and sort-tags for them.
     *
     * If there are multiple sort tags against one artist, an adverse effect occurs. Index bloat of Lucene, search
     * dropouts, etc. are direct consequences. Also, when creating sort keys using sort tags, there is a problem that if
     * the tags are not uniform, consistency will be lost. Therefore, multiple sort tags are merged internally. These
     * are determined at the time of scanning.
     *
     * If there are multiple sort tags, they are processed according to the rules that determine which is correct.
     * Jpsonic uses the following concept to determine the correct tag.
     *
     * - The latest(changed) file should be more reliable. - Most reliable in the following order:
     * album_artist_sort/artist_sort/composer_sort
     *
     * This is because the priorities are easy to recursively reflect the user's intentions.
     */
    public List<SortCandidate> guessPersonsSorts() {
        List<SortCandidate> candidates = query(
                "select name, sort, source, duplicate_persons_with_priority.changed from "
                        + "   (select distinct name, sort, source, changed " + "   from "
                        + "       (select distinct album_artist as name, album_artist_sort as sort, 1 as source, type, changed from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present "
                        + "       union select distinct artist as name, artist_sort as sort, 2 as source, type, changed from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present "
                        + "       union select distinct composer as name, composer_sort as sort, 3 as source, type, changed from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present "
                        + "       ) as person_all_with_priority " + "       where name in "
                        + "           (select name from " + "               (select name, count(sort) from "
                        + "                   (select distinct name, sort from "
                        + "                       (select distinct album_artist as name, album_artist_sort as sort from media_file where type = 'MUSIC' and album_artist is not null and album_artist_sort is not null and present "
                        + "                       union select distinct artist as name, artist_sort as sort from media_file where type = 'MUSIC' and artist is not null and artist_sort is not null and present "
                        + "                       union select distinct composer as name, composer_sort as sort from media_file where type = 'MUSIC' and composer is not null and composer_sort is not null and present "
                        + "                       ) person_union " + "                   ) duplicate "
                        + "               group by name " + "               having 1 < count(sort) "
                        + "           ) duplicate_names) " + "   ) duplicate_persons_with_priority "
                        + "   join media_file m on type = 'MUSIC' and name in(album_artist, artist ,composer) "
                        + "   group by name, sort, source, duplicate_persons_with_priority.changed "
                        + "   having max(m.changed) = duplicate_persons_with_priority.changed "
                        + "order by name, changed desc, source ",
                sortCandidateMapper);

        List<SortCandidate> result = new ArrayList<>();
        candidates.forEach((candidate) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(candidate.getName()))) {
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

    public void updateGenres(List<Genre> genres) {
        deligate.updateGenres(genres);
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
}
