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

package com.tesshu.jpsonic.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.tesshu.jpsonic.service.upnp.UpnpServiceFactory;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jupnp.UpnpService;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({ "shutdownHook", "upnpServiceFactory" })
public class UPnPService {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPService.class);

    private final SettingsService settingsService;
    private final UpnpServiceFactory upnpServiceFactory;
    private final AtomicReference<Boolean> running;
    private final ReentrantLock runningLock = new ReentrantLock();

    private UpnpService deligate;

    public UPnPService(SettingsService settingsService, UpnpServiceFactory upnpServiceFactory) {
        super();
        this.settingsService = settingsService;
        this.upnpServiceFactory = upnpServiceFactory;
        running = new AtomicReference<>(false);
    }

    private void infoIfEnabled(String msg) {
        if (LOG.isInfoEnabled()) {
            LOG.info(msg);
        }
    }

    private void errorIfEnabled(String msg, Throwable t) {
        if (LOG.isErrorEnabled()) {
            LOG.error(msg, t);
        }
    }

    @PostConstruct
    public void init() {
        if (settingsService.isDlnaEnabled() || settingsService.isSonosEnabled()) {
            start();
            if (settingsService.isDlnaEnabled()) {
                setEnabled(true);
            }
        }
    }

    private void start() {
        runningLock.lock();
        try {
            running.getAndUpdate(isRunning -> {
                if (isRunning) {
                    return true;
                } else {
                    infoIfEnabled("Starting UPnP service...");
                    createService();
                    if (0 < SettingsService.getDefaultUPnPPort()) {
                        infoIfEnabled("Successfully started UPnP service on port %s!"
                                .formatted(SettingsService.getDefaultUPnPPort()));
                    } else {
                        infoIfEnabled("Starting UPnP service - Done!");
                    }
                    return true;
                }
            });
        } finally {
            runningLock.unlock();
        }
    }

    @PreDestroy()
    private void stop() {
        runningLock.lock();
        try {
            running.getAndUpdate(isRunning -> {
                if (deligate != null && isRunning) {
                    infoIfEnabled("Shutting down UPnP service...");
                    deligate.shutdown();
                    infoIfEnabled("Shutting down UPnP service - Done!");
                }
                return false;
            });
        } finally {
            runningLock.unlock();
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void createService() {
        deligate = upnpServiceFactory.createUpnpService();

        try {
            deligate.startup();
        } catch (RuntimeException e) {
            // RouterException: The exception is wrapped in Runtime and thrown...
            if (e.getCause() instanceof RouterException) {
                errorIfEnabled("Failed to start UPnP service.", e);
                return;
            }
            throw e;
        }

        try {
            deligate.getControlPoint().search();
        } catch (IllegalArgumentException e) {
            errorIfEnabled("Network search failed.", e);
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            start();
            try {
                deligate.getRegistry().addDevice(upnpServiceFactory.createServerDevice());
                infoIfEnabled("Enabling UPnP media server [%s](%s)".formatted(settingsService.getDlnaServerName(),
                        settingsService.getDlnaBaseLANURL()));
            } catch (ExecutionException e) {
                ConcurrentUtils.handleCauseUnchecked(e);
                errorIfEnabled("Failed to start UPnP media server.", e);
            }
        } else {
            stop();
        }
    }
}
