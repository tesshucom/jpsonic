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
 * (C) 2023 tesshucom
 */

package com.tesshu.jpsonic.dao.dialect;

import static com.tesshu.jpsonic.dao.base.DaoUtils.prefix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.tesshu.jpsonic.dao.base.DaoUtils;
import com.tesshu.jpsonic.dao.base.TemplateWrapper;
import com.tesshu.jpsonic.domain.DuplicateSort;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SortCandidate;
import com.tesshu.jpsonic.spring.DatabaseConfiguration.ProfileNameConstants;
import com.tesshu.jpsonic.util.LegacyMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Component
@Profile({ ProfileNameConstants.URL, ProfileNameConstants.JNDI })
public class AnsiMediaFileDao implements DialectMediaFileDao {

    private static final String QUERY_COLUMNS = DaoUtils.getQueryColumns(MediaFile.class);

    // Expected maximum number of album child elements (can be expanded)
    private static final int ALBUM_CHILD_MAX = 10_000;

    private final TemplateWrapper template;

    private final RowMapper<MediaFile> rowMapper = DaoUtils.createRowMapper(MediaFile.class);
    private final RowMapper<MediaFile> iRowMapper;
    private final RowMapper<MediaFile> artistId3Mapper = (resultSet, rowNum) -> new MediaFile(-1, null,
            resultSet.getString(1), null, null, null, null, null, resultSet.getString(2), null, null, null, null, null,
            false, null, null, null, null, resultSet.getString(5), null, -1, null, null, null, null, null, null, false,
            -1, null, null, null, null, null, null, resultSet.getString(4), null, null, null, resultSet.getString(3),
            null, null, null, null, -1, "");
    private final RowMapper<DuplicateSort> sortCandidateMapper = (rs, rowNum) -> new SortCandidate(rs.getInt(1),
            rs.getString(2), rs.getString(3), -1);
    private final RowMapper<SortCandidate> sortCandidateWithIdMapper = (rs, rowNum) -> new SortCandidate(rs.getInt(1),
            rs.getString(2), rs.getString(3), rs.getInt(4));

    public AnsiMediaFileDao(TemplateWrapper templateWrapper) {
        template = templateWrapper;
        iRowMapper = (resultSet, rowNum) -> {
            MediaFile mediaFile = rowMapper.mapRow(resultSet, rowNum);
            if (mediaFile != null) {
                mediaFile.setRownum(resultSet.getInt("irownum"));
            }
            return mediaFile;
        };
    }

    @Override
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

    @Override
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

    private List<String> getValidTypes4ID3(boolean withPodcast) {
        List<String> types = new ArrayList<>();
        types.add(MediaFile.MediaType.MUSIC.name());
        types.add(MediaFile.MediaType.AUDIOBOOK.name());
        if (withPodcast) {
            types.add(MediaFile.MediaType.PODCAST.name());
        }
        return types;
    }

    @Override
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

    @Override
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
                        group by mf.album, mf.album_artist) fetched
                    on fetched.album = mf_album and fetched.album_artist = mf_album_artist
                            and fetched.file_order = unregistered.al_order * :childMax
                            + unregistered.mf_order + unregistered.folder_order * :childMax * 10
                limit :count
                """;
        return template.namedQuery(query, rowMapper, args);
    }

    @Override
    public List<SortCandidate> getCopyableSortAlbums(List<MusicFolder> folders) {
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

    @Override
    public List<SortCandidate> getCopyableSortPersons(List<MusicFolder> folders) {
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

    @Override
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

    @Override
    public List<SortCandidate> getNoSortPersons(List<MusicFolder> folders) {
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

    @Override
    public List<SortCandidate> getSortCandidatePersons(@NonNull List<DuplicateSort> duplicates) {
        List<SortCandidate> result = new ArrayList<>();
        if (duplicates.isEmpty()) {
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
        duplicates.forEach(cand -> {
            args.put("name", cand.getName());
            args.put("sote", cand.getSort());
            result.addAll(template.namedQuery(query, sortCandidateWithIdMapper, args));
        });
        return result;
    }

    @Override
    public List<SortCandidate> getNoSortAlbums(List<MusicFolder> folders) {
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

    @Override
    public List<SortCandidate> getDuplicateSortAlbums(List<MusicFolder> folders) {
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

    @Override
    public List<DuplicateSort> getDuplicateSortPersons(List<MusicFolder> folders) {
        List<DuplicateSort> result = new ArrayList<>();
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
        List<DuplicateSort> dups = template.namedQuery(query, sortCandidateMapper, args);
        dups.forEach((dup) -> {
            if (result.stream().noneMatch(r -> r.getName().equals(dup.getName()))) {
                result.add(dup);
            }
        });
        return result;
    }
}
