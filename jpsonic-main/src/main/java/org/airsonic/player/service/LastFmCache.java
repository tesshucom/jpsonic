/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.service;

import de.umass.lastfm.cache.Cache;
import de.umass.lastfm.cache.FileSystemCache;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Based on {@link FileSystemCache}, but properly closes files and enforces
 * time-to-live (by ignoring HTTP header directives).
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class LastFmCache extends Cache {

    private static final Logger LOG = LoggerFactory.getLogger(LastFmCache.class);

    private final File cacheDir;
    private final long ttl;

    public LastFmCache(File cacheDir, final long ttl) {
        this.cacheDir = cacheDir;
        this.ttl = ttl;

        setExpirationPolicy((method, params) -> ttl);
    }

    @Override
    public boolean contains(String cacheEntryName) {
        return getXmlFile(cacheEntryName).exists();
    }

    @Override
    public InputStream load(String cacheEntryName) {
        try (InputStream in = Files.newInputStream(Paths.get(getXmlFile(cacheEntryName).toURI()))) {
            return new ByteArrayInputStream(IOUtils.toByteArray(in));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void remove(String cacheEntryName) {
        File xml = getXmlFile(cacheEntryName);
        if (!xml.delete() && LOG.isWarnEnabled()) {
            LOG.warn("The file '{}' could not be deleted.", xml.getAbsolutePath());
        }
        File meta = getMetaFile(cacheEntryName);
        if (!meta.delete() && LOG.isWarnEnabled()) {
            LOG.warn("The file '{}' could not be deleted.", meta.getAbsolutePath());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Override
    public void store(String cacheEntryName, InputStream inputStream, long expirationDate) {
        createCache();

        File xmlFile = getXmlFile(cacheEntryName);
        try (OutputStream xmlOut = Files.newOutputStream(Paths.get(xmlFile.toURI()))) {

            IOUtils.copy(inputStream, xmlOut);

            File metaFile = getMetaFile(cacheEntryName);
            Properties properties = new Properties();

            // Note: Ignore the given expirationDate, since Last.fm sets it to just one day ahead.
            properties.setProperty("expiration-date", Long.toString(getExpirationDate()));

            try (OutputStream metaOut = Files.newOutputStream(Paths.get(metaFile.toURI()))) {
                properties.store(metaOut, null);
            }

        } catch (IOException e) {
            // we ignore the exception. if something went wrong we just don't cache it.
        }
    }

    private long getExpirationDate() {
        return System.currentTimeMillis() + ttl;
    }

    private void createCache() {
        if (!cacheDir.exists() && !cacheDir.mkdirs() && LOG.isWarnEnabled()) {
            LOG.warn("The directory '{}' could not be created.", cacheDir.getAbsolutePath());
        }
    }

    @Override
    public boolean isExpired(String cacheEntryName) {
        File f = getMetaFile(cacheEntryName);
        if (!f.exists()) {
            return false;
        }
        try (InputStream in = Files.newInputStream(Paths.get(f.toURI()))) {
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
        File[] listFiles = cacheDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.isFile()) {
                    if (!file.delete() && LOG.isWarnEnabled()) {
                        LOG.warn("The file '{}' could not be deleted.", file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private File getXmlFile(String cacheEntryName) {
        return new File(cacheDir, cacheEntryName + ".xml");
    }

    private File getMetaFile(String cacheEntryName) {
        return new File(cacheDir, cacheEntryName + ".meta");
    }
}
