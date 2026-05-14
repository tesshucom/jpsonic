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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.filesystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.tesshu.jpsonic.infrastructure.core.EnvironmentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for locating executable binaries across the system.
 */
public class ExecutableResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutableResolver.class);

    private ExecutableResolver() {
        // no-op
    }

    /**
     * Searches for the specified executable within the directories listed in the OS
     * PATH environment variable.
     * 
     * @param executableName The name of the executable to find.
     * @return The absolute path to the executable if found; otherwise null.
     */
    public static Path lookForExecutable(String executableName) {
        for (String path : System.getenv("PATH").split(File.pathSeparator, -1)) {
            Path file = Path.of(path, executableName);
            if (Files.exists(file)) {
                LOG.debug("Found {} in {}", executableName, path);
                return file;
            } else {
                LOG.debug("Looking for {} in {} (not found)", executableName, path);
            }
        }
        return null;
    }

    /**
     * Locates the binary used for transcoding by prioritizing system-specific
     * directories before searching the OS PATH. Includes automatic extension
     * completion for Windows (.exe).
     * 
     * @param executableName The base name of the transcoding executable.
     * @return The absolute path to the executable if found; otherwise null.
     */
    public static Path lookForTranscodingExecutable(String executableName) {
        for (String name : Arrays.asList(executableName, "%s.exe".formatted(executableName))) {
            Path executableLocation = EnvironmentProvider
                .getInstance()
                .getTranscodeDirectory()
                .resolve(name);
            if (Files.exists(executableLocation)) {
                return executableLocation;
            }
            executableLocation = lookForExecutable(executableName);
            if (executableLocation != null && Files.exists(executableLocation)) {
                return executableLocation;
            }
        }
        return null;
    }
}
