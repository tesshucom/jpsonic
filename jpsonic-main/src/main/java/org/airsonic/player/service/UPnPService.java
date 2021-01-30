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

package org.airsonic.player.service;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.airsonic.player.service.upnp.ApacheUpnpServiceConfiguration;
import org.airsonic.player.service.upnp.CustomContentDirectory;
import org.airsonic.player.service.upnp.MSMediaReceiverRegistrarService;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author Sindre Mehus
 * 
 * @version $Id$
 */
@Service
public class UPnPService {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPService.class);

    @Autowired
    private SettingsService settingsService;

    private UpnpService deligate;

    @Autowired
    @Qualifier("dispatchingContentDirectory")
    private CustomContentDirectory dispatchingContentDirectory;

    private AtomicReference<Boolean> running = new AtomicReference<>(false);

    private static final Object LOCK = new Object();

    @PostConstruct
    public void init() {
        if (settingsService.isDlnaEnabled() || settingsService.isSonosEnabled()) {
            ensureServiceStarted();
            if (settingsService.isDlnaEnabled()) {
                // Start DLNA media server?
                setMediaServerEnabled(true);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ensureServiceStopped()));
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

    public void ensureServiceStopped() {
        running.getAndUpdate(bo -> {
            if (bo) {
                if (deligate != null) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Disabling UPnP/DLNA media server");
                    }
                    deligate.getRegistry().removeAllLocalDevices();
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Shutting down UPnP service...");
                    }
                    deligate.shutdown();
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Shutting down UPnP service - Done!");
                    }
                }
                return false;
            } else {
                return false;
            }
        });

    }

    private void startService() {
        try {
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
        } catch (Throwable x) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to start UPnP service: " + x, x);
            }
        }
    }

    private void createService() {
        synchronized (LOCK) {
            UpnpServiceConfiguration upnpConf = 0 < SettingsService.getDefaultUPnPPort()
                    ? new DefaultUpnpServiceConfiguration(SettingsService.getDefaultUPnPPort())
                    : new ApacheUpnpServiceConfiguration();
            deligate = new UpnpServiceImpl(upnpConf);
            // Asynch search for other devices (most importantly UPnP-enabled routers for port-mapping)
            deligate.getControlPoint().search();
        }
    }

    public void setMediaServerEnabled(boolean enabled) {
        if (enabled) {
            ensureServiceStarted();
            try {
                deligate.getRegistry().addDevice(createMediaServerDevice());
                if (LOG.isInfoEnabled()) {
                    LOG.info("Enabling UPnP/DLNA media server");
                }
            } catch (Exception x) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to start UPnP/DLNA media server: " + x, x);
                }
            }
        } else {
            ensureServiceStopped();
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    /*
     * [PMD.AvoidInstantiatingObjectsInLoops] (DLNAProtocolInfo, AssertionError) Not reusable
     */
    private LocalDevice createMediaServerDevice() throws Exception {

        // TODO: DLNACaps

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
            } catch (Exception e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Error in adding dlna protocols.", new AssertionError("Errors with unclear cases.", e));
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

        Icon icon;
        try (InputStream in = getClass().getResourceAsStream("logo-512.png")) {
            icon = new Icon("image/png", 512, 512, 32, "logo-512", in);
        }

        String serverName = settingsService.getDlnaServerName();
        DeviceDetails details = new DeviceDetails(serverName, new ManufacturerDetails(serverName),
                new ModelDetails(serverName), new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5) }, null);
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(serverName));
        DeviceType type = new UDADeviceType("MediaServer", 1);
        return new LocalDevice(identity, type, details, new Icon[] { icon },
                new LocalService[] { directoryservice, connetionManagerService, receiverService });
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

    public UpnpService getUpnpService() {
        return deligate;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setCustomContentDirectory(CustomContentDirectory customContentDirectory) {
        this.dispatchingContentDirectory = customContentDirectory;
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
