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

package com.tesshu.jpsonic.ajax;

import static com.tesshu.jpsonic.util.XMLUtil.createSAXBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.util.StringUtil;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provides AJAX-enabled services for retrieving song lyrics from chartlyrics.com.
 * <p/>
 * See http://www.chartlyrics.com/api.aspx for details.
 * <p/>
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
@Service("ajaxLyricsService")
public class LyricsService {

    private static final Logger LOG = LoggerFactory.getLogger(LyricsService.class);

    /**
     * Returns lyrics for the given song and artist.
     *
     * @param artist
     *            The artist.
     * @param song
     *            The song.
     * 
     * @return The lyrics, never <code>null</code> .
     */
    public LyricsInfo getLyrics(final String artist, final String song) {
        LyricsInfo lyrics = new LyricsInfo();
        try {

            String url = "http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?artist="
                    + StringUtil.urlEncode(artist) + "&song=" + StringUtil.urlEncode(song);
            String xml = executeGetRequest(url);
            lyrics = parseSearchResult(xml);

        } catch (HttpResponseException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get lyrics for song '{}'. Request failed: {}", song, e.toString());
            }
            if (Objects.equals(HttpStatus.SC_SERVICE_UNAVAILABLE, e.getStatusCode())) {
                lyrics.setTryLater(true);
            }
        } catch (SocketException | ConnectTimeoutException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get lyrics for song '{}': {}", song, e.toString());
            }
            lyrics.setTryLater(true);
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get lyrics for song '" + song + "'.", e);
            }
        } catch (ExecutionException e) {
            ConcurrentUtils.handleCauseUnchecked(e);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to get lyrics for song '" + song + "'.", e);
            }
        }
        return lyrics;
    }

    private LyricsInfo parseSearchResult(String xml) throws ExecutionException {
        SAXBuilder builder = createSAXBuilder();
        Document document;
        try {
            document = builder.build(new StringReader(xml));
        } catch (JDOMException | IOException e) {
            throw new ExecutionException("Unable to parse XML.", e);
        }

        Element root = document.getRootElement();
        Namespace ns = root.getNamespace();

        String lyric = StringUtils.trimToNull(root.getChildText("Lyric", ns));
        String song = root.getChildText("LyricSong", ns);
        String artist = root.getChildText("LyricArtist", ns);

        return new LyricsInfo(lyric, artist, song);
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private String executeGetRequest(String url) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(15_000).setSocketTimeout(15_000).build();
        HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return client.execute(method, responseHandler);
        }
    }
}
