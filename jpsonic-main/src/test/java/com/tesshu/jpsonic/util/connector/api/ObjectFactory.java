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

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java
 * element interface generated in the com.tesshu.jpsonic.util.connector.api
 * package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the
 * Java representation for XML content. The Java representation of XML content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    @SuppressWarnings("PMD.FieldNamingConventions") // XJC Naming Conventions
    private static final QName _SubsonicResponse_QNAME = new QName("http://subsonic.org/restapi",
            "subsonic-response");

    /**
     * Create an instance of {@link Response }
     */
    public Response createResponse() {
        return new Response();
    }

    /**
     * Create an instance of {@link MusicFolders }
     */
    public MusicFolders createMusicFolders() {
        return new MusicFolders();
    }

    /**
     * Create an instance of {@link MusicFolder }
     */
    public MusicFolder createMusicFolder() {
        return new MusicFolder();
    }

    /**
     * Create an instance of {@link Indexes }
     */
    public Indexes createIndexes() {
        return new Indexes();
    }

    /**
     * Create an instance of {@link Index }
     */
    public Index createIndex() {
        return new Index();
    }

    /**
     * Create an instance of {@link Artist }
     */
    public Artist createArtist() {
        return new Artist();
    }

    /**
     * Create an instance of {@link Genres }
     */
    public Genres createGenres() {
        return new Genres();
    }

    /**
     * Create an instance of {@link Genre }
     */
    public Genre createGenre() {
        return new Genre();
    }

    /**
     * Create an instance of {@link ArtistsID3 }
     */
    public ArtistsID3 createArtistsID3() {
        return new ArtistsID3();
    }

    /**
     * Create an instance of {@link IndexID3 }
     */
    public IndexID3 createIndexID3() {
        return new IndexID3();
    }

    /**
     * Create an instance of {@link ArtistID3 }
     */
    public ArtistID3 createArtistID3() {
        return new ArtistID3();
    }

    /**
     * Create an instance of {@link ArtistWithAlbumsID3 }
     */
    public ArtistWithAlbumsID3 createArtistWithAlbumsID3() {
        return new ArtistWithAlbumsID3();
    }

    /**
     * Create an instance of {@link AlbumID3 }
     */
    public AlbumID3 createAlbumID3() {
        return new AlbumID3();
    }

    /**
     * Create an instance of {@link AlbumWithSongsID3 }
     */
    public AlbumWithSongsID3 createAlbumWithSongsID3() {
        return new AlbumWithSongsID3();
    }

    /**
     * Create an instance of {@link Videos }
     */
    public Videos createVideos() {
        return new Videos();
    }

    /**
     * Create an instance of {@link VideoInfo }
     */
    public VideoInfo createVideoInfo() {
        return new VideoInfo();
    }

    /**
     * Create an instance of {@link Captions }
     */
    public Captions createCaptions() {
        return new Captions();
    }

    /**
     * Create an instance of {@link AudioTrack }
     */
    public AudioTrack createAudioTrack() {
        return new AudioTrack();
    }

    /**
     * Create an instance of {@link VideoConversion }
     */
    public VideoConversion createVideoConversion() {
        return new VideoConversion();
    }

    /**
     * Create an instance of {@link Directory }
     */
    public Directory createDirectory() {
        return new Directory();
    }

    /**
     * Create an instance of {@link Child }
     */
    public Child createChild() {
        return new Child();
    }

    /**
     * Create an instance of {@link NowPlaying }
     */
    public NowPlaying createNowPlaying() {
        return new NowPlaying();
    }

    /**
     * Create an instance of {@link NowPlayingEntry }
     */
    public NowPlayingEntry createNowPlayingEntry() {
        return new NowPlayingEntry();
    }

    /**
     * Create an instance of {@link SearchResult }
     */
    public SearchResult createSearchResult() {
        return new SearchResult();
    }

    /**
     * Create an instance of {@link SearchResult2 }
     */
    public SearchResult2 createSearchResult2() {
        return new SearchResult2();
    }

    /**
     * Create an instance of {@link SearchResult3 }
     */
    public SearchResult3 createSearchResult3() {
        return new SearchResult3();
    }

    /**
     * Create an instance of {@link Playlists }
     */
    public Playlists createPlaylists() {
        return new Playlists();
    }

    /**
     * Create an instance of {@link Playlist }
     */
    public Playlist createPlaylist() {
        return new Playlist();
    }

    /**
     * Create an instance of {@link PlaylistWithSongs }
     */
    public PlaylistWithSongs createPlaylistWithSongs() {
        return new PlaylistWithSongs();
    }

    /**
     * Create an instance of {@link JukeboxStatus }
     */
    public JukeboxStatus createJukeboxStatus() {
        return new JukeboxStatus();
    }

    /**
     * Create an instance of {@link JukeboxPlaylist }
     */
    public JukeboxPlaylist createJukeboxPlaylist() {
        return new JukeboxPlaylist();
    }

    /**
     * Create an instance of {@link ChatMessages }
     */
    public ChatMessages createChatMessages() {
        return new ChatMessages();
    }

    /**
     * Create an instance of {@link ChatMessage }
     */
    public ChatMessage createChatMessage() {
        return new ChatMessage();
    }

    /**
     * Create an instance of {@link AlbumList }
     */
    public AlbumList createAlbumList() {
        return new AlbumList();
    }

    /**
     * Create an instance of {@link AlbumList2 }
     */
    public AlbumList2 createAlbumList2() {
        return new AlbumList2();
    }

    /**
     * Create an instance of {@link Songs }
     */
    public Songs createSongs() {
        return new Songs();
    }

    /**
     * Create an instance of {@link Lyrics }
     */
    public Lyrics createLyrics() {
        return new Lyrics();
    }

    /**
     * Create an instance of {@link Podcasts }
     */
    public Podcasts createPodcasts() {
        return new Podcasts();
    }

    /**
     * Create an instance of {@link PodcastChannel }
     */
    public PodcastChannel createPodcastChannel() {
        return new PodcastChannel();
    }

    /**
     * Create an instance of {@link NewestPodcasts }
     */
    public NewestPodcasts createNewestPodcasts() {
        return new NewestPodcasts();
    }

    /**
     * Create an instance of {@link PodcastEpisode }
     */
    public PodcastEpisode createPodcastEpisode() {
        return new PodcastEpisode();
    }

    /**
     * Create an instance of {@link InternetRadioStations }
     */
    public InternetRadioStations createInternetRadioStations() {
        return new InternetRadioStations();
    }

    /**
     * Create an instance of {@link InternetRadioStation }
     */
    public InternetRadioStation createInternetRadioStation() {
        return new InternetRadioStation();
    }

    /**
     * Create an instance of {@link Bookmarks }
     */
    public Bookmarks createBookmarks() {
        return new Bookmarks();
    }

    /**
     * Create an instance of {@link Bookmark }
     */
    public Bookmark createBookmark() {
        return new Bookmark();
    }

    /**
     * Create an instance of {@link PlayQueue }
     */
    public PlayQueue createPlayQueue() {
        return new PlayQueue();
    }

    /**
     * Create an instance of {@link Shares }
     */
    public Shares createShares() {
        return new Shares();
    }

    /**
     * Create an instance of {@link Share }
     */
    public Share createShare() {
        return new Share();
    }

    /**
     * Create an instance of {@link Starred }
     */
    public Starred createStarred() {
        return new Starred();
    }

    /**
     * Create an instance of {@link AlbumInfo }
     */
    public AlbumInfo createAlbumInfo() {
        return new AlbumInfo();
    }

    /**
     * Create an instance of {@link ArtistInfoBase }
     */
    public ArtistInfoBase createArtistInfoBase() {
        return new ArtistInfoBase();
    }

    /**
     * Create an instance of {@link ArtistInfo }
     */
    public ArtistInfo createArtistInfo() {
        return new ArtistInfo();
    }

    /**
     * Create an instance of {@link ArtistInfo2 }
     */
    public ArtistInfo2 createArtistInfo2() {
        return new ArtistInfo2();
    }

    /**
     * Create an instance of {@link SimilarSongs }
     */
    public SimilarSongs createSimilarSongs() {
        return new SimilarSongs();
    }

    /**
     * Create an instance of {@link SimilarSongs2 }
     */
    public SimilarSongs2 createSimilarSongs2() {
        return new SimilarSongs2();
    }

    /**
     * Create an instance of {@link TopSongs }
     */
    public TopSongs createTopSongs() {
        return new TopSongs();
    }

    /**
     * Create an instance of {@link Starred2 }
     */
    public Starred2 createStarred2() {
        return new Starred2();
    }

    /**
     * Create an instance of {@link License }
     */
    public License createLicense() {
        return new License();
    }

    /**
     * Create an instance of {@link ScanStatus }
     */
    public ScanStatus createScanStatus() {
        return new ScanStatus();
    }

    /**
     * Create an instance of {@link Users }
     */
    public Users createUsers() {
        return new Users();
    }

    /**
     * Create an instance of {@link User }
     */
    public User createUser() {
        return new User();
    }

    /**
     * Create an instance of {@link Error }
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Response }{@code >}
     *
     * @param value Java instance representing xml element's value.
     *
     * @return the new instance of {@link JAXBElement }{@code <}{@link Response
     *         }{@code >}
     */
    @XmlElementDecl(namespace = "http://subsonic.org/restapi", name = "subsonic-response")
    public JAXBElement<Response> createSubsonicResponse(Response value) {
        return new JAXBElement<>(_SubsonicResponse_QNAME, Response.class, null, value);
    }

}
