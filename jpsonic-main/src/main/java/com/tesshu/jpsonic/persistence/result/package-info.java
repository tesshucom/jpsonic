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
 * Contains non-persistent result objects used within the persistence layer.
 *
 * <p>
 * The classes in this package represent read-only values such as search
 * results, scan snapshots, or derived information that is not stored in the
 * database. These objects are produced by persistence operations but do not
 * correspond to persistent entities.
 * </p>
 *
 * <p>
 * By separating these result types from persistent entities, the persistence
 * layer can clearly distinguish between data that originates from the database
 * and data that is computed or aggregated at runtime.
 * </p>
 */
package com.tesshu.jpsonic.persistence.result;
