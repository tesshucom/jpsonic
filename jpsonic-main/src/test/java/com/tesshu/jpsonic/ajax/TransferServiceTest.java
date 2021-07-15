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

package com.tesshu.jpsonic.ajax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tesshu.jpsonic.NeedsHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@ExtendWith(NeedsHome.class)
class TransferServiceTest {

    private static final String ADMIN_NAME = "admin";

    @Mock
    private AjaxHelper ajaxHelper;
    @Autowired
    private MockHttpSession session;

    private TransferService transferService;

    @BeforeEach
    public void setup() {
        Mockito.when(ajaxHelper.getSession()).thenReturn(session);
        transferService = new TransferService(ajaxHelper);
    }

    @Test
    @WithMockUser(username = ADMIN_NAME)
    void testGetUploadInfo() {
        UploadInfo uploadInfo = transferService.getUploadInfo();
        assertEquals(0L, uploadInfo.getBytesTotal());
        assertEquals(0L, uploadInfo.getBytesUploaded());
    }
}
