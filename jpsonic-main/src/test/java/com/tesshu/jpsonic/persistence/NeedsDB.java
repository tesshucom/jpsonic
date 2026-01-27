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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that the annotated test class requires a database environment.
 *
 * <p>
 * At present, this annotation behaves identically to {@link NeedsHome} and
 * relies on the temporary home directory created for the test class. In the
 * current test infrastructure, the database used during tests is a file‑based
 * database located under the Jpsonic home directory, and therefore its
 * lifecycle is implicitly managed by {@code NeedsHome}.
 * </p>
 *
 * <p>
 * Although currently a variant of {@code NeedsHome}, this annotation
 * establishes a dedicated semantic layer for tests that depend on a database.
 * Future database‑focused setup or teardown procedures will be centralized
 * through this annotation.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(NeedsDBExtension.class)
public @interface NeedsDB {
}
