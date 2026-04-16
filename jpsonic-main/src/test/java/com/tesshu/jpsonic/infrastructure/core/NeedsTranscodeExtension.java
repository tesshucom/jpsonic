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

package com.tesshu.jpsonic.infrastructure.core;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indicates that the annotated test depends on a transcoding environment
 * (typically backed by {@code ffmpeg}).
 *
 * <p>
 * Applies {@link NeedsTranscodeExtension}, which emits a warning if the system
 * property {@code jpsonic-transcodePath} is not set.
 * </p>
 *
 * <p>
 * This annotation complements {@link NeedsHome} by explicitly declaring that
 * the test relies on the ffmpeg linkage mechanism prepared there.
 * </p>
 *
 * <p>
 * It does not affect test execution (no skip or forced failure). Tests are
 * expected to fail when the transcoding environment is not available.
 * </p>
 *
 * <p>
 * Typical use cases include:
 * </p>
 * <ul>
 * <li>Tests that directly invoke transcoding</li>
 * <li>Tests that indirectly depend on transcoding through stream processing or
 * file format handling</li>
 * <li>Making hidden ffmpeg dependencies explicit</li>
 * </ul>
 */
public final class NeedsTranscodeExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger LOG = LoggerFactory.getLogger(NeedsTranscodeExtension.class);

    private static final String PROP = "jpsonic-transcodePath";

    @Override
    public void beforeAll(ExtensionContext context) {
        check(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        check(context);
    }

    private void check(ExtensionContext context) {

        String transcodePath = System.getProperty(PROP);

        if (transcodePath == null || transcodePath.isBlank()) {

            String target = context.getUniqueId(); // ← ここ変更

            LOG.warn("""
                    [NeedsTranscode] Missing required system property: '{}'
                    This test is expected to FAIL without transcode environment.
                    Target: {}
                    """, PROP, target);
        }
    }
}
