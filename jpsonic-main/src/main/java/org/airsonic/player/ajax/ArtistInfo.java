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
 * (C) 2014 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.ajax;

import java.util.List;

import org.airsonic.player.domain.ArtistBio;

/**
 * @author Sindre Mehus
 */
public class ArtistInfo {

    private final List<SimilarArtist> similarArtists;
    private final ArtistBio artistBio;
    private final List<TopSong> topSongs;

    public ArtistInfo(List<SimilarArtist> similarArtists, ArtistBio artistBio, List<TopSong> topSongs) {
        this.similarArtists = similarArtists;
        this.artistBio = artistBio;
        this.topSongs = topSongs;
    }

    public List<SimilarArtist> getSimilarArtists() {
        return similarArtists;
    }

    public ArtistBio getArtistBio() {
        return artistBio;
    }

    public List<TopSong> getTopSongs() {
        return topSongs;
    }
}
