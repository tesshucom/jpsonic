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

package com.tesshu.jpsonic.persistence;

import com.tesshu.jpsonic.infrastructure.NeedsHomeExtension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class NeedsDBExtension implements BeforeAllCallback, AfterAllCallback {

    private final NeedsHomeExtension needsHomeExtension = new NeedsHomeExtension();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Ensure JPSONIC_HOME is initialized
        needsHomeExtension.beforeAll(context);

        // DB initialization is intentionally minimal for now.
        // Spring/HSQLDB will create an empty DB automatically.
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // No DB cleanup needed at this stage.

        // Delegate deletion to NeedsHomeExtension
        needsHomeExtension.afterAll(context);
    }
}
