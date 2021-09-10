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

package com.tesshu.jpsonic.controller;

import static com.tesshu.jpsonic.service.ServiceMockUtils.mock;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.ExecutionException;

import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OutlineHelpSelectorTest {

    private OutlineHelpSelector outlineHelpSelector;

    @BeforeEach
    public void setup() throws ExecutionException {
        outlineHelpSelector = new OutlineHelpSelector(mock(SecurityService.class));
    }

    @Test
    void testIsShowOutlineHelp() throws Exception {
        assertFalse(outlineHelpSelector.isShowOutlineHelp(new MockHttpServletRequest(), ServiceMockUtils.ADMIN_NAME));
    }
}
