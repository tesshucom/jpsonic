package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.TransferStatus;
import com.tesshu.jpsonic.spring.LifecyclePhase;
import org.springframework.context.SmartLifecycle;

public class StreamDisposer implements SmartLifecycle {

    private final StatusService statusService;

    public StreamDisposer(StatusService statusService) {
        this.statusService = statusService;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        statusService
            .getAllStreamStatuses()
            .stream()
            .filter(TransferStatus::isActive)
            .forEach(TransferStatus::terminate);
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return statusService.getAllStreamStatuses().stream().anyMatch(TransferStatus::isActive);
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.STREAM.getValue();
    }
}
