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

package com.tesshu.jpsonic.spring;

import java.util.Collections;

import javax.xml.ws.Endpoint;

import com.tesshu.jpsonic.service.SonosService;
import com.tesshu.jpsonic.service.sonos.SonosFaultInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml" })
public class SonosConfiguration {

    @Bean
    public Endpoint sonosEndpoint(SonosService sonosService, SonosFaultInterceptor sonosFaultInterceptor) {
        EndpointImpl endpoint = new EndpointImpl(sonosService);
        endpoint.publish("/Sonos");
        endpoint.setOutFaultInterceptors(Collections.singletonList(sonosFaultInterceptor));
        return endpoint;
    }

}
