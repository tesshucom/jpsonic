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

package com.tesshu.jpsonic.infrastructure.search;

import java.util.List;

import com.tesshu.jpsonic.domain.model.SearchResult;
import com.tesshu.jpsonic.infrastructure.search.criteria.GenreMasterCriteria;
import com.tesshu.jpsonic.infrastructure.search.criteria.UPnPSearchCriteria;
import com.tesshu.jpsonic.persistence.api.entity.Album;
import com.tesshu.jpsonic.persistence.api.entity.Genre;
import com.tesshu.jpsonic.persistence.api.entity.MediaFile;
import com.tesshu.jpsonic.persistence.api.entity.MusicFolder;
import com.tesshu.jpsonic.persistence.param.ShuffleSelectionParam;

public interface MediaSearchProvider {

    <T> SearchResult<T> search(UPnPSearchCriteria criteria);

    List<MediaFile> getRandomSongs(ShuffleSelectionParam criteria);

    List<com.tesshu.jpsonic.domain.model.MediaFile> getRandomSongs(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> musicFolders, long offset, long count,
            int cacheMax, String... genres);

    List<com.tesshu.jpsonic.domain.model.MediaFile> getRandomSongsByArtist(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> musicFolders,
            com.tesshu.jpsonic.domain.model.Artist artist, long offset, long count, int cacheMax);

    List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders);

    List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders);

    List<Album> getRandomAlbumsId3(long count, long offset, int cacheMax,
            List<MusicFolder> musicFolders);

    List<com.tesshu.jpsonic.domain.model.Album> getRandomAlbumsId3(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> musicFolders, long offset, long count,
            int cacheMax);

    List<Genre> getGenres(boolean sortByAlbum);

    List<com.tesshu.jpsonic.domain.model.Genre> getGenres(GenreMasterCriteria criteria, long offset,
            long maxResults);

    List<com.tesshu.jpsonic.domain.model.Genre> findLegacyGenres(boolean sortByAlbum, long offset,
            long count);

    int getGenresCount(boolean sortByAlbum);

    int getGenresCount(GenreMasterCriteria criteria);

    List<MediaFile> getAlbumsByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders);

    List<com.tesshu.jpsonic.domain.model.MediaFile> findAlbumsByGenres(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> musicFolders, String genres,
            long offset, long count);

    List<Album> getAlbumId3sByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders);

    List<com.tesshu.jpsonic.domain.model.Album> findAlbumId3sByGenres(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> musicFolders, String genres,
            long offset, long count);

    List<MediaFile> getSongsByGenres(String genres, long offset, long count,
            List<MusicFolder> musicFolders, MediaFile.MediaType... types);

    List<com.tesshu.jpsonic.domain.model.MediaFile> getSongsByGenres(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> folders, List<String> genres,
            long offset, long count, com.tesshu.jpsonic.domain.model.MediaFile.Type... types);

    int countChldren(List<com.tesshu.jpsonic.domain.model.MusicFolder> folders, String genre,
            com.tesshu.jpsonic.domain.model.Album album,
            com.tesshu.jpsonic.domain.model.MediaFile.Type... types);

    List<com.tesshu.jpsonic.domain.model.MediaFile> findChildren(
            List<com.tesshu.jpsonic.domain.model.MusicFolder> folders, String genre,
            com.tesshu.jpsonic.domain.model.Album album, long offset, long count,
            com.tesshu.jpsonic.domain.model.MediaFile.Type... types);
}
