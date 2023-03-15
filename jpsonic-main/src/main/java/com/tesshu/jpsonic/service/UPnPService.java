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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.tesshu.jpsonic.service.upnp.ApacheUpnpServiceConfiguration;
import com.tesshu.jpsonic.service.upnp.CustomContentDirectory;
import com.tesshu.jpsonic.service.upnp.MSMediaReceiverRegistrarService;
import com.tesshu.jpsonic.util.concurrent.ConcurrentUtils;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.support.model.dlna.DLNAProfiles;
import org.fourthline.cling.support.model.dlna.DLNAProtocolInfo;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("shutdownHook")
public class UPnPService {

    // WMP handles this value rigorously.
    // If you adopt the Cling default, the device will disconnect in 30 minutes
    private static final int MIN_ADVERTISEMENT_AGE_SECONDS = 60 * 60 * 24;

    private static final Logger LOG = LoggerFactory.getLogger(UPnPService.class);

    private final SettingsService settingsService;
    private final VersionService versionService;
    private final CustomContentDirectory dispatchingContentDirectory;
    private final AtomicReference<Boolean> running;
    private final Object lock = new Object();

    private UpnpService deligate;

    public UPnPService(SettingsService settingsService, VersionService versionService,
            @Qualifier("dispatchingContentDirectory") CustomContentDirectory dispatchingContentDirectory) {
        super();
        this.settingsService = settingsService;
        this.versionService = versionService;
        this.dispatchingContentDirectory = dispatchingContentDirectory;
        running = new AtomicReference<>(false);
    }

    @PostConstruct
    public void init() {
        if (settingsService.isDlnaEnabled() || settingsService.isSonosEnabled()) {
            ensureServiceStarted();
            if (settingsService.isDlnaEnabled()) {
                // Start DLNA media server?
                setMediaServerEnabled(true);
            }
        }
    }

    public void ensureServiceStarted() {
        running.getAndUpdate(bo -> {
            if (bo) {
                return true;
            } else {
                startService();
                return true;
            }
        });
    }

