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

package com.tesshu.jpsonic.feature.auth.rememberme;

import com.tesshu.jpsonic.infrastructure.core.LifecyclePhase;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class RememberMeLifecycle implements SmartLifecycle {

    private final RememberMeKeyManager keyManager;

    public RememberMeLifecycle(RememberMeKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    public void start() {
        keyManager.init();
    }

    @Override
    public void stop() {
        keyManager.stop();
    }

    @Override
    public boolean isRunning() {
        return keyManager.isRunning();
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.REMEMBERME.getValue();
    }
}
