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
 * Provides database‑specific behavior for the persistence layer.
 *
 * <p>
 * This package abstracts differences between database engines, covering not
 * only SQL clause variations but also cases where entire statements must be
 * generated differently depending on the underlying database.
 * </p>
 *
 * <p>
 * By isolating vendor‑specific SQL behavior here, the rest of the persistence
 * layer can operate in a database-agnostic manner.
 * </p>
 */
package com.tesshu.jpsonic.persistence.dialect;
