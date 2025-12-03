package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.spring.LifecyclePhase;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class UPnPDisposer implements SmartLifecycle {

    private final UPnPService upnpService;

    public UPnPDisposer(UPnPService upnpService) {
        this.upnpService = upnpService;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        upnpService.stop();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return upnpService.isRunning();
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.UPNP.getValue();
    }
}
