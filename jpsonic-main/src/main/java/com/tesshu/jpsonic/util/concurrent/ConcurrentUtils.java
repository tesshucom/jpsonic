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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import java.util.concurrent.ExecutionException;

public final class ConcurrentUtils {

    private ConcurrentUtils() {
    }

    public static void handleCauseUnchecked(final ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof Error) {
            throw (Error) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
    }
}
