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
 * (C) 2015 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.service.sonos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tesshu.jpsonic.SuppressFBWarnings;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sindre Mehus
 */
public class SonosServiceRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(SonosServiceRegistration.class);

    public void setEnabled(String airsonicBaseUrl, String sonosControllerIp, boolean enabled, String sonosServiceName,
            int sonosServiceId) throws IOException {
        String localUrl = airsonicBaseUrl + "ws/Sonos";

        if (LOG.isInfoEnabled()) {
            LOG.info((enabled ? "Enabling" : "Disabling") + " Sonos music service, using Sonos controller IP "
                    + sonosControllerIp + ", SID " + sonosServiceId + ", and Airsonic URL " + localUrl);
        }

        List<Pair<String, String>> params = new ArrayList<>();
        params.add(Pair.of("sid", String.valueOf(sonosServiceId)));
        String keyCaps = "caps";
        if (enabled) {
            params.add(Pair.of("name", sonosServiceName));
            params.add(Pair.of("uri", localUrl));
            params.add(Pair.of("secureUri", localUrl));
            params.add(Pair.of("pollInterval", "1200"));
            params.add(Pair.of("authType", "UserId"));
            params.add(Pair.of("containerType", "MService"));
            params.add(Pair.of(keyCaps, "search"));
            params.add(Pair.of(keyCaps, "trFavorites"));
            params.add(Pair.of(keyCaps, "alFavorites"));
            params.add(Pair.of(keyCaps, "ucPlaylists"));
            params.add(Pair.of(keyCaps, "extendedMD"));
            params.add(Pair.of("presentationMapVersion", "1"));
            params.add(Pair.of("presentationMapUri", airsonicBaseUrl + "sonos/presentationMap.xml"));
            params.add(Pair.of("stringsVersion", "5"));
            params.add(Pair.of("stringsUri", airsonicBaseUrl + "sonos/strings.xml"));
        }

        String controllerUrl = String.format("http://%s:1400/customsd", sonosControllerIp);
        String result = execute(controllerUrl, params);
        if (LOG.isInfoEnabled()) {
            LOG.info("Sonos controller returned: " + result);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // (BasicNameValuePair) Not reusable
    private String execute(String url, List<Pair<String, String>> parameters) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        for (Pair<String, String> parameter : parameters) {
            params.add(new BasicNameValuePair(parameter.getLeft(), parameter.getRight()));
        }
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20 * 1000) // 20 seconds
                .setSocketTimeout(20 * 1000) // 20 seconds
                .build();
        HttpPost request = new HttpPost(url);
        request.setConfig(requestConfig);
        request.setEntity(new UrlEncodedFormEntity(params, StringUtil.ENCODING_UTF8));

        return executeRequest(request);
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    private String executeRequest(HttpUriRequest request) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            return client.execute(request, responseHandler);

        }
    }
}
