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

package org.airsonic.player.util;

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

import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
    }

    public static boolean copyFile(final File toCopy, final File destFile) {
        try (OutputStream os = Files.newOutputStream(Paths.get(destFile.toURI()));
                InputStream is = Files.newInputStream(Paths.get(toCopy.toURI()))) {
            return FileUtils.copyStream(is, os);
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
                if (!FileUtils.copyFilesRecusively(child, newDestDir)) {
                    return false;
                }
            }
        } else {
            return FileUtils.copyFile(toCopy, new File(destDir, toCopy.getName()));
        }
        return true;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static boolean copyJarResourcesRecursively(final File destDir, final JarURLConnection jarConnection)
            throws IOException {
        try (JarFile jarFile = jarConnection.getJarFile()) {
            for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                final JarEntry entry = e.nextElement();
                if (entry.getName().startsWith(jarConnection.getEntryName())) {
                    final String filename = StringUtils.removeStart(entry.getName(), //
                            jarConnection.getEntryName());

                    final File f = new File(destDir, filename);
                    if (entry.isDirectory()) {
                        if (!FileUtils.ensureDirectoryExists(f)) {
                            throw new IOException("Could not create directory: " + f.getAbsolutePath());
                        }
                    } else {
                        try (InputStream entryInputStream = jarFile.getInputStream(entry)) {
                            if (!FileUtils.copyStream(entryInputStream, f)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean copyResourcesRecursively(final URL originUrl, final File destination) {
        try {
            final URLConnection urlConnection = originUrl.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                return FileUtils.copyJarResourcesRecursively(destination, (JarURLConnection) urlConnection);
            } else {
                return FileUtils.copyFilesRecusively(new File(originUrl.getPath()), destination);
            }
        } catch (final IOException e) {
            LOG.error("Exception occurred while copying file.", e);
        }
        return false;
    }

    private static boolean copyStream(final InputStream is, final File f) {
        try (OutputStream os = Files.newOutputStream(Paths.get(f.toURI()))) {
            return FileUtils.copyStream(is, os);
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
