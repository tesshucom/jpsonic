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
import org.mockito.Mockito;

@SuppressWarnings("PMD.TooManyStaticImports")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MP4ParserTest {

    private TranscodingService transcodingService;
    private MP4Parser parser;

    @BeforeEach
    void setUp() {
        transcodingService = new TranscodingService(mock(SettingsService.class), null, null, null, null);
        parser = new MP4Parser(transcodingService);
    }

    private MediaFile createTestMediafile() throws URISyntaxException, IOException {
        MediaFile mediaFile = new MediaFile();
        File file = new File(
                MusicParserTest.class.getResource("/MEDIAS/Metadata/tagger3/tagged/test.stem.mp4").toURI());
        mediaFile.setPath(file.getAbsolutePath());
        mediaFile.setFileSize(Files.size(file.toPath()));
        return mediaFile;
    }

    /**
     * Except for BitRate, the analysis results of FFProbe and Tika are the same.
     */
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
    }

    @Test
    @Order(1)
    void testGetThreshold() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile();
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        long threshold = parser.getThreshold(mediaFile, statistics);
        assertEquals(MP4ParseStatistics.CMD_LEAD_TIME_DEFAULT * MP4ParseStatistics.TIKA_BPMS_DEFAULT, threshold);
    }

    @Test
    @Order(2)
    void testParseWithFFProbeNoCmd() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile();
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        parser.getThreshold(mediaFile, statistics);

        parser = new MP4Parser(mock(TranscodingService.class));

        MetaData metaData = parser.parseWithFFProbe(mediaFile, statistics);
        assertNull(metaData.getWidth());
        assertNull(metaData.getHeight());
        assertNull(metaData.getDurationSeconds());
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
        assertNull(metaData.getBitRate());
    }

    @Test
    @Order(3)
    void testParseWithFFProbe() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile();
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        parser.getThreshold(mediaFile, statistics);
        MetaData metaData = parser.parseWithFFProbe(mediaFile, statistics);
        assertTagsWrittenByMp3tag(metaData);

        assertEquals(226, metaData.getBitRate()); // FFProbe Only!
    }

    @Test
    @Order(4)
    void testParseWithTika() throws URISyntaxException, IOException {
        MediaFile mediaFile = createTestMediafile();
        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        parser.getThreshold(mediaFile, statistics);
        MetaData metaData = parser.parseWithTika(mediaFile, statistics);
        assertTagsWrittenByMp3tag(metaData);

        assertNull(metaData.getBitRate()); // None!
    }

    @Test
    @Order(5)
    void testGetRawMetaData() throws URISyntaxException, IOException {

        transcodingService = mock(TranscodingService.class);
        parser = new MP4Parser(transcodingService);
        MediaFile mediaFile = createTestMediafile();
        parser.getRawMetaData(mediaFile);
        // If the argument is only mediaFile, FFProbe is used
        Mockito.verify(transcodingService, Mockito.times(2)).getTranscodeDirectory();
        Mockito.clearInvocations(transcodingService);

        Map<String, MP4ParseStatistics> statistics = new ConcurrentHashMap<>();
        assertThat(parser.getThreshold(mediaFile, statistics), greaterThan(mediaFile.getFileSize()));
        parser.getRawMetaData(mediaFile, statistics);
        // With statistics : FFProbe is not used for small files
        Mockito.verify(transcodingService, Mockito.never()).getTranscodeDirectory();

        statistics.clear();
        MP4ParseStatistics s = new MP4ParseStatistics();
        s.addTikaLeadTime(1000, 1_000_000_000);
        s.addTikaLeadTime(1000, 1_000_000_000);
        statistics.putIfAbsent("root", s);
        assertThat(mediaFile.getFileSize(), greaterThan(parser.getThreshold(mediaFile, statistics)));
        parser.getRawMetaData(mediaFile, statistics);
        // With statistics : FFProbe is used for big files
        Mockito.verify(transcodingService, Mockito.times(2)).getTranscodeDirectory();
    }
}
