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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.playlist;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.xspf.Location;
import chameleon.playlist.xspf.Playlist;
import chameleon.playlist.xspf.StringContainer;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.MediaFileService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class XspfPlaylistImportHandler implements PlaylistImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(XspfPlaylistImportHandler.class);

    private final MediaFileService mediaFileService;

    public XspfPlaylistImportHandler(MediaFileService mediaFileService) {
        super();
        this.mediaFileService = mediaFileService;
    }

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return Playlist.class.equals(playlistClass);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (File) Not reusable
    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Playlist xspfPlaylist = (Playlist) inputSpecificPlaylist;
        xspfPlaylist.getTracks().forEach(track -> {
            MediaFile mediaFile = null;
            for (StringContainer sc : track.getStringContainers()) {
                if (sc instanceof Location) {
                    Location location = (Location) sc;
                    try {
                        File file = new File(new URI(location.getText()));
                        mediaFile = mediaFileService.getMediaFile(file);
                        if (mediaFile == null && sc.getText() != null) {
                            mediaFile = mediaFileService.getMediaFile(new File(sc.getText()));
                        }
                    } catch (URISyntaxException e) {
                        LOG.error("Unable to generate URI.", e);
                    } catch (SecurityException e) {
                        if (LOG.isTraceEnabled()) {
                            LOG.error("SecurityException will be ignored.", e);
                        }
                    }
                }
            }
            if (mediaFile == null) {
                StringBuilder errorMsg = new StringBuilder("Could not find media file matching ");
                errorMsg.append(track.getStringContainers().stream().map(StringContainer::getText)
                        .collect(Collectors.joining(",")));
                errors.add(errorMsg.toString());
            } else {
                mediaFiles.add(mediaFile);
            }
        });
        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
