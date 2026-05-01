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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import com.tesshu.jpsonic.infrastructure.settings.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy for determining which files and directories should be excluded during
 * the library scanning process.
 * <p>
 * This component filters out system-specific noise (e.g., Synology metadata,
 * macOS DS_Store), user-defined patterns, and illegal file system structures
 * like trailing dots on Windows.
 * </p>
 */
@Component
public class ScanningExclusionPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(ScanningExclusionPolicy.class);

    /**
     * Reserved names and metadata directories specific to Synology DSM and common
     * network file systems.
     * 
     * @see <a href=
     *      "https://kb.synology.com/en-in/DSM/help/FileStation/connect?version=7">Synology
     *      Knowledge Base:Remote Connection</a>
     */
    private static final List<String> SYNOLOGY_RESERVED_WORDS = List
        .of("._", ".SYNOPPSDB", ".DS_Store", "@eaDir", "@sharebin", "@tmp",
                ".SynologyWorkingDirectory");

    private final SettingsFacade settingsFacade;

    public ScanningExclusionPolicy(SettingsFacade settingsFacade) {
        this.settingsFacade = settingsFacade;
    }

    /**
     * Evaluates whether a given path should be ignored by the media scanner.
     * <p>
     * The evaluation order is:
     * <ul>
     * <li>Symlinks (if configured to be ignored)</li>
     * <li>User-defined regex patterns</li>
     * <li>Hidden files (starting with a single dot)</li>
     * <li>Windows-prohibited trailing dots</li>
     * <li>OS/Device-specific reserved words (e.g., Thumbs.db, Synology
     * metadata)</li>
     * </ul>
     * </p>
     *
     * @param path The path to evaluate.
     * @return true if the path meets any exclusion criteria; false otherwise.
     */
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean isExcluded(Path path) {
        if (settingsFacade.get(FileSystemSKeys.ignoreSymlinks) && Files.isSymbolicLink(path)) {
            LOG.info("Excluding symbolic link %s".formatted(path));
            return true;
        }

        Path fileName = path.getFileName();
        if (fileName == null) {
            return true;
        }

        // Exclude those that match a user-specified pattern
        String name = fileName.toString();
        Pattern excludePattern = settingsFacade
            .getCachedPattern(FileSystemSKeys.excludePatternString);
        if (excludePattern != null && excludePattern.matcher(name).matches()) {
            LOG
                .info("Excluding file which matches exclude pattern %s : %s"
                    .formatted(settingsFacade.get(FileSystemSKeys.excludePatternString), path));
            return true;
        }

        // Exclude all hidden files starting with a single "."
        if (name.charAt(0) == '.' && !name.startsWith("..")) {
            return true;
        }

        // Exclude files end with a dot (Windows prohibitions)
        if (name.endsWith(".")) {
            LOG.warn("""
                    Excluding files ending with Dot. \
                    Recommended to replace with a UNICODE String \
                    like One dot leader or Horizontal Ellipsis. : %s\
                    """.formatted(path));
            return true;
        }

        // Exclude Thumbnail on Windows
        if ("Thumbs.db".equals(name)) {
            return true;
        }

        // Exclude files or dir created on Synology devices
        return SYNOLOGY_RESERVED_WORDS.stream().anyMatch(name::equals);
    }
}
