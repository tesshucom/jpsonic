
package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.MediaFile;
import org.subsonic.restapi.IndexID3;

public interface Id3Wrapper {

    boolean isIndex();

    boolean isAlbum();

    boolean isArtist();

    boolean isSong();

    String getId();

    String getName();

    String getArtist();

    String getComment();

    IndexID3 getIndexId3();

    MediaFile getSong();

    int getChildSize();

}
