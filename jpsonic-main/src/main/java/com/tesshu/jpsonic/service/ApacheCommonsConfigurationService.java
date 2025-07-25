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

package com.tesshu.jpsonic.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

import com.tesshu.jpsonic.util.FileUtil;
import com.tesshu.jpsonic.util.LegacyMap;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApacheCommonsConfigurationService {

    private static final Logger LOG = LoggerFactory
        .getLogger(ApacheCommonsConfigurationService.class);
    public static final String HEADER_COMMENT = """
            Jpsonic preferences.  NOTE: This file is automatically generated.\
             Do not modify while application is running\
            """;

    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
    private final Configuration config;

    public ApacheCommonsConfigurationService() {
        Path propertyFile = SettingsService.getPropertyFile();
        if (!Files.exists(propertyFile)) {
            try {
                FileUtil.touch(propertyFile);
            } catch (IOException e) {
                throw new CompletionException("Could not create new property file", e);
            }
        }
        Parameters params = new Parameters();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        layout.setHeaderComment(HEADER_COMMENT);
        layout.setGlobalSeparator("=");
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class)
            .configure(params
                .properties()
                .setFile(propertyFile.toFile())
                .setSynchronizer(new ReadWriteSynchronizer())
                .setLayout(layout));
        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new CompletionException("Could not load property file at " + propertyFile, e);
        }
    }

    public void save() {
        try {
            builder.save();
        } catch (ConfigurationException e) {
            LOG.error("Unable to write to property file.", e);
        }
    }

    public Object getProperty(String key) {
        return config.getProperty(key);
    }

    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    public void clearProperty(String key) {
        config.clearProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }

    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    public int getInteger(String key, int defaultValue) {
        return config.getInteger(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    public ImmutableConfiguration getImmutableSnapshot() {
        MapConfiguration mapConfiguration = new MapConfiguration(LegacyMap.of());
        mapConfiguration.copy(config);
        return mapConfiguration;
    }
}
