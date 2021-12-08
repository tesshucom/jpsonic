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

package com.tesshu.jpsonic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import com.tesshu.jpsonic.controller.ViewName;
import com.tesshu.jpsonic.filter.BootstrapVerificationFilter;
import com.tesshu.jpsonic.filter.FontSchemeFilter;
import com.tesshu.jpsonic.filter.MetricsFilter;
import com.tesshu.jpsonic.filter.ParameterDecodingFilter;
import com.tesshu.jpsonic.filter.RESTFilter;
import com.tesshu.jpsonic.filter.RequestEncodingFilter;
import com.tesshu.jpsonic.filter.ResponseHeaderFilter;
import com.tesshu.jpsonic.spring.DatabaseConfiguration.ProfileNameConstants;
import com.tesshu.jpsonic.util.LegacyHsqlUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.directwebremoting.servlet.DwrServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.ReflectionUtils;

@SpringBootApplication(exclude = { JmxAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        MultipartAutoConfiguration.class, // TODO: update to use spring boot builtin multipart support
        LiquibaseAutoConfiguration.class }, scanBasePackages = { "com.tesshu.jpsonic", "com.tesshu.jpsonic" })
@EnableScheduling
public class Application extends SpringBootServletInitializer
        implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    /**
     * Registers the DWR servlet.
     *
     * @return a registration bean.
     */
    @Bean
    public ServletRegistrationBean<Servlet> dwrServletRegistrationBean() {
        ServletRegistrationBean<Servlet> servlet = new ServletRegistrationBean<>(new DwrServlet(), "/dwr/*");
        servlet.addInitParameter("crossDomainSessionSecurity", "false");
        return servlet;
    }

    @Bean
    public FilterRegistrationBean<Filter> bootstrapVerificationFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(bootstrapVerificationFiler());
        registration.addUrlPatterns("/*");
        registration.setName("BootstrapVerificationFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public Filter bootstrapVerificationFiler() {
        return new BootstrapVerificationFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> parameterDecodingFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(parameterDecodingFilter());
        registration.addUrlPatterns("/*");
        registration.setName("ParameterDecodingFilter");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public Filter parameterDecodingFilter() {
        return new ParameterDecodingFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> restFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(restFilter());
        registration.addUrlPatterns("/rest/*");
        registration.setName("RESTFilter");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    public Filter restFilter() {
        return new RESTFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> requestEncodingFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(requestEncodingFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("encoding", "UTF-8");
        registration.setName("RequestEncodingFilter");
        registration.setOrder(4);
        return registration;
    }

    @Bean
    public Filter requestEncodingFilter() {
        return new RequestEncodingFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> cacheFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(cacheFilter());
        registration.addUrlPatterns("/fonts/*", "/icons/*", "/script/*", "/scss/*", "/style/*", "/dwr/*",
                "/" + ViewName.COVER_ART.value());
        registration.addInitParameter("Cache-Control", "max-age=36000");
        registration.setName("CacheFilter");
        registration.setOrder(5);
        return registration;
    }

    @Bean
    public Filter cacheFilter() {
        return new ResponseHeaderFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> noCacheFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(noCacheFilter());
        registration.addUrlPatterns("/" + ViewName.STATUS_CHART.value(), "/" + ViewName.USER_CHART.value(),
                "/" + ViewName.PLAY_QUEUE.value(), "/" + ViewName.PODCAST_CHANNELS.value(),
                "/" + ViewName.PODCAST_CHANNEL.value(), "/" + ViewName.HELP.value(), "/" + ViewName.TOP.value(),
                "/" + ViewName.HOME.value());
        registration.addInitParameter("Cache-Control", "no-cache, post-check=0, pre-check=0");
        registration.addInitParameter("Pragma", "no-cache");
        registration.addInitParameter("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        registration.setName("NoCacheFilter");
        registration.setOrder(6);
        return registration;
    }

    @Bean
    public Filter metricsFilter() {
        return new MetricsFilter();
    }

    @Bean
    public FilterRegistrationBean<Filter> metricsFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(metricsFilter());
        registration.setOrder(7);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<FontSchemeFilter> fontSchemeFilterRegistration() {
        FilterRegistrationBean<FontSchemeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FontSchemeFilter());
        registration.addUrlPatterns("*.view", "/login", "/index");
        registration.setOrder(8);
        return registration;
    }

    @Bean
    public Filter noCacheFilter() {
        return new ResponseHeaderFilter();
    }

    private static SpringApplicationBuilder doConfigure(SpringApplicationBuilder application) {
        application.application().addListeners((ApplicationListener<ApplicationPreparedEvent>) event -> {
            if (event.getApplicationContext().getEnvironment()
                    .acceptsProfiles(Profiles.of(ProfileNameConstants.LEGACY))) {
                LegacyHsqlUtil.checkHsqldbDatabaseVersion();
            }
        });

        // Customize the application or call application.sources(...) to add sources
        // Since our example is itself a @Configuration class (via @SpringBootApplication)
        // we actually don't need to override this method.
        return application.sources(Application.class).web(WebApplicationType.SERVLET);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return doConfigure(application);
    }

    private void invokeHelper(@NonNull Class<?> helperClass, @NonNull Class<?> factoryClass,
            @NonNull Object factoryInstance) {
        try {
            Method configure = ReflectionUtils.findMethod(helperClass, "configure", factoryClass);
            if (configure == null) {
                throw new IllegalArgumentException(
                        "Unreachable code: The configure method does not exist in the helper class.");
            } else {
                configure.invoke(null, factoryInstance);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to apply ApplicationHelper.", e);
            }
        }
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {

        Class<?> factoryClass = null;
        Class<?> helperClass = null;
        try {
            factoryClass = Class.forName("org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory");
            helperClass = Class.forName("com.tesshu.jpsonic.TomcatApplicationHelper");
        } catch (NoClassDefFoundError | ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No tomcat classes found");
            }
        }
        if (factoryClass == null) {
            try {
                factoryClass = Class
                        .forName("org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory");
                helperClass = Class.forName("com.tesshu.jpsonic.JettyApplicationHelper");
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No jetty classes found");
                }
            }
        }
        if (factoryClass == null || helperClass == null) {
            throw new IllegalArgumentException("Unreachable code: There should be a class according to the profile.");
        }

        invokeHelper(helperClass, factoryClass, factoryClass.cast(container));

    }

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        doConfigure(builder).run(args);
    }
}
