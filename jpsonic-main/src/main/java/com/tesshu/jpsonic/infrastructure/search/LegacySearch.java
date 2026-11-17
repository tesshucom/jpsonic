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
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.infrastructure.search;

import com.tesshu.jpsonic.infrastructure.search.criteria.HttpSearchCriteria;

/**
 * Legacy search interface for the migration from persistence entities to domain
 * models. This interface will be removed once the migration is complete.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface LegacySearch {

    LegacySearchResult search(HttpSearchCriteria criteria);

}
