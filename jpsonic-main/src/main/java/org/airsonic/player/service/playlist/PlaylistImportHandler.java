
package org.airsonic.player.service.playlist;

import java.util.List;

import chameleon.playlist.SpecificPlaylist;
import org.airsonic.player.domain.MediaFile;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.Ordered;

public interface PlaylistImportHandler extends Ordered {
    boolean canHandle(Class<? extends SpecificPlaylist> playlistClass);

    Pair<List<MediaFile>, List<String>> handle(SpecificPlaylist inputSpecificPlaylist);
}
