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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tesshu.jpsonic.service.SecurityService;
import com.tesshu.jpsonic.service.ServiceMockUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ViewAsListSelectorTest {

    @Test
    void testIsShowOutlineHelp() throws Exception {
        ViewAsListSelector selector = new ViewAsListSelector(mock(SecurityService.class));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter(Attributes.Request.USER_NAME.value(), ServiceMockUtils.ADMIN_NAME);
        req.addParameter(Attributes.Request.VIEW_AS_LIST.value(), Boolean.TRUE.toString());
        assertTrue(selector.isViewAsList(req, ServiceMockUtils.ADMIN_NAME));
    }
}
