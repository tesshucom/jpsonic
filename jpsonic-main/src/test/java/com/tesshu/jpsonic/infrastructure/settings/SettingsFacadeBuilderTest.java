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

package com.tesshu.jpsonic.infrastructure.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SettingsFacadeBuilderTest {

    @Test
    void testCaptureInt() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        SettingsFacade facade = new SettingsFacadeBuilder()
            .captureInt(SKeys.advanced.bandwidth.bufferSize, captor)
            .build();
        facade.staging(SKeys.advanced.bandwidth.bufferSize, 123);
        assertThat(captor.getAllValues()).containsExactly(123);
    }

    @Test
    void testCaptureIntMultiple() {
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        SettingsFacade facade = new SettingsFacadeBuilder()
            .captureInt(SKeys.advanced.bandwidth.bufferSize, captor)
            .build();
        facade.staging(SKeys.advanced.bandwidth.bufferSize, 123);
        facade.staging(SKeys.advanced.bandwidth.bufferSize, 456);
        assertEquals(2, captor.getAllValues().size());
        assertEquals(123, captor.getAllValues().get(0));
        assertEquals(456, captor.getAllValues().get(1));
    }

    @Test
    void testWithDefault() {
        SettingsFacade settingsFacade = new SettingsFacadeBuilder().buildWithDefault();

        assertEquals(SKeys.general.index.indexString.defaultValue(),
                settingsFacade.get(SKeys.general.index.indexString));
        assertEquals(SKeys.advanced.scanLog.scanLogRetention.defaultValue(),
                settingsFacade.get(SKeys.advanced.scanLog.scanLogRetention));
        assertEquals(SKeys.advanced.bandwidth.uploadBitrateLimit.defaultValue(),
                settingsFacade.get(SKeys.advanced.bandwidth.uploadBitrateLimit));
        assertEquals(SKeys.advanced.index.ignoreFullWidth.defaultValue(),
                settingsFacade.get(SKeys.advanced.index.ignoreFullWidth));
    }
}
