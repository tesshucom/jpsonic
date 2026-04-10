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
 * (C) 2026 tesshucom
 */

package com.tesshu.jpsonic.service.settings;

import com.tesshu.jpsonic.infrastructure.core.LifecyclePhase;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class SettingsStorageDisposer implements SmartLifecycle {

    private final SettingsStorage settingsStorage;

    public SettingsStorageDisposer(SettingsStorage settingsStorage) {
        super();
        this.settingsStorage = settingsStorage;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        settingsStorage.cleanup();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return settingsStorage.isRunning();
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.PROPERTY.getValue();
    }
}
