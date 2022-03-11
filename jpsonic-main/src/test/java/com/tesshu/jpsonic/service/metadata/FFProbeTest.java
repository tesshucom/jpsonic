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
 * (C) 2022 tesshucom
 */

package com.tesshu.jpsonic.service.metadata;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.TranscodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("PMD.TooManyStaticImports")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FFProbeTest {

    private FFProbe ffProbe;

    @BeforeEach
    void setUp() {
        TranscodingService transcodingService = new TranscodingService(mock(SettingsService.class), null, null, null,
                null);
        ffProbe = new FFProbe(transcodingService);
    }

    private MediaFile createTestMediafile(String path) throws URISyntaxException, IOException {
        MediaFile mediaFile = new MediaFile();
        File file = new File(MusicParserTest.class.getResource(path).toURI());
        mediaFile.setPath(file.getAbsolutePath());
        mediaFile.setFileSize(Files.size(file.toPath()));
        return mediaFile;
    }

    void assertEmpty(MetaData metaData) {
        assertNull(metaData.getWidth());
        assertNull(metaData.getHeight());
        assertNull(metaData.getAlbumArtist());
        assertNull(metaData.getAlbumName());
        assertNull(metaData.getArtist());
        assertNull(metaData.getDiscNumber());
        assertNull(metaData.getGenre());
        assertNull(metaData.getMusicBrainzRecordingId());
        assertNull(metaData.getMusicBrainzReleaseId());
        assertNull(metaData.getTitle());
        assertNull(metaData.getTrackNumber());
        assertNull(metaData.getYear());
        assertNull(metaData.getArtistSort());
        assertNull(metaData.getAlbumSort());
        assertNull(metaData.getTitleSort());
        assertNull(metaData.getAlbumArtistSort());
        assertNull(metaData.getComposer());
        assertNull(metaData.getComposerSort());
        assertNull(metaData.getDurationSeconds());
        assertNull(metaData.getBitRate());
    }

    void assertTagsWrittenByMp3tag(MetaData metaData) {
        assertEquals(1920, metaData.getWidth());
        assertEquals(1080, metaData.getHeight());
        assertEquals(13, metaData.getDurationSeconds());
        assertEquals("Mp3tag:AlbumArtist", metaData.getAlbumArtist());
        assertEquals("Mp3tag:Album", metaData.getAlbumName());
        assertEquals("Mp3tag:Artist", metaData.getArtist());
        assertEquals(Integer.valueOf(98), metaData.getDiscNumber());
        assertEquals("Mp3tag:Genre", metaData.getGenre());
        assertNull(metaData.getMusicBrainzRecordingId());
        assertNull(metaData.getMusicBrainzReleaseId());
        assertEquals("Mp3tag:Title", metaData.getTitle());
        assertEquals(Integer.valueOf(96), metaData.getTrackNumber());
        assertEquals(Integer.valueOf(2022), metaData.getYear());
        assertNull(metaData.getArtistSort());
        assertNull(metaData.getAlbumSort());
        assertNull(metaData.getTitleSort());
        assertNull(metaData.getAlbumArtistSort());
        assertEquals("Mp3tag:Composer", metaData.getComposer());
        assertNull(metaData.getComposerSort());
        assertEquals(226, metaData.getBitRate());
    }

    @Test
    @Order(1)
    void testParseWithoutCmd() throws URISyntaxException, IOException {
        ffProbe = new FFProbe(mock(TranscodingService.class));
        MediaFile mediaFile = createTestMediafile("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
        MetaData metaData = ffProbe.parse(mediaFile, null);
        assertEmpty(metaData);
    }

    @Test
    @Order(2)
    void testBlank() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile("/MEDIAS/Metadata/tagger3/blank/blank.mp4");
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        MetaData metaData = ffProbe.parse(mediaFile, statistics);
        assertEmpty(metaData);
    }

    @Test
    @Order(3)
    void testNoHeader() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile("/MEDIAS/Metadata/tagger3/noheader/empty.mp3");
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        MetaData metaData = ffProbe.parse(mediaFile, statistics);
        assertEmpty(metaData);
    }

    @Test
    @Order(4)
    void testIllegalFilePath() throws URISyntaxException, IOException {
        MediaFile mediaFile = new MediaFile();
        File file = new File("fake");
        mediaFile.setPath(file.getAbsolutePath());
        mediaFile.setFileSize(Long.valueOf(5_000));
        MetaData metaData = ffProbe.parse(mediaFile, null);
        assertEmpty(metaData);
    }

    @Test
    @Order(5)
    void testTagged() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        MetaData metaData = ffProbe.parse(mediaFile, statistics);
        assertTagsWrittenByMp3tag(metaData);
    }

    @Test
    @Order(5)
    void testTaggedWithStatistics() throws URISyntaxException, IOException {

        String folder = "/tagged";

        MediaFile mediaFile = createTestMediafile("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4");
        mediaFile.setFolder(folder);

        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        MP4ParseStatistics s = new MP4ParseStatistics();
        statistics.put(folder, s);

        MetaData metaData = ffProbe.parse(mediaFile, statistics);
        assertTagsWrittenByMp3tag(metaData);

        assertEquals(1, statistics.get(folder).leadTimeCmd.size());
        assertEquals(0, statistics.get(folder).leadTimeTika.size());

        assertThat(statistics.get(folder).leadTimeCmd.get(0), greaterThan(0L));
    }
}
