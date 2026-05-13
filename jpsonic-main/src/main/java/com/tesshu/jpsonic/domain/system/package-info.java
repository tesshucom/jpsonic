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

/**
 * Defines the “system-level contracts” shared across the entire application.
 *
 * <p>
 * Classes placed in this package have the following characteristics:
 * </p>
 * <ul>
 * <li>They are not persisted (i.e., not Entities).</li>
 * <li>They are referenced by both the UI (Controller) and Service layers.</li>
 * <li>They represent stable, application-wide logical settings, identifiers, or
 * schemes.</li>
 * <li>Most are enums or lightweight value objects without state (Version is the
 * only exception).</li>
 * </ul>
 *
 * <p>
 * The following types of classes must not be placed in this package:
 * </p>
 * <ul>
 * <li>Persisted data models (Entities)</li>
 * <li>UI-specific View Objects (VOs)</li>
 * <li>Service-internal logic or implementation details</li>
 * <li>Layer-specific concepts such as search models or persistence
 * parameters</li>
 * </ul>
 *
 * <p>
 * The purpose of this package is to consolidate “cross-cutting and stable
 * concepts” in order to clarify dependency directions between layers and to
 * simplify future maintenance and architectural evolution.
 * </p>
 */
package com.tesshu.jpsonic.domain.system;
