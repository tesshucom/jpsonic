package org.airsonic.player.service.upnp.processor;

import org.airsonic.player.domain.Album;

interface FolderArtistAlbumWrapper extends FolderArtistWrapper {

    Album getAlbum();

    boolean isAlbum();

}