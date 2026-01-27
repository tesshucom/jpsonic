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

package com.tesshu.jpsonic.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that the annotated test class requires a temporary and isolated
 * Jpsonic home directory.
 *
 * <p>
 * This annotation applies {@link NeedsHomeExtension}, which creates a dedicated
 * temporary home directory for the duration of the test class and configures
 * the application to resolve all home‑relative paths against it.
 * </p>
 *
 * <p>
 * Use this annotation for tests that rely on the Jpsonic home directory.
 * Typical examples include:
 * </p>
 *
 * <ul>
 * <li>Logic that depends on files located under the home directory</li>
 * <li>Services that read or write configuration files (e.g.,
 * {@code SettingsService})</li>
 * <li>Components that produce logs or other output under the home
 * directory</li>
 * </ul>
 *
 * <p>
 * This annotation is intended for class-level use only.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(NeedsHomeExtension.class)
public @interface NeedsHome {
}
