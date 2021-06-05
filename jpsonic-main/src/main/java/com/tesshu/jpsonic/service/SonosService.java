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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import com.sonos.services._1.AbstractMedia;
import com.sonos.services._1.AddToContainerResult;
import com.sonos.services._1.AppLinkResult;
import com.sonos.services._1.ContentKey;
import com.sonos.services._1.CreateContainerResult;
import com.sonos.services._1.Credentials;
import com.sonos.services._1.DeleteContainerResult;
import com.sonos.services._1.DeviceAuthTokenResult;
import com.sonos.services._1.DeviceLinkCodeResult;
import com.sonos.services._1.EncryptionContext;
import com.sonos.services._1.ExtendedMetadata;
import com.sonos.services._1.GetExtendedMetadata;
import com.sonos.services._1.GetExtendedMetadataResponse;
import com.sonos.services._1.GetExtendedMetadataText;
import com.sonos.services._1.GetExtendedMetadataTextResponse;
import com.sonos.services._1.GetMediaMetadata;
import com.sonos.services._1.GetMediaMetadataResponse;
import com.sonos.services._1.GetMetadata;
import com.sonos.services._1.GetMetadataResponse;
import com.sonos.services._1.GetSessionId;
import com.sonos.services._1.GetSessionIdResponse;
import com.sonos.services._1.HttpHeaders;
import com.sonos.services._1.LastUpdate;
import com.sonos.services._1.MediaCollection;
import com.sonos.services._1.MediaList;
import com.sonos.services._1.MediaMetadata;
import com.sonos.services._1.MediaUriAction;
import com.sonos.services._1.PositionInformation;
import com.sonos.services._1.RateItem;
import com.sonos.services._1.RateItemResponse;
import com.sonos.services._1.RelatedBrowse;
import com.sonos.services._1.RemoveFromContainerResult;
import com.sonos.services._1.RenameContainerResult;
import com.sonos.services._1.ReorderContainerResult;
import com.sonos.services._1.ReportPlaySecondsResult;
import com.sonos.services._1.Search;
import com.sonos.services._1.SearchResponse;
import com.sonos.services._1.SegmentMetadataList;
import com.sonos.services._1.UserInfo;
import com.sonos.services._1_1.CustomFault;
import com.sonos.services._1_1.SonosSoap;
import com.tesshu.jpsonic.domain.AlbumListType;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.User;
import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.sonos.SonosHelper;
import com.tesshu.jpsonic.service.sonos.SonosServiceRegistration;
import com.tesshu.jpsonic.service.sonos.SonosSoapFault;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

/**
 * For manual testing of this service: curl -s -X POST -H "Content-Type: text/xml;charset=UTF-8" -H 'SOAPACTION:
 * "http://www.sonos.com/Services/1.1#getSessionId"' -d @getSessionId.xml http://localhost:4040/ws/Sonos | xmllint
 * --format -
 *
 * @author Sindre Mehus
 */
@Service
public class SonosService implements SonosSoap {

    private static final Logger LOG = LoggerFactory.getLogger(SonosService.class);
    public static final String ID_ROOT = "root";
    public static final String ID_SHUFFLE = "shuffle";
    public static final String ID_ALBUMLISTS = "albumlists";
    public static final String ID_PLAYLISTS = "playlists";
    public static final String ID_PODCASTS = "podcasts";
    public static final String ID_LIBRARY = "library";
    public static final String ID_STARRED = "starred";
    public static final String ID_STARRED_ARTISTS = "starred-artists";
    public static final String ID_STARRED_ALBUMS = "starred-albums";
    public static final String ID_STARRED_SONGS = "starred-songs";
    public static final String ID_SEARCH = "search";
    public static final String ID_SHUFFLE_MUSICFOLDER_PREFIX = "shuffle-musicfolder:";
    public static final String ID_SHUFFLE_ARTIST_PREFIX = "shuffle-artist:";
    public static final String ID_SHUFFLE_ALBUMLIST_PREFIX = "shuffle-albumlist:";
    public static final String ID_RADIO_ARTIST_PREFIX = "radio-artist:";
    public static final String ID_MUSICFOLDER_PREFIX = "musicfolder:";
    public static final String ID_PLAYLIST_PREFIX = "playlist:";
    public static final String ID_ALBUMLIST_PREFIX = "albumlist:";
    public static final String ID_PODCAST_CHANNEL_PREFIX = "podcast-channel:";
    public static final String ID_DECADE_PREFIX = "decade:";
    public static final String ID_GENRE_PREFIX = "genre:";
    public static final String ID_SIMILAR_ARTISTS_PREFIX = "similarartists:";
    // Note: These must match the values in presentationMap.xml
    public static final String ID_SEARCH_ARTISTS = "search-artists";
    public static final String ID_SEARCH_ALBUMS = "search-albums";
    public static final String ID_SEARCH_SONGS = "search-songs";

