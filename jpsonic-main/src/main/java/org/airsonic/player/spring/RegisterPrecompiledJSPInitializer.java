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

package org.airsonic.player.spring;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CompletionException;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.airsonic.player.service.SettingsService;
import org.airsonic.player.spring.webxmldomain.ServletDef;
import org.airsonic.player.spring.webxmldomain.ServletMappingDef;
import org.airsonic.player.spring.webxmldomain.WebApp;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
//import org.springframework.stereotype.Component;

//@Component
public class RegisterPrecompiledJSPInitializer implements ServletContextInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterPrecompiledJSPInitializer.class);

    @Override
    public void onStartup(ServletContext servletContext) {
        if (SettingsService.isDevelopmentMode()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not registering precompiled jsps");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Registering precompiled jsps");
            }
            registerPrecompiledJSPs(servletContext);
        }
    }

    private static void registerPrecompiledJSPs(ServletContext servletContext) {
        WebApp webApp = parseXmlFragment();
        for (ServletDef def : webApp.getServletDefs()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Registering precompiled JSP: {} -> {}", def.getName(), def.getSclass());
            }
            ServletRegistration.Dynamic reg = servletContext.addServlet(def.getName(), def.getSclass());
            // Need to set loadOnStartup somewhere between 0 and 128. 0 is highest priority. 99 should be fine
            reg.setLoadOnStartup(99);
        }

        for (ServletMappingDef mapping : webApp.getServletMappingDefs()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Mapping servlet: {} -> {}", mapping.getName(), mapping.getUrlPattern());
            }
            servletContext.getServletRegistration(mapping.getName()).addMapping(mapping.getUrlPattern());
        }
    }

    private static WebApp parseXmlFragment() {
        try (InputStream precompiledJspWebXml = RegisterPrecompiledJSPInitializer.class
                .getResourceAsStream("/precompiled-jsp-web.xml")) {
            JAXBContext jaxbContext = new JAXBDataBinding(WebApp.class).getContext();
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            try (InputStream webXmlIS = new SequenceInputStream(
                    new SequenceInputStream(IOUtils.toInputStream("<web-app>", Charset.defaultCharset()),
                            precompiledJspWebXml),
                    IOUtils.toInputStream("</web-app>", Charset.defaultCharset()))) {
                return (WebApp) unmarshaller.unmarshal(webXmlIS);
            }
        } catch (IOException e) {
            throw new CompletionException("Could not close stream.", e);
        } catch (JAXBException e) {
            throw new CompletionException("Could not parse precompiled-jsp-web.xml", e);
        }
    }

}
