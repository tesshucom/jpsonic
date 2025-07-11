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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.util.connector.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Response", propOrder = { "musicFolders", "indexes", "directory", "genres",
        "artists", "artist", "album", "song", "videos", "videoInfo", "nowPlaying", "searchResult",
        "searchResult2", "searchResult3", "playlists", "playlist", "jukeboxStatus",
        "jukeboxPlaylist", "license", "users", "user", "chatMessages", "albumList", "albumList2",
        "randomSongs", "songsByGenre", "lyrics", "podcasts", "newestPodcasts",
        "internetRadioStations", "bookmarks", "playQueue", "shares", "starred", "starred2",
        "albumInfo", "artistInfo", "artistInfo2", "similarSongs", "similarSongs2", "topSongs",
        "scanStatus", "error" })
public class Response {

    protected MusicFolders musicFolders;
    protected Indexes indexes;
    protected Directory directory;
    protected Genres genres;
    protected ArtistsID3 artists;
    protected ArtistWithAlbumsID3 artist;
    protected AlbumWithSongsID3 album;
    protected Child song;
    protected Videos videos;
    protected VideoInfo videoInfo;
    protected NowPlaying nowPlaying;
    protected SearchResult searchResult;
    protected SearchResult2 searchResult2;
    protected SearchResult3 searchResult3;
    protected Playlists playlists;
    protected PlaylistWithSongs playlist;
    protected JukeboxStatus jukeboxStatus;
    protected JukeboxPlaylist jukeboxPlaylist;
    protected License license;
    protected Users users;
    protected User user;
    protected ChatMessages chatMessages;
    protected AlbumList albumList;
    protected AlbumList2 albumList2;
    protected Songs randomSongs;
    protected Songs songsByGenre;
    protected Lyrics lyrics;
    protected Podcasts podcasts;
    protected NewestPodcasts newestPodcasts;
    protected InternetRadioStations internetRadioStations;
    protected Bookmarks bookmarks;
    protected PlayQueue playQueue;
    protected Shares shares;
    protected Starred starred;
    protected Starred2 starred2;
    protected AlbumInfo albumInfo;
    protected ArtistInfo artistInfo;
    protected ArtistInfo2 artistInfo2;
    protected SimilarSongs similarSongs;
    protected SimilarSongs2 similarSongs2;
    protected TopSongs topSongs;
    protected ScanStatus scanStatus;
    protected Error error;
    @XmlAttribute(name = "status", required = true)
    protected ResponseStatus status;
    @XmlAttribute(name = "version", required = true)
    protected String version;

    public MusicFolders getMusicFolders() {
        return musicFolders;
    }

    public void setMusicFolders(MusicFolders value) {
        this.musicFolders = value;
    }

    public Indexes getIndexes() {
        return indexes;
    }

    public void setIndexes(Indexes value) {
        this.indexes = value;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory value) {
        this.directory = value;
    }

    public Genres getGenres() {
        return genres;
    }

    public void setGenres(Genres value) {
        this.genres = value;
    }

    public ArtistsID3 getArtists() {
        return artists;
    }

    public void setArtists(ArtistsID3 value) {
        this.artists = value;
    }

    public ArtistWithAlbumsID3 getArtist() {
        return artist;
    }

    public void setArtist(ArtistWithAlbumsID3 value) {
        this.artist = value;
    }

    public AlbumWithSongsID3 getAlbum() {
        return album;
    }

    public void setAlbum(AlbumWithSongsID3 value) {
        this.album = value;
    }

    public Child getSong() {
        return song;
    }

    public void setSong(Child value) {
        this.song = value;
    }

    public Videos getVideos() {
        return videos;
    }

    public void setVideos(Videos value) {
        this.videos = value;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo value) {
        this.videoInfo = value;
    }

    public NowPlaying getNowPlaying() {
        return nowPlaying;
    }

    public void setNowPlaying(NowPlaying value) {
        this.nowPlaying = value;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult value) {
        this.searchResult = value;
    }

    public SearchResult2 getSearchResult2() {
        return searchResult2;
    }

    public void setSearchResult2(SearchResult2 value) {
        this.searchResult2 = value;
    }

    public SearchResult3 getSearchResult3() {
        return searchResult3;
    }

