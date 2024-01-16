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

package com.tesshu.jpsonic.controller;

import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
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

    @GetMapping
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletRequestBindingException, ExecutionException {
        String url = ServletRequestUtils.getRequiredStringParameter(request, Attributes.Request.URL.value());

        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(15))
                .setResponseTimeout(Timeout.ofSeconds(15)).build();
        HttpGet method = new HttpGet(URI.create(url));
        method.setConfig(requestConfig);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // Use HttpClientResponseHandler
            try (CloseableHttpResponse resp = client.execute(method)) {
                int statusCode = resp.getCode();
                if (statusCode == OK.value()) {
                    try (InputStream in = resp.getEntity().getContent()) {
                        IOUtils.copy(in, response.getOutputStream());
                    }
                } else {
                    response.sendError(statusCode);
                }
            }
        } catch (IOException e) {
            throw new ExecutionException("Unable to handle proxy for external HTTP request.", e);
        }
        return null;
    }
}
