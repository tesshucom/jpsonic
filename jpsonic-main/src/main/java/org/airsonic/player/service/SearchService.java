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

package org.airsonic.player.service;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.Genre;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchResult;
import org.airsonic.player.service.search.SearchCriteria;
import org.airsonic.player.service.search.UPnPSearchCriteria;

import java.util.List;

/**
 * Performs Lucene-based searching.
 *
 * @author Sindre Mehus
 * 
 * @version $Id$
 * 
 * @see MediaScannerService
 */
public interface SearchService {

    /**
     * Perform a multi-field search corresponding to SearchCriteria.
     * 
     * It is the most popular search inherited from legacy servers and has been used from the Web and REST since ancient
     * times.
     * 
     * @since 107.3.0
     * 
     * @return search result
     */
    SearchResult search(SearchCriteria criteria);

    /**
     * Perform a search that comply with the UPnP Service Template with UPnPCriteria. Criteria is built using a
     * dedicated Director class (UPnPCriteriaDirector).
     * 
     * @param <T>
     *            see UPnPCriteria#getAssignableClass
     * 
     * @since 106.1.0
     * 
     * @return search result
     */
    <T> ParamSearchResult<T> search(UPnPSearchCriteria criteria);

    /**
     * Returns a number of random songs.
     *
     * @param criteria
     *            Search criteria.
     * 
     * @return List of random songs.
     */
    List<MediaFile> getRandomSongs(RandomSearchCriteria criteria);

    /**
     * Returns random songs. The song returned by this list is limited to MesiaType=SONG. In other words, PODCAST,
     * AUDIOBOOK and VIDEO are not included.
     * 
     * This method uses a very short-lived cache. This cache is not for long-running transactions like paging, but for
     * short-term repetitive calls.
     * 
     * @since 106.1.0
     * 
     * @param count
     *            Number of albums to return.
     * @param offset
     *            offset
     * @param casheMax
     *            Data duplication due to paging is avoided when the cache is an iterative call within the valid period.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return List of random albums.
     */
    List<MediaFile> getRandomSongs(int count, int offset, int casheMax, List<MusicFolder> musicFolders);

    /**
     * Returns random songs. The song returned by this list is limited to MesiaType=SONG. In other words, PODCAST,
     * AUDIOBOOK and VIDEO are not included.
     * 
     * This method uses a very short-lived cache. This cache is not for long-running transactions like paging, but for
     * short-term repetitive calls.
     * 
     * @since 107.0.0
     * 
     * @param count
     *            Number of albums to return.
     * @param offset
     *            offset
     * @param casheMax
     *            Data duplication due to paging is avoided when the cache is an iterative call within the valid period.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return List of random albums.
     */
    List<MediaFile> getRandomSongsByArtist(Artist artist, int count, int offset, int casheMax,
            List<MusicFolder> musicFolders);

    /**
     * Returns a number of random albums.
     *
     * @param count
     *            Number of albums to return.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return List of random albums.
     */
    List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders);

    /**
     * Returns random albums, using ID3 tag.
     *
     * @param count
     *            Number of albums to return.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return List of random albums.
     */
    List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders);

    /**
     * Returns random albums, using ID3 tag.
     * 
     * Unlike getRandom Album Id3, this method uses a very short-lived. This cache is not for long-running transactions
     * like paging, but for short-term repetitive calls.
     * 
     * @since 106.1.0
     * 
     * @param count
     *            Number of albums to return.
     * @param offset
     *            offset
     * @param casheMax
     *            Data duplication due to paging is avoided when the cache is an iterative call within the valid period.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return List of random albums.
     */
    List<Album> getRandomAlbumsId3(int count, int offset, int casheMax, List<MusicFolder> musicFolders);

    /**
     * Returns all genres in the music collection.
     *
     * @since 101.2.0
     * 
     * @param sortByAlbum
     *            Whether to sort by album count, rather than song count.
     * 
     * @return Sorted list of genres.
     */
    List<Genre> getGenres(boolean sortByAlbum);

    /**
     * Returns all genres in the music collection.
     *
     * @since 105.3.0
     * 
     * @param sortByAlbum
     *            Whether to sort by album count, rather than song count.
     * @param offset
     *            offset
     * @param maxResults
     *            maxResults
     * 
     * @return Sorted list of genres.
     */
    List<Genre> getGenres(boolean sortByAlbum, long offset, long maxResults);

    /**
     * Returns count of Genres.
     * 
     * @since 105.3.0
     * 
     * @param sortByAlbum
     *            Whether to sort by album count, rather than song count.
     * 
     * @return Count of Genres
     */
    int getGenresCount(boolean sortByAlbum);

    /**
     * Returns albums in a genre.
     *
     * @since 101.2.0
     * 
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param genres
     *            A genre name or multiple genres represented by delimiter strings defined in the specification.
     * @param musicFolders
     *            Only return albums in these folders.
     * 
     * @return Albums in the genre.
     */
    List<MediaFile> getAlbumsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders);

    /**
     * Returns albums in a genre.
     *
     * @since 101.2.0
     * 
     * @param offset
     *            Number of albums to skip.
     * @param count
     *            Maximum number of albums to return.
     * @param genres
     *            A genre name or multiple genres represented by delimiter strings defined in the specification.
     * @param musicFolders
     *            Only return albums from these folders.
     * 
     * @return Albums in the genre.
     */
    List<Album> getAlbumId3sByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders);

    /**
     * Returns songs in a genre.
     *
     * @since 101.2.0
     * 
     * @param offset
     *            Number of songs to skip.
     * @param count
     *            Maximum number of songs to return.
     * @param genres
     *            A genre name or multiple genres represented by delimiter strings defined in the specification.
     * @param musicFolders
     *            Only return songs from these folders.
     * 
     * @return songs in the genre.
     */
    List<MediaFile> getSongsByGenres(String genres, int offset, int count, List<MusicFolder> musicFolders);

}
