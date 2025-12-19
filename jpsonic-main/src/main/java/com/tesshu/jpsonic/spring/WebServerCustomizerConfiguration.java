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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.ReflectionUtils;

@Configuration
@Profile("!test")
public class WebServerCustomizerConfiguration {

    private static final Logger LOG = LoggerFactory
        .getLogger(WebServerCustomizerConfiguration.class);

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        return new CustomWebServerFactoryCustomizer();
    }

    private static class CustomWebServerFactoryCustomizer
            implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

        @Override
        public void customize(ConfigurableServletWebServerFactory container) {

            Class<?> factoryClass = null;
            Class<?> helperClass = null;
            try {
                factoryClass = Class
                    .forName(
                            "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory");
                helperClass = Class.forName("com.tesshu.jpsonic.TomcatApplicationHelper");
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No tomcat classes found");
                }
            }
            if (factoryClass == null) {
                try {
                    factoryClass = Class
                        .forName(
                                "org.springframework.boot.jetty.servlet.JettyServletWebServerFactory");
                    helperClass = Class.forName("com.tesshu.jpsonic.JettyApplicationHelper");
                } catch (NoClassDefFoundError | ClassNotFoundException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No jetty classes found");
                    }
                }
            }
            if (factoryClass == null || helperClass == null) {
                throw new IllegalArgumentException(
                        "Unreachable code: There should be a class according to the profile.");
            }

            invokeHelper(helperClass, factoryClass, factoryClass.cast(container));

        }

        private void invokeHelper(@NonNull Class<?> helperClass, @NonNull Class<?> factoryClass,
                @NonNull Object factoryInstance) {
            Method configure = ReflectionUtils.findMethod(helperClass, "configure", factoryClass);
            if (configure == null) {
                throw new IllegalArgumentException(
                        "Unreachable code: The configure method does not exist in the helper class.");
            }
            try {
                configure.invoke(null, factoryInstance);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to apply ApplicationHelper.", e);
                }
            }
        }
    }
}
