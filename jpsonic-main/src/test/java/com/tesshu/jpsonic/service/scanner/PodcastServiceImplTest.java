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

package com.tesshu.jpsonic.service.scanner;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.domain.PodcastChannel;
import com.tesshu.jpsonic.domain.PodcastEpisode;
import com.tesshu.jpsonic.service.MediaFileService;
import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals" })
class PodcastServiceImplTest {

    private SettingsService settingsService;
    private SecurityService securityService;
    private PodcastServiceImpl podcastService;

    @BeforeEach
    public void setup() throws ExecutionException {
        settingsService = mock(SettingsService.class);
        securityService = mock(SecurityService.class);
        MediaFileService mediaFlieService = new MediaFileService(settingsService, null, null, null, null, null);
        podcastService = new PodcastServiceImpl(null, settingsService, securityService, mediaFlieService,
                mock(WritableMediaFileService.class), null, null, null, null, null);
    }

    private ZonedDateTime toJST(String date) {
        // Parse and return in Japan Standard Time
        // "Japan" is a value for testing, actually systemDefault is used in Jpsonic.
        return ZonedDateTime.ofInstant(podcastService.parseDate(date), ZoneId.of("Japan"));
    }

    @Test
    void testParseDate() {

        assertNull(podcastService.parseDate(null)); // null
        assertNull(podcastService.parseDate("09 Sep 2022 08:44:00 +0000")); // no days of the week
        assertNull(podcastService.parseDate("Fri, 09 Sep 2022 08:49:03")); // no zone
        assertNull(podcastService.parseDate("Fri, 09 Sep 2022 08:45 +0000")); // no seconds

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        assertEquals("2022-09-09 17:41:00", fmt.format(toJST("Fri, 09 Sep 2022 08:41:00 +0000")));
        assertEquals("2022-09-09 16:42:00", fmt.format(toJST("Fri, 09 Sep 2022 08:42:00 +0100")));
        assertEquals("2022-09-09 15:43:00", fmt.format(toJST("Fri, 09 Sep 2022 08:43:00 +0200")));
        assertEquals("2022-09-09 08:46:00", fmt.format(toJST("Fri, 09 Sep 2022 08:46:00 JST")));
        assertEquals("2022-09-09 08:47:01", fmt.format(toJST("Fri, 09 Sep 2022 08:47:01 ROK")));
        assertEquals("2022-09-09 22:48:02", fmt.format(toJST("Fri, 09 Sep 2022 08:48:02 CDT")));
        assertEquals("2022-09-09 21:49:03", fmt.format(toJST("Fri, 09 Sep 2022 08:49:03 EST")));

        // Accept non zero-fill-numeric values
        assertEquals("2022-09-09 08:07:05", fmt.format(toJST("Fri, 9 Sep 2022 8:7:5 JST")));
    }

    @Test
    void testFormatDuration() {

        /*
         * itunes:duration - Duration of the episode, in one of the following formats: 1:10:00, 10:00, 1800. In the
         * first two formats the values for hours, minutes, or seconds cannot exceed two digits each.
         */
        assertNull(podcastService.formatDuration(null));
        assertEquals("1:10:00", podcastService.formatDuration("1:10:00"));
        assertEquals("10:00", podcastService.formatDuration("10:00"));
        assertEquals("0:59", podcastService.formatDuration("59"));
        assertEquals("1:00", podcastService.formatDuration("60"));
        assertEquals("59:59", podcastService.formatDuration("3599"));
        assertEquals("1:00:00", podcastService.formatDuration("3600"));
    }

