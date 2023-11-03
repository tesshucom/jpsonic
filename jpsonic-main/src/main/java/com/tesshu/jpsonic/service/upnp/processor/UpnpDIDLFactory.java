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

import java.net.URI;
import java.util.Arrays;

import com.tesshu.jpsonic.controller.Attributes;
import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.CoverArtScheme;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Player;
import com.tesshu.jpsonic.domain.Playlist;
import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.service.CoverArtPresentation;
import com.tesshu.jpsonic.service.JWTSecurityService;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.PlayerService;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import com.tesshu.jpsonic.service.upnp.ProcId;
import com.tesshu.jpsonic.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.fourthline.cling.support.model.DIDLObject.Property;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.AUTHOR;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.seamless.util.MimeType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class that defines the conversion of domain objects to DIDL objects. Note that when a Container is defined, a
 * specific ProcId is specified. This means that the structure of some data tree nodes is effectively shared across
 * multiple ContentProcessors.
 */
@Component
public class UpnpDIDLFactory implements CoverArtPresentation {

    private final SettingsService settingsService;
    private final JWTSecurityService jwtSecurityService;
    private final MediaFileService mediaFileService;
    private final PlayerService playerService;
    private final TranscodingService transcodingService;

    private static final String SUB_DIR_EXT = "/ext/";

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

    public Property<URI> toArtistArt(@NonNull MediaFile artist) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", artist.getId()).queryParam("size", CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    public Property<URI> toArtistArt(Artist artist) {
        URI uri = createURIWithToken(UriComponentsBuilder
                .fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                .queryParam("id", createCoverArtKey(artist)).queryParam("size", CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    public Property<URI> toAlbumArt(@NonNull MediaFile album) {
        URI uri = createURIWithToken(UriComponentsBuilder
                .fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value()).queryParam("id", album.getId())
                .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    public Property<URI> toAlbumArt(Album album) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", createCoverArtKey(album))
                        .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    public Property<URI> toPodcastArt(PodcastChannel channel) {
        URI uri = createURIWithToken(
                UriComponentsBuilder.fromUriString(getBaseUrl() + SUB_DIR_EXT + ViewName.COVER_ART.value())
                        .queryParam("id", createCoverArtKey(channel))
                        .queryParam(Attributes.Request.SIZE.value(), CoverArtScheme.LARGE.getSize()));
        return new ALBUM_ART_URI(uri);
    }

    public Property<URI> toPlaylistArt(Playlist playlist) {
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

    public MusicAlbum toAlbum(Album album) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.ALBUM.getValue() + ProcId.CID_SEPA + album.getId());
        container.setParentID(ProcId.ALBUM.getValue());
        container.setTitle(album.getName());
        container.addProperty(toAlbumArt(album));
        if (album.getArtist() != null) {
            container.addProperty(toPerson(album.getArtist()));
        }
        container.setDescription(album.getComment());
        return container;
    }

    public Res toRes(MediaFile file) {
        Player player = playerService.getGuestPlayer(null);
        MimeType mimeType = getMimeType(file, player);
        Res res = new Res(mimeType, null, createStreamURI(file, player));
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
}
