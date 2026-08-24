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

package com.tesshu.jpsonic.feature.upnp.content;

import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.tesshu.jpsonic.domain.language.StringUtil;
import com.tesshu.jpsonic.domain.model.Album;
import com.tesshu.jpsonic.domain.model.Artist;
import com.tesshu.jpsonic.domain.model.Genre;
import com.tesshu.jpsonic.domain.model.MediaFile;
import com.tesshu.jpsonic.domain.model.MediaFile.DurationSeconds;
import com.tesshu.jpsonic.domain.model.MediaFile.Format;
import com.tesshu.jpsonic.domain.model.MusicFolder;
import com.tesshu.jpsonic.domain.model.MusicIndex;
import com.tesshu.jpsonic.domain.model.Player;
import com.tesshu.jpsonic.domain.model.Playlist;
import com.tesshu.jpsonic.domain.model.PodcastChannel;
import com.tesshu.jpsonic.domain.model.PodcastEpisode;
import com.tesshu.jpsonic.domain.model.PodcastEpisode.EpisodePhase;
import com.tesshu.jpsonic.domain.provider.resource.MediaFileProvider;
import com.tesshu.jpsonic.domain.provider.resource.PlayerProvider;
import com.tesshu.jpsonic.domain.system.CoverArtScheme;
import com.tesshu.jpsonic.domain.system.PreferredFormatScheme;
import com.tesshu.jpsonic.domain.type.CoverArtType;
import com.tesshu.jpsonic.feature.crypt.upnp.StreamPayload.StreamType;
import com.tesshu.jpsonic.feature.crypt.upnp.UpnpPayloadCodec;
import com.tesshu.jpsonic.feature.transcoding.ResolvedAudioTranscodingParameters;
import com.tesshu.jpsonic.feature.transcoding.TranscodingParametersPlanner;
import com.tesshu.jpsonic.feature.upnp.UPnPSKeys;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderArtist;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenre;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.FolderGenreAlbum;
import com.tesshu.jpsonic.feature.upnp.content.processor.composite.GenreAlbum;
import com.tesshu.jpsonic.infrastructure.settings.SKeys;
import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * Converts Jpsonic domain and composite models into UPnP DIDL representations.
 *
 * <p>
 * The factory forms the representation boundary between the content processors
 * and the UPnP data model. Processors determine which domain data belongs in a
 * result, while this factory defines how that data is represented as UPnP
 * containers, items, and resources.
 * </p>
 *
 * <p>
 * Because the same domain structure can appear in multiple ContentDirectory
 * hierarchies, the factory accepts the applicable {@link ProcId} when
 * constructing containers rather than assigning processor-specific namespaces
 * itself. This allows DIDL representation logic to be shared across processors
 * without coupling those processors together.
 * </p>
 */
@Component
public class UPnPDIDLFactory {

