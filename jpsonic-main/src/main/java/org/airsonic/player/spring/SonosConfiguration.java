
package org.airsonic.player.spring;

import java.util.Collections;

import javax.xml.ws.Endpoint;

import org.airsonic.player.service.SonosService;
import org.airsonic.player.service.sonos.SonosFaultInterceptor;
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
