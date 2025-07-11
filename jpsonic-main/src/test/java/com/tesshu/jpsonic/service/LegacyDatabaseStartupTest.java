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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.tesshu.jpsonic.NeedsHome;
import com.tesshu.jpsonic.dao.MusicFolderDao;
import com.tesshu.jpsonic.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@SpringBootConfiguration
@ComponentScan(basePackages = "com.tesshu.jpsonic")
@ExtendWith(NeedsHome.class)
class LegacyDatabaseStartupTest {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyDatabaseStartupTest.class);

    @Autowired
    private MusicFolderDao musicFolderDao;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path dbDirectory = Path.of(System.getProperty("jpsonic.home"), "/db");
        FileUtil.createDirectories(dbDirectory);
        copyResourcesRecursively(
                LegacyDatabaseStartupTest.class.getResource("/db/pre-liquibase/db"), dbDirectory);
    }

    @Test
    void testStartup() {
        assertEquals(1, musicFolderDao.getAllMusicFolders().size());
    }

    private static boolean copyFile(final Path toCopy, final Path destFile) {
        try (OutputStream os = Files.newOutputStream(destFile);
                InputStream is = Files.newInputStream(toCopy)) {
            return copyStream(is, os);
        } catch (IOException e) {
            LOG.error("Exception occurred while copying file.", e);
        }
        return false;
    }

    private static boolean copyFilesRecusively(@NonNull final Path source, Path destDir) {
        Path sourceFileName = source.getFileName();
        assert sourceFileName != null;
        assert Files.isDirectory(destDir);

        if (Files.isDirectory(source)) {
            final Path newDestDir = Path.of(destDir.toString(), sourceFileName.toString());
            if (!Files.exists(newDestDir) && FileUtil.createDirectories(newDestDir) == null) {
                return false;
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(source)) {
                for (Path child : ds) {
                    if (!copyFilesRecusively(child, newDestDir)) {
                        return false;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return copyFile(source, Path.of(destDir.toString(), sourceFileName.toString()));
        }
        return true;
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity" })
    private static boolean copyJarResourcesRecursively(final Path destDir,
            final JarURLConnection jarConnection) throws IOException {
        try (JarFile jarFile = jarConnection.getJarFile()) {
            for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                final JarEntry entry = e.nextElement();
                if (entry.getName().startsWith(jarConnection.getEntryName())) {
                    final String filename = StringUtils
                        .removeStart(entry.getName(), //
                                jarConnection.getEntryName());

                    final Path f = Path.of(destDir.toString(), filename);
                    if (entry.isDirectory()) {
                        if (!ensureDirectoryExists(f)) {
                            throw new IOException("Could not create directory: " + f);
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

    private static boolean copyResourcesRecursively(final URL originUrl, final Path destination) {
        try {
            final URLConnection urlConnection = originUrl.openConnection();
            if (urlConnection instanceof JarURLConnection connection) {
                return copyJarResourcesRecursively(destination, connection);
            } else {
                try {
                    return copyFilesRecusively(Path.of(originUrl.toURI()), destination);
                } catch (URISyntaxException e) {
                    LOG.error("Exception occurred while copying file.", e);
                }
            }
        } catch (final IOException e) {
            LOG.error("Exception occurred while copying file.", e);
        }
        return false;
    }

    private static boolean copyStream(final InputStream is, final Path f) {
        try (OutputStream os = Files.newOutputStream(f)) {
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

    private static boolean ensureDirectoryExists(final Path f) {
        return Files.exists(f) || FileUtil.createDirectories(f) != null;
    }

}
