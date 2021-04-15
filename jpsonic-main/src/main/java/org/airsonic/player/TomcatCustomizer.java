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

package org.airsonic.player;

import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
@Qualifier("tomcatCustomizer")
public final class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addContextCustomizers(context -> {

            StandardJarScanFilter standardJarScanFilter = new StandardJarScanFilter();
            standardJarScanFilter.setTldScan(
                    "dwr-*.jar,jstl-*.jar,spring-security-taglibs-*.jar,spring-web-*.jar,spring-webmvc-*.jar,string-*.jar,taglibs-standard-impl-*.jar,tomcat-annotations-api-*.jar,tomcat-embed-jasper-*.jar");
            standardJarScanFilter.setTldSkip("*");
            context.getJarScanner().setJarScanFilter(standardJarScanFilter);

            boolean development = System.getProperty("airsonic.development") != null;

            // Increase the size and time before eviction of the Tomcat
            // cache so that resources aren't uncompressed too often.
            // See https://github.com/jhipster/generator-jhipster/issues/3995

            StandardRoot resources = new StandardRoot();
            if (development) {
                resources.setCachingAllowed(false);
            } else {
                resources.setCacheMaxSize(100_000);
                resources.setCacheObjectMaxSize(4000);
                resources.setCacheTtl(24 * 3600 * 1000); // 1 day, in milliseconds
            }
            context.setResources(resources);

            // Put Jasper in production mode so that JSP aren't recompiled
            // on each request.
            // See http://stackoverflow.com/questions/29653326/spring-boot-application-slow-because-of-jsp-compilation
            Container jsp = context.findChild("jsp");
            if (jsp instanceof Wrapper) {
                ((Wrapper) jsp).addInitParameter("development", Boolean.toString(development));
            }
        });
    }
}
