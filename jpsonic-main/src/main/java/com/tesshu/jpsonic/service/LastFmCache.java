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
 * (C) 2014 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.tesshu.jpsonic.util.FileUtil;
import de.umass.lastfm.cache.Cache;
import de.umass.lastfm.cache.FileSystemCache;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on {@link FileSystemCache}, but properly closes files and enforces time-to-live (by ignoring HTTP header
 * directives).
 *
 * @author Sindre Mehus
 */
public class LastFmCache extends Cache {

    private static final Logger LOG = LoggerFactory.getLogger(LastFmCache.class);

    private final Path cacheDir;
    private final long ttl;
    private final Object lock = new Object();

    public LastFmCache(Path cacheDir, final long ttl) {
        super();
        this.cacheDir = cacheDir;
        this.ttl = ttl;
        setExpirationPolicy((method, params) -> ttl);
    }

    @Override
    public boolean contains(String cacheEntryName) {
        return Files.exists(getXmlFile(cacheEntryName));
    }

    @Override
    public InputStream load(String cacheEntryName) {
        try (InputStream in = Files.newInputStream(getXmlFile(cacheEntryName))) {
            return new ByteArrayInputStream(IOUtils.toByteArray(in));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void remove(String cacheEntryName) {
        FileUtil.deleteIfExists(getXmlFile(cacheEntryName));
        FileUtil.deleteIfExists(getMetaFile(cacheEntryName));
    }

    @Override
    public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
        createCache();

        Path xmlFile = getXmlFile(cacheEntryName);
        try (OutputStream xmlOut = Files.newOutputStream(xmlFile)) {

            IOUtils.copy(inputStream, xmlOut);

            Path metaFile = getMetaFile(cacheEntryName);
            Properties properties = new Properties();

            // Note: Ignore the given expirationDate, since Last.fm sets it to just one day ahead.
            properties.setProperty("expiration-date", Long.toString(getExpirationDate()));

            try (OutputStream metaOut = Files.newOutputStream(metaFile)) {
                properties.store(metaOut, null);
            }

        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot generate cache.", e);
            }
        }
    }

    private long getExpirationDate() {
        return System.currentTimeMillis() + ttl;
    }

    private void createCache() {
        if (!Files.exists(cacheDir)) {
            synchronized (lock) {
                if (FileUtil.createDirectories(cacheDir) == null && LOG.isWarnEnabled()) {
                    LOG.warn("The directory '{}' could not be created.", cacheDir);
                }

            }
        }
    }

    @Override
    public boolean isExpired(String cacheEntryName) {
        Path f = getMetaFile(cacheEntryName);
        if (!Files.exists(f)) {
            return false;
        }
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            long expirationDate = Long.parseLong(p.getProperty("expiration-date"));
            return expirationDate < System.currentTimeMillis();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void clear() {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(cacheDir)) {
            ds.forEach(child -> {
                if (Files.isRegularFile(child)) {
                    FileUtil.deleteIfExists(child);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getXmlFile(String cacheEntryName) {
        return Path.of(cacheDir.toString(), cacheEntryName + ".xml");
    }

    private Path getMetaFile(String cacheEntryName) {
        return Path.of(cacheDir.toString(), cacheEntryName + ".meta");
    }
}