    @PreDestroy()
    public void ensureServiceStopped() {
        running.getAndUpdate(isRunning -> {
            if (deligate != null && isRunning) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Shutting down UPnP service...");
                }
                deligate.getRegistry().removeAllLocalDevices();
                deligate.shutdown();
                if (LOG.isInfoEnabled()) {
                    LOG.info("Shutting down UPnP service - Done!");
                }
            }
            return false;
        });

    }

    private void startService() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting UPnP service...");
        }
        createService();
        if (0 < SettingsService.getDefaultUPnPPort()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Successfully started UPnP service on port {}!", SettingsService.getDefaultUPnPPort());
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("Starting UPnP service - Done!");
            }
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException") // fourthline/UpnpServiceImpl#UpnpServiceImpl
    private void createService() {
        synchronized (lock) {
            try {
                deligate = new UpnpServiceImpl(new ApacheUpnpServiceConfiguration());
            } catch (RuntimeException e) {
                // The exception is wrapped in Runtime and thrown!
                if (e.getCause() instanceof RouterException) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Failed to start UPnP service.", e);
                    }
                    return;
                }
                // Other than this, it is not inspected, so rethrow
                throw e;
            }

            // Asynch search for other devices (most importantly UPnP-enabled routers for port-mapping)
            try {
                deligate.getControlPoint().search();
            } catch (IllegalArgumentException e) {
                if (LOG.isInfoEnabled()) {
                    LOG.error("Network search failed.", e);
                }
            }
        }
    }

    public void setMediaServerEnabled(boolean enabled) {
        if (enabled) {
            ensureServiceStarted();
            try {
                deligate.getRegistry().addDevice(createMediaServerDevice());
                if (LOG.isInfoEnabled()) {
                    LOG.info("Enabling UPnP media server [{}]({})", settingsService.getDlnaServerName(),
                            settingsService.getDlnaBaseLANURL());
                }
            } catch (ExecutionException e) {
                ConcurrentUtils.handleCauseUnchecked(e);
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to start UPnP media server.", e);
                }
            }
        } else {
            ensureServiceStopped();
        }
    }

    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops", "PMD.UnusedAssignment" })
    /*
     * [AvoidInstantiatingObjectsInLoops] (DLNAProtocolInfo, AssertionError) Not reusable [UnusedAssignment] (icon)
     * false positive
     */
    private LocalDevice createMediaServerDevice() throws ExecutionException {

        @SuppressWarnings("unchecked")
        LocalService<CustomContentDirectory> directoryservice = new AnnotationLocalServiceBinder()
                .read(CustomContentDirectory.class);
        ServiceManager serviceManager = new ServiceManager(directoryservice, dispatchingContentDirectory);
        directoryservice.setManager(serviceManager);

        final ProtocolInfos protocols = new ProtocolInfos();
        for (DLNAProfiles dlnaProfile : DLNAProfiles.values()) {
            if (dlnaProfile == DLNAProfiles.NONE) {
                continue;
            }
            try {
                protocols.add(new DLNAProtocolInfo(dlnaProfile));
            } catch (IllegalArgumentException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Error in adding dlna protocols with unclear cases.", e);
                }
            }
        }

        @SuppressWarnings("unchecked")
        LocalService<ConnectionManagerService> connetionManagerService = new AnnotationLocalServiceBinder()
                .read(ConnectionManagerService.class);
        connetionManagerService
                .setManager(new DefaultServiceManager<ConnectionManagerService>(connetionManagerService) {
                    @Override
                    protected ConnectionManagerService createServiceInstance() {
                        return new ConnectionManagerService(protocols, null);
                    }
                });

        // For compatibility with Microsoft
        @SuppressWarnings("unchecked")
        LocalService<MSMediaReceiverRegistrarService> receiverService = new AnnotationLocalServiceBinder()
                .read(MSMediaReceiverRegistrarService.class);
        receiverService.setManager(new DefaultServiceManager<>(receiverService, MSMediaReceiverRegistrarService.class));

        Icon icon = null;
        try (InputStream in = UPnPService.class.getResourceAsStream("logo-512.png")) {
            icon = new Icon("image/png", 512, 512, 32, "logo-512", in);
        } catch (IOException e) {
            throw new ExecutionException("Icon cannot be generated", e);
        }

        String serverName = settingsService.getDlnaServerName();
        String serialNumber = versionService.getLocalBuildNumber();
        DLNADoc[] dlnaDocs = { new DLNADoc("DMS", DLNADoc.Version.V1_5) };
        URI modelURI = URI.create("https://tesshu.com");
        URI manufacturerURI = URI.create("https://github.com/jpsonic/jpsonic");
        URI presentationURI = URI.create(settingsService.getDlnaBaseLANURL());
        ManufacturerDetails manufacturerDetails = new ManufacturerDetails(serverName, modelURI);
        ModelDetails modelDetails = new ModelDetails(serverName, null, versionService.getLocalVersion().toString(),
                manufacturerURI);
        DeviceDetails details = new DeviceDetails(serverName, manufacturerDetails, modelDetails, serialNumber, null,
                presentationURI, dlnaDocs, null);
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(serverName),
                MIN_ADVERTISEMENT_AGE_SECONDS);
        DeviceType type = new UDADeviceType("MediaServer", 1);
        try {
            return new LocalDevice(identity, type, details, new Icon[] { icon },
                    new LocalService[] { directoryservice, connetionManagerService, receiverService });
        } catch (ValidationException e) {
            throw new ExecutionException("LocalDevice/Service cannot be generated", e);
        }
    }

    public List<String> getSonosControllerHosts() {
        ensureServiceStarted();
        List<String> result = new ArrayList<>();
        for (Device<?, ?, ?> device : deligate.getRegistry()
                .getDevices(new DeviceType("schemas-upnp-org", "ZonePlayer"))) {
            if (device instanceof RemoteDevice) {
                URL descriptorURL = ((RemoteDevice) device).getIdentity().getDescriptorURL();
                if (descriptorURL != null) {
                    result.add(descriptorURL.getHost());
                }
            }
        }
        return result;
    }

    private static class ServiceManager extends DefaultServiceManager<CustomContentDirectory> {

        private final CustomContentDirectory directory;

        public ServiceManager(LocalService<CustomContentDirectory> service, CustomContentDirectory directory) {
            super(service);
            this.directory = directory;
        }

        @Override
        protected CustomContentDirectory createServiceInstance() {
            return this.directory;
        }
    }
}
