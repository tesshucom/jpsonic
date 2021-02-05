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

package org.airsonic.player.service.playlist;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import chameleon.playlist.Media;
import chameleon.playlist.Parallel;
import chameleon.playlist.Playlist;
import chameleon.playlist.PlaylistVisitor;
import chameleon.playlist.Sequence;
import chameleon.playlist.SpecificPlaylist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class DefaultPlaylistImportHandler implements PlaylistImportHandler {

    @Autowired
    protected MediaFileService mediaFileService;

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylist> playlistClass) {
        return true;
    }

    @Override
    public Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist) {
        List<MediaFile> mediaFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            inputSpecificPlaylist.toPlaylist().acceptDown(new PlaylistVisitor() {
                @Override
                public void beginVisitPlaylist(Playlist playlist) {
                    // Nothing is currently done.
                }

                @Override
                public void endVisitPlaylist(Playlist playlist) {
                    // Nothing is currently done.
                }

                @Override
                public void beginVisitParallel(Parallel parallel) {
                    // Nothing is currently done.
                }

                @Override
                public void endVisitParallel(Parallel parallel) {
                    // Nothing is currently done.
                }

                @Override
                public void beginVisitSequence(Sequence sequence) {
                    // Nothing is currently done.
                }

                @Override
                public void endVisitSequence(Sequence sequence) {
                    // Nothing is currently done.
                }

                @Override
                public void beginVisitMedia(Media media) {
                    try {
                        URI uri = media.getSource().getURI();
                        File file = new File(uri);
                        MediaFile mediaFile = mediaFileService.getMediaFile(file);
                        if (mediaFile == null) {
                            errors.add("Cannot find media file " + file);
                        } else {
                            mediaFiles.add(mediaFile);
                        }
                    } catch (Exception e) {
                        errors.add(e.getMessage());
                    }
                }

                @Override
                public void endVisitMedia(Media media) {
                    // Nothing is currently done.
                }
            });
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
