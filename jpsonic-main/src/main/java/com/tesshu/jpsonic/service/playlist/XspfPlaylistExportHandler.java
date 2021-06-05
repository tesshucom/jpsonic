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

import java.util.Date;
import java.util.List;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistProvider;
import chameleon.playlist.xspf.Location;
import chameleon.playlist.xspf.Track;
import chameleon.playlist.xspf.XspfProvider;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.dao.PlaylistDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.Playlist;
import org.springframework.stereotype.Component;

@Component
public class XspfPlaylistExportHandler implements PlaylistExportHandler {

    private final MediaFileDao mediaFileDao;
    private final PlaylistDao playlistDao;

    public XspfPlaylistExportHandler(MediaFileDao mediaFileDao, PlaylistDao playlistDao) {
        super();
        this.mediaFileDao = mediaFileDao;
        this.playlistDao = playlistDao;
    }

    @Override
    public boolean canHandle(Class<? extends SpecificPlaylistProvider> providerClass) {
        return XspfProvider.class.equals(providerClass);
    }

    @Override
    public SpecificPlaylist handle(int id, SpecificPlaylistProvider provider) {
        return createXsfpPlaylistFromDBId(id);
    }

    private chameleon.playlist.xspf.Playlist createXsfpPlaylistFromDBId(int id) {
        chameleon.playlist.xspf.Playlist newPlaylist = new chameleon.playlist.xspf.Playlist();
        Playlist playlist = playlistDao.getPlaylist(id);
        newPlaylist.setTitle(playlist.getName());
        newPlaylist.setCreator("Airsonic user " + playlist.getUsername());
        newPlaylist.setDate(new Date());
        List<MediaFile> files = mediaFileDao.getFilesInPlaylist(id);

        files.stream().map(mediaFile -> {
            Track track = new Track();
            track.setTrackNumber(mediaFile.getTrackNumber());
            track.setCreator(mediaFile.getArtist());
            track.setTitle(mediaFile.getTitle());
            track.setAlbum(mediaFile.getAlbumName());
            track.setDuration(mediaFile.getDurationSeconds());
            track.setImage(mediaFile.getCoverArtPath());
            Location location = new Location();
            location.setText(mediaFile.getPath());
            track.getStringContainers().add(location);
            return track;
        }).forEach(newPlaylist::addTrack);

        return newPlaylist;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
