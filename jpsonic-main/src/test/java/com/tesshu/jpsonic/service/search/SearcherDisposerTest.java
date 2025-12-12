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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearcherDisposerTest {

    private AnalyzerFactory analyzerFactory;
    private IndexManager indexManager;
    private SearcherDisposer disposer;

    @BeforeEach
    void setUp() {
        analyzerFactory = mock(AnalyzerFactory.class);
        indexManager = mock(IndexManager.class);
        disposer = new SearcherDisposer(analyzerFactory, indexManager);
    }

    @Test
    void startSetsRunningTrue() {
        disposer.start();
        assertTrue(disposer.isRunning());
    }

    @Test
    void stopCallsDestroyAndSetsRunningFalse() {
        disposer.start();
        disposer.stop();

        verify(indexManager).destroy();
        verify(analyzerFactory).destroy();
        assertFalse(disposer.isRunning());
    }

    @Test
    void stopWithCallbackCallsDestroyAndCallback() {
        disposer.start();
        Runnable callback = mock(Runnable.class);

        disposer.stop(callback);

        verify(indexManager).destroy();
        verify(analyzerFactory).destroy();
        verify(callback).run();
        assertFalse(disposer.isRunning());
    }

    @Test
    void isRunningReturnsCorrectValue() {
        assertFalse(disposer.isRunning());

        disposer.start();
        assertTrue(disposer.isRunning());

        disposer.stop();
        assertFalse(disposer.isRunning());
    }
}