    private final SonosHelper sonosHelper;
    private final MediaFileService mediaFileService;
    private final SecurityService securityService;
    private final SettingsService settingsService;
    private final PlaylistService playlistService;
    private final UPnPService upnpService;

    /**
     * The context for the request. This is used to get the Auth information form the headers as well as using the
     * request url to build the correct media resource url.
     */
    @Resource
    private WebServiceContext context;

    public SonosService(SonosHelper sonosHelper, MediaFileService mediaFileService, SecurityService securityService,
            SettingsService settingsService, PlaylistService playlistService, UPnPService upnpService) {
        super();
        this.sonosHelper = sonosHelper;
        this.mediaFileService = mediaFileService;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.playlistService = playlistService;
        this.upnpService = upnpService;
    }

    public void setMusicServiceEnabled(boolean enabled, String baseUrl) {
        List<String> sonosControllers = upnpService.getSonosControllerHosts();
        if (sonosControllers.isEmpty()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("No Sonos controller found");
            }
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Found Sonos controllers: " + sonosControllers);
        }

        String sonosServiceName = settingsService.getSonosServiceName();
        int sonosServiceId = settingsService.getSonosServiceId();

        for (String sonosController : sonosControllers) {
            try {
                new SonosServiceRegistration().setEnabled(baseUrl, sonosController, enabled, sonosServiceName,
                        sonosServiceId);
                break;
            } catch (IOException x) {
                LOG.warn("Failed to enable/disable music service in Sonos controller {}: {}", sonosController, x);
            }
        }
    }

    @Override
    public LastUpdate getLastUpdate() throws CustomFault {
        LastUpdate result = new LastUpdate();
        // Effectively disabling caching
        result.setCatalog(RandomStringUtils.randomAlphanumeric(8));
        result.setFavorites(RandomStringUtils.randomAlphanumeric(8));
        return result;
    }

    @Override
    public GetMetadataResponse getMetadata(GetMetadata parameters) throws CustomFault {
        String id = parameters.getId();
        int index = parameters.getIndex();
        int count = parameters.getCount();
        String username = getUsername();
        HttpServletRequest request = getRequest();

        if (LOG.isDebugEnabled()) {
            LOG.debug("getMetadata: id={} index={} count={} recursive={}", id, index, count, parameters.isRecursive());
        }

        List<? extends AbstractMedia> media = null;
        MediaList mediaList = null;

        if (ID_ROOT.equals(id)) {
            media = sonosHelper.forRoot();
        } else {
            if (id.startsWith(ID_ALBUMLIST_PREFIX)) {
                AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_ALBUMLIST_PREFIX, ""));
                mediaList = sonosHelper.forAlbumList(albumListType, index, count, username, request);
            } else {
                media = getMedia(id, count, username, request);
            }
        }

        if (mediaList == null) {
            mediaList = SonosHelper.createSubList(index, count, media);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("getMetadata result: id={} index={} count={} total={}", id, mediaList.getIndex(),
                    mediaList.getCount(), mediaList.getTotal());
        }

        GetMetadataResponse response = new GetMetadataResponse();
        response.setGetMetadataResult(mediaList);
        return response;
    }

    private List<? extends AbstractMedia> getMedia(String id, int count, String username, HttpServletRequest request) {

        List<? extends AbstractMedia> media = getMediaWithId(id, count, username, request);
        if (media != null) {
            return media;
        }

        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            media = sonosHelper.forPlaylist(playlistId, username, request);
        } else if (id.startsWith(ID_DECADE_PREFIX)) {
            int decade = Integer.parseInt(id.replace(ID_DECADE_PREFIX, ""));
            media = sonosHelper.forDecade(decade, username, request);
        } else if (id.startsWith(ID_GENRE_PREFIX)) {
            int genre = Integer.parseInt(id.replace(ID_GENRE_PREFIX, ""));
            media = sonosHelper.forGenre(genre, username, request);
        } else if (id.startsWith(ID_PODCAST_CHANNEL_PREFIX)) {
            int channelId = Integer.parseInt(id.replace(ID_PODCAST_CHANNEL_PREFIX, ""));
            media = sonosHelper.forPodcastChannel(channelId, username, request);
        } else if (id.startsWith(ID_MUSICFOLDER_PREFIX)) {
            int musicFolderId = Integer.parseInt(id.replace(ID_MUSICFOLDER_PREFIX, ""));
            media = sonosHelper.forMusicFolder(musicFolderId, username, request);
        } else if (id.startsWith(ID_SHUFFLE_MUSICFOLDER_PREFIX)) {
            int musicFolderId = Integer.parseInt(id.replace(ID_SHUFFLE_MUSICFOLDER_PREFIX, ""));
            media = sonosHelper.forShuffleMusicFolder(musicFolderId, count, username, request);
        } else if (id.startsWith(ID_SHUFFLE_ARTIST_PREFIX)) {
            int mediaFileId = Integer.parseInt(id.replace(ID_SHUFFLE_ARTIST_PREFIX, ""));
            media = sonosHelper.forShuffleArtist(mediaFileId, count, username, request);
        } else if (id.startsWith(ID_SHUFFLE_ALBUMLIST_PREFIX)) {
            AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_SHUFFLE_ALBUMLIST_PREFIX, ""));
            media = sonosHelper.forShuffleAlbumList(albumListType, count, username, request);
        } else if (id.startsWith(ID_RADIO_ARTIST_PREFIX)) {
            int mediaFileId = Integer.parseInt(id.replace(ID_RADIO_ARTIST_PREFIX, ""));
            media = sonosHelper.forRadioArtist(mediaFileId, count, username, request);
        } else if (id.startsWith(ID_SIMILAR_ARTISTS_PREFIX)) {
            int mediaFileId = Integer.parseInt(id.replace(ID_SIMILAR_ARTISTS_PREFIX, ""));
            media = sonosHelper.forSimilarArtists(mediaFileId, username, request);
        } else {
            media = sonosHelper.forDirectoryContent(Integer.parseInt(id), username, request);
        }
        return media;
    }

    private List<? extends AbstractMedia> getMediaWithId(String id, int count, String username,
            HttpServletRequest request) {
        List<? extends AbstractMedia> media = null;
        if (ID_SHUFFLE.equals(id)) {
            media = sonosHelper.forShuffle(count, username, request);
        } else if (ID_LIBRARY.equals(id)) {
            media = sonosHelper.forLibrary(username, request);
        } else if (ID_PLAYLISTS.equals(id)) {
            media = sonosHelper.forPlaylists(username, request);
        } else if (ID_ALBUMLISTS.equals(id)) {
            media = sonosHelper.forAlbumLists();
        } else if (ID_PODCASTS.equals(id)) {
            media = sonosHelper.forPodcastChannels();
        } else if (ID_STARRED.equals(id)) {
            media = sonosHelper.forStarred();
        } else if (ID_STARRED_ARTISTS.equals(id)) {
            media = sonosHelper.forStarredArtists(username, request);
        } else if (ID_STARRED_ALBUMS.equals(id)) {
            media = sonosHelper.forStarredAlbums(username, request);
        } else if (ID_STARRED_SONGS.equals(id)) {
            media = sonosHelper.forStarredSongs(username, request);
        } else if (ID_SEARCH.equals(id)) {
            media = sonosHelper.forSearchCategories();
        }
        return media;
    }

    @Override
    public GetExtendedMetadataResponse getExtendedMetadata(GetExtendedMetadata parameters) throws CustomFault {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getExtendedMetadata: " + parameters.getId());
        }

        int id = Integer.parseInt(parameters.getId());
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        AbstractMedia abstractMedia = sonosHelper.forMediaFile(mediaFile, getUsername(), getRequest());

        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        if (abstractMedia instanceof MediaCollection) {
            extendedMetadata.setMediaCollection((MediaCollection) abstractMedia);
        } else {
            extendedMetadata.setMediaMetadata((MediaMetadata) abstractMedia);
        }

        RelatedBrowse relatedBrowse = new RelatedBrowse();
        relatedBrowse.setType("RELATED_ARTISTS");
        relatedBrowse.setId(ID_SIMILAR_ARTISTS_PREFIX + id);
        extendedMetadata.getRelatedBrowse().add(relatedBrowse);

        GetExtendedMetadataResponse response = new GetExtendedMetadataResponse();
        response.setGetExtendedMetadataResult(extendedMetadata);
        return response;
    }

    @Override
    public SearchResponse search(Search parameters) throws CustomFault {
        IndexType indexType = getIndexType(parameters.getId());
        MediaList mediaList = sonosHelper.forSearch(parameters.getTerm(), parameters.getIndex(), parameters.getCount(),
                indexType, getUsername(), getRequest());
        SearchResponse response = new SearchResponse();
        response.setSearchResult(mediaList);
        return response;
    }

    private IndexType getIndexType(String id) {
        IndexType indexType;
        if (ID_SEARCH_ARTISTS.equals(id)) {
            indexType = IndexType.ARTIST;
        } else if (ID_SEARCH_ALBUMS.equals(id)) {
            indexType = IndexType.ALBUM;
        } else if (ID_SEARCH_SONGS.equals(id)) {
            indexType = IndexType.SONG;
        } else {
            throw new IllegalArgumentException("Invalid search category: " + id);
        }
        return indexType;
    }

    @Override
    public GetSessionIdResponse getSessionId(GetSessionId parameters) throws CustomFault {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getSessionId: " + parameters.getUsername());
        }
        User user = securityService.getUserByName(parameters.getUsername());
        if (user == null || !StringUtils.equals(user.getPassword(), parameters.getPassword())) {
            throw new SonosSoapFault.LoginInvalid();
        }

        // Use username as session ID for easy access to it later.
        GetSessionIdResponse result = new GetSessionIdResponse();
        result.setGetSessionIdResult(user.getUsername());
        return result;
    }

    @Override
    public UserInfo getUserInfo() throws CustomFault {
        return null;
    }

    @Override
    public GetMediaMetadataResponse getMediaMetadata(GetMediaMetadata parameters) throws CustomFault {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getMediaMetadata: " + parameters.getId());
        }

        GetMediaMetadataResponse response = new GetMediaMetadataResponse();

        // This method is called whenever a playlist is modified. Don't know why.
        // Return an empty response to avoid ugly log message.
        if (parameters.getId().startsWith(ID_PLAYLIST_PREFIX)) {
            return response;
        }

        int id = Integer.parseInt(parameters.getId());
        MediaFile song = mediaFileService.getMediaFile(id);

        response.setGetMediaMetadataResult(sonosHelper.forSong(song, getUsername(), getRequest()));

        return response;
    }

    @Override
    public void getMediaURI(String id, MediaUriAction action, Integer secondsSinceExplicit,
            Holder<String> deviceSessionToken, Holder<String> getMediaURIResult,
            Holder<EncryptionContext> deviceSessionKey, Holder<EncryptionContext> contentKey,
            Holder<HttpHeaders> httpHeaders, Holder<Integer> uriTimeout,
            Holder<PositionInformation> positionInformation, Holder<String> privateDataFieldName) throws CustomFault {
        getMediaURIResult.value = sonosHelper.getMediaURI(Integer.parseInt(id), getUsername(), getRequest());
        if (LOG.isDebugEnabled()) {
            LOG.debug("getMediaURI: " + id + " -> " + getMediaURIResult.value);
        }
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI") // Because it's jws API
    @Override
    public CreateContainerResult createContainer(String containerType, String title, String parentId, String seedId)
            throws CustomFault {
        Date now = new Date();
        Playlist playlist = new Playlist();
        playlist.setName(title);
        playlist.setUsername(getUsername());
        playlist.setCreated(now);
        playlist.setChanged(now);
        playlist.setShared(false);

        playlistService.createPlaylist(playlist);
        CreateContainerResult result = new CreateContainerResult();
        result.setId(ID_PLAYLIST_PREFIX + playlist.getId());
        addItemToPlaylist(playlist.getId(), seedId, -1);

        return result;
    }

    @Override
    public DeleteContainerResult deleteContainer(String id) throws CustomFault {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                playlistService.deletePlaylist(playlistId);
            }
        }
        return new DeleteContainerResult();
    }

    @Override
    public RenameContainerResult renameContainer(String id, String title) throws CustomFault {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                playlist.setName(title);
                playlistService.updatePlaylist(playlist);
            }
        }
        return new RenameContainerResult();
    }

    @Override
    public AddToContainerResult addToContainer(String id, String parentId, int index, String updateId)
            throws CustomFault {
        if (parentId.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(parentId.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                addItemToPlaylist(playlistId, id, index);
            }
        }
        return new AddToContainerResult();
    }

    @Override
    public DeviceAuthTokenResult refreshAuthToken() throws CustomFault {
        return null;
    }

    private void addItemToPlaylist(int playlistId, String id, final int index) throws CustomFault {
        if (StringUtils.isBlank(id)) {
            return;
        }

        GetMetadata parameters = new GetMetadata();
        parameters.setId(id);
        parameters.setIndex(0);
        parameters.setCount(Integer.MAX_VALUE);
        GetMetadataResponse metadata = getMetadata(parameters);
        List<MediaFile> newSongs = new ArrayList<>();

        metadata.getGetMetadataResult().getMediaCollectionOrMediaMetadata().stream()
                .filter(media -> StringUtils.isNumeric(media.getId())).forEach(media -> {
                    MediaFile mediaFile = mediaFileService.getMediaFile(Integer.parseInt(media.getId()));
                    if (mediaFile != null && mediaFile.isFile()) {
                        newSongs.add(mediaFile);
                    }
                });

        List<MediaFile> existingSongs = playlistService.getFilesInPlaylist(playlistId);
        int i = index;
        if (i == -1) {
            i = existingSongs.size();
        }

        existingSongs.addAll(i, newSongs);
        playlistService.setFilesInPlaylist(playlistId, existingSongs);
    }

    @Override
    public ReorderContainerResult reorderContainer(String id, String from, int to, String updateId) throws CustomFault {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                setFilesInPlaylist(playlistId, from, to);
            }
        }
        return new ReorderContainerResult();
    }

    private void setFilesInPlaylist(int playlistId, String from, int to) {
        SortedMap<Integer, MediaFile> indexToSong = new ConcurrentSkipListMap<>();
        List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
        for (int i = 0; i < songs.size(); i++) {
            indexToSong.put(i, songs.get(i));
        }

        List<MediaFile> movedSongs = new ArrayList<>();
        for (Integer i : parsePlaylistIndices(from)) {
            movedSongs.add(indexToSong.remove(i));
        }

        List<MediaFile> updatedSongs = new ArrayList<>();
        updatedSongs.addAll(indexToSong.headMap(to).values());
        updatedSongs.addAll(movedSongs);
        updatedSongs.addAll(indexToSong.tailMap(to).values());

        playlistService.setFilesInPlaylist(playlistId, updatedSongs);
    }

    @Override
    public RemoveFromContainerResult removeFromContainer(String id, String indices, String updateId)
            throws CustomFault {
        if (id.startsWith(ID_PLAYLIST_PREFIX)) {
            int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
            Playlist playlist = playlistService.getPlaylist(playlistId);
            if (playlist != null && playlist.getUsername().equals(getUsername())) {
                SortedSet<Integer> indicesToRemove = parsePlaylistIndices(indices);
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                List<MediaFile> updatedSongs = new ArrayList<>();
                for (int i = 0; i < songs.size(); i++) {
                    if (!indicesToRemove.contains(i)) {
                        updatedSongs.add(songs.get(i));
                    }
                }
                playlistService.setFilesInPlaylist(playlistId, updatedSongs);
            }
        }
        return new RemoveFromContainerResult();
    }

    protected SortedSet<Integer> parsePlaylistIndices(String indices) {
        // Comma-separated, may include ranges: 1,2,4-7
        SortedSet<Integer> result = new TreeSet<>();

        for (String part : StringUtils.split(indices, ',')) {
            if (StringUtils.isNumeric(part)) {
                result.add(Integer.parseInt(part));
            } else {
                int dashIndex = part.indexOf('-');
                int from = Integer.parseInt(part.substring(0, dashIndex));
                int to = Integer.parseInt(part.substring(dashIndex + 1));
                for (int i = from; i <= to; i++) {
                    result.add(i);
                }
            }
        }
        return result;
    }

    @Override
    public String createItem(String favorite) throws CustomFault {
        int id = Integer.parseInt(favorite);
        sonosHelper.star(id, getUsername());
        return favorite;
    }

    @Override
    public void deleteItem(String favorite) throws CustomFault {
        int id = Integer.parseInt(favorite);
        sonosHelper.unstar(id, getUsername());
    }

    private HttpServletRequest getRequest() {
        MessageContext messageContext = context == null ? null : context.getMessageContext();

        // See org.apache.cxf.transport.http.AbstractHTTPDestination#HTTP_REQUEST
        return messageContext == null ? null : (HttpServletRequest) messageContext.get("HTTP.REQUEST");
    }

    private String getUsername() {
        MessageContext messageContext = context.getMessageContext();
        if (!(messageContext instanceof WrappedMessageContext)) {
            writeError("Message context is null or not an instance of WrappedMessageContext.");
            return null;
        }
        Message message = ((WrappedMessageContext) messageContext).getWrappedMessage();
        return getUsername(message);
    }

    private String getUsername(Message message) {
        List<Header> headers = CastUtils.cast((List<?>) message.get(Header.HEADER_LIST));
        if (headers == null) {
            writeError("No headers found");
        } else {
            // Unwrap the node using JAXB
            JAXBContext jaxbContext = null;
            try {
                jaxbContext = new JAXBDataBinding(Credentials.class).getContext();
            } catch (JAXBException e) {
                // failed to get the credentials object from the headers
                writeError("JAXB error trying to unwrap credentials", e);
            }
            if (jaxbContext == null) {
                return null;
            }
            return getUsername(headers, jaxbContext);
        }
        return null;
    }

    private String getUsername(List<Header> headers, JAXBContext jaxbContext) {
        for (Header h : headers) {
            Object o = h.getObject();
            if (o instanceof Node) {
                try {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    o = unmarshaller.unmarshal((Node) o);
                } catch (JAXBException e) {
                    // failed to get the credentials object from the headers
                    writeError("JAXB error trying to unwrap credentials", e);
                }
            }
            if (o instanceof Credentials) {
                Credentials c = (Credentials) o;

                // Note: We're using the username as session ID.
                String username = c.getSessionId();
                if (username == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No session id in credentials object, get from login");
                    }
                    username = c.getLogin().getUsername();
                }
                return username;
            } else {
                writeError("No credentials object");
            }
        }
        return null;
    }

    protected static void writeError(String msg, Throwable t) {
        if (LOG.isErrorEnabled()) {
            LOG.warn(msg, t);
        }
    }

    protected static void writeError(String msg) {
        if (LOG.isErrorEnabled()) {
            LOG.warn(msg);
        }
    }

    @Override
    public RateItemResponse rateItem(RateItem parameters) throws CustomFault {
        return null;
    }

    @Override
    public SegmentMetadataList getStreamingMetadata(String id, XMLGregorianCalendar startTime, int duration)
            throws CustomFault {
        return null;
    }

    @Override
    public GetExtendedMetadataTextResponse getExtendedMetadataText(GetExtendedMetadataText parameters)
            throws CustomFault {
        return null;
    }

    @Override
    public DeviceLinkCodeResult getDeviceLinkCode(String householdId) throws CustomFault {
        return null;
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI") // Because it's jws API
    @Override
    public AppLinkResult getAppLink(String householdId, String hardware, String osVersion, String sonosAppName,
            String callbackPath) throws CustomFault {
        return null;
    }

    @Override
    public void reportAccountAction(String type) throws CustomFault {
        // Nothing is currently done.
    }

    @Override
    public void setPlayedSeconds(String id, int seconds, String contextId, String privateData, Integer offsetMillis)
            throws CustomFault {
        // Nothing is currently done.
    }

    @Override
    public ReportPlaySecondsResult reportPlaySeconds(String id, int seconds, String contextId, String privateData,
            Integer offsetMillis) throws CustomFault {
        return null;
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI") // Because it's jws API
    @Override
    public DeviceAuthTokenResult getDeviceAuthToken(String householdId, String linkCode, String linkDeviceId,
            String callbackPath) throws CustomFault {
        return null;
    }

    @Override
    public void reportStatus(String id, int errorCode, String message) throws CustomFault {
        // Nothing is currently done.
    }

    @Override
    public String getScrollIndices(String id) throws CustomFault {
        return null;
    }

    @Override
    public void reportPlayStatus(String id, String status, String contextId, Integer offsetMillis) throws CustomFault {
        // Nothing is currently done.
    }

    @Override
    public ContentKey getContentKey(String id, String uri, String deviceSessionToken) throws CustomFault {
        return null;
    }
}
