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

package org.airsonic.player.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.airsonic.player.NeedsHome;
import org.airsonic.player.dao.MusicFolderDao;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
public class LegacyDatabaseStartupTest {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyDatabaseStartupTest.class);

    @Autowired
    private MusicFolderDao musicFolderDao;

    @BeforeAll
    public static void beforeAll() throws IOException {
        String homePath = System.getProperty("jpsonic.home");
        File dbDirectory = new File(homePath, "/db");
        FileUtils.forceMkdir(dbDirectory);
        copyResourcesRecursively(LegacyDatabaseStartupTest.class.getResource("/db/pre-liquibase/db"), dbDirectory);
    }

    @Test
    public void testStartup() {
        assertEquals(1, musicFolderDao.getAllMusicFolders().size());
    }

    private static boolean copyFile(final File toCopy, final File destFile) {
        try (OutputStream os = Files.newOutputStream(Paths.get(destFile.toURI()));
                InputStream is = Files.newInputStream(Paths.get(toCopy.toURI()))) {
            return copyStream(is, os);
        } catch (IOException e) {
            LOG.error("Exception occurred while copying file.", e);
        }
        return false;
    }

    private static boolean copyFilesRecusively(@NonNull final File toCopy, final File destDir) {
        assert destDir.isDirectory();

        if (toCopy.isDirectory()) {
            final File newDestDir = new File(destDir, toCopy.getName());
            if (!newDestDir.exists() && !newDestDir.mkdir()) {
                return false;
            }
            for (final File child : Objects.requireNonNull(toCopy.listFiles())) {
                if (!copyFilesRecusively(child, newDestDir)) {
                    return false;
                }
            }
        } else {
            return copyFile(toCopy, new File(destDir, toCopy.getName()));
        }
        return true;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static boolean copyJarResourcesRecursively(final File destDir, final JarURLConnection jarConnection)
            throws IOException {
        try (JarFile jarFile = jarConnection.getJarFile()) {
            for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                final JarEntry entry = e.nextElement();
                if (entry.getName().startsWith(jarConnection.getEntryName())) {
                    final String filename = StringUtils.removeStart(entry.getName(), //
                            jarConnection.getEntryName());

                    final File f = new File(destDir, filename);
                    if (entry.isDirectory()) {
                        if (!ensureDirectoryExists(f)) {
                            throw new IOException("Could not create directory: " + f.getAbsolutePath());
                        }
                    } else {
                        try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                            if (!copyStream(entryInputStream, f)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean copyResourcesRecursively(final URL originUrl, final File destination) {
        try {
            final URLConnection urlConnection = originUrl.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                return copyJarResourcesRecursively(destination, (JarURLConnection) urlConnection);
            } else {
                return copyFilesRecusively(new File(originUrl.getPath()), destination);
            }
        } catch (final IOException e) {
            LOG.error("Exception occurred while copying file.", e);
        }
        return false;
    }

    private static boolean copyStream(final InputStream is, final File f) {
        try (OutputStream os = Files.newOutputStream(Paths.get(f.toURI()))) {
            return copyStream(is, os);
        } catch (IOException e) {
            LOG.error("Exception occurred while copying stream.", e);
        }
        return false;
    }

    private static boolean copyStream(final InputStream is, final OutputStream os) {
        try {
            final byte[] buf = new byte[1024];
            for (int len = is.read(buf); len > 0; len = is.read(buf)) {
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
            return true;
        } catch (final IOException e) {
            LOG.error("Exception occurred while copying stream.", e);
        }
        return false;
    }

    private static boolean ensureDirectoryExists(final File f) {
        return f.exists() || f.mkdir();
    }

}
