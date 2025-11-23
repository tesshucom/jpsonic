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
 * (C) 2025 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import java.util.concurrent.atomic.AtomicReference;

import com.tesshu.jpsonic.spring.LifecyclePhase;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class SearcherDisposer implements SmartLifecycle {

    private final AnalyzerFactory analyzerFactory;
    private final IndexManager indexManager;
    private final AtomicReference<Boolean> running;

    public SearcherDisposer(AnalyzerFactory analyzerFactory, IndexManager indexManager) {
        this.analyzerFactory = analyzerFactory;
        this.indexManager = indexManager;
        running = new AtomicReference<>(false);
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        indexManager.destroy();
        analyzerFactory.destroy();
        running.set(false);
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.SEARCHER.getValue();
    }
}
