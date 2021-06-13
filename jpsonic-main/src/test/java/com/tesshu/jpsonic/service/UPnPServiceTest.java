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

package com.tesshu.jpsonic.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class UPnPServiceTest {

    @Autowired
    private UPnPService uPnPService;

    @SuppressWarnings("unchecked")
    private boolean isRunning() throws ExecutionException {
        Field field;
        try {
            field = uPnPService.getClass().getDeclaredField("running");
            field.setAccessible(true);
            return ((AtomicReference<Boolean>) field.get(uPnPService)).get();
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new ExecutionException(e);
        }
    }

    @Test
    void testEnsureServiceStopped() throws ExecutionException {
        assertFalse(isRunning());
        uPnPService.ensureServiceStopped();
        assertFalse(isRunning());

        uPnPService.ensureServiceStarted();
        assertTrue(isRunning());
        uPnPService.ensureServiceStopped();
        assertFalse(isRunning());
    }
}
