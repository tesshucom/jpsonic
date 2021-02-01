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

package org.airsonic.player.controller;

import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tesshu.jpsonic.SuppressFBWarnings;
import com.tesshu.jpsonic.controller.Attributes;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * A proxy for external HTTP requests.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/proxy")
public class ProxyController {

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", justification = "False positive by try with resources.")
    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException, ClientProtocolException, IOException {
        String url = ServletRequestUtils.getRequiredStringParameter(request, Attributes.Request.URL.value());

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(15_000).setSocketTimeout(15_000).build();
        HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse resp = client.execute(method)) {
                int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode == OK.value()) {
                    try (InputStream in = resp.getEntity().getContent()) {
                        IOUtils.copy(in, response.getOutputStream());
                    }
                } else {
                    response.sendError(statusCode);
                }
            }
        }
        return null;
    }
}
