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
 * Contains non-persistent parameter objects used within the persistence layer.
 *
 * <p>
 * The classes in this package represent input parameters such as search
 * conditions, filters, or other criteria passed into persistence operations.
 * These objects are used to control or refine database queries but are not
 * themselves persistent entities.
 * </p>
 *
 * <p>
 * By separating these parameter types from persistent entities, the persistence
 * layer can clearly distinguish between data that originates from the database
 * and data that is supplied at runtime to influence query behavior.
 * </p>
 */
package com.tesshu.jpsonic.persistence.param;
