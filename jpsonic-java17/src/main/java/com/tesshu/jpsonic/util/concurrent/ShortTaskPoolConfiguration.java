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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "short-task-pool")
public class ShortTaskPoolConfiguration {

    private int corePoolSize;
    private int queueCapacity;
    private int maxPoolSize;

    public void setCorePoolSize(@NonNull String corePoolSize) {
        this.corePoolSize = Integer.parseInt(corePoolSize);
    }

    public void setQueueCapacity(@NonNull String queueCapacity) {
        this.queueCapacity = Integer.parseInt(queueCapacity);
    }

    public void setMaxPoolSize(@NonNull String maxPoolSize) {
        this.maxPoolSize = Integer.parseInt(maxPoolSize);
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }
}
