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
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic;

import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.StatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Hook to control the shutdown order. ContextClosedEvent -> Stop presentation layer(including UPnP) -> Change stream
 * transfer status/playQueue status -> Safe cancellation of pooling tasks -> @PreDestroy of this class -> If the profile
 * is 'legacy' LegacyHsqlDaoHelper#onDestroy
 */
@Component
@DependsOn({ "shortExecutor", "jukeExecutor", "podcastDownloadExecutor", "podcastRefreshExecutor", "scanExecutor" })
public class ShutdownHook implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);

    private final StatusService statusService;
    private final SettingsService settingsService;

    public ShutdownHook(@Lazy StatusService statusService, SettingsService settingsService) {
        super();
        this.statusService = statusService;
        this.settingsService = settingsService;
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    /*
     * The Transfer Status check is done when working with most streams. However, many of these processes are
     * overcrowded processes. If any of these processes are already running and then perform a shutdown, the Spring
     * mechanism may not be able to handle interrupts well. If can't interrupt beautifully, we may experience some
     * non-fatal side effects.
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        statusService.getAllStreamStatuses().stream().filter(TransferStatus::isActive)
                .forEach(TransferStatus::terminate);
        if (settingsService.isVerboseLogShutdown() && LOG.isInfoEnabled()) {
            LOG.info("Changed the status of all active streams to 'terminate'.");
        }
    }

    @PreDestroy
    public void preDestroy() {
        // Referenced by @DependsOn
        if (settingsService.isVerboseLogShutdown() && LOG.isInfoEnabled()) {
            LOG.info("Safely stop running tasks. It can take up to about 30s to complete.");
        }
    }
}
