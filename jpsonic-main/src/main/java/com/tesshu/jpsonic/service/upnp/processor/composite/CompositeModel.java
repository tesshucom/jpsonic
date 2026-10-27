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
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.service.upnp.processor.composite;

/**
 * An interface that represents a composite model made up of multiple domain
 * models. Since UPnP does not use sessions, the set of models used for
 * summarization is represented by an ID.
 */
@FunctionalInterface
public interface CompositeModel {

    /**
     * The composite ID of the SubModels that make up the Model. This implementation
     * class will need to provide a parsing method for ID and be able to recover the
     * SubModel's ID.
     */
    String createCompositeId();
}