    private static final ThreadLocal<DateTimeFormatter> DATE_FORMAT = ThreadLocal
        .withInitial(
                () -> DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()));

    private final SettingsFacade settingsFacade;
    private final MediaFileProvider mediaFileProvider;
    private final PlayerProvider playerProvider;
    private final UpnpPayloadCodec upnpPayloadCodec;
    private final TranscodingParametersPlanner transcodingParametersPlanner;

    public UPnPDIDLFactory(SettingsFacade settingsFacade, UpnpPayloadCodec upnpPayloadCodec,
            MediaFileProvider mediaFileProvider, PlayerProvider playerProvider,
            TranscodingParametersPlanner transcodingParametersPlanner) {
        this.settingsFacade = settingsFacade;
        this.upnpPayloadCodec = upnpPayloadCodec;
        this.mediaFileProvider = mediaFileProvider;
        this.playerProvider = playerProvider;
        this.transcodingParametersPlanner = transcodingParametersPlanner;
    }

    public UPNP.ARTIST toPerson(String artistName) {
        return new UPNP.ARTIST(new PersonWithRole(artistName));
    }

    public UPNP.AUTHOR toComposer(String composerName) {
        return new AUTHOR(new PersonWithRole(composerName, "composer"));
    }

    private String getBaseUrl() {
        String dlnaBaseLANURL = settingsFacade.get(UPnPSKeys.basic.baseLanUrl);
        if (StringUtils.isBlank(dlnaBaseLANURL)) {
            throw new IllegalArgumentException("UPnP Base LAN URL is not set correctly");
        }
        return dlnaBaseLANURL;
    }

    URI createCoverArtURI(String id, int size) {
        String payload = upnpPayloadCodec.encodeArt(id, size);
        // The file extension is provisional (it wasn't originally designed precisely).
        return UriComponentsBuilder
            .fromUriString(getBaseUrl() + "/ext/upnp/art/" + payload + ".jpeg")
            .build()
            .toUri();
    }

    private Property<URI> toArtistArt(@NonNull MediaFile artist) {
        URI uri = createCoverArtURI(Integer.toString(artist.id()), CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toArtistArt(Artist artist) {
        URI uri = createCoverArtURI(CoverArtType.ARTIST.createKey(artist.id()),
                CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toAlbumArt(@NonNull MediaFile album) {
        URI uri = createCoverArtURI(Integer.toString(album.id()), CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toAlbumArt(Album album) {
        URI uri = createCoverArtURI(CoverArtType.ID3ALBUM.createKey(album.id()),
                CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toPodcastArt(PodcastChannel channel) {
        URI uri = createCoverArtURI(CoverArtType.PODCAST.createKey(channel.id()),
                CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private Property<URI> toPlaylistArt(Playlist playlist) {
        URI uri = createCoverArtURI(CoverArtType.PLAYLIST.createKey(playlist.id()),
                CoverArtScheme.LARGE.getSize());
        return new ALBUM_ART_URI(uri);
    }

    private String formatDuration(DurationSeconds durationSeconds) {
        if (durationSeconds == DurationSeconds.UNDEFINED) {
            return null;
        }
        return StringUtil.formatDurationHMMSS(durationSeconds) + ".0";
    }

    private StorageFolder createMusicFolder(ProcId procId, int id, String name, int childCount) {
        StorageFolder container = new StorageFolder();
        container.setId(procId.getValue() + ProcId.CID_SEPA + id);
        container.setParentID(procId.getValue());
        container.setTitle(name);
        container.setChildCount(childCount);
        return container;
    }

    public StorageFolder toMusicFolder(ProcId procId, MusicFolder folder, int childCount) {
        return createMusicFolder(procId, folder.id(), folder.name(), childCount);
    }

    public StorageFolder toMusicFolder(ProcId procId, MediaFile folder, int childCount) {
        return createMusicFolder(procId, folder.id(), folder.name(), childCount);
    }

    public GenreContainer toMusicIndex(ProcId procId, MusicIndex musicIndex, int childCount) {
        GenreContainer container = new GenreContainer();
        container.setId(procId.getValue() + ProcId.CID_SEPA + musicIndex.index());
        container.setParentID(procId.getValue());
        container.setTitle(musicIndex.index());
        container.setChildCount(childCount);
        return container;
    }

    public GenreContainer toGenre(ProcId procId, Genre genre, int childCount) {
        GenreContainer container = new GenreContainer();
        container.setId(procId.getValue() + ProcId.CID_SEPA + genre.name());
        container.setParentID(procId.getValue());
        container.setTitle(genre.name());
        container.setChildCount(childCount);
        return container;
    }

    public GenreContainer toGenre(ProcId procId, FolderGenre folderGenre, int childCount) {
        GenreContainer container = new GenreContainer();
        container.setId(procId.getValue() + ProcId.CID_SEPA + folderGenre.createCompositeId());
        container.setParentID(procId.getValue());
        container.setTitle(folderGenre.genre().name());
        container.setChildCount(childCount);
        return container;
    }

    public PlaylistContainer toPlaylist(Playlist playlist) {
        PlaylistContainer container = new PlaylistContainer();
        container.setId(ProcId.PLAYLIST.getValue() + ProcId.CID_SEPA + playlist.id());
        container.setParentID(ProcId.PLAYLIST.getValue());
        container.setTitle(playlist.name());
        container.setDescription(playlist.comment());
        container.setChildCount(playlist.fileCount());
        container.addProperty(toPlaylistArt(playlist));
        return container;
    }

    public MusicArtist toArtist(MediaFile artist, int childCount) {
        MusicArtist container = new MusicArtist();
        container.setTitle(artist.name());
        container.setId(ProcId.MEDIA_FILE.getValue() + ProcId.CID_SEPA + artist.id());
        container.setParentID(ProcId.MEDIA_FILE.getValue());
        artist.thumbUri().ifPresent(path -> container.addProperty(toArtistArt(artist)));
        container.setChildCount(childCount);
        return container;
    }

    public MusicArtist toArtist(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(ProcId.ARTIST.getValue() + ProcId.CID_SEPA + artist.id());
        container.setParentID(ProcId.ARTIST.getValue());
        container.setTitle(artist.name());
        container.setChildCount(artist.albumCount());
        artist.thumbUri().ifPresent(thumbUri -> container.addProperty(toArtistArt(artist)));
        return container;
    }

    public MusicArtist toArtist(ProcId procId, FolderArtist folderArtist, int childCount) {
        MusicArtist container = new MusicArtist();
        container.setId(procId.getValue() + ProcId.CID_SEPA + folderArtist.createCompositeId());
        container.setParentID(procId.getValue());
        container.setTitle(folderArtist.artist().name());
        container.setChildCount(folderArtist.artist().albumCount());
        if (folderArtist.artist().thumbUri() != null) {
            container.addProperty(toArtistArt(folderArtist.artist()));
        }
        return container;
    }

    public MusicAlbum toAlbum(MediaFile album, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.MEDIA_FILE.getValue() + ProcId.CID_SEPA + album.id());
        container.setParentID(ProcId.MEDIA_FILE.getValue() + ProcId.CID_SEPA + album.parentId());
        container.setChildCount(childCount);
        container.setTitle(album.name());
        container.addProperty(toPerson(album.artist()));
        container.addProperty(toAlbumArt(album));
        container.setDescription(album.comment());
        return container;
    }

    public MusicAlbum toAlbum(Album album) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.ALBUM_ID3.getValue() + ProcId.CID_SEPA + album.id());
        container.setParentID(ProcId.ALBUM_ID3.getValue());
        container.setTitle(album.name());
        container.setChildCount(album.songCount());
        container.addProperty(toAlbumArt(album));
        album.artist().ifPresent(artist -> container.addProperty(toPerson(artist)));
        container.setDescription(album.comment());
        return container;
    }

    public MusicAlbum toAlbum(ProcId procId, FolderAlbum folderAlbum, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container.setId(procId.getValue() + ProcId.CID_SEPA + folderAlbum.createCompositeId());
        container.setParentID(procId.getValue());
        container.setTitle(folderAlbum.album().name());
        container.setChildCount(childCount);
        container.addProperty(toAlbumArt(folderAlbum.album()));
        folderAlbum.album().artist().ifPresent(artist -> container.addProperty(toPerson(artist)));
        container.setDescription(folderAlbum.album().comment());
        return container;
    }

    public MusicAlbum toAlbum(PodcastChannel channel, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container.setId(ProcId.PODCAST.getValue() + ProcId.CID_SEPA + channel.id());
        container.setParentID(ProcId.PODCAST.getValue());
        container.setTitle(channel.title());
        container.setChildCount(childCount);
        channel.thumbUri().ifPresent(thumbUri -> container.addProperty(toPodcastArt(channel)));
        return container;
    }

    public MusicAlbum toAlbum(FolderGenreAlbum composite, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container
            .setId(ProcId.ALBUM_ID3_BY_FOLDER_GENRE.getValue() + ProcId.CID_SEPA
                    + composite.createCompositeId());
        container.setParentID(ProcId.ALBUM_ID3_BY_GENRE.getValue());
        container.setTitle(composite.album().name());
        container.setChildCount(childCount);
        container.addProperty(toAlbumArt(composite.album()));
        composite.album().artist().ifPresent(artist -> container.addProperty(toPerson(artist)));
        container.setDescription(composite.album().comment());
        return container;
    }

    public MusicAlbum toAlbumWithGenre(GenreAlbum composite, int childCount) {
        MusicAlbum container = new MusicAlbum();
        container
            .setId(ProcId.ALBUM_ID3_BY_GENRE.getValue() + ProcId.CID_SEPA
                    + composite.createCompositeId());
        container.setParentID(ProcId.ALBUM_ID3_BY_GENRE.getValue());
        container.setTitle(composite.album().name());
        container.setChildCount(childCount);
        container.addProperty(toAlbumArt(composite.album()));
        composite.album().artist().ifPresent(artist -> container.addProperty(toPerson(artist)));
        container.setDescription(composite.album().comment());
        return container;
    }

    @Nullable
    String getPreferredTargetFormat() {
        PreferredFormatScheme formatSheme = PreferredFormatScheme
            .of(settingsFacade.get(SKeys.transcoding.preferredFormatShemeName));
        return switch (formatSheme) {
        case ANNOYMOUS, OTHER_THAN_REQUEST -> settingsFacade.get(SKeys.transcoding.preferredFormat);
        case REQUEST_ONLY -> null;
        };
    }

    Res toRes(MediaFile mediaFile) {
        Player player = playerProvider.getUPnPPlayer();
        ResolvedAudioTranscodingParameters parameters = transcodingParametersPlanner
            .resolveAudioTranscodingParameters(player, mediaFile, null, getPreferredTargetFormat());

        Format format = parameters.outputFormat();
        String payload = upnpPayloadCodec
            .encodeStream(mediaFile.id(),
                    mediaFile.isVideo() ? StreamType.MOVIE : StreamType.MUSIC);
        String fileName = format == Format.UNDEFINED ? payload : payload + "." + format.value();
        String resourceUri = UriComponentsBuilder
            .fromUriString(getBaseUrl() + "/ext/upnp/stream/" + fileName)
            .toUriString();

        MimeType mimeType = MimeType.valueOf(parameters.outputMime());
        Res res = new Res(mimeType, null, resourceUri);
        res.setDuration(formatDuration(mediaFile.durationSeconds()));
        return res;
    }

    public MusicTrack toMusicTrack(MediaFile song) {

        MusicTrack item = new MusicTrack();

        item.setId(String.valueOf(song.id()));
        item.setTitle(song.title());
        item.setAlbum(song.album());
        if (song.artist() != null) {
            item.addProperty(toPerson(song.artist()));
        }
        song.year().ifPresent(year -> item.setDate(year.getValue() + "-01-01"));
        song.trackNumber().ifPresent(item::setOriginalTrackNumber);
        if (song.genre() != null) {
            item.setGenres(new String[] { song.genre() });
        }
        item.setResources(Arrays.asList(toRes(song)));
        item.setDescription(song.comment());
        song.parentId().ifPresent(parentId -> {
            MediaFile parent = mediaFileProvider.requireMediaFile(parentId);
            item.setParentID(String.valueOf(parent.id()));
            item.addProperty(toAlbumArt(parent));
        });
        return item;
    }

    public MusicTrack toMusicTrack(PodcastEpisode episode, @NonNull PodcastChannel channel) {
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.setId(String.valueOf(episode.id()));
        musicTrack.setTitle(episode.title());
        musicTrack.setParentID(String.valueOf(episode.channelId()));
        musicTrack.setAlbum(channel.title());
        channel.thumbUri().ifPresent(thumbUri -> musicTrack.addProperty(toPodcastArt(channel)));
        episode
            .publishDate()
            .ifPresent(publishDate -> musicTrack.setDate(DATE_FORMAT.get().format(publishDate)));
        episode.path().ifPresent(path -> {
            if (episode.episodePhase() == EpisodePhase.COMPLETED) {
                MediaFile song = mediaFileProvider.requireMediaFile(path);
                musicTrack.setResources(Arrays.asList(toRes(song)));
            }
        });
        return musicTrack;
    }

    public VideoItem toVideo(MediaFile video) {
        VideoItem videoItem = new VideoItem();
        videoItem.setId(String.valueOf(video.id()));
        videoItem.setTitle(video.title());
        videoItem.setResources(Arrays.asList(toRes(video)));
        videoItem.setDescription(video.comment());
        video.parentId().ifPresent(parentId -> {
            MediaFile parent = mediaFileProvider.requireMediaFile(parentId);
            videoItem.setParentID(String.valueOf(parent.id()));
            videoItem.addProperty(toAlbumArt(parent));
        });
        if (video.genre() != null) {
            videoItem.setGenres(new String[] { video.genre() });
        }
        videoItem.setCreator(video.artist());
        if (video.composer() != null) {
            videoItem.addProperty(toComposer(video.composer()));
        }
        return videoItem;
    }
}
