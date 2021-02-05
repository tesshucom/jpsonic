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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.search.AbstractAirsonicHomeTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

public class MediaScannerServiceUpdateAlbumTest extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private MediaFileDao mediaFileDao;

    @Autowired
    private AlbumDao albumDao;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (ObjectUtils.isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();
            File musicDir1 = new File(resolveBaseMediaPath.apply("Scan/Id3LIFO"));
            musicFolders.add(new MusicFolder(1, musicDir1, "alphaBeticalProps", true, new Date()));
            File musicDir2 = new File(resolveBaseMediaPath.apply("Scan/Null"));
            musicFolders.add(new MusicFolder(2, musicDir2, "noTagFirstChild", true, new Date()));
            File musicDir3 = new File(resolveBaseMediaPath.apply("Scan/Reverse"));
            musicFolders.add(new MusicFolder(3, musicDir3, "fileAndPropsNameInReverse", true, new Date()));
        }
        return musicFolders;
    }

    @Before
    public void setup() {
        populateDatabaseOnlyOnce();
    }

    /*
     * File structured albums are only affected by the first child of the system file path. FIFO. Only the last
     * registration is valid if the same name exists in the case of Id3 album. LIFO. Depending on the data pattern,
     * different album data of Genre can be created in File structure / Id3.
     * 
     * Jpsonic changes DB registration logic of Id3 album to eliminate data inconsistency.
     * 
     */
    @Test
    public void testUpdateAlbum() {

        // LIFO
        List<MusicFolder> folder = musicFolders.stream().filter(f -> "alphaBeticalProps".equals(f.getName()))
                .collect(Collectors.toList());
        List<MediaFile> albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
        assertEquals(1, albums.size());
        MediaFile album = albums.get(0);
        assertEquals("ALBUM1", album.getName());
        assertEquals("albumArtistA", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertEquals("genreA", album.getGenre());
        assertEquals(Integer.valueOf(2001), album.getYear());
        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        List<Album> albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
        assertEquals(1, albumId3s.size());
        Album albumId3 = albumId3s.get(0);
        assertEquals("albumA", albumId3.getName());
        assertEquals("albumArtistA", albumId3.getArtist());

        // [For legacy implementations, the following values ​​are inserted]
        // assertEquals("genreB", albumId3.getGenre());
        // assertEquals(Integer.valueOf(2002), albumId3.getYear());

        // [For Jpsonic, modified to insert the following values]
        // Other than this case, it is the same specification as the legacy.
        assertEquals("genreA", albumId3.getGenre());
        assertEquals(Integer.valueOf(2001), albumId3.getYear());

        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        // Null
        folder = musicFolders.stream().filter(f -> "noTagFirstChild".equals(f.getName())).collect(Collectors.toList());
        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
        assertEquals(1, albums.size());
        album = albums.get(0);
        assertEquals("ALBUM2", album.getName());
        assertEquals("ARTIST2", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertNull(album.getGenre());
        assertNull(album.getYear());
        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
        assertEquals(2, albumId3s.size());

        albumId3 = albumId3s.get(0);
        assertEquals("ALBUM2", albumId3.getName());
        assertEquals("ARTIST2", albumId3.getArtist());
        assertNull(albumId3.getGenre());
        assertNull(albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

        albumId3 = albumId3s.get(1);
        assertEquals("albumC", albumId3.getName());
        assertEquals("albumArtistC", albumId3.getArtist());
        assertEquals("genreC", albumId3.getGenre());
        assertEquals(Integer.valueOf(2002), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

        // Reverse
        folder = musicFolders.stream().filter(f -> "fileAndPropsNameInReverse".equals(f.getName()))
                .collect(Collectors.toList());
        albums = mediaFileDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, folder);
        assertEquals(1, albums.size());
        album = albums.get(0);
        assertEquals("ALBUM3", album.getName());
        assertEquals("albumArtistE", album.getArtist());
        assertNull(album.getAlbumArtist());
        assertEquals("genreE", album.getGenre());
        assertEquals(Integer.valueOf(2002), album.getYear());
        assertNull(album.getMusicBrainzReleaseId());
        assertNull(album.getMusicBrainzRecordingId());

        albumId3s = albumDao.getAlphabeticalAlbums(0, Integer.MAX_VALUE, false, false, folder);
        assertEquals(2, albumId3s.size());

        albumId3 = albumId3s.get(0);
        assertEquals("albumD", albumId3.getName());
        assertEquals("albumArtistD", albumId3.getArtist());
        assertEquals("genreD", albumId3.getGenre());
        assertEquals(Integer.valueOf(2001), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

        albumId3 = albumId3s.get(1);
        assertEquals("albumE", albumId3.getName());
        assertEquals("albumArtistE", albumId3.getArtist());
        assertEquals("genreE", albumId3.getGenre());
        assertEquals(Integer.valueOf(2002), albumId3.getYear());
        assertNull(albumId3.getMusicBrainzReleaseId());

    }

}
