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

package com.tesshu.jpsonic.service.upnp.processor;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.Genre;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.MusicIndex;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.domain.PodcastStatus;
import com.tesshu.jpsonic.service.CoverArtPresentation;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jupnp.support.model.DIDLObject.Property;
import org.jupnp.support.model.DIDLObject.Property.UPNP;
import org.jupnp.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.jupnp.support.model.DIDLObject.Property.UPNP.AUTHOR;
import org.jupnp.support.model.PersonWithRole;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.container.GenreContainer;
import org.jupnp.support.model.container.MusicAlbum;
import org.jupnp.support.model.container.MusicArtist;
import org.jupnp.support.model.container.PlaylistContainer;
import org.jupnp.support.model.container.StorageFolder;
import org.jupnp.support.model.item.MusicTrack;
import org.jupnp.support.model.item.VideoItem;
import org.jupnp.util.MimeType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class that defines the conversion of domain objects to DIDL objects. Note that when a Container is defined, a
 * specific ProcId is specified. This means that the structure of some data tree nodes is effectively shared across
 * multiple ContentProcessors.
 */
@Component
public class UpnpDIDLFactory implements CoverArtPresentation {

    private static final ThreadLocal<DateTimeFormatter> DATE_FORMAT = ThreadLocal
            .withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()));
    private static final String SUB_DIR_EXT = "/ext/";

    private final SettingsService settingsService;
    private final JWTSecurityService jwtSecurityService;
    private final MediaFileService mediaFileService;
    private final PlayerService playerService;
    private final TranscodingService transcodingService;

    public UpnpDIDLFactory(SettingsService settingsService, JWTSecurityService jwtSecurityService,
            MediaFileService mediaFileService, PlayerService playerService, TranscodingService transcodingService) {
        this.settingsService = settingsService;
        this.jwtSecurityService = jwtSecurityService;
        this.mediaFileService = mediaFileService;
        this.playerService = playerService;
        this.transcodingService = transcodingService;
    }

    public UPNP.ARTIST toPerson(String artistName) {
        return new UPNP.ARTIST(new PersonWithRole(artistName));
    }

    public UPNP.AUTHOR toComposer(String composerName) {
        return new AUTHOR(new PersonWithRole(composerName, "composer"));
    }

    private UriComponentsBuilder addJWTToken(UriComponentsBuilder builder) {
        return jwtSecurityService.addJWTToken(builder);
    }

    String createURIStringWithToken(UriComponentsBuilder builder, MediaFile song) {
        String token = addJWTToken(builder).toUriString();
        if (settingsService.isUriWithFileExtensions() && !StringUtils.isEmpty(song.getFormat())) {
            Player player = playerService.getGuestPlayer(null);
            String fmt = transcodingService.getSuffix(player, song, null);
            token = token.concat(".").concat(fmt);
        }
        return token;
    }

    private URI createURIWithToken(UriComponentsBuilder builder) {
        return addJWTToken(builder).build().encode().toUri();
    }

    private String getBaseUrl() {
        String dlnaBaseLANURL = settingsService.getDlnaBaseLANURL();
        if (StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new IllegalArgumentException("UPnP Base LAN URL is not set correctly");
        }
        return dlnaBaseLANURL;
    }

    private Property<URI> toArtistArt(@NonNull MediaFile artist) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", artist.getId()).queryParam("size", CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toArtistArt(Artist artist) {
        URI uri = createURIWithToken(UriComponentsBuilder
                .fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                .queryParam("id", createCoverArtKey(artist)).queryParam("size", CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toAlbumArt(@NonNull MediaFile album) {
        URI uri = createURIWithToken(UriComponentsBuilder
                .fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value()).queryParam("id", album.getId())
                .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toAlbumArt(Album album) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", createCoverArtKey(album))
                        .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toPodcastArt(PodcastChannel channel) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", createCoverArtKey(channel))
                        .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toPlaylistArt(Playlist playlist) {
        URI uri = addJWTToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", createCoverArtKey(playlist))
                        .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize())).build().encode()
                                .toUri();
        return new ALBUM_ART_URI(uri);
    }

    private String createStreamURI(MediaFile song, Player player) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/ext/stream")
                .queryParam("id", song.getId()).queryParam("player", player.getId());
        if (song.isVideo()) {
            builder.queryParam("format", TranscodingService.FORMAT_RAW);
        }
        return createURIStringWithToken(builder, song);
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        return StringUtil.formatDurationHMMSS(seconds) + ".0";
    }

    private MimeType getMimeType(MediaFile song, Player player) {
        String suffix = song.isVideo() ? FilenameUtils.getExtension(song.getPathString())
                : transcodingService.getSuffix(player, song, null);
        String mimeTypeString = StringUtil.getMimeType(suffix);

        return mimeTypeString == null ? null : MimeType.valueOf(mimeTypeString);
    }

    private StorageFolder createMusicFolder(int id, String name, ProcId procId, int childCount) {
        StorageFolder container = new StorageFolder();
        container.setId(procId.getValue() + ProcId.CID_SEPA + id);
        container.setParentID(procId.getValue());
        container.setTitle(name);
        container.setChildCount(childCount);
        return container;
    }

    public StorageFolder toMusicFolder(MusicFolder folder, ProcId procId, int childCount) {
        return createMusicFolder(folder.getId(), folder.getName(), procId, childCount);
    }

    public StorageFolder toMusicFolder(MediaFile folder, ProcId procId, int childCount) {
        return createMusicFolder(folder.getId(), folder.getName(), procId, childCount);
    }

    public GenreContainer toMusicIndex(MusicIndex musicIndex, ProcId procId, int childCount) {
        GenreContainer container = new GenreContainer();
        container.setId(procId.getValue() + ProcId.CID_SEPA + musicIndex.getIndex());
        container.setParentID(procId.getValue());
        container.setTitle(musicIndex.getIndex());
        container.setChildCount(childCount);
        return container;
    }

    public GenreContainer toGenre(Genre genre, ProcId procId, boolean isCountVisible, int childCount) {
        GenreContainer container = new GenreContainer();
        container.setId(procId.getValue() + ProcId.CID_SEPA + genre.getName());
        container.setParentID(procId.getValue());
        container.setTitle(isCountVisible ? genre.getName().concat(" ").concat(Integer.toString(genre.getSongCount()))
                : genre.getName());
        container.setChildCount(childCount);
        return container;
    }

    public PlaylistContainer toPlaylist(Playlist playlist) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(ProcId.PLAYLIST.getValue() + ProcId.CID_SEPA + playlist.getId());
        container.setParentID(ProcId.PLAYLIST.getValue());
        container.setTitle(playlist.getName());
        container.setDescription(playlist.getComment());
        container.setChildCount(playlist.getFileCount());
        container.addProperty(toPlaylistArt(playlist));
        return container;
    }

    public MusicArtist toArtist(MediaFile artist, int childCount) {
        MusicArtist container = new MusicArtist();
        container.setTitle(artist.getName());
        container.setId(ProcId.FOLDER.getValue() + ProcId.CID_SEPA + artist.getId());
        mediaFileService.getParent(artist).ifPresent(parent -> container.setParentID(String.valueOf(parent.getId())));
        artist.getCoverArtPath().ifPresent(path -> container.addProperty(toArtistArt(artist)));
        container.setChildCount(childCount);
        return container;
    }

    public MusicArtist toArtist(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(ProcId.ARTIST.getValue() + ProcId.CID_SEPA + artist.getId());
        container.setParentID(ProcId.ARTIST.getValue());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());
        if (artist.getCoverArtPath() != null) {
            container.addProperty(toArtistArt(artist));
        }
        return container;
    }

    public MusicAlbum toAlbum(MediaFile album, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.FOLDER.getValue() + ProcId.CID_SEPA + album.getId());
        mediaFileService.getParent(album).ifPresent(parent -> container.setParentID(String.valueOf(parent.getId())));
        container.setChildCount(childCount);
        container.setTitle(album.getName());
        container.addProperty(toPerson(album.getArtist()));
        container.addProperty(toAlbumArt(album));
        container.setDescription(album.getComment());
        return container;
    }

    public MusicAlbum toAlbum(Album album) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.ALBUM.getValue() + ProcId.CID_SEPA + album.getId());
        container.setParentID(ProcId.ALBUM.getValue());
        container.setTitle(album.getName());
        container.setChildCount(album.getSongCount());
        container.addProperty(toAlbumArt(album));
        if (album.getArtist() != null) {
            container.addProperty(toPerson(album.getArtist()));
        }
        container.setDescription(album.getComment());
        return container;
    }

    public MusicAlbum toAlbum(PodcastChannel channel, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.PODCAST.getValue() + ProcId.CID_SEPA + channel.getId());
        container.setParentID(ProcId.PODCAST.getValue());
        container.setTitle(channel.getTitle());
        container.setChildCount(childCount);
        if (!isEmpty(channel.getImageUrl())) {
            container.addProperty(toPodcastArt(channel));
        }
        return container;
    }

    private Res toRes(MediaFile file) {
        Player player = playerService.getGuestPlayer(null);
        MimeType mimeType = getMimeType(file, player);
        Res res = new Res(mimeType, file.getFileSize(), createStreamURI(file, player));
        res.setDuration(formatDuration(file.getDurationSeconds()));
        return res;
    }

    public MusicTrack toMusicTrack(MediaFile song) {

        MusicTrack item = new MusicTrack();

        item.setId(String.valueOf(song.getId()));
        item.setTitle(song.getTitle());
        item.setAlbum(song.getAlbumName());
        if (song.getArtist() != null) {
            item.addProperty(toPerson(song.getArtist()));
        }
        Integer year = song.getYear();
        if (year != null) {
            item.setDate(year + "-01-01");
        }
        item.setOriginalTrackNumber(song.getTrackNumber());
        if (song.getGenre() != null) {
            item.setGenres(new String[] { song.getGenre() });
        }
        item.setResources(Arrays.asList(toRes(song)));
        item.setDescription(song.getComment());

        MediaFile parent = mediaFileService.getParentOf(song);
        if (parent != null) {
            item.setParentID(String.valueOf(parent.getId()));
            item.addProperty(toAlbumArt(parent));
        }

        return item;
    }

    public MusicTrack toMusicTrack(PodcastEpisode episode, @NonNull PodcastChannel channel) {
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.setId(String.valueOf(episode.getId()));
        musicTrack.setTitle(episode.getTitle());
        musicTrack.setParentID(String.valueOf(episode.getChannelId()));
        musicTrack.setAlbum(channel.getTitle());
        if (!isEmpty(channel.getImageUrl())) {
            musicTrack.addProperty(toPodcastArt(channel));
        }
        if (!isEmpty(episode.getPublishDate())) {
            musicTrack.setDate(DATE_FORMAT.get().format(episode.getPublishDate()));
        }
        if (episode.getStatus() == PodcastStatus.COMPLETED && !isEmpty(episode.getMediaFileId())) {
            MediaFile song = mediaFileService.getMediaFileStrict(episode.getMediaFileId());
            musicTrack.setResources(Arrays.asList(toRes(song)));
        }
        return musicTrack;
    }

    public VideoItem toVideo(MediaFile video) {
        VideoItem videoItem = new VideoItem();
        videoItem.setId(String.valueOf(video.getId()));
        videoItem.setTitle(video.getTitle());
        videoItem.setResources(Arrays.asList(toRes(video)));
        videoItem.setDescription(video.getComment());

        MediaFile parent = mediaFileService.getParentOf(video);
        if (parent != null) {
            videoItem.setParentID(String.valueOf(parent.getId()));
            videoItem.addProperty(toAlbumArt(parent));
        }
        if (video.getGenre() != null) {
            videoItem.setGenres(new String[] { video.getGenre() });
        }
        videoItem.setCreator(video.getArtist());
        if (video.getComposer() != null) {
            videoItem.addProperty(toComposer(video.getComposer()));
        }
        return videoItem;
    }
}
