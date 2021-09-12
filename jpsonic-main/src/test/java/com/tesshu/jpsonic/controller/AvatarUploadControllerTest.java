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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tesshu.jpsonic.service.AvatarService;
import com.tesshu.jpsonic.service.SecurityService;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

class AvatarUploadControllerTest {

    private AvatarUploadController controller;

    @BeforeEach
    public void setup() {
        controller = new AvatarUploadController(mock(SecurityService.class), mock(AvatarService.class));
    }

    @Test
    void testFailHandleRequest() throws Exception {
        assertThrows(FileUploadException.class, () -> {
            controller.handleRequestInternal(new MockMultipartHttpServletRequest());
        });
    }
}
