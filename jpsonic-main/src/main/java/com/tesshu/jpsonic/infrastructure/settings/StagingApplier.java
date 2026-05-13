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

package com.tesshu.jpsonic.infrastructure.settings;

/**
 * An interface for registering multiple configuration items as a logical group,
 * typically used for processing requests from controllers.
 * 
 * Beyond simple data mapping, this interface is responsible for resolving
 * interdependent registration specifications (business logic)—such as "if A is
 * enabled, B must be set to a specific value"—within the implementation class
 * and ensuring persistence via {@link SettingsFacade} or other means.
 *
 * @param <F> The type of the form object used for registration.
 */
@FunctionalInterface
public interface StagingApplier<F> {

    /**
     * Executes the registration and update process for configuration items based on
     * the content of the provided form, while maintaining consistency between
     * related settings.
     *
     * @param form The form object containing the registration details.
     */
    void apply(F form, SettingsStagingPort stagingPort);
}
