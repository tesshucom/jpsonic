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
 * Defines “behavioral contracts” used within the persistence layer.
 *
 * <p>
 * The interfaces placed in this package are not simple markers. They represent
 * contracts that guarantee consistent behavior across common utilities and
 * browsing operations.
 * </p>
 *
 * <p>
 * When applying these contracts to new entities in the future, those types
 * should provide behavior consistent with existing Indexable / Orderable
 * implementations.
 * </p>
 *
 * <p>
 * This package exists to stabilize dependency directions. The contracts defined
 * here may be implemented by both persistent entities (api.entity) and
 * non-persistent search results or snapshots (result).
 * </p>
 */
package com.tesshu.jpsonic.persistence.contract;
