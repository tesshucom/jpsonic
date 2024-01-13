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

package com.tesshu.jpsonic.service.upnp.transport;

import java.util.concurrent.ExecutorService;

/**
 * Default settings for the UPnP Client used by Jpsonic.
 * It will be mainly used for device searches.
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public record DefaultStreamClientConf(
        ExecutorService executorService, 
        int defaultMaxPerRoute,
        int maxTotal,
        int bufferSize,
        int socketTimeoutSeconds) implements ClingStreamClientConf {

    public DefaultStreamClientConf(ExecutorService executorService) {
        this(executorService, 2, 20, 8094, 10);
    }

    @Override
    public ExecutorService getRequestExecutorService() {
        return executorService;
    }

    /**
     * Will be used only once at service startup.
     */
    @Override
    public int getRetryIterations() {
        return 0;
    }
}