    @Test
    void testIsAudioEpisode() {

        Mockito.when(settingsService.getMusicFileTypesAsArray()).thenReturn(Arrays.asList(
                "mp3 ogg oga aac m4a m4b flac wav wma aif aiff aifc ape mpc shn mka opus dsf dsd".split("\\s+")));

        assertTrue(podcastService.isAudioEpisode("http://tesshu.com/episode.mp3"));
        assertTrue(podcastService.isAudioEpisode("http://tesshu.com/episode.m4a"));
        assertTrue(podcastService.isAudioEpisode("http://tesshu.com/episode.ogg"));
        assertTrue(podcastService.isAudioEpisode("http://tesshu.com/episode.opus"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/episode.oma"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/episode.exe"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/episode.sh"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/withoutExtenssion"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/withoutExtenssion/"));
        assertTrue(podcastService.isAudioEpisode("http://tesshu.com/episode.mp3?size=mid"));
        assertFalse(podcastService.isAudioEpisode("http://tesshu.com/episode.sh?size=mid"));
    }

    @Nested
    class GetFiletTest {

        // https://github.com/tesshucom/jpsonic/pull/2281
        @Test
        void testExtensionAndDot() throws URISyntaxException {

            Path podcastFolder = Path.of(PodcastServiceImplTest.class.getResource("/MEDIAS/Podcast").toURI());
            Mockito.when(settingsService.getPodcastFolder()).thenReturn(podcastFolder.toString());
            Mockito.when(securityService.isWriteAllowed(Mockito.any(Path.class))).thenReturn(true);

            final String channelTitle = "chTitle";
            final PodcastChannel channel = new PodcastChannel(null, null, channelTitle, null, null, null, null);
            final int epId = 99;
            final Instant publishDate = Instant.now();
            final String pubDateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
                    .format(publishDate);

            String episodeTitle = "epTitle";
            String episodeUrl = "http://tesshu.com/chTitle/epTitle.mp3";

            PodcastEpisode episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate,
                    null, null, null, null, null);
            String fileName = channel.getTitle() + " - " + pubDateStr + " - " + epId + " - " + episodeTitle + ".mp3";
            assertEquals(podcastFolder.toString() + File.separator + channelTitle + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());

            episodeUrl = "http://tesshu.com/chTitle/epTitle.m4a";

            episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate, null, null,
                    null, null, null);
            fileName = channel.getTitle() + " - " + pubDateStr + " - " + epId + " - " + episodeTitle + ".m4a";
            assertEquals(podcastFolder.toString() + File.separator + channelTitle + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());

            episodeUrl = "http://tesshu.com/Star+Wars/Star+Wars+Ep.1.mp3";
            episodeTitle = "Star Wars Ep.1";

            episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate, null, null,
                    null, null, null);
            fileName = channel.getTitle() + " - " + pubDateStr + " - " + epId + " - Star Wars Ep.1.mp3";
            assertEquals(podcastFolder.toString() + File.separator + channelTitle + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());
        }

        // https://github.com/tesshucom/jpsonic/pull/2492
        // If the title ends with Dot, it will be replaced with a hyphen when creating the filename.
        @Test
        void testDotAtTheEnd() throws URISyntaxException {

            Path podcastFolder = Path.of(PodcastServiceImplTest.class.getResource("/MEDIAS/Podcast").toURI());
            Mockito.when(settingsService.getPodcastFolder()).thenReturn(podcastFolder.toString());
            Mockito.when(securityService.isWriteAllowed(Mockito.any(Path.class))).thenReturn(true);

            final String channelTitle = "chTitleIf...";
            final PodcastChannel channel = new PodcastChannel(null, null, channelTitle, null, null, null, null);
            final int epId = 99;
            final Instant publishDate = Instant.now();
            final String pubDateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
                    .format(publishDate);

            String episodeTitle = "epTitleIf...";
            String episodeUrl = "http://tesshu.com/chTitle/epTitle.mp3";

            PodcastEpisode episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate,
                    null, null, null, null, null);
            String fileName = "chTitleIf-." + " - " + pubDateStr + " - " + epId + " - " + "epTitleIf" + ".mp3";
            assertEquals(podcastFolder.toString() + File.separator + "chTitleIf" + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());

            episodeUrl = "http://tesshu.com/chTitleIf.../epTitleIf....m4a";

            episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate, null, null,
                    null, null, null);
            fileName = "chTitleIf-." + " - " + pubDateStr + " - " + epId + " - " + "epTitleIf" + ".m4a";
            assertEquals(podcastFolder.toString() + File.separator + "chTitleIf" + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());

            episodeUrl = "http://tesshu.com/Star+Wars/Star+Wars+Ep.1.mp3";
            episodeTitle = "Star Wars Ep.1";

            episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate, null, null,
                    null, null, null);
            fileName = "chTitleIf-." + " - " + pubDateStr + " - " + epId + " - Star Wars Ep.1.mp3";
            assertEquals(podcastFolder.toString() + File.separator + "chTitleIf" + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());
        }

        @Test
        void testEpisodeUrlWithQuery() throws URISyntaxException {

            Path podcastFolder = Path.of(PodcastServiceImplTest.class.getResource("/MEDIAS/Podcast").toURI());
            Mockito.when(settingsService.getPodcastFolder()).thenReturn(podcastFolder.toString());
            Mockito.when(securityService.isWriteAllowed(Mockito.any(Path.class))).thenReturn(true);

            final String channelTitle = "chTitle";
            final PodcastChannel channel = new PodcastChannel(null, null, channelTitle, null, null, null, null);
            final int epId = 99;
            final Instant publishDate = Instant.now();
            final String pubDateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
                    .format(publishDate);

            String episodeTitle = "epTitle";
            String episodeUrl = "http://tesshu.com/chTitle/epTitle.mp3?size=mid";

            PodcastEpisode episode = new PodcastEpisode(epId, null, episodeUrl, null, episodeTitle, null, publishDate,
                    null, null, null, null, null);
            String fileName = channel.getTitle() + " - " + pubDateStr + " - " + epId + " - " + episodeTitle + ".mp3";
            assertEquals(podcastFolder.toString() + File.separator + channelTitle + File.separator + fileName,
                    podcastService.getFile(channel, episode).toString());
        }
    }
}
