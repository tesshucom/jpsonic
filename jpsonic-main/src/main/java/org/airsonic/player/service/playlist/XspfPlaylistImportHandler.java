package org.airsonic.player.service.playlist;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.xspf.Location;
import chameleon.playlist.xspf.Playlist;
import chameleon.playlist.xspf.StringContainer;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFileService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class XspfPlaylistImportHandler implements PlaylistImportHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(XspfPlaylistImportHandler.class);

    @Autowired
    MediaFileService mediaFileService;

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
                    } catch (URISyntaxException e) {
                        LOG.error("Unable to generate URI.", e);
                    }
                    if (mediaFile == null) {
                        try {
                            File file = new File(sc.getText());
                            mediaFile = mediaFileService.getMediaFile(file);
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (mediaFile != null) {
                mediaFiles.add(mediaFile);
            } else {
                StringBuilder errorMsg = new StringBuilder("Could not find media file matching ");
                try {
                    errorMsg.append(track.getStringContainers().stream().map(StringContainer::getText).collect(Collectors.joining(",")));
                } catch (Exception e) {
                    LOG.error(errorMsg.toString(), e);
                }
                errors.add(errorMsg.toString());
            }
        });
        return Pair.of(mediaFiles, errors);
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
