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

import java.util.List;

import com.google.common.collect.Lists;
import com.tesshu.jpsonic.service.ApacheCommonsConfigurationService;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class CustomPropertySourceConfigurer
        implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    public static final String DATASOURCE_CONFIG_TYPE = "DatabaseConfigType";

    @Override
    public void initialize(ConfigurableWebApplicationContext ctx) {

        ApacheCommonsConfigurationService configurationService = new ApacheCommonsConfigurationService();
        ImmutableConfiguration snapshot = configurationService.getImmutableSnapshot();

        @SuppressWarnings("rawtypes")
        PropertySource ps = new CommonsConfigurationPropertySource("jpsonic-pre-init-configs", snapshot);

        ctx.getEnvironment().getPropertySources().addLast(ps);

        addDataSourceProfile(ctx);
    }

    private void addDataSourceProfile(ConfigurableWebApplicationContext ctx) {
        DataSourceConfigType dataSourceConfigType;
        String rawType = ctx.getEnvironment().getProperty(DATASOURCE_CONFIG_TYPE);
        if (StringUtils.isNotBlank(rawType)) {
            dataSourceConfigType = DataSourceConfigType.valueOf(StringUtils.upperCase(rawType));
        } else {
            dataSourceConfigType = DataSourceConfigType.LEGACY;
        }
        String dataSourceTypeProfile = StringUtils.lowerCase(dataSourceConfigType.name());
        List<String> existingProfiles = Lists.newArrayList(ctx.getEnvironment().getActiveProfiles());
        existingProfiles.add(dataSourceTypeProfile);
        ctx.getEnvironment().setActiveProfiles(existingProfiles.toArray(new String[0]));
    }
}