    public void setSearchResult3(SearchResult3 value) {
        this.searchResult3 = value;
    }

    public Playlists getPlaylists() {
        return playlists;
    }

    public void setPlaylists(Playlists value) {
        this.playlists = value;
    }

    public PlaylistWithSongs getPlaylist() {
        return playlist;
    }

    public void setPlaylist(PlaylistWithSongs value) {
        this.playlist = value;
    }

    public JukeboxStatus getJukeboxStatus() {
        return jukeboxStatus;
    }

    public void setJukeboxStatus(JukeboxStatus value) {
        this.jukeboxStatus = value;
    }

    public JukeboxPlaylist getJukeboxPlaylist() {
        return jukeboxPlaylist;
    }

    public void setJukeboxPlaylist(JukeboxPlaylist value) {
        this.jukeboxPlaylist = value;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License value) {
        this.license = value;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users value) {
        this.users = value;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User value) {
        this.user = value;
    }

    public ChatMessages getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(ChatMessages value) {
        this.chatMessages = value;
    }

    public AlbumList getAlbumList() {
        return albumList;
    }

    public void setAlbumList(AlbumList value) {
        this.albumList = value;
    }

    public AlbumList2 getAlbumList2() {
        return albumList2;
    }

    public void setAlbumList2(AlbumList2 value) {
        this.albumList2 = value;
    }

    public Songs getRandomSongs() {
        return randomSongs;
    }

    public void setRandomSongs(Songs value) {
        this.randomSongs = value;
    }

    public Songs getSongsByGenre() {
        return songsByGenre;
    }

    public void setSongsByGenre(Songs value) {
        this.songsByGenre = value;
    }

    public Lyrics getLyrics() {
        return lyrics;
    }

    public void setLyrics(Lyrics value) {
        this.lyrics = value;
    }

    public Podcasts getPodcasts() {
        return podcasts;
    }

    public void setPodcasts(Podcasts value) {
        this.podcasts = value;
    }

    public NewestPodcasts getNewestPodcasts() {
        return newestPodcasts;
    }

    public void setNewestPodcasts(NewestPodcasts value) {
        this.newestPodcasts = value;
    }

    public InternetRadioStations getInternetRadioStations() {
        return internetRadioStations;
    }

    public void setInternetRadioStations(InternetRadioStations value) {
        this.internetRadioStations = value;
    }

    public Bookmarks getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(Bookmarks value) {
        this.bookmarks = value;
    }

    public PlayQueue getPlayQueue() {
        return playQueue;
    }

    public void setPlayQueue(PlayQueue value) {
        this.playQueue = value;
    }

    public Shares getShares() {
        return shares;
    }

    public void setShares(Shares value) {
        this.shares = value;
    }

    public Starred getStarred() {
        return starred;
    }

    public void setStarred(Starred value) {
        this.starred = value;
    }

    public Starred2 getStarred2() {
        return starred2;
    }

    public void setStarred2(Starred2 value) {
        this.starred2 = value;
    }

    public AlbumInfo getAlbumInfo() {
        return albumInfo;
    }

    public void setAlbumInfo(AlbumInfo value) {
        this.albumInfo = value;
    }

    public ArtistInfo getArtistInfo() {
        return artistInfo;
    }

    public void setArtistInfo(ArtistInfo value) {
        this.artistInfo = value;
    }

    public ArtistInfo2 getArtistInfo2() {
        return artistInfo2;
    }

    public void setArtistInfo2(ArtistInfo2 value) {
        this.artistInfo2 = value;
    }

    public SimilarSongs getSimilarSongs() {
        return similarSongs;
    }

    public void setSimilarSongs(SimilarSongs value) {
        this.similarSongs = value;
    }

    public SimilarSongs2 getSimilarSongs2() {
        return similarSongs2;
    }

    public void setSimilarSongs2(SimilarSongs2 value) {
        this.similarSongs2 = value;
    }

    public TopSongs getTopSongs() {
        return topSongs;
    }

    public void setTopSongs(TopSongs value) {
        this.topSongs = value;
    }

    public ScanStatus getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(ScanStatus value) {
        this.scanStatus = value;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error value) {
        this.error = value;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus value) {
        this.status = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        this.version = value;
    }
}
