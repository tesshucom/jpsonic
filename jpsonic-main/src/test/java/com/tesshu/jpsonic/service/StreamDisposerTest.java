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

package com.tesshu.jpsonic.service;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.tesshu.jpsonic.service.StatusService.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.TooManyStaticImports")
class StreamDisposerTest {

    private StatusService statusService;
    private StreamDisposer disposer;

    @BeforeEach
    public void setup() {
        statusService = spy(new StatusService(null));
        disposer = new StreamDisposer(statusService);
    }

    @Test
    void testStop() {
        TransferStatus status = mock(TransferStatus.class);
        when(status.isActive()).thenReturn(false);
        when(statusService.getAllStreamStatuses()).thenReturn(List.of(status));
        disposer.stop();
        verify(status, never()).terminate();

        status = mock(TransferStatus.class);
        when(status.isActive()).thenReturn(true);
        when(statusService.getAllStreamStatuses()).thenReturn(List.of(status));
        disposer.stop();
        verify(status, times(1)).terminate();
    }

    @Test
    void testIsRunning() {
        // noActive
        TransferStatus status = mock(TransferStatus.class);
        when(status.isActive()).thenReturn(false);
        when(statusService.getAllStreamStatuses()).thenReturn(List.of(status));
        assertFalse(disposer.isRunning(), "Expected false when all streams are inactive");

        // oneActive
        TransferStatus status1 = mock(TransferStatus.class);
        TransferStatus status2 = mock(TransferStatus.class);
        when(status1.isActive()).thenReturn(false);
        when(status2.isActive()).thenReturn(true);
        when(statusService.getAllStreamStatuses()).thenReturn(List.of(status1, status2));
        assertTrue(disposer.isRunning(), "Expected true when at least one stream is active");

        // allActive
        status1 = mock(TransferStatus.class);
        status2 = mock(TransferStatus.class);
        when(status1.isActive()).thenReturn(true);
        when(status2.isActive()).thenReturn(true);
        when(statusService.getAllStreamStatuses()).thenReturn(List.of(status1, status2));
        assertTrue(disposer.isRunning(), "Expected true when all streams are active");

        // empty list
        when(statusService.getAllStreamStatuses()).thenReturn(List.of());
        assertFalse(disposer.isRunning(), "Expected false when there are no streams");
    }
}
