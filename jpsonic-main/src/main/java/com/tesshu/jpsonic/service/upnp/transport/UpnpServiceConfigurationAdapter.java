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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class for injecting ExecutorService into
 * DefaultUpnpServiceConfiguration.
 * 
 * The inserted ExecutorService is assumed to be an ExecutorService generated
 * via Spring Boot.
 */
public class UpnpServiceConfigurationAdapter extends DefaultUpnpServiceConfiguration {

    private static final Logger LOG = LoggerFactory
        .getLogger(UpnpServiceConfigurationAdapter.class);

    private final ExecutorService defaultExecutorService;
    private final ExecutorService asyncExecutorService;
    private final Executor registryMaintainerExecutor;

    public UpnpServiceConfigurationAdapter(ExecutorService defaultExecutorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor) {
        super();
        this.defaultExecutorService = defaultExecutorService;
        this.asyncExecutorService = asyncExecutorService;
        this.registryMaintainerExecutor = registryMaintainerExecutor;
    }

    public UpnpServiceConfigurationAdapter(ExecutorService executorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor,
            int streamListenPort) {
        super(streamListenPort);
        this.defaultExecutorService = executorService;
        this.asyncExecutorService = asyncExecutorService;
        this.registryMaintainerExecutor = registryMaintainerExecutor;
    }

    public UpnpServiceConfigurationAdapter(ExecutorService executorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor,
            int streamListenPort, int multicastResponsePort) {
        super(streamListenPort, multicastResponsePort);
        this.defaultExecutorService = executorService;
        this.asyncExecutorService = asyncExecutorService;
        this.registryMaintainerExecutor = registryMaintainerExecutor;
    }

    protected UpnpServiceConfigurationAdapter(ExecutorService executorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor,
            boolean checkRuntime) {
        super(checkRuntime);
        this.defaultExecutorService = executorService;
        this.asyncExecutorService = asyncExecutorService;
        this.registryMaintainerExecutor = registryMaintainerExecutor;
    }

    protected UpnpServiceConfigurationAdapter(ExecutorService executorService,
            ExecutorService asyncExecutorService, Executor registryMaintainerExecutor,
            int streamListenPort, int multicastResponsePort, boolean checkRuntime) {
        super(streamListenPort, multicastResponsePort, checkRuntime);
        this.defaultExecutorService = executorService;
        this.asyncExecutorService = asyncExecutorService;
        this.registryMaintainerExecutor = registryMaintainerExecutor;
    }

    @Override
    public Executor getRegistryMaintainerExecutor() {
        return registryMaintainerExecutor;
    }

    /**
     * The use of standard ExecutorService will be avoided in order to meet
     * requirements such as shutsown being linked to the application lifecycle and
     * shutdown flow, ensuring logging of UncaughtExceptions, and not accessing the
     * SecurityManager.
     */
    @Deprecated
    @Override
    protected final ExecutorService createDefaultExecutorService() {
        return null;
    }

    @Override
    protected ExecutorService getDefaultExecutorService() {
        return defaultExecutorService;
    }

    @Override
    public ExecutorService getAsyncProtocolExecutor() {
        return asyncExecutorService;
    }

    @Override
    public void shutdown() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Executor service will not be shut down");
        }
    }
}
