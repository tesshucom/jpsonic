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

package com.tesshu.jpsonic.service.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import com.tesshu.jpsonic.service.SettingsService;
import com.tesshu.jpsonic.service.UPnPService;
import com.tesshu.jpsonic.service.VersionService;
import com.tesshu.jpsonic.service.upnp.processor.CustomContentDirectory;
import com.tesshu.jpsonic.service.upnp.transport.JpsonicUpnpServiceConf;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.ProtocolInfos;
import org.jupnp.support.model.dlna.DLNAProfiles;
import org.jupnp.support.model.dlna.DLNAProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn({ "upnpExecutorService" })
public class UpnpServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(UpnpServiceFactory.class);

    /*
     * WMP handles this value rigorously. If you adopt the Cling default, the device will disconnect in 30 minutes.
     */
    private static final int MIN_ADVERTISEMENT_AGE_SECONDS = 60 * 60 * 24;

    private final SettingsService settingsService;
    private final VersionService versionService;
    private final CustomContentDirectory dispatchingContentDirectory;
    private final ExecutorService upnpExecutorService;

    public UpnpServiceFactory(SettingsService settingsService, VersionService versionService,
            @Qualifier("dispatchingContentDirectory") CustomContentDirectory dispatchingContentDirectory,
            @Qualifier("upnpExecutorService") ExecutorService upnpExecutorService) {
        super();
        this.settingsService = settingsService;
        this.versionService = versionService;
        this.dispatchingContentDirectory = dispatchingContentDirectory;
        this.upnpExecutorService = upnpExecutorService;
    }

    public UpnpService createUpnpService() {
        UpnpServiceConfiguration conf = new JpsonicUpnpServiceConf(upnpExecutorService, SettingsService.getBrand(),
                versionService.getLocalVersion());
        return new UpnpServiceImpl(conf);
    }

    @SuppressWarnings({ "PMD.UnusedAssignment" }) // [UnusedAssignment] (icon) false positive
    public LocalDevice createServerDevice() throws ExecutionException {
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
        URI modelURI = URI.create("https://github.com/jpsonic");
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
