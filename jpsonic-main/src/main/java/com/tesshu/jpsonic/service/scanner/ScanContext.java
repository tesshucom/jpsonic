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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.scanner;

import java.time.Instant;

/**
 * An immutable object that holds shared state across the entire scan process.
 *
 * <p>
 * {@code ScanContext} contains configuration values and control flags that are
 * passed to each {@code ScanProcedure} during the scan. It is designed to be
 * immutable to ensure thread safety and consistency of state.
 *
 * <p>
 * This object is constructed once at the start of a scan and is passed
 * read-only to all procedures. When updates are needed, a new
 * {@code ScanContext} instance is created based on the original.
 *
 * <p>
 * Among its properties, {@code scanDate} is automatically generated at the
 * beginning of the scan. All other fields are user-configurable via the GUI.
 *
 * <h3>Key Properties</h3>
 * <ul>
 * <li><b>{@code scanDate}</b> The timestamp when the scan starts. This is
 * automatically generated and used for logging and identifying the scan
 * session.</li>
 * <li><b>{@code ignoreFileTimestamps}</b> Whether to ignore file modification
 * timestamps when determining changes. If {@code true}, all files will be
 * scanned regardless of their last modified time.</li>
 * <li><b>{@code podcastFolder}</b> Path to a special folder used for podcasts.
 * This may be used for filtering or classification.</li>
 * <li><b>{@code sortStrict}</b> Whether strict sorting should be applied when
 * ordering songs or albums. May affect how symbols, case, and other variations
 * are handled.</li>
 * <li><b>{@code useScanLog}</b> Indicates whether to persist scan results in a
 * log for later review.</li>
 * <li><b>{@code scanLogRetention}</b> Number of days to retain the scan log, if
 * explicitly specified.</li>
 * <li><b>{@code defaultScanLogRetention}</b> The default retention period (in
 * days) used when no specific value is provided.</li>
 * <li><b>{@code useScanEvents}</b> Whether to emit scan-related events during
 * processing.</li>
 * <li><b>{@code measureMemory}</b> Whether to measure and record memory usage
 * throughout the scan process.</li>
 * </ul>
 *
 * @see MediaScannerServiceImpl
 */
public record ScanContext(Instant scanDate, boolean ignoreFileTimestamps, String podcastFolder,
        boolean sortStrict, boolean useScanLog, int scanLogRetention, int defaultScanLogRetention,
        boolean useScanEvents, boolean measureMemory) {
}
