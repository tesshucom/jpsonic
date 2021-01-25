package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;

interface FolderArtistWrapper {

    Artist getArtist();

    MusicFolder getFolder();

    String getId();

    String getName();

    MediaFile getSong();

    boolean isArtist();
}