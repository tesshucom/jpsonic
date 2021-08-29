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

package com.tesshu.jpsonic.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.tesshu.jpsonic.AbstractNeedsScan;
import com.tesshu.jpsonic.TestCaseUtils;
import com.tesshu.jpsonic.dao.MediaFileDao;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MusicFolder;
import com.tesshu.jpsonic.domain.SearchResult;
import com.tesshu.jpsonic.service.search.IndexType;
import com.tesshu.jpsonic.service.search.SearchCriteriaDirector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * getChildrenOf Integration Test. This method changes the records of repository without scanning.
 * Note that many properties are overwritten in real time.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MediaScannerServiceGetChildrenOfTest extends AbstractNeedsScan {

    private List<MusicFolder> musicFolders;
    private File artist;
    private File album;
    private File song;

    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private SearchCriteriaDirector criteriaDirector;
    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        return musicFolders;
    }

    @BeforeEach
    public void setup(@TempDir File tempDir) throws IOException, URISyntaxException {

        // Create a musicfolder for verification
        File musicFolder = new File(tempDir.getPath());
        artist = new File(musicFolder, "ARTIST");
        assertTrue(artist.mkdirs());
        this.album = new File(artist, "ALBUM");
        assertTrue(album.mkdirs());
        this.musicFolders = Arrays.asList(new MusicFolder(1, musicFolder, "musicFolder", true, new Date()));

        // Copy the song file from the test resource. No tags are registered in this file.
        File sample = new File(MediaScannerServiceGetChildrenOfTest.class
                .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sample.mp3").toURI());
        this.song = new File(this.album, "sample.mp3");
        Files.copy(sample.toPath(), song.toPath());
        assertTrue(song.exists());

        // Exec scan
        populateDatabaseOnlyOnce();
    }

    @Test
    void testSpecialCharactersInDirName() throws URISyntaxException, IOException, InterruptedException {

        MediaFile artist = mediaFileDao.getMediaFile(this.artist.getPath());
        assertEquals(this.artist.getPath(), artist.getPath());
        assertEquals("ARTIST", artist.getName());
        MediaFile album = mediaFileDao.getMediaFile(this.album.getPath());
        assertEquals(this.album.getPath(), album.getPath());
        assertEquals("ALBUM", album.getName());
        MediaFile song = mediaFileDao.getMediaFile(this.song.getPath());
        assertEquals(this.song.getPath(), song.getPath());
        assertEquals("ARTIST", song.getArtist());
        assertEquals("ALBUM", song.getAlbumName());

        // Copy the song file from the test resource. Tags are registered in this file.
        assertTrue(this.song.delete());
        File sampleEdited = new File(MediaScannerServiceGetChildrenOfTest.class
                .getResource("/MEDIAS/Scan/Timestamp/ARTIST/ALBUM/sampleEdited.mp3").toURI());
        this.song = new File(this.album, "sample.mp3");
        Files.copy(sampleEdited.toPath(), this.song.toPath());
        assertTrue(song.exists());

        /*
         * If you get it via Dao, you can get the record before had been copied. (It's a expected behavior)
         */
        List<MediaFile> albums = mediaFileDao.getChildrenOf(this.artist.getPath());
        assertEquals(1, albums.size());
        List<MediaFile> songs = mediaFileDao.getChildrenOf(this.album.getPath());
        assertEquals(1, songs.size());
        song = songs.get(0);
        assertEquals(this.song.getPath(), song.getPath());
        assertEquals("ARTIST", song.getArtist());
        assertEquals("ALBUM", song.getAlbumName());

        /*
         * Doing the same thing over the service does not give the same result. This is because the timestamp is
         * checked, and if the file is found to change, it will be parsed and the result will be saved in storage. Note
         * that the naming is get, but the actual processing is get & Update.
         */
        albums = mediaFileService.getChildrenOf(artist, true, true, false, false);
        assertEquals(1, albums.size());
        songs = mediaFileService.getChildrenOf(album, true, true, false, false);
        assertEquals(1, songs.size());

        /*
         * Note that the update process at this point is partial. Pay attention to the consistency.
         */

        // Artist and Album are not subject to the update process
        artist = mediaFileDao.getMediaFile(this.artist.getPath());
        assertEquals(this.artist.getPath(), artist.getPath());
        assertEquals("ARTIST", artist.getName());
        album = mediaFileDao.getMediaFile(this.album.getPath());
        assertEquals(this.album.getPath(), album.getPath());
        assertEquals("ALBUM", album.getName());

        // Only songs are updated
        song = mediaFileDao.getMediaFile(this.song.getPath());
        assertEquals(this.song.getPath(), song.getPath());
        assertEquals("Edited artist!", song.getArtist());
        assertEquals("Edited album!", song.getAlbumName());

        /*
         * Not reflected in the search at this point. (It's a expected behavior)
         */
        SearchResult result = searchService.search(
                criteriaDirector.construct("Edited", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
        assertEquals(0, result.getMediaFiles().size());

        result = searchService.search(
                criteriaDirector.construct("sample", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
        assertEquals(1, result.getMediaFiles().size());
        result = searchService.search(
                criteriaDirector.construct("ALBUM", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ALBUM));
        assertEquals(1, result.getMediaFiles().size());
        result = searchService.search(
                criteriaDirector.construct("ARTIST", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ARTIST));
        assertEquals(1, result.getMediaFiles().size());

        // Exec scan
        TestCaseUtils.execScan(mediaScannerService);
        // Await for Lucene to finish writing(asynchronous).
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
        }

        artist = mediaFileDao.getMediaFile(this.artist.getPath());
        assertEquals(this.artist.getPath(), artist.getPath());
        assertNull(artist.getTitle());
        assertEquals("ARTIST", artist.getName());
        assertEquals("ARTIST", artist.getArtist());
        album = mediaFileDao.getMediaFile(this.album.getPath());
        assertEquals(this.album.getPath(), album.getPath());
        assertNull(album.getTitle());
        assertEquals("ALBUM", album.getName());
        assertEquals("Edited album!", album.getAlbumName());

        song = mediaFileDao.getMediaFile(this.song.getPath());
        assertEquals(this.song.getPath(), song.getPath());
        assertEquals("Edited song!", song.getTitle());
        assertEquals("Edited song!", song.getName());
        assertEquals("Edited artist!", song.getArtist());
        assertEquals("Edited album!", song.getAlbumName());

        result = searchService.search(
                criteriaDirector.construct("sample", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
        assertEquals(0, result.getMediaFiles().size()); // good (1 -> 0)
        result = searchService.search(
                criteriaDirector.construct("Edited song!", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.SONG));
        assertEquals(1, result.getMediaFiles().size()); // good (0 -> 1)

        result = searchService.search(criteriaDirector.construct("Edited album!", 0, Integer.MAX_VALUE, false,
                musicFolders, IndexType.ALBUM));
        assertEquals(1, result.getMediaFiles().size()); // good (0 -> 1)

        /*
         * Not reflected in the artist of file structure. (It's a expected behavior)
         */
        result = searchService.search(criteriaDirector.construct("Edited artist!", 0, Integer.MAX_VALUE, false,
                musicFolders, IndexType.ARTIST));
        assertEquals(0, result.getMediaFiles().size()); // good
        result = searchService.search(
                criteriaDirector.construct("ARTIST", 0, Integer.MAX_VALUE, false, musicFolders, IndexType.ARTIST));
        assertEquals(1, result.getMediaFiles().size()); // good (1 -> 1)

        result = searchService.search(criteriaDirector.construct("Edited album!", 0, Integer.MAX_VALUE, false,
                musicFolders, IndexType.ALBUM_ID3));
        assertEquals(1, result.getAlbums().size());
        result = searchService.search(criteriaDirector.construct("Edited artist!", 0, Integer.MAX_VALUE, false,
                musicFolders, IndexType.ARTIST_ID3));
        assertEquals(1, result.getArtists().size());
    }
}
