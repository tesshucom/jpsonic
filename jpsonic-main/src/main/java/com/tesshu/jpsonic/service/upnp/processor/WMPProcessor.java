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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.service.JMediaFileService;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP.AUTHOR;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class WMPProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(WMPProcessor.class);

    private static final BrowseResult EMPTY = new BrowseResult(StringUtils.EMPTY, 0, 0L, 0L);

    // When crawling folderPath
    private static final String MS_FILTER_FOLDER_PATH = "dc:title,microsoft:folderPath";
    // I haven't seen it used by anyone other than WMP so far, but ...
    private static final String MS_FILTER_ALL = "*";

    // When crawling a specific class object
    private static final String MS_QUERY_PLAYLIST_CONTAINER_ALL = "upnp:class derivedfrom \"object.container.playlistContainer\" and @refID exists false";
    private static final String MS_QUERY_AUDIO_ITEM_ALL = "upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false";
    private static final String MS_QUERY_VIDEO_ITEM_ALL = "upnp:class derivedfrom \"object.item.videoItem\" and @refID exists false";
    private static final String MS_QUERY_IMAGE_ITEM_ALL = "upnp:class derivedfrom \"object.item.imageItem\" and @refID exists false";

    /*
     * Issued when displaying a right-click popup in WMP. No matter what you click on, artist, album, etc., it will ask
     * for the ID of the first audio item in the container. Probably a preparatory movement for "play". You can play it
     * even if you ignore this message because it is only synchronized.
     */
    private static final Pattern MS_QUERY_AUDIO_ITEM_SINGLE = Pattern.compile("dc:title = \"[0-9]+\"");

    private final JMediaFileService mediaFileService;
    private final MediaFileUpnpProcessor mediaFileUpnpProcessor;
    private final UpnpProcessorUtil util;

    public WMPProcessor(JMediaFileService mediaFileService, MediaFileUpnpProcessor mediaFileUpnpProcessor,
            UpnpProcessorUtil util) {
        super();
        this.mediaFileService = mediaFileService;
        this.mediaFileUpnpProcessor = mediaFileUpnpProcessor;
        this.util = util;
    }

    public boolean isAvailable(String filter) {
        return MS_FILTER_FOLDER_PATH.equals(filter) || MS_FILTER_ALL.equals(filter);
    }

    public BrowseResult getBrowseResult(@NonNull String query, @Nullable String filter, long count, long offset) {
        if (MS_FILTER_FOLDER_PATH.equals(filter)) {
            return getFolderPaths(query, filter, count, offset);
        } else if (MS_FILTER_ALL.equals(filter)) {
            return getSomthing(query, count, offset);
        }
        return EMPTY;
    }

    final BrowseResult getFolderPaths(String query, String filter, long count, long offset) {
        if (MS_QUERY_PLAYLIST_CONTAINER_ALL.equals(query)) {
            return EMPTY; // no spport
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unexpected query: filter={}, {}", filter, query);
        }
        return EMPTY;
    }

    /*
     * These specifications depend on the WMP view and UPnP model spec.
     */
    MusicTrack createMusicTrack(MediaFile song) {
        MusicTrack item = new MusicTrack();
        item.setId(String.valueOf(song.getId()));
        item.setTitle(song.getTitle());
        item.setOriginalTrackNumber(song.getTrackNumber());
        item.setResources(Arrays.asList(mediaFileUpnpProcessor.createResourceForSong(song)));
        item.setDescription(song.getComment());

        MediaFile parent = mediaFileService.getParentOf(song);
        if (!ObjectUtils.isEmpty(parent)) {
            item.setParentID(String.valueOf(parent.getId()));
        }
        item.addProperty(new ALBUM_ART_URI(mediaFileUpnpProcessor.createAlbumArtURI(parent)));

        // Multi-artist is probably difficult with MS specs
        if (song.getAlbumArtist() != null) {
            item.setArtists(new PersonWithRole[] { new PersonWithRole(song.getAlbumArtist()) });
        }
        item.setAlbum(song.getAlbumName());
        if (song.getGenre() != null) {
            item.setGenres(new String[] { song.getGenre() });
        }
        if (song.getYear() != null) {
            item.setDate(song.getYear() + "-01-01");
        }
        item.setCreator(song.getArtist());
        if (song.getComposer() != null) {
            item.addProperty(new AUTHOR(new PersonWithRole(song.getComposer(), "composer")));
        }
        return item;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // DIDLParser#generate
    private BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws ExecutionException {
        String result;
        try {
            result = new DIDLParser().generate(didl);
        } catch (Exception e) {
            throw new ExecutionException("Unable to generate XML representation of content model.", e);
        }
        return new BrowseResult(result, count, totalMatches);
    }

    private BrowseResult createAudioItemBrowseResult(long count, long offset) {
        if (offset == 0) {
            LOG.info("object.item.audioItem data crawling started.");
        }
        List<MusicFolder> folders = util.getGuestMusicFolders();
        List<MediaFile> songs = mediaFileService.getSongs(count, offset, folders);
        DIDLContent didl = new DIDLContent();
        songs.forEach(song -> didl.addItem(createMusicTrack(song)));
        long total = mediaFileService.countSongs(folders);
        if (LOG.isInfoEnabled() && offset % 1000 == 0 || count != songs.size() || offset + songs.size() == total) {
            LOG.info(offset + "-" + (offset + songs.size()) + "/" + total + "("
                    + Math.round((float) (offset + songs.size()) / (float) total * 100) + "%)");
        }
        try {
            return createBrowseResult(didl, (int) didl.getCount(), (int) total);
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            return EMPTY;
        }
    }

    private BrowseResult createSingleObjectBrowseResult(String query) {
        int id = Integer.parseInt(query.replaceAll("[^0-9]", ""));
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile.isAudio()) {
            DIDLContent didl = new DIDLContent();
            didl.addItem(createMusicTrack(mediaFile));
            try {
                return createBrowseResult(didl, (int) didl.getCount(), 1);
            } catch (ExecutionException e) {
                ConcurrentUtils.handleCauseUnchecked(e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unexpected ogject: filter={}, query={}", MS_FILTER_ALL, query);
        }
        return EMPTY;
    }

    final BrowseResult getSomthing(String query, long count, long offset) {
        if (MS_QUERY_AUDIO_ITEM_ALL.equals(query)) {
            return createAudioItemBrowseResult(count, offset);
        } else if (MS_QUERY_VIDEO_ITEM_ALL.equals(query)) {
            return EMPTY; // object.item.videoItem has not been implemented yet.
        } else if (MS_QUERY_IMAGE_ITEM_ALL.equals(query)) {
            return EMPTY; // no spport
        } else if (MS_QUERY_AUDIO_ITEM_SINGLE.matcher(query).matches()) {
            return createSingleObjectBrowseResult(query);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Unexpected query: filter={}, query={}", MS_FILTER_ALL, query);
        }
        return EMPTY;
    }
}
