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

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InternetRadioServiceTest {

    private static final String TEST_RADIO_NAME = "Test Radio";
    private static final String TEST_RADIO_HOMEPAGE = "http://example.com";
    private static final String TEST_PLAYLIST_URL_MOVE = "http://example.com/stream_move.m3u";
    private static final String TEST_PLAYLIST_URL_MOVE_LOOP = "http://example.com/stream_infinity_move.m3u";
    private static final String TEST_PLAYLIST_URL_LARGE = "http://example.com/stream_infinity_repeat.m3u";
    private static final String TEST_PLAYLIST_URL_LARGE_2 = "http://example.com/stream_infinity_big.m3u";
    private static final String TEST_PLAYLIST_URL_1 = "http://example.com/stream1.m3u";
    private static final String TEST_PLAYLIST_URL_2 = "http://example.com/stream2.m3u";
    private static final String TEST_STREAM_URL_1 = "http://example.com/stream1";
    private static final String TEST_STREAM_URL_2 = "http://example.com/stream2";
    private static final String TEST_STREAM_URL_3 = "http://example.com/stream3";
    private static final String TEST_STREAM_URL_4 = "http://example.com/stream4";
    private static final String TEST_STREAM_PLAYLIST_CONTENTS_1 = "http://example.com/stream1\n"
            + "http://example.com/stream2\n";
    private static final String TEST_STREAM_PLAYLIST_CONTENTS_2 = "#EXTM3U\n"
            + "#EXTINF:123, Sample artist - Sample title\n" + "http://example.com/stream3\n"
            + "#EXTINF:321,Example Artist - Example title\n" + "http://example.com/stream4\n";

    private InternetRadio radio1;
    private InternetRadio radioMove;
    private InternetRadio radioMoveLoop;
    private InternetRadio radioLarge;
    private InternetRadio radioLarge2;

    @Spy
    private InternetRadioService internetRadioService;

    @BeforeEach
    public void setup() throws ExecutionException {

        // Prepare a mock InternetRadio object
        radio1 = new InternetRadio(1, TEST_RADIO_NAME, TEST_PLAYLIST_URL_1, TEST_RADIO_HOMEPAGE, true, new Date());
        radioMove = new InternetRadio(3, TEST_RADIO_NAME, TEST_PLAYLIST_URL_MOVE, TEST_RADIO_HOMEPAGE, true,
                new Date());
        radioMoveLoop = new InternetRadio(3, TEST_RADIO_NAME, TEST_PLAYLIST_URL_MOVE_LOOP, TEST_RADIO_HOMEPAGE, true,
                new Date());
        radioLarge = new InternetRadio(4, TEST_RADIO_NAME, TEST_PLAYLIST_URL_LARGE, TEST_RADIO_HOMEPAGE, true,
                new Date());
        radioLarge2 = new InternetRadio(5, TEST_RADIO_NAME, TEST_PLAYLIST_URL_LARGE_2, TEST_RADIO_HOMEPAGE, true,
                new Date());

        // Prepare the mocked URL connection for the simple playlist
        HttpURLConnection mockURLConnection1 = Mockito.mock(HttpURLConnection.class);
        InputStream mockURLInputStream1 = new ByteArrayInputStream(TEST_STREAM_PLAYLIST_CONTENTS_1.getBytes());
        try {
            lenient().doReturn(mockURLInputStream1).when(mockURLConnection1).getInputStream();
            lenient().doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnection1).getResponseCode();

            // Prepare the mocked URL connection for the second simple playlist
            HttpURLConnection mockURLConnection2 = Mockito.mock(HttpURLConnection.class);
            InputStream mockURLInputStream2 = new ByteArrayInputStream(TEST_STREAM_PLAYLIST_CONTENTS_2.getBytes());
            lenient().doReturn(mockURLInputStream2).when(mockURLConnection2).getInputStream();
            lenient().doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnection2).getResponseCode();

            // Prepare the mocked URL connection for the redirection to simple playlist
            HttpURLConnection mockURLConnectionMove = Mockito.mock(HttpURLConnection.class);
            // InputStream mockURLInputStreamMove = new ByteArrayInputStream("".getBytes());
            lenient().doReturn(HttpURLConnection.HTTP_MOVED_PERM).when(mockURLConnectionMove).getResponseCode();
            lenient().doReturn(TEST_PLAYLIST_URL_2).when(mockURLConnectionMove).getHeaderField(eq("Location"));

            // Prepare the mocked URL connection for the redirection loop
            HttpURLConnection mockURLConnectionMoveLoop = Mockito.mock(HttpURLConnection.class);
            // InputStream mockURLInputStreamMoveLoop = new ByteArrayInputStream("".getBytes());
            lenient().doReturn(HttpURLConnection.HTTP_MOVED_PERM).when(mockURLConnectionMoveLoop).getResponseCode();
            lenient().doReturn(TEST_PLAYLIST_URL_MOVE_LOOP).when(mockURLConnectionMoveLoop)
                    .getHeaderField(eq("Location"));

            // Prepare the mocked URL connection for the 'content too large' test
            HttpURLConnection mockURLConnectionLarge = Mockito.mock(HttpURLConnection.class);
            try (InputStream mockURLInputStreamLarge = new InputStream() {
                private long pos;

                @Override
                public int read() {
                    return TEST_STREAM_PLAYLIST_CONTENTS_2
                            .charAt((int) (pos++ % TEST_STREAM_PLAYLIST_CONTENTS_2.length()));
                }
            }) {
                lenient().doReturn(mockURLInputStreamLarge).when(mockURLConnectionLarge).getInputStream();
            }
            lenient().doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnectionLarge).getResponseCode();

            // Prepare the mocked URL connection for the 'content too large' test
            // (return a single entry with 'aaaa...' running infinitely long).
            HttpURLConnection mockURLConnectionLarge2 = Mockito.mock(HttpURLConnection.class);
            try (InputStream mockURLInputStreamLarge2 = new InputStream() {
                // private long pos = 0;

                @Override
                public int read() {
                    return 0x41;
                }
            }) {
                lenient().doReturn(mockURLInputStreamLarge2).when(mockURLConnectionLarge2).getInputStream();
            }
            lenient().doReturn(HttpURLConnection.HTTP_OK).when(mockURLConnectionLarge2).getResponseCode();

            // Prepare the mock 'connectToURL' method
            lenient().doReturn(mockURLConnection1).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_1)));
            lenient().doReturn(mockURLConnection2).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_2)));
            lenient().doReturn(mockURLConnectionMove).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_MOVE)));
            lenient().doReturn(mockURLConnectionMoveLoop).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_MOVE_LOOP)));
            lenient().doReturn(mockURLConnectionLarge).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_LARGE)));
            lenient().doReturn(mockURLConnectionLarge2).when(internetRadioService)
                    .connectToURL(eq(new URL(TEST_PLAYLIST_URL_LARGE_2)));
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    @Test
    public void testParseSimplePlaylist() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radio1);
        assertEquals(2, radioSources.size());
        assertEquals(TEST_STREAM_URL_1, radioSources.get(0).getStreamUrl());
        assertEquals(TEST_STREAM_URL_2, radioSources.get(1).getStreamUrl());
    }

    @Test
    public void testRedirects() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioMove);
        assertEquals(2, radioSources.size());
        assertEquals(TEST_STREAM_URL_3, radioSources.get(0).getStreamUrl());
        assertEquals(TEST_STREAM_URL_4, radioSources.get(1).getStreamUrl());
    }

    @Test
    public void testLargeInput() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioLarge);
        // A PlaylistTooLarge exception is thrown internally, and the
        // `getInternetRadioSources` method logs it and returns a
        // limited number of sources.
        assertEquals(250, radioSources.size());
    }

    @Test
    public void testLargeInputURL() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioLarge2);
        // A PlaylistTooLarge exception is thrown internally, and the
        // `getInternetRadioSources` method logs it and returns a
        // limited number of bytes from the input.
        assertEquals(1, radioSources.size());
    }

    @Test
    public void testRedirectLoop() {
        List<InternetRadioSource> radioSources = internetRadioService.getInternetRadioSources(radioMoveLoop);
        // A PlaylistHasTooManyRedirects exception is thrown internally,
        // and the `getInternetRadioSources` method logs it and returns 0 sources.
        assertEquals(0, radioSources.size());
    }
}
