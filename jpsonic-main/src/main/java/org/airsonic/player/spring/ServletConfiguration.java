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

import java.util.Properties;

import org.airsonic.player.controller.PodcastController;
import org.airsonic.player.i18n.AirsonicLocaleResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@Configuration
public class ServletConfiguration implements WebMvcConfigurer {

    private final AirsonicLocaleResolver airsonicLocaleResolver;

    public ServletConfiguration(AirsonicLocaleResolver airsonicLocaleResolver) {
        super();
        this.airsonicLocaleResolver = airsonicLocaleResolver;
    }

    @Bean
    public SimpleUrlHandlerMapping podcastUrlMapping(PodcastController podcastController) {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setAlwaysUseFullPath(true);
        Properties properties = new Properties();
        properties.put("/podcast/**", podcastController);
        handlerMapping.setMappings(properties);
        return handlerMapping;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return airsonicLocaleResolver;
    }

    // Both setUseSuffixPatternMatch and favorPathExtension calls allow URLs
    // with extensions to be resolved to the same mapping as without extension.
    //
    // In Airsonic's case, this is necessary, because a lot of our mappings assume
    // this behavior (for example "/home.view" URL to a "/home" mapping, or the
    // entire Subsonic REST API controller).
    //
    // Starting from Spring Boot 2.0, this feature is not enabled by default anymore,
    // so we must enable it manually.
    //
    // See:
    // https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide#spring-mvc-path-matching-default-behavior-change

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(true);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true);
    }
}
